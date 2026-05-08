package com.fbp.engine.engine;

public class FlowManagerException extends RuntimeException {
    public enum ErrorType {
        NOT_FOUND,
        CONFLICT,
        VALIDATION
    }

    private final ErrorType errorType;

    public FlowManagerException(String message) {
        this(ErrorType.VALIDATION, message);
    }

    public FlowManagerException(String message, Throwable cause) {
        this(ErrorType.VALIDATION, message, cause);
    }

    public FlowManagerException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public FlowManagerException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
