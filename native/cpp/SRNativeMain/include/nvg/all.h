#include "nanovg.h"
#include "nanovg_gl.h"
#include <vector>
#include <string>
#include <array>
#include <stdexcept>

struct NanoVGPaint
{
    NVGpaint paint;
};
typedef struct NanoVGPaint NanoVGPaint;

struct NanoVGColor
{
    NVGcolor color;
};
typedef struct NanoVGColor NanoVGColor;

struct TextMetricsResult
{
    float ascender;
    float descender;
    float lineHeight;
};

struct TextBoundsResult
{
    float advance;
    std::array<float, 4> bounds;
};

class NanoVGContext
{
private:
    NVGcontext *ctx;

public:
    NanoVGContext(int flags, GlFunctionTable glFuncTable)
    {
        ctx = nvgCreateGL3(flags, glFuncTable);
        if (!ctx)
        {
            throw std::runtime_error("Failed to create NanoVG context");
        }
    }
    ~NanoVGContext()
    {
        nvgDeleteGL3(ctx);
    }
    void *GetContext()
    {
        return ctx;
    }

    void BeginFrame(float windowWidth, float windowHeight, float devicePixelRatio);
    void CancelFrame();
    void EndFrame();
    void GlobalCompositeOperation(int op);
    void GlobalCompositeBlendFunc(int sfactor, int dfactor);
    void GlobalCompositeBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);
    void Save();
    void Restore();
    void Reset();
    void ShapeAntiAlias(int enabled);
    void StrokeColor(NanoVGColor color);
    NanoVGColor ColorRGBA(int r, int g, int b, int a);
    NanoVGColor ColorRGBAf(float r, float g, float b, float a);
    void StrokePaint(NanoVGPaint paint);
    void FillColor(NanoVGColor color);
    void FillPaint(NanoVGPaint paint);
    void MiterLimit(float limit);
    void StrokeWidth(float size);
    void LineCap(int cap);
    void LineJoin(int join);
    void GlobalAlpha(float alpha);
    void ResetTransform();
    void Transform(float a, float b, float c, float d, float e, float f);
    void Translate(float x, float y);
    void Rotate(float angle);
    void SkewX(float angle);
    void SkewY(float angle);
    void Scale(float x, float y);
    std::array<float, 6> CurrentTransform();
    int CreateImageFromHandle(int textureId, int w, int h, int imageFlags);
    void DeleteImage(int image);
    NanoVGPaint LinearGradient(float sx, float sy, float ex, float ey, NanoVGColor icol, NanoVGColor ocol);
    NanoVGPaint BoxGradient(float x, float y, float w, float h, float r, float f, NanoVGColor icol, NanoVGColor ocol);
    NanoVGPaint RadialGradient(float cx, float cy, float inr, float outr, NanoVGColor icol, NanoVGColor ocol);
    NanoVGPaint ImagePattern(float ox, float oy, float ex, float ey, float angle, int image, float alpha);
    void Scissor(float x, float y, float w, float h);
    void IntersectScissor(float x, float y, float w, float h);
    void ResetScissor();
    void BeginPath();
    void MoveTo(float x, float y);
    void LineTo(float x, float y);
    void BezierTo(float c1x, float c1y, float c2x, float c2y, float x, float y);
    void QuadTo(float cx, float cy, float x, float y);
    void ArcTo(float x1, float y1, float x2, float y2, float radius);
    void ClosePath();
    void PathWinding(int dir);
    void Arc(float cx, float cy, float r, float a0, float a1, int dir);
    void Rect(float x, float y, float w, float h);
    void RoundedRect(float x, float y, float w, float h, float r);
    void RoundedRectVarying(float x, float y, float w, float h, float radTopLeft, float radTopRight, float radBottomRight, float radBottomLeft);
    void Ellipse(float cx, float cy, float rx, float ry);
    void Circle(float cx, float cy, float r);
    void Fill();
    void Stroke();
    int CreateFont(const std::string &name, const std::string &filename);
    int CreateFontAtIndex(const std::string &name, const std::string &filename, const int fontIndex);
    int CreateFontMem(const std::string &name, unsigned char *data, int ndata, int freeData);
    int CreateFontMemAtIndex(const std::string &name, unsigned char *data, int ndata, int freeData, const int fontIndex);
    int FindFont(const std::string &name);
    int AddFallbackFontId(int baseFont, int fallbackFont);
    int AddFallbackFont(const std::string &baseFont, const std::string &fallbackFont);
    void ResetFallbackFontsId(int baseFont);
    void ResetFallbackFonts(const std::string &baseFont);
    void FontSize(float size);
    void FontBlur(float blur);
    void TextLetterSpacing(float spacing);
    void TextLineHeight(float lineHeight);
    void TextAlign(int align);
    void FontFaceId(int font);
    void FontFace(const std::string &font);
    float Text(float x, float y, const std::string &string, const std::string *end = nullptr);
    void TextBox(float x, float y, float breakRowWidth, const std::string &string, const std::string *end = nullptr);
    TextBoundsResult TextBounds(float x, float y, const std::string &string, const std::string *end = nullptr);
    std::array<float, 4> TextBoxBounds(float x, float y, float breakRowWidth, const std::string &string, const std::string *end = nullptr);
    std::vector<NVGglyphPosition> TextGlyphPositions(float x, float y, const std::string &string, const std::string *end = nullptr);
    TextMetricsResult TextMetrics();
    std::vector<NVGtextRow> TextBreakLines(const std::string &string, const std::string *end, float breakRowWidth);
    void RoundedRectComplex(float x, float y, float w, float h, float tl, float tr, float bl, float br);
    void RoundedRectEllipse(float x, float y, float w, float h, float rw, float rh);
    void LineStyle(int lineStyle);
};