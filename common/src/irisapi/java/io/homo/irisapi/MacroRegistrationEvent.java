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

package io.homo.irisapi;

import net.irisshaders.iris.helpers.StringPair;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

public class MacroRegistrationEvent extends Event {
    private final List<StringPair> macros = new ArrayList<>();

    public void registerMacro(String name, String value) {
        macros.add(new StringPair(name, value));
    }

    public void registerMacros(List<StringPair> macros) {
        this.macros.addAll(macros);
    }

    public List<StringPair> getMacros() {
        return new ArrayList<>(macros);
    }
}
