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

package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.minecraft.B3DVulkanBridge;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SRWorkModeManager {
    public static final String HACK = "hack";
    public static final String SHADER_COMPAT = "shader_compat";

    private static final String[] BOOTSTRAP_CLASSES = new String[]{
            "io.homo.superresolution.common.workmode.HackWorkModeBootstrap",
            "io.homo.superresolution.shadercompat.ShaderCompatBootstrap"
    };

    private static final Map<String, SRWorkModeProvider> PROVIDERS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private SRWorkModeManager() {
    }

    public static void bootstrapProviders() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        if (B3DVulkanBridge.isB3DVulkanBackend()) {
            return;
        }
        for (String className : BOOTSTRAP_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                Method register = clazz.getMethod("register");
                register.invoke(null);
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable throwable) {
                SuperResolution.LOGGER.warn("Failed to initialize work mode {}", className, throwable);
            }
        }
    }

    public static void register(SRWorkModeProvider provider) {
        SRWorkModeProvider old = PROVIDERS.put(provider.id(), provider);
        if (old != null && old != provider) {
            SuperResolution.LOGGER.warn("Work mode {} was registered more than once; using the new implementation {}", provider.id(), provider.getClass().getName());
        }
    }

    public static void onClientSetup() {
        bootstrapProviders();
        for (SRWorkModeProvider provider : PROVIDERS.values()) {
            provider.onClientSetup();
        }
    }

    @Nullable
    public static SRWorkModeProvider getProvider(String id) {
        return PROVIDERS.get(id);
    }

    @Nullable
    public static SRWorkModeProvider getCurrentProvider() {
        SRWorkModeProvider fallback = PROVIDERS.get(HACK);
        for (SRWorkModeProvider provider : PROVIDERS.values()) {
            if (!HACK.equals(provider.id()) && provider.isActive()) {
                return provider;
            }
        }
        return fallback != null && fallback.isActive() ? fallback : null;
    }

    public static boolean hasAvailableWorkMode() {
        return getCurrentProvider() != null;
    }

    public static SRWorkModeState getCurrentState() {
        SRWorkModeProvider provider = getCurrentProvider();
        if (provider == null) {
            return SRWorkModeState.defaults();
        }
        SRWorkModeState state = provider.getState();
        return state == null ? SRWorkModeState.defaults() : state;
    }

    public static boolean isCurrentMode(String id) {
        SRWorkModeProvider provider = getCurrentProvider();
        return provider != null && provider.id().equals(id);
    }

    public static void reloadShaderPack() {
        SRWorkModeProvider provider = getCurrentProvider();
        if (provider != null) {
            provider.reloadShaderPack();
        }
    }
}
