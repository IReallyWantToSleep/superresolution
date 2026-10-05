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

package io.homo.superresolution.api.platform;

import java.nio.file.Path;

public abstract class Platform {
    public static Platform currentPlatform = null;
    private static Boolean isInstallIris = null;
    protected IrisPlatform irisPlatform = null;

    public abstract boolean isModLoaded(String modId);

    public abstract boolean isDevelopmentEnvironment();

    public abstract String getModVersionString(String modId);

    public OperatingSystem getOS() {
        return new OperatingSystem();
    }

    public abstract EnvironmentType getEnv();

    public abstract Path getGameFolder();

    public IrisPlatform iris() {
        return irisPlatform;
    }

    public abstract void init();

    public boolean isInstallIris() {
        if (isInstallIris == null) {
            isInstallIris = currentPlatform.isModLoaded("iris") || currentPlatform.isModLoaded("oculus");
        }
        return isInstallIris;
    }

    public abstract String getMinecraftVersion();

    public abstract boolean isForge();

    public abstract boolean isNeoForge();

    public abstract boolean isForgeLike();

    public abstract boolean isFabric();

}
