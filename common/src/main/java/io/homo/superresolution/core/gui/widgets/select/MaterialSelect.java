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
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.render.RenderLayer;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenu;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuItem;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuSelectionMode;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaPositionType;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MaterialSelect<T> extends MaterialContainerWidget<MaterialSelect<T>> {
    private final MaterialSelectField field;
    private final MaterialMenu menu;
    private final List<SelectOption<T>> options = new ArrayList<>();
    private T selectedValue = null;
    private Consumer<WidgetEvent.ChangeEvent<T>> onSelectionChanged;
    private Function<T, String> displayFormatter = Object::toString;
    private float width = 280;
    private float minWidth = 160;
    private boolean autoWidth = true;
    private boolean widthDirty = true;
    private boolean suppressNextToggle = false;
    private MenuPosition menuPosition = MenuPosition.AUTO_START;
    private float cachedMenuX;
    private float cachedMenuY;
    private float cachedMenuHeight;
    private MenuSide cachedMenuSide;
    private MenuAlign cachedMenuAlign;

    private static final float DIRECTIONAL_ANIMATION_SCALE = 0.96f;
    private static final float DIRECTIONAL_ANIMATION_OFFSET = 6.0f;
    private static final long DIRECTIONAL_ANIMATION_DURATION = 200;

    private final Animator.FloatAnimator directionalAnimator = Animator.ofFloat(0f, 0f)
            .duration(DIRECTIONAL_ANIMATION_DURATION)
            .timeInterpolator(TimeInterpolator.easeOutCubic());

    public MaterialSelect() {
        this.style = new MaterialSelectStyle();

        layout().setFlexDirection(YogaFlexDirection.COLUMN);

        field = new MaterialSelectField(this);
        field.onClick(e -> toggleMenu());
        addChild(field);

        menu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.SingleAtLeastOne)
                .setExpanded(false);
        menu.layout().setWidth(width);
        menu.layout().setHeightAuto();
        addChild(menu);

        updateSize();
    }

    public static <T> MaterialSelect<T> create() {
        return new MaterialSelect<>();
    }

    public MaterialMenu getMenu() {
        return menu;
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
        suppressNextToggle = false;
        if (autoWidth && widthDirty) {
            recalculateWidth(ctx);
        }
        updateSize();
        super.layouting(ctx);
        menu.layouting(ctx);
        menu.style().zIndex(10000000);
        menu.layout().setMinWidth(StyleSizeLength.undefined());
        menu.layout().setMaxWidth(StyleSizeLength.undefined());
        menu.layout().setWidth(width);
        menu.layout().setHeightAuto();
        if (menu.isExpanded() || menu.isVisible() || directionalAnimator.isRunning()) {
            computeMenuPlacement(ctx);
        }
    }

    private void computeMenuPlacement(RenderContext ctx) {
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
            boolean horizontalFits = switch (align) {
                case START -> (viewportWidth - fieldX - viewportPadding) >= width;
                case END -> (fieldX + fieldWidth - viewportPadding) >= width;
                case CENTER -> (fieldX + fieldWidth / 2f - viewportPadding) >= width / 2f &&
                        (viewportWidth - (fieldX + fieldWidth / 2f) - viewportPadding) >= width / 2f;
            };

            boolean verticalFits = switch (align) {
                case START -> (viewportHeight - fieldY - viewportPadding) >= naturalMenuHeight;
                case END -> (fieldY + fieldHeight - viewportPadding) >= naturalMenuHeight;
                case CENTER -> (fieldY + fieldHeight / 2f - viewportPadding) >= naturalMenuHeight / 2f &&
                        (viewportHeight - (fieldY + fieldHeight / 2f) - viewportPadding) >= naturalMenuHeight / 2f;
            };

            boolean fitsBottom = (spaceBelow >= naturalMenuHeight) && horizontalFits;
            boolean fitsTop = (spaceAbove >= naturalMenuHeight) && horizontalFits;
            boolean fitsLeft = (spaceLeft >= width) && verticalFits;
            boolean fitsRight = (spaceRight >= width) && verticalFits;

            if (fitsBottom) {
                side = MenuSide.BOTTOM;
            } else if (fitsTop) {
                side = MenuSide.TOP;
            } else if (fitsLeft) {
                side = MenuSide.LEFT;
            } else if (fitsRight) {
                side = MenuSide.RIGHT;
            } else {
                if (spaceBelow >= spaceAbove) {
                    side = MenuSide.BOTTOM;
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

        this.cachedMenuX = menuX;
        this.cachedMenuY = menuY;
        this.cachedMenuHeight = menuHeight;
        this.cachedMenuSide = side;
        this.cachedMenuAlign = align;

        menu.layout().setPositionType(YogaPositionType.ABSOLUTE);
        menu.layout().setPosition(YogaEdge.LEFT, menuX);
        menu.layout().setPosition(YogaEdge.TOP, menuY);
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        if (!isVisible()) {
            return;
        }
        ctx.beginGroup(style().zIndex());

        renderSelf(ctx, inputState);
        field.render(ctx, inputState);

        directionalAnimator.update();
        if (menu.isExpanded() || menu.isVisible() || directionalAnimator.isRunning()) {
            final float finalMenuX = cachedMenuX;
            final float finalMenuY = cachedMenuY;
            final float menuWidth = menu.getBounds().width;
            final float menuHeight = cachedMenuHeight;
            final MenuSide finalSide = cachedMenuSide;
            final MenuAlign align = cachedMenuAlign;

            float directionalProgress = directionalAnimator.get();
            float scale = DIRECTIONAL_ANIMATION_SCALE + (1f - DIRECTIONAL_ANIMATION_SCALE) * directionalProgress;
            float invProgress = 1f - directionalProgress;

            float pivotX, pivotY, offsetX, offsetY;
            switch (finalSide) {
                case BOTTOM:
                    pivotX = switch (align) {
                        case START -> finalMenuX;
                        case CENTER -> finalMenuX + menuWidth / 2f;
                        case END -> finalMenuX + menuWidth;
                    };
                    pivotY = finalMenuY;
                    offsetX = 0;
                    offsetY = -DIRECTIONAL_ANIMATION_OFFSET * invProgress;
                    break;
                case TOP:
                    pivotX = switch (align) {
                        case START -> finalMenuX;
                        case CENTER -> finalMenuX + menuWidth / 2f;
                        case END -> finalMenuX + menuWidth;
                    };
                    pivotY = finalMenuY + menuHeight;
                    offsetX = 0;
                    offsetY = DIRECTIONAL_ANIMATION_OFFSET * invProgress;
                    break;
                case LEFT:
                    pivotX = finalMenuX + menuWidth;
                    pivotY = switch (align) {
                        case START -> finalMenuY;
                        case CENTER -> finalMenuY + menuHeight / 2f;
                        case END -> finalMenuY + menuHeight;
                    };
                    offsetX = DIRECTIONAL_ANIMATION_OFFSET * invProgress;
                    offsetY = 0;
                    break;
                case RIGHT:
                default:
                    pivotX = finalMenuX;
                    pivotY = switch (align) {
                        case START -> finalMenuY;
                        case CENTER -> finalMenuY + menuHeight / 2f;
                        case END -> finalMenuY + menuHeight;
                    };
                    offsetX = -DIRECTIONAL_ANIMATION_OFFSET * invProgress;
                    offsetY = 0;
                    break;
            }

            final float fScale = scale;
            final float fPivotX = pivotX;
            final float fPivotY = pivotY;
            final float fOffsetX = offsetX;
            final float fOffsetY = offsetY;


            ctx.deferToLayer(RenderLayer.Floating, 1000, (deferredCtx) -> {
                deferredCtx.resetScissor();
                deferredCtx.save();
                deferredCtx.translate(fPivotX + fOffsetX, fPivotY + fOffsetY);
                deferredCtx.scale(fScale, fScale);
                deferredCtx.translate(-fPivotX, -fPivotY);
                menu.render(deferredCtx, inputState);
                deferredCtx.restore();
            });
        }
        ctx.endGroup();
    }

    @Override
    public AbstractWidget<?> findInteractiveWidgetAt(Vector2f absPos) {
        if (field.hitTest(absPos)) {
            return field;
        }
        if (menu.isExpanded() && menu.isVisible() && menu.hitTest(absPos)) {
            return menu.findInteractiveWidgetAt(absPos);
        }
        return null;
    }

    @Override
    protected Rectangle getViewRegion() {
        return getBounds();
    }

    @Override
    protected void renderSelf(RenderContext ctx, UIInputState inputState) {
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
        this.minWidth = width;
        this.autoWidth = false;
        field.width(width);
        updateSize();
        return this;
    }

    public MaterialSelect<T> minWidth(float minWidth) {
        this.minWidth = minWidth;
        this.widthDirty = true;
        return this;
    }

    public MaterialSelect<T> autoWidth(boolean autoWidth) {
        this.autoWidth = autoWidth;
        this.widthDirty = true;
        return this;
    }

    public MaterialSelect<T> displayFormatter(Function<T, String> formatter) {
        this.displayFormatter = formatter;
        this.widthDirty = true;
        updateDisplayValue();
        return this;
    }

    public MaterialSelect<T> menuPosition(MenuPosition position) {
        this.menuPosition = position;
        return this;
    }

    public MaterialSelect<T> addOption(T value, String displayText) {
        return addOption(value, displayText, null, null);
    }

    public MaterialSelect<T> addOption(T value, String displayText, MaterialSymbol icon) {
        return addOption(value, displayText, icon, null);
    }

    public MaterialSelect<T> addOption(T value, String displayText, @Nullable Supplier<Optional<Tooltip>> tooltipSupplier) {
        return addOption(value, displayText, null, tooltipSupplier);
    }

    public MaterialSelect<T> addOption(T value, String displayText, MaterialSymbol icon, @Nullable Supplier<Optional<Tooltip>> tooltipSupplier) {
        SelectOption<T> option = new SelectOption<>(value, displayText, icon, tooltipSupplier);
        options.add(option);

        MaterialMenuItem item = MaterialMenuItem.create()
                .text(displayText)
                .value(value)
                .selectable(true);

        if (icon != null) {
            item.icon(icon);
        }

        if (tooltipSupplier != null) {
            item.setTooltipSupplier(tooltipSupplier);
        }

        item.onSelectionChanged(selected -> {
            if (selected) {
                handleSelection(value, displayText);
            }
        });

        menu.addItem(item);
        widthDirty = true;
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
        widthDirty = true;
        return this;
    }

    public T getValue() {
        return selectedValue;
    }

    public MaterialSelect<T> setValue(T value) {
        this.selectedValue = value;
        menu.selectItemQuietly(value);
        updateDisplayValue();
        return this;
    }

    public MaterialSelect<T> onSelectionChanged(Consumer<T> listener) {
        this.onSelectionChanged = (event) -> {
            listener.accept(event.getNewValue());
        };
        return this;
    }

    private void handleSelection(T value, String displayText) {
        T oldValue = this.selectedValue;
        this.selectedValue = value;
        field.value(displayText);
        closeMenu();
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(new WidgetEvent.ChangeEvent<>(oldValue, value));
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
        if (isDisabled()) {
            return;
        }
        if (suppressNextToggle) {
            suppressNextToggle = false;
            return;
        }
        if (menu.isExpanded()) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        if (isDisabled()) {
            return;
        }
        menu.expand();
        field.setMenuOpen(true);
        if (getFrame() != null) {
            getFrame().requestFocus(this);
        }
        directionalAnimator.fromTo(directionalAnimator.get(), 1f).start();
    }

    void closeMenu() {
        menu.collapse();
        field.setMenuOpen(false);
        directionalAnimator.fromTo(directionalAnimator.get(), 0f).start();
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
    public MaterialSelectStyle style() {
        return (MaterialSelectStyle) super.style();
    }

    @Override
    public void clearHover() {
        super.clearHover();
        field.clearHover();
        menu.clearHover();
    }

    @Override
    @SuppressWarnings("unchecked")
    public MaterialSelect<T> setFocused(boolean focused) {
        if (focused == isFocused()) {
            return this;
        }
        super.setFocused(focused);
        field.setFocused(focused);
        if (!focused && menu.isExpanded()) {
            closeMenu();
            suppressNextToggle = true;
        }
        return this;
    }

    @Override
    public boolean managesChildRendering() {
        return true;
    }

    @Override
    public boolean isFloatingWidget() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MaterialSelect<T> setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        field.setDisabled(disabled);
        if (disabled && menu.isExpanded()) {
            closeMenu();
        }
        return this;
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

    private void recalculateWidth(RenderContext ctx) {
        if (!autoWidth || options.isEmpty()) {
            widthDirty = false;
            return;
        }
        MaterialSelectSize size = style().size();
        float maxTextWidth = 0;
        for (SelectOption<T> option : options) {
            float tw = ctx.measureTextWidth(
                    option.displayText(), size.inputFontSize(), size.inputFontSize() + 1);
            maxTextWidth = Math.max(maxTextWidth, tw);
        }

        float fieldPadding = size.horizontalPadding() * 2;
        float trailingIconSpace = size.iconSize() + size.iconTextGap();
        float contentWidth = maxTextWidth + fieldPadding + trailingIconSpace + 12;

        this.width = Math.max(minWidth, contentWidth);
        field.width(this.width);
        widthDirty = false;
    }

    @Override
    public boolean hitTest(Vector2f absolutePos) {
        Rectangle menuBounds = menu.getRawBounds();

        return field.hitTest(absolutePos) ||
                (menu.isExpanded() && menuBounds.in(absolutePos));
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

    private record SelectOption<T>(T value,

                                   String displayText,

                                   MaterialSymbol icon,

                                   @Nullable Supplier<Optional<Tooltip>> tooltipSupplier) {
    }
}
