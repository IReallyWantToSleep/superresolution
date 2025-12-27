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

import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.AbstractLayoutElement;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

import java.util.ArrayList;
import java.util.List;

public class MaterialMenu extends MaterialContainerWidget<MaterialMenu> {
    private MaterialMenuSelectionMode selectionMode = MaterialMenuSelectionMode.None;

    public MaterialMenu() {
        this.style = new MaterialMenuStyle();
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        layout().setGap(YogaGutter.COLUMN, 2);
        updateSize();
    }

    public void updateSize() {
        MaterialMenuSize size = style().size();

        float maxItemWidth = 0;
        for (io.homo.superresolution.core.gui.core.layout.ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                maxItemWidth = Math.max(maxItemWidth, group.computeContentWidth());
            }
        }

        float finalWidth = Math.max(size.minWidth(), Math.min(maxItemWidth, size.maxWidth()));

        layout().setWidth(finalWidth);
        layout().setGap(YogaGutter.ALL, size.verticalPadding());
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        for (ILayoutElement child : getChildren()) {
            if (child instanceof MaterialMenuGroup group) {
                group.style().colors(style().colors());
                group.scheme(scheme());
            }
        }
        updateSize();
        super.render(drawContext, inputState);
    }

    public static MaterialMenu create() {
        return new MaterialMenu();
    }

    @Override
    protected Rectangle getViewRegion() {
        return getAbsoluteViewRect();
    }

    @Override
    public MaterialMenuStyle style() {
        return (MaterialMenuStyle) super.style();
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
