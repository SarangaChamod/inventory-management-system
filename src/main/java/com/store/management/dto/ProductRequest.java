package com.store.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload for creating or fully updating a product")
public record ProductRequest(

        @Schema(description = "Product name", example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @Schema(description = "Product description", example = "2.4 GHz ergonomic wireless mouse")
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description,

        @Schema(description = "Stock keeping unit, unique (stored in upper case)", example = "WM-1001")
        @Size(max = 64, message = "sku must be at most 64 characters")
        @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "sku may contain only letters, digits, '.', '_' and '-'")
        String sku,

        @Schema(description = "Category name", example = "Electronics")
        @Size(max = 100, message = "category must be at most 100 characters")
        String category,

        @Schema(description = "Unit price, must be positive", example = "24.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        @Digits(integer = 10, fraction = 2, message = "price must have at most 10 integer digits and 2 decimals")
        BigDecimal price,

        @Schema(description = "Quantity in stock, must be zero or more", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity must not be negative")
        Integer quantity
) {
}
