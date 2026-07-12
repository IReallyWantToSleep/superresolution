package io.homo.superresolution.core.ngx;

@FunctionalInterface
public interface NgxProgressCallback {
    boolean onProgress(float currentProgress);
}
