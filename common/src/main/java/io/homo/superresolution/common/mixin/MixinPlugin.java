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

package io.homo.superresolution.common.mixin;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private final String MIXIN_CLASS_START = "io.homo.superresolution.common.mixin.";

    public MixinPlugin() {
    }

    public void onLoad(String s) {
        GraphicsCapabilities.init();
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String tClass, String mClassPath) {
        String mixinClassify = getClassName(mClassPath).split("\\.")[0];
        String mixinName = getClassName(mClassPath).split("\\.")[1];
        return shouldApplyMixinByName(mixinName) && (
                switch (mixinClassify) {
                    case "core", "gui", "compat" -> true;
                    #if IS_DEV == 1
                    case "debug" -> true;
                    #else
                    case "debug" -> Platform.currentPlatform.isDevelopmentEnvironment();
                    #endif
                    default -> false;
                }
        );
    }

    private boolean shouldApplyMixinByName(String name) {
        if (name.contains("ForceOpenGLVersion_WindowMixin")) {
            return !(Platform.currentPlatform.isModLoaded("threatengl") ||
                    Platform.currentPlatform.isModLoaded("gpu_tape") ||
                    Platform.currentPlatform.isModLoaded("gpu_booster")
            );
        }
        return true;
    }

    public void acceptTargets(Set<String> set, Set<String> set1) {
    }

    public List<String> getMixins() {
        return List.of();
    }

    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }

    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {
    }

    private String getClassName(String mClassPath) {
        return mClassPath.replace(MIXIN_CLASS_START, "");
    }
}
