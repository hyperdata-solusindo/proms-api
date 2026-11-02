package com.hyperdata.proms.module.sample.product.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hyperdata.proms.common.exception.DuplicateResourceException;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.dto.ProductListItemResponse;
import com.hyperdata.proms.module.sample.product.dto.UpdateProductRequest;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName(
            "Given valid request and new SKU, when createProduct, then successfully save and return detail response")
    void shouldCreateProductSuccessfully() {
        CreateProductRequest request =
                new CreateProductRequest("SKU-100", "Laptop Pro", new BigDecimal("15000000"), 10);
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .sku("SKU-100")
                .name("Laptop Pro")
                .price(new BigDecimal("15000000"))
                .stock(10)
                .active(true)
                .createdAt(Instant.now())
                .build();
        ProductDetailResponse expected = new ProductDetailResponse(
                product.getId(), "SKU-100", "Laptop Pro", new BigDecimal("15000000"), 10, true, product.getCreatedAt());

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
    @DisplayName("Given existing ID, when getProductById, then return detail response")
    void shouldReturnProductWhenFound() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
                .id(id)
                .sku("SKU-100")
                .name("Laptop Pro")
                .price(new BigDecimal("15000000"))
                .stock(10)
                .active(true)
                .createdAt(Instant.now())
                .build();
        ProductDetailResponse expected = new ProductDetailResponse(
                id, "SKU-100", "Laptop Pro", new BigDecimal("15000000"), 10, true, product.getCreatedAt());

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(expected);

        ProductDetailResponse actual = productService.getProductById(id);

        assertNotNull(actual);
        assertEquals(id, actual.id());
    }

    @Test
    @DisplayName("Given nonexistent ID, when getProductById, then throw ResourceNotFoundException")
    void shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(id));
    }

    @Test
    @DisplayName("Given filter and pageable, when getProducts has matching results, then return paginated response")
    void shouldReturnPaginatedProducts() {
        ProductFilter filter = new ProductFilter("Laptop", null, true);
        Pageable pageable = PageRequest.of(0, 10);

        Product product = Product.builder()
                .id(UUID.randomUUID())
                .sku("SKU-100")
                .name("Laptop Pro")
                .price(new BigDecimal("15000000"))
                .stock(10)
                .active(true)
                .createdAt(Instant.now())
                .build();
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        ProductListItemResponse itemResponse =
                new ProductListItemResponse(product.getId(), "SKU-100", "Laptop Pro", new BigDecimal("15000000"), 10);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(productMapper.toListItemResponse(product)).thenReturn(itemResponse);

        PageResponse<ProductListItemResponse> actual = productService.getProducts(filter, pageable);

        assertNotNull(actual);
        assertEquals(1, actual.totalElements());
        assertEquals(1, actual.items().size());
        assertEquals("Laptop Pro", actual.items().get(0).name());
    }

    @Test
    @DisplayName("Given empty database, when getProducts, then return empty PageResponse")
    void shouldReturnEmptyPageWhenNoProducts() {
        ProductFilter filter = new ProductFilter(null, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        PageResponse<ProductListItemResponse> actual = productService.getProducts(filter, pageable);

        assertNotNull(actual);
        assertEquals(0, actual.totalElements());
        assertTrue(actual.items().isEmpty());
    }

    @Test
    @DisplayName("Given existing ID and valid update request, when updateProduct, then update and return detail")
    void shouldUpdateProductSuccessfully() {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request =
                new UpdateProductRequest("Laptop Pro 2026", new BigDecimal("16000000"), 15, true);
        Product product = Product.builder()
                .id(id)
                .sku("SKU-100")
                .name("Laptop Pro")
                .price(new BigDecimal("15000000"))
                .stock(10)
                .active(true)
                .createdAt(Instant.now())
                .build();
        ProductDetailResponse expected = new ProductDetailResponse(
                id, "SKU-100", "Laptop Pro 2026", new BigDecimal("16000000"), 15, true, product.getCreatedAt());

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDetailResponse(product)).thenReturn(expected);

        ProductDetailResponse actual = productService.updateProduct(id, request);

        assertNotNull(actual);
        assertEquals("Laptop Pro 2026", actual.name());
        assertEquals(new BigDecimal("16000000"), actual.price());
        assertEquals(15, actual.stock());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Given nonexistent ID, when updateProduct, then throw ResourceNotFoundException")
    void shouldThrowWhenUpdateNotFound() {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request =
                new UpdateProductRequest("Laptop Pro 2026", new BigDecimal("16000000"), 15, true);
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(id, request));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Given existing ID, when deleteProduct, then delete successfully")
    void shouldDeleteProductSuccessfully() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        assertDoesNotThrow(() -> productService.deleteProduct(id));
        verify(productRepository).deleteById(id);
    }

    @Test
    @DisplayName("Given nonexistent ID, when deleteProduct, then throw ResourceNotFoundException")
    void shouldThrowWhenDeleteNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(id));
        verify(productRepository, never()).deleteById(any());
    }
}
