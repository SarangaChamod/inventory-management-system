package com.store.management.repository;

import com.store.management.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against H2 (PostgreSQL mode) with the real Flyway migrations,
 * so it also checks that the schema matches the entities.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        productRepository.saveAndFlush(product("Wireless Mouse", "WM-1001", "Electronics", 10));
        productRepository.saveAndFlush(product("Gaming Mouse 100%", "GM-2002", "Electronics", 0));
        productRepository.saveAndFlush(product("Office Chair", "OC-3003", "Furniture", 4));
    }

    private static Product product(String name, String sku, String category, int quantity) {
        Product p = new Product();
        p.setName(name);
        p.setSku(sku);
        p.setCategory(category);
        p.setPrice(new BigDecimal("19.99"));
        p.setQuantity(quantity);
        return p;
    }

    @Test
    void save_setsIdAndTimestamps() {
        Product saved = productRepository.saveAndFlush(product("Desk", "DK-1", "Furniture", 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsBySkuIgnoreCase_findsExistingSku() {
        assertThat(productRepository.existsBySkuIgnoreCase("wm-1001")).isTrue();
        assertThat(productRepository.existsBySkuIgnoreCase("NOPE")).isFalse();
    }

    @Test
    void existsBySkuIgnoreCaseAndIdNot_ignoresOwnRow() {
        Product mouse = productRepository.findAll().stream()
                .filter(p -> "WM-1001".equals(p.getSku())).findFirst().orElseThrow();

        assertThat(productRepository.existsBySkuIgnoreCaseAndIdNot("WM-1001", mouse.getId())).isFalse();
        assertThat(productRepository.existsBySkuIgnoreCaseAndIdNot("GM-2002", mouse.getId())).isTrue();
    }

    @Test
    void uniqueConstraint_rejectsDuplicateSku() {
        assertThatThrownBy(() -> productRepository.saveAndFlush(product("Copy", "WM-1001", "Electronics", 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_rejectsNegativeQuantity() {
        assertThatThrownBy(() -> productRepository.saveAndFlush(product("Bad", "BAD-1", "Electronics", -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nameContains_isCaseInsensitive() {
        List<Product> result = productRepository.findAll(
                ProductSpecifications.nameContains("MOUSE"), Sort.by("id"));

        assertThat(result).extracting(Product::getSku).containsExactly("WM-1001", "GM-2002");
    }

    @Test
    void nameContains_treatsWildcardsLiterally() {
        List<Product> result = productRepository.findAll(ProductSpecifications.nameContains("100%"));

        assertThat(result).extracting(Product::getSku).containsExactly("GM-2002");
    }

    @Test
    void categoryEquals_filtersByCategory() {
        List<Product> result = productRepository.findAll(
                ProductSpecifications.categoryEquals("furniture"));

        assertThat(result).extracting(Product::getName).containsExactly("Office Chair");
    }

    @Test
    void findByIdForUpdate_returnsProduct() {
        Long id = productRepository.findAll().get(0).getId();

        assertThat(productRepository.findByIdForUpdate(id)).isPresent();
    }
}
