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

package io.homo.superresolution.common.config.special;

import io.homo.superresolution.api.config.ModConfigSpecBuilder;
import io.homo.superresolution.core.impl.Pair;
import io.homo.superresolution.common.upscale.AlgorithmDescriptions;

import java.util.HashMap;
import java.util.Map;

public class SpecialConfigs {
    public FSR1SpecialConfig FSR1;
    public FSR2SpecialConfig FSR2;
    public FSRSpecialConfig FSR;
    public SGSR2SpecialConfig SGSR2;
    public SGSR1SpecialConfig SGSR1;

    public transient Map<String, Pair<SpecialConfig, String>> description = new HashMap<>();

    public SpecialConfigs(ModConfigSpecBuilder builder) {
        builder.comment("special", "Algorithm special configuration");
        FSR1 = new FSR1SpecialConfig(builder);
        FSR2 = new FSR2SpecialConfig(builder);
        FSR = new FSRSpecialConfig(builder);
        SGSR2 = new SGSR2SpecialConfig(builder);
        SGSR1 = new SGSR1SpecialConfig(builder);
        description.put("fsr1", Pair.of(FSR1, AlgorithmDescriptions.FSR1.getDisplayName()));
        description.put("fsr2", Pair.of(FSR2, AlgorithmDescriptions.FSR2.getDisplayName()));
        description.put("fsr", Pair.of(FSR, AlgorithmDescriptions.FSR.getDisplayName()));
        description.put("sgsr2", Pair.of(SGSR2, AlgorithmDescriptions.SGSR2.getDisplayName()));
        description.put("sgsr1", Pair.of(SGSR1, AlgorithmDescriptions.SGSR1.getDisplayName()));
    }
}
