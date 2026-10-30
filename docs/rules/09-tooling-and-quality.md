# Rule 09: Automated Quality Gates (ArchUnit & Spotless)

## 1. Spotless Java Formatter
Spotless enforces Google Java Format (AOSP 4-space indent) with import sorting and trailing whitespace cleanup.

Commands:
- Apply formatting: `./gradlew spotlessApply`
- Check compliance: `./gradlew spotlessCheck`

Always run `./gradlew spotlessApply` before completing any task.

## 2. ArchUnit Architecture Guardrails
Located in `src/test/java/com/hyperdata/proms/architecture/ArchitectureTest.java`.

ArchUnit strictly enforces:
1. Controllers do not depend on Repositories.
2. Facades in `module.*.*.facade` must be Java interfaces.
3. Facade implementations must reside in `*.facade.internal.*` and carry `@Component`.
4. Facade methods must not return JPA `@Entity` types.
5. Controllers must be annotated with `@RestController`.
6. Services must be annotated with `@Service`.

Run ArchUnit checks:
```bash
./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"
```
