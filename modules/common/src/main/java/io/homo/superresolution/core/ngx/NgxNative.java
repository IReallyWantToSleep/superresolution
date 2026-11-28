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

package io.homo.superresolution.core.ngx;

/**
 * The NGX entry points. Every one is {@code synchronized} on this class: the NGX API is not
 * thread-safe (DLSS Programming Guide 5.2.4; DLSS-FG Programming Guide: "Ensure that all NGX API
 * calls are made from a single thread or are properly synchronized"), and two threads call it.
 * The render thread records DLSS (and DLSS-RR) evaluations into Minecraft's frame, while frame
 * generation records DLSS-G evaluations on its own thread, Wisteria's included, since it goes
 * through {@link NgxVulkan} too, so nothing else kept their calls apart. The lock does not fix
 * the Xid 109 (context switch timeout) hangs inside the DLSS evaluation on the 26.3 Vulkan
 * backend: they continued with it in place, with DLSS-G idle when they hit.
 *
 * <p>The lock is held only for the native call itself. Each parameter object and feature is used
 * by one thread, so a sequence of parameter writes followed by an evaluation needs no wider
 * lock. Feature creation can hold it for a few hundred milliseconds (DLSS-G loads its kernels),
 * which then delays the other thread's evaluation once.</p>
 */
final class NgxNative {
    private NgxNative() {
    }

    static synchronized native int nInitWithProjectId(
            String projectId,
            int engineType,
            String engineVersion,
            String applicationDataPath,
            long instance,
            long physicalDevice,
            long device,
            long getInstanceProcAddr,
            long getDeviceProcAddr,
            NgxFeatureCommonInfo featureInfo,
            int sdkVersion
    );

    static synchronized native int nShutdown();

    static synchronized native int nGetFeatureRequirements(
            long instance,
            long physicalDevice,
            NgxFeatureDiscoveryInfo discoveryInfo,
            NgxFeatureRequirement outRequirements
    );

    static synchronized native int nAllocateParameters(NgxParameters outParameters);

    static synchronized native int nGetCapabilityParameters(NgxParameters outParameters);

    static synchronized native int nDestroyParameters(long parameters);

    static synchronized native int nCreateFeature(long commandBuffer, int feature, long parameters, NgxFeature outFeature);

    static synchronized native int nReleaseFeature(long feature);

    static synchronized native int nEvaluateFeature(
            long commandBuffer,
            long feature,
            long parameters,
            NgxProgressCallback progressCallback
    );

    static synchronized native void nParametersSetUnsignedLong(long parameters, String name, long value);

    static synchronized native void nParametersSetFloat(long parameters, String name, float value);

    static synchronized native void nParametersSetDouble(long parameters, String name, double value);

    static synchronized native void nParametersSetUnsignedInt(long parameters, String name, long value);

    static synchronized native void nParametersSetInt(long parameters, String name, int value);

    static synchronized native void nParametersSetPointer(long parameters, String name, long value);

    static synchronized native int nParametersGetUnsignedLong(long parameters, String name, long[] outValue);

    static synchronized native int nParametersGetFloat(long parameters, String name, float[] outValue);

    static synchronized native int nParametersGetDouble(long parameters, String name, double[] outValue);

    static synchronized native int nParametersGetUnsignedInt(long parameters, String name, long[] outValue);

    static synchronized native int nParametersGetInt(long parameters, String name, int[] outValue);

    static synchronized native int nParametersGetPointer(long parameters, String name, long[] outValue);

    static synchronized native void nParametersReset(long parameters);
}
