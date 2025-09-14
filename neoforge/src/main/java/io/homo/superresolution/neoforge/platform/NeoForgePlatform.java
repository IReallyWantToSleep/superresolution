/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

import io.homo.superresolution.common.platform.EnvType;
import io.homo.superresolution.common.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

import java.nio.file.Path;

public class NeoForgePlatform extends Platform {
    @Override
    public void init() {
        this.irisPlatform = new IrisNeoForgePlatform();
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

    @Override
    public EnvType getEnv() {
        return FMLLoader.getDist().isClient() ? EnvType.CLIENT : EnvType.SERVER;
    }

    @Override
    public Path getGameFolder() {
        return FMLLoader.getGamePath();
    }
}
