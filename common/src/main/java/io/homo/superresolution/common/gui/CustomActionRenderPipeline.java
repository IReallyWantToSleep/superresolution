/*
 * Super Resolution
 * Copyright (c) 2026. 187J3X1-114514
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

package io.homo.superresolution.common.gui;
#if MC_VER > MC_1_21_5

import net.minecraft.client.renderer.ShaderDefines;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
#endif

#if MC_VER > MC_1_21_10
import net.minecraft.resources.Identifier;
#else
import net.minecraft.resources.ResourceLocation;
#endif

import java.util.List;
import java.util.Optional;

#if MC_VER > MC_1_21_5
public class CustomActionRenderPipeline extends RenderPipeline {
    private Runnable action;

    public CustomActionRenderPipeline(
            Runnable action
    ) {
        super(
                #if MC_VER > MC_1_21_10
                Identifier.withDefaultNamespace(""),
                Identifier.withDefaultNamespace(""),
                Identifier.withDefaultNamespace(""),
                #else
                ResourceLocation.withDefaultNamespace(""),
                ResourceLocation.withDefaultNamespace(""),
                ResourceLocation.withDefaultNamespace(""),
                #endif
                ShaderDefines.EMPTY,
                List.of(),
                List.of(),
                Optional.of(BlendFunction.ADDITIVE),
                DepthTestFunction.NO_DEPTH_TEST,
                PolygonMode.FILL,
                false,
                false,
                false,
                false,
                LogicOp.NONE,
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                1,
                1,
                1
        );
        this.action = action;
    }

    public Runnable getAction() {
        return action;
    }
}

#else
public class CustomActionRenderPipeline {
}
#endif