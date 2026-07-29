#include <jni.h>

#include "d3d12_interop_internal.h"

#if defined(ON_WIN64)

#include <cstdint>

namespace {

using sr::d3d12::D3D12InteropContext;
using sr::d3d12::SharedTexture;

// Converts the opaque pointer-sized handle stored by Java back into its native
// context. A zero jlong naturally becomes a null pointer.
D3D12InteropContext *fromHandle(jlong handle) {
  return reinterpret_cast<D3D12InteropContext *>(static_cast<intptr_t>(handle));
}

SharedTexture *getResource(D3D12InteropContext *context, jint index) {
  if (!context || index < 0 ||
      static_cast<uint32_t>(index) >= sr::d3d12::RESOURCE_COUNT) {
    sr::d3d12::setError("Invalid D3D12 interop resource index.");
    return nullptr;
  }
  return &context->resources[static_cast<uint32_t>(index)];
}

// Centralizes pointer/HANDLE conversion at the JNI boundary.
template <typename T> jlong toHandle(T *pointer) {
  return static_cast<jlong>(reinterpret_cast<intptr_t>(pointer));
}

} // namespace

extern "C" {

/**
 * Creates a D3D12 context on the adapter used by OpenGL.
 *
 * The returned pointer is opaque to Java. Zero indicates failure, with the
 * reason available through Nd3d12GetLastError.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12CreateContext(
    JNIEnv *, jclass, jlong adapterLuid, jint renderWidth, jint renderHeight,
    jint outputWidth, jint outputHeight, jint colorFormat) {
  if (adapterLuid == 0 || renderWidth <= 0 || renderHeight <= 0 ||
      outputWidth <= 0 || outputHeight <= 0) {
    sr::d3d12::clearError();
    sr::d3d12::setError(
        "Invalid D3D12 interop context dimensions or adapter LUID.");
    return 0;
  }

  auto context = sr::d3d12::createContext(
      static_cast<uint64_t>(adapterLuid), static_cast<uint32_t>(renderWidth),
      static_cast<uint32_t>(renderHeight), static_cast<uint32_t>(outputWidth),
      static_cast<uint32_t>(outputHeight), colorFormat);
  return context ? toHandle(context.release()) : 0;
}

/**
 * Waits for outstanding GPU work and destroys a context. A null handle is a
 * no-op.
 */
JNIEXPORT void JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12DestroyContext(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  if (!context) {
    return;
  }
  sr::d3d12::waitForFence(context, context->lastSubmittedFenceValue);
  delete context;
}

/**
 * Returns the borrowed ID3D12Device pointer used by the FidelityFX backend.
 * Its lifetime remains tied to the context.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetDevice(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  return context ? toHandle(context->device.get()) : 0;
}

/**
 * Returns the borrowed command list into which Java records upscaling work
 * between Nd3d12BeginFrame and Nd3d12ExecuteFrame.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetCommandList(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  return context ? toHandle(context->commandList.get()) : 0;
}

/**
 * Returns the borrowed ID3D12Resource at the Java-defined resource index.
 * Invalid handles or indices return zero.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResource(
    JNIEnv *, jclass, jlong contextHandle, jint index) {
  SharedTexture *texture = getResource(fromHandle(contextHandle), index);
  return texture ? toHandle(texture->resource.get()) : 0;
}

/**
 * Returns the texture's exported NT handle for OpenGL memory-object import.
 * Ownership remains with the context.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResourceSharedHandle(
    JNIEnv *, jclass, jlong contextHandle, jint index) {
  SharedTexture *texture = getResource(fromHandle(contextHandle), index);
  return texture ? toHandle(texture->sharedHandle.get()) : 0;
}

/**
 * Returns the driver-reported allocation size needed when OpenGL imports the
 * shared texture memory.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetResourceAllocationSize(
    JNIEnv *, jclass, jlong contextHandle, jint index) {
  SharedTexture *texture = getResource(fromHandle(contextHandle), index);
  return texture ? static_cast<jlong>(texture->allocationSize) : 0;
}

/**
 * Returns the shared fence's exported NT handle for OpenGL semaphore import.
 * Ownership remains with the context.
 */
JNIEXPORT jlong JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetFenceSharedHandle(
    JNIEnv *, jclass, jlong contextHandle) {
  D3D12InteropContext *context = fromHandle(contextHandle);
  return context ? toHandle(context->fenceSharedHandle.get()) : 0;
}

/**
 * Prepares the command list for a frame and queues a GPU wait for OpenGL's
 * input-ready fence value.
 */
JNIEXPORT jint JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12BeginFrame(
    JNIEnv *, jclass, jlong contextHandle, jlong waitFenceValue) {
  if (waitFenceValue <= 0) {
    sr::d3d12::setError("Invalid D3D12 begin-frame arguments.");
    return E_INVALIDARG;
  }
  return static_cast<jint>(sr::d3d12::beginFrame(
      fromHandle(contextHandle), static_cast<uint64_t>(waitFenceValue)));
}

/**
 * Submits the recorded command list and signals the output-ready fence value
 * consumed by OpenGL.
 */
JNIEXPORT jint JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12ExecuteFrame(
    JNIEnv *, jclass, jlong contextHandle, jlong signalFenceValue) {
  if (signalFenceValue <= 0) {
    sr::d3d12::setError("Invalid D3D12 execute-frame state or fence value.");
    return E_INVALIDARG;
  }
  return static_cast<jint>(sr::d3d12::executeFrame(
      fromHandle(contextHandle), static_cast<uint64_t>(signalFenceValue)));
}

/**
 * Blocks until the latest submitted D3D12 frame completes.
 */
JNIEXPORT jint JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12WaitIdle(
    JNIEnv *, jclass, jlong contextHandle) {
  return static_cast<jint>(sr::d3d12::waitIdle(fromHandle(contextHandle)));
}

/**
 * Returns the thread-local diagnostic recorded by the most recent failed
 * interop operation on this thread.
 */
JNIEXPORT jstring JNICALL
Java_io_homo_superresolution_core_graphics_d3d12_D3D12InteropNative_Nd3d12GetLastError(
    JNIEnv *env, jclass) {
  return env->NewStringUTF(sr::d3d12::lastError().c_str());
}

} // extern "C"

#endif
