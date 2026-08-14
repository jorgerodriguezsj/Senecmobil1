#include <jni.h>
#include "whisper.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_vozmayores_audio_WhisperEngine_nativeSystemInfo(
        JNIEnv *env,
        jobject /* thiz */) {
    const char *info = whisper_print_system_info();
    return env->NewStringUTF(info != nullptr ? info : "");
}
