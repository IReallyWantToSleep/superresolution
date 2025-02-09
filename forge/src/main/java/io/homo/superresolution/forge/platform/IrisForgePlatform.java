package io.homo.superresolution.forge.platform;

import io.homo.superresolution.common.platform.IrisPlatform;
import net.irisshaders.iris.api.v0.IrisApi;

public class IrisForgePlatform extends IrisPlatform {
    @Override
    public boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
