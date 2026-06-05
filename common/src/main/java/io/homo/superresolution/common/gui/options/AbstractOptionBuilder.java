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
import io.homo.superresolution.core.gui.core.impl.Tooltip;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractOptionBuilder<VT, OT extends AbstractOptionEntry<VT, OT>, SELF> {
    protected boolean requireRestartGame = false;
    protected @Nullable Supplier<VT> defaultValue = null;
    protected @Nullable Function<VT, Optional<Text>> errorSupplier;
    protected @Nullable OptionRequirement enableRequirement = null;
    protected @Nullable OptionRequirement displayRequirement = null;
    protected Function<VT, Boolean> saveConsumer = null;
    protected Function<VT, Optional<Tooltip>> tooltipSupplier = (list) -> Optional.of(Tooltip.empty());
    protected Function<VT, Optional<Text[]>> descriptionsSupplier = (v) -> Optional.empty();
    protected VT value;
    protected Text name;
    protected OptionCategory category;

    public AbstractOptionBuilder(Text name, VT value) {
        this.name = name;
        this.value = value;
    }

    protected OptionCategory getCategory() {
        return category;
    }

    protected SELF setCategory(OptionCategory category) {
        this.category = category;
        return (SELF) this;
    }

    public abstract OT build();

    protected OT finishBuild(OT option) {
        option.setDefaultValue(defaultValue);
        option.setErrorSupplier(errorSupplier);
        option.setEnableRequirement(enableRequirement);
        option.setDisplayRequirement(displayRequirement);
        option.setRequiresRestartGame(requireRestartGame);
        option.setSaveConsumer(saveConsumer);
        option.setTooltipSupplier(tooltipSupplier);
        option.setDescriptionsSupplier(descriptionsSupplier);

        category.addEntry(option);
        return option;
    }

    public SELF setValue(VT value) {
        this.value = value;
        return (SELF) this;
    }

    public SELF setName(Text name) {
        this.name = name;
        return (SELF) this;
    }

    public SELF setRequireRestartGame(boolean requireRestartGame) {
        this.requireRestartGame = requireRestartGame;
        return (SELF) this;
    }

    public SELF setDefaultValue(@Nullable Supplier<VT> defaultValue) {
        this.defaultValue = defaultValue;
        return (SELF) this;
    }

    public SELF setErrorSupplier(@Nullable Function<VT, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return (SELF) this;
    }

    public SELF setEnableRequirement(@Nullable OptionRequirement enableRequirement) {
        this.enableRequirement = enableRequirement;
        return (SELF) this;
    }

    public SELF setDisplayRequirement(@Nullable OptionRequirement displayRequirement) {
        this.displayRequirement = displayRequirement;
        return (SELF) this;
    }

    public SELF setSaveConsumer(Function<VT, Boolean> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return (SELF) this;
    }

    public SELF setSaveConsumer(Consumer<VT> saveConsumer) {
        this.saveConsumer = (v) -> {
            saveConsumer.accept(v);
            return true;
        };
        return (SELF) this;
    }

    public SELF setTooltipSupplier(Function<VT, Optional<Tooltip>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return (SELF) this;
    }

    public SELF setDescription(Text description) {
        this.descriptionsSupplier = (v) -> Optional.of(new Text[]{description});
        return (SELF) this;
    }

    public SELF setDescription(String description) {
        return setDescription(Text.literal(description));
    }

    public SELF setDescriptions(Text... descriptions) {
        this.descriptionsSupplier = (v) -> Optional.of(descriptions);
        return (SELF) this;
    }

    public SELF setDescriptionsSupplier(Function<VT, Optional<Text[]>> supplier) {
        this.descriptionsSupplier = supplier;
        return (SELF) this;
    }
}
