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

package io.homo.superresolution.core.gui.widgets.select;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.MouseButton;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class MaterialSelectField extends MaterialWidget<MaterialSelectField> {
    private static final long ANIMATION_DURATION = 200;
    private final Animator.FloatAnimator focusAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());
    private final Animator.FloatAnimator labelAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());
    private final Animator.FloatAnimator arrowAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());
    private Supplier<String> labelSupplier = () -> "";
    private Supplier<String> valueSupplier = () -> "";
    private Supplier<String> placeholderSupplier = () -> "";
    private Supplier<String> supportingTextSupplier = () -> "";
    private Supplier<MaterialSymbol> leadingIconSupplier = () -> null;
    private Supplier<MaterialSymbol> trailingIconSupplier = () -> MaterialSymbols.iconArrowDropDown();
    private boolean focused = false;
    private boolean menuOpen = false;
    private float width = 280;

    public MaterialSelectField() {
        this.style = new MaterialSelectStyle();
        updateSize();
    }

    public static MaterialSelectField create() {
        return new MaterialSelectField();
    }

    @Override
    protected void init() {
        eventBus.addListener(this::onPress);
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateSize();
    }

    @Override
    public MaterialSelectStyle style() {
        return (MaterialSelectStyle) super.style();
    }

    @Override
    public MaterialSelectField setFocused(boolean focused) {
        if (focused != this.focused) {
            this.focused = focused;
            focusAnimator.fromTo(focusAnimator.get(), focused ? 1f : 0f).start();
            updateLabelState();
        }
        return this;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        focusAnimator.update();
        labelAnimator.update();
        arrowAnimator.update();

        if (!isVisible()) {
            return;
        }

        MaterialSelectSize size = style().size();
        MaterialSelectColors colors = style().colors();
        Rectangle bounds = getRawBounds();

        float focusProgress = focusAnimator.get();
        float labelProgress = labelAnimator.get();
        float arrowProgress = arrowAnimator.get();

        ctx.save();

        if (isDisabled()) {
            ctx.pushAlpha(0.38f);
        }

        Color outlineColor = colors.outline(scheme()).copy()
                .lerp(colors.outlineFocused(scheme()), focusProgress);
        float outlineWidth = size.outlineWidth() + (size.outlineFocusedWidth() - size.outlineWidth()) * focusProgress;

        ctx.beginPath();
        ctx.roundedRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                size.cornerRadius()
        );
        ctx.strokeWidth(outlineWidth);
        ctx.strokeColor(outlineColor);
        ctx.endPath(false);

        float contentStartX = bounds.x + size.horizontalPadding();
        float contentEndX = bounds.x + bounds.width - size.horizontalPadding();

        MaterialSymbol leadingIcon = leadingIconSupplier.get();
        if (leadingIcon != null) {
            float iconCenterX = contentStartX + size.iconSize() / 2f;
            float iconCenterY = bounds.y + bounds.height / 2f;
            leadingIcon.render(ctx, colors.leadingIcon(scheme()), size.iconSize(),
                    new Vector2f(iconCenterX, iconCenterY));
            contentStartX += size.iconSize() + size.iconTextGap();
        }

        MaterialSymbol trailingIcon = trailingIconSupplier.get();
        if (trailingIcon != null) {
            float iconCenterX = contentEndX - size.iconSize() / 2f;
            float iconCenterY = bounds.y + bounds.height / 2f;

            ctx.save();
            ctx.translate(iconCenterX, iconCenterY);
            ctx.rotate((float) Math.toRadians(180 * arrowProgress));
            ctx.translate(-iconCenterX, -iconCenterY);
            trailingIcon.render(ctx, colors.trailingIcon(scheme()), size.iconSize(),
                    new Vector2f(iconCenterX, iconCenterY));
            ctx.restore();

            contentEndX -= size.iconSize() + size.iconTextGap();
        }

        String label = labelSupplier.get();
        if (label != null && !label.isEmpty()) {
            float labelFontSize = size.labelCenterFontSize() + (size.labelFontSize() - size.labelCenterFontSize()) * labelProgress;

            float labelCenterY = bounds.y + bounds.height / 2f;
            float labelFloatY = bounds.y;

            float labelY = labelCenterY + (labelFloatY - labelCenterY) * labelProgress;
            float labelX = contentStartX;

            Color labelColor = colors.label(scheme()).copy()
                    .lerp(colors.labelFocused(scheme()), focusProgress);

            if (labelProgress > 0) {
                NanoVG.context.save();
                NanoVG.context.fontSize(labelFontSize);
                float labelWidth = ctx.measureTextWidth(label, labelFontSize, labelFontSize + 1);
                NanoVG.context.restore();

                float bgPadding = 4;
                ctx.beginPath();
                ctx.rect(
                        labelX - bgPadding,
                        labelY - labelFontSize / 2f,
                        labelWidth + bgPadding * 2,
                        labelFontSize
                );
                ctx.fillColor(colors.background(scheme()).copy().alpha((int) (255 * labelProgress)));
                ctx.endPath(true);
            }

            ctx.drawAlignedText(
                    ctx.font(),
                    labelFontSize,
                    label,
                    labelX,
                    labelY,
                    contentEndX - labelX,
                    labelFontSize,
                    labelColor,
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );
        }

        String value = valueSupplier.get();
        String placeholder = placeholderSupplier.get();

        if (labelProgress > 0.5f) {
            String displayText = (value != null && !value.isEmpty()) ? value : placeholder;
            Color textColor = (value != null && !value.isEmpty())
                    ? colors.inputText(scheme())
                    : colors.placeholder(scheme());

            if (displayText != null && !displayText.isEmpty()) {
                float textY = bounds.y + bounds.height / 2f;
                ctx.drawAlignedText(
                        ctx.font(),
                        size.inputFontSize(),
                        displayText,
                        contentStartX,
                        textY,
                        contentEndX - contentStartX,
                        size.inputFontSize(),
                        textColor,
                        TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                        false
                );
            }
        }

        String supportingText = supportingTextSupplier.get();
        if (supportingText != null && !supportingText.isEmpty()) {
            float supportingY = bounds.y + bounds.height + size.supportingTextTopMargin();
            ctx.drawAlignedText(
                    ctx.font(),
                    size.supportingTextFontSize(),
                    supportingText,
                    bounds.x + size.horizontalPadding(),
                    supportingY,
                    bounds.width - size.horizontalPadding() * 2,
                    size.supportingTextFontSize(),
                    colors.supportingText(scheme()),
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_TOP),
                    false
            );
        }

        if (isDisabled()) {
            ctx.popAlpha();
        }

        ctx.restore();
    }

    @Override
    public void destroy() {
    }

    private void onPress(MouseEvent.MousePressEvent event) {
        if (event.getButton() == MouseButton.Left.id()) {
            if (isVisible() && !isDisabled()) {
                eventBus.post(new WidgetEvent.ClickEvent<>(this));
            }
        }
    }

    public MaterialSelectField label(String label) {
        this.labelSupplier = () -> label;
        return this;
    }

    public MaterialSelectField labelSupplier(Supplier<String> supplier) {
        this.labelSupplier = supplier;
        return this;
    }

    public MaterialSelectField value(String value) {
        this.valueSupplier = () -> value;
        updateLabelState();
        return this;
    }

    public MaterialSelectField valueSupplier(Supplier<String> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public MaterialSelectField placeholder(String placeholder) {
        this.placeholderSupplier = () -> placeholder;
        return this;
    }

    public MaterialSelectField supportingText(String text) {
        this.supportingTextSupplier = () -> text;
        return this;
    }

    public MaterialSelectField leadingIcon(MaterialSymbol icon) {
        this.leadingIconSupplier = () -> icon;
        return this;
    }

    public MaterialSelectField trailingIcon(MaterialSymbol icon) {
        this.trailingIconSupplier = () -> icon;
        return this;
    }

    public MaterialSelectField width(float width) {
        this.width = width;
        updateSize();
        return this;
    }

    public String getValue() {
        return valueSupplier.get();
    }

    public String getLabel() {
        return labelSupplier.get();
    }

    boolean isMenuOpen() {
        return menuOpen;
    }

    void setMenuOpen(boolean open) {
        this.menuOpen = open;
        setFocused(open);
        arrowAnimator.fromTo(arrowAnimator.get(), open ? 1f : 0f).start();
    }

    private void updateLabelState() {
        String value = valueSupplier.get();
        boolean hasValue = value != null && !value.isEmpty();
        boolean shouldFloat = focused || hasValue;
        labelAnimator.fromTo(labelAnimator.get(), shouldFloat ? 1f : 0f).start();
    }

    private void updateSize() {
        MaterialSelectSize size = style().size();
        layout().setWidth(width);
        layout().setHeight(size.containerHeight());
    }
}
