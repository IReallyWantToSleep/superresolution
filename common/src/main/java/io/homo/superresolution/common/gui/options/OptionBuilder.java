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
import io.homo.superresolution.core.gui.MaterialElevation;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.core.impl.Rectangle;
import io.homo.superresolution.core.gui.widgets.MaterialContainerWidget;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaEdge;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaFlexDirection;
import io.homo.superresolution.thirdparty.yoga.appliedenergistics.yoga.YogaGutter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OptionBuilder {
    protected OptionCategory category;
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

    public ColorSelectBuilder colorSelectOption(
            Text name,
            Color value
    ) {
        return new ColorSelectBuilder(name, value).setCategory(category);
    }

    public HintBuilder hintOption(Text name) {
        return new HintBuilder(name).setCategory(category);
    }

    public OptionBuilder addEntry(AbstractOptionEntry<?, ?> entry) {
        entries.add(entry);
        return this;
    }

    public OptionsContainer build() {
        OptionsContainer container = new OptionsContainer();

        for (AbstractOptionEntry<?, ?> entry : category.getEntries()) {
            entry.setSaveRunnable(saveRunnable);
            container.addEntry(entry);
        }

        for (AbstractOptionEntry<?, ?> entry : entries) {
            entry.setSaveRunnable(saveRunnable);
            container.addEntry(entry);
        }

        return container;
    }

    public static class OptionsContainer extends MaterialContainerWidget<OptionsContainer> {
        private static final float CORNER_RADIUS = 16f;
        private static final float PADDING = 8f;
        private static final float GAP = 8f;
        private final List<AbstractOptionEntry<?, ?>> entries = new ArrayList<>();

        public OptionsContainer() {
            initLayout();
        }

        @Override
        protected Rectangle getViewRegion() {
            return getBounds();
        }

        @Override
        protected void renderSelf(RenderContext ctx, UIInputState inputState) {
            Rectangle bounds = getBounds();
            MaterialElevation.draw(
                    ctx,
                    1,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CORNER_RADIUS
            );
            ctx.roundedRect(
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height,
                    CORNER_RADIUS,
                    scheme().surfaceContainer(),
                    true
            );
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
        }

        public List<AbstractOptionEntry<?, ?>> getEntries() {
            return entries;
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
