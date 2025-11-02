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

import io.homo.superresolution.common.gui.impl.Text;
import io.homo.superresolution.core.gui.MaterialScheme;
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSlider;
import io.homo.superresolution.core.gui.widgets.sliders.MaterialSliderSize;

import java.util.function.Function;

public class NumberSliderOptionEntry extends AbstractOptionEntry<Number, NumberSliderOptionEntry> {
    protected MaterialSlider slider;
    protected MaterialLabel label;
    protected Number max;
    protected Number min;
    protected Number step;
    protected Function<Number, String> valueFormater;

    public NumberSliderOptionEntry(
            Text name,
            Number value,
            Number max,
            Number min
    ) {
        super(name, value);
        this.max = max;
        this.min = min;
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
    protected NumberSliderOptionEntry setScheme(MaterialScheme scheme) {
        slider.scheme(scheme);
        label.scheme(scheme);
        return super.setScheme(scheme);
    }

    @Override
    protected void initWidget() {
        slider = MaterialSlider.create()
                .scheme(scheme);
        label = MaterialLabel.create()
                .text(() -> this.name.getString());
        container.addChild(slider);
        container.addChild(label);
        container.scheme(scheme);

        slider.style().valueIndicator(true);
        if (step != null && step.doubleValue() != 0) {
            slider.style().steps(true);
            slider.setStep(step);
        }
        slider.style().size(MaterialSliderSize.ExtraSmall);
        slider.setValueIndicatorTextFormater(valueFormater);
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(value);
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        container.render(drawContext, inputState);
    }
}
