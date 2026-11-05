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
- **Controller Tests**: Slice tests with `@WebMvcTest(FeatureController.class)`.
- **Repository Tests**: Use `@DataJpaTest` with embedded H2 database (`testRuntimeOnly("com.h2database:h2")`) only if testing custom complex `@Query` or JPA Specifications.
