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

package io.homo.superresolution.neoforge.mixin.compat;

import io.homo.superresolution.api.platform.Platform;
import io.homo.superresolution.neoforge.earlywindow.NeoOpenGLVersionOverride;
import io.homo.superresolution.neoforge.platform.NeoForgePlatform;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
#if MC_VER >= MC_1_21_5
import net.neoforged.fml.loading.FMLConfig;
import io.homo.superresolution.core.utils.MessageBox;
#endif
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CompatMixinPlugin implements IMixinConfigPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("SuperResolution-Mixin");
    private final String CLASS_START = "io.homo.superresolution.neoforge.mixin.compat.";

    public CompatMixinPlugin() {
    }

    public void onLoad(String s) {

        Platform.currentPlatform = new NeoForgePlatform();
        Platform.currentPlatform.init();
        #if MC_VER >= MC_1_21_5
        NeoOpenGLVersionOverride.override();
        #endif
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String s, String s1) {
        boolean b = _shouldApplyMixin(s, s1);
        if (!b) {
            LOGGER.info("已禁用Mixin {}", s1);
        } else {
            LOGGER.info("已启用Mixin {}", s1);
        }
        return b;
    }

    public boolean _shouldApplyMixin(String s, String s1) {
        String modid = s1.replace(CLASS_START, "").split("\\.")[0];
        if (Objects.equals(modid, "reesessodiumoptions")) {
            return Platform.currentPlatform.isModLoaded("reeses_sodium_options");
        }
        return Platform.currentPlatform.isModLoaded(modid);
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
}
