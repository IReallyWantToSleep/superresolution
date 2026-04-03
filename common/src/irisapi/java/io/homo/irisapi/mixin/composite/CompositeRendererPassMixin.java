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

package io.homo.irisapi.mixin.composite;

import io.homo.irisapi.NamedCompositePass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = {"net.irisshaders.iris.pipeline.CompositeRenderer$Pass"})
public class CompositeRendererPassMixin implements NamedCompositePass {
    @Unique
    private String superresolution$name0;

    #if MC_VER < MC_1_20_1
    @Override
    public String superresolution$getName() {
        return superresolution$name0;
    }

    @Override
    public void superresolution$setName(String name) {
        superresolution$name0 = name;
    }
    #else
    @Shadow
    String name;

    @Override
    public String superresolution$getName() {
        return name;
    }

    @Override
    public void superresolution$setName(String name) {
        this.name = name;
    }
    #endif
}
