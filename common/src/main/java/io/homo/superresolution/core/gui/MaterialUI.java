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

package io.homo.superresolution.core.gui;

import io.homo.superresolution.common.SuperResolution;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVG;
import net.neoforged.bus.EventBus;
import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;

public class MaterialUI {
    public static final IEventBus EVENT_BUS;

    static {
        EVENT_BUS = createEventBus("UI Main");
    }

    public static void init() {
        NanoVG.init();
        MaterialSymbols.init();
        EVENT_BUS.start();
    }

    public static IEventBus createEventBus(String name) {
        return BusBuilder.builder()
                .setExceptionHandler((
                        bus,
                        event,
                        listeners,
                        index,
                        throwable
                ) -> {
                    SuperResolution.LOGGER.error(
                            "{}: 处理事件 {} 时发生错误，监听器 {}",
                            name,
                            event.getClass(),
                            listeners[index]
                    );
                    throwable.printStackTrace();
                })
                .build();
    }
}
