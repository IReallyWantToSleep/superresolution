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

import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.UIDrawContext;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Easing;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.math.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MouseCursor;

import java.util.function.Supplier;

public class MaterialSwitch extends MaterialWidget<MaterialSwitch, MaterialSwitchStyle, MaterialSwitchAnimationSet> {
    private Rectangle rectangle = new Rectangle();

    public boolean isChecked() {
        return checked;
    }

    public MaterialSwitch toggleChecked() {
        boolean newChecked = !this.checked;

        if (newChecked) {
            // 打开开关
            animationSet.handlePosition
                    .ease(Easing.LINEAR)
                    .animateTo(rectangle.width - 32, 200);
            animationSet.handleSize
                    .ease(Easing.LINEAR)
                    .animateTo(
                            (style.showCheckedIconWhenEnable() || style.showCheckedIconAlways()) ?
                                    MaterialSwitchSize.Default.handleSizeCheckedWithIcon() :
                                    MaterialSwitchSize.Default.handleSizeChecked(),
                            150
                    );
        } else {
            // 关闭开关
            animationSet.handlePosition
                    .ease(Easing.LINEAR)
                    .animateTo(0, 200);
            animationSet.handleSize
                    .ease(Easing.LINEAR)
                    .animateTo(
                            (style.showUncheckedIconWhenEnable() || style.showUncheckedIconAlways()) ?
                                    MaterialSwitchSize.Default.handleSizeWithIcon() :
                                    MaterialSwitchSize.Default.handleSize(),
                            150
                    );
        }

        animationSet.change.set(0);
        animationSet.change.animateTo(1, 200);
        this.checked = newChecked;
        return this;
    }

    private boolean checked;

    public MaterialSwitch() {
        this.style = new MaterialSwitchStyle();
        updateRectangle();
    }

    public static MaterialSwitch create() {
        return new MaterialSwitch();
    }

    @Override
    protected void init() {
        this.animationSet = new MaterialSwitchAnimationSet();
        onMouseHover((widget, mousePosition, hover) -> onHover(mousePosition, hover));
        onMouseRelease((widget, mousePosition) -> onRelease(mousePosition));
        onMousePress((widget, mousePosition) -> onPress(mousePosition));
    }

    private void updateRectangle() {
        rectangle.x = getAbsolutePosition().x;
        rectangle.y = getAbsolutePosition().y;
        rectangle.width = MaterialSwitchSize.Default.trackWidth();
        rectangle.height = MaterialSwitchSize.Default.trackHeight();
    }

    @Override
    public Rectangle getBounds() {
        return rectangle;
    }

    @Override
    public void render(UIDrawContext drawContext, UIInputState inputState) {
        updateRectangle();
        if (animationSet.handleSize.floatValue() <
                ((isChecked() && (style.showCheckedIconWhenEnable() && isChecked() || style.showCheckedIconAlways())) ||
                        (!isChecked() && (style.showUncheckedIconWhenEnable() && !isChecked() || style.showUncheckedIconAlways())) ?
                        MaterialSwitchSize.Default.handleSizeWithIcon() : MaterialSwitchSize.Default.handleSize())
        ) {
            animationSet.handleSize.set(
                    ((isChecked() && (style.showCheckedIconWhenEnable() && isChecked() || style.showCheckedIconAlways())) ||
                            (!isChecked() && (style.showUncheckedIconWhenEnable() && !isChecked() || style.showUncheckedIconAlways())) ?
                            MaterialSwitchSize.Default.handleSizeWithIcon() : MaterialSwitchSize.Default.handleSize())
            );
        }
        animationSet.update();
        SwitchColors colors = getSwitchColors();

        drawContext.drawRoundedRect(
                rectangle.x,
                rectangle.y,
                MaterialSwitchSize.Default.trackWidth(),
                MaterialSwitchSize.Default.trackHeight(),
                MaterialSwitchSize.Default.trackHeight() / 2,
                colors.trackColor,
                true
        );

        if (!isChecked()) {
            drawContext.beginPath();
            drawContext.strokeColor(scheme().outline());
            drawContext.strokeWidth(MaterialSwitchSize.Default.trackOutlineWidth());
            drawContext.roundedRect(
                    rectangle.x,
                    rectangle.y,
                    MaterialSwitchSize.Default.trackWidth(),
                    MaterialSwitchSize.Default.trackHeight(),
                    MaterialSwitchSize.Default.trackHeight() / 2
            );
            drawContext.endPath(false);
        }
        float handleSize = animationSet.handleSize.floatValue();
        float handleX = rectangle.x + 16 + animationSet.handlePosition.floatValue();

        drawContext.drawArc(
                handleX,
                rectangle.getCenterY(),
                handleSize / 2,
                colors.handleColor,
                true
        );

        if (isHovered() || animationSet.hover.floatValue() > 0.001) {
            drawContext.drawArc(
                    handleX,
                    rectangle.getCenterY(),
                    20,
                    scheme().onSurface().copy().alpha((int) (0.1 * 255 * animationSet.hover.floatValue())),
                    true
            );
        }
        if ((isChecked() && (style.showCheckedIconWhenEnable() && isChecked() || style.showCheckedIconAlways()))) {
            float checkedIconX = rectangle.x + rectangle.width - 16;
            MaterialSymbols.iconCheck().render(
                    drawContext,
                    colors.iconColor.copy().alpha(
                            !animationSet.handlePosition.isRunning() ? 255 : Math.min((int) ((animationSet.handlePosition.progress() * 1.8) * 255), 255)
                    ),
                    MaterialSwitchSize.Default.iconSize(),
                    new Vector2f(
                            checkedIconX,
                            rectangle.getCenterY()
                    )
            );
        }
        if ((!isChecked() && (style.showUncheckedIconWhenEnable() && !isChecked() || style.showUncheckedIconAlways()))) {
            float closeIconX = rectangle.x + 16;
            MaterialSymbols.iconClose().render(
                    drawContext,
                    colors.iconColor.copy().alpha(
                            !animationSet.handlePosition.isRunning() ? 255 : Math.min((int) ((animationSet.handlePosition.progress() * 1.8) * 255), 255)
                    ),
                    MaterialSwitchSize.Default.iconSize(),
                    new Vector2f(
                            closeIconX,
                            rectangle.getCenterY()
                    )
            );
        }
    }

    private SwitchColors getSwitchColors() {
        SwitchColors colors = new SwitchColors();
        colors.trackColor = isChecked() ? scheme().primary() : scheme().surfaceContainerHighest();
        colors.handleColor = isChecked() ? scheme().onPrimary() : scheme().outline();
        colors.iconColor = isChecked() ? scheme().onPrimaryContainer() : scheme().surfaceContainerHighest();
        return colors;
    }

    private void onHover(Vector2f mousePosition, boolean hover) {
        if (hover) {
            animationSet.hover
                    .ease(Easing.LINEAR)
                    .animateTo(1, 200);
            MouseCursor.HAND.use();
        } else {
            animationSet.hover
                    .ease(Easing.LINEAR)
                    .animateTo(0, 200);
            MouseCursor.ARROW.use();
        }
        System.out.printf("hover %s %s %s%n", mousePosition.x, mousePosition.y, hover);

    }

    private void onPress(Vector2f mousePosition) {
        animationSet.handleSize
                .ease(Easing.LINEAR)
                .animateTo(
                        (style.showCheckedIconWhenEnable() && isChecked()) || style.showCheckedIconAlways() ?
                                MaterialSwitchSize.Default.handleSizePressWithIcon() :
                                MaterialSwitchSize.Default.handleSizePress(),
                        150
                );
        animationSet.hover
                .ease(Easing.LINEAR)
                .animateTo(1, 200);
        animationSet.press
                .ease(Easing.LINEAR)
                .animateTo(1, 200);
        System.out.printf("press %s %s%n", mousePosition.x, mousePosition.y);
    }

    private void onRelease(Vector2f mousePosition) {
        toggleChecked();
        animationSet.handleSize
                .ease(Easing.LINEAR)
                .animateTo(
                        (style.showUncheckedIconWhenEnable() && !isChecked()) || style.showUncheckedIconAlways() ?
                                !isChecked() ? MaterialSwitchSize.Default.handleSizeCheckedWithIcon() : MaterialSwitchSize.Default.handleSizeWithIcon() :
                                isChecked() ? MaterialSwitchSize.Default.handleSizeChecked() : MaterialSwitchSize.Default.handleSize(),
                        150
                );
        animationSet.hover
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(isHovered() ? 1 : 0, 200);
        animationSet.press
                .ease(Easing.cubicBezier(0.2f, 0, 0, 1))
                .animateTo(0, 200);

        System.out.printf("release %s %s%n", mousePosition.x, mousePosition.y);
    }

    private static class SwitchColors {
        Color iconColor;
        Color handleColor;
        Color trackColor;
    }
}