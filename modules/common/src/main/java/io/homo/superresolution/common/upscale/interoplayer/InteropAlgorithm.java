package io.homo.superresolution.common.upscale.interoplayer;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.InitializationDescription;
import io.homo.superresolution.api.interop.InteropResourceDescription;
import io.homo.superresolution.api.interop.InteropResourceRequirement;
import io.homo.superresolution.api.interop.InteropResourceType;
import io.homo.superresolution.common.config.SuperResolutionConfig;
import io.homo.superresolution.common.upscale.DispatchResource;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.srapi.SRRenderApiType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static io.homo.superresolution.api.interop.InteropResourceRequirement.FormatSource.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.Presence.*;
import static io.homo.superresolution.api.interop.InteropResourceRequirement.SizeSource.*;
import static io.homo.superresolution.api.interop.InteropResourceType.*;

public abstract class InteropAlgorithm<C extends InteropCommandContext> extends AbstractAlgorithm {
    private InteropImplementation<C> interopImplementation;
    private InteropOutputTexture publishedOutput;
    private SRRenderApiType inputApi;
    private boolean initialized;
    private boolean destroyed;
    private boolean contextOwned;
    private boolean contextReady;
    private boolean creatingContext;
    private boolean operationInProgress;

    protected abstract SRRenderApiType executionApi();

    protected abstract InteropImplementation<C> createInteropImplementation(SRRenderApiType inputApi);

    protected abstract void createAlgorithmContext(InteropResourceAccess resources);

    protected abstract void destroyAlgorithmContext();

    protected abstract boolean dispatchAlgorithm(InteropDispatchResource<C> resource);

    protected List<InteropResourceRequirement> getInteropResourceRequirements() {
        return List.of(
                InteropResourceRequirement.input(
                        Color, Required, RenderSize, InternalColorConfigOrContext,
                        SuperResolutionConfig.getInternalTextureFormat()),
                InteropResourceRequirement.input(
                        Depth, Required, RenderSize, Fixed, TextureFormat.R32F),
                InteropResourceRequirement.input(
                        MotionVectors, Required, RenderSize, Context, TextureFormat.RG16F),
                InteropResourceRequirement.input(
                        Exposure, Optional, OneByOne, Fixed, TextureFormat.R32F),
                InteropResourceRequirement.output(
                        OutputColor, OutputSize, InternalColorConfig, null)
        );
    }

    protected boolean validateExecutionResources(InteropResourceSet resources) {
        return true;
    }

    protected final void executeInteropCommands(Consumer<C> recorder) {
        if (!creatingContext) {
            throw new IllegalStateException("Provider setup recording is only available during context creation");
        }
        interopImplementation.executeCommands(Objects.requireNonNull(recorder, "recorder"));
    }

    protected final SRRenderApiType inputApi() {
        return inputApi;
    }

    public final boolean supportsInputCapture() {
        return interopImplementation instanceof VulkanInputCaptureInterop;
    }

    @Override
    public final void initialize(InitializationDescription description) {
        beginOperation();
        try {
            if (initialized || interopImplementation != null) {
                throw new IllegalStateException("Interop algorithm still owns resources");
            }
            initDesc = Objects.requireNonNull(description, "description");
            inputApi = SRRenderApiType.OPENGL;
            destroyed = false;
            interopImplementation = Objects.requireNonNull(
                    createInteropImplementation(inputApi),
                    "Interop implementation factory returned null");
            try {
                if (interopImplementation.inputApi() != inputApi
                        || interopImplementation.executionApi() != executionApi()) {
                    throw new IllegalStateException("Interop implementation API pair does not match algorithm");
                }
                interopImplementation.initialize(description, getInteropResourceRequirements());
                createContext();
                initialized = true;
                invalidateHistory();
            } catch (RuntimeException | Error failure) {
                cleanupFailedInitialization(failure);
                throw failure;
            }
        } finally {
            operationInProgress = false;
        }
    }

    @Override
    public final boolean dispatch(DispatchResource resource) {
        beginOperation();
        try {
            requireInitialized();
            if (resource == null || resource.inputApi() != inputApi) {
                throw new IllegalArgumentException(
                        "Dispatch input API does not match the initialized interop API");
            }
            if (!interopImplementation.validateInputResources(resource)) {
                invalidateHistory();
                return false;
            }
            if (!contextReady || interopImplementation.resourcesNeedRebuild(resource)) {
                replaceResources(() -> interopImplementation.rebuildForDispatch(resource));
            }
            if (!validateExecutionResources(interopImplementation.resourceAccess().resources())) {
                invalidateHistory();
                return false;
            }

            InteropDispatchResource<C> prepared = Objects.requireNonNull(
                    interopImplementation.beginDispatch(resource), "Prepared interop dispatch");
            boolean success = false;
            Throwable dispatchFailure = null;
            try {
                if (prepared.commandContext().api() != executionApi()) {
                    throw new IllegalStateException("Dispatch command context API does not match algorithm");
                }
                success = dispatchAlgorithm(prepared);
            } catch (RuntimeException | Error failure) {
                dispatchFailure = failure;
                throw failure;
            } finally {
                try {
                    interopImplementation.completeDispatch(prepared, success);
                } catch (RuntimeException | Error completionFailure) {
                    success = false;
                    if (dispatchFailure != null) {
                        dispatchFailure.addSuppressed(completionFailure);
                    } else {
                        throw completionFailure;
                    }
                } finally {
                    if (!success) {
                        invalidateHistory();
                    }
                }
            }
            if (success) {
                publishedOutput = new InteropOutputTexture(
                        prepared.outputTexture(),
                        Objects.requireNonNull(interopImplementation.outputTexture(), "Completed source output"));
            }
            return success;
        } catch (RuntimeException | Error failure) {
            invalidateHistory();
            throw failure;
        } finally {
            operationInProgress = false;
        }
    }

    @Override
    public final void resize(int width, int height) {
        beginOperation();
        try {
            requireInitialized();
            if (width < 1 || height < 1) {
                throw new IllegalArgumentException("Output dimensions must be positive");
            }
            replaceResources(() -> interopImplementation.resize(width, height));
        } finally {
            operationInProgress = false;
        }
    }

    private void replaceResources(Runnable rebuild) {
        interopImplementation.awaitUsers();
        destroyContext();
        publishedOutput = null;
        invalidateHistory();
        rebuild.run();
        try {
            createContext();
        } catch (RuntimeException | Error failure) {
            try {
                destroyContext();
            } catch (RuntimeException | Error cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private void createContext() {
        contextReady = false;
        contextOwned = true;
        creatingContext = true;
        try {
            createAlgorithmContext(interopImplementation.resourceAccess());
            contextReady = true;
        } finally {
            creatingContext = false;
        }
    }

    private void destroyContext() {
        contextReady = false;
        if (contextOwned) {
            destroyAlgorithmContext();
            contextOwned = false;
        }
    }

    private void cleanupFailedInitialization(Throwable failure) {
        try {
            interopImplementation.awaitUsers();
            destroyContext();
            interopImplementation.destroy();
            interopImplementation = null;
            destroyed = true;
        } catch (RuntimeException | Error cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
        publishedOutput = null;
        initialized = false;
    }

    @Override
    public final void destroy() {
        beginOperation();
        try {
            if (destroyed && interopImplementation == null) {
                return;
            }
            if (interopImplementation != null) {
                interopImplementation.awaitUsers();
                destroyContext();
                publishedOutput = null;
                interopImplementation.destroy();
                interopImplementation = null;
            }
            initialized = false;
            destroyed = true;
            publishedOutput = null;
        } finally {
            operationInProgress = false;
        }
    }

    public final Map<InteropResourceType, InteropResourceDescription> getInteropResourceDescriptions() {
        return interopImplementation == null ? Map.of() : interopImplementation.resourceDescriptions();
    }

    @Override
    public final ITexture getOutputTexture() {
        return publishedOutput == null ? null : publishedOutput.sourceView();
    }

    private void requireInitialized() {
        if (!initialized || destroyed || interopImplementation == null) {
            throw new IllegalStateException("Interop algorithm is uninitialized or destroyed");
        }
    }

    private void beginOperation() {
        if (operationInProgress) {
            throw new IllegalStateException("Interop lifecycle and dispatch calls cannot be reentrant");
        }
        operationInProgress = true;
    }
}
