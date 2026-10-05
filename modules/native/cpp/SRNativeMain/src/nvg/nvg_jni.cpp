#include "io_homo_superresolution_thirdparty_nanovg_NanoVGColor.h"
#include "io_homo_superresolution_thirdparty_nanovg_NanoVGContext.h"
#include "io_homo_superresolution_thirdparty_nanovg_NanoVGPaint.h"
#include "nvg/nanovg_cpp.h"
#include <string>
#include "define.h"

static JNIEnv *g0_envForCallback = nullptr;

static constexpr jint NANOVG_BACKEND_MODE_RHI_DIRECT = 1;
static constexpr const char *NANOVG_RHI_BRIDGE_CLASS = "io/homo/superresolution/thirdparty/nanovg/NanoVGRhiBridge";

JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGColor_nGetNanoVGColorR(
    JNIEnv *, jobject, jlong ptr) {
    auto *color = reinterpret_cast<NanoVGColor *>(ptr);
    return color->color.r;
}

JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGColor_nGetNanoVGColorG(
    JNIEnv *, jobject, jlong ptr) {
    auto *color = reinterpret_cast<NanoVGColor *>(ptr);
    return color->color.g;
}

JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGColor_nGetNanoVGColorB(
    JNIEnv *, jobject, jlong ptr) {
    auto *color = reinterpret_cast<NanoVGColor *>(ptr);
    return color->color.b;
}

JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGColor_nGetNanoVGColorA(
    JNIEnv *, jobject, jlong ptr) {
    auto *color = reinterpret_cast<NanoVGColor *>(ptr);
    return color->color.a;
}

JNIEXPORT void JNICALL

Java_io_homo_superresolution_thirdparty_nanovg_NanoVGColor_nDelete(JNIEnv *, jobject, jlong ptr) {
    auto *color = reinterpret_cast<NanoVGColor *>(ptr);

    delete color;
}

JNIEXPORT void JNICALL

Java_io_homo_superresolution_thirdparty_nanovg_NanoVGPaint_nDelete(JNIEnv *, jobject, jlong ptr) {
    auto *paint = reinterpret_cast<NanoVGPaint *>(ptr);

    delete paint;
}

void *java_glfwGetProcAddress(const char *name) {
    if (!g0_envForCallback) {
        return nullptr;
    };

    jclass cpp_helper = g0_envForCallback->FindClass(JAVA_CPPHELPER_CLASS);
    jmethodID methodID = g0_envForCallback->GetStaticMethodID(
        cpp_helper,
        "CPP_glfwGetProcAddress",
        "(Ljava/lang/String;)J");

    if (!methodID) {
        g0_envForCallback->ThrowNew(g0_envForCallback->FindClass("java/lang/RuntimeException"), "methodID is null");
        return nullptr;
    }

    jstring jmsg = g0_envForCallback->NewStringUTF(name);
    jlong jlongValue = g0_envForCallback->CallStaticLongMethod(cpp_helper, methodID, jmsg);
    g0_envForCallback->DeleteLocalRef(jmsg);

    return reinterpret_cast<void *>(jlongValue);
}

static jclass findRhiBridgeClass(JNIEnv *env) {
    return env->FindClass(NANOVG_RHI_BRIDGE_CLASS);
}

static jobject makeDirectBuffer(JNIEnv *env, const void *ptr, int bytes) {
    if (!ptr || bytes <= 0) {
        return nullptr;
    }
    return env->NewDirectByteBuffer(const_cast<void *>(ptr), bytes);
}

static int rhiCreateTextureCallback(void *, int imageId, int type, int w, int h, int imageFlags,
                                    const unsigned char *data, int dataSize) {
    if (!g0_envForCallback) {
        return 0;
    }
    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return 0;
    }
    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nCreateTexture",
        "(IIIIILjava/nio/ByteBuffer;I)Z");
    if (!method) {
        return 0;
    }

    jobject dataBuffer = makeDirectBuffer(env, data, dataSize);
    jboolean ok = env->CallStaticBooleanMethod(
        bridgeClass,
        method,
        imageId,
        type,
        w,
        h,
        imageFlags,
        dataBuffer,
        dataSize);
    if (dataBuffer) {
        env->DeleteLocalRef(dataBuffer);
    }
    return ok == JNI_TRUE ? 1 : 0;
}

static int rhiRegisterExternalTextureCallback(void *, int imageId, unsigned int externalTextureHandle,
                                              int w, int h, int imageFlags) {
    if (!g0_envForCallback) {
        return 0;
    }
    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return 0;
    }
    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nRegisterExternalTexture",
        "(IIIII)Z");
    if (!method) {
        return 0;
    }
    jboolean ok = env->CallStaticBooleanMethod(
        bridgeClass,
        method,
        imageId,
        static_cast<jint>(externalTextureHandle),
        w,
        h,
        imageFlags);
    return ok == JNI_TRUE ? 1 : 0;
}

static int rhiUpdateTextureCallback(void *, int imageId, int x, int y, int w, int h,
                                    const unsigned char *data, int dataSize) {
    if (!g0_envForCallback) {
        return 0;
    }
    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return 0;
    }
    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nUpdateTexture",
        "(IIIIILjava/nio/ByteBuffer;I)Z");
    if (!method) {
        return 0;
    }

    jobject dataBuffer = makeDirectBuffer(env, data, dataSize);
    jboolean ok = env->CallStaticBooleanMethod(
        bridgeClass,
        method,
        imageId,
        x,
        y,
        w,
        h,
        dataBuffer,
        dataSize);
    if (dataBuffer) {
        env->DeleteLocalRef(dataBuffer);
    }
    return ok == JNI_TRUE ? 1 : 0;
}

static int rhiDeleteTextureCallback(void *, int imageId) {
    if (!g0_envForCallback) {
        return 0;
    }
    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return 0;
    }
    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nDeleteTexture",
        "(I)V");
    if (!method) {
        return 0;
    }
    env->CallStaticVoidMethod(bridgeClass, method, imageId);
    return 1;
}

static void rhiViewportCallback(void *, float width, float height, float devicePixelRatio) {
    if (!g0_envForCallback) {
        return;
    }
    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return;
    }
    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nViewport",
        "(FFF)V");
    if (!method) {
        return;
    }
    env->CallStaticVoidMethod(bridgeClass, method, width, height, devicePixelRatio);
}

static void rhiFlushCallback(void *, float viewWidth, float viewHeight,
                             const void *verts, int nverts,
                             const void *paths, int npaths,
                             const void *calls, int ncalls,
                             const unsigned char *uniforms, int uniformBytes, int fragSize, int callStride) {
    if (!g0_envForCallback) {
        return;
    }

    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return;
    }

    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nFlush",
        "(FFLjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;III)V");
    if (!method) {
        return;
    }

    int vertsBytes = nverts * static_cast<int>(sizeof(NVGvertex));
    int pathsBytes = npaths * static_cast<int>(sizeof(NVGRHIPath));
    int callsBytes = ncalls * callStride;

    jobject vertsBuffer = makeDirectBuffer(env, verts, vertsBytes);
    jobject pathsBuffer = makeDirectBuffer(env, paths, pathsBytes);
    jobject callsBuffer = makeDirectBuffer(env, calls, callsBytes);
    jobject uniformsBuffer = makeDirectBuffer(env, uniforms, uniformBytes);

    env->CallStaticVoidMethod(
        bridgeClass,
        method,
        viewWidth,
        viewHeight,
        vertsBuffer,
        nverts,
        pathsBuffer,
        npaths,
        callsBuffer,
        ncalls,
        uniformsBuffer,
        uniformBytes,
        fragSize,
        callStride);

    if (vertsBuffer) env->DeleteLocalRef(vertsBuffer);
    if (pathsBuffer) env->DeleteLocalRef(pathsBuffer);
    if (callsBuffer) env->DeleteLocalRef(callsBuffer);
    if (uniformsBuffer) env->DeleteLocalRef(uniformsBuffer);
}

static void rhiDestroyCallback(void *) {
    if (!g0_envForCallback) {
        return;
    }
    JNIEnv *env = g0_envForCallback;
    jclass bridgeClass = findRhiBridgeClass(env);
    if (!bridgeClass) {
        return;
    }
    jmethodID method = env->GetStaticMethodID(
        bridgeClass,
        "nDestroy",
        "()V");
    if (!method) {
        return;
    }
    env->CallStaticVoidMethod(bridgeClass, method);
}

static NVGRHICallbacks g_rhiCallbacks = {
    rhiCreateTextureCallback,
    rhiRegisterExternalTextureCallback,
    rhiUpdateTextureCallback,
    rhiDeleteTextureCallback,
    rhiViewportCallback,
    rhiFlushCallback,
    rhiDestroyCallback
};

static jlong createContextInternal(JNIEnv *env, jint flags) {
    g0_envForCallback = env;
    auto *ctx = new NanoVGContext(
        flags,
        &g_rhiCallbacks,
        nullptr);
    ctx->Reset();
    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_createContext(
    JNIEnv *env, jclass, jint flags) {
    try {
        return createContextInternal(env, flags);
    } catch (const std::exception &e) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
        return 0;
    }
}

extern "C" JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCreateContextEx(
    JNIEnv *env, jclass, jint flags, jint backendMode) {
    try {
        return createContextInternal(env, flags);
    } catch (const std::exception &e) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nDeleteContext(
    JNIEnv *env, jclass, jlong ptr) {
    g0_envForCallback = env;
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);

    delete ctx;
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nBeginFrame(
    JNIEnv *env, jclass, jlong ptr, jfloat windowWidth, jfloat windowHeight, jfloat devicePixelRatio) {
    g0_envForCallback = env;
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->BeginFrame(windowWidth, windowHeight, devicePixelRatio);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCancelFrame(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->CancelFrame();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nEndFrame(
    JNIEnv *env, jclass, jlong ptr) {
    g0_envForCallback = env;
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->EndFrame();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nGlobalCompositeOperation(
    JNIEnv *, jclass, jlong ptr, jint op) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->GlobalCompositeOperation(op);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nGlobalCompositeBlendFunc(
    JNIEnv *, jclass, jlong ptr, jint sfactor, jint dfactor) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->GlobalCompositeBlendFunc(sfactor, dfactor);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nGlobalCompositeBlendFuncSeparate(
    JNIEnv *, jclass, jlong ptr, jint srcRGB, jint dstRGB, jint srcAlpha, jint dstAlpha) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->GlobalCompositeBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nSave(JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Save();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRestore(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Restore();
}

JNIEXPORT void JNICALL

Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nReset(JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Reset();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nShapeAntiAlias(
    JNIEnv *, jclass, jlong ptr, jint enabled) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->ShapeAntiAlias(enabled);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nStrokeColor(
    JNIEnv *, jclass, jlong ptr, jlong colorPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *color = reinterpret_cast<NanoVGColor *>(colorPtr);
    ctx->StrokeColor(*color);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nColorRGBA(
    JNIEnv *, jclass, jlong ptr, jint r, jint g, jint b, jint a) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *color = new NanoVGColor(ctx->ColorRGBA(r, g, b, a));
    return reinterpret_cast<jlong>(color);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nColorRGBAf(
    JNIEnv *, jclass, jlong ptr, jfloat r, jfloat g, jfloat b, jfloat a) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *color = new NanoVGColor(ctx->ColorRGBAf(r, g, b, a));
    return reinterpret_cast<jlong>(color);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nStrokePaint(
    JNIEnv *, jclass, jlong ptr, jlong paintPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *paint = reinterpret_cast<NanoVGPaint *>(paintPtr);
    ctx->StrokePaint(*paint);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFillColor(
    JNIEnv *, jclass, jlong ptr, jlong colorPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *color = reinterpret_cast<NanoVGColor *>(colorPtr);
    ctx->FillColor(*color);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFillPaint(
    JNIEnv *, jclass, jlong ptr, jlong paintPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *paint = reinterpret_cast<NanoVGPaint *>(paintPtr);
    ctx->FillPaint(*paint);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nMiterLimit(
    JNIEnv *, jclass, jlong ptr, jfloat limit) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->MiterLimit(limit);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nStrokeWidth(
    JNIEnv *, jclass, jlong ptr, jfloat size) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->StrokeWidth(size);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nLineCap(
    JNIEnv *, jclass, jlong ptr, jint cap) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->LineCap(cap);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nLineJoin(
    JNIEnv *, jclass, jlong ptr, jint join) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->LineJoin(join);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nGlobalAlpha(
    JNIEnv *, jclass, jlong ptr, jfloat alpha) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->GlobalAlpha(alpha);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nResetTransform(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->ResetTransform();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTransform(
    JNIEnv *, jclass, jlong ptr, jfloat a, jfloat b, jfloat c, jfloat d, jfloat e, jfloat f) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Transform(a, b, c, d, e, f);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTranslate(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Translate(x, y);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRotate(
    JNIEnv *, jclass, jlong ptr, jfloat angle) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Rotate(angle);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nSkewX(
    JNIEnv *, jclass, jlong ptr, jfloat angle) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->SkewX(angle);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nSkewY(
    JNIEnv *, jclass, jlong ptr, jfloat angle) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->SkewY(angle);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nScale(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Scale(x, y);
}

JNIEXPORT jfloatArray JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCurrentTransform(
    JNIEnv *env, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    std::array<float, 6> xform = ctx->CurrentTransform();

    jfloatArray result = env->NewFloatArray(6);
    env->SetFloatArrayRegion(result, 0, 6, xform.data());
    return result;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCreateImageFromHandle(
    JNIEnv *env, jclass, jlong ptr, jint textureId, jint w, jint h, jint imageFlags) {
    g0_envForCallback = env;
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    return ctx->CreateImageFromHandle(textureId, w, h, imageFlags);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nDeleteImage(
    JNIEnv *env, jclass, jlong ptr, jint image) {
    g0_envForCallback = env;
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->DeleteImage(image);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nLinearGradient(
    JNIEnv *, jclass, jlong ptr, jfloat sx, jfloat sy, jfloat ex, jfloat ey, jlong icolPtr, jlong ocolPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *icol = reinterpret_cast<NanoVGColor *>(icolPtr);
    auto *ocol = reinterpret_cast<NanoVGColor *>(ocolPtr);
    auto *paint = new NanoVGPaint(ctx->LinearGradient(sx, sy, ex, ey, *icol, *ocol));
    return reinterpret_cast<jlong>(paint);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nBoxGradient(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h, jfloat r, jfloat f, jlong icolPtr,
    jlong ocolPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *icol = reinterpret_cast<NanoVGColor *>(icolPtr);
    auto *ocol = reinterpret_cast<NanoVGColor *>(ocolPtr);
    auto *paint = new NanoVGPaint(ctx->BoxGradient(x, y, w, h, r, f, *icol, *ocol));
    return reinterpret_cast<jlong>(paint);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRadialGradient(
    JNIEnv *, jclass, jlong ptr, jfloat cx, jfloat cy, jfloat inr, jfloat outr, jlong icolPtr, jlong ocolPtr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *icol = reinterpret_cast<NanoVGColor *>(icolPtr);
    auto *ocol = reinterpret_cast<NanoVGColor *>(ocolPtr);
    auto *paint = new NanoVGPaint(ctx->RadialGradient(cx, cy, inr, outr, *icol, *ocol));
    return reinterpret_cast<jlong>(paint);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nImagePattern(
    JNIEnv *, jclass, jlong ptr, jfloat ox, jfloat oy, jfloat ex, jfloat ey, jfloat angle, jint image, jfloat alpha) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    auto *paint = new NanoVGPaint(ctx->ImagePattern(ox, oy, ex, ey, angle, image, alpha));
    return reinterpret_cast<jlong>(paint);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nScissor(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Scissor(x, y, w, h);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nIntersectScissor(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->IntersectScissor(x, y, w, h);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nResetScissor(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->ResetScissor();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nBeginPath(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->BeginPath();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nMoveTo(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->MoveTo(x, y);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nLineTo(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->LineTo(x, y);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nBezierTo(
    JNIEnv *, jclass, jlong ptr, jfloat c1x, jfloat c1y, jfloat c2x, jfloat c2y, jfloat x, jfloat y) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->BezierTo(c1x, c1y, c2x, c2y, x, y);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nQuadTo(
    JNIEnv *, jclass, jlong ptr, jfloat cx, jfloat cy, jfloat x, jfloat y) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->QuadTo(cx, cy, x, y);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nArcTo(
    JNIEnv *, jclass, jlong ptr, jfloat x1, jfloat y1, jfloat x2, jfloat y2, jfloat radius) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->ArcTo(x1, y1, x2, y2, radius);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nClosePath(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->ClosePath();
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nPathWinding(
    JNIEnv *, jclass, jlong ptr, jint dir) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->PathWinding(dir);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nArc(
    JNIEnv *, jclass, jlong ptr, jfloat cx, jfloat cy, jfloat r, jfloat a0, jfloat a1, jint dir) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Arc(cx, cy, r, a0, a1, dir);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRect(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Rect(x, y, w, h);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRoundedRect(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h, jfloat r) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->RoundedRect(x, y, w, h, r);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRoundedRectVarying(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h, jfloat radTopLeft, jfloat radTopRight,
    jfloat radBottomRight, jfloat radBottomLeft) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->RoundedRectVarying(x, y, w, h, radTopLeft, radTopRight, radBottomRight, radBottomLeft);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRoundedRectComplex(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h, jfloat tl, jfloat tr, jfloat bl, jfloat br) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->RoundedRectComplex(x, y, w, h, tl, tr, bl, br);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nRoundedRectEllipse(
    JNIEnv *, jclass, jlong ptr, jfloat x, jfloat y, jfloat w, jfloat h, jfloat rw, jfloat rh) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->RoundedRectEllipse(x, y, w, h, rw, rh);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nEllipse(
    JNIEnv *, jclass, jlong ptr, jfloat cx, jfloat cy, jfloat rx, jfloat ry) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Ellipse(cx, cy, rx, ry);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCircle(
    JNIEnv *, jclass, jlong ptr, jfloat cx, jfloat cy, jfloat r) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Circle(cx, cy, r);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFill(JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Fill();
}

JNIEXPORT void JNICALL

Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nStroke(JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->Stroke();
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCreateFont(
    JNIEnv *env, jclass, jlong ptr, jstring name, jstring filename) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const char *filenameStr = env->GetStringUTFChars(filename, nullptr);

    int result = ctx->CreateFont(nameStr, filenameStr);

    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(filename, filenameStr);

    return result;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCreateFontAtIndex(
    JNIEnv *env, jclass, jlong ptr, jstring name, jstring filename, jint fontIndex) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const char *filenameStr = env->GetStringUTFChars(filename, nullptr);

    int result = ctx->CreateFontAtIndex(nameStr, filenameStr, fontIndex);

    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(filename, filenameStr);

    return result;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCreateFontMem(
    JNIEnv *env, jclass, jlong ptr, jstring name, jobject data, jint freeData) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *nameStr = env->GetStringUTFChars(name, nullptr);

    unsigned char *dataPtr = nullptr;
    jsize dataSize = 0;
    jbyteArray byteArray = nullptr;

    dataPtr = static_cast<unsigned char *>(env->GetDirectBufferAddress(data));
    if (dataPtr != nullptr) {
        jlong capacity = env->GetDirectBufferCapacity(data);
        if (capacity <= 0) {
            env->ReleaseStringUTFChars(name, nameStr);
            return -1;
        }
        dataSize = static_cast<jsize>(capacity);
    } else {
        byteArray = (jbyteArray) data;
        dataSize = env->GetArrayLength(byteArray);
        if (dataSize <= 0) {
            env->ReleaseStringUTFChars(name, nameStr);
            return -1;
        }
        dataPtr = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(byteArray, nullptr));
        if (dataPtr == nullptr) {
            env->ReleaseStringUTFChars(name, nameStr);
            return -1;
        }
    }

    int actualFreeData = (byteArray != nullptr) ? 0 : freeData;
    int result = ctx->CreateFontMem(nameStr, dataPtr, dataSize, actualFreeData);

    if (byteArray != nullptr) {
        env->ReleaseByteArrayElements(byteArray, reinterpret_cast<jbyte *>(dataPtr), JNI_ABORT);
    }
    env->ReleaseStringUTFChars(name, nameStr);

    return result;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nCreateFontMemAtIndex(
    JNIEnv *env, jclass, jlong ptr, jstring name, jobject data, jint freeData, jint fontIndex) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *nameStr = env->GetStringUTFChars(name, nullptr);

    unsigned char *dataPtr = nullptr;
    jsize dataSize = 0;
    jbyteArray byteArray = nullptr;

    dataPtr = static_cast<unsigned char *>(env->GetDirectBufferAddress(data));
    if (dataPtr != nullptr) {
        jlong capacity = env->GetDirectBufferCapacity(data);
        if (capacity <= 0) {
            env->ReleaseStringUTFChars(name, nameStr);
            return -1;
        }
        dataSize = static_cast<jsize>(capacity);
    } else {
        byteArray = static_cast<jbyteArray>(data);
        dataSize = env->GetArrayLength(byteArray);
        if (dataSize <= 0) {
            env->ReleaseStringUTFChars(name, nameStr);
            return -1;
        }
        dataPtr = reinterpret_cast<unsigned char *>(env->GetByteArrayElements(byteArray, nullptr));
        if (dataPtr == nullptr) {
            env->ReleaseStringUTFChars(name, nameStr);
            return -1;
        }
    }

    int actualFreeData = byteArray != nullptr ? 0 : freeData;
    int result = ctx->CreateFontMemAtIndex(nameStr, dataPtr, dataSize, actualFreeData, fontIndex);

    if (byteArray != nullptr) {
        env->ReleaseByteArrayElements(byteArray, reinterpret_cast<jbyte *>(dataPtr), JNI_ABORT);
    }
    env->ReleaseStringUTFChars(name, nameStr);

    return result;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFindFont(
    JNIEnv *env, jclass, jlong ptr, jstring name) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *nameStr = env->GetStringUTFChars(name, nullptr);

    int result = ctx->FindFont(nameStr);

    env->ReleaseStringUTFChars(name, nameStr);

    return result;
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nAddFallbackFontId(
    JNIEnv *, jclass, jlong ptr, jint baseFont, jint fallbackFont) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    return ctx->AddFallbackFontId(baseFont, fallbackFont);
}

JNIEXPORT jint JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nAddFallbackFont(
    JNIEnv *env, jclass, jlong ptr, jstring baseFont, jstring fallbackFont) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *baseFontStr = env->GetStringUTFChars(baseFont, nullptr);
    const char *fallbackFontStr = env->GetStringUTFChars(fallbackFont, nullptr);

    int result = ctx->AddFallbackFont(baseFontStr, fallbackFontStr);

    env->ReleaseStringUTFChars(baseFont, baseFontStr);
    env->ReleaseStringUTFChars(fallbackFont, fallbackFontStr);

    return result;
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nResetFallbackFontsId(
    JNIEnv *, jclass, jlong ptr, jint baseFont) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->ResetFallbackFontsId(baseFont);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nResetFallbackFonts(
    JNIEnv *env, jclass, jlong ptr, jstring baseFont) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *baseFontStr = env->GetStringUTFChars(baseFont, nullptr);

    ctx->ResetFallbackFonts(baseFontStr);

    env->ReleaseStringUTFChars(baseFont, baseFontStr);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFontSize(
    JNIEnv *, jclass, jlong ptr, jfloat size) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->FontSize(size);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFontBlur(
    JNIEnv *, jclass, jlong ptr, jfloat blur) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->FontBlur(blur);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextLetterSpacing(
    JNIEnv *, jclass, jlong ptr, jfloat spacing) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->TextLetterSpacing(spacing);
}


JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextLineHeight(
    JNIEnv *, jclass, jlong ptr, jfloat lineHeight) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->TextLineHeight(lineHeight);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextAlign(
    JNIEnv *, jclass, jlong ptr, jint align) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->TextAlign(align);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFontFaceId(
    JNIEnv *, jclass, jlong ptr, jint font) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->FontFaceId(font);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFontSetVariationAxis(
    JNIEnv *env, jclass, jlong ptr, jint font, jstring axisTag, jfloat value) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *axisTagStr = nullptr;
    if (axisTag != nullptr) {
        axisTagStr = env->GetStringUTFChars(axisTag, nullptr);
    }

    ctx->FontSetVariationAxis(static_cast<int>(font), axisTagStr, value);

    if (axisTagStr != nullptr) {
        env->ReleaseStringUTFChars(axisTag, axisTagStr);
    }
}

JNIEXPORT jobjectArray JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFontGetVariationAxis(
    JNIEnv *env, jclass, jlong ptr, jint font) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    std::vector<std::string> axes = ctx->FontGetVariationAxis(static_cast<int>(font));

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(axes.size()), stringClass, nullptr);
    for (jsize i = 0; i < static_cast<jsize>(axes.size()); ++i) {
        jstring s = env->NewStringUTF(axes[static_cast<size_t>(i)].c_str());
        env->SetObjectArrayElement(result, i, s);
        env->DeleteLocalRef(s);
    }
    return result;
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nLineStyle(
    JNIEnv *, jclass, jlong ptr, jint lineStyle) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    ctx->LineStyle(lineStyle);
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nFontFace(
    JNIEnv *env, jclass, jlong ptr, jstring font) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *fontStr = env->GetStringUTFChars(font, nullptr);

    ctx->FontFace(fontStr);

    env->ReleaseStringUTFChars(font, fontStr);
}

JNIEXPORT jlong JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextMeasureStateVersion(
    JNIEnv *, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    return static_cast<jlong>(ctx->TextMeasureStateVersion());
}

JNIEXPORT jfloat JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nText(
    JNIEnv *env, jclass, jlong ptr, jfloat x, jfloat y, jstring string) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *stringStr = env->GetStringUTFChars(string, nullptr);

    float result = ctx->Text(x, y, stringStr, nullptr);

    env->ReleaseStringUTFChars(string, stringStr);

    return result;
}

JNIEXPORT void JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextBox(
    JNIEnv *env, jclass, jlong ptr, jfloat x, jfloat y, jfloat breakRowWidth, jstring string) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *stringStr = env->GetStringUTFChars(string, nullptr);

    ctx->TextBox(x, y, breakRowWidth, stringStr, nullptr);

    env->ReleaseStringUTFChars(string, stringStr);
}

JNIEXPORT jobject JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextBounds(
    JNIEnv *env, jclass, jlong ptr, jfloat x, jfloat y, jstring string) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *stringStr = env->GetStringUTFChars(string, nullptr);

    TextBoundsResult result = ctx->TextBounds(x, y, stringStr, nullptr);

    env->ReleaseStringUTFChars(string, stringStr);

    jclass resultClass = env->FindClass("io/homo/superresolution/thirdparty/nanovg/TextBoundsResult");
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(F[F)V");

    jfloatArray boundsArray = env->NewFloatArray(4);
    env->SetFloatArrayRegion(boundsArray, 0, 4, result.bounds.data());

    return env->NewObject(resultClass, constructor, result.advance, boundsArray);
}

JNIEXPORT jfloatArray JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextBoxBounds(
    JNIEnv *env, jclass, jlong ptr, jfloat x, jfloat y, jfloat breakRowWidth, jstring string) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *stringStr = env->GetStringUTFChars(string, nullptr);

    std::array<float, 4> bounds = ctx->TextBoxBounds(x, y, breakRowWidth, stringStr, nullptr);

    env->ReleaseStringUTFChars(string, stringStr);

    jfloatArray result = env->NewFloatArray(4);
    env->SetFloatArrayRegion(result, 0, 4, bounds.data());
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextGlyphPositions(
    JNIEnv *env, jclass, jlong ptr, jfloat x, jfloat y, jstring string) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *stringStr = env->GetStringUTFChars(string, nullptr);

    std::vector<NVGglyphPosition> positions = ctx->TextGlyphPositions(x, y, stringStr, nullptr);

    env->ReleaseStringUTFChars(string, stringStr);

    jclass posClass = env->FindClass("io/homo/superresolution/thirdparty/nanovg/NVGglyphPosition");
    jobjectArray result = env->NewObjectArray(positions.size(), posClass, nullptr);

    jmethodID constructor = env->GetMethodID(posClass, "<init>", "(Ljava/lang/String;FFF)V");

    for (size_t i = 0; i < positions.size(); i++) {
        jstring strObj = env->NewStringUTF(positions[i].str);
        jobject posObj = env->NewObject(posClass, constructor, strObj,
                                        positions[i].x, positions[i].minx, positions[i].maxx);
        env->SetObjectArrayElement(result, i, posObj);
        env->DeleteLocalRef(strObj);
        env->DeleteLocalRef(posObj);
    }

    return result;
}

JNIEXPORT jobject JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextMetrics(
    JNIEnv *env, jclass, jlong ptr) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    TextMetricsResult result = ctx->TextMetrics();

    jclass resultClass = env->FindClass("io/homo/superresolution/thirdparty/nanovg/TextMetricsResult");
    jmethodID constructor = env->GetMethodID(resultClass, "<init>", "(FFF)V");

    return env->NewObject(resultClass, constructor, result.ascender, result.descender, result.lineHeight);
}

JNIEXPORT jobjectArray JNICALL Java_io_homo_superresolution_thirdparty_nanovg_NanoVGContext_nTextBreakLines(
    JNIEnv *env, jclass, jlong ptr, jstring string, jfloat breakRowWidth) {
    auto *ctx = reinterpret_cast<NanoVGContext *>(ptr);
    const char *stringStr = env->GetStringUTFChars(string, nullptr);
    std::string cppStr(stringStr);
    std::vector<NVGtextRow> rows = ctx->TextBreakLines(cppStr, nullptr, breakRowWidth);
    env->ReleaseStringUTFChars(string, stringStr);

    jclass rowClass = env->FindClass("io/homo/superresolution/thirdparty/nanovg/NVGtextRow");
    jobjectArray result = env->NewObjectArray(rows.size(), rowClass, nullptr);

    jmethodID constructor = env->GetMethodID(rowClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;FFF)V");

    for (size_t i = 0; i < rows.size(); i++) {
        jstring startObj = env->NewStringUTF(rows[i].start);
        jstring endObj = env->NewStringUTF(rows[i].end);

        jobject rowObj = env->NewObject(rowClass, constructor, startObj, endObj, rows[i].width, rows[i].minx,
                                        rows[i].maxx);
        env->SetObjectArrayElement(result, i, rowObj);

        env->DeleteLocalRef(startObj);
        env->DeleteLocalRef(endObj);
        env->DeleteLocalRef(rowObj);
    }

    return result;
}
