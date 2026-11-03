# Rule 01: Architecture & Structure (2-Layer Modular Monolith)

## 1. Package Layout

The project follows a **2-Layer Modular Monolith** architecture:
`com.hyperdata.proms.module.<domain>.<feature>`

Example:
- Domain: `inventory`, Feature: `product` -> `com.hyperdata.proms.module.inventory.product`
- Domain: `sales`, Feature: `order` -> `com.hyperdata.proms.module.sales.order`
- Domain: `identity`, Feature: `auth` -> `com.hyperdata.proms.module.identity.auth`

Inside each `<feature>` directory, strictly maintain this folder structure:
```text
<feature>/
├── controller/                       # Web REST controllers & API contracts
│   ├── <Feature>Api.java             # Contract Interface (Routing & OpenAPI Specs)
│   └── <Feature>Controller.java      # Implementation Class (@RestController)
├── facade/                           # Public gateway to other modules
│   ├── <Feature>Facade.java          # Public Interface (Contract)
│   ├── dto/                          # Public Record DTOs returned by Facade
│   │   └── <Feature>SummaryDto.java
│   └── internal/                     # Hidden Implementation
│       └── Default<Feature>Facade.java
├── service/                          # Internal business logic
│   └── <Feature>Service.java
├── repository/                       # Spring Data JPA Repository
│   ├── <Feature>Repository.java
│   └── spec/                         # Specifications for dynamic querying
│       └── <Feature>Specs.java
├── entity/                           # JPA Entities
│   └── <Feature>.java
├── mapper/                           # MapStruct interfaces
│   └── <Feature>Mapper.java
└── dto/                              # Internal HTTP Request/Response DTOs
    ├── Create<Feature>Request.java
    └── <Feature>DetailResponse.java
```

## 2. Strict Boundary Rules

1. **Internal Encapsulation**: Classes in `service`, `repository`, `entity`, and `mapper` are **private** to this feature. Modul lain dilarang meng-import class dari sub-package ini.
2. **Facade Contract Rule**: Modul luar **HANYA** boleh meng-import interface di `facade/` dan Record DTO di `facade/dto/`.
3. **No Entity Leakage**: Facade dan Controller **DILARANG** mengembalikan JPA `@Entity`. Seluruh data yang keluar wajib dibungkus dalam Java 21 `record`.
4. **Controller Boundary & Contract Interface Pattern**:
   - Seluruh REST controller wajib mengimplementasikan interface kontraknya (`<Feature>Controller implements <Feature>Api`).
   - Rute HTTP (`@GetMapping`, `@PostMapping`, dll.) serta seluruh metadata dokumentasi OpenAPI (`@Tag`, `@Operation`, `@ApiResponses`, `@Parameter`, `@SecurityRequirements`) wajib dideklarasikan pada interface `<Feature>Api`.
   - Class `<Feature>Controller` murni bertindak sebagai implementasi bersih: hanya beranotasi `@RestController` dan `@RequiredArgsConstructor`, menginjeksi Service/Facade, memanggil logika bisnis, dan mengembalikan `ApiResponse<T>`.
   - Controller dilarang meng-inject Repository.
5. **Transactional Boundary**: Anotasi `@Transactional` hanya boleh digunakan di layer Service atau Facade implementation, dilarang di Controller.
6. **The Golden Stack for Cross-Module Enrichment**: Datatable atau listing queries **DILARANG** melakukan SQL `JOIN` lintas tabel modul lain. Wajib menerapkan **The Golden Stack** (L1 Cache + `Lookups` in-memory batch extraction/map + MapStruct DTO assembly). Lihat [Rule 11](11-the-golden-stack.md).
