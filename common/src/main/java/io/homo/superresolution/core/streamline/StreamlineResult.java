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
