package io.homo.superresolution.forge.platform;

import io.homo.superresolution.common.platform.IrisPlatform;

import java.lang.reflect.Method;

public class IrisForgePlatform extends IrisPlatform {
    @Override
    public boolean isShaderPackInUse() {
        try{
            Class<?> irisApiClazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstanceMethod = irisApiClazz.getMethod("getInstance");
            Object irisApiInstance = getInstanceMethod.invoke(null);
            Method isShaderPackInUseMethod = irisApiInstance.getClass().getMethod("isShaderPackInUse");
            return (boolean) isShaderPackInUseMethod.invoke(irisApiInstance);
        } catch (Exception e) {
            return false;
        }
    }
}
