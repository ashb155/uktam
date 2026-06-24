cmake_minimum_required(VERSION 3.22.1)
project(indicoffline)

add_library(llama_jni SHARED llama_jni.cpp)

target_include_directories(llama_jni PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

target_link_libraries(llama_jni
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libllama-common.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml-base.so
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libggml-cpu.so
    android
    log
)