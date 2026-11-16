package com.store.management.service;

import com.store.management.dto.ProductRequest;
import com.store.management.dto.ProductResponse;
import com.store.management.dto.StockStatusResponse;
import com.store.management.entity.Product;
import com.store.management.exception.DuplicateResourceException;
import com.store.management.exception.InsufficientStockException;
import com.store.management.exception.ResourceNotFoundException;
import com.store.management.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private static ProductRequest request(String sku, int quantity) {
        return new ProductRequest(" Wireless Mouse ", "Ergonomic", sku, "Electronics",
                new BigDecimal("24.99"), quantity);
    }

    private static Product product(Long id, int quantity) {
        Product p = new Product();
        p.setId(id);
        p.setName("Wireless Mouse");
        p.setSku("WM-1001");
        p.setCategory("Electronics");
        p.setPrice(new BigDecimal("24.99"));
        p.setQuantity(quantity);
        return p;
    }

    @Test
    void createProduct_savesNormalizedProduct() {
        when(productRepository.existsBySkuIgnoreCase("WM-1001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request(" wm-1001 ", 10));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Wireless Mouse");
        assertThat(captor.getValue().getSku()).isEqualTo("WM-1001");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.quantity()).isEqualTo(10);
    }

    @Test
    void createProduct_withDuplicateSku_throwsConflict() {
        when(productRepository.existsBySkuIgnoreCase("WM-1001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request("WM-1001", 10)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("WM-1001");
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_withoutSku_skipsDuplicateCheck() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.createProduct(request("  ", 1));

        assertThat(response.sku()).isNull();
        verify(productRepository, never()).existsBySkuIgnoreCase(anyString());
    }

    @Test
    void getProduct_whenMissing_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProducts_returnsMappedList() {
        when(productRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(product(1L, 5), product(2L, 7)));

        List<ProductResponse> result = productService.getProducts("mouse", "electronics");

        assertThat(result).extracting(ProductResponse::id).containsExactly(1L, 2L);
    }

    @Test
    void updateProduct_updatesFields() {
        Product existing = product(1L, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("NEW-1", 1L)).thenReturn(false);
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductRequest update = new ProductRequest("Keyboard", null, "new-1", "Office",
                new BigDecimal("49.50"), 3);
        ProductResponse response = productService.updateProduct(1L, update);

        assertThat(response.name()).isEqualTo("Keyboard");
        assertThat(response.sku()).isEqualTo("NEW-1");
        assertThat(response.price()).isEqualByComparingTo("49.50");
        assertThat(response.quantity()).isEqualTo(3);
    }

    @Test
    void updateProduct_withSkuOfAnotherProduct_throwsConflict() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, 5)));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("TAKEN", 1L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, request("taken", 5)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteProduct_whenMissing_throwsNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(5L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void deleteProduct_deletesExisting() {
        Product existing = product(1L, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(1L);

        verify(productRepository).delete(existing);
    }

    @Test
    void increaseStock_addsQuantity() {
        Product existing = product(1L, 5);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductResponse response = productService.increaseStock(1L, 10);

        assertThat(response.quantity()).isEqualTo(15);
    }

    @Test
    void decreaseStock_subtractsQuantity() {
        Product existing = product(1L, 5);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        when(productRepository.saveAndFlush(existing)).thenReturn(existing);

        ProductResponse response = productService.decreaseStock(1L, 5);

        assertThat(response.quantity()).isZero();
    }

    @Test
    void decreaseStock_belowZero_throwsInsufficientStock() {
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product(1L, 2)));

        assertThatThrownBy(() -> productService.decreaseStock(1L, 3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available 2")
                .hasMessageContaining("requested 3");
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void checkStock_reportsAvailability() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, 4)));

        StockStatusResponse enough = productService.checkStock(1L, 4);
        StockStatusResponse notEnough = productService.checkStock(1L, 5);

        assertThat(enough.available()).isTrue();
        assertThat(notEnough.available()).isFalse();
        assertThat(notEnough.availableQuantity()).isEqualTo(4);
    }
}
