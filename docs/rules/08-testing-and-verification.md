# Rule 08: Testing Standards & BDD @DisplayName

## 1. Two-Tier Testing Strategy
1. **Inner Loop (During Feature Dev)**: Run ONLY the tests of the feature you are working on. Fast feedback (< 5 seconds):
   ```bash
   ./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"
   ```
2. **Outer Loop (Definition of Done)**: Run full project verification suite before completion:
   ```bash
   ./gradlew check
   ```
   *(Menjalankan seluruh unit test, slice test, integrasi, ArchUnit guardrails, serta verifikasi formatting Spotless).*

## 2. Human-Readable BDD Format
All test methods MUST use `@DisplayName` in BDD format:
`@DisplayName("Given [state/precondition], when [action], then [expected outcome]")`

Example:
```java
@Test
@DisplayName("Given existing SKU, when createProduct, then throw DuplicateResourceException")
void shouldThrowWhenSkuExists() {
    // ...
}
```

## 3. Test Layers & Scope
- **Service Tests**: Pure unit tests with Mockito (`@ExtendWith(MockitoExtension.class)`). Do NOT spin up Spring context.
- **Controller Tests**: Fast standalone MockMvc unit tests with `@ExtendWith(MockitoExtension.class)` using `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build()`. DILARANG menggunakan `@WebMvcTest(FeatureController.class)` atau mem-boot Spring ApplicationContext penuh untuk menjaga feedback test tetap instan (< 1 detik).
- **Cache Integration Tests**: Gunakan `@SpringJUnitConfig(classes = {TestConfig.class})` dengan nested static `@Configuration` yang meng-`@Import(CacheConfig.class)` dan me-mock repository. Ini memungkinkan verifikasi akurat `@Cacheable`, `@CacheEvict`, dan key formatting multi-tenant tanpa overhead database.
- **Specification & Validation Tests**: Pure unit tests untuk komposisi `Specification<T>` (`<Feature>SpecsTest`) dan validasi YAVI (`<Request>ValidationTest` via `Request.validator.validate(dto)`) tanpa Spring runner.
- **Repository Tests**: Use `@DataJpaTest` with embedded H2 database (`testRuntimeOnly("com.h2database:h2")`) only if testing custom complex `@Query` JPQL Level 2.
