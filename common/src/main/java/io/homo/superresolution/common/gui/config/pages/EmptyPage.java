package io.homo.superresolution.common.gui.config.pages;

import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.frame.Frame;
import io.homo.superresolution.core.gui.core.frame.ScrollableFrame;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;

public final class EmptyPage implements ConfigPage {
    public static final EmptyPage INSTANCE = new EmptyPage();

    private EmptyPage() {
    }

    @Override
    public Frame create(ConfigPageContext context) {
        ScrollableFrame frame = new ScrollableFrame();
        ContainerWidget container = new ContainerWidget();
        container.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        container.layout().setWidthPercent(100);
        frame.setRoot(container);
        return frame;
    }
}
