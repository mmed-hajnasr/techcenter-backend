package com.isi.techcenter_backend.auth.error;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(AuthException exception) {
        HttpStatus status = mapAuthStatus(exception.getErrorType());
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                exception.getErrorType().name(),
                exception.getMessage(),
                OffsetDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        AppErrorType errorType = mapValidationError(fieldError == null ? null : fieldError.getField());
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
            errorType.name(),
                "Invalid request payload",
                OffsetDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandled(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "Unexpected internal error",
                OffsetDateTime.now()));
    }

    private HttpStatus mapAuthStatus(AppErrorType errorType) {
        return switch (errorType) {
            case INCORRECT_LOGIN, NOT_LOGGED_IN, UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case INVALID_EMAIL, INVALID_USERNAME, INVALID_PASSWORD -> HttpStatus.BAD_REQUEST;
        };
    }

    private AppErrorType mapValidationError(String fieldName) {
        if ("email".equals(fieldName)) {
            return AppErrorType.INVALID_EMAIL;
        }
        if ("username".equals(fieldName)) {
            return AppErrorType.INVALID_USERNAME;
        }
        return AppErrorType.INVALID_PASSWORD;
    }
}
