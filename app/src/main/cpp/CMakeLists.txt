cmake_minimum_required(VERSION 3.22.1)
project(voz)

# Opciones comunes de ggml (whisper y llama comparten backend).
# - GGML_NATIVE OFF: nada de -march=native en cross-compile.
# - GGML_OPENMP OFF: OpenMP en NDK es un dolor de configurar.
# - GGML_LLAMAFILE OFF: código x86-only.
# - GGML_CPU_KLEIDIAI OFF: micro-optimizaciones ARM que requieren extras.
set(GGML_NATIVE            OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP            OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE         OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI      OFF CACHE BOOL "" FORCE)

# llama.cpp: solo la libreria, sin ejemplos/tools/server ni Web UI.
set(LLAMA_BUILD_COMMON     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS      OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS      OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES   OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER     OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP        OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_UI         OFF CACHE BOOL "" FORCE)
set(LLAMA_USE_PREBUILT_UI  OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL             OFF CACHE BOOL "" FORCE)

# whisper.cpp: solo la libreria.
set(WHISPER_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER   OFF CACHE BOOL "" FORCE)

# Orden importante: llama primero crea el target ggml y whisper lo
# reutiliza via su guard "if (NOT TARGET ggml)".
add_subdirectory(llama.cpp)
add_subdirectory(whisper.cpp)

add_library(voz-native SHARED
    whisper_jni.cpp
    llama_jni.cpp
)

target_compile_features(voz-native PRIVATE cxx_std_17)

target_link_libraries(voz-native
    PRIVATE
    whisper
    llama
    log
    android
)
