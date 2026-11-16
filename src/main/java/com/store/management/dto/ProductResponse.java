package com.store.management.dto;

import com.store.management.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Product returned by the API")
public record ProductResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Wireless Mouse") String name,
        @Schema(example = "2.4 GHz ergonomic wireless mouse") String description,
        @Schema(example = "WM-1001") String sku,
        @Schema(example = "Electronics") String category,
        @Schema(example = "24.99") BigDecimal price,
        @Schema(example = "50") Integer quantity,
        @Schema(example = "2026-11-16T10:15:30Z") Instant createdAt,
        @Schema(example = "2026-11-16T10:15:30Z") Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getCategory(),
                product.getPrice(),
                product.getQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
