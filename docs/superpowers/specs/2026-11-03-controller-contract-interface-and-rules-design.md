# Technical Design Specification: Controller Contract Interface Pattern & Architecture Rules Standardization

- **Date**: 2026-11-03
- **Topic**: Controller Contract Interface Pattern (`<Feature>Api`), Automated Global OpenAPI Error Responses, Strict No-FQCN Rule, and Red Hat Java / Eclipse JDT Formatter Alignment
- **Target Repository**: `hyperdata-solusindo/proms-api-java`
- **Status**: Approved Architecture Specification (Ready for Implementation)

---

## 1. Executive Summary & Context

Saat ini, implementasi controller Spring Boot dan dokumentasi OpenAPI (`springdoc-openapi`) menghadapi beberapa isu arsitektural di level enterprise:

1. **Polusi Anotasi (*Annotation Overload*)**: 
   Anotasi metadata dokumentasi (`@Tag`, `@Operation`, `@ApiResponses`, `@ApiResponse`, `@Content`, `@Schema`, `@SecurityRequirements`) mendominasi file controller, mengaburkan logika bisnis delegasi service.
2. **Duplikasi Response Error Standar**:
   Response error umum seperti `400 Bad Request` (validasi input YAVI) dan `500 Internal Server Error` harus diketik ulang secara manual di setiap method controller.
3. **Polusi FQCN (*Fully Qualified Class Name*)**:
   Penggunaan penulisan kelas panjang seperti `com.hyperdata.proms.common.response.ApiResponse.class` alih-alih me-`import` kelas memicu peringatan IDE dan menurunkan keterbacaan kode (*readability*).
4. **Kesenjangan Dokumentasi Formatter**:
   Dokumen master [Rule 09](file:///home/kholifan/html/project-promps/api/docs/rules/09-tooling-and-quality.md) masih mencatat *Palantir Java Format*, padahal Spotless telah resmi dimigrasikan ke **Eclipse JDT Formatter Engine** ([`.vscode/eclipse-java-formatter.xml`](file:///home/kholifan/html/project-promps/api/.vscode/eclipse-java-formatter.xml)) agar selaras 100% dengan ekstensi **Red Hat Java** (`redhat.java`).

Spesifikasi teknis ini menetapkan standardisasi arsitektur komprehensif untuk menyelesaikan masalah tersebut.

---

## 2. Key Architectural Decisions (ADR)

### ADR 1: Penerapan Contract Interface Pattern (`<Feature>Api.java`)
- **Keputusan**: Seluruh deklarasi rute HTTP (`@GetMapping`, `@PostMapping`, dll.) dan seluruh metadata dokumentasi OpenAPI (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`) dipindahkan ke sebuah **Java Interface** bernama **`<Feature>Api.java`**.
- **Lokasi File**: Disimpan langsung di dalam package yang sama dengan controller (`com.hyperdata.proms.module.<domain>.<feature>.controller`).
- **Tanggung Jawab Class Controller**: Class `<Feature>Controller.java` bertindak murni sebagai kelas implementasi (`implements <Feature>Api`) yang terbebas dari seluruh anotasi OpenAPI. Controller hanya memiliki `@RestController`, injeksi dependensi via `@RequiredArgsConstructor`, dan pemanggilan service bisnis.

### ADR 2: Global `OperationCustomizer` untuk Respon Error Standar
- **Keputusan**: Menambahkan `@Bean OperationCustomizer` pada [`OpenApiConfig.java`](file:///home/kholifan/html/project-promps/api/src/main/java/com/hyperdata/proms/common/docs/OpenApiConfig.java).
- **Fungsi**: Secara otomatis mendaftarkan envelope error standar (`400 Bad Request` & `500 Internal Server Error`) dengan skema `ApiResponse<?>` ke seluruh endpoint yang dideteksi oleh Springdoc OpenAPI.
- **Dampak**: Developer tidak perlu lagi mendeklarasikan error 400 dan 500 di level controller, kecuali untuk error spesifik domain (seperti `401 Unauthorized` atau `404 Not Found`).

### ADR 3: Universal Strict No-FQCN / Explicit Imports Rule
- **Keputusan**: Dilarang keras menuliskan FQCN di seluruh codebase Java (Entity, DTO, Service, Facade, Controller, Mapper, Repository, dan Test).
- **Pengecualian**: Hanya diizinkan jika terjadi *class name collision* dari dua package berbeda dalam file yang sama (misal `java.util.Date` vs `java.sql.Date`). Di luar kondisi tersebut, seluruh kelas wajib di-import di header file.
- **Formalisasi**: Diresmikan ke dalam [Rule 02: Coding Standards](file:///home/kholifan/html/project-promps/api/docs/rules/02-coding-standards.md).

### ADR 4: Quality Gate & Formatter Synchronization
- **Keputusan**: Menyelaraskan seluruh dokumen quality gate dengan profil Eclipse JDT Formatter ([`.vscode/eclipse-java-formatter.xml`](file:///home/kholifan/html/project-promps/api/.vscode/eclipse-java-formatter.xml)) dengan aturan:
  - Batas lebar baris: **120 karakter**.
  - Indentasi dasar: **4 spasi**.
  - Continuation indent: **1 level (+4 spasi)**.
  - Bracket style: Egyptian Braces / 1TBS (`end_of_line`).
- **ArchUnit Enforcement**: Menambahkan tes arsitektur pada [`ArchitectureTest.java`](file:///home/kholifan/html/project-promps/api/src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java) untuk memastikan seluruh `@RestController` mengimplementasikan interface kontraknya.

---

## 3. Package Structure & Blueprint

Pada setiap modul, folder `controller/` akan memiliki sepasang file:

```text
com.hyperdata.proms.module.<domain>.<feature>.controller/
├── <Feature>Api.java           # Kontrak HTTP Routing & Dokumentasi OpenAPI / Scalar
└── <Feature>Controller.java    # Implementasi Bersih Spring REST Controller
```

### Visual Pola:

```java
// ============================================================================
// 1. KONTRAK INTERFACE: AuthApi.java
// ============================================================================
package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.ApiResponse;
import com.hyperdata.proms.module.identity.auth.dto.LoginRequest;
import com.hyperdata.proms.module.identity.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerResponse;
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
        @SwaggerResponse(responseCode = "200", description = "Login berhasil, token diterbitkan"),
        @SwaggerResponse(
            responseCode = "401",
            description = "Email atau kata sandi salah",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/auth/login")
    ApiResponse<LoginResponse> login(@RequestBody LoginRequest request);

    @Operation(
        summary = "Logout Pengguna",
        description = "Mencabut token sesi pengguna yang sedang aktif.")
    @SwaggerResponse(responseCode = "200", description = "Logout berhasil, token dicabut")
    @PostMapping("/auth/logout")
    ApiResponse<Void> logout();
}

// ============================================================================
// 2. IMPLEMENTASI BERSIH: AuthController.java
// ============================================================================
package com.hyperdata.proms.module.identity.auth.controller;

import com.hyperdata.proms.common.response.ApiResponse;
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
    public ApiResponse<LoginResponse> login(LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ApiResponse.ok(response, "Berhasil login");
    }

    @Override
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok(null, "Berhasil logout");
    }
}
```

---

## 4. Technical Design: Global OpenAPI Error Customizer

Di dalam [`OpenApiConfig.java`](file:///home/kholifan/html/project-promps/api/src/main/java/com/hyperdata/proms/common/docs/OpenApiConfig.java), ditambahkan bean `OperationCustomizer`:

```java
    @Bean
    public OperationCustomizer globalErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            var responses = operation.getResponses();
            if (responses == null) {
                responses = new io.swagger.v3.oas.models.responses.ApiResponses();
                operation.setResponses(responses);
            }

            var apiResponseSchema = new Schema<ApiResponse<?>>()
                .description("Envelope standar respon API");

            if (!responses.containsKey("400")) {
                responses.addApiResponse(
                    "400",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Format request tidak valid / validasi YAVI gagal")
                        .content(new Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            if (!responses.containsKey("500")) {
                responses.addApiResponse(
                    "500",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Kesalahan internal server")
                        .content(new Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            new MediaType().schema(apiResponseSchema))));
            }

            return operation;
        };
    }
```

---

## 5. ArchUnit Architectural Guardrails

Pada [`ArchitectureTest.java`](file:///home/kholifan/html/project-promps/api/src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java), guardrail disesuaikan dan diperluas:

```java
    // 1. Controller classes wajib beranotasi @RestController (abaikan interface)
    @ArchTest
    static final ArchRule controllers_must_be_annotated_with_rest_controller = classes().that()
        .resideInAPackage("..controller..")
        .and().areNotInterfaces()
        .should()
        .beAnnotatedWith(RestController.class);

    // 2. Controller classes wajib mengimplementasikan interface kontraknya
    @ArchTest
    static final ArchRule controllers_must_implement_api_interface = classes().that()
        .resideInAPackage("com.hyperdata.proms.module..controller..")
        .and().areNotInterfaces()
        .should()
        .implement(resideInAPackage("com.hyperdata.proms.module..controller.."));
```

---

## 6. Migration Matrix

| Target File | Perubahan Teknis |
| :--- | :--- |
| `docs/rules/01-architecture-and-structure.md` | Tambahkan `<Feature>Api.java` ke folder diagram dan aturan boundary implementasi interface. |
| `docs/rules/02-coding-standards.md` | Tambahkan Section 6: Strict No-FQCN Rule. |
| `docs/rules/09-tooling-and-quality.md` | Perbarui ke Eclipse JDT Formatter Engine & Red Hat Java. |
| `docs/rules/12-openapi-and-scalar-documentation.md` | Terapkan Contract Interface Pattern, kurung rapat `)))`, dan Global Customizer. |
| `OpenApiConfig.java` | Tambahkan `OperationCustomizer` untuk respon 400 & 500. |
| `ArchitectureTest.java` | Update rules ArchUnit untuk interface controller. |
| `AuthApi.java` (Baru) & `AuthController.java` | Migrasi kontrak auth & bersihkan controller. |
| `UserApi.java` (Baru) & `UserController.java` | Migrasi kontrak `/me` & bersihkan controller. |
| `ProductApi.java` (Baru) & `ProductController.java` | Migrasi kontrak produk & bersihkan controller. |

---

## 7. Verification & Quality Acceptance

- **Kompilasi**: `./gradlew compileJava testClasses` (Lulus tanpa error/warning FQCN).
- **ArchUnit**: `./gradlew test --tests "*ArchitectureTest*"` (Lulus 100%).
- **Spotless Formatting**: `./gradlew spotlessCheck` (Lulus 100% menggunakan profil `PromsJavaFormat`).
- **API Runtime Compatibility**: Endpoint `/auth/login`, `/auth/logout`, `/me`, dan `/docs` beroperasi dengan spesifikasi yang identik.
