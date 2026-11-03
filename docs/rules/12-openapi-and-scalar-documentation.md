# Rule 12: OpenAPI & Scalar API Documentation Standards

> **CORE PRINCIPLE**: Seluruh REST API endpoint, Request DTO, dan Response DTO wajib terdokumentasi secara mandiri (*self-documenting*) di Scalar (`/docs`). Dokumentasi harus kaya konteks: grup yang teratur (`@Tag`), maksud aksi yang jelas (`@Operation`), media type eksplisit, skenario error lengkap (`@ApiResponses`), serta nilai contoh realistis (`@Schema(example = "...")`) agar form interaktif Scalar langsung terisi dan siap diuji.

---

## 1. Quick Matrix Standar Anotasi

| Target | Wajib Ada | Atribut Kunci | Fungsi di UI Scalar |
| :--- | :--- | :--- | :--- |
| **Controller Class** | `@Tag` | `name`, `description` | Folder/kategori grup di sidebar navigasi. |
| **Endpoint Method** | `@Operation` | `summary` (≤7 kata), `description` | Judul endpoint pada baris URL & penjelasan detail. |
| **Endpoint Status** | `@ApiResponses` | `responseCode`, `description`, `content` | Menampilkan skema balasan sukses & envelope error [Rule 06](06-api-and-error-handling.md). |
| **Content-Type** | Mapping Params | `produces`, `consumes` = `application/json` | Mencegah fallback liar `*/*`. |
| **Path / Query Param**| `@Parameter` | `description`, `example`, `required` | Form input parameter pada panel *Try It*. |
| **Public Endpoint** | `@SecurityRequirements` | *kosongkan parameter* (`@SecurityRequirements()`) | Melepas icon gembok otorisasi pada endpoint publik. |
| **DTO Record Class** | `@Schema` | `description` | Judul model pada tab Schemas/Models. |
| **DTO Record Field** | `@Schema` | `description`, `example`, `requiredMode`, `format` | Mengisi otomatis form body request & contoh response. |

---

## 2. Canonical Few-Shot Patterns

### Pattern 1: Request DTO (Schema + YAVI Validation)
> Anotasi `@Schema` diletakkan langsung pada setiap komponen record field berdampingan dengan validasi YAVI [Rule 03](03-validation-and-yavi.md).

```java
@Schema(description = "Payload permintaan login autentikasi pengguna")
public record LoginRequest(
        @Schema(
            description = "Alamat email akun terdaftar",
            example = "admin@proms.com",
            format = "email",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @Schema(
            description = "Kata sandi akun pengguna",
            example = "SecretPassword123!",
            format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED
        )
        String password
) {
    public static final Validator<LoginRequest> VALIDATOR = ValidatorBuilder.<LoginRequest>of()
            .constraint(LoginRequest::email, "email", c -> c.notBlank().email())
            .constraint(LoginRequest::password, "password", c -> c.notBlank())
            .build();
}
```

### Pattern 2: Response DTO & Enums
> Berikan `example` konkret (UUID, ISO-8601 Timestamp, enum value) agar Scalar menampilkan visual data menyerupai respon asli API.

```java
@Schema(description = "Payload data profil pengguna")
public record UserProfileResponse(
        @Schema(description = "ID unik pengguna", example = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11")
        UUID id,

        @Schema(description = "Nama lengkap pengguna", example = "Budi Santoso")
        String fullName,

        @Schema(description = "Status akun pengguna", example = "ACTIVE")
        UserStatus status,

        @Schema(description = "Waktu terakhir masuk", example = "2026-11-03T08:30:00Z")
        Instant lastLoginAt
) {}
```

### Pattern 3: Controller Endpoint (POST / Action & Public Endpoint)
> Gunakan `@SecurityRequirements()` untuk endpoint publik, dan daftarkan response code `200/201`, `400`, `401`.

```java
@Tag(name = "Authentication", description = "Manajemen autentikasi, token sesi, dan keamanan akun")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "Login Pengguna",
        description = "Memverifikasi email dan kata sandi, lalu menerbitkan JWT access token beserta ringkasan tenant aktif."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login berhasil, token diterbitkan"),
        @ApiResponse(responseCode = "400", description = "Format request tidak valid / validasi YAVI gagal",
            content = @Content(schema = @Schema(implementation = com.hyperdata.proms.common.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Email atau kata sandi salah",
            content = @Content(schema = @Schema(implementation = com.hyperdata.proms.common.response.ApiResponse.class)))
    })
    @SecurityRequirements() // Menonaktifkan kebutuhan Bearer Token untuk endpoint publik
    @PostMapping(
        value = "/auth/login",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Berhasil login");
    }
}
```

### Pattern 4: Controller Endpoint dengan Parameter (GET / Detail & Filter)
> Gunakan `@Parameter` pada `@PathVariable` dan `@RequestParam` agar kolom isian di Scalar memiliki penjelasan dan nilai default/contoh.

```java
@Operation(
    summary = "Ambil Detail Produk",
    description = "Mengambil data lengkap produk berdasarkan UUID spesifik."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Data produk ditemukan"),
    @ApiResponse(responseCode = "404", description = "Produk tidak ditemukan",
        content = @Content(schema = @Schema(implementation = com.hyperdata.proms.common.response.ApiResponse.class)))
})
@GetMapping(value = "/products/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
public ApiResponse<ProductDetailResponse> getProductById(
        @Parameter(description = "ID unik produk", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d", required = true)
        @PathVariable UUID id
) {
    return ApiResponse.ok(productService.getById(id));
}
```

---

## 3. Global Bearer Security Configuration (`OpenApiConfig`)

Untuk mengaktifkan tombol **Authorize** (Bearer JWT) di seluruh Scalar UI:

```java
package com.hyperdata.proms.common.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Proms ERP API Reference")
                        .version("1.0.0")
                        .description("Spesifikasi REST API Enterprise Proms ERP")
                        .contact(new Contact().name("Hyperdata Solusindo").email("support@hyperdata.co.id")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

---

## 4. Prohibited Anti-Patterns (❌ NEVER DO)

1. ❌ **JANGAN biarkan field DTO tanpa atribut `example`**: Menyebabkan Scalar hanya menampilkan placeholder kosong (`""` atau `0`) yang tidak informatif.
2. ❌ **JANGAN biarkan mapping menghasilkan `*/*`**: Selalu deklarasikan `produces = MediaType.APPLICATION_JSON_VALUE` (dan `consumes` untuk request body).
3. ❌ **JANGAN letakkan anotasi OpenAPI di JPA Entity**: Anotasi `@Schema` hanya milik DTO (`record`). Entity tidak boleh terekspos ke layer dokumentasi API.
4. ❌ **JANGAN gunakan `@SecurityRequirement` manual di tiap method berulang kali**: Gunakan konfigurasi global `OpenApiConfig`. Hanya gunakan `@SecurityRequirements()` (kosong) saat mengecualikan endpoint publik.
5. ❌ **JANGAN buat summary `@Operation` terlalu panjang**: `summary` harus singkat (≤7 kata). Gunakan atribut `description` untuk penjelasan panjang/markdown.

---

## 5. Verification Checklist (Definition of Done)

- [ ] Class controller memiliki anotasi `@Tag(name = "...", description = "...")`.
- [ ] Semua method endpoint memiliki `@Operation(summary = "...", description = "...")`.
- [ ] Explicit media type dideklarasikan pada `@GetMapping`, `@PostMapping`, dll.
- [ ] Parameter URL (`@PathVariable`, `@RequestParam`) dianotasi `@Parameter(example = "...")`.
- [ ] Status HTTP utama (`200/201`, `400`, `401`, `404`) didaftarkan di `@ApiResponses`.
- [ ] Semua record DTO Request & Response memiliki `@Schema` dengan atribut `example` realistis di setiap field.
- [ ] Endpoint publik diberi anotasi `@SecurityRequirements()` agar tidak meminta token di UI Scalar.
