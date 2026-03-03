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
import io.homo.superresolution.core.utils.Color;

import java.util.function.Consumer;
import java.util.function.Function;

public class NumberSliderBuilder extends AbstractOptionBuilder<Number, NumberSliderOptionEntry, NumberSliderBuilder> {
    protected Number step;
    protected Number max;
    protected Number min;
    protected Number value;
    protected Function<Number, String> valueFormater;
    protected Consumer<Number> valueChangeListener = (v)->{};

    public NumberSliderBuilder(Text name, Number value, Number max, Number min) {
        super(name, value);
        this.max = max;
        this.min = min;
        this.value = value;
    }
    public Consumer<Number> getValueChangeListener() {
        return valueChangeListener;
    }

    public NumberSliderBuilder setValueChangeListener(Consumer<Number> valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
        return this;
    }
    public Number getValue() {
        return value;
    }

    @Override
    public NumberSliderBuilder setValue(Number value) {
        this.value = value;
        return this;
    }

    public Function<Number, String> getValueFormater() {
        return valueFormater;
    }

    public NumberSliderBuilder setValueFormater(Function<Number, String> valueFormater) {
        this.valueFormater = valueFormater;
        return this;
    }

    @Override
    public NumberSliderOptionEntry build() {
        NumberSliderOptionEntry entry = new NumberSliderOptionEntry(
                this.name,
                this.value,
                max,
                min
        );
        entry.step = step;
        if (step != null && step.doubleValue() != 0) {
            entry.slider.style().steps(false);
            entry.slider.setStep(step);
        }
        entry.valueFormater = valueFormater;
        entry.valueChangeListener = this.valueChangeListener;
        return finishBuild(entry);
    }

    public Number getStep() {
        return step;
    }

    public NumberSliderBuilder setStep(Number step) {
        this.step = step;
        return this;
    }

    public Number getMax() {
        return max;
    }

    public NumberSliderBuilder setMax(Number max) {
        this.max = max;
        return this;
    }

    public Number getMin() {
        return min;
    }

    public NumberSliderBuilder setMin(Number min) {
        this.min = min;
        return this;
    }
}
