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

import io.homo.superresolution.common.gui.impl.OptionRequirement;
import io.homo.superresolution.common.gui.impl.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AbstractOptionBuilder<VT, OT extends AbstractOptionEntry<VT, ?, OT>, SELF> {
    protected boolean requireRestartGame = false;
    protected @Nullable Supplier<VT> defaultValue = null;
    protected @Nullable Function<VT, Optional<Text>> errorSupplier;
    protected @Nullable OptionRequirement enableRequirement = null;
    protected @Nullable OptionRequirement displayRequirement = null;
    protected Consumer<VT> saveConsumer = null;
    protected Function<VT, Optional<Text[]>> tooltipSupplier = (list) -> Optional.empty();
    protected VT value;
    protected Text name;

    public abstract OT build();

    protected OT finishBuild(OT option) {
        option.setDefaultValue(defaultValue);
        option.setErrorSupplier(errorSupplier);
        option.setEnableRequirement(enableRequirement);
        option.setDisplayRequirement(displayRequirement);
        option.setRequiresRestartGame(requireRestartGame);
        option.setSaveConsumer(saveConsumer);
        option.setTooltipSupplier(tooltipSupplier);
        return option;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setValue(VT value) {
        this.value = value;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setName(Text name) {
        this.name = name;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setRequireRestartGame(boolean requireRestartGame) {
        this.requireRestartGame = requireRestartGame;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setDefaultValue(@Nullable Supplier<VT> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setErrorSupplier(@Nullable Function<VT, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setEnableRequirement(@Nullable OptionRequirement enableRequirement) {
        this.enableRequirement = enableRequirement;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setDisplayRequirement(@Nullable OptionRequirement displayRequirement) {
        this.displayRequirement = displayRequirement;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setSaveConsumer(Consumer<VT> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public AbstractOptionBuilder<VT, OT, SELF> setTooltipSupplier(Function<VT, Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }
}
