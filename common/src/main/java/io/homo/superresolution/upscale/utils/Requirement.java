package io.homo.superresolution.upscale.utils;

import io.homo.superresolution.upscale.AlgorithmManager;

import java.util.ArrayList;

public class Requirement {
    private final ArrayList<String> includeExtension = new ArrayList<>();
    private final ArrayList<String> excludeExtension = new ArrayList<>();
    private int major_version = -1;
    private int minor_version = -1;
    private boolean aboveVersion = true;

    protected Requirement() {
    }

    public static Requirement nothing() {
        return new Requirement();
    }

    public Requirement version(int major_version, int minor_version, boolean above) {
        this.aboveVersion = above;
        this.major_version = major_version;
        this.minor_version = minor_version;
        return this;
    }

    public Requirement version(int major_version, int minor_version) {
        version(major_version, minor_version, this.aboveVersion);
        return this;
    }

    public Requirement above(boolean above) {
        this.aboveVersion = above;
        return this;
    }

    public Requirement majorVersion(int major_version) {
        this.major_version = major_version;
        return this;
    }

    public Requirement minorVersion(int minor_version) {
        this.minor_version = minor_version;
        return this;
    }

    public Requirement includeExtension(String name) {
        this.includeExtension.add(name);
        return this;
    }

    public Requirement excludeExtension(String name) {
        this.excludeExtension.add(name);
        return this;
    }

    public boolean check() {
        boolean version = true;
        boolean includeExtensionReq = true;
        boolean excludeExtensionReq = true;

        if (aboveVersion) {
            if (major_version != -1 && !(major_version >= AlgorithmHelper.GLVersion[0])) version = false;
            if (minor_version != -1 && !(minor_version >= AlgorithmHelper.GLVersion[1])) version = false;
        } else {
            if (major_version != -1 && !(major_version < AlgorithmHelper.GLVersion[0])) version = false;
            if (minor_version != -1 && !(minor_version < AlgorithmHelper.GLVersion[1])) version = false;
        }

        for (String name : includeExtension)
            if (!AlgorithmManager.helper.hasGLExtension(name)) includeExtensionReq = false;
        for (String name : excludeExtension)
            if (AlgorithmManager.helper.hasGLExtension(name)) excludeExtensionReq = false;
        return version & includeExtensionReq & excludeExtensionReq;

    }
}
