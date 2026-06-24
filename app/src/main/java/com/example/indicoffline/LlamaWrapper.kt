package com.example.indicoffline

object LlamaWrapper {
    init {
        System.loadLibrary("ggml")
        System.loadLibrary("ggml-base")
        System.loadLibrary("ggml-cpu")
        System.loadLibrary("llama-common")
        System.loadLibrary("llama")
        System.loadLibrary("llama_jni")
    }

    external fun loadModel(modelPath: String): Long
    external fun completion(ctx: Long, prompt: String): String
    external fun freeModel(ctx: Long)
}