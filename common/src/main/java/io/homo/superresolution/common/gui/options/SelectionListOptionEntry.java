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

package io.homo.superresolution.common.gui.options;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.MaterialSymbols;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonVariant;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.menu.*;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 选择列表选项条目
 * 使用 Button + Menu 组件实现下拉选择
 */
public class SelectionListOptionEntry<T> extends AbstractOptionEntry<T, SelectionListOptionEntry<T>> {
    protected final ImmutableList<T> values;
    protected final AtomicInteger index;
    protected final int original;

    // UI组件
    protected ContainerWidget selectorContainer;
    protected MaterialButton selectButton;
    protected MaterialLabel headerLabel;
    protected MaterialMenu dropdownMenu;
    protected boolean menuExpanded = false;

    protected Function<T, String> nameProvider;
    protected String headerText = "";

    private static final float BUTTON_MIN_WIDTH = 150f;

    public SelectionListOptionEntry(Text name, T value, ImmutableList<T> values) {
        super(name, value);
        this.values = values;
        this.index = new AtomicInteger(values.indexOf(value));
        this.index.compareAndSet(-1, 0);
        this.original = values.indexOf(value);
        this.nameProvider = t -> t.toString();
        init();
    }

    @Override
    protected void init() {
        this.container = new OptionContainerWidget(this);
        initLayout();
        initWidget();
    }

    @Override
    protected void initLayout() {
    }

    @Override
    protected void initWidget() {
        selectorContainer = new ContainerWidget();
        selectorContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        selectorContainer.layout().setAlignItems(YogaAlign.FLEX_END);

        if (headerText != null && !headerText.isEmpty()) {
            headerLabel = MaterialLabel.create()
                    .text(headerText)
                    .fontSize(12)
                    .scheme(scheme);
            headerLabel.layout().setMargin(YogaEdge.BOTTOM, 4);
            selectorContainer.addChild(headerLabel);
        }

        selectButton = MaterialButton.create(MaterialButtonSize.Small)
                .text(() -> nameProvider.apply(value()))
                .icon(MaterialSymbols.iconExpandMore())
                .variant(MaterialButtonVariant.Outlined)
                .scheme(scheme);
        selectButton.layout().setMinWidth(BUTTON_MIN_WIDTH);
        selectButton.onClick(event -> toggleMenu());

        selectorContainer.addChild(selectButton);

        dropdownMenu = MaterialMenu.create()
                .selectionMode(MaterialMenuSelectionMode.Single)
                .scheme(scheme);
        dropdownMenu.setExpanded(false);
        dropdownMenu.style().zIndex(100);
        dropdownMenu.layout().setPositionType(YogaPositionType.ABSOLUTE);
        dropdownMenu.layout().setPosition(YogaEdge.TOP, 0);
        dropdownMenu.layout().setPosition(YogaEdge.RIGHT, 0f);
        for (int i = 0; i < values.size(); i++) {
            T itemValue = values.get(i);
            MaterialMenuItem menuItem = MaterialMenuItem.create()
                    .text(nameProvider.apply(itemValue))
                    .value(i)
                    .selectable(true)
                    .scheme(scheme);

            if (i == index.get()) {
                menuItem.selected(true);
            }

            final int itemIndex = i;
            menuItem.onClick(clickEvent -> {
                selectValue(itemIndex);
                collapseMenu();
            });

            dropdownMenu.addItem(menuItem);
        }

        selectorContainer.addChild(dropdownMenu);

        container.addControl(selectorContainer);
        container.scheme(scheme);
    }

    private void toggleMenu() {
        if (menuExpanded) {
            collapseMenu();
        } else {
            expandMenu();
        }
    }

    private void expandMenu() {
        menuExpanded = true;
        dropdownMenu.expand();
        selectButton.icon(MaterialSymbols.iconExpandLess());
    }

    private void collapseMenu() {
        menuExpanded = false;
        dropdownMenu.collapse();
        selectButton.icon(MaterialSymbols.iconExpandMore());
    }

    private void selectValue(int newIndex) {
        index.set(newIndex);
        this.value = values.get(newIndex);

        // 更新菜单选中状态
        dropdownMenu.selectItemQuietly(newIndex);

        if (saveConsumer != null) {
            saveConsumer.accept(value());
        }
    }

    @Override
    protected SelectionListOptionEntry<T> setScheme(MaterialScheme scheme) {
        selectButton.scheme(scheme);
        dropdownMenu.scheme(scheme);
        if (headerLabel != null) {
            headerLabel.scheme(scheme).color(scheme.onSurfaceVariant());
        }
        return super.setScheme(scheme);
    }

    @Override
    public void render(RenderContext ctx, UIInputState inputState) {
        container.render(ctx, inputState);
    }

    public boolean isEdited() {
        return !Objects.equals(index.get(), original);
    }

    @Override
    public T value() {
        return values.get(index.get());
    }

    private int getDefaultIndex() {
        return defaultValue == null ? 0 : Math.max(0, values.indexOf(defaultValue.get()));
    }

    public SelectionListOptionEntry<T> setNameProvider(Function<T, String> nameProvider) {
        this.nameProvider = nameProvider != null ? nameProvider : t -> t.toString();
        return this;
    }

    public SelectionListOptionEntry<T> setHeaderText(String headerText) {
        this.headerText = headerText;
        return this;
    }

    @Override
    public float getEntryHeight() {
        return 56f;
    }
}