package com.store.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Amount by which to increase or decrease a product's stock")
public record StockAdjustmentRequest(

        @Schema(description = "Number of units, must be at least 1", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 1_000_000, message = "quantity must be at most 1000000")
        Integer quantity
) {
}
