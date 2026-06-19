package io.homo.superresolution.common.workmode;

import io.homo.superresolution.common.SuperResolution;
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
        for (String className : BOOTSTRAP_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                Method register = clazz.getMethod("register");
                register.invoke(null);
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable throwable) {
                SuperResolution.LOGGER.warn("初始化工作模式 {} 失败", className, throwable);
            }
        }
    }

    public static void register(SRWorkModeProvider provider) {
        SRWorkModeProvider old = PROVIDERS.put(provider.id(), provider);
        if (old != null && old != provider) {
            SuperResolution.LOGGER.warn("工作模式 {} 被重复注册，将使用新的实现 {}", provider.id(), provider.getClass().getName());
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

    public static SRWorkModeProvider getCurrentProvider() {
        SRWorkModeProvider fallback = PROVIDERS.get(HACK);
        for (SRWorkModeProvider provider : PROVIDERS.values()) {
            if (!HACK.equals(provider.id()) && provider.isActive()) {
                return provider;
            }
        }
        if (fallback == null) {
            throw new IllegalStateException("未注册 hack 工作模式");
        }
        return fallback;
    }

    public static SRWorkModeState getCurrentState() {
        SRWorkModeProvider provider;
        try {
            provider = getCurrentProvider();
        } catch (IllegalStateException ignored) {
            return SRWorkModeState.defaults();
        }
        SRWorkModeState state = provider.getState();
        return state == null ? SRWorkModeState.defaults() : state;
    }

    public static boolean isCurrentMode(String id) {
        try {
            return getCurrentProvider().id().equals(id);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    public static void reloadShaderPack() {
        try {
            getCurrentProvider().reloadShaderPack();
        } catch (IllegalStateException ignored) {
        }
    }
}
