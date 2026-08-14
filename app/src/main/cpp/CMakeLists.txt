cmake_minimum_required(VERSION 3.22.1)
project(voz)

# Opciones de whisper.cpp / ggml para Android
# - GGML_NATIVE OFF: evita -march=native en cross-compile.
# - GGML_OPENMP OFF: el NDK no siempre expone libomp de forma predecible;
#   se puede reactivar más adelante si se necesita.
# - GGML_LLAMAFILE OFF: código específico de x86.
# - GGML_CPU_KLEIDIAI OFF: micro-optimizaciones ARM que requieren extras.
# - Sin tests, ejemplos ni server.
set(GGML_NATIVE            OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP            OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE         OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI      OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER   OFF CACHE BOOL "" FORCE)

add_subdirectory(whisper.cpp)

add_library(voz-native SHARED
    whisper_jni.cpp
)

target_compile_features(voz-native PRIVATE cxx_std_17)

target_link_libraries(voz-native
    PRIVATE
    whisper
    log
    android
)
