package io.homo.superresolution.core.graphics.impl.shader.uniform;

import io.homo.superresolution.core.impl.Destroyable;

public enum ShaderUniformAccess {
    /**
     * 只读
     */
    Read,
    /**
     * 只写
     */
    Write,
    /**
     * 可读可写
     */
    Both
}
