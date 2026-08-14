#include <jni.h>
#include <string>
#include <vector>
#include "llama.h"

namespace {
    struct LlmContext {
        llama_model   *model   = nullptr;
        llama_context *ctx     = nullptr;
        llama_sampler *sampler = nullptr;
    };

    bool g_backend_inited = false;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_vozmayores_llm_LlmEngine_nativeLoadModel(
        JNIEnv *env, jobject /* thiz */, jstring j_path) {
    if (!g_backend_inited) {
        llama_backend_init();
        g_backend_inited = true;
    }

    const char *path = env->GetStringUTFChars(j_path, nullptr);
    if (path == nullptr) return 0;

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0; // solo CPU en Android

    llama_model *model = llama_model_load_from_file(path, mp);
    env->ReleaseStringUTFChars(j_path, path);
    if (model == nullptr) return 0;

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx           = 2048;
    cp.n_batch         = 512;
    cp.n_ubatch        = 512;
    cp.n_threads       = 4;
    cp.n_threads_batch = 4;

    llama_context *ctx = llama_init_from_model(model, cp);
    if (ctx == nullptr) {
        llama_model_free(model);
        return 0;
    }

    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    auto *llm = new LlmContext{model, ctx, sampler};
    return reinterpret_cast<jlong>(llm);
}

extern "C" JNIEXPORT void JNICALL
Java_com_vozmayores_llm_LlmEngine_nativeFreeModel(
        JNIEnv * /* env */, jobject /* thiz */, jlong ctx_ptr) {
    if (ctx_ptr == 0) return;
    auto *llm = reinterpret_cast<LlmContext *>(ctx_ptr);
    if (llm->sampler) llama_sampler_free(llm->sampler);
    if (llm->ctx)     llama_free(llm->ctx);
    if (llm->model)   llama_model_free(llm->model);
    delete llm;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_vozmayores_llm_LlmEngine_nativeGenerate(
        JNIEnv *env, jobject /* thiz */,
        jlong ctx_ptr, jstring j_prompt, jint max_tokens) {
    if (ctx_ptr == 0) return env->NewStringUTF("");
    auto *llm = reinterpret_cast<LlmContext *>(ctx_ptr);

    const char *prompt_c = env->GetStringUTFChars(j_prompt, nullptr);
    if (prompt_c == nullptr) return env->NewStringUTF("");
    std::string prompt(prompt_c);
    env->ReleaseStringUTFChars(j_prompt, prompt_c);

    const llama_vocab *vocab = llama_model_get_vocab(llm->model);

    // Vacía la KV cache de la generación anterior.
    llama_memory_clear(llama_get_memory(llm->ctx), true);

    // Primer llama_tokenize con buffer NULL devuelve -tokens_requeridos.
    int n_prompt = -llama_tokenize(
        vocab, prompt.c_str(), (int32_t) prompt.size(),
        nullptr, 0, /* add_special */ true, /* parse_special */ true);
    if (n_prompt <= 0) return env->NewStringUTF("");

    std::vector<llama_token> tokens(n_prompt);
    if (llama_tokenize(
            vocab, prompt.c_str(), (int32_t) prompt.size(),
            tokens.data(), (int32_t) tokens.size(), true, true) < 0) {
        return env->NewStringUTF("");
    }

    llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t) tokens.size());
    if (llama_decode(llm->ctx, batch) != 0) return env->NewStringUTF("");

    std::string result;
    int n_generated = 0;
    llama_token new_token = 0;

    while (n_generated < max_tokens) {
        new_token = llama_sampler_sample(llm->sampler, llm->ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) break;

        char piece[128];
        int n = llama_token_to_piece(
            vocab, new_token, piece, sizeof(piece), /* lstrip */ 0, /* special */ false);
        if (n > 0) {
            result.append(piece, n);
        }

        batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(llm->ctx, batch) != 0) break;
        n_generated++;
    }

    return env->NewStringUTF(result.c_str());
}
