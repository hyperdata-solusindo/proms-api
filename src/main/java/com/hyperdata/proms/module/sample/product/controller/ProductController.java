package com.hyperdata.proms.module.sample.product.controller;

import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.dto.ProductListItemResponse;
import com.hyperdata.proms.module.sample.product.dto.UpdateProductRequest;
import com.hyperdata.proms.module.sample.product.service.ProductService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDetailResponse> createProduct(@RequestBody
    CreateProductRequest request) {
        ProductDetailResponse response = productService.createProduct(request);
        return ApiResponse.created(response, "Product created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProductById(@PathVariable
    UUID id) {
        ProductDetailResponse response = productService.getProductById(id);
        return ApiResponse.ok(response, "Product retrieved successfully");
    }

    @GetMapping
    public PageResponse<ProductListItemResponse> getProducts(
        ProductFilter filter, @PageableDefault(size = 20)
        Pageable pageable) {
        return productService.getProducts(filter, pageable);
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDetailResponse> updateProduct(
        @PathVariable
        UUID id, @RequestBody
        UpdateProductRequest request) {
        ProductDetailResponse response = productService.updateProduct(id, request);
        return ApiResponse.ok(response, "Product updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable
    UUID id) {
        productService.deleteProduct(id);
        return ApiResponse.ok(null, "Product deleted successfully");
    }
}
