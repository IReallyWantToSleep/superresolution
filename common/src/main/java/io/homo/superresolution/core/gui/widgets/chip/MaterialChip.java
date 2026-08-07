/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation: either version 3 of the License, or
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

package io.homo.superresolution.core.gui.widgets.chip;

import io.homo.superresolution.core.gui.MaterialElevation;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialWidgetOverlay;
import io.homo.superresolution.core.gui.core.MouseButton;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaterialChip extends MaterialWidget<MaterialChip> {
    private static final float CONTAINER_HEIGHT = 32f;
    private static final float CONTAINER_RADIUS = 8f;
    private static final float ICON_SIZE = 18f;
    private static final float AVATAR_SIZE = 24f;
    private static final float AVATAR_RADIUS = 12f;
    private static final float LABEL_FONT_SIZE = 14f;
    private static final float LABEL_LINE_HEIGHT = 20f;
    private static final float LABEL_WEIGHT = 500f;
    private static final float ICON_TEXT_GAP = 8f;
    private static final float ICON_PADDING = 8f;
    private static final float TEXT_PADDING = 16f;
    private static final float AVATAR_START_PADDING = 4f;
    private static final float CLOSE_TOUCH_TARGET = 48f;

    private Supplier<String> textSupplier = () -> "";
    private Supplier<MaterialSymbol> leadingIconSupplier = () -> null;
    private Supplier<MaterialSymbol> trailingIconSupplier = () -> null;
    private Supplier<MaterialSymbol> avatarSupplier = () -> null;
    private Consumer<MaterialChip> trailingIconAction;
    private boolean selected;
    private boolean dragged;
    private boolean pressOnTrailingIcon;
    private final Animator.FloatAnimator selectionAnimator = Animator.ofFloat(0f, 0f)
            .duration(200)
            .timeInterpolator(TimeInterpolator.easeOutCubic());

    private final MaterialWidgetOverlay<MaterialChip> overlay = new MaterialWidgetOverlay<>(this) {
        @Override
        protected void drawShape(RenderContext ctx, MaterialChip widget, Color color) {
            Rectangle bounds = widget.getRawBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CONTAINER_RADIUS,
                    color,
                    true
            );
        }

        @Override
        protected void drawShape(RenderContext ctx, MaterialChip widget, IPaint paint) {
            Rectangle bounds = widget.getRawBounds();
            ctx.beginPath();
            ctx.paint(paint);
            ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTAINER_RADIUS);
            ctx.endPath(true);
        }
    };

    public MaterialChip(MaterialChipType type) {
        style().type(type);
        style().elevated(false);
        overlay.setRippleAlpha(0.12f);
        getLayoutNode().setDebugName("MaterialChip");
        updateRectangle(null);
    }

    public static MaterialChip create(MaterialChipType type) {
        return new MaterialChip(type);
    }

    public static MaterialChip assist(String text) {
        return create(MaterialChipType.Assist).text(text);
    }

    public static MaterialChip filter(String text) {
        return create(MaterialChipType.Filter).text(text);
    }

    public static MaterialChip input(String text) {
        return create(MaterialChipType.Input).text(text);
    }

    public static MaterialChip suggestion(String text) {
        return create(MaterialChipType.Suggestion).text(text);
    }

    @Override
    protected void init() {
        style = new MaterialChipStyle();
        onMousePress(this::onPress);
        onMouseRelease(this::onRelease);
        onMouseDrag(this::onDrag);
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateRectangle(ctx);
    }

    @Override
    public MaterialChipStyle style() {
        return (MaterialChipStyle) style;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        if (!isVisible()) {
            return;
        }

        overlay.update();
        selectionAnimator.update();
        Rectangle bounds = getRawBounds();
        ChipColors colors = getChipColors();

        ctx.beginGroup(style().zIndex());
        drawBackground(ctx, bounds, colors);
        drawContent(ctx, bounds, colors);
        ctx.endGroup();
    }

    protected void drawBackground(RenderContext ctx, Rectangle bounds, ChipColors colors) {
        int elevation = getElevation();
        if (elevation > 0) {
            MaterialElevation.draw(ctx, elevation, bounds.x, bounds.y, bounds.width, bounds.height,
                    CONTAINER_RADIUS, scheme().shadow());
        }
        drawContainerBackground(ctx, bounds, colors.background);
        if (colors.selectedBackground != null && colors.selectedBackground.alpha() > 0) {
            ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTAINER_RADIUS,
                    colors.selectedBackground, true);
        }
        if (!isDisabled() && shouldRenderMaterialOverlay()) {
            overlay.renderHoverOverlay(ctx, colors.stateLayer);
        }
        if (colors.outline != null) {
            ctx.beginPath();
            ctx.strokeWidth(1f);
            ctx.strokeColor(colors.outline);
            ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTAINER_RADIUS);
            ctx.endPath(false);
        }

        if (!isDisabled() && shouldRenderMaterialOverlay()) {
            if (!isDisabled() && dragged) {
                ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTAINER_RADIUS,
                        colors.stateLayer.copy().alpha((int) (255f * 0.16f)), true);
            }
            overlay.renderRippleOverlay(ctx, colors.stateLayer);
        }
    }

    protected boolean shouldRenderMaterialOverlay() {
        return true;
    }

    protected void drawContainerBackground(RenderContext ctx, Rectangle bounds, Color color) {
        if (color != null) {
            ctx.roundedRect(bounds.x, bounds.y, bounds.width, bounds.height, CONTAINER_RADIUS, color, true);
        }
    }

    protected void drawContent(RenderContext ctx, Rectangle bounds, ChipColors colors) {
        ctx.save();
        if (isDisabled()) {
            ctx.pushAlpha(0.38f);
        }
        float contentX = bounds.x + getLeadingStartPadding();
        MaterialSymbol avatar = avatarSupplier.get();
        MaterialSymbol leadingIcon = leadingIconSupplier.get();
        if (avatar != null) {
            float centerX = contentX + AVATAR_SIZE / 2f;
            float centerY = bounds.getCenterY();
            ctx.arc(centerX, centerY, AVATAR_RADIUS, scheme().primaryContainer(), true);
            avatar.render(ctx, scheme().onPrimaryContainer(), ICON_SIZE, new Vector2f(centerX, centerY));
            contentX += AVATAR_SIZE + ICON_TEXT_GAP;
        } else if (leadingIcon != null) {
            float centerX = contentX + ICON_SIZE / 2f;
            leadingIcon.render(ctx, colors.leadingIcon, ICON_SIZE, new Vector2f(centerX, bounds.getCenterY()));
            contentX += ICON_SIZE + ICON_TEXT_GAP;
        }
        drawText(ctx, bounds, colors, contentX);
        MaterialSymbol trailingIcon = trailingIconSupplier.get();
        if (trailingIcon != null) {
            float centerX = bounds.x + bounds.width - 8f - ICON_SIZE / 2f;
            trailingIcon.render(ctx, colors.trailingIcon, ICON_SIZE, new Vector2f(centerX, bounds.getCenterY()));
        }
        ctx.restore();
    }

    protected void drawText(RenderContext ctx, Rectangle bounds, ChipColors colors, float contentX) {
        String text = textSupplier.get();
        ctx.drawAlignedText(ctx.font(), LABEL_FONT_SIZE, text == null ? "" : text, contentX, bounds.getCenterY(),
                Math.max(0f, bounds.x + bounds.width - getTrailingContentEnd() - contentX), LABEL_LINE_HEIGHT,
                LABEL_WEIGHT, colors.label, TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE), false);
    }

    @Override
    public void destroy() {
        if (overlay != null) {
            overlay.destroy();
        }
        if (selectionAnimator.isRunning()) {
            selectionAnimator.cancel();
        }
    }

    public MaterialChip text(String text) {
        this.textSupplier = () -> text;
        return this;
    }

    public MaterialChip text(Supplier<String> supplier) {
        this.textSupplier = supplier == null ? () -> "" : supplier;
        return this;
    }

    public MaterialChip leadingIcon(MaterialSymbol icon) {
        this.leadingIconSupplier = () -> icon;
        this.avatarSupplier = () -> null;
        return this;
    }

    public MaterialChip leadingIcon(Supplier<MaterialSymbol> supplier) {
        this.leadingIconSupplier = supplier == null ? () -> null : supplier;
        this.avatarSupplier = () -> null;
        return this;
    }

    public MaterialChip trailingIcon(MaterialSymbol icon) {
        this.trailingIconSupplier = () -> icon;
        return this;
    }

    public MaterialChip trailingIcon(Supplier<MaterialSymbol> supplier) {
        this.trailingIconSupplier = supplier == null ? () -> null : supplier;
        return this;
    }

    public MaterialChip avatar(MaterialSymbol avatar) {
        this.avatarSupplier = () -> avatar;
        this.leadingIconSupplier = () -> null;
        return this;
    }

    public MaterialChip avatar(Supplier<MaterialSymbol> supplier) {
        this.avatarSupplier = supplier == null ? () -> null : supplier;
        this.leadingIconSupplier = () -> null;
        return this;
    }

    public MaterialChip elevated(boolean elevated) {
        style().elevated(elevated);
        return this;
    }

    public boolean isElevated() {
        return style().elevated();
    }

    public MaterialChip selected(boolean selected) {
        setSelected(selected);
        return this;
    }

    public MaterialChip setSelected(boolean selected) {
        if (this.selected == selected) {
            return this;
        }
        this.selected = selected;
        selectionAnimator
                .fromTo(selectionAnimator.get(), selected ? 1f : 0f)
                .start();
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    public MaterialChip onTrailingIconClick(Consumer<MaterialChip> action) {
        this.trailingIconAction = action;
        return this;
    }

    public MaterialChip onRemove(Consumer<MaterialChip> action) {
        return onTrailingIconClick(action);
    }

    public boolean isDragged() {
        return dragged;
    }

    @Override
    public boolean hitTest(Vector2f absolutePos) {
        if (super.hitTest(absolutePos)) {
            return true;
        }
        if (trailingIconSupplier.get() == null || trailingIconAction == null) {
            return false;
        }

        Transform fullTransform = getFullTransform();
        Vector2f localPos = fullTransform.isIdentity()
                ? absolutePos
                : fullTransform.inverseTransformPoint(absolutePos);
        Rectangle bounds = getRawBounds();
        float centerX = bounds.x + bounds.width - 8f - ICON_SIZE / 2f;
        float centerY = bounds.getCenterY();
        float halfTarget = getTrailingTouchTargetWidth() / 2f;
        return localPos.x >= centerX - halfTarget
                && localPos.x <= centerX + halfTarget
                && localPos.y >= centerY - halfTarget
                && localPos.y <= centerY + halfTarget;
    }

    private void onPress(MouseEvent.MousePressEvent event) {
        if (event.getButton() != MouseButton.Left.id() || isDisabled() || !isVisible()) {
            return;
        }

        pressOnTrailingIcon = trailingIconSupplier.get() != null && isInTrailingSlot(event.getMousePosition());
        if (pressOnTrailingIcon && trailingIconAction != null) {
            return;
        }

        eventBus.post(new WidgetEvent.ClickEvent<>(this));
        if (supportsSelection()) {
            toggleSelected();
        }
    }

    private void onRelease(MouseEvent.MouseReleaseEvent event) {
        if (pressOnTrailingIcon && trailingIconAction != null
                && isInTrailingSlot(event.getMousePosition())) {
            trailingIconAction.accept(this);
        }
        pressOnTrailingIcon = false;
        dragged = false;
    }

    private void onDrag(MouseEvent.MouseDragEvent event) {
        if (event.getButton() == MouseButton.Left.id() && isPressed()) {
            dragged = true;
        }
    }

    private void toggleSelected() {
        boolean oldSelected = selected;
        setSelected(!selected);
        eventBus.post(new WidgetEvent.ChangeEvent<>(oldSelected, selected));
        eventBus.post(new WidgetEvent.InputEvent<>(oldSelected, selected));
    }

    private boolean supportsSelection() {
        return style().type() == MaterialChipType.Filter || style().type() == MaterialChipType.Input;
    }

    private boolean isInTrailingSlot(Vector2f mousePosition) {
        Rectangle bounds = getRawBounds();
        float centerX = bounds.x + bounds.width - 8f - ICON_SIZE / 2f;
        float centerY = bounds.getCenterY();
        float halfTarget = getTrailingTouchTargetWidth() / 2f;
        return mousePosition.x >= centerX - halfTarget
                && mousePosition.x <= centerX + halfTarget
                && mousePosition.y >= centerY - halfTarget
                && mousePosition.y <= centerY + halfTarget;
    }

    private float getLeadingStartPadding() {
        return avatarSupplier.get() != null
                ? AVATAR_START_PADDING
                : hasLeadingIcon()
                ? ICON_PADDING
                : hasTrailingIcon()
                ? ICON_PADDING
                : TEXT_PADDING;
    }

    private float getTrailingContentEnd() {
        return hasTrailingIcon()
                ? 8f + ICON_SIZE
                : 8f;
    }

    private float getTrailingTouchTargetWidth() {
        return style().type() == MaterialChipType.Input
                ? CLOSE_TOUCH_TARGET
                : ICON_SIZE;
    }

    private boolean hasLeadingIcon() {
        return avatarSupplier.get() != null || leadingIconSupplier.get() != null;
    }

    private boolean hasTrailingIcon() {
        return trailingIconSupplier.get() != null;
    }

    private void updateRectangle(RenderContext ctx) {
        float textWidth = ctx == null
                ? 0f
                : ctx.measureTextWidth(
                        textSupplier.get() == null ? "" : textSupplier.get(),
                        LABEL_FONT_SIZE,
                        LABEL_LINE_HEIGHT,
                        LABEL_WEIGHT
                );
        float width = textWidth;
        boolean hasLeading = hasLeadingIcon();
        boolean hasTrailing = hasTrailingIcon();

        if (!hasLeading && !hasTrailing) {
            width += TEXT_PADDING * 2f;
        } else {
            width += getLeadingStartPadding() + 8f;
            if (hasLeading) {
                width += avatarSupplier.get() != null
                        ? AVATAR_SIZE + ICON_TEXT_GAP
                        : ICON_SIZE + ICON_TEXT_GAP;
            }
            if (hasTrailing) {
                width += ICON_TEXT_GAP + ICON_SIZE;
            }
        }
        setElementSize(width, CONTAINER_HEIGHT);
    }

    private int getElevation() {
        if (isDisabled() || !style().elevated()) {
            return 0;
        }
        if (dragged) {
            return 4;
        }
        if (isPressed() || isFocused()) {
            return 1;
        }
        return isHovered() ? 2 : 1;
    }

    private ChipColors getChipColors() {
        float selectionProgress = supportsSelection()
                ? Math.max(0f, Math.min(1f, selectionAnimator.get()))
                : 0f;
        ChipColors colors = new ChipColors();

        if (isDisabled()) {
            colors.background = (selectionProgress > 0f || style().elevated())
                    ? scheme().onSurface().copy().alpha((int) (255f * 0.12f))
                    : null;
            colors.outline = selectionProgress > 0f
                    ? null
                    : scheme().onSurface().copy().alpha((int) (255f * 0.12f));
            colors.label = scheme().onSurface();
            colors.leadingIcon = scheme().onSurface();
            colors.trailingIcon = scheme().onSurface();
            colors.stateLayer = scheme().onSurface();
            return colors;
        }

        Color unselectedLabel = style().type() == MaterialChipType.Assist
                ? scheme().onSurface()
                : scheme().onSurfaceVariant();
        Color unselectedIcon = style().type() == MaterialChipType.Assist
                || style().type() == MaterialChipType.Suggestion
                ? scheme().primary()
                : scheme().onSurfaceVariant();
        Color selectedContent = scheme().onSecondaryContainer();
        colors.label = Color.lerp(unselectedLabel, selectedContent, selectionProgress);
        colors.leadingIcon = Color.lerp(unselectedIcon, selectedContent, selectionProgress);
        colors.trailingIcon = Color.lerp(unselectedIcon, selectedContent, selectionProgress);
        colors.stateLayer = colors.label;
        colors.background = style().elevated() ? scheme().surfaceContainerLow() : null;
        colors.selectedBackground = selectionProgress <= 0f
                ? null
                : scheme().secondaryContainer().copy().alpha((int) (255f * selectionProgress));
        colors.outline = style().elevated()
                ? null
                : scheme().outline().copy().alpha((int) (255f * (1f - selectionProgress)));
        return colors;
    }

    protected static class ChipColors {
        Color background;
        Color selectedBackground;
        Color outline;
        Color label;
        Color leadingIcon;
        Color trailingIcon;
        Color stateLayer;
    }
}
