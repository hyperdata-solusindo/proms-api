package com.hyperdata.proms.module.sample.product.service;

import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.validation.YaviValidator;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request) {
        YaviValidator.validateAndThrow(CreateProductRequest.validator, request);

        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException(String.format("Product with SKU [%s] already exists", request.sku()));
        }

        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toDetailResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID id) {
        Product product =
                productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toDetailResponse(product);
    }
}
