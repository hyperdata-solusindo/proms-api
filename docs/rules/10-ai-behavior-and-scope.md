# Rule 10: AI Behavior, Scope Lockdown & Definition of Done

## 1. Scope Lockdown (Strict Guardrails)
- **NO ROGUE REFACTORING**: Do NOT rename methods, touch logic, or reformat files outside your assigned feature module without explicit user instruction.
- **NO UNNECESSARY DEPENDENCIES**: Do NOT add new dependencies to `build.gradle.kts` unless explicitly asked.
- **ZERO DEAD / UNUSED CODE**: Do NOT declare unused fields, variables, methods, parameters, or imports. Do NOT inject dependencies (repositories, services, facades) that are not actively used by the class.
- **YAGNI (You Aren't Gonna Need It)**: Do NOT create premature abstractions, empty interfaces for internal services, or generic base classes with only one implementation. Use concrete service classes directly. Interfaces are strictly reserved for cross-module `Facade`.

## 2. Conventional Commits
Format messages as:
`<type>(<scope>): <short description in present tense>`
- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
- Scope: module name (e.g. `product`, `order`, `auth`, `common`).
- Note: Never run `git commit` or `git push` automatically without user instruction.

## 3. Self-Correction Loop
Before concluding a task, run the verification commands:
1. `./gradlew test --tests "com.hyperdata.proms.module.<domain>.<feature>.*"`
2. `./gradlew spotlessApply`
3. `./gradlew test --tests "com.hyperdata.proms.architecture.ArchitectureTest"`

Fix any compiler, formatting, or test failures autonomously before reporting completion.
