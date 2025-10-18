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

package io.homo.superresolution.common.gui.options;

import com.google.common.collect.ImmutableList;
import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.event.events.WidgetEvent;
import io.homo.superresolution.core.gui.core.layout.LinearLayout;
import io.homo.superresolution.core.gui.widgets.button.MaterialButton;
import io.homo.superresolution.core.gui.widgets.button.MaterialButtonSize;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

public class SelectionListOptionEntry<T> extends AbstractOptionEntry<T, LinearLayout, SelectionListOptionEntry<T>> {
    protected final ImmutableList<T> values;
    protected final AtomicInteger index;
    protected final int original;
    protected MaterialButton selectButton;
    protected MaterialLabel label;
    protected Function<T, String> nameProvider;

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
        this.container.setLayout(layout);
        initWidget();
    }

    @Override
    protected void initLayout() {
        layout = new LinearLayout();
    }

    @Override
    protected void initWidget() {
        selectButton = MaterialButton.create(MaterialButtonSize.Small)
                .text(() -> nameProvider.apply(value()));
        selectButton.onClick(event -> {
            index.incrementAndGet();
            index.compareAndSet(values.size(), 0);
            if (saveConsumer != null) {
                saveConsumer.accept(value());
            }
        });

        label = MaterialLabel.create()
                .text(() -> this.name.getString());

        container.addChild(label);
        container.addChild(selectButton);
        container.scheme(scheme);

        layout.setElementPosition(
                label,
                LinearLayout.HorizontalAlignment.LEFT,
                LinearLayout.VerticalAlignment.CENTER
        );
        layout.setElementPosition(
                selectButton,
                LinearLayout.HorizontalAlignment.RIGHT,
                LinearLayout.VerticalAlignment.CENTER
        );
    }

    @Override
    protected SelectionListOptionEntry<T> setScheme(MaterialScheme scheme) {
        selectButton.scheme(scheme);
        label.scheme(scheme);
        return super.setScheme(scheme);
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        selectButton.text(nameProvider.apply(value()));
        container.getLayout().layout(container);
        container.render(drawContext, inputState);
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
}