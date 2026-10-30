package com.hyperdata.proms.module.sample.product.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;

    @Mock private ProductMapper productMapper;

    @InjectMocks private ProductService productService;

    @Test
    @DisplayName(
            "Given valid request and new SKU, when createProduct, then successfully save and return"
                    + " detail response")
    void shouldCreateProductSuccessfully() {
        CreateProductRequest request =
                new CreateProductRequest("SKU-100", "Laptop Pro", new BigDecimal("15000000"), 10);
        Product product =
                Product.builder()
                        .id(UUID.randomUUID())
                        .sku("SKU-100")
                        .name("Laptop Pro")
                        .price(new BigDecimal("15000000"))
                        .stock(10)
                        .active(true)
                        .createdAt(Instant.now())
                        .build();
        ProductDetailResponse expected =
                new ProductDetailResponse(
                        product.getId(),
                        "SKU-100",
                        "Laptop Pro",
                        new BigDecimal("15000000"),
                        10,
                        true,
                        product.getCreatedAt());

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDetailResponse(product)).thenReturn(expected);

        ProductDetailResponse actual = productService.createProduct(request);

        assertNotNull(actual);
        assertEquals(expected.id(), actual.id());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Given existing SKU, when createProduct, then throw DuplicateResourceException")
    void shouldThrowWhenSkuExists() {
        CreateProductRequest request =
                new CreateProductRequest("SKU-DUP", "Laptop Pro", new BigDecimal("15000000"), 10);
        when(productRepository.existsBySku("SKU-DUP")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given nonexistent ID, when getProductById, then throw ResourceNotFoundException")
    void shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(id));
    }
}
