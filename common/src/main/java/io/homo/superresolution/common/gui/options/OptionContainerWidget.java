/*
 * Super Resolution
 * Copyright (c) 2025-2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.common.gui.options;

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaJustify;

public class OptionContainerWidget extends MaterialContainerWidget<OptionContainerWidget> {
    private static final float CORNER_RADIUS = 12f;
    private static final float PADDING_HORIZONTAL = 16f;
    private static final float PADDING_VERTICAL = 12f;
    private static final float MIN_CONTENT_HEIGHT = 32f; // 56 - 12*2 = 32
    protected AbstractOptionEntry<?, ?> entry;
    protected ContainerWidget leftContainer;
    protected MaterialLabel nameLabel;
    protected MaterialLabel descriptionLabel;
    protected ContainerWidget rightContainer;

    public OptionContainerWidget(AbstractOptionEntry<?, ?> entry) {
        this.entry = entry;
        initLayout();
    }

    private void initLayout() {
        layout().setFlexDirection(YogaFlexDirection.ROW);
        layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        layout().setAlignItems(YogaAlign.CENTER);
        layout().setWidthPercent(100);
        layout().setPadding(YogaEdge.HORIZONTAL, PADDING_HORIZONTAL);
        layout().setPadding(YogaEdge.VERTICAL, PADDING_VERTICAL);
        layout().setMinHeight(MIN_CONTENT_HEIGHT);

        leftContainer = new ContainerWidget();
        leftContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        leftContainer.layout().setFlexGrow(1f);
        leftContainer.layout().setFlexShrink(1f);
        leftContainer.layout().setAlignItems(YogaAlign.FLEX_START);
        leftContainer.layout().setJustifyContent(YogaJustify.CENTER);

        nameLabel = MaterialLabel.create()
                .text(() -> entry.getName().getString())
                .color((MaterialScheme::onSurface))
                .fontSize(16);
        leftContainer.addChild(nameLabel);

        descriptionLabel = MaterialLabel.create()
                .text(() -> getDescriptionText())
                .color((MaterialScheme::onSurfaceVariant))
                .fontSize(11.5f)
                .lineHeight(14);
        descriptionLabel.style().wrap(true);
        descriptionLabel.layout().setWidthPercent(100);
        descriptionLabel.layout().setMargin(YogaEdge.TOP, 6);
        leftContainer.addChild(descriptionLabel);

        super.addChild(leftContainer);

        rightContainer = new ContainerWidget();
        rightContainer.layout().setFlexDirection(YogaFlexDirection.ROW);
        rightContainer.layout().setAlignItems(YogaAlign.CENTER);
        rightContainer.layout().setJustifyContent(YogaJustify.FLEX_END);
        rightContainer.layout().setMargin(YogaEdge.LEFT, 16);

        super.addChild(rightContainer);
    }

    private String getDescriptionText() {
        if (entry.getTooltipSupplier() != null) {
            var tooltipOpt = ((AbstractOptionEntry<Object, Object>) entry).getTooltipSupplier().apply(entry.value());
            if (tooltipOpt.isPresent()) {
                var texts = tooltipOpt.get();
                if (texts.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < texts.length; i++) {
                        if (i > 0) {
                            sb.append("\n");
                        }
                        sb.append(texts[i].getString());
                    }
                    return sb.toString();
                }
            }
        }
        return "";
    }

    public void addControl(ILayoutElement control) {
        rightContainer.addChild(control);
    }

    @Override
    public void layouting(RenderContext ctx) {
        super.layouting(ctx);
        entry.tick(ctx);
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        String desc = getDescriptionText();
        boolean hasDescription = desc != null && !desc.isEmpty();
        descriptionLabel.setVisible(hasDescription);

        super.render(ctx, inputState);
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
    }

    @Override
    public Rectangle getBounds() {
        return super.getBounds();
    }

    public ContainerWidget getLeftContainer() {
        return leftContainer;
    }

    public ContainerWidget getRightContainer() {
        return rightContainer;
    }

    public MaterialLabel getNameLabel() {
        return nameLabel;
    }

    public MaterialLabel getDescriptionLabel() {
        return descriptionLabel;
    }
}
