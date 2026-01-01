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
import io.homo.superresolution.core.gui.core.AbstractWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.core.layout.ILayoutElement;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.gui.widgets.MaterialWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MaterialNavigationDrawer extends MaterialContainerWidget<MaterialNavigationDrawer> {
    private static final float CORNER_RADIUS = 16f;

    private MaterialNavigationDrawerItem selectedItem = null;
    private Consumer<MaterialNavigationDrawerItem> onItemSelectedHandler;
    private final List<MaterialNavigationDrawerItem> allItems = new ArrayList<>();

    public MaterialNavigationDrawer() {
        getLayoutNode().setDebugName("MaterialNavigationDrawer");
        layout().setFlexDirection(YogaFlexDirection.COLUMN);
        layout().setGap(YogaGutter.ROW, 1.5f);
    }

    public static MaterialNavigationDrawer create() {
        return new MaterialNavigationDrawer();
    }

    @Override
    protected void init() {
    }

    @Override
    protected Rectangle getViewRegion() {
        return getAbsoluteViewRect();
    }

    public MaterialNavigationDrawer addHeader(String title, MaterialSymbol icon) {
        MaterialNavigationDrawerHeader header = MaterialNavigationDrawerHeader.create()
                .title(title)
                .icon(icon);
        addChild(header);
        return this;
    }


    public MaterialNavigationDrawer addHeader(String title) {
        MaterialNavigationDrawerHeader header = MaterialNavigationDrawerHeader.create()
                .title(title);
        addChild(header);
        return this;
    }


    public MaterialNavigationDrawer addHeader(MaterialNavigationDrawerHeader header) {
        addChild(header);
        return this;
    }

    public MaterialNavigationDrawer addSectionHeader(String title) {
        MaterialNavigationDrawerSectionHeader sectionHeader = MaterialNavigationDrawerSectionHeader.create(title);
        addChild(sectionHeader);
        return this;
    }

    public MaterialNavigationDrawer addSectionHeader(MaterialNavigationDrawerSectionHeader sectionHeader) {
        addChild(sectionHeader);
        return this;
    }

    public MaterialNavigationDrawer addItem(String text, MaterialSymbol icon) {
        MaterialNavigationDrawerItem item = MaterialNavigationDrawerItem.create(text, icon);
        setupItem(item);
        addChild(item);
        return this;
    }

    public MaterialNavigationDrawer addItem(String text, MaterialSymbol icon, Object value) {
        MaterialNavigationDrawerItem item = MaterialNavigationDrawerItem.create(text, icon).value(value);
        setupItem(item);
        addChild(item);
        return this;
    }

    public MaterialNavigationDrawer addItem(MaterialNavigationDrawerItem item) {
        setupItem(item);
        addChild(item);
        return this;
    }

    public MaterialNavigationDrawer addDivider() {
        MaterialNavigationDrawerDivider divider = MaterialNavigationDrawerDivider.create();
        addChild(divider);
        return this;
    }

    public MaterialNavigationDrawer addDivider(MaterialNavigationDrawerDivider divider) {
        addChild(divider);
        return this;
    }

    public MaterialNavigationDrawer addFlexibleSpacer() {
        MaterialNavigationDrawerSpacer spacer = new MaterialNavigationDrawerSpacer();
        addChild(spacer);
        return this;
    }

    public MaterialNavigationDrawer onItemSelected(Consumer<MaterialNavigationDrawerItem> handler) {
        this.onItemSelectedHandler = handler;
        return this;
    }

    public MaterialNavigationDrawerItem getSelectedItem() {
        return selectedItem;
    }

    public MaterialNavigationDrawer setSelectedItem(MaterialNavigationDrawerItem item) {
        if (selectedItem != item) {
            if (selectedItem != null) {
                selectedItem.setSelected(false);
            }
            selectedItem = item;
            if (selectedItem != null) {
                selectedItem.setSelected(true);
            }
        }
        return this;
    }

    public MaterialNavigationDrawer setSelectedByValue(Object value) {
        for (MaterialNavigationDrawerItem item : allItems) {
            if (value != null && value.equals(item.getValue())) {
                setSelectedItem(item);
                return this;
            }
        }
        return this;
    }

    private void setupItem(MaterialNavigationDrawerItem item) {
        allItems.add(item);
        item.onClick((clickEvent) -> {
            MaterialNavigationDrawerItem clickedItem = (MaterialNavigationDrawerItem) clickEvent.getWidget();
            setSelectedItem(clickedItem);
            if (onItemSelectedHandler != null) {
                onItemSelectedHandler.accept(clickedItem);
            }
        });
    }

    @Override
    public void addChild(ILayoutElement element) {
        super.addChild(element);
        if (element instanceof MaterialNavigationDrawerHeader header) {
            header.scheme(scheme());
        } else if (element instanceof MaterialNavigationDrawerSectionHeader sectionHeader) {
            sectionHeader.scheme(scheme());
        } else if (element instanceof MaterialNavigationDrawerItem item) {
            item.scheme(scheme());
        } else if (element instanceof MaterialNavigationDrawerDivider divider) {
            divider.scheme(scheme());
        }
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        layout().setPadding(YogaEdge.HORIZONTAL, 12);
        Rectangle bounds = getBounds();
        drawContext.beginBatch();

        Color backgroundColor = scheme().surfaceContainerLow();
        drawContext.roundedRect(
                bounds.x,
                bounds.y,
                bounds.width,
                bounds.height,
                CORNER_RADIUS,
                backgroundColor,
                true
        );

        drawContext.endBatch(getZIndex());

        renderSelf(drawContext, inputState);
    }

    @Override
    protected void renderSelf(IUIDrawContext drawContext, UIInputState inputState) {
        if (!isVisible()) return;
        for (ILayoutElement child : getChildren()) {
            if (child instanceof AbstractWidget<?>) {
                AbstractWidget<?> widget = (AbstractWidget<?>) child;
                if (widget.isVisible()) {
                    widget.render(drawContext, inputState);
                }
            }
        }
    }

    private static final class MaterialNavigationDrawerSpacer extends MaterialWidget<MaterialNavigationDrawerSpacer> {
        MaterialNavigationDrawerSpacer() {
            getLayoutNode().setDebugName("NavigationDrawerSpacer");
            layout().setWidthPercent(100);
            layout().setHeight(0);
            layout().setFlexGrow(1f);
        }

        @Override
        protected void init() {
        }

        @Override
        protected boolean isInteractive() {
            return false;
        }

        @Override
        public void render(IUIDrawContext drawContext, UIInputState inputState) {
        }
    }
}
