# GitHub Copilot Instructions
Follow the master architectural conventions and coding standards located in `AGENTS.md` (The 16 Golden Rules) and `docs/rules/`.
- Modular monolith in `com.hyperdata.proms.module.<domain>.<feature>`
- Inter-module calls strictly through `<Feature>Client` contract interface in `client/contract/<Feature>Client.java`
- Primary key IDs must use UUIDv7 via Hibernate `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)`
- Java 21 Records for DTOs
- YAVI for validations
- MapStruct for entity/DTO conversions
- ArchUnit compliance required
