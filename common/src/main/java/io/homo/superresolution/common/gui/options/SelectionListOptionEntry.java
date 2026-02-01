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
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuColors;
import io.homo.superresolution.core.gui.widgets.select.MaterialSelect;
import io.homo.superresolution.core.gui.widgets.select.MaterialSelectColors;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SelectionListOptionEntry<T> extends AbstractOptionEntry<T, SelectionListOptionEntry<T>> {
    private static final float SELECT_WIDTH = 200f;
    protected final ImmutableList<T> values;
    protected final AtomicInteger index;
    protected final int original;
    protected ContainerWidget selectorContainer;
    protected MaterialSelect<T> materialSelect;
    protected MaterialLabel headerLabel;
    protected Function<T, String> nameProvider;
    protected String headerText = "";

    public SelectionListOptionEntry(
            Text name,
            T value,
            ImmutableList<T> values,
            Function<T, String> nameProvider
    ) {
        super(name, value);
        this.values = values;
        this.index = new AtomicInteger(values.indexOf(value));
        this.index.compareAndSet(-1, 0);
        this.original = values.indexOf(value);
        this.nameProvider = nameProvider;
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

        materialSelect = MaterialSelect.<T>create()
                .width(SELECT_WIDTH)
                .displayFormatter(nameProvider)
                .scheme(scheme);
        materialSelect.getMenu().style().colors(MaterialMenuColors.VIBRANT);
        if (headerText != null && !headerText.isEmpty()) {
            materialSelect.label(headerText);
        }

        for (T itemValue : values) {
            materialSelect.addOption(itemValue, nameProvider.apply(itemValue));
        }
        materialSelect.setValue(value());

        materialSelect.onSelectionChanged(newValue -> {
            int newIndex = values.indexOf(newValue);
            if (newIndex >= 0) {
                index.set(newIndex);
                this.value = newValue;
                if (saveConsumer != null) {
                    saveConsumer.accept(value());
                }
                if (saveRunnable != null) {
                    saveRunnable.run();
                }
            }
        });

        selectorContainer.addChild(materialSelect);

        container.addControl(selectorContainer);
        container.scheme(scheme);
    }

    @Override
    protected SelectionListOptionEntry<T> setScheme(MaterialScheme scheme) {
        if (materialSelect != null) {
            materialSelect.scheme(scheme);
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
}