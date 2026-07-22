#include <jni.h>
#include <vector>
#include <string>
#include <cstring>
#include <thread>
#include <algorithm>
#include "llama.h"

#include <sched.h>
#include <unistd.h>
#include <fstream>
#include <dirent.h>
#include <android/log.h>

std::vector<int> get_performance_cores() {
    std::vector<int> perf_cores;
    std::vector<std::pair<int, long>> core_capacities;
    bool used_freq_fallback = false;

    for (int i = 0; i < 32; ++i) { 
        std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpu_capacity";
        std::ifstream file(path);
        if (file.is_open()) {
            long capacity;
            if (file >> capacity) {
                core_capacities.push_back({i, capacity});
            }
        }
    }

    if (core_capacities.empty()) {
        used_freq_fallback = true;
        for (int i = 0; i < 32; ++i) { 
            std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(i) + "/cpufreq/cpuinfo_max_freq";
            std::ifstream file(path);
            if (file.is_open()) {
                long freq;
                if (file >> freq) {
                    core_capacities.push_back({i, freq});
                }
            }
        }
    }

    if (!core_capacities.empty()) {
        long max_capacity = core_capacities[0].second;
        for (const auto& core : core_capacities) {
            if (core.second > max_capacity) max_capacity = core.second;
        }

        long threshold = used_freq_fallback ? (long)(max_capacity * 0.8) : (long)(max_capacity * 0.5);

        std::sort(core_capacities.begin(), core_capacities.end(), [](const std::pair<int, long>& a, const std::pair<int, long>& b) {
            return a.second > b.second;
        });

        for (const auto& core : core_capacities) {
            if (core.second >= threshold && perf_cores.size() < 4) {
                perf_cores.push_back(core.first);
                __android_log_print(ANDROID_LOG_DEBUG, "LlamaJNI", "Selected Core %d | Score: %ld", core.first, core.second);
            } else {
                __android_log_print(ANDROID_LOG_DEBUG, "LlamaJNI", "Rejected Core %d | Score: %ld", core.first, core.second);
            }
        }
    }
    return perf_cores;
}

struct LlamaState {
    llama_model *model;
    llama_context *ctx;
    llama_sampler *sampler;
    int token_count;
    bool is_generating;
};

#include <sys/system_properties.h>
#include <cstdlib>

int get_android_api_level() {
    char osVersion[PROP_VALUE_MAX+1];
    int len = __system_property_get("ro.build.version.sdk", osVersion);
    return (len > 0) ? std::atoi(osVersion) : 0;
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_indicoffline_LlamaWrapper_loadModel(JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    
    llama_model_params params = llama_model_default_params();
    llama_model *model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (!model) return 0L;

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024;
    ctx_params.n_batch = 512;
    std::vector<int> perf_cores = get_performance_cores();
    int threads = 4; 

    cpu_set_t original_mask;
    CPU_ZERO(&original_mask);
    bool has_original_mask = (sched_getaffinity(0, sizeof(original_mask), &original_mask) == 0);

    if (!perf_cores.empty()) {
        cpu_set_t mask;
        CPU_ZERO(&mask);
        std::string core_list = "[";
        for (size_t i = 0; i < perf_cores.size(); ++i) {
            CPU_SET(perf_cores[i], &mask);
            core_list += std::to_string(perf_cores[i]);
            if (i < perf_cores.size() - 1) core_list += ", ";
        }
        core_list += "]";

        if (sched_setaffinity(0, sizeof(cpu_set_t), &mask) == 0) {
            threads = perf_cores.size();
            __android_log_print(ANDROID_LOG_DEBUG, "LlamaJNI", "Successfully pinned thread to %d performance cores: %s", threads, core_list.c_str());
        } else {
            __android_log_print(ANDROID_LOG_ERROR, "LlamaJNI", "Failed to set thread affinity.");
            int hw_threads = std::thread::hardware_concurrency();
            threads = (hw_threads > 0) ? std::min(4, hw_threads) : 4;
        }
    } else {
        __android_log_print(ANDROID_LOG_WARN, "LlamaJNI", "Could not detect performance cores. Falling back to default.");
        int hw_threads = std::thread::hardware_concurrency();
        threads = (hw_threads > 0) ? std::min(4, hw_threads) : 4;
    }
    
    ctx_params.n_threads = threads;
    ctx_params.n_threads_batch = threads;
    

    if (get_android_api_level() >= 33) {
        ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_ENABLED;
    } else {
        ctx_params.flash_attn_type = LLAMA_FLASH_ATTN_TYPE_DISABLED;
        ctx_params.type_k = GGML_TYPE_F32;
        ctx_params.type_v = GGML_TYPE_F32;
    }

    llama_context *ctx = llama_init_from_model(model, ctx_params);
    
    if (has_original_mask) {
        sched_setaffinity(0, sizeof(original_mask), &original_mask);
        __android_log_print(ANDROID_LOG_DEBUG, "LlamaJNI", "Restored original JNI thread affinity.");
    }

    if (!ctx) {
        llama_model_free(model);
        return 0L;
    }

    LlamaState *state = new LlamaState();
    state->model = model;
    state->ctx = ctx;
    state->sampler = nullptr;
    state->token_count = 0;
    state->is_generating = false;

    return (jlong) state;
}

JNIEXPORT jboolean JNICALL
Java_com_example_indicoffline_LlamaWrapper_startCompletion(JNIEnv *env, jobject, jlong statePtr, jstring promptStr) {
    LlamaState *state = (LlamaState *) statePtr;
    if (!state || !state->model || !state->ctx) {
        return JNI_FALSE;
    }

    llama_model *model = state->model;
    llama_context *ctx = state->ctx;

    if (state->sampler) {
        llama_sampler_free(state->sampler);
        state->sampler = nullptr;
    }

    llama_memory_t mem = llama_get_memory(ctx);
    if (mem) {
        llama_memory_seq_rm(mem, -1, -1, -1);
    }

    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);
    const llama_vocab *vocab = llama_model_get_vocab(model);

    std::vector<llama_token> tokens(1024);
    int n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        env->ReleaseStringUTFChars(promptStr, prompt);
        return JNI_FALSE;
    }
    tokens.resize(n_tokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(ctx, batch) != 0) {
        env->ReleaseStringUTFChars(promptStr, prompt);
        return JNI_FALSE;
    }

    state->sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(state->sampler, llama_sampler_init_greedy());
    state->token_count = 0;
    state->is_generating = true;

    env->ReleaseStringUTFChars(promptStr, prompt);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_example_indicoffline_LlamaWrapper_getNextToken(JNIEnv *env, jobject, jlong statePtr) {
    LlamaState *state = (LlamaState *) statePtr;
    if (!state || !state->is_generating || !state->sampler) {
        return nullptr;
    }

    if (state->token_count >= 200) {
        state->is_generating = false;
        llama_sampler_free(state->sampler);
        state->sampler = nullptr;
        return nullptr;
    }

    const llama_vocab *vocab = llama_model_get_vocab(state->model);
    llama_token token = llama_sampler_sample(state->sampler, state->ctx, -1);
    
    if (llama_vocab_is_eog(vocab, token)) {
        state->is_generating = false;
        llama_sampler_free(state->sampler);
        state->sampler = nullptr;
        return nullptr;
    }

    char buf[256];
    int len = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, true);
    if (len < 0) {
        state->is_generating = false;
        llama_sampler_free(state->sampler);
        state->sampler = nullptr;
        return nullptr;
    }

    state->token_count++;

    llama_batch next = llama_batch_get_one(&token, 1);
    if (llama_decode(state->ctx, next) != 0) {
        state->is_generating = false;
        llama_sampler_free(state->sampler);
        state->sampler = nullptr;
    }

    return env->NewStringUTF(std::string(buf, len).c_str());
}

JNIEXPORT void JNICALL
Java_com_example_indicoffline_LlamaWrapper_freeModel(JNIEnv *env, jobject, jlong statePtr) {
    if (statePtr) {
        auto *state = (LlamaState *) statePtr;
        if (state->sampler) llama_sampler_free(state->sampler);
        if (state->ctx) llama_free(state->ctx);
        if (state->model) llama_model_free(state->model);
        delete state;
    }
}

}