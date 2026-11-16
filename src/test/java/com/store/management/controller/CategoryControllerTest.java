package com.store.management.controller;

import com.store.management.dto.CategoryRequest;
import com.store.management.dto.CategoryResponse;
import com.store.management.exception.DuplicateResourceException;
import com.store.management.exception.ResourceNotFoundException;
import com.store.management.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    private static CategoryResponse sample(long id, String name) {
        return new CategoryResponse(id, name, "desc", Instant.parse("2026-11-16T10:15:30Z"));
    }

    @Test
    void getCategories_returnsList() throws Exception {
        when(categoryService.getCategories()).thenReturn(List.of(sample(1L, "Electronics")));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getCategory_whenMissing_returns404() throws Exception {
        when(categoryService.getCategory(5L)).thenThrow(new ResourceNotFoundException("Category", 5L));

        mockMvc.perform(get("/api/categories/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found with id 5"));
    }

    @Test
    void createCategory_returns201() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(sample(1L, "Electronics"));

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Electronics\",\"description\":\"desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createCategory_withBlankName_returns400() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").value("name is required"));
        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_withDuplicateName_returns409() throws Exception {
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new DuplicateResourceException("A category named 'Electronics' already exists"));

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Electronics\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateCategory_returns200() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryRequest.class))).thenReturn(sample(1L, "Home"));

        mockMvc.perform(put("/api/categories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Home\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Home"));
    }

    @Test
    void deleteCategory_returns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
        verify(categoryService).deleteCategory(1L);
    }
}
