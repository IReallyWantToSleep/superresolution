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

        public UpscaleConfig(boolean enabled, PipelineTrigger trigger, TextureFormat internalFormat,
                             Map<String, InputTexture> inputTextures, Map<String, OutputTexture> outputTextures) {
            this.enabled = enabled;
            this.trigger = trigger;
            this.internalFormat = internalFormat;
            this.inputTextures = inputTextures;
            this.outputTextures = outputTextures;
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

        public JitterConfig(boolean enabled) {
            this.enabled = enabled;
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