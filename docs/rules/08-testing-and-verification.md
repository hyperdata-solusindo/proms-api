# Rule 08: Testing Standards & BDD @DisplayName

## 1. Two-Tier Testing Strategy
1. **Inner Loop (During Feature Dev)**: Run ONLY the tests of the feature you are working on. Fast feedback (< 5 seconds):
   ```bash
   ./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"
   ```
2. **Outer Loop (Definition of Done)**: Run full test and verification suite:
   ```bash
   ./gradlew test
   ./gradlew spotlessCheck
   ```

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
- **Repository Tests**: Use `@DataJpaTest` with test containers or embedded database only if testing custom complex `@Query`.
