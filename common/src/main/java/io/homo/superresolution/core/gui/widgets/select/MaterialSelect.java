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

import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialSymbol;
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.render.RenderLayer;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.menu.*;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPhysicalEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class MaterialSelect<T> extends MaterialContainerWidget<MaterialSelect<T>> {


    public enum MenuPosition {
        AUTO(null, MenuAlign.START),
        AUTO_START(null, MenuAlign.START),
        AUTO_CENTER(null, MenuAlign.CENTER),
        AUTO_END(null, MenuAlign.END),

        BELOW(MenuSide.BOTTOM, MenuAlign.START),
        ABOVE(MenuSide.TOP, MenuAlign.START),

        TOP_START(MenuSide.TOP, MenuAlign.START),
        TOP_CENTER(MenuSide.TOP, MenuAlign.CENTER),
        TOP_END(MenuSide.TOP, MenuAlign.END),

        BOTTOM_START(MenuSide.BOTTOM, MenuAlign.START),
        BOTTOM_CENTER(MenuSide.BOTTOM, MenuAlign.CENTER),
        BOTTOM_END(MenuSide.BOTTOM, MenuAlign.END),

        LEFT_START(MenuSide.LEFT, MenuAlign.START),
        LEFT_CENTER(MenuSide.LEFT, MenuAlign.CENTER),
        LEFT_END(MenuSide.LEFT, MenuAlign.END),

        RIGHT_START(MenuSide.RIGHT, MenuAlign.START),
        RIGHT_CENTER(MenuSide.RIGHT, MenuAlign.CENTER),
        RIGHT_END(MenuSide.RIGHT, MenuAlign.END);

        private final MenuSide side;
        private final MenuAlign align;

        MenuPosition(MenuSide side, MenuAlign align) {
            this.side = side;
            this.align = align;
        }

        public boolean isAuto() {
            return side == null;
        }

        public MenuSide side() {
            return side;
        }

        public MenuAlign align() {
            return align;
        }
    }

    private enum MenuSide {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    private enum MenuAlign {
        START,
        CENTER,
        END
    }

    private final MaterialSelectField field;

    public MaterialMenu getMenu() {
        return menu;
    }

    private final MaterialMenu menu;
    private final List<SelectOption<T>> options = new ArrayList<>();

    private T selectedValue = null;
    private Consumer<T> onSelectionChanged;
    private Function<T, String> displayFormatter = Object::toString;
    private float width = 280;
    private MenuPosition menuPosition = MenuPosition.AUTO_START;

    public MaterialSelect() {
        this.style = new MaterialSelectStyle();

        layout().setFlexDirection(YogaFlexDirection.COLUMN);

        field = new MaterialSelectField();
        field.onClick(e -> toggleMenu());
        addChild(field);

        menu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.SingleAtLeastOne)
                .setExpanded(false);
        menu.layout().setWidth(width);
        menu.layout().setHeightAuto();
        menu.setParent(this);

        updateSize();
    }

    public static <T> MaterialSelect<T> create() {
        return new MaterialSelect<>();
    }

    @Override
    public MaterialSelectStyle style() {
        return (MaterialSelectStyle) super.style();
    }

    @Override
    protected void init() {
    }

    public MaterialSelect<T> label(String label) {
        field.label(label);
        return this;
    }

    public MaterialSelect<T> placeholder(String placeholder) {
        field.placeholder(placeholder);
        return this;
    }

    public MaterialSelect<T> supportingText(String text) {
        field.supportingText(text);
        return this;
    }

    public MaterialSelect<T> leadingIcon(MaterialSymbol icon) {
        field.leadingIcon(icon);
        return this;
    }

    public MaterialSelect<T> width(float width) {
        this.width = width;
        field.width(width);
        updateSize();
        return this;
    }

    public MaterialSelect<T> displayFormatter(Function<T, String> formatter) {
        this.displayFormatter = formatter;
        updateDisplayValue();
        return this;
    }

    public MaterialSelect<T> menuPosition(MenuPosition position) {
        this.menuPosition = position;
        return this;
    }

    public MaterialSelect<T> addOption(T value, String displayText) {
        return addOption(value, displayText, null);
    }

    public MaterialSelect<T> addOption(T value, String displayText, MaterialSymbol icon) {
        SelectOption<T> option = new SelectOption<>(value, displayText, icon);
        options.add(option);

        MaterialMenuItem item = MaterialMenuItem.create()
                .text(displayText)
                .value(value)
                .selectable(true);

        if (icon != null) {
            item.icon(icon);
        }

        item.onSelectionChanged(selected -> {
            if (selected) {
                handleSelection(value, displayText);
            }
        });

        menu.addItem(item);
        return this;
    }

    public MaterialSelect<T> addOptions(List<T> values) {
        for (T value : values) {
            addOption(value, displayFormatter.apply(value));
        }
        return this;
    }

    public MaterialSelect<T> clearOptions() {
        options.clear();
        for (ILayoutElement child : new ArrayList<>(menu.getChildren())) {
            menu.removeChild(child);
        }
        return this;
    }

    public MaterialSelect<T> setValue(T value) {
        this.selectedValue = value;
        menu.selectItemQuietly(value);
        updateDisplayValue();
        return this;
    }

    public T getValue() {
        return selectedValue;
    }

    public MaterialSelect<T> onSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = listener;
        return this;
    }

    private void handleSelection(T value, String displayText) {
        T oldValue = this.selectedValue;
        this.selectedValue = value;
        field.value(displayText);
        closeMenu();
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(value);
        }
        eventBus.post(new WidgetEvent.ChangeEvent<>(oldValue, value));
    }

    private void updateDisplayValue() {
        if (selectedValue != null) {
            for (SelectOption<T> option : options) {
                if (option.value.equals(selectedValue)) {
                    field.value(option.displayText);
                    return;
                }
            }
            field.value(displayFormatter.apply(selectedValue));
        } else {
            field.value("");
        }
    }

    private void toggleMenu() {
        if (menu.isExpanded()) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        menu.expand();
        field.setMenuOpen(true);
    }

    private void closeMenu() {
        menu.collapse();
        field.setMenuOpen(false);
    }

    public boolean isMenuOpen() {
        return menu.isExpanded();
    }

    private void updateSize() {
        MaterialSelectSize size = style().size();
        layout().setWidth(width);
        layout().setHeight(size.containerHeight());
    }

    @Override
    public void mouseMove(float x, float y) {
        Vector2f absPos = new Vector2f(x, y);

        if (menu.isExpanded() || menu.isVisible()) {
            menu.mouseMove(x, y);
        }

        if (field.hitTest(absPos)) {
            field.mouseMove(x, y);
        }
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        Vector2f absPos = new Vector2f(x, y);

        if (menu.isExpanded() || menu.isVisible()) {
            menu.mouseScroll(x, y, scrollX);
        }

        if (field.hitTest(absPos)) {
            field.mouseScroll(x, y, scrollX);
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        Vector2f absPos = new Vector2f(x, y);

        if (menu.isExpanded() || menu.isVisible()) {
            menu.mouseRelease(x, y, button);
        }

        if (field.hitTest(absPos)) {
            field.mouseRelease(x, y, button);
        }
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateSize();
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        if (!isVisible()) {
            return;
        }
        ctx.beginGroup(style().zIndex());

        renderSelf(ctx, inputState);
        field.render(ctx, inputState);

        if (menu.isExpanded() || menu.isVisible()) {
            Rectangle selfBounds = getRawBounds();
            MaterialSelectSize size = style().size();

            menu.layout().setWidth(width);
            menu.layout().setHeightAuto();
            menu.layout().setMaxHeight(StyleSizeLength.undefined());
            menu.getLayoutNode().calculateLayout(width, Float.MAX_VALUE);
            float naturalMenuHeight = menu.getMenuHeight();

            float viewportHeight = ctx.viewportHeight();
            float viewportWidth = ctx.viewportWidth();
            float viewportPadding = 8f;

            Rectangle fieldBounds = this.getBounds();
            float fieldX = fieldBounds.x;
            float fieldY = fieldBounds.y;
            float fieldWidth = fieldBounds.width;
            float fieldHeight = fieldBounds.height;

            float spaceBelow = viewportHeight - (fieldY + fieldHeight) - viewportPadding;
            float spaceAbove = fieldY - viewportPadding;
            float spaceRight = viewportWidth - (fieldX + fieldWidth) - viewportPadding;
            float spaceLeft = fieldX - viewportPadding;

            MenuSide side = menuPosition.side();
            MenuAlign align = menuPosition.align();

            if (menuPosition.isAuto()) {
                if (spaceBelow >= naturalMenuHeight) {
                    side = MenuSide.BOTTOM;
                } else if (spaceLeft >= width) {
                    side = MenuSide.LEFT;
                } else if (spaceRight >= width) {
                    side = MenuSide.RIGHT;
                } else if (spaceAbove >= naturalMenuHeight) {
                    side = MenuSide.TOP;
                } else {
                    float maxSpace = Math.max(Math.max(spaceBelow, spaceAbove), Math.max(spaceLeft, spaceRight));
                    if (maxSpace == spaceBelow) {
                        side = MenuSide.BOTTOM;
                    } else if (maxSpace == spaceLeft) {
                        side = MenuSide.LEFT;
                    } else if (maxSpace == spaceRight) {
                        side = MenuSide.RIGHT;
                    } else {
                        side = MenuSide.TOP;
                    }
                }
            }

            float availableHeight;
            switch (side) {
                case TOP:
                    availableHeight = Math.min(spaceAbove, naturalMenuHeight);
                    break;
                case BOTTOM:
                    availableHeight = Math.min(spaceBelow, naturalMenuHeight);
                    break;
                case LEFT:
                case RIGHT:
                default:
                    availableHeight = Math.min(naturalMenuHeight, viewportHeight - viewportPadding * 2);
                    break;
            }

            float menuHeight = availableHeight;

            float menuX;
            float menuY;

            switch (side) {
                case TOP:
                    menuY = fieldY - menuHeight - 4;
                    menuX = alignHorizontal(fieldX, fieldWidth, width, align);
                    break;
                case LEFT:
                    menuX = fieldX - width - 4;
                    menuY = alignVertical(fieldY, fieldHeight, menuHeight, align);
                    break;
                case RIGHT:
                    menuX = fieldX + fieldWidth + 4;
                    menuY = alignVertical(fieldY, fieldHeight, menuHeight, align);
                    break;
                case BOTTOM:
                default:
                    menuY = fieldY + fieldHeight + 4;
                    menuX = alignHorizontal(fieldX, fieldWidth, width, align);
                    break;
            }

            menuX = clamp(menuX, viewportPadding, viewportWidth - width - viewportPadding);
            menuY = clamp(menuY, viewportPadding, viewportHeight - menuHeight - viewportPadding);

            final float finalMenuX = menuX;
            final float finalMenuY = menuY;
            final float menuWidth = width;

            ctx.deferToLayer(RenderLayer.Floating, 1000, (deferredCtx) -> {
                menu.layout().setWidth(menuWidth);
                menu.layout().setMaxHeight(Float.MAX_VALUE);

                menu.layout().setPositionType(YogaPositionType.ABSOLUTE);
                menu.layout().setPosition(YogaEdge.LEFT, finalMenuX);
                menu.layout().setPosition(YogaEdge.TOP, finalMenuY);
                menu.getLayoutNode().calculateLayout(menuWidth, Float.MAX_VALUE);

                deferredCtx.resetScissor();
                menu.render(deferredCtx, inputState);
            });
        }
        ctx.endGroup();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
    }

    @Override
    public boolean managesChildRendering() {
        return true;
    }

    @Override
    public boolean managesChildEvents() {
        return true;
    }

    @Override
    public boolean isFloatingWidget() {
        return menu.isExpanded() || menu.isVisible();
    }

    @Override
    public AbstractWidget<?> findInteractiveWidgetAt(Vector2f absPos) {
        if (menu.isExpanded() && menu.isVisible() && menu.hitTest(absPos)) {
            return this;
        }
        if (field.hitTest(absPos)) {
            return this;
        }

        return null;
    }

    @Override
    public void mousePress(float x, float y, int button) {
        Vector2f absPos = new Vector2f(x, y);

        if (menu.isExpanded() || menu.isVisible()) {
            if (menu.hitTest(absPos)) {
                menu.mousePress(x, y, button);
                return;
            }
            if (field.hitTest(absPos)) {
                field.mousePress(x, y, button);
                return;
            }
            closeMenu();
            return;
        }

        if (field.hitTest(absPos)) {
            field.mousePress(x, y, button);
        }
    }

    @Override
    public boolean hitTest(Vector2f absolutePos) {
        Rectangle menuBounds = menu.getRawBounds();

        return field.hitTest(absolutePos) ||
                (menu.isExpanded() && menuBounds.in(absolutePos));
    }

    @Override
    public void destroy() {
        if (field != null) {
            field.destroy();
        }
        if (menu != null) {
            menu.destroy();
        }
    }

    private float alignHorizontal(float fieldX, float fieldWidth, float menuWidth, MenuAlign align) {
        return switch (align) {
            case START -> fieldX;
            case CENTER -> fieldX + (fieldWidth - menuWidth) / 2f;
            case END -> fieldX + fieldWidth - menuWidth;
        };
    }

    private float alignVertical(float fieldY, float fieldHeight, float menuHeight, MenuAlign align) {
        return switch (align) {
            case START -> fieldY;
            case CENTER -> fieldY + (fieldHeight - menuHeight) / 2f;
            case END -> fieldY + fieldHeight - menuHeight;
        };
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(value, min));
    }

    private record SelectOption<T>(T value,

                                   String displayText,

                                   MaterialSymbol icon) {
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }
}
