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
import io.homo.superresolution.core.utils.Color;
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
    public MaterialSelect<T> scheme(MaterialScheme scheme) {
        super.scheme(scheme);
        field.scheme(scheme);
        menu.scheme(scheme);
        return this;
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
    public void render(RenderContext ctx, UIInputState inputState) {
        updateSize();
        if (!isVisible()) {
            return;
        }
        ctx.beginGroup(style().zIndex());

        renderSelf(ctx, inputState);
        field.render(ctx, inputState);

        if (menu.isExpanded() || menu.isVisible()) {
            Rectangle selfBounds = getRawBounds();
            MaterialSelectSize size = style().size();

            Vector2f menuPos = new Vector2f(selfBounds.x, selfBounds.y + size.containerHeight() + 4);
            Vector2f screenPos = getFullTransform().transformPoint(menuPos);

            final float menuWidth = width;
            final float menuX = screenPos.x;
            final float menuY = screenPos.y;
            ctx.deferToLayer(RenderLayer.Floating, 1000, (deferredCtx) -> {
                deferredCtx.rect(
                        menuX,
                        menuY,
                        menuWidth,
                        menu.getLayoutNode().getLayoutHeight(),
                        Color.black(),
                        true
                );
                menu.layout().setWidth(menuWidth);
                menu.layout().setHeightAuto();
                menu.layout().setMaxHeight(StyleSizeLength.undefined());

                menu.layout().setPositionType(YogaPositionType.ABSOLUTE);
                menu.layout().setPosition(YogaEdge.LEFT, menuX);
                menu.layout().setPosition(YogaEdge.TOP, menuY);
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

    private record SelectOption<T>(T value,

                                   String displayText,

                                   MaterialSymbol icon) {
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }
}
