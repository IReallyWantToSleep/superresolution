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

package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.common.config.SuperResolutionConfig;

import java.util.HashMap;
import java.util.Map;

public abstract class SpecialConfig {
    protected final ModConfigSpecBuilder specBuilder;

    public SpecialConfig(ModConfigSpecBuilder specBuilder) {
        this.specBuilder = specBuilder;
    }

    public Map<String, SpecialConfigDescription<?>> getDescriptions() {
        Map<String, SpecialConfigDescription<?>> descriptions = new HashMap<>();
        buildDescriptions(descriptions);
        return descriptions;
    }

    protected abstract void buildDescriptions(Map<String, SpecialConfigDescription<?>> map);

    protected SpecialConfigs getSpecialConfigs() {
        return SuperResolutionConfig.SPECIAL;
    }
}
