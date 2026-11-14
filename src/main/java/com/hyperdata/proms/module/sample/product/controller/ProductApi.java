package com.hyperdata.proms.module.sample.product.controller;

import com.hyperdata.proms.common.response.PageResponse;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.sample.product.dto.CreateProductRequest;
import com.hyperdata.proms.module.sample.product.dto.ProductDetailResponse;
import com.hyperdata.proms.module.sample.product.dto.ProductFilter;
import com.hyperdata.proms.module.sample.product.dto.ProductListItemResponse;
import com.hyperdata.proms.module.sample.product.dto.UpdateProductRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Products", description = "Manajemen katalog dan data produk")
@RequestMapping("/api/v1/products")
public interface ProductApi {

    @Operation(
        summary = "Tambah Produk Baru",
        description = "Membuat produk baru di dalam sistem.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produk berhasil dibuat")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RestResponse<ProductDetailResponse> createProduct(@RequestBody
    CreateProductRequest request);

    @Operation(
        summary = "Ambil Detail Produk",
        description = "Mengambil data lengkap produk berdasarkan UUID spesifik.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data produk ditemukan"),
        @ApiResponse(
            responseCode = "404",
            description = "Produk tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @GetMapping("/{id}")
    RestResponse<ProductDetailResponse> getProductById(
        @Parameter(description = "ID unik produk", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable
        UUID id);

    @Operation(
        summary = "Daftar Produk",
        description = "Mengambil daftar produk dengan pagination dan filter.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Daftar produk berhasil diambil")
    })
    @GetMapping
    RestResponse<PageResponse<ProductListItemResponse>> getProducts(
        ProductFilter filter,
        @PageableDefault(size = 20)
        Pageable pageable);

    @Operation(
        summary = "Perbarui Produk",
        description = "Memperbarui data produk berdasarkan ID spesifik.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produk berhasil diperbarui"),
        @ApiResponse(
            responseCode = "404",
            description = "Produk tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @PutMapping("/{id}")
    RestResponse<ProductDetailResponse> updateProduct(
        @Parameter(description = "ID unik produk", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable
        UUID id,
        @RequestBody
        UpdateProductRequest request);

    @Operation(
        summary = "Hapus Produk",
        description = "Menghapus produk dari sistem berdasarkan ID spesifik.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produk berhasil dihapus"),
        @ApiResponse(
            responseCode = "404",
            description = "Produk tidak ditemukan",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @DeleteMapping("/{id}")
    RestResponse<Void> deleteProduct(
        @Parameter(description = "ID unik produk", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable
        UUID id);
}
