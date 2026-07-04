#include <jni.h>
#include <vector>
#include <string>
#include <cstring>
#include <thread>
#include <algorithm>
#include "llama.h"

struct LlamaState {
    llama_model *model;
    llama_context *ctx;
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
    int hw_threads = std::thread::hardware_concurrency();
    int threads = (hw_threads > 0) ? std::min(4, hw_threads) : 4;
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
    if (!ctx) {
        llama_model_free(model);
        return 0L;
    }

    LlamaState *state = new LlamaState();
    state->model = model;
    state->ctx = ctx;

    return (jlong) state;
}

JNIEXPORT jstring JNICALL
Java_com_example_indicoffline_LlamaWrapper_completion(JNIEnv *env, jobject, jlong statePtr, jstring promptStr) {
    LlamaState *state = (LlamaState *) statePtr;
    if (!state || !state->model || !state->ctx) {
        return env->NewStringUTF("ERROR: state is null");
    }

    llama_model *model = state->model;
    llama_context *ctx = state->ctx;

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
        return env->NewStringUTF("ERROR: tokenization failed");
    }
    tokens.resize(n_tokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(ctx, batch) != 0) {
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("ERROR: decode failed");
    }

    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string result;
    result.reserve(1024);

    for (int i = 0; i < 200; i++) {
        llama_token token = llama_sampler_sample(sampler, ctx, -1);
        if (llama_vocab_is_eog(vocab, token)) break;

        char buf[256];
        int len = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, true);
        if (len < 0) break;
        result.append(buf, len);

        llama_batch next = llama_batch_get_one(&token, 1);
        if (llama_decode(ctx, next) != 0) break;
    }

    llama_sampler_free(sampler);
    env->ReleaseStringUTFChars(promptStr, prompt);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_indicoffline_LlamaWrapper_freeModel(JNIEnv *env, jobject, jlong statePtr) {
    if (statePtr) {
        auto *state = (LlamaState *) statePtr;
        if (state->ctx) llama_free(state->ctx);
        if (state->model) llama_model_free(state->model);
        delete state;
    }
}

}