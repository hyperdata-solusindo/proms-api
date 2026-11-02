package com.hyperdata.proms.module.sample.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyperdata.proms.common.exception.GlobalExceptionHandler;
import com.hyperdata.proms.common.exception.ResourceNotFoundException;
import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.dto.ProductListItemResponse;
import com.hyperdata.proms.module.sample.product.dto.UpdateProductRequest;
import com.hyperdata.proms.module.sample.product.service.ProductService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Given valid CreateProductRequest, when POST /api/v1/products, then return 201 Created")
    void shouldCreateProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest("SKU-1", "MacBook Pro", new BigDecimal("25000000"), 5);
        UUID id = UUID.randomUUID();
        ProductDetailResponse response = new ProductDetailResponse(
                id, "SKU-1", "MacBook Pro", new BigDecimal("25000000"), 5, true, Instant.now());

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.sku").value("SKU-1"));
    }

    @Test
    @DisplayName("Given existing product ID, when GET /api/v1/products/{id}, then return 200 OK")
    void shouldGetProductById() throws Exception {
        UUID id = UUID.randomUUID();
        ProductDetailResponse response = new ProductDetailResponse(
                id, "SKU-1", "MacBook Pro", new BigDecimal("25000000"), 5, true, Instant.now());

        when(productService.getProductById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro"));
    }

    @Test
    @DisplayName("Given non-existent product ID, when GET /api/v1/products/{id}, then return 404 Not Found")
    void shouldReturn404WhenProductNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.getProductById(id)).thenThrow(new ResourceNotFoundException("Product", id));

        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Given pageable and filter, when GET /api/v1/products, then return PageResponse")
    void shouldGetProductsPage() throws Exception {
        UUID id = UUID.randomUUID();
        ProductListItemResponse item =
                new ProductListItemResponse(id, "SKU-1", "MacBook Pro", new BigDecimal("25000000"), 5);
        PageResponse<ProductListItemResponse> pageResponse = new PageResponse<>(List.of(item), 0, 20, 1, 1, true, true);

        when(productService.getProducts(any(ProductFilter.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products")
                        .param("name", "MacBook")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("Given valid UpdateProductRequest, when PUT /api/v1/products/{id}, then return 200 OK")
    void shouldUpdateProduct() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateProductRequest request = new UpdateProductRequest("MacBook M3", new BigDecimal("27000000"), 8, true);
        ProductDetailResponse response = new ProductDetailResponse(
                id, "SKU-1", "MacBook M3", new BigDecimal("27000000"), 8, true, Instant.now());

        when(productService.updateProduct(eq(id), any(UpdateProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/products/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.name").value("MacBook M3"));
    }

    @Test
    @DisplayName("Given existing product ID, when DELETE /api/v1/products/{id}, then return 200 OK")
    void shouldDeleteProduct() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(productService).deleteProduct(id);

        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));
    }
}
