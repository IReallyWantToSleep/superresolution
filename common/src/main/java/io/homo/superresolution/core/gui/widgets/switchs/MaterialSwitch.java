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

package io.homo.superresolution.core.gui.widgets.switchs;

import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.*;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;
import org.joml.Vector2f;

public class MaterialSwitch extends MaterialWidget<MaterialSwitch> {
    protected Animator.FloatAnimator hoverAnimator;
    protected Animator.FloatAnimator pressAnimator;
    protected Animator.FloatAnimator handlePositionAnimator;
    protected Animator.FloatAnimator changeAnimator;
    protected Animator.FloatAnimator handleSizeAnimator;
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
        if (checked != isChecked())
            toggleChecked();
        return this;
    }

    @Override
    protected boolean isInteractive() {
        return true;
    }

    public MaterialSwitch toggleChecked() {
        boolean newChecked = !this.checked;
        eventBus.post(new WidgetEvent.ChangeEvent<>(newChecked));
        if (newChecked) {
            // 打开开关
            handlePositionAnimator
                    .timeInterpolator(new BezierInterpolator(0.175, 0.885, 0.32, 1.275))
                    .duration(250)
                    .to(getBounds().width - 32)
                    .start();
            handleSizeAnimator
                    .timeInterpolator(new BezierInterpolator(0.2, 0, 0, 1))
                    .duration(200)
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
                    .duration(250)
                    .to(0f)
                    .start();
            handleSizeAnimator
                    .timeInterpolator(new BezierInterpolator(0.2, 0, 0, 1))
                    .duration(200)
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
                .duration(200)
                .fromTo(0f, 1f)
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

        float initialHandleSize = (style().showUncheckedIconWhenEnable() || style().showUncheckedIconAlways())
                ? MaterialSwitchSize.Default.handleSizeWithIcon()
                : MaterialSwitchSize.Default.handleSize();
        this.handleSizeAnimator = new Animator.FloatAnimator();
        this.handleSizeAnimator.set(initialHandleSize);

        onHover((event) -> onHover(event.getMousePosition(), event.isHovering()));
        onMouseRelease((event) -> onRelease(event.getMousePosition()));
        onMousePress((event) -> onPress(event.getMousePosition()));
    }

    private void updateRectangle() {
        setElementSize(MaterialSwitchSize.Default.trackWidth(), MaterialSwitchSize.Default.trackHeight());

    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        Animator.updateAll(
                hoverAnimator,
                pressAnimator,
                handlePositionAnimator,
                changeAnimator,
                handleSizeAnimator);
        drawContext.beginBatch();
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
        drawContext.roundedRect(
                bounds.x,
                bounds.y,
                MaterialSwitchSize.Default.trackWidth(),
                MaterialSwitchSize.Default.trackHeight(),
                MaterialSwitchSize.Default.trackHeight() / 2,
                colors.trackColor,
                true);

        if (!isChecked()) {
            drawContext.beginPath();
            drawContext.strokeColor(
                    isDisabled() ? scheme().onSurface().copy().alpha((int) (255 * 0.08))
                            : scheme().outline());
            drawContext.strokeWidth(MaterialSwitchSize.Default.trackOutlineWidth());
            drawContext.roundedRect(
                    bounds.x,
                    bounds.y,
                    MaterialSwitchSize.Default.trackWidth(),
                    MaterialSwitchSize.Default.trackHeight(),
                    MaterialSwitchSize.Default.trackHeight() / 2);
            drawContext.endPath(false);
        }
        float handleSize = handleSizeAnimator.get();
        float handleX = bounds.x + 16 + handlePositionAnimator.get();

        drawContext.arc(
                handleX,
                bounds.getCenterY(),
                handleSize / 2,
                colors.handleColor,
                true);

        if (isHovered() || hoverAnimator.get() > 0.001) {
            drawContext.arc(
                    handleX,
                    bounds.getCenterY(),
                    20,
                    scheme().onSurface().copy()
                            .alpha((int) (0.1 * 255 * hoverAnimator.get())),
                    true);
        }
        if ((isChecked() && (style().showCheckedIconWhenEnable() && isChecked()
                || style().showCheckedIconAlways()))) {
            float checkedIconX = bounds.x + bounds.width - 16;
            MaterialSymbols.iconCheck().render(
                    drawContext,
                    colors.iconColor.copy().alpha(
                            !handlePositionAnimator.isRunning() ? 255
                                    : Math.min((int) ((handlePositionAnimator
                                            .progress() * 1.8) * 255),
                                    255)),
                    MaterialSwitchSize.Default.iconSize(),
                    new Vector2f(
                            checkedIconX,
                            bounds.getCenterY()));
        }
        if ((!isChecked() && (style().showUncheckedIconWhenEnable() && !isChecked()
                || style().showUncheckedIconAlways()))) {
            float closeIconX = bounds.x + 16;
            float alpha = isDisabled() ? 1
                    : clamp(!handlePositionAnimator.isRunning() ? 255f
                            : Math.min(((handlePositionAnimator.progress() * 1.8f)
                            * 255f), 255f) / 255f,
                    0, 1);
            MaterialSymbols.iconClose().render(
                    drawContext,
                    colors.iconColor,
                    MaterialSwitchSize.Default.iconSize(),
                    new Vector2f(
                            closeIconX,
                            bounds.getCenterY()));
        }
        drawContext.endBatch(getZIndex());
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    private SwitchColors getSwitchColors() {
        SwitchColors colors = new SwitchColors();
        colors.trackColor = isDisabled()
                ? (isChecked() ? scheme().onSurface().copy().alpha((int) (255 * 0.1))
                : scheme().surfaceVariant()).copy().alpha((int) (255 * 0.1))
                : (isChecked() ? scheme().primary() : scheme().surfaceContainerHighest());
        colors.handleColor = isDisabled()
                ? (isChecked() ? scheme().surface()
                : scheme().onSurface().copy().alpha((int) (255 * 0.38)))
                : (isChecked() ? scheme().onPrimary() : scheme().outline());
        colors.iconColor = isDisabled()
                ? (isChecked() ? scheme().surfaceContainerHighest().copy().alpha((int) (0 * 0.38))
                : scheme().surfaceContainerHighest().copy().alpha((int) (255 * 0.38)))
                : (isChecked() ? scheme().primary() : scheme().surfaceContainerHighest());
        return colors;
    }

    private void onHover(Vector2f mousePosition, boolean hover) {
        if (hover) {
            hoverAnimator
                    .timeInterpolator(TimeInterpolator.linear())
                    .to(1f)
                    .duration(200)
                    .start();
            MouseCursor.HAND.use();
        } else {
            hoverAnimator
                    .timeInterpolator(TimeInterpolator.linear())
                    .to(0f)
                    .duration(200)
                    .start();
            MouseCursor.ARROW.use();
        }

    }

    @Override
    public MaterialSwitchStyle style() {
        return (MaterialSwitchStyle) style;
    }

    private void onPress(Vector2f mousePosition) {
        handleSizeAnimator
                .timeInterpolator(TimeInterpolator.linear())
                .duration(150)
                .to((style().showCheckedIconWhenEnable() && isChecked()) || style()
                        .showCheckedIconAlways() ? MaterialSwitchSize.Default
                        .handleSizePressWithIcon()
                        : MaterialSwitchSize.Default
                        .handleSizePress())
                .start();
        hoverAnimator
                .timeInterpolator(new BezierInterpolator(0.2, 0, 0, 1))
                .duration(200)
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
                    .duration(200)
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
    }
}