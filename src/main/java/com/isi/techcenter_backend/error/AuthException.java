package com.isi.techcenter_backend.error;

public class AuthException extends RuntimeException {

    private final AppErrorType errorType;

    public AuthException(AppErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public AppErrorType getErrorType() {
        return errorType;
    }
}
