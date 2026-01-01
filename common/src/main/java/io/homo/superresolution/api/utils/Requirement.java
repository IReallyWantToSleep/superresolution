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

package io.homo.superresolution.api.utils;

import io.homo.superresolution.api.platform.*;
import io.homo.superresolution.core.RenderSystems;
import io.homo.superresolution.core.graphics.GraphicsCapabilities;

import java.util.*;
import java.util.function.Supplier;

public class Requirement {
    private final Set<String> requiredGlExtensions = new HashSet<>();
    private final Set<OperatingSystem> supportedOS = new HashSet<>();
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
        if (System.getenv().containsKey("SR_TEST")) {
            return new Result(
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true
            );
        }
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

        final OperatingSystem current = Platform.currentPlatform.getOS();
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


    public Set<OperatingSystem> getSupportedOS() {
        return Collections.unmodifiableSet(supportedOS);
    }

    public Requirement addSupportedOS(SystemArchitecture arch) {
        return addSupportedOS(new OperatingSystem(arch, OperatingSystemType.ANY));
    }

    public Requirement addSupportedOS(OperatingSystemType type) {
        return addSupportedOS(new OperatingSystem(SystemArchitecture.ANY, type));
    }

    public Requirement addSupportedOS(SystemArchitecture arch, OperatingSystemType type) {
        return addSupportedOS(new OperatingSystem(arch, type));
    }

    public Requirement addSupportedOS(OperatingSystem operatingSystem) {
        supportedOS.add(Objects.requireNonNull(operatingSystem, "操作系统配置不能为null"));
        return this;
    }

    @Deprecated
    public ArrayList<OperatingSystem> getIncludeOS() {
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