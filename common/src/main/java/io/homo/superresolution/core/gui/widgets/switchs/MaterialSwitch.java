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

package io.homo.superresolution.core.gui.widgets.switchs;

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.BezierInterpolator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import org.joml.Vector2f;

public class MaterialSwitch extends MaterialWidget<MaterialSwitch> {
    private static final long COLOR_TRANSITION_DURATION = 67;
    private static final long ICON_OPACITY_DURATION = 33;
    private static final long HANDLE_POSITION_DURATION = 300;
    private static final long HANDLE_SIZE_DURATION = 250;
    private static final long PRESSED_HANDLE_SIZE_DURATION = 100;

    protected Animator.FloatAnimator hoverAnimator;
    protected Animator.FloatAnimator pressAnimator;
    protected Animator.FloatAnimator handlePositionAnimator;
    protected Animator.FloatAnimator changeAnimator;
    protected Animator.FloatAnimator handleSizeAnimator;
    protected Animator.FloatAnimator iconOpacityAnimator;
    private boolean checked;

    public MaterialSwitch() {
    }

    public static MaterialSwitch create() {
        return new MaterialSwitch();
    }

    public boolean isChecked() {
        return checked;
    }

    public MaterialSwitch setChecked(boolean checked) {
        if (checked != isChecked()) {
            this.checked = checked;
            if (handlePositionAnimator != null) {
                handlePositionAnimator.cancel();
                handlePositionAnimator.set(checked ? getBounds().width - 32 : 0f);
            }
            if (handleSizeAnimator != null) {
                handleSizeAnimator.cancel();
                handleSizeAnimator.set(checked
                        ? ((style().showCheckedIconWhenEnable() || style().showCheckedIconAlways())
                           ? MaterialSwitchSize.Default.handleSizeCheckedWithIcon()
                           : MaterialSwitchSize.Default.handleSizeChecked())
                        : ((style().showUncheckedIconWhenEnable() || style().showUncheckedIconAlways())
                           ? MaterialSwitchSize.Default.handleSizeWithIcon()
                           : MaterialSwitchSize.Default.handleSize()));
            }
            if (changeAnimator != null) {
                changeAnimator.cancel();
                changeAnimator.set(checked ? 1f : 0f);
            }
            if (iconOpacityAnimator != null) {
                iconOpacityAnimator.cancel();
                iconOpacityAnimator.set(checked ? 1f : 0f);
            }
        }
        return this;
    }

    public MaterialSwitch toggleChecked() {
        boolean newChecked = !this.checked;
        eventBus.post(new WidgetEvent.ChangeEvent<>(!newChecked, newChecked));
        eventBus.post(new WidgetEvent.InputEvent<>(!newChecked, newChecked));
        if (newChecked) {
            // 打开开关
            handlePositionAnimator
                    .timeInterpolator(new BezierInterpolator(0.175, 0.885, 0.32, 1.275))
                    .duration(HANDLE_POSITION_DURATION)
                    .to(getBounds().width - 32)
                    .start();
            handleSizeAnimator
                    .timeInterpolator(new BezierInterpolator(0.2, 0, 0, 1))
                    .duration(HANDLE_SIZE_DURATION)
                    .to(
                            (style().showCheckedIconWhenEnable()
                                    || style().showCheckedIconAlways())
                                    ? MaterialSwitchSize.Default
                                      .handleSizeCheckedWithIcon()
                                    : MaterialSwitchSize.Default
                                      .handleSizeChecked())
                    .start();
        } else {
            // 关闭开关
            handlePositionAnimator
                    .timeInterpolator(new BezierInterpolator(0.175, 0.885, 0.32, 1.275))
                    .duration(HANDLE_POSITION_DURATION)
                    .to(0f)
                    .start();
            handleSizeAnimator
                    .timeInterpolator(new BezierInterpolator(0.2, 0, 0, 1))
                    .duration(HANDLE_SIZE_DURATION)
                    .to(
                            (style().showUncheckedIconWhenEnable()
                                    || style().showUncheckedIconAlways())
                                    ? MaterialSwitchSize.Default
                                      .handleSizeWithIcon()
                                    : MaterialSwitchSize.Default
                                      .handleSize())
                    .start();
        }

        changeAnimator
                .timeInterpolator(TimeInterpolator.linear())
                .duration(COLOR_TRANSITION_DURATION)
                .fromTo(changeAnimator.get(), newChecked ? 1f : 0f)
                .start();
        iconOpacityAnimator
                .timeInterpolator(TimeInterpolator.linear())
                .duration(ICON_OPACITY_DURATION)
                .fromTo(iconOpacityAnimator.get(), newChecked ? 1f : 0f)
                .start();
        this.checked = newChecked;
        return this;
    }

    @Override
    protected void init() {
        this.style = new MaterialSwitchStyle();
        updateRectangle();
        getLayoutNode().setDebugName("MaterialSwitch");
        this.hoverAnimator = new Animator.FloatAnimator();
        this.hoverAnimator.set(0f);

        this.pressAnimator = new Animator.FloatAnimator();
        this.pressAnimator.set(0f);

        this.handlePositionAnimator = new Animator.FloatAnimator();
        this.handlePositionAnimator.set(isChecked() ? getBounds().width - 32 : 0f);

        this.changeAnimator = new Animator.FloatAnimator();
        this.changeAnimator.set(0f);

        this.iconOpacityAnimator = new Animator.FloatAnimator();
        this.iconOpacityAnimator.set(0f);

        float initialHandleSize = (style().showUncheckedIconWhenEnable() || style().showUncheckedIconAlways())
                ? MaterialSwitchSize.Default.handleSizeWithIcon()
                : MaterialSwitchSize.Default.handleSize();
        this.handleSizeAnimator = new Animator.FloatAnimator();
        this.handleSizeAnimator.set(initialHandleSize);

        onHover((event) -> onHover(event.getMousePosition(), event.isHovering()));
        onMouseRelease((event) -> onRelease(event.getMousePosition()));
        onMousePress((event) -> onPress(event.getMousePosition()));
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateRectangle();
    }

    @Override
    public MaterialSwitchStyle style() {
        return (MaterialSwitchStyle) style;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        Animator.updateAll(
                hoverAnimator,
                pressAnimator,
                handlePositionAnimator,
                changeAnimator,
                handleSizeAnimator,
                iconOpacityAnimator);
        updateRectangle();
        Rectangle bounds = getBounds();
        if (handleSizeAnimator
                .get() < ((isChecked() && (style().showCheckedIconWhenEnable() && isChecked()
                || style().showCheckedIconAlways())) ||
                (!isChecked() && (style().showUncheckedIconWhenEnable() && !isChecked()
                        || style().showUncheckedIconAlways()))
                ? MaterialSwitchSize.Default
                  .handleSizeWithIcon()
                : MaterialSwitchSize.Default
                  .handleSize())) {
            handleSizeAnimator.set(
                    ((isChecked() && (style().showCheckedIconWhenEnable() && isChecked()
                            || style().showCheckedIconAlways())) ||
                            (!isChecked() && (style().showUncheckedIconWhenEnable()
                                    && !isChecked()
                                    || style().showUncheckedIconAlways()))
                            ? MaterialSwitchSize.Default
                              .handleSizeWithIcon()
                            : MaterialSwitchSize.Default
                              .handleSize()));
        }
        SwitchColors colors = getSwitchColors();
        ctx.beginGroup(style().zIndex());

        ctx.roundedRect(
                bounds.x,
                bounds.y,
                MaterialSwitchSize.Default.trackWidth(),
                MaterialSwitchSize.Default.trackHeight(),
                MaterialSwitchSize.Default.trackHeight() / 2,
                colors.trackColor,
                true);

        if (colors.outlineAlpha > 0.001f) {
            ctx.beginPath();
            ctx.strokeColor(colors.outlineColor.copy()
                    .alpha((int) (colors.outlineColor.alpha() * colors.outlineAlpha)));
            ctx.strokeWidth(MaterialSwitchSize.Default.trackOutlineWidth());
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    MaterialSwitchSize.Default.trackWidth(),
                    MaterialSwitchSize.Default.trackHeight(),
                    MaterialSwitchSize.Default.trackHeight() / 2);
            ctx.endPath(false);
        }
        float handleSize = handleSizeAnimator.get();
        float handleX = bounds.x + 16 + handlePositionAnimator.get();

        ctx.arc(
                handleX,
                bounds.getCenterY(),
                handleSize / 2,
                colors.handleColor,
                true);

        if (!isDisabled() && (isHovered() || hoverAnimator.get() > 0.001)) {
            ctx.arc(
                    handleX,
                    bounds.getCenterY(),
                    20,
                    scheme().onSurface().copy()
                            .alpha((int) (0.1 * 255 * hoverAnimator.get())),
                    true);
        }
        float iconOpacityProgress = getIconOpacityProgress();
        if (style().showCheckedIconWhenEnable() || style().showCheckedIconAlways()) {
            renderIcon(
                    ctx,
                    MaterialSymbols.iconCheck(),
                    colors.iconColor,
                    iconOpacityProgress,
                    handleX,
                    bounds.getCenterY());
        }
        if (style().showUncheckedIconWhenEnable() || style().showUncheckedIconAlways()) {
            renderIcon(
                    ctx,
                    MaterialSymbols.iconClose(),
                    colors.iconColor,
                    1f - iconOpacityProgress,
                    handleX,
                    bounds.getCenterY());
        }
        ctx.endGroup();
    }

    private void updateRectangle() {
        setElementSize(MaterialSwitchSize.Default.trackWidth(), MaterialSwitchSize.Default.trackHeight());
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    private float getCheckedProgress() {
        if (changeAnimator == null || changeAnimator.get() == null) {
            return isChecked() ? 1f : 0f;
        }
        return clamp(changeAnimator.get(), 0f, 1f);
    }

    private float getIconOpacityProgress() {
        if (iconOpacityAnimator == null || iconOpacityAnimator.get() == null) {
            return isChecked() ? 1f : 0f;
        }
        return clamp(iconOpacityAnimator.get(), 0f, 1f);
    }

    private void renderIcon(
            RenderContext ctx,
            MaterialSymbol icon,
            Color color,
            float alpha,
            float x,
            float y) {
        if (alpha <= 0.001f) {
            return;
        }
        icon.render(
                ctx,
                color.copy().alpha((int) (color.alpha() * clamp(alpha, 0f, 1f))),
                MaterialSwitchSize.Default.iconSize(),
                new Vector2f(x, y));
    }

    private SwitchColors getSwitchColors() {
        SwitchColors colors = new SwitchColors();
        float checkedProgress = getCheckedProgress();
        colors.trackColor = Color.lerp(
                getUncheckedTrackColor(),
                getCheckedTrackColor(),
                checkedProgress);
        colors.handleColor = Color.lerp(
                getUncheckedHandleColor(),
                getCheckedHandleColor(),
                checkedProgress);
        colors.iconColor = Color.lerp(
                getUncheckedIconColor(),
                getCheckedIconColor(),
                checkedProgress);
        colors.outlineColor = getUncheckedOutlineColor();
        colors.outlineAlpha = 1f - checkedProgress;
        return colors;
    }

    private Color getCheckedTrackColor() {
        return isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.1))
                : scheme().primary();
    }

    private Color getUncheckedTrackColor() {
        return isDisabled()
                ? scheme().surfaceVariant().copy().alpha((int) (255 * 0.1))
                : scheme().surfaceContainerHighest();
    }

    private Color getCheckedHandleColor() {
        return isDisabled()
                ? scheme().surface()
                : scheme().onPrimary();
    }

    private Color getUncheckedHandleColor() {
        return isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.38))
                : scheme().outline();
    }

    private Color getCheckedIconColor() {
        return isDisabled()
                ? scheme().surfaceContainerHighest().copy().alpha(0)
                : scheme().primary();
    }

    private Color getUncheckedIconColor() {
        return isDisabled()
                ? scheme().surfaceContainerHighest().copy().alpha((int) (255 * 0.38))
                : scheme().surfaceContainerHighest();
    }

    private Color getUncheckedOutlineColor() {
        return isDisabled()
                ? scheme().onSurface().copy().alpha((int) (255 * 0.08))
                : scheme().outline();
    }

    private void onHover(Vector2f mousePosition, boolean hover) {
        if (hover) {
            hoverAnimator
                    .timeInterpolator(TimeInterpolator.linear())
                    .to(1f)
                    .duration(COLOR_TRANSITION_DURATION)
                    .start();
        } else {
            hoverAnimator
                    .timeInterpolator(TimeInterpolator.linear())
                    .to(0f)
                    .duration(COLOR_TRANSITION_DURATION)
                    .start();
        }

    }

    private void onPress(Vector2f mousePosition) {
        handleSizeAnimator
                .timeInterpolator(TimeInterpolator.linear())
                .duration(PRESSED_HANDLE_SIZE_DURATION)
                .to((style().showCheckedIconWhenEnable() && isChecked()) || style()
                        .showCheckedIconAlways() ? MaterialSwitchSize.Default
                                                   .handleSizePressWithIcon()
                        : MaterialSwitchSize.Default
                          .handleSizePress())
                .start();
        hoverAnimator
                .timeInterpolator(new BezierInterpolator(0.2, 0, 0, 1))
                .duration(COLOR_TRANSITION_DURATION)
                .to(1f)
                .start();
        pressAnimator
                .timeInterpolator(TimeInterpolator.linear())
                .duration(200)
                .to(1f)
                .start();
    }

    private void onRelease(Vector2f mousePosition) {
        toggleChecked();
        if (isHovered()) {
            hoverAnimator
                    .timeInterpolator(TimeInterpolator.linear())
                    .duration(COLOR_TRANSITION_DURATION)
                    .to(1f)
                    .start();
        }
        pressAnimator
                .timeInterpolator(new BezierInterpolator(0.2f, 0, 0, 1))
                .to(0f)
                .duration(200)
                .start();
    }

    private static class SwitchColors {
        Color iconColor;
        Color handleColor;
        Color trackColor;
        Color outlineColor;
        float outlineAlpha;
    }
}
