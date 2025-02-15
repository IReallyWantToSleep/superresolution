package io.homo.superresolution.common.upscale.utils;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.common.platform.Arch;
import io.homo.superresolution.common.platform.OS;
import io.homo.superresolution.common.platform.OSType;
import io.homo.superresolution.common.platform.Platform;

import java.util.ArrayList;

public class Requirement {
    private final ArrayList<String> includeExtension = new ArrayList<>();
    private int glMajorVersion = -1;
    private int glMinorVersion = -1;
    private boolean developmentEnvironment = false;
    private boolean requireVulkan = false;
    private ArrayList<OS> includeOS = new ArrayList<>();
    protected Requirement() {
    }

    public static Requirement nothing() {
        return new Requirement();
    }

    public boolean isRequireVulkan() {
        return requireVulkan;
    }

    public ArrayList<OS> getIncludeOS() {
        return includeOS;
    }

    public boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }

    public Requirement developmentEnvironment(boolean developmentEnvironment) {
        this.developmentEnvironment = developmentEnvironment;
        return this;
    }

    public Requirement requireVulkan(boolean requireVulkan) {
        this.requireVulkan = requireVulkan;
        return this;
    }

    public Requirement glVersion(int major_version, int minor_version) {
        this.glMajorVersion = major_version;
        this.glMinorVersion = minor_version;
        return this;
    }

    public Requirement glMajorVersion(int major_version) {
        this.glMajorVersion = major_version;
        return this;
    }

    public Requirement glMinorVersion(int minor_version) {
        this.glMinorVersion = minor_version;
        return this;
    }

    public Requirement includeExtension(String name) {
        this.includeExtension.add(name);
        return this;
    }

    public Result check() {
        return new Result(checkOS(), checkGlVersion(), checkExtension(), checkEnv(), checkVulkan());
    }

    public boolean checkGlVersion() {
        boolean version = true;
        if (glMajorVersion != -1 && glMajorVersion > AlgorithmHelper.GLVersion[0]) version = false;
        if (glMinorVersion != -1 && glMinorVersion > AlgorithmHelper.GLVersion[1]) version = false;
        return version;
    }

    public boolean checkVulkan() {
        if (requireVulkan) return SuperResolution.interopManager.supportVulkan;
        return true;
    }

    public boolean checkOS() {
        OS currentOS = Platform.currentPlatform.getOS();
        boolean os = includeOS.isEmpty();
        for (OS o : includeOS) {
            if (o.arch.equals(currentOS.arch) && o.type.equals(currentOS.type)) {
                os = true;
                break;
            }
        }
        return os;
    }

    public boolean checkEnv() {
        return !developmentEnvironment || Platform.currentPlatform.isDevelopmentEnvironment();
    }

    public boolean checkExtension() {
        return getMissingExtension().isEmpty();
    }

    public ArrayList<String> getIncludeExtension() {
        return includeExtension;
    }

    public int getGlMajorVersion() {
        return glMajorVersion;
    }

    public int getGlMinorVersion() {
        return glMinorVersion;
    }

    public ArrayList<String> getMissingExtension() {
        ArrayList<String> missingExtension = new ArrayList<>();
        for (String name : includeExtension)
            if (!AlgorithmHelper.hasGLExtension(name)) {
                missingExtension.add(name);
            }
        return missingExtension;
    }

    public Requirement addIncludeOS(Arch arch) {
        includeOS.add(new OS(arch, OSType.ANY));
        return this;
    }

    public Requirement addIncludeOS(OSType type) {
        includeOS.add(new OS(Arch.ANY, type));
        return this;
    }

    public Requirement addIncludeOS(Arch arch, OSType type) {
        includeOS.add(new OS(arch, type));
        return this;
    }

    public Requirement addIncludeOS(OS os) {
        includeOS.add(os);
        return this;
    }

    public record Result(boolean os, boolean glVersion, boolean glExtension, boolean env, boolean vulkan) {
        public boolean support() {
            return os && glVersion && glExtension && env && vulkan;
        }
    }
}
