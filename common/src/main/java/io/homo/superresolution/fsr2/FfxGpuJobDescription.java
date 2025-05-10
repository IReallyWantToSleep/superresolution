package io.homo.superresolution.fsr2;

// 类：FfxGpuJobDescription
public class FfxGpuJobDescription<T> {
    public FfxGpuJobType jobType;
    public T jobDescriptor;

    public static class Copy extends FfxGpuJobDescription<Copy.CopyJobDescriptor> {
        protected static class CopyJobDescriptor {
            public FfxResource src;
            public FfxResource dst;
        }
    }

    public static class ClearFloat extends FfxGpuJobDescription<ClearFloat.ClearFloatDescriptor> {
        protected static class ClearFloatDescriptor {
            public float[] color;
            public FfxResource target;
        }
    }
}
