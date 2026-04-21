package com.isi.techcenter_backend.auth.error;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        String error,
        String message,
        OffsetDateTime timestamp) {
}
