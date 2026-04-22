package com.isi.techcenter_backend.error;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        String error,
        String message,
        OffsetDateTime timestamp) {
}
