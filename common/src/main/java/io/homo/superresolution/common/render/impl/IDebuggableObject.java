package io.homo.superresolution.common.render.impl;

public interface IDebuggableObject {
    String getDebugLabel();

    void updateDebugLabel(String newLabel);
}
