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

package io.homo.superresolution.core.graphics.system;

import io.homo.superresolution.core.graphics.impl.BlendFactor;
import io.homo.superresolution.core.graphics.impl.DepthFunc;

public interface IRenderState {
    float[] viewport();

    IRenderState viewport(float x, float y, float w, float h);

    boolean depthTest();

    IRenderState depthTest(boolean enable);

    boolean depthWrite();

    IRenderState depthWrite(boolean enable);

    boolean blend();

    IRenderState blend(boolean enable);

    boolean cullFace();

    IRenderState cullFace(boolean enable);

    boolean stencilTest();

    IRenderState stencilTest(boolean enable);

    boolean colorMaskR();

    IRenderState colorMaskR(boolean enable);

    boolean colorMaskG();

    IRenderState colorMaskG(boolean enable);

    boolean colorMaskB();

    IRenderState colorMaskB(boolean enable);

    boolean colorMaskA();

    IRenderState colorMaskA(boolean enable);

    default IRenderState colorMask(boolean r, boolean g, boolean b, boolean a) {
        colorMaskR(r);
        colorMaskG(g);
        colorMaskB(b);
        colorMaskA(a);
        return this;
    }

    BlendFactor blendSrcFactor();

    IRenderState blendSrcFactor(BlendFactor factor);

    BlendFactor blendDstFactor();

    IRenderState blendDstFactor(BlendFactor factor);

    default IRenderState blendFunc(BlendFactor src, BlendFactor dst) {
        return blendSrcFactor(src).blendDstFactor(dst);
    }

    DepthFunc depthFunc();

    IRenderState depthFunc(DepthFunc func);

    StateSnapshot get();

    default void apply(StateSnapshot snapshot) {
        this.depthTest(snapshot.depthTest);
        this.depthWrite(snapshot.depthWrite);
        this.blend(snapshot.blend);
        this.cullFace(snapshot.cullFace);
        this.stencilTest(snapshot.stencilTest);
        this.colorMaskR(snapshot.colorMaskR);
        this.colorMaskG(snapshot.colorMaskG);
        this.colorMaskB(snapshot.colorMaskB);
        this.colorMaskA(snapshot.colorMaskA);
        this.blendSrcFactor(snapshot.blendSrcFactor);
        this.blendDstFactor(snapshot.blendDstFactor);
        this.depthFunc(snapshot.depthFunc);
    }

    class StateSnapshot {
        public final boolean depthTest;
        public final boolean depthWrite;
        public final boolean blend;
        public final boolean cullFace;
        public final boolean stencilTest;
        public final boolean colorMaskR;
        public final boolean colorMaskG;
        public final boolean colorMaskB;
        public final boolean colorMaskA;
        public final BlendFactor blendSrcFactor;
        public final BlendFactor blendDstFactor;
        public final DepthFunc depthFunc;
        public final float[] viewport;


        public StateSnapshot(IRenderState state) {
            this.depthTest = state.depthTest();
            this.depthWrite = state.depthWrite();
            this.blend = state.blend();
            this.cullFace = state.cullFace();
            this.stencilTest = state.stencilTest();
            this.colorMaskR = state.colorMaskR();
            this.colorMaskG = state.colorMaskG();
            this.colorMaskB = state.colorMaskB();
            this.colorMaskA = state.colorMaskA();
            this.blendSrcFactor = state.blendSrcFactor();
            this.blendDstFactor = state.blendDstFactor();
            this.depthFunc = state.depthFunc();
            this.viewport = state.viewport();
        }
    }

}
