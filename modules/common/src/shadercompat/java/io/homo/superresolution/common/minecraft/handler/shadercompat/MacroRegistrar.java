package io.homo.superresolution.common.minecraft.handler.shadercompat;

@FunctionalInterface
public interface MacroRegistrar {
    void registerMacro(String name, String value);
}
