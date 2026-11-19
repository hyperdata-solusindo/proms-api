# Rule 12: OpenAPI & Scalar API Documentation Standards

> **CORE PRINCIPLE**: Seluruh REST API endpoint, Request DTO, dan Response DTO wajib terdokumentasi secara mandiri (*self-documenting*) di Scalar (`/docs`). Proyek mengadopsi **Contract Interface Pattern** (`<Feature>Api.java`) untuk memisahkan metadata dokumentasi dan rute HTTP dari logika implementasi controller. Respon error standar (`400 Bad Request` dan `500 Internal Server Error`) ditangani secara otomatis dan global oleh `OperationCustomizer`, sehingga controller/interface hanya mendokumentasikan respon sukses (`200/201`) dan error spesifik domain (seperti `401 Unauthorized` atau `404 Not Found`).

---

## 1. Quick Matrix Standar Anotasi

| Target | Wajib Ada | Atribut Kunci | Fungsi di UI Scalar |
| :--- | :--- | :--- | :--- |
| **API Contract Interface** | `@Tag` | `name`, `description` | Sub-fitur di dalam folder grup domain (`x-tagGroups`). |
| **Internal / Utility Controller** | `@Hidden` | - | Menyembunyikan controller teknis dari daftar API. |
| **Endpoint Method** | `@Operation` | `summary` (≤7 kata), `description` | Judul endpoint pada baris URL & penjelasan detail. |
| **Concurrency Mutating Endpoint** | `@ApiConcurrencyControl` | - | Otomatis menyuntikkan header `If-Match` required dan respons error `409` (`STALE_VERSION`) & `428` (`IF_MATCH_REQUIRED`). |
| **Not Found Status** | `@ApiResponseNotFound` | - | Otomatis mendokumentasikan respons error `404 Not Found` standar. |
| **Domain-specific Status** | `@ApiResponses` | `responseCode`, `description`, `content` | Menampilkan skema respon sukses & error spesifik domain. |
| **Global Error Status** | *Dikelola otomatis* | Via `OpenApiConfig` `OperationCustomizer` | Respon `400` dan `500` terpasang otomatis ke seluruh endpoint. |
| **Domain Grouping** | *Dikelola otomatis* | Via `OpenApiConfig` (`x-tagGroups`) | Folder kategori domain 1:1 mengikuti package `<domain>`. |
| **Path / Query Param** | `@Parameter` | `description`, `example`, `required` | Form input parameter pada panel *Try It*. |
| **Public Endpoint** | `@SecurityRequirements` | Kosong (`@SecurityRequirements`) | Melepas icon gembok otorisasi pada endpoint publik. |
| **DTO Record Class** | `@Schema` | `description` | Judul model pada tab Schemas/Models. |
| **DTO Record Field** | `@Schema` | `description`, `example`, `requiredMode`, `format` | Mengisi otomatis form body request & contoh response. |
| **DTO Version Field** | `@VersionEpochSchema` | - | Mengubah tampilan field `Instant version` di Scalar menjadi `integer · int64` dengan contoh Epoch Millis `1731301234000`. |

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
    public static final Validator<LoginRequest> validator = ValidatorBuilder.<LoginRequest>of()
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
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RestResponse.class),
                examples = @ExampleObject(
                    name = "InvalidCredentials",
                    summary = "Email atau kata sandi salah",
                    value = """
                        {
                          "statusCode": 401,
                          "message": "Email atau kata sandi tidak sesuai",
                          "data": null,
                          "error": {
                            "code": "UNAUTHENTICATED",
                            "detail": "Pastikan email dan kata sandi yang Anda masukkan sudah benar."
                          }
                        }
                        """
                )))
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

### Pattern 5: Concurrency Protected Mutating Endpoint (`@ApiConcurrencyControl`)
> Seluruh operasi mutasi resource yang dilindungi optimistik locking **WAJIB** dianotasikan dengan `@ApiConcurrencyControl` dan `@ApiResponseNotFound` (jika resource dicari berdasarkan ID). Hindari menulis manual puluhan baris `@Parameter(name = "If-Match", ...)` dan `@ApiResponses`.

```java
package com.hyperdata.proms.module.pm.board.controller;

import com.hyperdata.proms.common.docs.annotation.ApiConcurrencyControl;
import com.hyperdata.proms.common.docs.annotation.ApiResponseNotFound;
import com.hyperdata.proms.common.response.RestResponse;
import com.hyperdata.proms.module.pm.board.dto.BoardDetailResponse;
import com.hyperdata.proms.module.pm.board.dto.UpdateBoardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface BoardApi {

    @Operation(
        summary = "Update Informasi Board",
        description = "Memperbarui nama dan deskripsi board dengan verifikasi concurrency optimistik melalui header If-Match.")
    @ApiResponse(responseCode = "200", description = "Board berhasil diperbarui")
    @ApiResponseNotFound
    @ApiConcurrencyControl
    @PutMapping("/{boardId}")
    RestResponse<BoardDetailResponse> updateBoard(
        @Parameter(description = "UUID Board", required = true)
        @PathVariable("boardId") UUID boardId,
        @RequestBody UpdateBoardRequest request);
}
```

### Pattern 6: Concurrency Version Field in Response DTO (`@VersionEpochSchema`)
> Gunakan `@VersionEpochSchema` pada field record `Instant version` agar Scalar menampilkan tipe `integer · int64` dan contoh format Epoch Milliseconds (`1731301234000`), menimpa teks generik default RFC 3339.

```java
package com.hyperdata.proms.module.pm.board.dto;

import com.hyperdata.proms.common.docs.annotation.VersionEpochSchema;
import java.time.Instant;
import java.util.UUID;

public record BoardDetailResponse(
    UUID id,
    String key,
    String name,
    String status,
    @VersionEpochSchema
    Instant version
) {}
```

### DOs and DONTs Dokumentasi OpenAPI / Scalar:
- ✅ **DO**: Selalu gunakan meta-anotasi `@ApiConcurrencyControl` pada endpoint mutasi (`PUT`/`PATCH`) untuk mendokumentasikan header `If-Match` dan status `409` (`STALE_VERSION`) serta `428` (`IF_MATCH_REQUIRED`).
- ✅ **DO**: Selalu gunakan `@ApiResponseNotFound` pada endpoint yang mengambil atau mengubah resource by ID untuk mereduksi boilerplate deklarasi 404.
- ✅ **DO**: Selalu gunakan `@VersionEpochSchema` pada record component bertipe `Instant version` di Response DTO.
- ❌ **DON'T**: Jangan mendeklarasikan `@Parameter(name = "If-Match", ...)` dan `@ApiResponse(...)` secara manual/berulang pada interface method jika sudah ada `@ApiConcurrencyControl`.
- ❌ **DON'T**: Jangan menambahkan `@ApiResponse` untuk error `400 Bad Request`, `422 Unprocessable Entity`, atau `500 Internal Server Error` di interface method karena ketiga status ini sudah diinjeksi secara global oleh `OpenApiConfig`.
- ❌ **DON'T**: Jangan menaruh anotasi OpenAPI di class Controller (`BoardController`); seluruh metadata dokumentasi wajib berada di Contract Interface (`BoardApi`).

---

## 3. Global Bearer Security & Error Response Configuration (`OpenApiConfig`)

[`OpenApiConfig.java`](../../src/main/java/com/hyperdata/proms/common/docs/OpenApiConfig.java) mengonfigurasi skema autentikasi Bearer Personal Access Token (PAT `<tokenId>|<plainToken>`) global dan secara otomatis melampirkan envelope error standar (`400 Bad Request`, `422 Unprocessable Entity`, dan `500 Internal Server Error`) ke seluruh endpoint via `OperationCustomizer`:

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
                var badRequestMedia = new MediaType().schema(apiResponseSchema);
                badRequestMedia.addExamples("BadRequestExample", new Example()
                    .summary("Format payload JSON tidak valid")
                    .value("""
                        {
                          "statusCode": 400,
                          "message": "Format data tidak valid",
                          "data": null,
                          "error": {
                            "code": "BAD_REQUEST",
                            "detail": "Format payload request JSON tidak valid atau rusak"
                          }
                        }
                        """));
                responses.addApiResponse("400", new ApiResponse()
                    .description("Format request tidak valid (Malformed JSON)")
                    .content(new Content().addMediaType(APPLICATION_JSON_VALUE, badRequestMedia)));
            }

            if (!responses.containsKey("422")) {
                var validationMedia = new MediaType().schema(apiResponseSchema);
                validationMedia.addExamples("ValidationExample", new Example()
                    .summary("Validasi gagal (YAVI / Field validation)")
                    .value("""
                        {
                          "statusCode": 422,
                          "message": "Validation failed",
                          "data": null,
                          "error": {
                            "code": "VALIDATION",
                            "detail": "Periksa kembali field yang diisi.",
                            "fields": [
                              { "field": "email", "message": "Format email tidak valid" }
                            ]
                          }
                        }
                        """));
                responses.addApiResponse("422", new ApiResponse()
                    .description("Kesalahan validasi atau aturan bisnis (Unprocessable Entity)")
                    .content(new Content().addMediaType(APPLICATION_JSON_VALUE, validationMedia)));
            }

            if (!responses.containsKey("500")) {
                var serverErrorMedia = new MediaType().schema(apiResponseSchema);
                serverErrorMedia.addExamples("InternalServerErrorExample", new Example()
                    .summary("Kesalahan internal server")
                    .value("""
                        {
                          "statusCode": 500,
                          "message": "An unexpected internal server error occurred",
                          "data": null,
                          "error": {
                            "code": "INTERNAL_SERVER_ERROR",
                            "detail": "Terjadi kesalahan internal pada server"
                          },
                          "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
                        }
                        """));
                responses.addApiResponse("500", new ApiResponse()
                    .description("Kesalahan internal server")
                    .content(new Content().addMediaType(APPLICATION_JSON_VALUE, serverErrorMedia)));
            }

            return operation;
        };
    }
}
```

### Domain Tag Groups (`x-tagGroups`)
Untuk memastikan sidebar Scalar UI merefleksikan arsitektur modular **`<domain>.<feature>`** secara 1:1, `OpenApiConfig` menyusun ekstensi `x-tagGroups` di root OpenAPI:
- Setiap controller di `com.hyperdata.proms.module.<domain>.<feature>` dikelompokkan ke grup **`<domain>`** (misal: *Identity*, *Sample*, *Platform*).
- Tag fitur (misal: *Authentication*, *User Profile*, *Products*) menjadi sub-item yang dapat di-expand/collapse di dalam domain tersebut.

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
9. ❌ **JANGAN biarkan controller internal / non-bisnis terekspos tanpa `@Hidden`**: Selalu berikan anotasi `@Hidden` pada controller sistem atau internal docs (misal `ScalarDocController`) agar tidak mencemari sidebar dokumentasi API.
10. ❌ **JANGAN membuat endpoint di luar hierarki modular `module.<domain>.<feature>`**: Semua endpoint bisnis wajib mematuhi package convention agar otomatis terpetakan ke dalam domain tag group Scalar.

---

## 5. Verification Checklist (Definition of Done)

- [ ] Interface `<Feature>Api` memiliki anotasi `@Tag(name = "...", description = "...")`.
- [ ] Controller class mengimplementasikan `<Feature>Api` dan beranotasi `@RestController`.
- [ ] Controller internal / utilitas teknis memiliki anotasi `@Hidden`.
- [ ] Semua method endpoint di interface memiliki `@Operation(summary = "...", description = "...")`.
- [ ] Parameter URL (`@PathVariable`, `@RequestParam`) dianotasi `@Parameter(example = "...")`.
- [ ] Status HTTP sukses (`200/201`) dan error domain spesifik (`401`, `404`) didaftarkan di interface.
- [ ] Respon 400 dan 500 tidak diduplikasi secara manual di method.
- [ ] Semua record DTO Request & Response memiliki `@Schema` dengan atribut `example` realistis di setiap field.
- [ ] Endpoint publik diberi anotasi `@SecurityRequirements` agar tidak meminta token di UI Scalar.
- [ ] Sidebar Scalar mengelompokkan API per domain secara 1:1 via `x-tagGroups`.
- [ ] Tidak ada FQCN di seluruh layer controller, interface, dan DTO.
