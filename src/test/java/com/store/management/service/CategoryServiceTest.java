package com.store.management.service;

import com.store.management.dto.CategoryRequest;
import com.store.management.dto.CategoryResponse;
import com.store.management.entity.Category;
import com.store.management.exception.DuplicateResourceException;
import com.store.management.exception.ResourceNotFoundException;
import com.store.management.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static Category category(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    @Test
    void createCategory_savesTrimmedName() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(new CategoryRequest("  Electronics ", "Gadgets"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.description()).isEqualTo("Gadgets");
    }

    @Test
    void createCategory_withDuplicateName_throwsConflict() {
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(new CategoryRequest("Electronics", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void getCategories_returnsAll() {
        when(categoryRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(category(1L, "A"), category(2L, "B")));

        assertThat(categoryService.getCategories()).extracting(CategoryResponse::name).containsExactly("A", "B");
    }

    @Test
    void getCategory_whenMissing_throwsNotFound() {
        when(categoryRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCategory_withNameOfAnotherCategory_throwsConflict() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Old")));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Taken", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, new CategoryRequest("Taken", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void updateCategory_updatesFields() {
        Category existing = category(1L, "Old");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("New", 1L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(existing)).thenReturn(existing);

        CategoryResponse response = categoryService.updateCategory(1L, new CategoryRequest("New", "Desc"));

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Desc");
    }

    @Test
    void deleteCategory_deletesExisting() {
        Category existing = category(1L, "A");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(existing);
    }
}
