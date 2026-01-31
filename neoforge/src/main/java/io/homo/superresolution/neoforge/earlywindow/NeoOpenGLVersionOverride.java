/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.neoforge.earlywindow;

import net.neoforged.fml.loading.FMLConfig;

#if MC_VER >= MC_1_21_5
import io.homo.superresolution.core.utils.MessageBox;
import io.homo.superresolution.common.SuperResolution;
#endif

public class NeoOpenGLVersionOverride {
    public static void override() {
        #if MC_VER < MC_1_21_4
        FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER, "fmlearlywindow");
        #endif
        #if MC_VER >= MC_1_21_5
        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            String infoZH = "SuperResolution需要覆盖OpenGL版本，但由于一些原因，你需要关闭游戏并重新打开它；在刚刚，SuperResolution已经把fml.toml中的earlyWindowControl修改为false，这将禁用早期加载界面，不过不用担心，这只会导致游戏在早期加载时你什么也看不到，但请注意不要把fml.toml中的earlyWindowControl修改为true，这会导致这条消息再次出现。";
            String infoEN = "SuperResolution requires OpenGL version override, but due to technical constraints, you must close and restart the game. The earlyWindowControl setting in fml.toml has been automatically set to false, which disables the early loading screen. Rest assured, this only means you won't see visual feedback during initial loading phases. Please do NOT manually revert earlyWindowControl to true, as this will trigger this message again.";

            FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL, false);
            SuperResolution.LOGGER.info(infoZH);
            SuperResolution.LOGGER.info(infoEN);
            MessageBox.createInfo(
                    """
                            %s
                            
                            %s
                            """.formatted(infoZH, infoEN),
                    "Info"
            );
            System.exit(1);
        }
        #endif
    }
}
