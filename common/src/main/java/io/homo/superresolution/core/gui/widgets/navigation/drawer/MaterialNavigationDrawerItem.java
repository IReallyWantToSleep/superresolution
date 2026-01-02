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

package io.homo.superresolution.core.gui.widgets.navigation.drawer;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialWidgetOverlay;
import io.homo.superresolution.core.gui.core.MouseButton;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.IPaint;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlign;
import io.homo.superresolution.core.gui.core.backends.interfaces.TextAlignType;
import io.homo.superresolution.core.gui.core.event.events.MouseEvent;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import org.joml.Vector2f;

import java.util.function.Supplier;

public class MaterialNavigationDrawerItem extends MaterialWidget<MaterialNavigationDrawerItem> {
    private static final float HEIGHT = 56f;
    private static final float ICON_SIZE = 24f;
    private static final float FONT_SIZE = 14f;
    private static final float ICON_MARGIN_LEFT = 16f;
    private static final float ICON_TEXT_GAP = 12f;
    private static final float CORNER_RADIUS = 28f;
    private static final long ANIMATION_DURATION = 200;

    private Supplier<String> textSupplier = () -> "";
    private Supplier<MaterialSymbol> iconSupplier = () -> null;
    private boolean selected = false;
    private Object value = null;

    private final Animator.FloatAnimator selectionAnimator = Animator.ofFloat(0f, 0f)
            .duration(ANIMATION_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());

    private final MaterialWidgetOverlay<MaterialNavigationDrawerItem> overlay = new MaterialWidgetOverlay<>(this) {
        @Override
        protected void drawShape(RenderContext ctx, MaterialNavigationDrawerItem widget, Color color) {
            Rectangle bounds = getRawBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CORNER_RADIUS,
                    color,
                    true
            );
        }

        @Override
        protected void drawShape(RenderContext ctx, MaterialNavigationDrawerItem widget, IPaint paint) {
            Rectangle bounds = getRawBounds();
            ctx.beginPath();
            ctx.paint(paint);
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CORNER_RADIUS
            );
            ctx.endPath(true);
        }
    };

    public MaterialNavigationDrawerItem() {
        getLayoutNode().setDebugName("NavigationDrawerItem");
        layout().setHeight(HEIGHT);
        layout().setWidthPercent(100);
    }

    public static MaterialNavigationDrawerItem create() {
        return new MaterialNavigationDrawerItem();
    }

    public static MaterialNavigationDrawerItem create(String text, MaterialSymbol icon) {
        return new MaterialNavigationDrawerItem().text(text).icon(icon);
    }

    @Override
    protected void init() {
        eventBus.addListener(this::onPress);
        eventBus.addListener(this::_onClick);
    }

    private void onPress(MouseEvent.MousePressEvent event) {
        if (event.getButton() == MouseButton.Left.id()) {
            if (isVisible() && !isDisabled()) {
                eventBus.post(new WidgetEvent.ClickEvent<>(this));
            }
        }
    }

    private void _onClick(WidgetEvent.ClickEvent<MaterialNavigationDrawerItem> event) {
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    public MaterialNavigationDrawerItem text(String text) {
        this.textSupplier = () -> text;
        return this;
    }

    public MaterialNavigationDrawerItem text(Supplier<String> supplier) {
        this.textSupplier = supplier;
        return this;
    }

    public MaterialNavigationDrawerItem icon(MaterialSymbol icon) {
        this.iconSupplier = () -> icon;
        return this;
    }

    public MaterialNavigationDrawerItem icon(Supplier<MaterialSymbol> supplier) {
        this.iconSupplier = supplier;
        return this;
    }

    public MaterialNavigationDrawerItem value(Object value) {
        this.value = value;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public boolean isSelected() {
        return selected;
    }

    public MaterialNavigationDrawerItem setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            if (selected) {
                selectionAnimator.fromTo(selectionAnimator.get(), 1f).duration(ANIMATION_DURATION).start();
            } else {
                selectionAnimator.fromTo(selectionAnimator.get(), 0f).duration(ANIMATION_DURATION).start();
            }
        }
        return this;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        Rectangle bounds = getBounds();
        overlay.update();
        selectionAnimator.update();

        float animProgress = selectionAnimator.get();

        if (animProgress > 0) {
            Color selectedBg = scheme().secondaryContainer().copy().alpha((int) (255 * animProgress));
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CORNER_RADIUS,
                    selectedBg,
                    true
            );
        }


        float contentX = bounds.x + ICON_MARGIN_LEFT;
        float centerY = bounds.y + bounds.height / 2;

        MaterialSymbol icon = iconSupplier.get();
        if (icon != null) {
            Color iconColor = selected ? scheme().onSecondaryContainer() : scheme().onSurfaceVariant();
            Vector2f iconPos = new Vector2f(contentX + ICON_SIZE / 2, centerY);
            icon.render(ctx, iconColor, ICON_SIZE, iconPos);
            contentX += ICON_SIZE + ICON_TEXT_GAP;
        }

        String text = textSupplier.get();
        if (text != null && !text.isEmpty()) {
            Color textColor = selected ? scheme().onSecondaryContainer() : scheme().onSurface();
            ctx.drawAlignedText(
                    ctx.font(),
                    FONT_SIZE,
                    text,
                    contentX,
                    bounds.getCenterY(),
                    bounds.width - contentX + bounds.x,
                    bounds.height,
                    textColor,
                    TextAlign.of(TextAlignType.ALIGN_LEFT, TextAlignType.ALIGN_MIDDLE),
                    false
            );
        }
        overlay.render(ctx, scheme().onSecondaryContainer().copy(), scheme().onSurface().copy());
    }
}
