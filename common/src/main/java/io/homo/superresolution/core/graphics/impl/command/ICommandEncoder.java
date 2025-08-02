package io.homo.superresolution.core.graphics.impl.command;

public interface ICommandEncoder {
    ICommandEncoder begin();


    ICommandBuffer end();
}
