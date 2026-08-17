#include "d3d12_interop_internal.h"

#if defined(ON_WIN64)

namespace sr::d3d12 {
    /**
     * Blocks the calling CPU thread until the shared fence reaches value.
     *
     * A null context, zero value, or already-completed value needs no wait. Other
     * cases arm the context's reusable event and wait indefinitely for D3D12 to
     * signal it.
     */
    bool waitForFence(D3D12InteropContext *context, uint64_t value) {
        if (!context || value == 0 || context->fence->GetCompletedValue() >= value) {
            return true;
        }

        const HRESULT hr =
                context->fence->SetEventOnCompletion(value, context->fenceEvent.get());
        if (FAILED(hr)) {
            setHresultError("ID3D12Fence::SetEventOnCompletion", hr);
            return false;
        }
        if (WaitForSingleObject(context->fenceEvent.get(), INFINITE) !=
            WAIT_OBJECT_0) {
            setError("Waiting for the D3D12 interop fence failed.");
            return false;
        }
        return true;
    }

    /**
     * Starts recording a frame.
     *
     * The CPU first waits for the previous submission so the allocator can be
     * reset safely. The D3D12 queue then waits for the value signaled by OpenGL,
     * ensuring that imported input textures are ready before upscaling commands
     * execute.
     */
    HRESULT beginFrame(D3D12InteropContext *context, uint64_t waitFenceValue) {
        if (!context || waitFenceValue == 0) {
            setError("Invalid D3D12 begin-frame arguments.");
            return E_INVALIDARG;
        }
        if (context->frameState == FrameState::Recording) {
            setError("The D3D12 command list is already recording.");
            return E_FAIL;
        }
        if (!waitForFence(context, context->lastSubmittedFenceValue)) {
            return E_FAIL;
        }

        HRESULT hr = context->commandAllocator->Reset();
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandAllocator::Reset", hr);
            return hr;
        }
        hr = context->commandList->Reset(context->commandAllocator.get(), nullptr);
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Reset", hr);
            return hr;
        }
        hr = context->queue->Wait(context->fence.get(), waitFenceValue);
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandQueue::Wait", hr);
            context->commandList->Close();
            return hr;
        }

        context->frameState = FrameState::Recording;
        return S_OK;
    }

    /**
     * Ends and submits a frame.
     *
     * Closing the command list transitions the context back to Idle. After
     * submission, the queue signals the shared fence value that OpenGL will wait
     * on before consuming the output texture.
     */
    HRESULT executeFrame(D3D12InteropContext *context, uint64_t signalFenceValue) {
        if (!context || context->frameState != FrameState::Recording ||
            signalFenceValue == 0) {
            setError("Invalid D3D12 execute-frame state or fence value.");
            return E_INVALIDARG;
        }

        HRESULT hr = context->commandList->Close();
        context->frameState = FrameState::Idle;
        if (FAILED(hr)) {
            setHresultError("ID3D12GraphicsCommandList::Close", hr);
            return hr;
        }

        ID3D12CommandList *commandLists[] = {context->commandList.get()};
        context->queue->ExecuteCommandLists(1, commandLists);
        hr = context->queue->Signal(context->fence.get(), signalFenceValue);
        if (FAILED(hr)) {
            setHresultError("ID3D12CommandQueue::Signal", hr);
            return hr;
        }

        context->lastSubmittedFenceValue = signalFenceValue;
        return S_OK;
    }

    /**
     * Waits until the most recently submitted frame has completed.
     */
    HRESULT waitIdle(D3D12InteropContext *context) {
        if (!context) {
            setError("The D3D12 interop context is null.");
            return E_INVALIDARG;
        }
        return waitForFence(context, context->lastSubmittedFenceValue)
                   ? S_OK
                   : E_FAIL;
    }
} // namespace sr::d3d12

#endif
