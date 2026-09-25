//
// Copyright (c) 2009-2013 Mikko Mononen memon@inside.org
//
// This software is provided 'as-is', without any express or implied
// warranty.  In no event will the authors be held liable for any damages
// arising from the use of this software.
// Permission is granted to anyone to use this software for any purpose,
// including commercial applications, and to alter it and redistribute it
// freely, subject to the following restrictions:
// 1. The origin of this software must not be misrepresented; you must not
//    claim that you wrote the original software. If you use this software
//    in a product, an acknowledgment in the product documentation would be
//    appreciated but is not required.
// 2. Altered source versions must be plainly marked as such, and must not be
//    misrepresented as being the original software.
// 3. This notice may not be removed or altered from any source distribution.
//

#include <cstdlib>
#include <cstring>
#include "nvg/nanovg_rhi.h"

static int rhinvg_maxi(int a, int b) { return a > b ? a : b; }

static SRRHINVGTexture *rhinvg_allocTexture(SRRHINVGContext *gl) {
    SRRHINVGTexture *tex = nullptr;

    for (int i = 0; i < gl->ntextures; i++) {
        if (gl->textures[i].id == 0) {
            tex = &gl->textures[i];
            break;
        }
    }
    if (tex == nullptr) {
        if (gl->ntextures + 1 > gl->ctextures) {
            int ctextures = rhinvg_maxi(gl->ntextures + 1, 4) + gl->ctextures / 2; // 1.5x Overallocate
            auto *textures = static_cast<SRRHINVGTexture *>(realloc(gl->textures, sizeof(SRRHINVGTexture) * ctextures));
            if (textures == nullptr)
                return nullptr;
            gl->textures = textures;
            gl->ctextures = ctextures;
        }
        tex = &gl->textures[gl->ntextures++];
    }

    memset(tex, 0, sizeof(*tex));
    tex->id = ++gl->textureId;

    return tex;
}

static SRRHINVGTexture *rhinvg_findTexture(SRRHINVGContext *gl, int id) {
    for (int i = 0; i < gl->ntextures; i++)
        if (gl->textures[i].id == id)
            return &gl->textures[i];
    return nullptr;
}

static int rhinvg_deleteTexture(SRRHINVGContext *gl, int id) {
    for (int i = 0; i < gl->ntextures; i++) {
        if (gl->textures[i].id == id) {
            memset(&gl->textures[i], 0, sizeof(gl->textures[i]));
            return 1;
        }
    }
    return 0;
}

static int rhinvg_renderCreateTexture(void *uptr, int type, int w, int h, int imageFlags, const unsigned char *data);

static int rhinvg_renderCreate(void *uptr) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    gl->fragSize = sizeof(SRRHINVGFragUniforms);
    return 1;
}

static int rhinvg_renderCreateTexture(void *uptr, int type, int w, int h, int imageFlags, const unsigned char *data) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    SRRHINVGTexture *tex = rhinvg_allocTexture(gl);

    if (tex == nullptr)
        return 0;

    tex->width = w;
    tex->height = h;
    tex->type = type;
    tex->flags = imageFlags;
    tex->tex = static_cast<GLuint>(tex->id);
    if (gl->rhiCallbacks.createTexture) {
        int channels = type == NVG_TEXTURE_RGBA ? 4 : 1;
        int dataSize = data ? (w * h * channels) : 0;
        int ok = gl->rhiCallbacks.createTexture(gl->rhiUserPtr, tex->id, type, w, h, imageFlags, data, dataSize);
        if (!ok) {
            memset(tex, 0, sizeof(*tex));
            return 0;
        }
    }
    return tex->id;
}

static int rhinvg_renderDeleteTexture(void *uptr, int image) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    gl->rhiCallbacks.deleteTexture(gl->rhiUserPtr, image);
    return rhinvg_deleteTexture(gl, image);
}

static int rhinvg_renderUpdateTexture(void *uptr, int image, int x, int y, int w, int h, const unsigned char *data) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    SRRHINVGTexture *tex = rhinvg_findTexture(gl, image);

    if (tex == nullptr)
        return 0;

    if (gl->rhiCallbacks.updateTexture) {
        int channels = tex->type == NVG_TEXTURE_RGBA ? 4 : 1;
        int dataSize = data ? (w * h * channels) : 0;
        if (!data || dataSize <= 0)
            return gl->rhiCallbacks.updateTexture(gl->rhiUserPtr, image, x, y, w, h, data, dataSize);

        if (w <= 0 || h <= 0 || x < 0 || y < 0 || x + w > tex->width || y + h > tex->height)
            return 0;

        auto *packed = static_cast<unsigned char *>(malloc(static_cast<size_t>(dataSize)));
        if (packed == nullptr)
            return 0;

        for (int row = 0; row < h; row++) {
            const unsigned char *src = data + (static_cast<size_t>(y + row) * tex->width + x) * channels;
            unsigned char *dst = packed + static_cast<size_t>(row) * w * channels;
            memcpy(dst, src, static_cast<size_t>(w) * channels);
        }

        int ok = gl->rhiCallbacks.updateTexture(gl->rhiUserPtr, image, x, y, w, h, packed, dataSize);
        free(packed);
        return ok;
    }
    return 1;
}

static int rhinvg_renderGetTextureSize(void *uptr, int image, int *w, int *h) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    SRRHINVGTexture *tex = rhinvg_findTexture(gl, image);
    if (tex == nullptr)
        return 0;
    *w = tex->width;
    *h = tex->height;
    return 1;
}

static void rhinvg_xformToMat3x4(float *m3, float *t) {
    m3[0] = t[0];
    m3[1] = t[1];
    m3[2] = 0.0f;
    m3[3] = 0.0f;
    m3[4] = t[2];
    m3[5] = t[3];
    m3[6] = 0.0f;
    m3[7] = 0.0f;
    m3[8] = t[4];
    m3[9] = t[5];
    m3[10] = 1.0f;
    m3[11] = 0.0f;
}

static NVGcolor rhinvg_premulColor(NVGcolor c) {
    c.r *= c.a;
    c.g *= c.a;
    c.b *= c.a;
    return c;
}

static int rhinvg_convertPaint(SRRHINVGContext *gl, SRRHINVGFragUniforms *frag, NVGpaint *paint,
                               NVGscissor *scissor, float width, float fringe, float strokeThr) {
    SRRHINVGTexture *tex = nullptr;
    float invxform[6];

    memset(frag, 0, sizeof(*frag));

    frag->innerCol = rhinvg_premulColor(paint->innerColor);
    frag->outerCol = rhinvg_premulColor(paint->outerColor);

    if (scissor->extent[0] < -0.5f || scissor->extent[1] < -0.5f) {
        memset(frag->scissorMat, 0, sizeof(frag->scissorMat));
        frag->scissorExt[0] = 1.0f;
        frag->scissorExt[1] = 1.0f;
        frag->scissorScale[0] = 1.0f;
        frag->scissorScale[1] = 1.0f;
    } else {
        nvgTransformInverse(invxform, scissor->xform);
        rhinvg_xformToMat3x4(frag->scissorMat, invxform);
        frag->scissorExt[0] = scissor->extent[0];
        frag->scissorExt[1] = scissor->extent[1];
        frag->scissorScale[0] = sqrtf(scissor->xform[0] * scissor->xform[0] + scissor->xform[2] * scissor->xform[2]) /
                                fringe;
        frag->scissorScale[1] = sqrtf(scissor->xform[1] * scissor->xform[1] + scissor->xform[3] * scissor->xform[3]) /
                                fringe;
    }

    memcpy(frag->extent, paint->extent, sizeof(frag->extent));
    frag->strokeMult = (width * 0.5f + fringe * 0.5f) / fringe;
    frag->strokeThr = strokeThr;

    if (paint->image != 0) {
        tex = rhinvg_findTexture(gl, paint->image);
        if (tex == nullptr)
            return 0;
        if ((tex->flags & NVG_IMAGE_FLIPY) != 0) {
            float m1[6], m2[6];
            nvgTransformTranslate(m1, 0.0f, frag->extent[1] * 0.5f);
            nvgTransformMultiply(m1, paint->xform);
            nvgTransformScale(m2, 1.0f, -1.0f);
            nvgTransformMultiply(m2, m1);
            nvgTransformTranslate(m1, 0.0f, -frag->extent[1] * 0.5f);
            nvgTransformMultiply(m1, m2);
            nvgTransformInverse(invxform, m1);
        } else {
            nvgTransformInverse(invxform, paint->xform);
        }
        frag->type = NSVG_SHADER_FILLIMG;

        if (tex->type == NVG_TEXTURE_RGBA)
            frag->texType = (tex->flags & NVG_IMAGE_PREMULTIPLIED) ? 0 : 1;
        else
            frag->texType = 2;
    } else {
        frag->type = NSVG_SHADER_FILLGRAD;
        frag->radius = paint->radius;
        frag->feather = paint->feather;
        nvgTransformInverse(invxform, paint->xform);
    }

    rhinvg_xformToMat3x4(frag->paintMat, invxform);

    return 1;
}

static SRRHINVGFragUniforms *nvg_fragUniformPtr(SRRHINVGContext *gl, int i);

static void rhinvg_renderViewport(void *uptr, float width, float height, float devicePixelRatio) {
    NVG_NOTUSED(devicePixelRatio);
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    gl->view[0] = width;
    gl->view[1] = height;
    gl->rhiCallbacks.viewport(gl->rhiUserPtr, width, height, devicePixelRatio);
}

static void rhinvg_renderCancel(void *uptr) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    gl->nverts = 0;
    gl->npaths = 0;
    gl->ncalls = 0;
    gl->nuniforms = 0;
}

#define RHINVG_BF_ZERO 0
#define RHINVG_BF_ONE 1
#define RHINVG_BF_SRC_COLOR 0x0300
#define RHINVG_BF_ONE_MINUS_SRC_COLOR 0x0301
#define RHINVG_BF_SRC_ALPHA 0x0302
#define RHINVG_BF_ONE_MINUS_SRC_ALPHA 0x0303
#define RHINVG_BF_DST_ALPHA 0x0304
#define RHINVG_BF_ONE_MINUS_DST_ALPHA 0x0305
#define RHINVG_BF_DST_COLOR 0x0306
#define RHINVG_BF_ONE_MINUS_DST_COLOR 0x0307
#define RHINVG_BF_SRC_ALPHA_SATURATE 0x0308

static GLenum rhinvg_convertBlendFuncFactor(int factor) {
    if (factor == NVG_ZERO)
        return RHINVG_BF_ZERO;
    if (factor == NVG_ONE)
        return RHINVG_BF_ONE;
    if (factor == NVG_SRC_COLOR)
        return RHINVG_BF_SRC_COLOR;
    if (factor == NVG_ONE_MINUS_SRC_COLOR)
        return RHINVG_BF_ONE_MINUS_SRC_COLOR;
    if (factor == NVG_DST_COLOR)
        return RHINVG_BF_DST_COLOR;
    if (factor == NVG_ONE_MINUS_DST_COLOR)
        return RHINVG_BF_ONE_MINUS_DST_COLOR;
    if (factor == NVG_SRC_ALPHA)
        return RHINVG_BF_SRC_ALPHA;
    if (factor == NVG_ONE_MINUS_SRC_ALPHA)
        return RHINVG_BF_ONE_MINUS_SRC_ALPHA;
    if (factor == NVG_DST_ALPHA)
        return RHINVG_BF_DST_ALPHA;
    if (factor == NVG_ONE_MINUS_DST_ALPHA)
        return RHINVG_BF_ONE_MINUS_DST_ALPHA;
    if (factor == NVG_SRC_ALPHA_SATURATE)
        return RHINVG_BF_SRC_ALPHA_SATURATE;
    return 0x0500; //GL_INVALID_ENUM
}

static SRRHINVGblend rhinvg_blendCompositeOperation(NVGcompositeOperationState op) {
    SRRHINVGblend blend;
    blend.srcRGB = rhinvg_convertBlendFuncFactor(op.srcRGB);
    blend.dstRGB = rhinvg_convertBlendFuncFactor(op.dstRGB);
    blend.srcAlpha = rhinvg_convertBlendFuncFactor(op.srcAlpha);
    blend.dstAlpha = rhinvg_convertBlendFuncFactor(op.dstAlpha);
    if (blend.srcRGB == GL_INVALID_ENUM || blend.dstRGB == GL_INVALID_ENUM || blend.srcAlpha == GL_INVALID_ENUM || blend
        .dstAlpha == GL_INVALID_ENUM) {
        blend.srcRGB = GL_ONE;
        blend.dstRGB = GL_ONE_MINUS_SRC_ALPHA;
        blend.srcAlpha = GL_ONE;
        blend.dstAlpha = GL_ONE_MINUS_SRC_ALPHA;
    }
    return blend;
}

static void rhinvg_renderFlush(void *uptr) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);

    if (gl->ncalls > 0 && gl->rhiCallbacks.flush) {
        gl->rhiCallbacks.flush(
            gl->rhiUserPtr,
            gl->view[0],
            gl->view[1],
            static_cast<const void *>(gl->verts),
            gl->nverts,
            static_cast<const void *>(gl->paths),
            gl->npaths,
            static_cast<const void *>(gl->calls),
            gl->ncalls,
            gl->uniforms,
            gl->nuniforms * gl->fragSize,
            gl->fragSize,
            sizeof(SRRHINVGCall));
    }

    gl->nverts = 0;
    gl->npaths = 0;
    gl->ncalls = 0;
    gl->nuniforms = 0;
}

static int rhinvg_maxVertCount(const NVGpath *paths, int npaths) {
    int count = 0;
    for (int i = 0; i < npaths; i++) {
        count += paths[i].nfill;
        count += paths[i].nstroke;
    }
    return count;
}

static SRRHINVGCall *rhinvg_allocCall(SRRHINVGContext *gl) {
    SRRHINVGCall *ret = nullptr;
    if (gl->ncalls + 1 > gl->ccalls) {
        int ccalls = rhinvg_maxi(gl->ncalls + 1, 128) + gl->ccalls / 2; // 1.5x Overallocate
        auto *calls = static_cast<SRRHINVGCall *>(realloc(gl->calls, sizeof(SRRHINVGCall) * ccalls));
        if (calls == nullptr)
            return nullptr;
        gl->calls = calls;
        gl->ccalls = ccalls;
    }
    ret = &gl->calls[gl->ncalls++];
    memset(ret, 0, sizeof(SRRHINVGCall));
    return ret;
}

static int rhinvg_allocPaths(SRRHINVGContext *gl, int n) {
    int ret = 0;
    if (gl->npaths + n > gl->cpaths) {
        int cpaths = rhinvg_maxi(gl->npaths + n, 128) + gl->cpaths / 2; // 1.5x Overallocate
        auto *paths = static_cast<SRRHINVGPath *>(realloc(gl->paths, sizeof(SRRHINVGPath) * cpaths));
        if (paths == nullptr)
            return -1;
        gl->paths = paths;
        gl->cpaths = cpaths;
    }
    ret = gl->npaths;
    gl->npaths += n;
    return ret;
}

static int rhinvg_allocVerts(SRRHINVGContext *gl, int n) {
    int ret = 0;
    if (gl->nverts + n > gl->cverts) {
        int cverts = rhinvg_maxi(gl->nverts + n, 4096) + gl->cverts / 2; // 1.5x Overallocate
        auto *verts = static_cast<NVGvertex *>(realloc(gl->verts, sizeof(NVGvertex) * cverts));
        if (verts == nullptr)
            return -1;
        gl->verts = verts;
        gl->cverts = cverts;
    }
    ret = gl->nverts;
    gl->nverts += n;
    return ret;
}

static int rhinvg_allocFragUniforms(SRRHINVGContext *gl, int n) {
    int ret = 0, structSize = gl->fragSize;
    if (gl->nuniforms + n > gl->cuniforms) {
        int cuniforms = rhinvg_maxi(gl->nuniforms + n, 128) + gl->cuniforms / 2; // 1.5x Overallocate
        auto *uniforms = static_cast<unsigned char *>(realloc(gl->uniforms, structSize * cuniforms));
        if (uniforms == nullptr)
            return -1;
        gl->uniforms = uniforms;
        gl->cuniforms = cuniforms;
    }
    ret = gl->nuniforms * structSize;
    gl->nuniforms += n;
    return ret;
}

static SRRHINVGFragUniforms *nvg_fragUniformPtr(SRRHINVGContext *gl, int i) {
    return reinterpret_cast<SRRHINVGFragUniforms *>(&gl->uniforms[i]);
}

static void glnvg_vset(NVGvertex *vtx, float x, float y, float u, float v) {
    vtx->x = x;
    vtx->y = y;
    vtx->u = u;
    vtx->v = v;
}

static void rhinvg_renderFill(void *uptr, NVGpaint *paint, NVGcompositeOperationState compositeOperation,
                              NVGscissor *scissor, float fringe,
                              const float *bounds, const NVGpath *paths, int npaths) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    SRRHINVGCall *call = rhinvg_allocCall(gl);
    NVGvertex *quad;
    SRRHINVGFragUniforms *frag;
    int maxverts, offset;

    if (call == nullptr)
        return;

    call->type = GLNVG_FILL;
    call->triangleCount = 4;
    call->pathOffset = rhinvg_allocPaths(gl, npaths);
    if (call->pathOffset == -1)
        goto error;
    call->pathCount = npaths;
    call->image = paint->image;
    call->blendFunc = rhinvg_blendCompositeOperation(compositeOperation);

    if (npaths == 1 && paths[0].convex) {
        call->type = GLNVG_CONVEXFILL;
        call->triangleCount = 0; // Bounding box fill quad not needed for convex fill
    }

    // Allocate vertices for all the paths.
    maxverts = rhinvg_maxVertCount(paths, npaths) + call->triangleCount;
    offset = rhinvg_allocVerts(gl, maxverts);
    if (offset == -1)
        goto error;

    for (int i = 0; i < npaths; i++) {
        SRRHINVGPath *copy = &gl->paths[call->pathOffset + i];
        const NVGpath *path = &paths[i];
        memset(copy, 0, sizeof(SRRHINVGPath));
        if (path->nfill > 0) {
            copy->fillOffset = offset;
            copy->fillCount = path->nfill;
            memcpy(&gl->verts[offset], path->fill, sizeof(NVGvertex) * path->nfill);
            offset += path->nfill;
        }
        if (path->nstroke > 0) {
            copy->strokeOffset = offset;
            copy->strokeCount = path->nstroke;
            memcpy(&gl->verts[offset], path->stroke, sizeof(NVGvertex) * path->nstroke);
            offset += path->nstroke;
        }
    }

    // Setup uniforms for draw calls
    if (call->type == GLNVG_FILL) {
        // Quad
        call->triangleOffset = offset;
        quad = &gl->verts[call->triangleOffset];
        glnvg_vset(&quad[0], bounds[2], bounds[3], 0.5f, 1.0f);
        glnvg_vset(&quad[1], bounds[2], bounds[1], 0.5f, 1.0f);
        glnvg_vset(&quad[2], bounds[0], bounds[3], 0.5f, 1.0f);
        glnvg_vset(&quad[3], bounds[0], bounds[1], 0.5f, 1.0f);

        call->uniformOffset = rhinvg_allocFragUniforms(gl, 2);
        if (call->uniformOffset == -1)
            goto error;
        call->uniformCount = 2;
        // Simple shader for stencil
        frag = nvg_fragUniformPtr(gl, call->uniformOffset);
        memset(frag, 0, sizeof(*frag));
        frag->strokeThr = -1.0f;
        frag->type = NSVG_SHADER_SIMPLE;
        // Fill shader
        rhinvg_convertPaint(gl, nvg_fragUniformPtr(gl, call->uniformOffset + gl->fragSize), paint, scissor, fringe,
                            fringe, -1.0f);
    } else {
        call->uniformOffset = rhinvg_allocFragUniforms(gl, 1);
        if (call->uniformOffset == -1)
            goto error;
        call->uniformCount = 1;
        // Fill shader
        rhinvg_convertPaint(gl, nvg_fragUniformPtr(gl, call->uniformOffset), paint, scissor, fringe, fringe, -1.0f);
    }

    return;

error:
    // We get here if call alloc was ok, but something else is not.
    // Roll back the last call to prevent drawing it.
    if (gl->ncalls > 0)
        gl->ncalls--;
}

static void rhinvg_renderStroke(void *uptr, NVGpaint *paint, NVGcompositeOperationState compositeOperation,
                                NVGscissor *scissor, float fringe,
                                float strokeWidth, const NVGpath *paths, int npaths) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    SRRHINVGCall *call = rhinvg_allocCall(gl);
    int i, maxverts, offset;

    if (call == nullptr)
        return;

    call->type = GLNVG_STROKE;
    call->pathOffset = rhinvg_allocPaths(gl, npaths);
    if (call->pathOffset == -1)
        goto error;
    call->pathCount = npaths;
    call->image = paint->image;
    call->blendFunc = rhinvg_blendCompositeOperation(compositeOperation);

    // Allocate vertices for all the paths.
    maxverts = rhinvg_maxVertCount(paths, npaths);
    offset = rhinvg_allocVerts(gl, maxverts);
    if (offset == -1)
        goto error;

    for (i = 0; i < npaths; i++) {
        SRRHINVGPath *copy = &gl->paths[call->pathOffset + i];
        const NVGpath *path = &paths[i];
        memset(copy, 0, sizeof(SRRHINVGPath));
        if (path->nstroke) {
            copy->strokeOffset = offset;
            copy->strokeCount = path->nstroke;
            memcpy(&gl->verts[offset], path->stroke, sizeof(NVGvertex) * path->nstroke);
            offset += path->nstroke;
        }
    }

    if (gl->flags & NVG_STENCIL_STROKES) {
        // Fill shader
        call->uniformOffset = rhinvg_allocFragUniforms(gl, 2);
        if (call->uniformOffset == -1)
            goto error;
        call->uniformCount = 2;

        rhinvg_convertPaint(gl, nvg_fragUniformPtr(gl, call->uniformOffset), paint, scissor, strokeWidth, fringe,
                            -1.0f);
        rhinvg_convertPaint(gl, nvg_fragUniformPtr(gl, call->uniformOffset + gl->fragSize), paint, scissor,
                            strokeWidth, fringe, 1.0f - 0.5f / 255.0f);
    } else {
        // Fill shader
        call->uniformOffset = rhinvg_allocFragUniforms(gl, 1);
        if (call->uniformOffset == -1)
            goto error;
        call->uniformCount = 1;
        rhinvg_convertPaint(gl, nvg_fragUniformPtr(gl, call->uniformOffset), paint, scissor, strokeWidth, fringe,
                            -1.0f);
    }

    return;

error:
    // We get here if call alloc was ok, but something else is not.
    // Roll back the last call to prevent drawing it.
    if (gl->ncalls > 0)
        gl->ncalls--;
}

static void rhinvg_renderTriangles(void *uptr, NVGpaint *paint, NVGcompositeOperationState compositeOperation,
                                   NVGscissor *scissor,
                                   const NVGvertex *verts, int nverts, float fringe, int fontImage) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    SRRHINVGCall *call = rhinvg_allocCall(gl);
    SRRHINVGFragUniforms *frag;

    if (call == nullptr)
        return;

    call->type = GLNVG_TRIANGLES;
    call->fontImage = fontImage;
    call->blendFunc = rhinvg_blendCompositeOperation(compositeOperation);

    // Allocate vertices for all the paths.
    call->triangleOffset = rhinvg_allocVerts(gl, nverts);
    if (call->triangleOffset == -1)
        goto error;
    call->triangleCount = nverts;

    memcpy(&gl->verts[call->triangleOffset], verts, sizeof(NVGvertex) * nverts);

    // Fill shader
    call->uniformOffset = rhinvg_allocFragUniforms(gl, 1);
    if (call->uniformOffset == -1)
        goto error;
    call->uniformCount = 1;
    frag = nvg_fragUniformPtr(gl, call->uniformOffset);

    call->image = paint->image;
    rhinvg_convertPaint(gl, frag, paint, scissor, 1.0f, fringe, -1.0f);
    if (paint->image != 0) {
        frag->type = NSVG_SHADER_TEXTIMG;
    } else if (paint->innerColor.r == paint->outerColor.r && paint->innerColor.g == paint->outerColor.g &&
               paint->innerColor.b == paint->outerColor.b && paint->innerColor.a == paint->outerColor.a) {
        call->image = fontImage;
        frag->texType = 2;
        frag->type = NSVG_SHADER_IMG;
    } else {
        frag->type = NSVG_SHADER_TEXTGRAD;
    }

    return;

error:
    // We get here if call alloc was ok, but something else is not.
    // Roll back the last call to prevent drawing it.
    if (gl->ncalls > 0)
        gl->ncalls--;
}

static void rhinvg_renderDelete(void *uptr) {
    auto *gl = static_cast<SRRHINVGContext *>(uptr);
    if (gl == nullptr)
        return;

    if (gl->rhiCallbacks.destroy) {
        gl->rhiCallbacks.destroy(gl->rhiUserPtr);
    }
    free(gl->textures);
    free(gl->paths);
    free(gl->verts);
    free(gl->uniforms);
    free(gl->calls);
    free(gl);
}

NVGcontext *nvgCreateRHI(int flags,
                         const NVGRHICallbacks *rhiCallbacks,
                         void *rhiUserPtr) {
    NVGparams params;
    NVGcontext *ctx = nullptr;
    auto *gl = static_cast<SRRHINVGContext *>(malloc(sizeof(SRRHINVGContext)));
    if (gl == NULL)
        goto error;
    memset(gl, 0, sizeof(SRRHINVGContext));

    memset(&params, 0, sizeof(params));
    params.renderCreate = rhinvg_renderCreate;
    params.renderCreateTexture = rhinvg_renderCreateTexture;
    params.renderDeleteTexture = rhinvg_renderDeleteTexture;
    params.renderUpdateTexture = rhinvg_renderUpdateTexture;
    params.renderGetTextureSize = rhinvg_renderGetTextureSize;
    params.renderViewport = rhinvg_renderViewport;
    params.renderCancel = rhinvg_renderCancel;
    params.renderFlush = rhinvg_renderFlush;
    params.renderFill = rhinvg_renderFill;
    params.renderStroke = rhinvg_renderStroke;
    params.renderTriangles = rhinvg_renderTriangles;
    params.renderDelete = rhinvg_renderDelete;
    params.userPtr = gl;
    params.edgeAntiAlias = flags & NVG_ANTIALIAS ? 1 : 0;

    gl->flags = flags;
    gl->rhiUserPtr = rhiUserPtr;
    memset(&gl->rhiCallbacks, 0, sizeof(gl->rhiCallbacks));
    if (rhiCallbacks) {
        gl->rhiCallbacks = *rhiCallbacks;
    }

    ctx = nvgCreateInternal(&params);
    if (ctx == nullptr)
        goto error;

    return ctx;

error:
    // 'gl' is freed by nvgDeleteInternal.
    if (ctx != nullptr)
        nvgDeleteInternal(ctx);
    return nullptr;
}

void nvgDeleteRHI(NVGcontext *ctx) {
    nvgDeleteInternal(ctx);
}

int nvSrRhiCreateImageFromHandleRHI(NVGcontext *ctx, GLuint textureId, int w, int h, int imageFlags) {
    auto *gl = static_cast<SRRHINVGContext *>(nvgInternalParams(ctx)->userPtr);
    SRRHINVGTexture *tex = rhinvg_allocTexture(gl);

    if (tex == nullptr)
        return 0;
    tex->type = NVG_TEXTURE_RGBA;
    tex->tex = textureId;
    tex->flags = imageFlags;
    tex->width = w;
    tex->height = h;
    int ok = gl->rhiCallbacks.registerExternalTexture(
        gl->rhiUserPtr,
        tex->id,
        textureId,
        w,
        h,
        imageFlags);
    if (!ok) {
        memset(tex, 0, sizeof(*tex));
        return 0;
    }
    return tex->id;
}

GLuint nvSrRhiImageHandleRHI(NVGcontext *ctx, int image) {
    auto *gl = static_cast<SRRHINVGContext *>(nvgInternalParams(ctx)->userPtr);
    SRRHINVGTexture *tex = rhinvg_findTexture(gl, image);
    return tex->tex;
}
