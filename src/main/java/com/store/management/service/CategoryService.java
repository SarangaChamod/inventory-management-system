package com.store.management.service;

import com.store.management.dto.CategoryRequest;
import com.store.management.dto.CategoryResponse;
import com.store.management.entity.Category;
import com.store.management.exception.DuplicateResourceException;
import com.store.management.exception.ResourceNotFoundException;
import com.store.management.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
    private static final String RESOURCE = "Category";

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    public CategoryResponse getCategory(Long id) {
        return CategoryResponse.from(findCategory(id));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists");
        }
        Category category = new Category();
        category.setName(name);
        category.setDescription(trimToNull(request.description()));
        Category saved = categoryRepository.save(category);
        log.info("Created category id={} name={}", saved.getId(), saved.getName());
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A category named '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(trimToNull(request.description()));
        Category saved = categoryRepository.saveAndFlush(category);
        log.info("Updated category id={}", saved.getId());
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        categoryRepository.delete(category);
        log.info("Deleted category id={}", id);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
