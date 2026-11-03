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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductService productService;

    @Override
    public ApiResponse<ProductDetailResponse> createProduct(CreateProductRequest request) {
        ProductDetailResponse response = productService.createProduct(request);
        return ApiResponse.created(response, "Product created successfully");
    }

    @Override
    public ApiResponse<ProductDetailResponse> getProductById(UUID id) {
        ProductDetailResponse response = productService.getProductById(id);
        return ApiResponse.ok(response, "Product retrieved successfully");
    }

    @Override
    public PageResponse<ProductListItemResponse> getProducts(
        ProductFilter filter,
        Pageable pageable) {
        return productService.getProducts(filter, pageable);
    }

    @Override
    public ApiResponse<ProductDetailResponse> updateProduct(
        UUID id,
        UpdateProductRequest request) {
        ProductDetailResponse response = productService.updateProduct(id, request);
        return ApiResponse.ok(response, "Product updated successfully");
    }

    @Override
    public ApiResponse<Void> deleteProduct(UUID id) {
        productService.deleteProduct(id);
        return ApiResponse.ok(null, "Product deleted successfully");
    }
}
