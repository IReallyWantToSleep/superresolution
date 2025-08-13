#pragma once
#include "sr/sr_api.h"

#include "ffx-fsr2-api/ffx_error.h"
#include "ffx-fsr2-api/ffx_types.h"
#include "ffx-fsr2-api/gl/ffx_fsr2_gl.h"

#define SRFSR_CHECK(_expr)                         \
    if (FfxErrorCode _rc = (_expr); _rc != FFX_OK) \
    return (SRReturnCode)SR_RETURN_CODE_ERROR

static GLenum getGLFormatFromSurfaceFormat(SRSurfaceFormat fmt)
{
    switch (fmt)
    {
    case SR_SURFACE_FORMAT_R32G32B32A32_TYPELESS:
        return GL_RGBA32F;
    case SR_SURFACE_FORMAT_R32G32B32A32_FLOAT:
        return GL_RGBA32F;
    case SR_SURFACE_FORMAT_R16G16B16A16_FLOAT:
        return GL_RGBA16F;
    case SR_SURFACE_FORMAT_R32G32_FLOAT:
        return GL_RG32F;
    case SR_SURFACE_FORMAT_R32_UINT:
        return GL_R32UI;
    case SR_SURFACE_FORMAT_R8G8B8A8_TYPELESS:
        return GL_RGBA8;
    case SR_SURFACE_FORMAT_R8G8B8A8_UNORM:
        return GL_RGBA8;
    case SR_SURFACE_FORMAT_R11G11B10_FLOAT:
        return GL_R11F_G11F_B10F;
    case SR_SURFACE_FORMAT_R16G16_FLOAT:
        return GL_RG16F;
    case SR_SURFACE_FORMAT_R16G16_UINT:
        return GL_RG16UI;
    case SR_SURFACE_FORMAT_R16_FLOAT:
        return GL_R16F;
    case SR_SURFACE_FORMAT_R16_UINT:
        return GL_R16UI;
    case SR_SURFACE_FORMAT_R16_UNORM:
        return GL_R16;
    case SR_SURFACE_FORMAT_R16_SNORM:
        return GL_R16_SNORM;
    case SR_SURFACE_FORMAT_R8_UNORM:
        return GL_R8;
    case SR_SURFACE_FORMAT_R8G8_UNORM:
        return GL_RG8;
    case SR_SURFACE_FORMAT_R32_FLOAT:
        return GL_R32F;
    case SR_SURFACE_FORMAT_R8_UINT:
        return GL_R8UI;
    default:
        FFX_ASSERT_FAIL("");
        return 0;
    }
}

FfxResource SRTextureResourceToFfxResourceGL(const SRTextureResource *srTex)
{
    return ffxGetTextureResourceGL(
        (GLuint)(srTex->handle),
        srTex->desc.width,
        srTex->desc.height,
        getGLFormatFromSurfaceFormat(srTex->desc.format),
        nullptr);
}