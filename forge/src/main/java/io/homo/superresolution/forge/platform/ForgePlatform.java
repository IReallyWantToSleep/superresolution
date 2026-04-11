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

package io.homo.superresolution.forge.platform;

import io.homo.superresolution.api.platform.EnvironmentType;
import io.homo.superresolution.api.platform.Platform;
import net.minecraft.SharedConstants;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;

import java.nio.file.Path;

public class ForgePlatform extends Platform {
    @Override
    public void init() {
        if (isInstallIris()) this.irisPlatform = new IrisForgePlatform();
    }

    @Override
    public String getMinecraftVersion() {
        #if MC_VER > MC_1_21_6
        return SharedConstants.getCurrentVersion().id();
        #else
        return SharedConstants.getCurrentVersion().getName();
        #endif
    }

    @Override
    public boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public String getModVersionString(String modId) {
        if (isModLoaded(modId)) return ModList.get().getModFileById(modId).versionString();
        return null;
    }

    public EnvironmentType getEnv() {
        return switch (FMLLoader.getDist()) {
            case CLIENT -> EnvironmentType.CLIENT;
            case DEDICATED_SERVER -> EnvironmentType.SERVER;
        };
    }

    public Path getGameFolder() {
        return FMLPaths.GAMEDIR.get();
    }

    public boolean isForge(){return true;}
    public boolean isNeoForge(){return false;}
    public boolean isForgeLike(){return true;}
    public  boolean isFabric(){return false;}
}
