package com.hyperdata.proms.module.sample.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.hyperdata.proms.common.config.CacheConfig;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.UpdateProductRequest;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {ProductServiceCacheIntegrationTest.TestConfig.class})
class ProductServiceCacheIntegrationTest {

    @Configuration
    @Import(CacheConfig.class)
    static class TestConfig {

        @Bean
        public ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        public ProductMapper productMapper() {
            return Mappers.getMapper(ProductMapper.class);
        }

        @Bean
        public ProductService productService(ProductRepository productRepository, ProductMapper productMapper) {
            return new ProductService(productRepository, productMapper);
        }
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("Given existing product, when getProductById called twice, then repository is queried only once")
    void givenExistingProduct_whenGetProductByIdCalledTwice_thenRepositoryQueriedOnlyOnce() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
            .id(productId)
            .sku("SKU-CACHE-1")
            .name("Cached Product")
            .price(new BigDecimal("250000"))
            .stock(50)
            .active(true)
            .createdAt(Instant.now())
            .build();

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        // First call - cache miss, queries DB
        ProductDetailResponse response1 = productService.getProductById(productId);
        assertThat(response1.name()).isEqualTo("Cached Product");

        // Second call - cache hit, served from L1 cache
        ProductDetailResponse response2 = productService.getProductById(productId);
        assertThat(response2.name()).isEqualTo("Cached Product");

        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    @DisplayName("Given cached product, when updateProduct called, then cache is evicted")
    void givenCachedProduct_whenUpdateProductCalled_thenCacheIsEvicted() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
            .id(productId)
            .sku("SKU-CACHE-2")
            .name("Old Product Name")
            .price(new BigDecimal("100000"))
            .stock(10)
            .active(true)
            .createdAt(Instant.now())
            .build();

        given(productRepository.findById(productId)).willReturn(Optional.of(product));
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

        // First call populates cache
        productService.getProductById(productId);

        // Update product should trigger @CacheEvict
        UpdateProductRequest updateRequest = new UpdateProductRequest(
            "New Product Name", new BigDecimal("120000"), 15, true);
        productService.updateProduct(productId, updateRequest);

        // Next getProductById should query repository again because cache was evicted
        productService.getProductById(productId);

        // findById was called: 1st for getProduct, 2nd for updateProduct, 3rd for getProduct post-eviction
        verify(productRepository, times(3)).findById(productId);
    }
}
