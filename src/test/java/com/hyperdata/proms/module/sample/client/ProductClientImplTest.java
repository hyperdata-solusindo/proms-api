package com.hyperdata.proms.module.sample.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyperdata.proms.module.sample.client.dto.ProductSummaryDto;
import com.hyperdata.proms.module.sample.client.impl.ProductClientImpl;
import com.hyperdata.proms.module.sample.product.entity.Product;
import com.hyperdata.proms.module.sample.product.mapper.ProductMapper;
import com.hyperdata.proms.module.sample.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductClientImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductClientImpl productClient;

    @Test
    @DisplayName("Given existing ID, when findProductSummaryById, then return summary DTO")
    void shouldReturnSummaryWhenFound() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
            .id(id)
            .sku("SKU-1")
            .name("Keyboard")
            .price(new BigDecimal("500000"))
            .stock(5)
            .active(true)
            .createdAt(Instant.now())
            .build();
        ProductSummaryDto summaryDto = new ProductSummaryDto(id, "SKU-1", "Keyboard", new BigDecimal("500000"), true);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toSummaryDto(product)).thenReturn(summaryDto);

        Optional<ProductSummaryDto> result = productClient.findProductSummaryById(id);

        assertTrue(result.isPresent());
        assertEquals("Keyboard", result.get().name());
    }

    @Test
    @DisplayName("Given non-existent ID, when findProductSummaryById, then return empty Optional")
    void shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ProductSummaryDto> result = productClient.findProductSummaryById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Given batch IDs, when findAllByIds, then return list of mapped summaries")
    void shouldReturnSummariesForBatchIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Set<UUID> ids = Set.of(id1, id2);

        Product p1 = Product.builder()
            .id(id1)
            .sku("SKU-1")
            .name("Keyboard")
            .price(BigDecimal.TEN)
            .stock(5)
            .active(true)
            .build();
        Product p2 = Product.builder()
            .id(id2)
            .sku("SKU-2")
            .name("Mouse")
            .price(BigDecimal.ONE)
            .stock(10)
            .active(true)
            .build();

        ProductSummaryDto s1 = new ProductSummaryDto(id1, "SKU-1", "Keyboard", BigDecimal.TEN, true);
        ProductSummaryDto s2 = new ProductSummaryDto(id2, "SKU-2", "Mouse", BigDecimal.ONE, true);

        when(productRepository.findAllById(ids)).thenReturn(List.of(p1, p2));
        when(productMapper.toSummaryDto(p1)).thenReturn(s1);
        when(productMapper.toSummaryDto(p2)).thenReturn(s2);

        List<ProductSummaryDto> result = productClient.findAllByIds(ids);

        assertEquals(2, result.size());
        assertEquals("Keyboard", result.get(0).name());
        assertEquals("Mouse", result.get(1).name());
    }

    @Test
    @DisplayName("Given null or empty IDs, when findAllByIds, then return empty list without querying repository")
    void shouldReturnEmptyListWhenIdsNullOrEmpty() {
        assertTrue(productClient.findAllByIds(null).isEmpty());
        assertTrue(productClient.findAllByIds(Collections.emptyList()).isEmpty());
        verify(productRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Given active product with enough stock, when isStockAvailable, then return true")
    void shouldReturnTrueWhenStockAvailable() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
            .id(id)
            .sku("SKU-1")
            .name("Keyboard")
            .stock(10)
            .active(true)
            .build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertTrue(productClient.isStockAvailable(id, 5));
        assertTrue(productClient.isStockAvailable(id, 10));
        assertFalse(productClient.isStockAvailable(id, 11));
    }

    @Test
    @DisplayName("Given inactive product, when isStockAvailable, then return false")
    void shouldReturnFalseWhenProductInactive() {
        UUID id = UUID.randomUUID();
        Product product = Product.builder()
            .id(id)
            .sku("SKU-1")
            .name("Keyboard")
            .stock(100)
            .active(false)
            .build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertFalse(productClient.isStockAvailable(id, 1));
    }
}
