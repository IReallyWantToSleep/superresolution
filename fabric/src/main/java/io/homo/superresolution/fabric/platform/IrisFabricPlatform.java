package io.homo.superresolution.fabric.platform;

import io.homo.superresolution.common.platform.IrisPlatform;
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisFabricPlatform extends IrisPlatform {
    @Override
    public boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
