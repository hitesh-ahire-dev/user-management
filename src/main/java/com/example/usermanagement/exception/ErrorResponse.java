package com.example.usermanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Consistent error payload returned to API clients.
 *
 * <p>{@code fieldErrors} is only serialized when present (validation failures),
 * keeping other error responses clean. Internal details such as stack traces or
 * SQL are intentionally never included here.
 *
 * @param timestamp   when the error occurred
 * @param status      numeric HTTP status
 * @param error       HTTP reason phrase
 * @param message     human-readable, safe message
 * @param path        the request URI
 * @param fieldErrors field-level validation messages (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
