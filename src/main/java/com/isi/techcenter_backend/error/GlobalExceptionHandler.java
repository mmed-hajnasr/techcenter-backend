package com.isi.techcenter_backend.error;

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
            case DOMAIN_NOT_FOUND, USER_NOT_FOUND, RESEARCHER_NOT_FOUND, PUBLICATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_EMAIL, INVALID_USERNAME, INVALID_PASSWORD, INVALID_ROLE, DOMAIN_NAME_ALREADY_EXISTS,
                    INVALID_DOMAIN_NAME, PUBLICATION_DOI_ALREADY_EXISTS, INVALID_PUBLICATION_TITLE ->
                HttpStatus.BAD_REQUEST;
        };
    }

    private AppErrorType mapValidationError(String fieldName) {
        if ("email".equals(fieldName)) {
            return AppErrorType.INVALID_EMAIL;
        }
        if ("username".equals(fieldName)) {
            return AppErrorType.INVALID_USERNAME;
        }
        if ("name".equals(fieldName)) {
            return AppErrorType.INVALID_DOMAIN_NAME;
        }
        if ("titre".equals(fieldName)) {
            return AppErrorType.INVALID_PUBLICATION_TITLE;
        }
        if ("role".equals(fieldName)) {
            return AppErrorType.INVALID_ROLE;
        }
        return AppErrorType.INVALID_PASSWORD;
    }
}
