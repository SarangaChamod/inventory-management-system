package com.store.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Consistent error body returned for every failed request")
public record ApiErrorResponse(
        @Schema(example = "2026-11-16T10:15:30Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Validation failed") String message,
        @Schema(example = "/api/products") String path,
        @Schema(description = "Field-level validation messages, present only for validation errors",
                example = "{\"price\": \"price must be positive\"}")
        Map<String, String> validationErrors
) {
}
