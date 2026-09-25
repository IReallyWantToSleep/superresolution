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
#pragma once

#include <cstddef>
#include <glad/gl.h>
#include "nanovg.h"

// Create flags

enum NVGcreateFlags
{
    // Flag indicating if geometry based anti-aliasing is used (may not be needed when using MSAA).
    NVG_ANTIALIAS = 1 << 0,
    // Flag indicating if strokes should be drawn using stencil buffer. The rendering will be a little
    // slower, but path overlaps (i.e. self-intersecting or sharp turns) will be drawn just once.
    NVG_STENCIL_STROKES = 1 << 1,
    // Flag indicating that additional debug checks are done.
    NVG_DEBUG = 1 << 2,
};

enum NVGbackendMode
{
    NVG_BACKEND_RHI_DIRECT = 1,
};

typedef struct NVGRHIPath
{
    int fillOffset;
    int fillCount;
    int strokeOffset;
    int strokeCount;
} NVGRHIPath;

typedef struct NVGRHICall
{
    int type;
    int image;
    int pathOffset;
    int pathCount;
    int triangleOffset;
    int triangleCount;
    int uniformOffset;
    int uniformCount;
    int blendSrcRGB;
    int blendDstRGB;
    int blendSrcAlpha;
    int blendDstAlpha;
    int fontImage;
} NVGRHICall;

static_assert(sizeof(NVGRHICall) == 52, "NVGRHICall ABI changed");
static_assert(offsetof(NVGRHICall, fontImage) == 48, "NVGRHICall fontImage offset changed");

typedef struct NVGRHICallbacks
{
    int (*createTexture)(void *userPtr, int imageId, int type, int w, int h, int imageFlags,
                         const unsigned char *data, int dataSize);
    int (*registerExternalTexture)(void *userPtr, int imageId, unsigned int externalTextureHandle,
                                   int w, int h, int imageFlags);
    int (*updateTexture)(void *userPtr, int imageId, int x, int y, int w, int h,
                         const unsigned char *data, int dataSize);
    int (*deleteTexture)(void *userPtr, int imageId);
    void (*viewport)(void *userPtr, float width, float height, float devicePixelRatio);
    void (*flush)(void *userPtr,
                  float viewWidth,
                  float viewHeight,
                  const void *verts,
                  int nverts,
                  const void *paths,
                  int npaths,
                  const void *calls,
                  int ncalls,
                  const unsigned char *uniforms,
                  int uniformBytes,
                  int fragSize,
                  int callStride);
    void (*destroy)(void *userPtr);
} NVGRHICallbacks;

NVGcontext *nvgCreateRHI(int flags,
                         const NVGRHICallbacks *rhiCallbacks,
                         void *rhiUserPtr);

void nvgDeleteRHI(NVGcontext *ctx);

int nvSrRhiCreateImageFromHandleRHI(NVGcontext *ctx, GLuint textureId, int w, int h, int flags);

GLuint nvSrRhiImageHandleRHI(NVGcontext *ctx, int image);

// These are additional flags on top of NVGimageFlags.
enum NVGimageFlagsRHI
{
    NVG_IMAGE_NODELETE = 1 << 16, // Do not delete GL texture handle.
};

enum SRRHINVGUniformLoc
{
    GLNVG_LOC_VIEWSIZE,
    GLNVG_LOC_TEX,
    GLNVG_LOC_FRAG,
    GLNVG_MAX_LOCS
};

enum SRRHINVGShaderType
{
    NSVG_SHADER_FILLGRAD,
    NSVG_SHADER_FILLIMG,
    NSVG_SHADER_SIMPLE,
    NSVG_SHADER_IMG,
    NSVG_SHADER_TEXTGRAD,
    NSVG_SHADER_TEXTIMG
};

enum SRRHINVGUniformBindings
{
    GLNVG_FRAG_BINDING = 0,
};

struct SRRHINVGShader
{
    GLuint prog;
    GLuint frag;
    GLuint vert;
    GLint loc[GLNVG_MAX_LOCS];
};
typedef struct SRRHINVGShader SRRHINVGShader;

struct SRRHINVGTexture
{
    int id;
    GLuint tex;
    int width, height;
    int type;
    int flags;
};
typedef struct SRRHINVGTexture SRRHINVGTexture;

struct SRRHINVGBlend
{
    GLenum srcRGB;
    GLenum dstRGB;
    GLenum srcAlpha;
    GLenum dstAlpha;
};
typedef struct SRRHINVGBlend SRRHINVGblend;

enum SRRHINVGCallType
{
    GLNVG_NONE = 0,
    GLNVG_FILL,
    GLNVG_CONVEXFILL,
    GLNVG_STROKE,
    GLNVG_TRIANGLES,
};

struct SRRHINVGCall
{
    int type;
    int image;
    int pathOffset;
    int pathCount;
    int triangleOffset;
    int triangleCount;
    int uniformOffset;
    int uniformCount;
    SRRHINVGblend blendFunc;
    int fontImage;
};
typedef struct SRRHINVGCall SRRHINVGCall;

static_assert(sizeof(SRRHINVGCall) == sizeof(NVGRHICall), "GLNVGcall and NVGRHICall must share an ABI");
static_assert(offsetof(SRRHINVGCall, fontImage) == offsetof(NVGRHICall, fontImage), "GLNVGcall fontImage offset changed");

struct SRRHINVGPath
{
    int fillOffset;
    int fillCount;
    int strokeOffset;
    int strokeCount;
};
typedef struct SRRHINVGPath SRRHINVGPath;

struct SRRHINVGFragUniforms
{
    float scissorMat[12]; // matrices are actually 3 vec4s
    float paintMat[12];
    struct NVGcolor innerCol;
    struct NVGcolor outerCol;
    float scissorExt[2];
    float scissorScale[2];
    float extent[2];
    float radius;
    float feather;
    float strokeMult;
    float strokeThr;
    int texType;
    int type;
};
typedef struct SRRHINVGFragUniforms SRRHINVGFragUniforms;

struct SRRHINVGContext
{
    SRRHINVGTexture *textures;
    float view[2];
    int ntextures;
    int ctextures;
    int textureId;
    int fragSize;
    int flags;
    NVGRHICallbacks rhiCallbacks;
    void *rhiUserPtr;

    // Per frame buffers
    SRRHINVGCall *calls;
    int ccalls;
    int ncalls;
    SRRHINVGPath *paths;
    int cpaths;
    int npaths;
    struct NVGvertex *verts;
    int cverts;
    int nverts;
    unsigned char *uniforms;
    int cuniforms;
    int nuniforms;

    int dummyTex;
};
typedef struct SRRHINVGContext SRRHINVGContext;