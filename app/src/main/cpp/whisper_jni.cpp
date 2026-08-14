#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_vozmayores_audio_WhisperEngine_nativeSystemInfo(
        JNIEnv *env, jobject /* thiz */) {
    const char *info = whisper_print_system_info();
    return env->NewStringUTF(info != nullptr ? info : "");
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_vozmayores_audio_WhisperEngine_nativeLoadModel(
        JNIEnv *env, jobject /* thiz */, jstring j_path) {
    const char *path = env->GetStringUTFChars(j_path, nullptr);
    if (path == nullptr) return 0;

    struct whisper_context_params cparams = whisper_context_default_params();
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);

    env->ReleaseStringUTFChars(j_path, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_vozmayores_audio_WhisperEngine_nativeFreeModel(
        JNIEnv * /* env */, jobject /* thiz */, jlong ctx_ptr) {
    if (ctx_ptr == 0) return;
    auto *ctx = reinterpret_cast<struct whisper_context *>(ctx_ptr);
    whisper_free(ctx);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_vozmayores_audio_WhisperEngine_nativeTranscribe(
        JNIEnv *env, jobject /* thiz */,
        jlong ctx_ptr, jshortArray j_samples,
        jstring j_lang, jstring j_prompt) {
    if (ctx_ptr == 0) return env->NewStringUTF("");
    auto *ctx = reinterpret_cast<struct whisper_context *>(ctx_ptr);

    const jsize n = env->GetArrayLength(j_samples);
    if (n <= 0) return env->NewStringUTF("");

    jshort *raw = env->GetShortArrayElements(j_samples, nullptr);
    if (raw == nullptr) return env->NewStringUTF("");

    std::vector<float> pcmf32(static_cast<size_t>(n));
    const float scale = 1.0f / 32768.0f;
    for (jsize i = 0; i < n; ++i) {
        pcmf32[i] = static_cast<float>(raw[i]) * scale;
    }
    env->ReleaseShortArrayElements(j_samples, raw, JNI_ABORT);

    const char *lang = env->GetStringUTFChars(j_lang, nullptr);
    const char *prompt = nullptr;
    if (j_prompt != nullptr) {
        prompt = env->GetStringUTFChars(j_prompt, nullptr);
    }

    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language         = lang;
    params.translate        = false;
    params.n_threads        = 4;
    params.no_context       = true;
    params.single_segment   = false;
    params.print_progress   = false;
    params.print_realtime   = false;
    params.print_timestamps = false;
    params.print_special    = false;
    params.suppress_blank   = true;
    if (prompt != nullptr && prompt[0] != '\0') {
        params.initial_prompt = prompt;
    }

    const int ret = whisper_full(ctx, params, pcmf32.data(), static_cast<int>(n));
    if (lang   != nullptr) env->ReleaseStringUTFChars(j_lang,   lang);
    if (prompt != nullptr) env->ReleaseStringUTFChars(j_prompt, prompt);

    if (ret != 0) {
        std::string err = "whisper_full ret=" + std::to_string(ret);
        return env->NewStringUTF(err.c_str());
    }

    std::string out;
    const int nseg = whisper_full_n_segments(ctx);
    for (int i = 0; i < nseg; ++i) {
        const char *t = whisper_full_get_segment_text(ctx, i);
        if (t != nullptr) out.append(t);
    }
    return env->NewStringUTF(out.c_str());
}
