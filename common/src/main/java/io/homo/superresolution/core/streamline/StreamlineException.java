package io.homo.superresolution.core.streamline;

public final class StreamlineException extends IllegalStateException {
    private final int result;

    public StreamlineException(String operation, int result) {
        super(operation + " failed: " + StreamlineResult.nameOf(result) + " (" + result + ")");
        this.result = result;
    }

    public int result() {
        return result;
    }
}
