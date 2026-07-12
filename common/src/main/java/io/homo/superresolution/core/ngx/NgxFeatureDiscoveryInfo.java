package io.homo.superresolution.core.ngx;

public final class NgxFeatureDiscoveryInfo {
    public int sdkVersion = NgxConstants.VERSION_API;
    public int feature;
    public final NgxApplicationIdentifier identifier = new NgxApplicationIdentifier();
    public String applicationDataPath;
    public NgxFeatureCommonInfo featureInfo;
}
