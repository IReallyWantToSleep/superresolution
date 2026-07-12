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

package io.homo.superresolution.core.streamline;

public final class StreamlineResult {
    private static final String[] NAMES = {
            "eOk",
            "eErrorIO",
            "eErrorDriverOutOfDate",
            "eErrorOSOutOfDate",
            "eErrorOSDisabledHWS",
            "eErrorDeviceNotCreated",
            "eErrorNoSupportedAdapterFound",
            "eErrorAdapterNotSupported",
            "eErrorNoPlugins",
            "eErrorVulkanAPI",
            "eErrorDXGIAPI",
            "eErrorD3DAPI",
            "eErrorNRDAPI",
            "eErrorNVAPI",
            "eErrorReflexAPI",
            "eErrorNGXFailed",
            "eErrorJSONParsing",
            "eErrorMissingProxy",
            "eErrorMissingResourceState",
            "eErrorInvalidIntegration",
            "eErrorMissingInputParameter",
            "eErrorNotInitialized",
            "eErrorComputeFailed",
            "eErrorInitNotCalled",
            "eErrorExceptionHandler",
            "eErrorInvalidParameter",
            "eErrorMissingConstants",
            "eErrorDuplicatedConstants",
            "eErrorMissingOrInvalidAPI",
            "eErrorCommonConstantsMissing",
            "eErrorUnsupportedInterface",
            "eErrorFeatureMissing",
            "eErrorFeatureNotSupported",
            "eErrorFeatureMissingHooks",
            "eErrorFeatureFailedToLoad",
            "eErrorFeatureWrongPriority",
            "eErrorFeatureMissingDependency",
            "eErrorFeatureManagerInvalidState",
            "eErrorInvalidState",
            "eWarnOutOfVRAM"
    };

    private StreamlineResult() {
    }

    public static String nameOf(int result) {
        if (result < 0 || result >= NAMES.length) {
            return "Unknown Streamline result";
        }
        return NAMES[result];
    }
}
