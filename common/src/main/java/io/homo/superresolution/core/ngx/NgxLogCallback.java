package io.homo.superresolution.core.ngx;

@FunctionalInterface
public interface NgxLogCallback {
    void onLog(String message, int loggingLevel, int sourceFeature);
}
