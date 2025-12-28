/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
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

package io.homo.superresolution.core.gui.widgets.menu;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.MaterialWidgetOverlay;
import io.homo.superresolution.core.gui.core.MouseButton;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonShape;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import org.joml.Vector2f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaterialMenuItem extends MaterialWidget<MaterialMenuItem> {
    private static final long ANIMATION_DURATION = 200;
    private static final long FADE_DURATION = 200;

    private Supplier<String> textSupplier = () -> "";
    private Supplier<MaterialSymbol> iconSupplier = () -> null;
    private Supplier<MaterialSymbol> rightIconSupplier = () -> null;
    private boolean selected = false;
    private boolean selectable = false;
    private Object value = null;
    private Consumer<Boolean> onSelectionChanged;

    private final Animator.FloatAnimator fadeAnimator = Animator.ofFloat(1f, 1f)
            .duration(FADE_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());

    private boolean fadeInStarted = false;
    private boolean fadeOutStarted = false;

    private final Animator.FloatAnimator selectionAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());

    private final MaterialWidgetOverlay<MaterialMenuItem> overlay = new MaterialWidgetOverlay<>(this) {
        private void drawPath(IUIDrawContext drawContext, MaterialMenuItem widget) {
            Rectangle bounds = getRawBounds();
            float baseRadius = 4;
            float selectedRadius = 12;
            float animProgress = selectionAnimator.get();

            float topLeft = baseRadius;
            float topRight = baseRadius;
            float bottomLeft = baseRadius;
            float bottomRight = baseRadius;

            if (widget.getParent() instanceof MaterialMenuGroup group) {
                int itemIndex = group.getChildren().indexOf(widget);
                if (itemIndex == group.getChildren().size() - 1) {
                    if (group.getParent() instanceof MaterialMenu menu) {
                        int groupIndex = menu.getChildren().indexOf(group);
                        if (groupIndex == menu.getChildren().size() - 1) {
                            bottomLeft = selectedRadius;
                            bottomRight = selectedRadius;
                        }
                    }
                } else if (itemIndex == 0) {
                    if (group.getParent() instanceof MaterialMenu menu) {
                        int groupIndex = menu.getChildren().indexOf(group);
                        if (groupIndex == 0) {
                            topLeft = selectedRadius;
                            topRight = selectedRadius;
                        }
                    }
                }
            }

            if (selectable && animProgress > 0) {
                topLeft = topLeft + (selectedRadius - topLeft) * animProgress;
                topRight = topRight + (selectedRadius - topRight) * animProgress;
                bottomLeft = bottomLeft + (selectedRadius - bottomLeft) * animProgress;
                bottomRight = bottomRight + (selectedRadius - bottomRight) * animProgress;
            }

            drawContext.beginPath();
            drawContext.roundedRectComplex(bounds.x, bounds.y, bounds.width, bounds.height, bottomLeft, bottomRight,
                    topLeft, topRight);
        }

        @Override
        protected void drawShape(IUIDrawContext drawContext, MaterialMenuItem widget, Color color) {
            drawPath(drawContext, widget);
            drawContext.fillColor(color);
            drawContext.endPath(true);
        }

        @Override
        protected void drawShape(IUIDrawContext drawContext, MaterialMenuItem widget, IPaint paint) {
            drawPath(drawContext, widget);
            drawContext.paint(paint);
            drawContext.endPath(true);
        }
    };

    public MaterialMenuItem() {
        this.style = new MaterialMenuItemStyle();
        updateSize();
    }

    public static MaterialMenuItem create() {
        return new MaterialMenuItem();
    }

    @Override
    public MaterialMenuItemStyle style() {
        return (MaterialMenuItemStyle) super.style();
    }

    private void onPress(MouseEvent.MousePressEvent event) {
        if (event.getButton() == MouseButton.Left.id()) {
            if (isVisible() && !isDisabled()) {
                eventBus.post(new WidgetEvent.ClickEvent<>(this));
            }
        }
    }

    private void onRelease(MouseEvent.MouseReleaseEvent event) {
    }

    private void _onClick(WidgetEvent.ClickEvent<MaterialMenuItem> event) {
        if (selectable) {
            handleSelectionClick();
        }
    }

    @Override
    protected void init() {
        eventBus.addListener(this::onPress);
        eventBus.addListener(this::onRelease);
        eventBus.addListener(this::_onClick);
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    private void handleSelectionClick() {
        if (getParent() instanceof MaterialMenuGroup group) {
            if (group.getParent() instanceof MaterialMenu menu) {
                menu.handleItemSelection(this);
            }
        }
    }

    public MaterialMenuItem text(String text) {
        this.textSupplier = () -> text;
        return this;
    }

    public MaterialMenuItem icon(MaterialSymbol icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public MaterialMenuItem rightIcon(MaterialSymbol icon) {
        this.rightIconSupplier = () -> icon;
        return this;
    }

    public MaterialMenuItem selectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public MaterialMenuItem selected(boolean selected) {
        setSelectedInternal(selected, true);
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    void setSelectedInternal(boolean selected, boolean notifyListener) {
        boolean oldSelected = this.selected;
        this.selected = selected;
        if (oldSelected != selected) {
            if (selected) {
                selectionAnimator.fromTo(selectionAnimator.get(), 1f).start();
            } else {
                selectionAnimator.fromTo(selectionAnimator.get(), 0f).start();
            }
            if (onSelectionChanged != null && notifyListener) {
                onSelectionChanged.accept(selected);
            }
        }
    }

    public MaterialMenuItem value(Object value) {
        this.value = value;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public MaterialMenuItem onSelectionChanged(Consumer<Boolean> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        return this;
    }

    void startFadeIn() {
        if (!fadeInStarted) {
            fadeInStarted = true;
            fadeOutStarted = false;
            fadeAnimator.fromTo(fadeAnimator.get(), 1f).start();
        }
    }

    void startFadeOut() {
        if (!fadeOutStarted) {
            fadeOutStarted = true;
            fadeInStarted = false;
            fadeAnimator.fromTo(fadeAnimator.get(), 0f).start();
        }
    }

    void resetFadeState(boolean visible) {
        fadeInStarted = visible;
        fadeOutStarted = !visible;
        fadeAnimator.set(visible ? 1f : 0f);
    }

    public float getFadeProgress() {
        return fadeAnimator.get();
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        updateSize();
        selectionAnimator.update();
        fadeAnimator.update();
        overlay.update();
        if (!isVisible())
            return;

        float fadeProgress = fadeAnimator.get();
        if (fadeProgress <= 0)
            return;

        float animProgress = selectionAnimator.get();

        drawContext.beginBatch();
        float savedAlpha = drawContext.globalAlpha();
        drawContext.globalAlpha(savedAlpha * fadeProgress);
        MaterialMenuItemSize size = style().size();
        Color contentColor = style().colors().itemText(scheme());
        Color iconColor = style().colors().itemIcon(scheme());
        Color selectedContentColor = style().colors().selectedItemText(scheme());
        Color selectedIconColor = style().colors().selectedItemIcon(scheme());
        Rectangle bounds = getRawBounds();

        if (selectable && animProgress > 0) {
            int alpha = isDisabled() ? (int) (0.15 * 255) : (int) (animProgress * 0.8f * 255);
            drawContext.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    12,
                    style().colors().selectedItemBackground(scheme()).copy().alpha(alpha),
                    true);
            contentColor = contentColor.copy().lerp(selectedContentColor, animProgress);
            iconColor = iconColor.copy().lerp(selectedIconColor, animProgress);
        }

        overlay.render(drawContext, style().colors().stateLayer(scheme()), style().colors().stateLayer(scheme()));

        float checkIconOffset = size.iconSize() + size.iconTextGap();
        float textOffset = animProgress * checkIconOffset;

        float currentX = bounds.x + size.horizontalPadding();
        drawContext.save();
        drawContext.transform().push();
        float lastAlpha = drawContext.globalAlpha();
        if (isDisabled())
            drawContext.globalAlpha(0.38f * lastAlpha);
        if (selectable && animProgress > 0) {
            MaterialSymbol checkIcon = MaterialSymbols.iconCheck();
            float iconCenterX = currentX + size.iconSize() / 2f;
            float iconCenterY = bounds.y + bounds.height / 2f;
            Color checkIconColor = iconColor.copy().alpha((int) (animProgress * 255));
            checkIcon.render(drawContext, checkIconColor, size.iconSize(), new Vector2f(iconCenterX, iconCenterY));
        }

        float textX = currentX + textOffset;

        MaterialSymbol userIcon = iconSupplier.get();
        if (userIcon != null && animProgress < 1) {
            float iconAlpha = 1 - animProgress;
            float iconCenterX = currentX + size.iconSize() / 2f;
            float iconCenterY = bounds.y + bounds.height / 2f;
            Color userIconColor = iconColor.copy().alpha((int) (iconAlpha * 255));
            userIcon.render(drawContext, userIconColor, size.iconSize(), new Vector2f(iconCenterX, iconCenterY));
            if (animProgress < 1) {
                textX = currentX + size.iconSize() + size.iconTextGap();
            }
        } else if (userIcon == null) {
            textX = currentX + textOffset;
        }

        String text = textSupplier.get();
        if (text != null && !text.isEmpty()) {
            drawContext.drawAlignedText(
                    drawContext.font(),
                    size.fontSize(),
                    text,
                    textX,
                    bounds.y + bounds.height / 2f,
                    bounds.width,
                    size.fontSize(),
                    contentColor,
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false);
        }

        MaterialSymbol rightIcon = rightIconSupplier.get();
        if (rightIcon != null) {
            float iconCenterX = bounds.x + bounds.width - size.horizontalPadding() - size.iconSize() / 2f;
            float iconCenterY = bounds.y + bounds.height / 2f;
            rightIcon.render(drawContext, iconColor, size.iconSize(), new Vector2f(iconCenterX, iconCenterY));
        }
        drawContext.globalAlpha(savedAlpha);
        drawContext.transform().pop();
        drawContext.restore();
        drawContext.endBatch(getZIndex());
    }

    public float computeContentWidth() {
        MaterialMenuItemSize size = style().size();
        NanoVG.context.save();
        NanoVG.context.fontSize(size.fontSize());
        float textWidth = NanoVG.RENDERER.TEXT.measureTextWidth(textSupplier.get(), size.fontSize());
        NanoVG.context.restore();

        float iconWidth = 0;
        if (iconSupplier.get() != null) {
            iconWidth = size.iconSize();
        }

        float rightIconWidth = 0;
        if (rightIconSupplier.get() != null) {
            rightIconWidth = size.iconSize();
        }

        return size.horizontalPadding() +
                iconWidth +
                (iconWidth == 0 ? 0 : size.iconTextGap()) +
                textWidth +
                (rightIconWidth == 0 ? 0 : size.iconTextGap()) +
                rightIconWidth +
                size.horizontalPadding();
    }

    private void updateSize() {
        MaterialMenuItemSize size = style().size();
        layout().setWidthPercent(100);
        layout().setHeight(size.height());
        layout().setFlexShrink(0);
    }

    @Override
    public void destroy() {
        if (overlay != null) {
            overlay.destroy();
        }
    }
}
