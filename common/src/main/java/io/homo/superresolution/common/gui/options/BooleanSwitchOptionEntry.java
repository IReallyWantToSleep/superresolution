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
import io.homo.superresolution.core.gui.core.UIInputState;
import io.homo.superresolution.core.gui.core.backends.render.RenderContext;
import io.homo.superresolution.core.gui.widgets.switchs.MaterialSwitch;

public class BooleanSwitchOptionEntry extends AbstractOptionEntry<Boolean, BooleanSwitchOptionEntry> {
    protected MaterialSwitch aSwitch;

    public BooleanSwitchOptionEntry(Text name, Boolean value) {
        super(name, value);
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
    protected void initWidget() {
        aSwitch = MaterialSwitch.create()
                .setChecked(value);
        aSwitch.onChange(event -> {
            this.value = (Boolean) event.getNewValue();
            if (saveConsumer != null) {
                saveConsumer.accept(this.value);
            }
            if (saveRunnable != null) {
                saveRunnable.run();
            }
        });
        container.addControl(aSwitch);
    }

    @Override
    public void tick(RenderContext ctx) {
        boolean enabled = updateRequirements();
        aSwitch.setDisabled(!enabled);
    }

    @Override
    public Boolean value() {
        return aSwitch.isChecked();
    }
}
