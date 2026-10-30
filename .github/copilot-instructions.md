# GitHub Copilot Instructions
Follow the master architectural conventions and coding standards located in `AGENTS.md` and `docs/rules/`.
- Modular monolith in `com.hyperdata.proms.module.<domain>.<feature>`
- Inter-module calls strictly through `<Feature>Facade` interface in `facade/`
- Java 21 Records for DTOs
- YAVI for validations
- MapStruct for entity/DTO conversions
- ArchUnit compliance required
