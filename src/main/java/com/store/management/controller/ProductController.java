package com.store.management.controller;

import com.store.management.dto.ApiErrorResponse;
import com.store.management.dto.ProductRequest;
import com.store.management.dto.ProductResponse;
import com.store.management.dto.StockAdjustmentRequest;
import com.store.management.dto.StockStatusResponse;
import com.store.management.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Products", description = "Product management and inventory operations")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "List products",
            description = "Returns all products. Optionally search by name (contains, case-insensitive) "
                    + "and/or filter by category (exact, case-insensitive).")
    @ApiResponse(responseCode = "200", description = "Products returned")
    public List<ProductResponse> getProducts(
            @Parameter(description = "Name fragment to search for", example = "mouse")
            @RequestParam(required = false) String name,
            @Parameter(description = "Category to filter by", example = "Electronics")
            @RequestParam(required = false) String category) {
        return productService.getProducts(name, category);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ProductResponse getProduct(@PathVariable Long id) {
        return productService.getProduct(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a product", description = "Replaces all editable fields of the product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate SKU",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stock")
    @Operation(summary = "Check available stock",
            description = "Returns the units in stock and whether at least `required` units are available.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock status returned"),
            @ApiResponse(responseCode = "400", description = "Invalid required quantity",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public StockStatusResponse checkStock(
            @PathVariable Long id,
            @Parameter(description = "Units needed", example = "10")
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "required must be at least 1") int required) {
        return productService.checkStock(id, required);
    }

    @PostMapping(value = "/{id}/stock/increase", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Increase product stock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock increased"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ProductResponse increaseStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return productService.increaseStock(id, request.quantity());
    }

    @PostMapping(value = "/{id}/stock/decrease", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Decrease product stock", description = "Fails with 409 if stock would become negative.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock decreased"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Product not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient stock",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ProductResponse decreaseStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return productService.decreaseStock(id, request.quantity());
    }
}
