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

package io.homo.superresolution.thirdparty.nanovg;

import java.nio.ByteBuffer;
import java.util.List;

public class NanoVGContext implements AutoCloseable {
    public static final int NVG_ANTIALIAS = 1;
    public static final int NVG_STENCIL_STROKES = 2;

    private static final int DEFAULT_FLAGS = 0;
    public static final NanoVGBackendMode DEFAULT_BACKEND_MODE = NanoVGBackendMode.GL_LEGACY;
    private long nativeHandle;

    public NanoVGContext(int flags) {
        this(flags, DEFAULT_BACKEND_MODE);
    }

    public NanoVGContext(int flags, NanoVGBackendMode backendMode) {
        NanoVGBackendMode resolvedMode = backendMode == null ? DEFAULT_BACKEND_MODE : backendMode;
        this.nativeHandle = nCreateContextEx(flags, resolvedMode.getNativeValue());
        if (this.nativeHandle == 0) {
            throw new RuntimeException("Failed to create NanoVG context");
        }
    }

    public NanoVGContext(NanoVGBackendMode backendMode) {
        this(DEFAULT_FLAGS, backendMode);
    }

    public NanoVGContext() {
        this(DEFAULT_FLAGS);
    }

    private static native long createContext(int flags);

    private static native long nCreateContextEx(int flags, int backendMode);

    private static native void nDeleteContext(long ctx);

    private static native void nBeginFrame(long ctx, float windowWidth, float windowHeight, float devicePixelRatio);

    private static native void nCancelFrame(long ctx);

    private static native void nEndFrame(long ctx);

    private static native void nGlobalCompositeOperation(long ctx, int op);

    private static native void nGlobalCompositeBlendFunc(long ctx, int sfactor, int dfactor);

    private static native void nGlobalCompositeBlendFuncSeparate(long ctx, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    private static native void nSave(long ctx);

    private static native void nRestore(long ctx);

    private static native void nReset(long ctx);

    private static native void nShapeAntiAlias(long ctx, int enabled);

    private static native void nStrokeColor(long ctx, long colorHandle);

    private static native long nColorRGBA(long ctx, int r, int g, int b, int a);

    private static native long nColorRGBAf(long ctx, float r, float g, float b, float a);

    private static native void nStrokePaint(long ctx, long paintHandle);

    private static native void nFillColor(long ctx, long colorHandle);

    private static native void nFillPaint(long ctx, long paintHandle);

    private static native void nMiterLimit(long ctx, float limit);

    private static native void nStrokeWidth(long ctx, float size);

    private static native void nLineCap(long ctx, int cap);

    private static native void nLineJoin(long ctx, int join);

    private static native void nGlobalAlpha(long ctx, float alpha);

    private static native void nResetTransform(long ctx);

    private static native void nTransform(long ctx, float a, float b, float c, float d, float e, float f);

    private static native void nTranslate(long ctx, float x, float y);

    private static native void nRotate(long ctx, float angle);

    private static native void nSkewX(long ctx, float angle);

    private static native void nSkewY(long ctx, float angle);

    private static native void nScale(long ctx, float x, float y);

    private static native float[] nCurrentTransform(long ctx);

    private static native int nCreateImageFromHandle(long ctx, int textureId, int w, int h, int imageFlags);

    private static native void nDeleteImage(long ctx, int image);

    private static native long nLinearGradient(long ctx, float sx, float sy, float ex, float ey, long icolHandle, long ocolHandle);

    private static native long nBoxGradient(long ctx, float x, float y, float w, float h, float r, float f, long icolHandle, long ocolHandle);

    private static native long nRadialGradient(long ctx, float cx, float cy, float inr, float outr, long icolHandle, long ocolHandle);

    private static native long nImagePattern(long ctx, float ox, float oy, float ex, float ey, float angle, int image, float alpha);

    private static native void nScissor(long ctx, float x, float y, float w, float h);

    private static native void nIntersectScissor(long ctx, float x, float y, float w, float h);

    private static native void nResetScissor(long ctx);

    private static native void nBeginPath(long ctx);

    private static native void nMoveTo(long ctx, float x, float y);

    private static native void nLineTo(long ctx, float x, float y);

    private static native void nBezierTo(long ctx, float c1x, float c1y, float c2x, float c2y, float x, float y);

    private static native void nQuadTo(long ctx, float cx, float cy, float x, float y);

    private static native void nArcTo(long ctx, float x1, float y1, float x2, float y2, float radius);

    private static native void nClosePath(long ctx);

    private static native void nPathWinding(long ctx, int dir);

    private static native void nArc(long ctx, float cx, float cy, float r, float a0, float a1, int dir);

    private static native void nRect(long ctx, float x, float y, float w, float h);

    private static native void nRoundedRect(long ctx, float x, float y, float w, float h, float r);

    private static native void nRoundedRectVarying(long ctx, float x, float y, float w, float h, float radTopLeft, float radTopRight, float radBottomRight, float radBottomLeft);

    private static native void nRoundedRectComplex(long ctx, float x, float y, float w, float h, float tl, float tr, float bl, float br);

    private static native void nRoundedRectEllipse(long ctx, float x, float y, float w, float h, float rw, float rh);

    private static native void nEllipse(long ctx, float cx, float cy, float rx, float ry);

    private static native void nCircle(long ctx, float cx, float cy, float r);

    private static native void nFill(long ctx);

    private static native void nStroke(long ctx);

    private static native int nCreateFont(long ctx, String name, String filename);

    private static native int nCreateFontAtIndex(long ctx, String name, String filename, int fontIndex);

    private static native int nCreateFontMem(long ctx, String name, ByteBuffer data, int freeData);

    private static native int nCreateFontMemAtIndex(long ctx, String name, ByteBuffer data, int freeData, int fontIndex);

    private static native int nFindFont(long ctx, String name);

    private static native int nAddFallbackFontId(long ctx, int baseFont, int fallbackFont);

    private static native int nAddFallbackFont(long ctx, String baseFont, String fallbackFont);

    private static native void nResetFallbackFontsId(long ctx, int baseFont);

    private static native void nResetFallbackFonts(long ctx, String baseFont);

    private static native void nFontSize(long ctx, float size);

    private static native void nFontBlur(long ctx, float blur);

    private static native void nTextLetterSpacing(long ctx, float spacing);

    private static native void nTextLineHeight(long ctx, float lineHeight);

    private static native void nTextAlign(long ctx, int align);

    private static native void nFontFaceId(long ctx, int font);
    private static native void nFontSetVariationAxis(long ctx,int font, String axisTag, float value);
    private static native String[] nFontGetVariationAxis(long ctx,int font);

    private static native void nFontFace(long ctx, String font);

    private static native float nText(long ctx, float x, float y, String string);

    private static native void nTextBox(long ctx, float x, float y, float breakRowWidth, String string);

    private static native TextBoundsResult nTextBounds(long ctx, float x, float y, String string);

    private static native float[] nTextBoxBounds(long ctx, float x, float y, float breakRowWidth, String string);

    private static native NVGglyphPosition[] nTextGlyphPositions(long ctx, float x, float y, String string);

    private static native TextMetricsResult nTextMetrics(long ctx);

    private static native NVGtextRow[] nTextBreakLines(long ctx, String string, float breakRowWidth);

    private static native void nLineStyle(long ctx, int lineStyle);

    public long getNativeHandle() {
        return nativeHandle;
    }

    public void beginFrame(float windowWidth, float windowHeight, float devicePixelRatio) {
        nBeginFrame(nativeHandle, windowWidth, windowHeight, devicePixelRatio);
    }

    public void cancelFrame() {
        nCancelFrame(nativeHandle);
    }

    public void endFrame() {
        nEndFrame(nativeHandle);
    }

    public void globalCompositeOperation(int op) {
        nGlobalCompositeOperation(nativeHandle, op);
    }

    public void globalCompositeBlendFunc(int sfactor, int dfactor) {
        nGlobalCompositeBlendFunc(nativeHandle, sfactor, dfactor);
    }

    public void globalCompositeBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        nGlobalCompositeBlendFuncSeparate(nativeHandle, srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    public void save() {
        nSave(nativeHandle);
    }

    public void restore() {
        nRestore(nativeHandle);
    }

    public void reset() {
        nReset(nativeHandle);
    }

    public void shapeAntiAlias(int enabled) {
        nShapeAntiAlias(nativeHandle, enabled);
    }

    public void strokeColor(NanoVGColor color) {
        nStrokeColor(nativeHandle, color.getNativeHandle());
    }

    public NanoVGColor colorRGBA(int r, int g, int b, int a) {
        long colorHandle = nColorRGBA(nativeHandle, r, g, b, a);
        return new NanoVGColor(colorHandle);
    }

    public NanoVGColor colorRGBAf(float r, float g, float b, float a) {
        long colorHandle = nColorRGBAf(nativeHandle, r, g, b, a);
        return new NanoVGColor(colorHandle);
    }

    public void strokePaint(NanoVGPaint paint) {
        nStrokePaint(nativeHandle, paint.getNativeHandle());
    }

    public void fillColor(NanoVGColor color) {
        nFillColor(nativeHandle, color.getNativeHandle());
    }

    public void fillPaint(NanoVGPaint paint) {
        nFillPaint(nativeHandle, paint.getNativeHandle());
    }

    public void miterLimit(float limit) {
        nMiterLimit(nativeHandle, limit);
    }

    public void strokeWidth(float size) {
        nStrokeWidth(nativeHandle, size);
    }

    public void lineCap(int cap) {
        nLineCap(nativeHandle, cap);
    }

    public void lineJoin(int join) {
        nLineJoin(nativeHandle, join);
    }

    public void globalAlpha(float alpha) {
        nGlobalAlpha(nativeHandle, alpha);
    }

    public void resetTransform() {
        nResetTransform(nativeHandle);
    }

    public void transform(float a, float b, float c, float d, float e, float f) {
        nTransform(nativeHandle, a, b, c, d, e, f);
    }

    public void translate(float x, float y) {
        nTranslate(nativeHandle, x, y);
    }

    public void rotate(float angle) {
        nRotate(nativeHandle, angle);
    }

    public void skewX(float angle) {
        nSkewX(nativeHandle, angle);
    }

    public void skewY(float angle) {
        nSkewY(nativeHandle, angle);
    }

    public void scale(float x, float y) {
        nScale(nativeHandle, x, y);
    }

    public float[] currentTransform() {
        return nCurrentTransform(nativeHandle);
    }

    public int createImageFromHandle(int textureId, int w, int h, int imageFlags) {
        return nCreateImageFromHandle(nativeHandle, textureId, w, h, imageFlags);
    }

    public void deleteImage(int image) {
        nDeleteImage(nativeHandle, image);
    }

    public NanoVGPaint linearGradient(float sx, float sy, float ex, float ey, NanoVGColor icol, NanoVGColor ocol) {
        long paintHandle = nLinearGradient(nativeHandle, sx, sy, ex, ey, icol.getNativeHandle(), ocol.getNativeHandle());
        return new NanoVGPaint(paintHandle);
    }

    public NanoVGPaint boxGradient(float x, float y, float w, float h, float r, float f, NanoVGColor icol, NanoVGColor ocol) {
        long paintHandle = nBoxGradient(nativeHandle, x, y, w, h, r, f, icol.getNativeHandle(), ocol.getNativeHandle());
        return new NanoVGPaint(paintHandle);
    }

    public NanoVGPaint radialGradient(float cx, float cy, float inr, float outr, NanoVGColor icol, NanoVGColor ocol) {
        long paintHandle = nRadialGradient(nativeHandle, cx, cy, inr, outr, icol.getNativeHandle(), ocol.getNativeHandle());
        return new NanoVGPaint(paintHandle);
    }

    public NanoVGPaint imagePattern(float ox, float oy, float ex, float ey, float angle, int image, float alpha) {
        long paintHandle = nImagePattern(nativeHandle, ox, oy, ex, ey, angle, image, alpha);
        return new NanoVGPaint(paintHandle);
    }

    public void scissor(float x, float y, float w, float h) {
        nScissor(nativeHandle, x, y, w, h);
    }

    public void intersectScissor(float x, float y, float w, float h) {
        nIntersectScissor(nativeHandle, x, y, w, h);
    }

    public void resetScissor() {
        nResetScissor(nativeHandle);
    }

    public void beginPath() {
        nBeginPath(nativeHandle);
    }

    public void moveTo(float x, float y) {
        nMoveTo(nativeHandle, x, y);
    }

    public void lineTo(float x, float y) {
        nLineTo(nativeHandle, x, y);
    }

    public void bezierTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        nBezierTo(nativeHandle, c1x, c1y, c2x, c2y, x, y);
    }

    public void quadTo(float cx, float cy, float x, float y) {
        nQuadTo(nativeHandle, cx, cy, x, y);
    }

    public void arcTo(float x1, float y1, float x2, float y2, float radius) {
        nArcTo(nativeHandle, x1, y1, x2, y2, radius);
    }

    public void closePath() {
        nClosePath(nativeHandle);
    }

    public void pathWinding(int dir) {
        nPathWinding(nativeHandle, dir);
    }

    public void arc(float cx, float cy, float r, float a0, float a1, int dir) {
        nArc(nativeHandle, cx, cy, r, a0, a1, dir);
    }

    public void rect(float x, float y, float w, float h) {
        nRect(nativeHandle, x, y, w, h);
    }

    public void roundedRect(float x, float y, float w, float h, float r) {
        nRoundedRect(nativeHandle, x, y, w, h, r);
    }

    public void roundedRectVarying(float x, float y, float w, float h, float radTopLeft, float radTopRight, float radBottomRight, float radBottomLeft) {
        nRoundedRectVarying(nativeHandle, x, y, w, h, radTopLeft, radTopRight, radBottomRight, radBottomLeft);
    }

    public void roundedRectComplex(float x, float y, float w, float h, float tl, float tr, float bl, float br) {
        nRoundedRectComplex(nativeHandle, x, y, w, h, tl, tr, bl, br);
    }

    public void roundedRectEllipse(float x, float y, float w, float h, float rw, float rh) {
        nRoundedRectEllipse(nativeHandle, x, y, w, h, rw, rh);
    }

    public void ellipse(float cx, float cy, float rx, float ry) {
        nEllipse(nativeHandle, cx, cy, rx, ry);
    }

    public void circle(float cx, float cy, float r) {
        nCircle(nativeHandle, cx, cy, r);
    }

    public void fill() {
        nFill(nativeHandle);
    }

    public void stroke() {
        nStroke(nativeHandle);
    }

    public int createFont(String name, String filename) {
        return nCreateFont(nativeHandle, name, filename);
    }

    public int createFontAtIndex(String name, String filename, int fontIndex) {
        return nCreateFontAtIndex(nativeHandle, name, filename, fontIndex);
    }

    public int createFontMem(String name, ByteBuffer data, int freeData) {
        return nCreateFontMem(nativeHandle, name, data, freeData);
    }

    public int createFontMemAtIndex(String name, ByteBuffer data, int freeData, int fontIndex) {
        return nCreateFontMemAtIndex(nativeHandle, name, data, freeData, fontIndex);
    }

    public int findFont(String name) {
        return nFindFont(nativeHandle, name);
    }

    public int addFallbackFontId(int baseFont, int fallbackFont) {
        return nAddFallbackFontId(nativeHandle, baseFont, fallbackFont);
    }

    public int addFallbackFont(String baseFont, String fallbackFont) {
        return nAddFallbackFont(nativeHandle, baseFont, fallbackFont);
    }

    public void resetFallbackFontsId(int baseFont) {
        nResetFallbackFontsId(nativeHandle, baseFont);
    }

    public void resetFallbackFonts(String baseFont) {
        nResetFallbackFonts(nativeHandle, baseFont);
    }

    public void fontSize(float size) {
        nFontSize(nativeHandle, size);
    }

    public void fontBlur(float blur) {
        nFontBlur(nativeHandle, blur);
    }

    public void textLetterSpacing(float spacing) {
        nTextLetterSpacing(nativeHandle, spacing);
    }

    public void textLineHeight(float lineHeight) {
        nTextLineHeight(nativeHandle, lineHeight);
    }

    public void textAlign(int align) {
        nTextAlign(nativeHandle, align);
    }

    public void fontFaceId(int font) {
        nFontFaceId(nativeHandle, font);
    }

    public void fontFace(String font) {
        nFontFace(nativeHandle, font);
    }

    public float text(float x, float y, String string) {
        return nText(nativeHandle, x, y, string);
    }

    public void textBox(float x, float y, float breakRowWidth, String string) {
        nTextBox(nativeHandle, x, y, breakRowWidth, string);
    }

    public void lineStyle(int lineStyle) {
        nLineStyle(nativeHandle, lineStyle);
    }

    public TextBoundsResult textBounds(float x, float y, String string) {
        return nTextBounds(nativeHandle, x, y, string);
    }

    public float[] textBoxBounds(float x, float y, float breakRowWidth, String string) {
        return nTextBoxBounds(nativeHandle, x, y, breakRowWidth, string);
    }

    public List<NVGglyphPosition> textGlyphPositions(float x, float y, String string) {
        return List.of(nTextGlyphPositions(nativeHandle, x, y, string));
    }


    public TextMetricsResult textMetrics() {
        return nTextMetrics(nativeHandle);
    }

    public List<NVGtextRow> textBreakLines(String string, float breakRowWidth) {
        return List.of(nTextBreakLines(nativeHandle, string, breakRowWidth));
    }

    public void fontSetVariationAxis(int font, String axisTag, float value){
        nFontSetVariationAxis(nativeHandle,font,axisTag,value);
    }
    public List<String> fontGetVariationAxis(int font){
        return List.of(nFontGetVariationAxis(nativeHandle,font));
    }


    public void delete() {
        if (nativeHandle != 0) {
            nDeleteContext(nativeHandle);
            nativeHandle = 0;
        }
    }

    @Override
    public void close() {
        delete();
    }
}
