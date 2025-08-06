package io.homo.superresolution.api.utils;

import io.homo.superresolution.common.platform.*;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;

import java.util.*;
import java.util.function.Supplier;

public class Requirement {
    private final Set<String> requiredGlExtensions = new HashSet<>();
    private final Set<OS> supportedOS = new HashSet<>();
    private final Set<String> requiredVulkanDeviceExtensions = new HashSet<>();
    private int glMajorVersion = -1;
    private int glMinorVersion = -1;
    private int vulkanMajorVersion = -1;
    private int vulkanMinorVersion = -1;
    private int vulkanPatchVersion = -1;
    private boolean requiresDevEnv = false;
    private boolean requiresVulkan = false;
    private final List<Supplier<Boolean>> additionalChecks = new ArrayList<>();

    private Requirement() {

    }

    public static Requirement nothing() {
        return new Requirement();
    }

    public Requirement vulkanMajorVersion(int vulkanMajorVersion) {
        this.vulkanMajorVersion = vulkanMajorVersion;
        return this;
    }

    public Requirement vulkanMinorVersion(int vulkanMinorVersion) {
        this.vulkanMinorVersion = vulkanMinorVersion;
        return this;
    }

    public Requirement vulkanPatchVersion(int vulkanPatchVersion) {
        this.vulkanPatchVersion = vulkanPatchVersion;
        return this;
    }

    public Requirement isTrue(Supplier<Boolean> dep) {
        additionalChecks.add(Objects.requireNonNull(dep, "dep不能为null"));
        return this;
    }

    public Requirement isFalse(Supplier<Boolean> dep) {
        additionalChecks.add(() -> !Objects.requireNonNull(dep, "dep不能为null").get());
        return this;
    }

    public Set<String> getRequiredGlExtensions() {
        return Collections.unmodifiableSet(requiredGlExtensions);
    }

    public Set<String> getRequiredVulkanDeviceExtensions() {
        return Collections.unmodifiableSet(requiredVulkanDeviceExtensions);
    }

    public boolean isRequiresVulkan() {
        return requiresVulkan;
    }

    public boolean isRequiresDevEnv() {
        return requiresDevEnv;
    }

    public int getGlMinorVersion() {
        return glMinorVersion;
    }

    public int getGlMajorVersion() {
        return glMajorVersion;
    }

    public int getVulkanMajorVersion() {
        return vulkanMajorVersion;
    }

    public int getVulkanMinorVersion() {
        return vulkanMinorVersion;
    }

    public int getVulkanPatchVersion() {
        return vulkanPatchVersion;
    }

    public Requirement developmentEnvironment(boolean developmentEnvironment) {
        this.requiresDevEnv = developmentEnvironment;
        return this;
    }

    public Requirement requireVulkan(boolean requireVulkan) {
        this.requiresVulkan = requireVulkan;
        return this;
    }

    public Requirement glVersion(int major, int minor) {
        this.glMajorVersion = major;
        this.glMinorVersion = minor;
        return this;
    }

    public Requirement vulkanVersion(int major, int minor, int patch) {
        this.vulkanMajorVersion = major;
        this.vulkanMinorVersion = minor;
        this.vulkanPatchVersion = patch;

        return this;
    }

    private boolean checkVulkanVersion() {
        if (vulkanMajorVersion == -1) return true;

        int[] current = GraphicsCapabilities.getVulkanVersion();
        return current[0] > vulkanMajorVersion ||
                (current[0] == vulkanMajorVersion &&
                        (current[1] > vulkanMinorVersion ||
                                (current[1] == vulkanMinorVersion && current[2] >= vulkanPatchVersion)));
    }

    private boolean checkVulkanDeviceExtensions() {
        return requiredVulkanDeviceExtensions.stream()
                .allMatch(GraphicsCapabilities::hasVulkanDeviceExtension);
    }

    private boolean checkAdditionalConditions() {
        for (Supplier<Boolean> dep : additionalChecks) {
            try {
                if (!dep.get()) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    public Requirement requireVulkanDeviceExtension(String extension) {
        this.requiredVulkanDeviceExtensions.add(extension);
        return this;
    }

    public Requirement glMajorVersion(int major) {
        this.glMajorVersion = major;
        return this;
    }

    public Requirement glMinorVersion(int minor) {
        this.glMinorVersion = minor;
        return this;
    }

    public Requirement requiredGlExtension(String name) {
        this.requiredGlExtensions.add(Objects.requireNonNull(name, "扩展名称不能为null"));
        return this;
    }

    public Result check() {
        return new Result(
                checkOSCompatibility(),
                checkGLVersion(),
                checkGLExtensions(),
                checkEnvironment(),
                checkVulkanSupport(),
                checkVulkanVersion(),
                checkVulkanDeviceExtensions(),
                checkAdditionalConditions()
        );
    }

    private boolean checkOSCompatibility() {
        if (supportedOS.isEmpty()) return true;

        final OS current = Platform.currentPlatform.getOS();
        return supportedOS.stream()
                .anyMatch(os -> os.arch.equals(current.arch) &&
                        os.type.equals(current.type));
    }

    private boolean checkGLVersion() {
        if (glMajorVersion == -1) return true;

        final int[] currentVersion = GraphicsCapabilities.getGLVersion();
        return currentVersion[0] > glMajorVersion ||
                (currentVersion[0] == glMajorVersion && currentVersion[1] >= glMinorVersion);
    }

    private boolean checkGLExtensions() {
        return requiredGlExtensions.stream()
                .allMatch(GraphicsCapabilities::hasGLExtension);
    }

    private boolean checkEnvironment() {
        return !requiresDevEnv || Platform.currentPlatform.isDevelopmentEnvironment();
    }

    private boolean checkVulkanSupport() {
        return !requiresVulkan || (RenderSystems.isSupportVulkan());
    }

    public List<String> getMissingGlExtensions() {
        return requiredGlExtensions.stream()
                .filter(ext -> !GraphicsCapabilities.hasGLExtension(ext))
                .toList();
    }

    public List<String> getMissingVkExtensions() {
        return requiredVulkanDeviceExtensions.stream()
                .filter(ext -> !GraphicsCapabilities.hasVulkanDeviceExtension(ext))
                .toList();
    }


    public Set<OS> getSupportedOS() {
        return Collections.unmodifiableSet(supportedOS);
    }

    public Requirement addSupportedOS(Arch arch) {
        return addSupportedOS(new OS(arch, OSType.ANY));
    }

    public Requirement addSupportedOS(OSType type) {
        return addSupportedOS(new OS(Arch.ANY, type));
    }

    public Requirement addSupportedOS(Arch arch, OSType type) {
        return addSupportedOS(new OS(arch, type));
    }

    public Requirement addSupportedOS(OS os) {
        supportedOS.add(Objects.requireNonNull(os, "操作系统配置不能为null"));
        return this;
    }

    @Deprecated
    public ArrayList<OS> getIncludeOS() {
        return new ArrayList<>(supportedOS);
    }

    public record Result(
            boolean osSupported,
            boolean glVersionMet,
            boolean glExtensionsPresent,
            boolean environmentValid,
            boolean vulkanAvailable,
            boolean vulkanVersionMet,
            boolean vulkanDeviceExtensionsMet,
            boolean additionalConditionsMet
    ) {
        public boolean support() {
            return osSupported && glVersionMet && glExtensionsPresent &&
                    environmentValid && vulkanAvailable && vulkanVersionMet && vulkanDeviceExtensionsMet && additionalConditionsMet;
        }
    }
}