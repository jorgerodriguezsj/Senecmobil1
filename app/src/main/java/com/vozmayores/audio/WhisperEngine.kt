package com.vozmayores.audio

object WhisperEngine {
    init {
        System.loadLibrary("voz-native")
    }

    external fun nativeSystemInfo(): String
}
