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

package io.homo.superresolution.core.graphics.impl.grape;

import io.homo.superresolution.core.graphics.impl.command.ICommandBuffer;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RenderGrape {
    private final Map<String, IGrapeJob> jobs = new LinkedHashMap<>();
    private final List<String> executionOrder = new ArrayList<>();

    public RenderGrape add(String name, IGrapeJob job) {
        jobs.put(name, job);
        executionOrder.add(name);
        return this;
    }

    public RenderGrape remove(String name) {
        jobs.remove(name);
        executionOrder.remove(name);
        return this;
    }

    public IGrapeJob get(String name) {
        return jobs.get(name);
    }

    public void execute(ICommandBuffer cmd) {
        for (String passName : executionOrder) {
            IGrapeJob pass = jobs.get(passName);
            if (pass != null) {
                pass.execute(cmd);
            }
        }
    }

    public void execute(ICommandBuffer cmd, String name) {
        IGrapeJob pass = jobs.get(name);
        if (pass != null) {
            pass.execute(cmd);
        }
    }

    public void destroy() {
        jobs.values().forEach(IGrapeJob::destroy);
        jobs.clear();
        executionOrder.clear();
    }
}
