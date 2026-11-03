package com.hyperdata.proms.module.sample.product.controller;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
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
    public RestResponse<ProductDetailResponse> createProduct(CreateProductRequest request) {
        ProductDetailResponse response = productService.createProduct(request);
        return RestResponse.created(response, "Product created successfully");
    }

    @Override
    public RestResponse<ProductDetailResponse> getProductById(UUID id) {
        ProductDetailResponse response = productService.getProductById(id);
        return RestResponse.ok(response, "Product retrieved successfully");
    }

    @Override
    public PageResponse<ProductListItemResponse> getProducts(
        ProductFilter filter,
        Pageable pageable) {
        return productService.getProducts(filter, pageable);
    }

    @Override
    public RestResponse<ProductDetailResponse> updateProduct(
        UUID id,
        UpdateProductRequest request) {
        ProductDetailResponse response = productService.updateProduct(id, request);
        return RestResponse.ok(response, "Product updated successfully");
    }

    @Override
    public RestResponse<Void> deleteProduct(UUID id) {
        productService.deleteProduct(id);
        return RestResponse.ok(null, "Product deleted successfully");
    }
}
