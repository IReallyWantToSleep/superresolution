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
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.interfaces.IUIDrawContext;
import io.homo.superresolution.core.gui.core.layout.LinearLayout;
import io.homo.superresolution.core.gui.widgets.label.MaterialLabel;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;

public class BooleanSwitchOptionEntry extends AbstractOptionEntry<Boolean, LinearLayout, BooleanSwitchOptionEntry> {
    protected MaterialSwitch aSwitch;
    protected MaterialLabel label;


    public BooleanSwitchOptionEntry(Text name, Boolean value) {
        super(name, value);
    }

    @Override
    protected void initLayout() {
        layout = new LinearLayout();
    }

    @Override
    protected void initWidget() {
        aSwitch = MaterialSwitch.create()
                .scheme(scheme);
        label = MaterialLabel.create()
                .text(() -> this.name.getString());
        container.addChild(aSwitch);
        container.addChild(label);
        layout.setElementPosition(
                label,
                LinearLayout.HorizontalAlignment.LEFT,
                LinearLayout.VerticalAlignment.CENTER
        );
        layout.setElementPosition(
                aSwitch,
                LinearLayout.HorizontalAlignment.RIGHT,
                LinearLayout.VerticalAlignment.CENTER
        );
    }

    @Override
    public void render(IUIDrawContext drawContext, UIInputState inputState) {
        container.render(drawContext, inputState);
    }
}
