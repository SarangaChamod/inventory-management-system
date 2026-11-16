package com.store.management.repository;

import com.store.management.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private static Category category(String name) {
        Category c = new Category();
        c.setName(name);
        return c;
    }

    @Test
    void save_setsIdAndCreatedAt() {
        Category saved = categoryRepository.saveAndFlush(category("Electronics"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void existsByNameIgnoreCase_matchesRegardlessOfCase() {
        Category saved = categoryRepository.saveAndFlush(category("Electronics"));

        assertThat(categoryRepository.existsByNameIgnoreCase("ELECTRONICS")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("electronics", saved.getId())).isFalse();
    }

    @Test
    void uniqueConstraint_rejectsDuplicateName() {
        categoryRepository.saveAndFlush(category("Furniture"));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(category("Furniture")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
