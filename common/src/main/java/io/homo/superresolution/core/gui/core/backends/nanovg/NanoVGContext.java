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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;

import org.joml.Vector2f;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.MinecraftUtil;
import io.homo.superresolution.core.utils.UIScalingCalculator;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.opengl.GlStates;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoSVG;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL42;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL30.*;

public class NanoVGContext {

    private static Transform S_currentTransform;
    private static Transform S_globalTransform;
    public GlFrameBuffer frameBuffer;
    public long contextPtr = -1;
    public long rastPtr = -1;
    public float globalScale = 1.0f;
    /// 状态
    private Color S_fillColor = Color.black();
    private Color S_strokeColor = Color.black();
    private float S_strokeWidth = 1f;
    private float S_alpha = 1f;

    public NanoVGContext(int nvgFlags) {
        contextPtr = NanoVGGL3.nvgCreate(nvgFlags);
        rastPtr = NanoSVG.nsvgCreateRasterizer();
        frameBuffer = GlFrameBuffer.create(
                TextureFormat.R11G11B10F,
                TextureFormat.DEPTH24_STENCIL8,
                (int) MinecraftUtil.getScreenSize().x,
                (int) MinecraftUtil.getScreenSize().y
        );
        frameBuffer.setClearColorRGBA(0, 0, 0, 1);
    }

    public float globalScale() {
        return globalScale;
    }

    ///

    public void globalScale(float globalScale) {
        this.globalScale = globalScale;
    }

    public void begin(boolean copyMinecraftFbo) {
        Vector2f screenSize = MinecraftUtil.getScreenSize();
        GlStates.save("nanovg-frame");
        //if (
        //        frameBuffer.getWidth() != ((int) screenSize.x) ||
        //                frameBuffer.getHeight() != ((int) screenSize.y)
        //) {
        //    frameBuffer.resizeFrameBuffer((int) screenSize.x, (int) screenSize.y);
        //}
        //frameBuffer.bind(FrameBufferBindPoint.All);
        //glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
        if (false) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, (int) RenderHandlerManager.getOriginRenderTarget().handle());
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) frameBuffer.handle());
            glBlitFramebuffer(
                    0,
                    0,
                    Minecraft.getInstance().getMainRenderTarget().width,
                    Minecraft.getInstance().getMainRenderTarget().height,
                    0,
                    0,
                    frameBuffer.getWidth(),
                    frameBuffer.getHeight(),
                    GL_COLOR_BUFFER_BIT,
                    GL_LINEAR
            );
        }

        NanoVG.nvgBeginFrame(
                contextPtr,
                screenSize.x,
                screenSize.y,
                1.0f
        );
        nvgReset(contextPtr);
        nvgScale(contextPtr, 1, 1);
        globalScale = (float) Math.max(UIScalingCalculator.calculateUIScaling((int) screenSize.x, (int) screenSize.y, 1.2f), 1);
    }

    public void end() {
        //glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) frameBuffer.handle());
        NanoVG.nvgEndFrame(contextPtr);
        //glBindFramebuffer(GL_READ_FRAMEBUFFER, (int) frameBuffer.handle());
        //glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) RenderHandlerManager.getOriginRenderTarget().handle());
        //glEnable(GL_BLEND);
        //GL42.glBlendFuncSeparate(GL_ONE, GL_ZERO, GL_ZERO, GL_ONE);
        //glBlitFramebuffer(
        //        0,
        //        0,
        //        frameBuffer.getWidth(),
        //        frameBuffer.getHeight(),
        //        0,
        //        0,
        //        Minecraft.getInstance().getMainRenderTarget().width,
        //        Minecraft.getInstance().getMainRenderTarget().height,
        //        GL_COLOR_BUFFER_BIT,
        //        GL_LINEAR
        //);
        GlStates.pop("nanovg-frame").restore();

    }

    public void save() {
        NanoVG.nvgSave(contextPtr);
    }

    public void restore() {
        NanoVG.nvgRestore(contextPtr);
    }

    public void line(
            float x1, float y1,
            float x2, float y2
    ) {
        NanoVG.nvgMoveTo(contextPtr, x1, y1);
        NanoVG.nvgLineTo(contextPtr, x2, y2);
    }

    public void rect(
            float x, float y,
            float width, float height
    ) {
        NanoVG.nvgRect(contextPtr, x, y, width, height);
    }

    public void roundedRect(
            float x, float y,
            float width, float height,
            float radius
    ) {
        NanoVG.nvgRoundedRect(contextPtr, x, y, width, height, radius);
    }

    public void beginPath() {
        NanoVG.nvgBeginPath(contextPtr);
    }

    public void endPath(boolean fill) {
        if (fill) {
            NanoVG.nvgFill(contextPtr);
        } else {
            NanoVG.nvgStroke(contextPtr);
        }
    }

    public void endPath() {
        NanoVG.nvgFill(contextPtr);
    }

    public void drawLine(
            float x1, float y1,
            float x2, float y2,
            Color color,
            float lineWidth
    ) {
        beginPath();
        fillColor(color);
        strokeWidth(lineWidth);
        line(x1, y1, x2, y2);
        endPath();
    }

    public void drawRect(
            float x, float y,
            float width, float height,
            Color color,
            boolean fill
    ) {
        beginPath();
        drawColor(fill, color);
        rect(x, y, width, height);
        endPath(fill);
    }

    public void drawRoundedRect(
            float x, float y,
            float width, float height,
            float radius,
            Color color,
            boolean fill
    ) {
        beginPath();
        drawColor(fill, color);
        roundedRect(x, y, width, height, radius);
        endPath(fill);
    }

    public NVGPaint imagePattern(
            float ox,
            float oy,
            float ex,
            float ey,
            float width,
            float height,
            float angle,
            float alpha,
            int image
    ) {
        return nvgImagePattern(
                contextPtr,
                ox,
                oy,
                ex,
                ey,
                angle,
                image,
                alpha,
                NVGPaint.calloc()
        );
    }

    public float globalAlpha() {
        return S_alpha;
    }

    public void globalAlpha(float alpha) {
        S_alpha = alpha;
    }

    public void resetScissor() {
        NanoVG.nvgResetScissor(contextPtr);
    }

    public void scissor(
            float x,
            float y,
            float width,
            float height
    ) {
        NanoVG.nvgScissor(
                contextPtr,
                x,
                y,
                width,
                height
        );
    }

    public void transform(Transform transform) {
        resetTransform();
        if (S_globalTransform != null) {
            float[] globalMat = S_globalTransform.transformMatrix();
            nvgTransform(contextPtr, globalMat[0], globalMat[1], globalMat[2],
                    globalMat[3], globalMat[4], globalMat[5]);
        }

        if (transform != null) {
            float[] mat = transform.transformMatrix();
            nvgTransform(contextPtr, mat[0], mat[1], mat[2], mat[3], mat[4], mat[5]);
            S_currentTransform = transform;
        }
    }

    public void globalTransform(Transform globalTransform) {
        if (globalTransform == null) {
            S_globalTransform = null;
            if (S_currentTransform != null) {
                transform(S_currentTransform);
            }
            return;
        }
        S_globalTransform = globalTransform;
        transform(null);
    }

    public void resetTransform() {
        nvgResetTransform(contextPtr);
        float[] globalScaleMat = Transform.identity().scale(globalScale).transformMatrix();
        nvgTransform(contextPtr, globalScaleMat[0], globalScaleMat[1], globalScaleMat[2],
                globalScaleMat[3], globalScaleMat[4], globalScaleMat[5]);
        S_currentTransform = null;
    }

    public void resetGlobalTransform() {
        nvgResetTransform(contextPtr);
        float[] globalScaleMat = Transform.identity().scale(globalScale).transformMatrix();
        nvgTransform(contextPtr, globalScaleMat[0], globalScaleMat[1], globalScaleMat[2],
                globalScaleMat[3], globalScaleMat[4], globalScaleMat[5]);
        S_globalTransform = null;
    }


    public NVGPaint linearGradient(
            float startX,
            float startY,
            float endX,
            float endY,
            Color from,
            Color to
    ) {
        return linearGradient(
                startX,
                startY,
                endX,
                endY,
                from,
                to,
                NVGPaint.calloc()
        );
    }

    public NVGPaint linearGradient(
            float startX,
            float startY,
            float endX,
            float endY,
            Color from,
            Color to,
            NVGPaint srcPaint
    ) {
        from = from.copy().alpha((int) (globalAlpha() * from.alpha()));
        to = to.copy().alpha((int) (globalAlpha() * to.alpha()));

        NVGPaint paint = NanoVG.nvgLinearGradient(
                contextPtr,
                startX,
                startY,
                endX,
                endY,
                from.nvg(),
                to.nvg(),
                srcPaint
        );
        nvgFillPaint(contextPtr, paint);
        return paint;
    }

    public void strokeWidth(float width) {
        S_strokeWidth = width;
        NanoVG.nvgStrokeWidth(contextPtr, width);
    }

    public void strokeColor(Color color) {
        color = color.copy().alpha((int) (globalAlpha() * color.alpha()));
        S_strokeColor = Color.rgba(color.integer());
        NanoVG.nvgStrokeColor(contextPtr, color.nvg());
    }

    public void fillColor(Color color) {
        color = color.copy().alpha((int) (globalAlpha() * color.alpha()));
        S_fillColor = Color.rgba(color.integer());
        NanoVG.nvgFillColor(contextPtr, color.nvg());
    }


    public void drawColor(boolean fill, Color color) {
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
    }

    public void fontSize(float size) {
        nvgFontSize(contextPtr, size);
    }

    public Color fillColor() {
        return S_fillColor;
    }

    public Color strokeColor() {
        return S_strokeColor;
    }

    public float strokeWidth() {
        return S_strokeWidth;
    }
}