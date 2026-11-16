package com.store.management.repository;

import com.store.management.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Reusable query predicates for searching and filtering products.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Case-insensitive "name contains" match. Wildcard characters in the input are treated literally. */
    public static Specification<Product> nameContains(String name) {
        String pattern = "%" + escapeLike(name.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    /** Case-insensitive exact category match. */
    public static Specification<Product> categoryEquals(String category) {
        String value = category.toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), value);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
