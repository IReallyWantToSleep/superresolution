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
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.style.StyleSizeLength;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MaterialMenu extends MaterialContainerWidget<MaterialMenu> {
    private static final long EXPAND_ANIMATION_DURATION = 300;

    private MaterialMenuSelectionMode selectionMode = MaterialMenuSelectionMode.None;
    private boolean expanded = true;
    private Consumer<Boolean> onExpandChanged;

    private final Animator.FloatAnimator expandAnimator = Animator.ofFloat(1f, 1f)
            .duration(250)
            .timeInterpolator(TimeInterpolator.easeOutCubic());

    public MaterialMenu() {
        this.style = new MaterialMenuStyle();
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        layout().setGap(YogaGutter.COLUMN, 2);
        updateSize();
    }

    public void updateSize() {
        MaterialMenuSize size = style().size();

        float maxItemWidth = 0;
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                maxItemWidth = Math.max(maxItemWidth, group.computeContentWidth());
            }
        }

        float finalWidth = Math.max(size.minWidth(), Math.min(maxItemWidth, size.maxWidth()));

        layout().setWidth(finalWidth);
        layout().setGap(YogaGutter.ALL, size.verticalPadding());

        layout().setHeightAuto();
        layout().setMaxHeight(StyleSizeLength.undefined());
    }

    @Override
    public void layouting(RenderContext ctx) {
        updateSize();
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

    public static MaterialMenu create() {
        return new MaterialMenu();
    }

    @Override
    protected Rectangle getViewRegion() {
        return getAbsoluteViewRect();
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

    public boolean isExpanded() {
        return expanded;
    }

    public MaterialMenu onExpandChanged(Consumer<Boolean> onExpandChanged) {
        this.onExpandChanged = onExpandChanged;
        return this;
    }

    @Override
    public MaterialMenuStyle style() {
        return (MaterialMenuStyle) super.style();
    }

    @Override
    protected boolean isInteractive() {
        return expanded || expandAnimator.isRunning();
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
    public void mouseMove(float x, float y) {
        super.mouseMove(x, y);
        if (isDisabled() || !isVisible()) {
            return;
        }

        Vector2f mousePos = new Vector2f(x, y);
        AbstractWidget<?> topChild = null;

        for (int i = getChildren().size() - 1; i >= 0; i--) {
            ILayoutElement child = getChildren().get(i);
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled() && widget.hitTest(mousePos)) {
                    topChild = widget.findInteractiveWidgetAt(mousePos);
                    if (topChild != null) {
                        break;
                    }
                }
            }
        }

        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    if (widget == topChild || (topChild != null && isAncestorOf(widget, topChild))) {
                        widget.mouseMove(x, y);
                    } else if (widget.isHovered()) {
                        widget.clearHover();
                    }
                }
            }
        }
    }

    private boolean isAncestorOf(AbstractWidget<?> potentialAncestor, AbstractWidget<?> descendant) {
        ILayoutElement current = descendant.getParent();
        while (current != null) {
            if (current == potentialAncestor) {
                return true;
            }
            if (current instanceof ILayoutElement) {
                current = ((ILayoutElement) current).getParent();
            } else {
                break;
            }
        }
        return false;
    }

    @Override
    public void mousePress(float x, float y, int button) {
        super.mousePress(x, y, button);
        if (isDisabled() || !isVisible()) {
            return;
        }

        Vector2f mousePos = new Vector2f(x, y);

        for (int i = getChildren().size() - 1; i >= 0; i--) {
            ILayoutElement child = getChildren().get(i);
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled() && widget.hitTest(mousePos)) {
                    AbstractWidget<?> interactive = widget.findInteractiveWidgetAt(mousePos);
                    if (interactive != null) {
                        interactive.mousePress(x, y, button);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void mouseRelease(float x, float y, int button) {
        super.mouseRelease(x, y, button);
        if (isDisabled() || !isVisible()) {
            return;
        }
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled()) {
                    widget.mouseRelease(x, y, button);
                }
            }
        }
    }

    @Override
    public void mouseScroll(float x, float y, double scrollX) {
        super.mouseScroll(x, y, scrollX);
        if (isDisabled() || !isVisible()) {
            return;
        }

        org.joml.Vector2f mousePos = new org.joml.Vector2f(x, y);

        for (int i = getChildren().size() - 1; i >= 0; i--) {
            ILayoutElement child = getChildren().get(i);
            if (child instanceof AbstractWidget<?> widget) {
                if (widget.isVisible() && !widget.isDisabled() && widget.hitTest(mousePos)) {
                    AbstractWidget<?> interactive = widget.findInteractiveWidgetAt(mousePos);
                    if (interactive != null) {
                        interactive.mouseScroll(x, y, scrollX);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public Transform getFullTransform() {
        if (getLayoutNode().getPositionType() == YogaPositionType.ABSOLUTE) {
            return Transform.identity();
        }

        Transform selfTransform = style().transform();

        if (getParent() instanceof AbstractLayoutElement parentElement) {
            Transform parentFullTransform = parentElement.getFullTransform();

            if (parentFullTransform.isIdentity()) {
                return selfTransform;
            }
            if (selfTransform.isIdentity()) {
                return parentFullTransform;
            }
            float[] combined = Transform.multiply(parentFullTransform.getMatrix(), selfTransform.getMatrix());
            return new Transform(combined);
        }

        return selfTransform;
    }

    private Rectangle getVisibleBounds() {
        Rectangle rawBounds = getRawBounds();
        float animProgress = expandAnimator.get();
        float currentHeight = rawBounds.height;
        return new Rectangle(rawBounds.x, rawBounds.y, rawBounds.width, currentHeight);
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
    protected void init() {
    }

    public MaterialMenu addGroup(MaterialMenuGroup group) {
        addChild(group);
        return this;
    }

    public MaterialMenu addItem(MaterialMenuItem item) {
        if (getChildren().isEmpty()) {
            addChild(new MaterialMenuGroup());
        }
        AbstractLayoutElement lastChild = (AbstractLayoutElement) getChildren().get(getChildren().size() - 1);
        if (lastChild instanceof MaterialMenuGroup group) {
            group.addItem(item);
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
