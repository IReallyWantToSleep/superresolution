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

package io.homo.superresolution.core.gui.core.backends.nanovg;

import io.homo.superresolution.common.minecraft.MinecraftWindow;
import io.homo.superresolution.common.minecraft.handler.RenderHandlerManager;
import io.homo.superresolution.core.graphics.impl.framebuffer.FrameBufferBindPoint;
import io.homo.superresolution.core.graphics.impl.texture.TextureFormat;
import io.homo.superresolution.core.graphics.opengl.GlStates;
import io.homo.superresolution.core.graphics.opengl.framebuffer.GlFrameBuffer;
import io.homo.superresolution.core.gui.core.backends.interfaces.Transform;
import io.homo.superresolution.core.utils.Color;
import io.homo.superresolution.core.utils.UIScalingCalculator;
import io.homo.superresolution.thirdparty.nanovg.NanoVGColor;
import io.homo.superresolution.thirdparty.nanovg.NanoVGPaint;
import net.minecraft.client.Minecraft;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL42;

import static org.lwjgl.opengl.GL30.*;

public class NanoVGContextWrapper {

    private static Transform S_currentTransform;
    private static Transform S_globalTransform;
    public GlFrameBuffer frameBuffer;
    public io.homo.superresolution.thirdparty.nanovg.NanoVGContext rawContext;
    public long rastPtr = -1;
    public float globalScale = 1.0f;
    /// 状态
    private Color S_fillColor = Color.black();
    private Color S_strokeColor = Color.black();
    private float S_strokeWidth = 1f;
    private float S_alpha = 1f;

    public NanoVGContextWrapper(int nvgFlags) {
        rawContext = new io.homo.superresolution.thirdparty.nanovg.NanoVGContext(nvgFlags);
        rastPtr = 0;
        frameBuffer = GlFrameBuffer.create(
                TextureFormat.R11G11B10F,
                TextureFormat.DEPTH24_STENCIL8,
                (int) MinecraftWindow.getWindowWidth(),
                (int) MinecraftWindow.getWindowHeight()
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
        Vector2f screenSize = MinecraftWindow.getWindowSize();
        GlStates.save("nanovg-frame");
        if (
                frameBuffer.getWidth() != ((int) screenSize.x) ||
                        frameBuffer.getHeight() != ((int) screenSize.y)
        ) {
            frameBuffer.resizeFrameBuffer((int) screenSize.x, (int) screenSize.y);
        }
        frameBuffer.bind(FrameBufferBindPoint.All);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
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
        globalScale = (float) Math.max(UIScalingCalculator.calculateUIScaling((int) screenSize.x, (int) screenSize.y, 1.2f), 1);
        rawContext.beginFrame(
                screenSize.x,
                screenSize.y,
                globalScale * 1.2f
        );
        rawContext.reset();
        rawContext.scale(1, 1);
    }

    public void end() {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) frameBuffer.handle());
        rawContext.endFrame();
        glBindFramebuffer(GL_READ_FRAMEBUFFER, (int) frameBuffer.handle());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, (int) RenderHandlerManager.getOriginRenderTarget().handle());
        glEnable(GL_BLEND);
        GL42.glBlendFuncSeparate(GL_ONE, GL_ZERO, GL_ZERO, GL_ONE);

        glBlitFramebuffer(
                0,
                0,
                frameBuffer.getWidth(),
                frameBuffer.getHeight(),
                0,
                0,
                Minecraft.getInstance().getMainRenderTarget().width,
                Minecraft.getInstance().getMainRenderTarget().height,
                GL_COLOR_BUFFER_BIT,
                GL_LINEAR
        );
        GlStates.pop("nanovg-frame").restore();

    }

    public void save() {
        rawContext.save();
    }

    public void restore() {
        rawContext.restore();
    }

    public void line(
            float x1, float y1,
            float x2, float y2
    ) {
        rawContext.moveTo(x1, y1);
        rawContext.lineTo(x2, y2);
    }

    public void rect(
            float x, float y,
            float width, float height
    ) {
        rawContext.rect(x, y, width, height);
    }

    public void roundedRect(
            float x, float y,
            float width, float height,
            float radius
    ) {
        rawContext.roundedRect(x, y, width, height, radius);
    }

    public void beginPath() {
        rawContext.beginPath();
    }

    public void endPath(boolean fill) {
        if (fill) {
            rawContext.fill();
        } else {
            rawContext.stroke();
        }
    }

    public void endPath() {
        rawContext.fill();
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

    public NanoVGPaint imagePattern(
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
        return rawContext.imagePattern(
                ox,
                oy,
                ex,
                ey,
                angle,
                image,
                alpha
        );
    }

    public float globalAlpha() {
        return S_alpha;
    }

    public void globalAlpha(float alpha) {
        S_alpha = alpha;
    }

    public void resetScissor() {
        rawContext.resetScissor();
    }

    public void scissor(
            float x,
            float y,
            float width,
            float height
    ) {
        rawContext.scissor(
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
            rawContext.transform(globalMat[0], globalMat[1], globalMat[2],
                    globalMat[3], globalMat[4], globalMat[5]);
        }

        if (transform != null) {
            float[] mat = transform.transformMatrix();
            rawContext.transform(mat[0], mat[1], mat[2], mat[3], mat[4], mat[5]);
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
        rawContext.resetTransform();
        float[] globalScaleMat = Transform.identity().scale(globalScale).transformMatrix();
        rawContext.transform(globalScaleMat[0], globalScaleMat[1], globalScaleMat[2],
                globalScaleMat[3], globalScaleMat[4], globalScaleMat[5]);
        S_currentTransform = null;
    }

    public void resetGlobalTransform() {
        rawContext.resetTransform();
        float[] globalScaleMat = Transform.identity().scale(globalScale).transformMatrix();
        rawContext.transform(globalScaleMat[0], globalScaleMat[1], globalScaleMat[2],
                globalScaleMat[3], globalScaleMat[4], globalScaleMat[5]);
        S_globalTransform = null;
    }

    public NanoVGColor color(Color color) {
        return rawContext.colorRGBA(
                color.red(),
                color.green(),
                color.blue(),
                color.alpha()
        );
    }

    public NanoVGPaint linearGradient(
            float startX,
            float startY,
            float endX,
            float endY,
            Color from,
            Color to
    ) {
        NanoVGColor fromNVG = color(from.copy().alpha((int) (globalAlpha() * from.alpha())));
        NanoVGColor toNVG = color(to.copy().alpha((int) (globalAlpha() * to.alpha())));

        NanoVGPaint paint = rawContext.linearGradient(
                startX,
                startY,
                endX,
                endY,
                fromNVG,
                toNVG
        );
        fromNVG.close();
        toNVG.close();
        return paint;
    }

    public void strokeWidth(float width) {
        S_strokeWidth = width;
        rawContext.strokeWidth(width);
    }

    public void strokeColor(Color color) {
        color = color.copy().alpha((int) (globalAlpha() * color.alpha()));
        S_strokeColor = Color.rgba(color.integer());
        try (NanoVGColor vgColor = color(color)) {
            rawContext.strokeColor(vgColor);
        }
    }

    public void fillColor(Color color) {
        color = color.copy().alpha((int) (globalAlpha() * color.alpha()));
        S_fillColor = Color.rgba(color.integer());
        try (NanoVGColor vgColor = color(color)) {
            rawContext.fillColor(vgColor);
        }
    }


    public void drawColor(boolean fill, Color color) {
        if (fill) {
            fillColor(color);
        } else {
            strokeColor(color);
        }
    }

    public void fontSize(float size) {
        rawContext.fontSize(size);
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