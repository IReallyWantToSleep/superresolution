#define NANOVG_GL3_IMPLEMENTATION
#include "nvg/all.h"

void NanoVGContext::BeginFrame(float windowWidth, float windowHeight, float devicePixelRatio)
{
    nvgBeginFrame(ctx, windowWidth, windowHeight, devicePixelRatio);
}

void NanoVGContext::CancelFrame()
{
    nvgCancelFrame(ctx);
}

void NanoVGContext::EndFrame()
{
    nvgEndFrame(ctx);
}

void NanoVGContext::GlobalCompositeOperation(int op)
{
    nvgGlobalCompositeOperation(ctx, op);
}

void NanoVGContext::GlobalCompositeBlendFunc(int sfactor, int dfactor)
{
    nvgGlobalCompositeBlendFunc(ctx, sfactor, dfactor);
}

void NanoVGContext::GlobalCompositeBlendFuncSeparate(int srcRGB, int dstRGB,
                                                     int srcAlpha, int dstAlpha)
{
    nvgGlobalCompositeBlendFuncSeparate(ctx, srcRGB, dstRGB, srcAlpha, dstAlpha);
}

void NanoVGContext::Save()
{
    nvgSave(ctx);
}

void NanoVGContext::Restore()
{
    nvgRestore(ctx);
}

void NanoVGContext::Reset()
{
    nvgReset(ctx);
}

void NanoVGContext::ShapeAntiAlias(int enabled)
{
    nvgShapeAntiAlias(ctx, enabled);
}

void NanoVGContext::StrokeColor(NanoVGColor color)
{
    nvgStrokeColor(ctx, color.color);
}

NanoVGColor NanoVGContext::ColorRGBA(int r, int g, int b, int a)
{
    NanoVGColor color;
    color.color = nvgRGBA(r, g, b, a);
    return color;
}
NanoVGColor NanoVGContext::ColorRGBAf(float r, float g, float b, float a)
{
    NanoVGColor color;
    color.color = nvgRGBAf(r, g, b, a);
    return color;
}

void NanoVGContext::StrokePaint(NanoVGPaint paint)
{
    nvgStrokePaint(ctx, paint.paint);
}

void NanoVGContext::FillColor(NanoVGColor color)
{
    nvgFillColor(ctx, color.color);
}

void NanoVGContext::FillPaint(NanoVGPaint paint)
{
    nvgFillPaint(ctx, paint.paint);
}

void NanoVGContext::MiterLimit(float limit)
{
    nvgMiterLimit(ctx, limit);
}

void NanoVGContext::StrokeWidth(float size)
{
    nvgStrokeWidth(ctx, size);
}

void NanoVGContext::LineCap(int cap)
{
    nvgLineCap(ctx, cap);
}

void NanoVGContext::LineJoin(int join)
{
    nvgLineJoin(ctx, join);
}

void NanoVGContext::GlobalAlpha(float alpha)
{
    nvgGlobalAlpha(ctx, alpha);
}

void NanoVGContext::ResetTransform()
{
    nvgResetTransform(ctx);
}

void NanoVGContext::Transform(float a, float b, float c, float d, float e, float f)
{
    nvgTransform(ctx, a, b, c, d, e, f);
}

void NanoVGContext::Translate(float x, float y)
{
    nvgTranslate(ctx, x, y);
}

void NanoVGContext::Rotate(float angle)
{
    nvgRotate(ctx, angle);
}

void NanoVGContext::SkewX(float angle)
{
    nvgSkewX(ctx, angle);
}

void NanoVGContext::SkewY(float angle)
{
    nvgSkewY(ctx, angle);
}

void NanoVGContext::Scale(float x, float y)
{
    nvgScale(ctx, x, y);
}

std::array<float, 6> NanoVGContext::CurrentTransform()
{
    std::array<float, 6> xform;
    nvgCurrentTransform(ctx, xform.data());
    return xform;
}

int NanoVGContext::CreateImageFromHandle(int textureId, int w, int h, int imageFlags)
{
    return nvglCreateImageFromHandleGL3(ctx, textureId, w, h, imageFlags);
}

void NanoVGContext::DeleteImage(int image)
{
    nvgDeleteImage(ctx, image);
}

NanoVGPaint NanoVGContext::LinearGradient(float sx, float sy, float ex, float ey,
                                          NanoVGColor icol, NanoVGColor ocol)
{
    NanoVGPaint paint;
    paint.paint = nvgLinearGradient(ctx, sx, sy, ex, ey, icol.color, ocol.color);
    return paint;
}

NanoVGPaint NanoVGContext::BoxGradient(float x, float y, float w, float h,
                                       float r, float f, NanoVGColor icol, NanoVGColor ocol)
{
    NanoVGPaint paint;
    paint.paint = nvgBoxGradient(ctx, x, y, w, h, r, f, icol.color, ocol.color);
    return paint;
}

NanoVGPaint NanoVGContext::RadialGradient(float cx, float cy, float inr, float outr,
                                          NanoVGColor icol, NanoVGColor ocol)
{
    NanoVGPaint paint;
    paint.paint = nvgRadialGradient(ctx, cx, cy, inr, outr, icol.color, ocol.color);
    return paint;
}

NanoVGPaint NanoVGContext::ImagePattern(float ox, float oy, float ex, float ey,
                                        float angle, int image, float alpha)
{
    NanoVGPaint paint;
    paint.paint = nvgImagePattern(ctx, ox, oy, ex, ey, angle, image, alpha);
    return paint;
}

void NanoVGContext::Scissor(float x, float y, float w, float h)
{
    nvgScissor(ctx, x, y, w, h);
}

void NanoVGContext::IntersectScissor(float x, float y, float w, float h)
{
    nvgIntersectScissor(ctx, x, y, w, h);
}

void NanoVGContext::ResetScissor()
{
    nvgResetScissor(ctx);
}

void NanoVGContext::BeginPath()
{
    nvgBeginPath(ctx);
}

void NanoVGContext::MoveTo(float x, float y)
{
    nvgMoveTo(ctx, x, y);
}

void NanoVGContext::LineTo(float x, float y)
{
    nvgLineTo(ctx, x, y);
}

void NanoVGContext::BezierTo(float c1x, float c1y, float c2x, float c2y, float x, float y)
{
    nvgBezierTo(ctx, c1x, c1y, c2x, c2y, x, y);
}

void NanoVGContext::QuadTo(float cx, float cy, float x, float y)
{
    nvgQuadTo(ctx, cx, cy, x, y);
}

void NanoVGContext::ArcTo(float x1, float y1, float x2, float y2, float radius)
{
    nvgArcTo(ctx, x1, y1, x2, y2, radius);
}

void NanoVGContext::ClosePath()
{
    nvgClosePath(ctx);
}

void NanoVGContext::PathWinding(int dir)
{
    nvgPathWinding(ctx, dir);
}

void NanoVGContext::Arc(float cx, float cy, float r, float a0, float a1, int dir)
{
    nvgArc(ctx, cx, cy, r, a0, a1, dir);
}

void NanoVGContext::Rect(float x, float y, float w, float h)
{
    nvgRect(ctx, x, y, w, h);
}

void NanoVGContext::RoundedRect(float x, float y, float w, float h, float r)
{
    nvgRoundedRect(ctx, x, y, w, h, r);
}

void NanoVGContext::RoundedRectVarying(float x, float y, float w, float h,
                                       float radTopLeft, float radTopRight,
                                       float radBottomRight, float radBottomLeft)
{
    nvgRoundedRectVarying(ctx, x, y, w, h, radTopLeft, radTopRight, radBottomRight, radBottomLeft);
}

void NanoVGContext::Ellipse(float cx, float cy, float rx, float ry)
{
    nvgEllipse(ctx, cx, cy, rx, ry);
}

void NanoVGContext::Circle(float cx, float cy, float r)
{
    nvgCircle(ctx, cx, cy, r);
}

void NanoVGContext::Fill()
{
    nvgFill(ctx);
}

void NanoVGContext::Stroke()
{
    nvgStroke(ctx);
}

int NanoVGContext::CreateFont(const std::string &name, const std::string &filename)
{
    return nvgCreateFont(ctx, name.c_str(), filename.c_str());
}

int NanoVGContext::CreateFontAtIndex(const std::string &name, const std::string &filename, const int fontIndex)
{
    return nvgCreateFontAtIndex(ctx, name.c_str(), filename.c_str(), fontIndex);
}

int NanoVGContext::CreateFontMem(const std::string &name, unsigned char *data, int ndata, int freeData)
{
    return nvgCreateFontMem(ctx, name.c_str(), data, ndata, freeData);
}

int NanoVGContext::CreateFontMemAtIndex(const std::string &name, unsigned char *data,
                                        int ndata, int freeData, const int fontIndex)
{
    return nvgCreateFontMemAtIndex(ctx, name.c_str(), data, ndata, freeData, fontIndex);
}

int NanoVGContext::FindFont(const std::string &name)
{
    return nvgFindFont(ctx, name.c_str());
}

int NanoVGContext::AddFallbackFontId(int baseFont, int fallbackFont)
{
    return nvgAddFallbackFontId(ctx, baseFont, fallbackFont);
}

int NanoVGContext::AddFallbackFont(const std::string &baseFont, const std::string &fallbackFont)
{
    return nvgAddFallbackFont(ctx, baseFont.c_str(), fallbackFont.c_str());
}

void NanoVGContext::ResetFallbackFontsId(int baseFont)
{
    nvgResetFallbackFontsId(ctx, baseFont);
}

void NanoVGContext::ResetFallbackFonts(const std::string &baseFont)
{
    nvgResetFallbackFonts(ctx, baseFont.c_str());
}

void NanoVGContext::FontSize(float size)
{
    nvgFontSize(ctx, size);
}

void NanoVGContext::FontBlur(float blur)
{
    nvgFontBlur(ctx, blur);
}

void NanoVGContext::TextLetterSpacing(float spacing)
{
    nvgTextLetterSpacing(ctx, spacing);
}

void NanoVGContext::TextLineHeight(float lineHeight)
{
    nvgTextLineHeight(ctx, lineHeight);
}

void NanoVGContext::TextAlign(int align)
{
    nvgTextAlign(ctx, align);
}

void NanoVGContext::FontFaceId(int font)
{
    nvgFontFaceId(ctx, font);
}

void NanoVGContext::FontFace(const std::string &font)
{
    nvgFontFace(ctx, font.c_str());
}

float NanoVGContext::Text(float x, float y, const std::string &string, const std::string *end)
{
    return nvgText(ctx, x, y, string.c_str(), end ? end->c_str() : nullptr);
}

void NanoVGContext::TextBox(float x, float y, float breakRowWidth, const std::string &string, const std::string *end)
{
    nvgTextBox(ctx, x, y, breakRowWidth, string.c_str(), end ? end->c_str() : nullptr);
}

TextBoundsResult NanoVGContext::TextBounds(float x, float y, const std::string &string, const std::string *end)
{
    TextBoundsResult result;
    result.advance = nvgTextBounds(ctx, x, y, string.c_str(), end ? end->c_str() : nullptr, result.bounds.data());
    return result;
}

std::array<float, 4> NanoVGContext::TextBoxBounds(float x, float y, float breakRowWidth,
                                                  const std::string &string, const std::string *end)
{
    std::array<float, 4> bounds;
    nvgTextBoxBounds(ctx, x, y, breakRowWidth, string.c_str(), end ? end->c_str() : nullptr, bounds.data());
    return bounds;
}

std::vector<NVGglyphPosition> NanoVGContext::TextGlyphPositions(float x, float y, const std::string &string, const std::string *end)
{
    std::vector<NVGglyphPosition> positions(string.length() + 1);
    int count = nvgTextGlyphPositions(ctx, x, y, string.c_str(), end ? end->c_str() : nullptr,
                                      positions.data(), positions.size());
    positions.resize(count);
    return positions;
}

TextMetricsResult NanoVGContext::TextMetrics()
{
    TextMetricsResult result;
    nvgTextMetrics(ctx, &result.ascender, &result.descender, &result.lineHeight);
    return result;
}

std::vector<NVGtextRow> NanoVGContext::TextBreakLines(const std::string &string, const std::string *end, float breakRowWidth)
{
    return nvgTextBreakLines(ctx, string.c_str(), end ? end->c_str() : nullptr, breakRowWidth);
}

void NanoVGContext::RoundedRectComplex(float x, float y, float w, float h, float tl, float tr, float bl, float br)
{
    nvgRoundedRectComplex(ctx, x, y, w, h, tl, tr, br, bl);
}

void NanoVGContext::RoundedRectEllipse(float x, float y, float w, float h, float rw, float rh)
{
    nvgRoundedRectEllipse(ctx, x, y, w, h, rw, rh);
}