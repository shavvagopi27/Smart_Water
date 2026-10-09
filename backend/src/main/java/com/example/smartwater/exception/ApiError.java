package com.example.smartwater.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(boolean success, String message, Instant timestamp, int status, Map<String, String> errors) {
    public static ApiError of(int status, String message) {
        return new ApiError(false, message, Instant.now(), status, null);
    }
    public static ApiError of(int status, String message, Map<String, String> errors) {
        return new ApiError(false, message, Instant.now(), status, errors);
    }
}
