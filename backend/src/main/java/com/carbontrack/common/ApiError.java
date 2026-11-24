package com.carbontrack.common;

import java.time.Instant;
import org.springframework.http.HttpStatus;

/** The single error body used by every failing response (shared contract A8). */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
