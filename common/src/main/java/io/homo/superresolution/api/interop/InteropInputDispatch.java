package io.homo.superresolution.api.interop;

import io.homo.superresolution.common.upscale.DispatchResource;

import java.util.Map;
import java.util.function.Consumer;

/** Optional capability exposed by algorithms that accept externally-produced interop inputs. */
public interface InteropInputDispatch {
    Map<InteropResourceType, InteropResourceDescription> getInteropResourceDescriptions();

    boolean dispatchWithInputWriter(
            DispatchResource dispatchResource,
            Consumer<InteropInputWriter> writer);
}
