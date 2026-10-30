package com.hyperdata.proms.module.sample.product.controller;

import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.service.ProductService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductDetailResponse> createProduct(
            @RequestBody CreateProductRequest request) {
        ProductDetailResponse response = productService.createProduct(request);
        return ApiResponse.created(response, "Product created successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDetailResponse> getProductById(@PathVariable UUID id) {
        ProductDetailResponse response = productService.getProductById(id);
        return ApiResponse.ok(response, "Product retrieved successfully");
    }
}
