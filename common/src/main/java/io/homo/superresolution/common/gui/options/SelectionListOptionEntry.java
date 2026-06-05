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

import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuColors;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuItem;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuItemSize;
import io.homo.superresolution.core.gui.widgets.menu.MaterialMenuSize;
import io.homo.superresolution.core.gui.widgets.select.MaterialSelect;
import io.homo.superresolution.core.gui.widgets.select.MaterialSelectSize;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaAlign;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class SelectionListOptionEntry<T> extends AbstractOptionEntry<T, SelectionListOptionEntry<T>> {
    private static final float SELECT_MIN_WIDTH = 150f;
    protected final AtomicInteger index;
    protected final T originalValue;
    protected List<T> values;
    protected ContainerWidget selectorContainer;
    protected MaterialSelect<T> materialSelect;
    protected Function<T, String> nameProvider;
    protected @Nullable Function<T, OptionRequirement> itemEnableRequirement = null;
    protected @Nullable Supplier<List<T>> valuesSupplier = null;
    protected @Nullable Function<T, Optional<Tooltip>> menuItemTooltipSupplier = null;
    protected String headerText = "";

    public SelectionListOptionEntry(
            Text name,
            T value,
            List<T> values,
            Function<T, String> nameProvider
    ) {
        super(name, value);
        this.values = new ArrayList<>(values);
        if (this.values.isEmpty() && value != null) {
            this.values.add(value);
        }
        this.index = new AtomicInteger(values.indexOf(value));
        this.index.compareAndSet(-1, 0);
        this.originalValue = value;
        this.nameProvider = nameProvider;
        init();
    }

    public SelectionListOptionEntry<T> setItemEnableRequirement(@Nullable Function<T, OptionRequirement> itemEnableRequirement) {
        this.itemEnableRequirement = itemEnableRequirement;
        return this;
    }

    public SelectionListOptionEntry<T> setValuesSupplier(@Nullable Supplier<List<T>> valuesSupplier) {
        this.valuesSupplier = valuesSupplier;
        return this;
    }

    public SelectionListOptionEntry<T> setMenuItemTooltip(@Nullable Function<T, Optional<Tooltip>> tooltipSupplier) {
        this.menuItemTooltipSupplier = tooltipSupplier;
        return this;
    }

    public List<T> getValues() {
        return Collections.unmodifiableList(values);
    }

    public SelectionListOptionEntry<T> setValues(List<T> newValues) {
        applyValues(newValues, value());
        return this;
    }

    public SelectionListOptionEntry<T> refreshDynamicValues() {
        if (valuesSupplier != null) {
            List<T> supplied = valuesSupplier.get();
            if (supplied != null) {
                applyValues(supplied, value());
            }
        }
        return this;
    }

    public SelectionListOptionEntry<T> setSelectedValue(T newValue) {
        if (newValue == null) {
            return this;
        }
        int newIndex = values.indexOf(newValue);
        if (newIndex < 0) {
            return this;
        }
        index.set(newIndex);
        this.value = newValue;
        if (materialSelect != null) {
            materialSelect.setValue(newValue);
        }
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

    @SuppressWarnings("unchecked")
    @Override
    protected void initWidget() {
        selectorContainer = new ContainerWidget();
        selectorContainer.layout().setFlexDirection(YogaFlexDirection.COLUMN);
        selectorContainer.layout().setAlignItems(YogaAlign.FLEX_END);

        materialSelect = MaterialSelect.<T>create()
                .minWidth(SELECT_MIN_WIDTH)
                .displayFormatter(nameProvider);
        materialSelect.style().size(MaterialSelectSize.Standard);
        materialSelect.getMenu().style().size(MaterialMenuSize.Standard);
        materialSelect.getMenu().style().colors(MaterialMenuColors.STANDARD);
        if (headerText != null && !headerText.isEmpty()) {
            materialSelect.label(headerText);
        }

        for (T itemValue : values) {
            Supplier<Optional<Tooltip>> tooltip = menuItemTooltipSupplier != null
                    ? () -> menuItemTooltipSupplier.apply(itemValue)
                    : null;
            materialSelect.addOption(itemValue, nameProvider.apply(itemValue), tooltip);
        }
        materialSelect.setValue(value());


        materialSelect.onChange((WidgetEvent.ChangeEvent event) -> {
            if (Objects.equals(event.getNewValue(), this.value) || Objects.equals(event.getNewValue(), event.getOldValue())) {
                return;
            }
            int newIndex = values.indexOf(((T) event.getNewValue()));
            if (newIndex >= 0) {
                index.set(newIndex);
                this.value = ((T) event.getNewValue());
                if (saveConsumer != null) {
                    if (!saveConsumer.apply(((T) event.getNewValue()))) {
                        materialSelect.getMenu().deselectItemQuietly(event.getNewValue());
                        materialSelect.setValue((T) event.getOldValue());

                        index.set(values.indexOf(((T) event.getOldValue())));
                        this.value = ((T) event.getOldValue());
                    }
                }
                if (saveRunnable != null) {
                    saveRunnable.run();
                }
            }
        });

        selectorContainer.addChild(materialSelect);
        materialSelect.setTooltipSupplier(this::resolveTooltip);

        container.addControl(selectorContainer);
    }

    @Override
    public T value() {
        if (values.isEmpty()) {
            return value;
        }
        int currentIndex = index.get();
        if (currentIndex < 0 || currentIndex >= values.size()) {
            currentIndex = Math.max(0, Math.min(values.size() - 1, currentIndex));
            index.set(currentIndex);
        }
        return values.get(currentIndex);
    }

    @Override
    public void tick(RenderContext ctx) {
        refreshDynamicValues();
        boolean enabled = updateRequirements();
        materialSelect.setDisabled(!enabled);
        materialSelect.getMenu().itemStyle(s -> s.size(MaterialMenuItemSize.Standard));
        for (T itemValue : values) {
            MaterialMenuItem menuItem = materialSelect.getMenu().getItemByValue(itemValue);
            if (menuItem == null) {
                continue;
            }
            if (itemEnableRequirement != null) {
                OptionRequirement requirement = itemEnableRequirement.apply(itemValue);
                boolean canSelect = requirement.check();
                menuItem.selectable(canSelect).setDisabled(!canSelect);
            } else {
                menuItem.selectable(true).setDisabled(false);
            }
        }
    }

    public boolean isEdited() {
        return !Objects.equals(value(), originalValue);
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

    private void applyValues(List<T> newValues, @Nullable T preferredValue) {
        List<T> sanitizedValues = newValues == null ? Collections.emptyList() : new ArrayList<>(newValues);
        if (sanitizedValues.isEmpty()) {
            if (preferredValue != null) {
                sanitizedValues.add(preferredValue);
            } else if (!values.isEmpty()) {
                sanitizedValues.add(values.get(0));
            } else {
                return;
            }
        }

        if (sanitizedValues.equals(this.values)) {
            return;
        }

        T previousValue = preferredValue != null ? preferredValue : value();
        int previousIndex = index.get();
        this.values = sanitizedValues;

        if (materialSelect != null) {
            materialSelect.clearOptions();
            for (T itemValue : this.values) {
                materialSelect.addOption(itemValue, nameProvider.apply(itemValue));
            }
        }

        T selectedValue;
        if (this.values.contains(previousValue)) {
            selectedValue = previousValue;
        } else if (previousIndex >= 0 && previousIndex < this.values.size()) {
            selectedValue = this.values.get(previousIndex);
        } else {
            selectedValue = this.values.get(0);
        }
        int selectedIndex = this.values.indexOf(selectedValue);
        index.set(Math.max(0, selectedIndex));
        this.value = selectedValue;
        if (materialSelect != null) {
            materialSelect.setValue(selectedValue);
        }
    }
}