# Rule 01: Architecture & Structure (Package-Enforced Modular Monolith)

## 1. Package Layout

The project follows a **Package-Enforced Modular Monolith** architecture:
`com.hyperdata.proms.module.<domain>`

Example:
- Domain: `inventory` -> `com.hyperdata.proms.module.inventory`
- Domain: `identity` -> `com.hyperdata.proms.module.identity`
- Domain: `pm` -> `com.hyperdata.proms.module.pm`
- Domain: `platform` -> `com.hyperdata.proms.module.platform`

Inside each `<domain>` directory, strictly maintain this standard structure:
```text
com.hyperdata.proms.module.<domain>/
├── client/                                    # 🌐 Public SDK / Etalase Publik
│   ├── contract/                              # 📜 Interface Kontrak (satu-satunya yang di-import modul luar)
│   │   ├── <Feature>Client.java               # Public Interface
│   │   └── <Feature2>Client.java
│   ├── dto/                                   # 📦 Public Data Contract Records (Immutable)
│   │   ├── <Feature>Response.java             # Java 21 Record
│   │   ├── <Feature>BatchLookupRequest.java   # Batch Request Record
│   │   └── <Feature>SummaryDto.java
│   └── impl/                                  # 🔒 In-Memory Implementation (Privat untuk domain ini)
│       ├── <Feature>ClientImpl.java           # @Component implements <Feature>Client
│       └── <Feature2>ClientImpl.java
└── <feature>/                                 # ⚙️ Private Domain Implementation
    ├── constant/                              # Feature domain constants (<Feature>Status.java, <Feature>Role.java, <Feature>Tab.java) & cache names
    ├── controller/                            # Web REST controllers & API contracts
    │   ├── <Feature>Api.java                  # Contract Interface (Routing & OpenAPI Specs)
    │   └── <Feature>Controller.java           # Implementation Class (@RestController)
    ├── service/                               # Internal business logic
    │   └── <Feature>Service.java
    ├── repository/                            # Spring Data JPA Repository
    │   ├── <Feature>Repository.java
    │   └── spec/                              # Specifications for dynamic querying
    │       └── <Feature>Specs.java
    ├── entity/                                # JPA Entities
    │   └── <Feature>.java
    ├── mapper/                                # MapStruct interfaces
    │   └── <Feature>Mapper.java
    └── dto/                                   # Internal HTTP Request/Response DTOs
        ├── Create<Feature>Request.java
        └── <Feature>DetailResponse.java
```

## 2. Strict Boundary Rules

1. **Internal Encapsulation**: Classes in `constant`, `service`, `repository`, `entity`, and `mapper` are **private** to this feature/domain. Modul luar dilarang meng-import class dari sub-package ini.
2. **Client Contract Rule**: Modul luar **HANYA** boleh meng-import interface di `client/contract/` dan Record DTO di `client/dto/`. Implementasi di `client/impl/` adalah privat.
3. **No Entity Leakage**: Client Contract dan Controller **DILARANG** mengembalikan JPA `@Entity`. Seluruh data yang keluar wajib dibungkus dalam Java 21 `record`.
4. **Controller Boundary & Contract Interface Pattern**:
   - Seluruh REST controller wajib mengimplementasikan interface kontraknya (`<Feature>Controller implements <Feature>Api`).
   - Rute HTTP (`@GetMapping`, `@PostMapping`, dll.) serta seluruh metadata dokumentasi OpenAPI (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirements`) wajib dideklarasikan pada interface `<Feature>Api`.
   - Class `<Feature>Controller` murni bertindak sebagai implementasi bersih: hanya beranotasi `@RestController` dan `@RequiredArgsConstructor`, menginjeksi Service/Client, memanggil logika bisnis, dan mengembalikan `RestResponse<T>`.
   - Controller dilarang meng-inject Repository.
5. **Transactional Boundary**: Anotasi `@Transactional` hanya boleh digunakan di layer Service atau Client implementation, dilarang di Controller.
6. **The Golden Stack for Cross-Module Enrichment**: Datatable atau listing queries **DILARANG** melakukan SQL `JOIN` lintas tabel modul lain. Wajib menerapkan **The Golden Stack** (L1 Cache + `Lookups` in-memory batch extraction/map + MapStruct DTO assembly). Lihat [Rule 11](11-the-golden-stack.md).
7. **Domain Constants Encapsulation & Declaration Pattern**:
   - Seluruh konstanta status, roles, tabs, dan types wajib berada di `module.<domain>.<feature>.constant`.
   - Wajib dideklarasikan sebagai `public final class` dengan **private constructor** (`private <Feature>Status() {}`) dan `public static final String` fields. Dilarang keras menggunakan *Constant Interface Anti-pattern* (`public interface <Feature>Constants`).
   - Modul luar dilarang mengimpor kelas dari sub-package `constant/` modul lain. Jika modul luar perlu memvalidasi status/perilaku, lakukan melalui Client interface (misal: `boardClient.validateForTaskCreation(...)`).

## 3. Matriks Hak Akses Antar Modul (Access Matrix)

| Modul Pemanggil (Consumer) | Paket Target di Modul Lain (Provider) | Status Akses | Tindakan Pelanggaran |
| :--- | :--- | :---: | :--- |
| `module.<caller>..*` | `module.<target>.client.contract.*` | ✅ **BOLEH** | Diizinkan via Constructor Injection |
| `module.<caller>..*` | `module.<target>.client.dto.*` | ✅ **BOLEH** | Diizinkan sebagai objek data |
| `module.<caller>..*` | `module.<target>.client.impl.*` | ❌ **DILARANG** | Digagalkan otomatis oleh ArchUnit |
| `module.<caller>..*` | `module.<target>.<feature>.service.*` | ❌ **DILARANG** | Digagalkan otomatis oleh ArchUnit |
| `module.<caller>..*` | `module.<target>.<feature>.repository.*` | ❌ **DILARANG** | Digagalkan otomatis oleh ArchUnit |
| `module.<caller>..*` | `module.<target>.<feature>.entity.*` | ❌ **DILARANG** | Digagalkan otomatis oleh ArchUnit |

## 4. SOP 5-Tahap Migrasi ke Microservices (Strangler Fig Pattern)

Ketika suatu domain diputuskan siap dipisahkan menjadi microservice independen (berdasarkan evaluasi bobot kesiapan: trafik, isolasi database, dsb.):

1. **Langkah 1 (Pre-Flight Gate)**: Pastikan `./gradlew check` hijau dan guardrail ArchUnit bebas pelanggaran.
2. **Langkah 2 (Lift & Shift)**: Salin folder `module/<target-domain>/` ke repo microservice baru. Sediakan REST API endpoint internal (`/api/v1/internal/...`).
3. **Langkah 3 (Deploy Standalone)**: Deploy microservice baru dengan database terpisah. Verifikasi via `/actuator/health`.
4. **Langkah 4 (Switch Implementation di Monolith)**: Di Monolith, hapus folder internal `service/`, `repository/`, `entity/`, dan class `<Feature>ClientImpl.java`. **Pertahankan** `client/contract/<Feature>Client.java` dan `client/dto/`. Buat class baru `client/impl/<Feature>ClientRemoteImpl.java` yang menggunakan Spring `RestClient` untuk memanggil microservice baru.
5. **Langkah 5 (Verifikasi Zero-Rewrite pada Konsumen)**: Jalankan targeted test modul konsumen (misal `BoardService`). Seluruh test wajib lulus tanpa mengubah satu baris pun kode bisnis pada konsumen.
