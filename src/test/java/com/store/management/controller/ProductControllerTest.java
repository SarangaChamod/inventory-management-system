package com.store.management.controller;

import com.store.management.dto.ProductRequest;
import com.store.management.dto.ProductResponse;
import com.store.management.dto.StockStatusResponse;
import com.store.management.exception.DuplicateResourceException;
import com.store.management.exception.InsufficientStockException;
import com.store.management.exception.ResourceNotFoundException;
import com.store.management.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"name":"Wireless Mouse","description":"Ergonomic","sku":"WM-1001",
             "category":"Electronics","price":24.99,"quantity":50}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private static ProductResponse sample(long id, int quantity) {
        Instant now = Instant.parse("2026-11-16T10:15:30Z");
        return new ProductResponse(id, "Wireless Mouse", "Ergonomic", "WM-1001", "Electronics",
                new BigDecimal("24.99"), quantity, now, now);
    }

    @Test
    void getProducts_returnsList() throws Exception {
        when(productService.getProducts(null, null)).thenReturn(List.of(sample(1L, 50)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].sku").value("WM-1001"));
    }

    @Test
    void getProducts_passesSearchAndFilterParams() throws Exception {
        when(productService.getProducts("mouse", "Electronics")).thenReturn(List.of(sample(1L, 50)));

        mockMvc.perform(get("/api/products").param("name", "mouse").param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getProduct_returnsProduct() throws Exception {
        when(productService.getProduct(1L)).thenReturn(sample(1L, 50));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.price").value(24.99));
    }

    @Test
    void getProduct_whenMissing_returns404() throws Exception {
        when(productService.getProduct(99L)).thenThrow(new ResourceNotFoundException("Product", 99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id 99"))
                .andExpect(jsonPath("$.path").value("/api/products/99"));
    }

    @Test
    void getProduct_withNonNumericId_returns400() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createProduct_returns201WithLocation() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(sample(1L, 50));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/products/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createProduct_withInvalidInput_returns400WithFieldErrors() throws Exception {
        String body = """
                {"name":"","price":-5,"quantity":-1}
                """;

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").value("price must be positive"))
                .andExpect(jsonPath("$.validationErrors.quantity").value("quantity must not be negative"));
        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_withMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable JSON request body"));
    }

    @Test
    void createProduct_withDuplicateSku_returns409() throws Exception {
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateResourceException("A product with SKU 'WM-1001' already exists"));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updateProduct_returns200() throws Exception {
        when(productService.updateProduct(eq(1L), any(ProductRequest.class))).thenReturn(sample(1L, 50));

        mockMvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteProduct_returns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
        verify(productService).deleteProduct(1L);
    }

    @Test
    void deleteProduct_whenMissing_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Product", 9L)).when(productService).deleteProduct(9L);

        mockMvc.perform(delete("/api/products/9"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkStock_returnsStatus() throws Exception {
        when(productService.checkStock(1L, 10))
                .thenReturn(new StockStatusResponse(1L, "Wireless Mouse", "WM-1001", 50, 10, true));

        mockMvc.perform(get("/api/products/1/stock").param("required", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(50))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void checkStock_withInvalidRequired_returns400() throws Exception {
        mockMvc.perform(get("/api/products/1/stock").param("required", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.required").value("required must be at least 1"));
    }

    @Test
    void increaseStock_returnsUpdatedProduct() throws Exception {
        when(productService.increaseStock(1L, 5)).thenReturn(sample(1L, 55));

        mockMvc.perform(post("/api/products/1/stock/increase")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(55));
    }

    @Test
    void decreaseStock_withZeroQuantity_returns400() throws Exception {
        mockMvc.perform(post("/api/products/1/stock/decrease")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.quantity").value("quantity must be at least 1"));
    }

    @Test
    void decreaseStock_whenInsufficient_returns409() throws Exception {
        when(productService.decreaseStock(1L, 100)).thenThrow(new InsufficientStockException(1L, 50, 100));

        mockMvc.perform(post("/api/products/1/stock/decrease")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":100}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Insufficient stock for product 1: available 50, requested 100"));
    }
}
