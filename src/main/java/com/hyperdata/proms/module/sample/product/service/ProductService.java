package com.hyperdata.proms.module.sample.product.service;

import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.util.Lookups;
import com.hyperdata.proms.common.validation.YaviValidator;
import com.hyperdata.proms.module.sample.product.constant.ProductCacheNames;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.dto.ProductListItemResponse;
import com.hyperdata.proms.module.sample.product.dto.UpdateProductRequest;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import com.hyperdata.proms.module.sample.product.repository.spec.ProductSpecs;
import java.util.Collections;
import java.util.List;
import com.hyperdata.proms.common.security.TenantScoped;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@TenantScoped
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

    @Cacheable(cacheNames = ProductCacheNames.PRODUCT_DETAILS, key = "#id")
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toDetailResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductListItemResponse> getProducts(ProductFilter filter, Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(ProductSpecs.of(filter), pageable);
        List<Product> products = productPage.getContent();
        if (products.isEmpty()) {
            return PageResponse.from(productPage, Collections.emptyList());
        }

        List<ProductListItemResponse> items = Lookups.map(products, productMapper::toListItemResponse);

        return PageResponse.from(productPage, items);
    }

    @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_DETAILS, key = "#id")
    @Transactional
    public ProductDetailResponse updateProduct(UUID id, UpdateProductRequest request) {
        YaviValidator.validateAndThrow(UpdateProductRequest.validator, request);

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());
        if (request.active() != null) {
            product.setActive(request.active());
        }

        Product updated = productRepository.save(product);
        return productMapper.toDetailResponse(updated);
    }

    @CacheEvict(cacheNames = ProductCacheNames.PRODUCT_DETAILS, key = "#id")
    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }
}
