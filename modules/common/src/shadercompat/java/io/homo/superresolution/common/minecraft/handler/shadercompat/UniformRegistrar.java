package io.homo.superresolution.common.minecraft.handler.shadercompat;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.function.Supplier;

public interface UniformRegistrar {
    void uniform1f(String name, Supplier<Float> value);
    void uniform2f(String name, Supplier<Vector2f> value);
    void uniform1i(String name, Supplier<Integer> value);
    void uniform2i(String name, Supplier<Vector2i> value);
}
