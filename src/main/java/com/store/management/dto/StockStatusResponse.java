package com.store.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stock availability of a product")
public record StockStatusResponse(
        @Schema(example = "1") Long productId,
        @Schema(example = "Wireless Mouse") String productName,
        @Schema(example = "WM-1001") String sku,
        @Schema(description = "Units currently in stock", example = "50") Integer availableQuantity,
        @Schema(description = "Units the caller asked about", example = "10") Integer requestedQuantity,
        @Schema(description = "True if availableQuantity >= requestedQuantity", example = "true") boolean available
) {
}
