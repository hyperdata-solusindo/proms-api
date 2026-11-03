# Rule 12: OpenAPI & Scalar API Documentation Standards

> **CORE PRINCIPLE**: Seluruh REST API endpoint, Request DTO, dan Response DTO wajib terdokumentasi secara mandiri (*self-documenting*) di Scalar (`/docs`). Proyek mengadopsi **Contract Interface Pattern** (`<Feature>Api.java`) untuk memisahkan metadata dokumentasi dan rute HTTP dari logika implementasi controller. Respon error standar (`400 Bad Request` dan `500 Internal Server Error`) ditangani secara otomatis dan global oleh `OperationCustomizer`, sehingga controller/interface hanya mendokumentasikan respon sukses (`200/201`) dan error spesifik domain (seperti `401 Unauthorized` atau `404 Not Found`).

---

## 1. Quick Matrix Standar Anotasi

| Target | Wajib Ada | Atribut Kunci | Fungsi di UI Scalar |
| :--- | :--- | :--- | :--- |
| **API Contract Interface** | `@Tag` | `name`, `description` | Folder/kategori grup di sidebar navigasi. |
| **Endpoint Method** | `@Operation` | `summary` (≤7 kata), `description` | Judul endpoint pada baris URL & penjelasan detail. |
| **Domain-specific Status** | `@ApiResponses` | `responseCode`, `description`, `content` | Menampilkan skema respon sukses & error spesifik domain. |
| **Global Error Status** | *Dikelola otomatis* | Via `OpenApiConfig` `OperationCustomizer` | Respon `400` dan `500` terpasang otomatis ke seluruh endpoint. |
| **Path / Query Param** | `@Parameter` | `description`, `example`, `required` | Form input parameter pada panel *Try It*. |
| **Public Endpoint** | `@SecurityRequirements` | Kosong (`@SecurityRequirements`) | Melepas icon gembok otorisasi pada endpoint publik. |
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

### Pattern 3: Contract Interface Pattern (`<Feature>Api.java`)
> Seluruh anotasi routing Spring (`@PostMapping`, `@GetMapping`, dll.) dan OpenAPI (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirements`) ditempatkan pada **Java Interface** kontrak fitur. Gunakan penutupan kurung rapat (`)))`) untuk keterbacaan yang ringkas. Respon `400` dan `500` tidak perlu dideklarasikan manual karena sudah diinjeksi oleh global customizer.

```java
package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Authentication", description = "Manajemen autentikasi, token sesi, dan keamanan akun")
public interface AuthApi {

    @Operation(
        summary = "Login Pengguna",
        description = "Memverifikasi email dan kata sandi, lalu menerbitkan personal access token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login berhasil, token diterbitkan"),
        @ApiResponse(
            responseCode = "401",
            description = "Email atau kata sandi salah",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/auth/login")
    RestResponse<LoginResponse> login(@RequestBody LoginRequest request);

    @Operation(
        summary = "Logout Pengguna",
        description = "Mencabut personal access token sesi aktif saat ini.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Berhasil logout, sesi dinonaktifkan"),
        @ApiResponse(
            responseCode = "401",
            description = "Tidak terautentikasi atau token tidak valid",
            content = @Content(schema = @Schema(implementation = RestResponse.class)))
    })
    @PostMapping("/auth/logout")
    RestResponse<Void> logout();
}
```

### Pattern 4: Clean Controller Implementation (`<Feature>Controller.java`)
> Class controller mengimplementasikan interface kontrak, bebas dari seluruh anotasi metadata OpenAPI dan HTTP routing redundan.

```java
package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.common.security.SecurityContextHelper;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import com.hyperdata.proms.module.identity.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public RestResponse<LoginResponse> login(LoginRequest request) {
        LoginResponse response = authService.login(request);
        return RestResponse.ok(response, "Berhasil login");
    }

    @Override
    public RestResponse<Void> logout() {
        var currentUser = SecurityContextHelper.getRequiredCurrentUser();
        authService.logout(currentUser.tokenId());
        return RestResponse.ok(null, "Berhasil logout");
    }
}
```

---

## 3. Global Bearer Security & Error Response Configuration (`OpenApiConfig`)

[`OpenApiConfig.java`](../../src/main/java/com/hyperdata/proms/common/docs/OpenApiConfig.java) mengonfigurasi autentikasi Bearer JWT global dan secara otomatis melampirkan envelope error standar (`400 Bad Request` dan `500 Internal Server Error`) ke seluruh endpoint via `OperationCustomizer`:

```java
package com.hyperdata.proms.common.docs;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.hyperdata.proms.common.response.RestResponse;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
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
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .name(BEARER_AUTH)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer globalErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            var apiResponseSchema = new Schema<RestResponse<?>>()
                .description("Envelope standar respon API");

            if (!responses.containsKey("400")) {
                responses.addApiResponse(
                    "400",
                    new ApiResponse()
                        .description("Format request tidak valid / validasi YAVI gagal")
                        .content(new Content().addMediaType(
                            APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            if (!responses.containsKey("500")) {
                responses.addApiResponse(
                    "500",
                    new ApiResponse()
                        .description("Kesalahan internal server")
                        .content(new Content().addMediaType(
                            APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            return operation;
        };
    }
}
```

---

## 4. Prohibited Anti-Patterns (❌ NEVER DO)

1. ❌ **JANGAN menuliskan anotasi OpenAPI langsung di dalam `@RestController`**: Selalu deklarasikan pada interface `<Feature>Api`.
2. ❌ **JANGAN menduplikasi deklarasi respon 400 dan 500 secara manual**: Gunakan konfigurasi otomatis `OperationCustomizer` di `OpenApiConfig`.
3. ❌ **JANGAN menuliskan FQCN di dalam kode**: Selalu impor kelas secara eksplisit (misal `@ApiResponse` dari Swagger dan `RestResponse` dari common response).
4. ❌ **JANGAN menambahkan `consumes = MediaType.APPLICATION_JSON_VALUE` dan `produces = MediaType.APPLICATION_JSON_VALUE` berlebihan**: Spring Boot dan Springdoc menangani JSON secara default.
5. ❌ **JANGAN biarkan field DTO tanpa atribut `example`**: Menyebabkan Scalar hanya menampilkan placeholder kosong (`""` atau `0`) yang tidak informatif.
6. ❌ **JANGAN letakkan anotasi OpenAPI di JPA Entity**: Anotasi `@Schema` hanya milik DTO (`record`). Entity tidak boleh terekspos ke layer dokumentasi API.
7. ❌ **JANGAN gunakan `@SecurityRequirement` manual di tiap method berulang kali**: Gunakan konfigurasi global `OpenApiConfig`. Hanya gunakan `@SecurityRequirements` (kosong) saat mengecualikan endpoint publik.
8. ❌ **JANGAN buat summary `@Operation` terlalu panjang**: `summary` harus singkat (≤7 kata). Gunakan atribut `description` untuk penjelasan panjang/markdown.

---

## 5. Verification Checklist (Definition of Done)

- [ ] Interface `<Feature>Api` memiliki anotasi `@Tag(name = "...", description = "...")`.
- [ ] Controller class mengimplementasikan `<Feature>Api` dan beranotasi `@RestController`.
- [ ] Semua method endpoint di interface memiliki `@Operation(summary = "...", description = "...")`.
- [ ] Parameter URL (`@PathVariable`, `@RequestParam`) dianotasi `@Parameter(example = "...")`.
- [ ] Status HTTP sukses (`200/201`) dan error domain spesifik (`401`, `404`) didaftarkan di interface.
- [ ] Respon 400 dan 500 tidak diduplikasi secara manual di method.
- [ ] Semua record DTO Request & Response memiliki `@Schema` dengan atribut `example` realistis di setiap field.
- [ ] Endpoint publik diberi anotasi `@SecurityRequirements` agar tidak meminta token di UI Scalar.
- [ ] Tidak ada FQCN di seluruh layer controller, interface, dan DTO.
