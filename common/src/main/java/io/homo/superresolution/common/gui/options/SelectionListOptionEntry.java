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
import io.homo.superresolution.common.gui.impl.OptionRequirement;
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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SelectionListOptionEntry<T> extends AbstractOptionEntry<T, SelectionListOptionEntry<T>> {
    private static final float SELECT_MIN_WIDTH = 150f;
    protected final ImmutableList<T> values;
    protected final AtomicInteger index;
    protected final int original;
    protected ContainerWidget selectorContainer;
    protected MaterialSelect<T> materialSelect;
    protected Function<T, String> nameProvider;
    protected @Nullable Function<T, OptionRequirement> itemEnableRequirement = null;
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

    public SelectionListOptionEntry<T> setItemEnableRequirement(@Nullable Function<T, OptionRequirement> itemEnableRequirement) {
        this.itemEnableRequirement = itemEnableRequirement;
        return this;
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
                .minWidth(SELECT_MIN_WIDTH)
                .displayFormatter(nameProvider);
        materialSelect.getMenu().style().colors(MaterialMenuColors.STANDARD);
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
    }

    @Override
    public T value() {
        return values.get(index.get());
    }

    @Override
    public void tick(RenderContext ctx) {
        boolean enabled = updateRequirements();
        materialSelect.setDisabled(!enabled);
        for (T itemValue : values) {
            if (itemEnableRequirement != null) {
                OptionRequirement requirement = itemEnableRequirement.apply(itemValue);
                boolean canSelect = requirement.check();
                materialSelect.getMenu().getItemByValue(
                        itemValue
                ).selectable(canSelect).setDisabled(!canSelect);
            } else {
                materialSelect.getMenu().getItemByValue(
                        itemValue
                ).selectable(true).setDisabled(false);
            }
        }
    }

    public boolean isEdited() {
        return !Objects.equals(index.get(), original);
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