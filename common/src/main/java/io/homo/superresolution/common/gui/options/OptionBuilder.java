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

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.ContainerWidget;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OptionBuilder {
    protected OptionCategory category;
    protected MaterialScheme scheme = MaterialScheme.defaultLight;
    protected List<AbstractOptionEntry<?, ?>> entries = new ArrayList<>();
    protected Runnable saveRunnable = () -> {
    };

    public OptionBuilder(OptionCategory category) {
        this.category = category;
    }

    public OptionBuilder setSaveRunnable(Runnable saveRunnable) {
        this.saveRunnable = saveRunnable;
        return this;
    }

    public OptionBuilder scheme(MaterialScheme scheme) {
        this.scheme = scheme;
        return this;
    }

    public <T extends Enum<T>> EnumSelectorBuilder<T> enumSelectorOption(
            Text name,
            Class<T> clazz,
            T value
    ) {
        return new EnumSelectorBuilder<>(name, clazz, value).setCategory(category);
    }

    public <T> SelectionListBuilder<T> selectorOption(
            Text name,
            T value,
            T[] values
    ) {
        return (SelectionListBuilder<T>) new SelectionListBuilder(name, value, values).setCategory(category);
    }

    public BooleanSwitchBuilder booleanOption(
            Text name,
            Boolean value
    ) {
        return new BooleanSwitchBuilder(name, value).setCategory(category);
    }

    public NumberSliderBuilder numberOption(
            Text name,
            Number value,
            Number max,
            Number min
    ) {
        return new NumberSliderBuilder(name, value, max, min).setCategory(category);
    }

    public OptionBuilder addEntry(AbstractOptionEntry<?, ?> entry) {
        entries.add(entry);
        entry.setScheme(scheme);
        return this;
    }

    public OptionsContainer build() {
        OptionsContainer container = new OptionsContainer(scheme);

        for (AbstractOptionEntry<?, ?> entry : category.getEntries()) {
            entry.setScheme(scheme);
            entry.setSaveRunnable(saveRunnable);
            container.addEntry(entry);
        }

        for (AbstractOptionEntry<?, ?> entry : entries) {
            entry.setScheme(scheme);
            entry.setSaveRunnable(saveRunnable);
            container.addEntry(entry);
        }

        return container;
    }

    public static class OptionsContainer extends ContainerWidget {
        private static final float CORNER_RADIUS = 16f;
        private static final float PADDING = 8f;
        private static final float GAP = 8f;
        private final MaterialScheme scheme;
        private final List<AbstractOptionEntry<?, ?>> entries = new ArrayList<>();

        public OptionsContainer(MaterialScheme scheme) {
            this.scheme = scheme;
            initLayout();
        }

        private void initLayout() {
            layout().setFlexDirection(YogaFlexDirection.COLUMN);
            layout().setWidthPercent(100);
            layout().setPadding(YogaEdge.ALL, PADDING);
            layout().setGap(YogaGutter.COLUMN, GAP);
        }

        public void addEntry(AbstractOptionEntry<?, ?> entry) {
            entries.add(entry);
            addChild(entry.getContainer());
            entry.setScheme(scheme);
        }

        public List<AbstractOptionEntry<?, ?>> getEntries() {
            return entries;
        }

        @Override
        protected void renderSelf(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CORNER_RADIUS,
                    scheme.surfaceContainerLow(),
                    true
            );
        }

        public void saveAll() {
            for (AbstractOptionEntry<?, ?> entry : entries) {
                if (entry.getSaveConsumer() != null) {
                    ((Consumer<Object>) entry.getSaveConsumer()).accept(entry.value());
                }
            }
        }
    }
}
