package io.homo.superresolution.common.gui.config.pages;

import io.homo.superresolution.core.gui.core.frame.Frame;

@FunctionalInterface
public interface ConfigPage {
    Frame create(ConfigPageContext context);
}
