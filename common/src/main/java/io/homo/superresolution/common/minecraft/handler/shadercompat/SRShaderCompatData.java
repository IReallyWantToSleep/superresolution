/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.minecraft.handler.shadercompat;


import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SRShaderCompatData {
    private final Map<String, WorldProfile> worldProfiles;
    private final WorldProfile defaultProfile;
    public final int version;

    public SRShaderCompatData(int version, Map<String, WorldProfile> worldProfiles, WorldProfile defaultProfile) {
        this.version = version;
        this.worldProfiles = worldProfiles != null ? worldProfiles : Collections.emptyMap();
        this.defaultProfile = defaultProfile;
    }

    public @Nullable WorldProfile getProfileForWorld(String worldName) {
        if (worldProfiles.containsKey(worldName)) {
            return worldProfiles.get(worldName);
        }
        return defaultProfile;
    }


    public static class WorldProfile {
        public final boolean enabled;
        public final UpscaleConfig upscale;
        public final JitterConfig jitter;

        public WorldProfile(boolean enabled, UpscaleConfig upscale, JitterConfig jitter) {
            this.enabled = enabled;
            this.upscale = upscale;
            this.jitter = jitter;
        }
    }

    public static class UpscaleConfig {
        public final boolean enabled;
        public final PipelineTrigger trigger;
        public final TextureFormat internalFormat;
        public final Map<String, InputTexture> inputTextures;
        public final Map<String, OutputTexture> outputTextures;
        public final @Nullable SourceConfig preExposure;
        public final boolean isHdrInput;
        public final boolean isAutoExposure;
        public final boolean isMotionJittered;


        public UpscaleConfig(boolean enabled, PipelineTrigger trigger, TextureFormat internalFormat,
                             Map<String, InputTexture> inputTextures, Map<String, OutputTexture> outputTextures,
                             @Nullable SourceConfig preExposure,
                             boolean isHdrInput,boolean isAutoExposure,boolean isMotionJittered) {
            this.enabled = enabled;
            this.trigger = trigger;
            this.internalFormat = internalFormat;
            this.inputTextures = inputTextures;
            this.outputTextures = outputTextures;
            this.preExposure = preExposure;
            this.isHdrInput = isHdrInput;
            this.isAutoExposure = isAutoExposure;
            this.isMotionJittered = isMotionJittered;
        }
    }

    public static class PipelineTrigger {
        public enum Order {
            BEFORE,
            AFTER
        }

        public final Order order;
        public final String passName;

        public PipelineTrigger(Order order, String passName) {
            this.order = order;
            this.passName = passName;
        }
    }

    public static class JitterConfig {
        public final boolean enabled;
        public final JitterSource source;
        public final JitterSourceConfig sourceConfig;

        public enum JitterSource {
            MOD,
            SHADERPACK
        }

        public JitterConfig(boolean enabled) {
            this(enabled, JitterSource.MOD, null);
        }

        public JitterConfig(boolean enabled, JitterSource source, JitterSourceConfig sourceConfig) {
            this.enabled = enabled;
            this.source = source;
            this.sourceConfig = sourceConfig;
        }
    }

    public static class JitterSourceConfig {
        public final SourceConfig jitterOffset;
        public final SourceConfig jitterSequenceLength;

        public JitterSourceConfig(SourceConfig jitterOffset, SourceConfig jitterSequenceLength) {
            this.jitterOffset = jitterOffset;
            this.jitterSequenceLength = jitterSequenceLength;
        }

        public Vector2f getJitterOffset(ShaderPipelineContext context) {
            if (jitterOffset.source == SourceConfig.SourceType.CONST){
                if (jitterOffset.type == SourceConfig.ValueType.VECTOR2F) {
                    return new Vector2f(
                            Float.parseFloat(((List<?>) jitterOffset.value).get(0).toString()),
                            Float.parseFloat(((List<?>) jitterOffset.value).get(1).toString())
                    );
                } else {
                    throw new IllegalArgumentException("Invalid type for jitterOffset: " + jitterOffset.type);
                }
            } else {
                if(jitterOffset.source == SourceConfig.SourceType.UNIFORM) {
                    Object value = context.getCustomUniformValue((String) jitterOffset.value);
                    if (value instanceof Vector2f) {
                        return (Vector2f) value;
                    } else {
                        throw new IllegalArgumentException("Expected Vector2f for uniform " + jitterOffset.value);
                    }
                } else if (jitterOffset.source == SourceConfig.SourceType.VARIABLE) {
                    Object value = context.getCustomVariableValue((String) jitterOffset.value);
                    if (value instanceof Vector2f) {
                        return (Vector2f) value;
                    } else {
                        throw new IllegalArgumentException("Expected Vector2f for variable " + jitterOffset.value);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported source type for jitterOffset: " + jitterOffset.source);
                }
            }
        }

        public int getJitterSequenceLength(ShaderPipelineContext context) {
            if (jitterSequenceLength.source == SourceConfig.SourceType.CONST){
                if (jitterSequenceLength.type == SourceConfig.ValueType.INT) {
                    return Integer.parseInt((jitterSequenceLength.value).toString());
                } else {
                    throw new IllegalArgumentException("Invalid type for jitterSequenceLength: " + jitterSequenceLength.type);
                }
            } else {
                if(jitterSequenceLength.source == SourceConfig.SourceType.UNIFORM) {
                    Object value = context.getCustomUniformValue((String) jitterSequenceLength.value);
                    if (value instanceof Integer) {
                        return (Integer) value;
                    } else {
                        throw new IllegalArgumentException("Expected Integer for uniform " + jitterSequenceLength.value);
                    }
                } else if (jitterSequenceLength.source == SourceConfig.SourceType.VARIABLE) {
                    Object value = context.getCustomVariableValue((String) jitterSequenceLength.value);
                    if (value instanceof Integer) {
                        return (Integer) value;
                    } else {
                        throw new IllegalArgumentException("Expected Integer for variable " + jitterSequenceLength.value);
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported source type for jitterSequenceLength: " + jitterSequenceLength.source);
                }
            }
        }
    }

    public static class SourceConfig {
        public enum SourceType {
            CONST,
            VARIABLE,
            UNIFORM;
            public static SourceType fromString(String value) {
                for (SourceType type : values()) {
                    if (type.name().equalsIgnoreCase(value)) {
                        return type;
                    }
                }
                throw new IllegalArgumentException("Invalid SourceType: " + value);
            }
        }
        public enum ValueType {
            FLOAT,
            INT,
            UINT,
            VECTOR2F,
            VECTOR3F,
            VECTOR4F;
            public static ValueType fromString(String value) {
                for (ValueType type : values()) {
                    if (type.name().equalsIgnoreCase(value)) {
                        return type;
                    }
                }
                throw new IllegalArgumentException("Invalid ValueType: " + value);
            }
        }
        public final SourceType source; // const/variable/uniform
        public final ValueType type; // float,int,uint,vector2f,vector3f,vector4f
        public final Object value; // for const: the actual value; for variable/uniform: the name

        public SourceConfig(String source, String type, Object value) {
            this.source = SourceType.fromString(source);
            this.type = ValueType.fromString(type);
            this.value = value;
        }
    }

    public static class InputTexture {
        public final boolean enabled;
        public final String sourceName;
        public final TextureRegion region;

        public InputTexture(boolean enabled, String sourceName, TextureRegion region) {
            this.enabled = enabled;
            this.sourceName = sourceName;
            this.region = region;
        }
    }

    public static class OutputTexture {
        public final boolean enabled;
        public final List<String> targetNames;
        public final TextureRegion region;

        public OutputTexture(boolean enabled, List<String> targetNames, TextureRegion region) {
            this.enabled = enabled;
            this.targetNames = targetNames;
            this.region = region;
        }
    }
}