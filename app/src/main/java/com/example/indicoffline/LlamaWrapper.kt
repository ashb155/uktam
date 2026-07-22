package com.example.indicoffline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
    external fun startCompletion(ctx: Long, prompt: String): Boolean
    external fun getNextToken(ctx: Long): String?
    
    fun completion(ctx: Long, prompt: String): String {
        if (!startCompletion(ctx, prompt)) {
            return "ERROR: failed to start generation"
        }
        val sb = java.lang.StringBuilder()
        while (true) {
            val token = getNextToken(ctx) ?: break
            sb.append(token)
        }
        return sb.toString()
    }

    fun generateStream(ctx: Long, prompt: String): Flow<String> = flow {
        if (!startCompletion(ctx, prompt)) {
            emit("ERROR: failed to start generation")
            return@flow
        }
        
        while (true) {
            val token = getNextToken(ctx) ?: break
            emit(token)
        }
    }

    external fun freeModel(ctx: Long)
}