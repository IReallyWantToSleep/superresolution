package io.homo.superresolution.common.minecraft.handler.shadercompat;

import io.homo.superresolution.api.AbstractAlgorithm;
import io.homo.superresolution.api.registry.AlgorithmDescription;
import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;
import io.homo.superresolution.core.graphics.impl.texture.ITexture;
import org.joml.Vector2f;

public interface SRCompatProcessor {
    int version();

    boolean needsPreProcessColor(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    boolean needsPreProcessDepth(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    boolean needsPreProcessMotionVectors(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    boolean needsPreProcessExposure(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    boolean needsAdaptJitter(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    boolean needsAdaptPreExposure(SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);

    Vector2f adaptJitterForAlgorithm(Vector2f rawJitter, AbstractAlgorithm algorithm, SRShaderCompatData config, AlgorithmDescription<?> description);
    Vector2f adaptJitterForShaderpack(Vector2f rawJitter, AbstractAlgorithm algorithm, SRShaderCompatData config, AlgorithmDescription<?> description);

    void preProcessColor(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                         SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    void preProcessDepth(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                         SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    void preProcessMotionVectors(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                                 SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    void preProcessExposure(ITexture input, ITexture output, ICommandBuffer commandBuffer,
                            SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);

    float adaptPreExposureForAlgorithm(float rawExposure, AbstractAlgorithm algorithm, SRShaderCompatData config, AlgorithmDescription<?> description);

    void registerMacros(MacroRegistrar registrar, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
    void registerUniforms(UniformRegistrar registrar, SRShaderCompatData config, AbstractAlgorithm algorithm, AlgorithmDescription<?> description);
}
