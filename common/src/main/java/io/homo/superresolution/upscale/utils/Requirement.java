package io.homo.superresolution.upscale.utils;

import java.util.ArrayList;

public class Requirement {
    private final ArrayList<String> includeExtension = new ArrayList<>();
    private int major_version = -1;
    private int minor_version = -1;

    protected Requirement() {
    }

    public static Requirement nothing() {
        return new Requirement();
    }

    public Requirement version(int major_version, int minor_version) {
        this.major_version = major_version;
        this.minor_version = minor_version;
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

    public boolean check() {
        return checkVersion() & checkExtension();
    }

    public boolean checkVersion() {
        boolean version = true;
        if (major_version != -1 && major_version > AlgorithmHelper.GLVersion[0]) version = false;
        if (minor_version != -1 && minor_version > AlgorithmHelper.GLVersion[1]) version = false;
        return version;
    }

    public boolean checkExtension(){
        return getMissingExtension().isEmpty();
    }

    public ArrayList<String> getIncludeExtension() {
        return includeExtension;
    }

    public int getMajorVersion() {
        return major_version;
    }

    public int getMinorVersion() {
        return minor_version;
    }

    public ArrayList<String> getMissingExtension(){
        ArrayList<String> missingExtension =  new ArrayList<>();
        for (String name : includeExtension)
            if (!AlgorithmHelper.hasGLExtension(name)) {
                missingExtension.add(name);
            }
        return missingExtension;
    }
}
