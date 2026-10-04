/*
 * ffx_fsr3.h -- compatibility header.
 *
 * SPDX-License-Identifier: MIT
 * =============================================================================
 * In AMD's FidelityFX SDK 1.1.3 this header aggregated the FSR3 API:
 *
 *     #include <FidelityFX/host/ffx_interface.h>
 *     #include <FidelityFX/host/ffx_fsr3upscaler.h>
 *     #include <FidelityFX/host/ffx_frameinterpolation.h>
 *
 * The Arm fork replaced FSR3 upscaling with NSS and deleted
 * `ffx_fsr3upscaler.h`, so the D3D12 frame-interpolation swapchain -- which this tree
 * imports from 1.1.3 and which includes this header -- stopped resolving.
 *
 * The swapchain needs exactly one thing from it that survives the restructure:
 * `FfxFsr3FrameGenerationFlags`, which 1.1.3 declared in ffx_fsr3upscaler.h and used as
 * the type of a cached copy of `FfxFrameGenerationConfig::flags`. That field is still
 * present and still a FfxUInt32, so the type is restored here as one.
 *
 * Providing the header rather than editing the swapchain keeps the imported code
 * untouched and makes any other 1.1.3-era include of ffx_fsr3.h keep working.
 */
#ifndef FFX_FSR3_H
#define FFX_FSR3_H

#include <FidelityFX/host/ffx_interface.h>
#include <FidelityFX/host/ffx_frameinterpolation.h>

/// Frame generation flags, as 1.1.3 declared them in ffx_fsr3upscaler.h.
///
/// The swapchain uses this only to cache FfxFrameGenerationConfig::flags so it can detect
/// a change; that field is a FfxUInt32 in this tree, so the alias is exact.
typedef uint32_t FfxFsr3FrameGenerationFlags;

#endif /* FFX_FSR3_H */
