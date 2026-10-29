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

package io.homo.superresolution.core.gui.widgets.menu;

import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.animator.Animator;
import io.homo.superresolution.core.gui.core.animator.TimeInterpolator;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MaterialMenu extends MaterialContainerWidget<MaterialMenu> {
    private static final long EXPAND_ANIMATION_DURATION = 300;
    private final Animator.FloatAnimator expandAnimator = Animator.ofFloat(1f, 1f)
            .duration(150)
            .timeInterpolator(TimeInterpolator.easeOutCubic());
    private MaterialMenuSelectionMode selectionMode = MaterialMenuSelectionMode.None;
    private boolean expanded = true;
    private Consumer<Boolean> onExpandChanged;

    public MaterialMenu() {
        this.style = new MaterialMenuStyle();
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        layout().setGap(YogaGutter.COLUMN, 2);
        updateSize();
    }

    public static MaterialMenu create() {
        return new MaterialMenu();
    }

    public void updateSize() {
        updateSize(null);
    }

    public void updateSize(RenderContext ctx) {
        MaterialMenuSize size = style().size();

        float maxItemWidth = 0;
        if (ctx != null) {
            for (ILayoutElement child : getChildren()) {
                if (child instanceof MaterialMenuGroup group) {
                    maxItemWidth = Math.max(maxItemWidth, group.computeContentWidth(ctx));
                }
            }
        }

        layout().setGap(YogaGutter.ALL, size.verticalPadding());

        layout().setHeightAuto();
        layout().setMaxHeight(StyleSizeLength.undefined());
    }

    @Override
    public MaterialMenuStyle style() {
        return (MaterialMenuStyle) super.style();
    }

    @Override
    public void clearHover() {
        super.clearHover();
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                widget.clearHover();
            }
        }
    }

    @Override
    protected boolean isInteractive() {
        return expanded || expandAnimator.isRunning();
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && isExpanded();
    }

    @Override
    public boolean managesChildRendering() {
        return true;
    }

    public float getMenuHeight() {
        MaterialMenuSize size = style().size();
        float totalHeight = 0;
        //TODO: 考虑padding
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                totalHeight += ((YogaNode) group.layout()).getLayout().measuredDimension(YogaDimension.HEIGHT);
            }
        }
        return totalHeight;
    }

    public MaterialMenu expand() {
        if (!expanded) {
            expanded = true;
            expandAnimator.fromTo(expandAnimator.get(), 1f).start();
            if (onExpandChanged != null) {
                onExpandChanged.accept(true);
            }
        }
        return this;
    }

    public MaterialMenu collapse() {
        if (expanded) {
            expanded = false;
            expandAnimator.fromTo(expandAnimator.get(), 0f).start();
            if (onExpandChanged != null) {
                onExpandChanged.accept(false);
            }
        }
        return this;
    }

    public MaterialMenu toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
        return this;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public MaterialMenu setExpanded(boolean expanded) {
        this.expanded = expanded;
        expandAnimator.set(expanded ? 1f : 0f);
        float progress = expanded ? 1f : 0f;
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                group.setExpandProgress(progress);
                for (ILayoutElement groupChild : group.getChildren()) {
                    if (groupChild instanceof MaterialMenuItem item) {
                        item.resetFadeState(expanded);
                    }
                }
            }
        }
        return this;
    }

    public MaterialMenu onExpandChanged(Consumer<Boolean> onExpandChanged) {
        this.onExpandChanged = onExpandChanged;
        return this;
    }

    @Override
    public boolean hitTest(org.joml.Vector2f absolutePos) {
        if (!isInteractive()) {
            return false;
        }

        Transform fullTransform = getFullTransform();
        org.joml.Vector2f testPos = absolutePos;

        if (!fullTransform.isIdentity()) {
            testPos = fullTransform.inverseTransformPoint(absolutePos);
        }

        Rectangle visibleBounds = getVisibleBounds();
        return visibleBounds.in(testPos);
    }

    private Rectangle getVisibleBounds() {
        Rectangle rawBounds = getRawBounds();
        float animProgress = expandAnimator.get();
        float currentHeight = rawBounds.height;
        return new Rectangle(rawBounds.x, rawBounds.y, rawBounds.width, currentHeight);
    }

    @Override
    protected void init() {
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateSize(ctx);
        super.layouting(ctx);
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        expandAnimator.update();
        float animProgress = expandAnimator.get();

        if (animProgress <= 0) {
            return;
        }

        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                group.style().colors(style().colors());
                group.setExpandProgress(1f);
                for (ILayoutElement groupChild : group.getChildren()) {
                    if (groupChild instanceof MaterialMenuItem item) {
                        item.resetFadeState(true);
                    }
                }
            }
        }

        ctx.beginGroup(style().zIndex());
        ctx.save();
        ctx.pushAlpha(animProgress);


        renderSelf(ctx, inputState);
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget && widget.isVisible()) {
                widget.renderWithChildren(ctx, inputState);
            }
        }


        ctx.popAlpha();
        ctx.restore();
        ctx.endGroup();
    }

    @Override
    public AbstractWidget<?> findInteractiveWidgetAt(org.joml.Vector2f absPos) {
        if (!hitTest(absPos)) {
            return null;
        }

        java.util.List<ILayoutElement> children = getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            ILayoutElement child = children.get(i);
            if (child instanceof AbstractWidget<?> widget && widget.isVisible() && !widget.isDisabled()) {
                AbstractWidget<?> interactive = widget.findInteractiveWidgetAt(absPos);
                if (interactive != null) {
                    return interactive;
                }
            }
        }

        return isInteractive() ? this : null;
    }

    @Override
    protected Rectangle getViewRegion() {
        return getAbsoluteViewRect();
    }

    public MaterialMenu addGroup(MaterialMenuGroup group) {
        addChild(group);
        return this;
    }

    public MaterialMenu addItem(MaterialMenuItem item) {
        if (getChildren().isEmpty()) {
            addChild(new MaterialMenuGroup(this));
        }
        AbstractLayoutElement lastChild = (AbstractLayoutElement) getChildren().get(getChildren().size() - 1);
        if (lastChild instanceof MaterialMenuGroup group) {
            group.addItem(item);
        }
        return this;
    }

    public MaterialMenu itemStyle(Consumer<MaterialMenuItemStyle> style) {
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                for (ILayoutElement groupChild : group.getChildren()) {
                    if (groupChild instanceof MaterialMenuItem item) {
                        style.accept(item.style());
                    }
                }
            }
        }
        return this;
    }

    public MaterialMenu selectionMode(MaterialMenuSelectionMode mode) {
        this.selectionMode = mode;
        return this;
    }

    public MaterialMenu selectItemQuietly(Object value) {
        selectItemQuietly(getItemByValue(value));
        return this;
    }

    public MaterialMenu selectItem(Object value) {
        selectItem(getItemByValue(value));
        return this;
    }

    public MaterialMenu deselectItem(Object value) {
        deselectItem(getItemByValue(value));
        return this;
    }

    public MaterialMenu deselectItemQuietly(Object value) {
        deselectItemQuietly(getItemByValue(value));
        return this;
    }

    public MaterialMenu selectItemQuietly(MaterialMenuItem item) {
        handleItemSelection(item, false);
        return this;
    }

    public MaterialMenu selectItem(MaterialMenuItem item) {
        handleItemSelection(item, true);
        return this;
    }

    public MaterialMenu deselectItem(MaterialMenuItem item) {
        item.setSelectedInternal(false, true);
        return this;
    }

    public MaterialMenu deselectItemQuietly(MaterialMenuItem item) {
        item.setSelectedInternal(false, false);
        return this;
    }

    public MaterialMenuItem getItemByValue(Object value) {
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                for (ILayoutElement groupChild : group.getChildren()) {
                    if (groupChild instanceof MaterialMenuItem item) {
                        if (item.getValue() != null && item.getValue().equals(value)) {
                            return item;
                        }
                    }
                }
            }
        }
        return null;
    }

    public MaterialMenuSelectionMode getSelectionMode() {
        return selectionMode;
    }

    void handleItemSelection(MaterialMenuItem clickedItem) {
        handleItemSelection(clickedItem, true);
    }

    void handleItemSelection(MaterialMenuItem clickedItem, boolean notifyListener) {
        if (selectionMode == MaterialMenuSelectionMode.None) {
            return;
        }

        boolean newSelectedState = !clickedItem.isSelected();

        switch (selectionMode) {
            case Single -> {
                for (ILayoutElement child : getChildren()) {
                    if (child instanceof MaterialMenuGroup group) {
                        for (ILayoutElement groupChild : group.getChildren()) {
                            if (groupChild instanceof MaterialMenuItem item) {
                                if (item != clickedItem && item.isSelected()) {
                                    item.setSelectedInternal(false, notifyListener);
                                }
                            }
                        }
                    }
                }
                clickedItem.setSelectedInternal(newSelectedState, notifyListener);
            }
            case SingleAtLeastOne -> {
                if (newSelectedState) {
                    for (ILayoutElement child : getChildren()) {
                        if (child instanceof MaterialMenuGroup group) {
                            for (ILayoutElement groupChild : group.getChildren()) {
                                if (groupChild instanceof MaterialMenuItem item) {
                                    if (item != clickedItem && item.isSelected()) {
                                        item.setSelectedInternal(false, notifyListener);
                                    }
                                }
                            }
                        }
                    }
                    clickedItem.setSelectedInternal(true, notifyListener);
                }
            }
            case SinglePerGroup -> {
                if (clickedItem.getParent() instanceof MaterialMenuGroup group) {
                    for (ILayoutElement groupChild : group.getChildren()) {
                        if (groupChild instanceof MaterialMenuItem item) {
                            if (item != clickedItem && item.isSelected()) {
                                item.setSelectedInternal(false, notifyListener);
                            }
                        }
                    }
                }
                clickedItem.setSelectedInternal(newSelectedState, notifyListener);
            }
            case Multiple -> {
                clickedItem.setSelectedInternal(newSelectedState, notifyListener);
            }
            case MultipleAtLeastOne -> {
                if (newSelectedState) {
                    clickedItem.setSelectedInternal(true, notifyListener);
                } else {
                    int selectedCount = 0;
                    for (ILayoutElement child : getChildren()) {
                        if (child instanceof MaterialMenuGroup group) {
                            for (ILayoutElement groupChild : group.getChildren()) {
                                if (groupChild instanceof MaterialMenuItem item) {
                                    if (item.isSelected()) {
                                        selectedCount++;
                                    }
                                }
                            }
                        }
                    }
                    if (selectedCount > 1) {
                        clickedItem.setSelectedInternal(false, notifyListener);
                    }
                }
            }
        }
    }

    public List<Object> getSelectedValues() {
        List<Object> values = new ArrayList<>();
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                for (ILayoutElement groupChild : group.getChildren()) {
                    if (groupChild instanceof MaterialMenuItem item) {
                        if (item.isSelected() && item.getValue() != null) {
                            values.add(item.getValue());
                        }
                    }
                }
            }
        }
        return values;
    }

    public List<MaterialMenuItem> getSelectedItems() {
        List<MaterialMenuItem> items = new ArrayList<>();
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                for (ILayoutElement groupChild : group.getChildren()) {
                    if (groupChild instanceof MaterialMenuItem item) {
                        if (item.isSelected()) {
                            items.add(item);
                        }
                    }
                }
            }
        }
        return items;
    }
}
