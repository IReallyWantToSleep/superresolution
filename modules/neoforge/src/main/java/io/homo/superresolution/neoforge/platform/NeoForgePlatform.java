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

package io.homo.superresolution.neoforge.platform;

import io.homo.superresolution.api.platform.EnvironmentType;
import io.homo.superresolution.api.platform.Platform;
import net.minecraft.SharedConstants;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.nio.file.Path;

public class NeoForgePlatform extends Platform {
    @Override
    public void init() {
        this.irisPlatform = new IrisNeoForgePlatform();
    }

    @Override
    public boolean isModLoaded(String modId) {
        #if MC_VER >= MC_1_21_9
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(modId) != null;
        #else
        return net.neoforged.fml.loading.LoadingModList.get().getModFileById(modId) != null;
        #endif
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        #if MC_VER >= MC_1_21_9
        return !FMLLoader.getCurrent().isProduction();
        #else
        return !FMLLoader.isProduction();
        #endif
    }

    @Override
    public String getModVersionString(String modId) {
        if (isModLoaded(modId)) {
            return ModList.get().getModFileById(modId).versionString();
        }
        return null;
    }

    @Override
    public EnvironmentType getEnv() {
        #if MC_VER >= MC_1_21_9
        return FMLLoader.getCurrent().getDist().isClient() ? EnvironmentType.CLIENT : EnvironmentType.SERVER;
        #else
        return FMLLoader.getDist().isClient() ? EnvironmentType.CLIENT : EnvironmentType.SERVER;
        #endif
    }

    public String getMinecraftVersion() {
        #if MC_VER > MC_1_21_6
        return SharedConstants.getCurrentVersion().id();
        #else
        return SharedConstants.VERSION_STRING;
        #endif
    }

    @Override
    public Path getGameFolder() {
        #if MC_VER >= MC_1_21_9
        return FMLLoader.getCurrent().getGameDir();
        #else
        return FMLLoader.getGamePath();
        #endif
    }

    public boolean isForge(){return false;}
    public boolean isNeoForge(){return true;}
    public boolean isForgeLike(){return true;}
    public  boolean isFabric(){return false;}
}
