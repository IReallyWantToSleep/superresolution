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

final class NgxNative {
    private NgxNative() {
    }

    static native int nInitWithProjectId(
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

    static native int nShutdown();

    static native int nGetFeatureRequirements(
            long instance,
            long physicalDevice,
            NgxFeatureDiscoveryInfo discoveryInfo,
            NgxFeatureRequirement outRequirements
    );

    static native int nAllocateParameters(NgxParameters outParameters);

    static native int nGetCapabilityParameters(NgxParameters outParameters);

    static native int nDestroyParameters(long parameters);

    static native int nCreateFeature(long commandBuffer, int feature, long parameters, NgxFeature outFeature);

    static native int nReleaseFeature(long feature);

    static native int nEvaluateFeature(
            long commandBuffer,
            long feature,
            long parameters,
            NgxProgressCallback progressCallback
    );

    static native void nParametersSetUnsignedLong(long parameters, String name, long value);

    static native void nParametersSetFloat(long parameters, String name, float value);

    static native void nParametersSetDouble(long parameters, String name, double value);

    static native void nParametersSetUnsignedInt(long parameters, String name, long value);

    static native void nParametersSetInt(long parameters, String name, int value);

    static native void nParametersSetPointer(long parameters, String name, long value);

    static native int nParametersGetUnsignedLong(long parameters, String name, long[] outValue);

    static native int nParametersGetFloat(long parameters, String name, float[] outValue);

    static native int nParametersGetDouble(long parameters, String name, double[] outValue);

    static native int nParametersGetUnsignedInt(long parameters, String name, long[] outValue);

    static native int nParametersGetInt(long parameters, String name, int[] outValue);

    static native int nParametersGetPointer(long parameters, String name, long[] outValue);

    static native void nParametersReset(long parameters);
}
