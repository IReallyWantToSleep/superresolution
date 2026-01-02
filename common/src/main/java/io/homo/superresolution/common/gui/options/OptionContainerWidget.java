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
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.*;

import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

/**
 * 选项容器组件
 * 布局：左侧为名称和描述，右侧为控件
 * 背景使用 Material3 的 surface 颜色
 */
public class OptionContainerWidget extends MaterialContainerWidget<OptionContainerWidget> {
    protected AbstractOptionEntry<?, ?> entry;

    // 左侧文本区域
    protected ContainerWidget leftContainer;
    protected MaterialLabel nameLabel;
    protected MaterialLabel descriptionLabel;

    // 右侧控件区域
    protected ContainerWidget rightContainer;

    // 圆角大小
    private static final float CORNER_RADIUS = 12f;
    private static final float PADDING_HORIZONTAL = 16f;
    private static final float PADDING_VERTICAL = 12f;

    public OptionContainerWidget(AbstractOptionEntry<?, ?> entry) {
        this.entry = entry;
        initLayout();
    }

    private void initLayout() {
        // 主容器横向布局
        layout().setFlexDirection(YogaFlexDirection.ROW);
        layout().setJustifyContent(YogaJustify.SPACE_BETWEEN);
        layout().setAlignItems(YogaAlign.CENTER);
        layout().setWidthPercent(100);
        layout().setPadding(YogaEdge.HORIZONTAL, PADDING_HORIZONTAL);
        layout().setPadding(YogaEdge.VERTICAL, PADDING_VERTICAL);

        // 左侧容器 - 名称和描述
        leftContainer = new ContainerWidget();
        leftContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        leftContainer.layout().setFlexGrow(1f);
        leftContainer.layout().setFlexShrink(1f);
        leftContainer.layout().setAlignItems(YogaAlign.FLEX_START);
        leftContainer.layout().setJustifyContent(YogaJustify.CENTER);

        nameLabel = MaterialLabel.create()
                .text(() -> entry.getName().getString())
                .fontSize(16)
                .scheme(scheme);
        leftContainer.addChild(nameLabel);

        descriptionLabel = MaterialLabel.create()
                .text(() -> getDescriptionText())
                .fontSize(12)
                .scheme(scheme);
        descriptionLabel.layout().setMargin(YogaEdge.TOP, 4);
        leftContainer.addChild(descriptionLabel);

        super.addChild(leftContainer);

        // 右侧容器 - 控件
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
                        if (i > 0) sb.append("\n");
                        sb.append(texts[i].getString());
                    }
                    return sb.toString();
                }
            }
        }
        return "";
    }

    /**
     * 添加控件到右侧区域
     */
    public void addControl(io.homo.superresolution.core.gui.core.layout.ILayoutElement control) {
        rightContainer.addChild(control);
    }

    public MaterialScheme scheme() {
        return scheme;
    }

    public OptionContainerWidget scheme(MaterialScheme scheme) {
        this.scheme = scheme;
        nameLabel.scheme(scheme).color(scheme.onSurface());
        descriptionLabel.scheme(scheme).color(scheme.onSurfaceVariant());
        return this;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        // 更新描述文本显示状态
        String desc = getDescriptionText();
        boolean hasDescription = desc != null && !desc.isEmpty();
        descriptionLabel.setVisible(hasDescription);

        // 根据是否有描述动态调整高度
        float height = hasDescription ? entry.getEntryHeight() : entry.getEntryHeight() - 16;
        setElementHeight(height);

        super.render(ctx, inputState);
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
    }

    @Override
    public Rectangle getBounds() {
        return super.getBounds();
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
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
