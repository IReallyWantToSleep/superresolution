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

package io.homo.superresolution.fabric.platform;

import io.homo.superresolution.api.platform.EnvironmentType;
import io.homo.superresolution.api.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.nio.file.Path;

public class FabricPlatform extends Platform {
    @Override
    public void init() {
        this.irisPlatform = new IrisFabricPlatform();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String getModVersionString(String modId) {
        if (isModLoaded(modId))
            return FabricLoader.getInstance().getModContainer(modId).orElseThrow().getMetadata().getVersion().getFriendlyString();
        return null;
    }

    public EnvironmentType getEnv() {
        return switch (FabricLoader.getInstance().getEnvironmentType()) {
            case CLIENT -> EnvironmentType.CLIENT;
            case SERVER -> EnvironmentType.SERVER;
        };
    }

    public Path getGameFolder() {
        return FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    }

    public String getMinecraftVersion() {
        #if MC_VER > MC_1_21_6
        return SharedConstants.getCurrentVersion().id();
        #else
        return SharedConstants.VERSION_STRING;
        #endif
    }


    public boolean isForge(){return false;}
    public boolean isNeoForge(){return false;}
    public boolean isForgeLike(){return false;}
    public  boolean isFabric(){return true;}
}
