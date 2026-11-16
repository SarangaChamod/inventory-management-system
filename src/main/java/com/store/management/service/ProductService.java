package com.store.management.service;

import com.store.management.dto.ProductRequest;
import com.store.management.dto.ProductResponse;
import com.store.management.dto.StockStatusResponse;
import com.store.management.entity.Product;
import com.store.management.exception.DuplicateResourceException;
import com.store.management.exception.InsufficientStockException;
import com.store.management.exception.ResourceNotFoundException;
import com.store.management.repository.ProductRepository;
import com.store.management.repository.ProductSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String RESOURCE = "Product";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Returns all products, optionally filtered by a name fragment and/or a category.
     * Both filters are case-insensitive; blank values are ignored.
     */
    public List<ProductResponse> getProducts(String name, String category) {
        Specification<Product> spec = Specification.where(null);
        if (StringUtils.hasText(name)) {
            spec = spec.and(ProductSpecifications.nameContains(name.trim()));
        }
        if (StringUtils.hasText(category)) {
            spec = spec.and(ProductSpecifications.categoryEquals(category.trim()));
        }
        return productRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String sku = normalizeSku(request.sku());
        if (sku != null && productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("A product with SKU '" + sku + "' already exists");
        }
        Product product = new Product();
        apply(product, request, sku);
        Product saved = productRepository.save(product);
        log.info("Created product id={} sku={}", saved.getId(), saved.getSku());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProduct(id);
        String sku = normalizeSku(request.sku());
        if (sku != null && productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateResourceException("A product with SKU '" + sku + "' already exists");
        }
        apply(product, request, sku);
        Product saved = productRepository.saveAndFlush(product);
        log.info("Updated product id={}", saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
        log.info("Deleted product id={}", id);
    }

    /** Reports whether at least {@code requiredQuantity} units are in stock. */
    public StockStatusResponse checkStock(Long id, int requiredQuantity) {
        Product product = findProduct(id);
        int available = product.getQuantity();
        return new StockStatusResponse(product.getId(), product.getName(), product.getSku(),
                available, requiredQuantity, available >= requiredQuantity);
    }

    @Transactional
    public ProductResponse increaseStock(Long id, int amount) {
        Product product = findProductForUpdate(id);
        long newQuantity = (long) product.getQuantity() + amount;
        if (newQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Resulting stock exceeds the maximum allowed value");
        }
        product.setQuantity((int) newQuantity);
        Product saved = productRepository.saveAndFlush(product);
        log.info("Increased stock of product id={} by {} to {}", id, amount, saved.getQuantity());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse decreaseStock(Long id, int amount) {
        Product product = findProductForUpdate(id);
        int available = product.getQuantity();
        if (amount > available) {
            throw new InsufficientStockException(id, available, amount);
        }
        product.setQuantity(available - amount);
        Product saved = productRepository.saveAndFlush(product);
        log.info("Decreased stock of product id={} by {} to {}", id, amount, saved.getQuantity());
        return ProductResponse.from(saved);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private Product findProductForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private static void apply(Product product, ProductRequest request, String normalizedSku) {
        product.setName(request.name().trim());
        product.setDescription(trimToNull(request.description()));
        product.setSku(normalizedSku);
        product.setCategory(trimToNull(request.category()));
        product.setPrice(request.price());
        product.setQuantity(request.quantity());
    }

    /** SKUs are stored trimmed and upper-cased so uniqueness is case-insensitive. */
    static String normalizeSku(String sku) {
        String trimmed = trimToNull(sku);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
