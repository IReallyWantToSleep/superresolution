package io.homo.superresolution.core.glslang;

import com.destranix.glslang.*;
import oiiaio.fsr.NativeLibManager;

import java.util.EnumSet;

import static com.destranix.glslang.Main.InitializeProcess;

public class ShaderCompiler {

    public static final int[] defaultTBuiltInResourceValues = {
            /* .MaxLights = */ 32,
            /* .MaxClipPlanes = */ 6,
            /* .MaxTextureUnits = */ 32,
            /* .MaxTextureCoords = */ 32,
            /* .MaxVertexAttribs = */ 64,
            /* .MaxVertexUniformComponents = */ 4096,
            /* .MaxVaryingFloats = */ 64,
            /* .MaxVertexTextureImageUnits = */ 32,
            /* .MaxCombinedTextureImageUnits = */ 80,
            /* .MaxTextureImageUnits = */ 32,
            /* .MaxFragmentUniformComponents = */ 4096,
            /* .MaxDrawBuffers = */ 32,
            /* .MaxVertexUniformVectors = */ 128,
            /* .MaxVaryingVectors = */ 8,
            /* .MaxFragmentUniformVectors = */ 16,
            /* .MaxVertexOutputVectors = */ 16,
            /* .MaxFragmentInputVectors = */ 15,
            /* .MinProgramTexelOffset = */ -8,
            /* .MaxProgramTexelOffset = */ 7,
            /* .MaxClipDistances = */ 8,
            /* .MaxComputeWorkGroupCountX = */ 65535,
            /* .MaxComputeWorkGroupCountY = */ 65535,
            /* .MaxComputeWorkGroupCountZ = */ 65535,
            /* .MaxComputeWorkGroupSizeX = */ 1024,
            /* .MaxComputeWorkGroupSizeY = */ 1024,
            /* .MaxComputeWorkGroupSizeZ = */ 64,
            /* .MaxComputeUniformComponents = */ 1024,
            /* .MaxComputeTextureImageUnits = */ 16,
            /* .MaxComputeImageUniforms = */ 8,
            /* .MaxComputeAtomicCounters = */ 8,
            /* .MaxComputeAtomicCounterBuffers = */ 1,
            /* .MaxVaryingComponents = */ 60,
            /* .MaxVertexOutputComponents = */ 64,
            /* .MaxGeometryInputComponents = */ 64,
            /* .MaxGeometryOutputComponents = */ 128,
            /* .MaxFragmentInputComponents = */ 128,
            /* .MaxImageUnits = */ 8,
            /* .MaxCombinedImageUnitsAndFragmentOutputs = */ 8,
            /* .MaxCombinedShaderOutputResources = */ 8,
            /* .MaxImageSamples = */ 0,
            /* .MaxVertexImageUniforms = */ 0,
            /* .MaxTessControlImageUniforms = */ 0,
            /* .MaxTessEvaluationImageUniforms = */ 0,
            /* .MaxGeometryImageUniforms = */ 0,
            /* .MaxFragmentImageUniforms = */ 8,
            /* .MaxCombinedImageUniforms = */ 8,
            /* .MaxGeometryTextureImageUnits = */ 16,
            /* .MaxGeometryOutputVertices = */ 256,
            /* .MaxGeometryTotalOutputComponents = */ 1024,
            /* .MaxGeometryUniformComponents = */ 1024,
            /* .MaxGeometryVaryingComponents = */ 64,
            /* .MaxTessControlInputComponents = */ 128,
            /* .MaxTessControlOutputComponents = */ 128,
            /* .MaxTessControlTextureImageUnits = */ 16,
            /* .MaxTessControlUniformComponents = */ 1024,
            /* .MaxTessControlTotalOutputComponents = */ 4096,
            /* .MaxTessEvaluationInputComponents = */ 128,
            /* .MaxTessEvaluationOutputComponents = */ 128,
            /* .MaxTessEvaluationTextureImageUnits = */ 16,
            /* .MaxTessEvaluationUniformComponents = */ 1024,
            /* .MaxTessPatchComponents = */ 120,
            /* .MaxPatchVertices = */ 32,
            /* .MaxTessGenLevel = */ 64,
            /* .MaxViewports = */ 16,
            /* .MaxVertexAtomicCounters = */ 0,
            /* .MaxTessControlAtomicCounters = */ 0,
            /* .MaxTessEvaluationAtomicCounters = */ 0,
            /* .MaxGeometryAtomicCounters = */ 0,
            /* .MaxFragmentAtomicCounters = */ 8,
            /* .MaxCombinedAtomicCounters = */ 8,
            /* .MaxAtomicCounterBindings = */ 1,
            /* .MaxVertexAtomicCounterBuffers = */ 0,
            /* .MaxTessControlAtomicCounterBuffers = */ 0,
            /* .MaxTessEvaluationAtomicCounterBuffers = */ 0,
            /* .MaxGeometryAtomicCounterBuffers = */ 0,
            /* .MaxFragmentAtomicCounterBuffers = */ 1,
            /* .MaxCombinedAtomicCounterBuffers = */ 1,
            /* .MaxAtomicCounterBufferSize = */ 16384,
            /* .MaxTransformFeedbackBuffers = */ 4,
            /* .MaxTransformFeedbackInterleavedComponents = */ 64,
            /* .MaxCullDistances = */ 8,
            /* .MaxCombinedClipAndCullDistances = */ 8,
            /* .MaxSamples = */ 4,
            /* .maxMeshOutputVerticesNV = */ 256,
            /* .maxMeshOutputPrimitivesNV = */ 512,
            /* .maxMeshWorkGroupSizeX_NV = */ 32,
            /* .maxMeshWorkGroupSizeY_NV = */ 1,
            /* .maxMeshWorkGroupSizeZ_NV = */ 1,
            /* .maxTaskWorkGroupSizeX_NV = */ 32,
            /* .maxTaskWorkGroupSizeY_NV = */ 1,
            /* .maxTaskWorkGroupSizeZ_NV = */ 1,
            /* .maxMeshViewCountNV = */ 4,
            /* .maxDualSourceDrawBuffersEXT = */ 1
    };
    public static final boolean[] defaultTLimitsValues = {
            /* .nonInductiveForLoops = */ true,
            /* .whileLoops = */ true,
            /* .doWhileLoops = */ true,
            /* .generalUniformIndexing = */ true,
            /* .generalAttributeMatrixVectorIndexing = */ true,
            /* .generalVaryingIndexing = */ true,
            /* .generalSamplerIndexing = */ true,
            /* .generalVariableIndexing = */ true,
            /* .generalConstantMatrixVectorIndexing = */ true
    };

    public static void init() {
        System.out.println(Global.GetSpirvGeneratorVersion());
        InitializeProcess();
    }

    public static void main(String[] args) {
        if (!NativeLibManager.check("I:\\super_resolution_moddev\\superresolution\\run")) {
            NativeLibManager.extract("I:\\super_resolution_moddev\\superresolution\\run");
        }
        NativeLibManager.load("I:\\super_resolution_moddev\\superresolution\\run");
        init();
        TShader shaderVertex = new TShader(EShLanguage.EShLangVertex);
        TShader shaderFragment = new TShader(EShLanguage.EShLangFragment);
        shaderVertex.setStrings(new String[]{
                """
                #version 330
                precision mediump float;
                
                layout (location = 0) in vec2 aPosition;
                layout (location = 1) in vec2 aTexCoord;
                out vec2 vTexCoord;
                void main() {
                    vTexCoord = aTexCoord;
                    gl_Position = vec4(aPosition, 0.0, 1.0);
                }
                """
        });
        shaderFragment.setStrings(new String[]{
                """
                #version 330
                precision mediump float;
                
                uniform sampler2D uTexture;
                in vec2 vTexCoord;
                out vec4 FragColor;
                void main() {
                    FragColor = texture(uTexture, vTexCoord);
                }
                """
        });
        shaderVertex.setEnvInput(EShSource.EShSourceGlsl, EShLanguage.EShLangVertex, EShClient.EShClientOpenGL, 450);
        shaderFragment.setEnvInput(EShSource.EShSourceGlsl, EShLanguage.EShLangFragment, EShClient.EShClientOpenGL, 450);
        shaderVertex.setEnvClient(EShClient.EShClientOpenGL, EShTargetClientVersion.EShTargetOpenGL_450);
        shaderFragment.setEnvClient(EShClient.EShClientOpenGL, EShTargetClientVersion.EShTargetOpenGL_450);

        TLimits limits = new TLimits(defaultTLimitsValues);
        TBuiltInResource resources = new TBuiltInResource(defaultTBuiltInResourceValues, limits);
        if (!shaderVertex.parse(resources, 450, true, EnumSet.of(EShMessages.EShMsgDefault))) {
            throw new AssertionError("Could not parse vertex shader!\r\n" + "Vertex Debuglog:\r\n" + shaderVertex.getInfoLog());
        }
        System.out.println("Vertex Infolog:\r\n" + shaderVertex.getInfoLog());
        if (!shaderFragment.parse(resources, 450, true, EnumSet.of(EShMessages.EShMsgDefault))) {
            throw new AssertionError("Could not parse fragment shader!\r\n" + "Fragment Debuglog:\r\n" + shaderFragment.getInfoLog());
        }
        System.out.println("Fragment Infolog:\r\n" + shaderFragment.getInfoLog());
        String[] strings = new String[]{""};
        shaderVertex.preprocess(
                resources,
                450,
                EProfile.ENoProfile,
                false,
                true,
                EnumSet.of(EShMessages.EShMsgDefault),
                strings,
                new TShader.ForbidIncluder()
        );
        System.out.println(strings[0]);

    }
}
