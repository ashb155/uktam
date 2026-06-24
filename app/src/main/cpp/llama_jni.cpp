#include <jni.h>
#include <vector>
#include <string>
#include <cstring>
#include "llama.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_indicoffline_LlamaWrapper_loadModel(JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    llama_model_params params = llama_model_default_params();
    llama_model *model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(modelPath, path);
    return (jlong) model;
}

JNIEXPORT jstring JNICALL
Java_com_example_indicoffline_LlamaWrapper_completion(JNIEnv *env, jobject, jlong modelPtr, jstring promptStr) {
    llama_model *model = (llama_model *) modelPtr;
    if (!model) return env->NewStringUTF("ERROR: model is null");

    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 4096;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    llama_context *ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) {
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("ERROR: failed to create context");
    }

    const llama_vocab *vocab = llama_model_get_vocab(model);

    std::vector<llama_token> tokens(2048);
    int n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        llama_free(ctx);
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("ERROR: tokenization failed");
    }
    tokens.resize(n_tokens);

    llama_batch batch = llama_batch_get_one(tokens.data(), n_tokens);
    if (llama_decode(ctx, batch) != 0) {
        llama_free(ctx);
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("ERROR: decode failed");
    }

    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.01f));
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
    llama_free(ctx);
    env->ReleaseStringUTFChars(promptStr, prompt);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_example_indicoffline_LlamaWrapper_freeModel(JNIEnv *env, jobject, jlong modelPtr) {
    if (modelPtr) llama_model_free((llama_model *) modelPtr);
}

}