package com.fbp.engine.api;

public class ApiServerException extends RuntimeException {
    public ApiServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
