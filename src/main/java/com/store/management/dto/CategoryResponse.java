package com.store.management.dto;

import com.store.management.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Category returned by the API")
public record CategoryResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Electronics") String name,
        @Schema(example = "Computers, accessories and gadgets") String description,
        @Schema(example = "2026-11-16T10:15:30Z") Instant createdAt
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt());
    }
}
