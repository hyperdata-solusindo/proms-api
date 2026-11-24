# Gradle to Maven Migration (Clean Break) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Memigrasikan seluruh sistem build proyek dari Gradle (`build.gradle.kts`) ke Apache Maven (`pom.xml`) menggunakan strategi Clean Break, menjamin 100% parity pada kompilasi Java 21, MapStruct/Lombok annotation processing, quality gates (Spotless, Checkstyle, JaCoCo), CI/CD workflows, Dockerfiles, dan dokumentasi AGENTS.md.

**Architecture:** Mengganti engine build dengan `pom.xml` berbasis `spring-boot-starter-parent:4.1.2`, mengonfigurasi Maven Compiler Plugin dengan urutan spesifik Lombok -> MapStruct binding -> MapStruct processor, memasang wrapper `mvnw`, menyelaraskan target output layer extraction ke `target/extracted/`, memperbarui pipeline CI/CD GitHub Actions dan container build, lalu menghapus seluruh artefak Gradle.

**Tech Stack:** Java 21 (Eclipse Temurin), Apache Maven 3.9+, Spring Boot 4.1.2, Spotless Maven Plugin, Maven Checkstyle Plugin, JaCoCo Maven Plugin, Docker, GitHub Actions.

**Spec:** [docs/superpowers/specs/2026-11-23-gradle-to-maven-migration-design.md](file:///home/kholifan/html/project-promps/api/docs/superpowers/specs/2026-11-23-gradle-to-maven-migration-design.md)

## Global Constraints
- Target Java Runtime: Java 21 (`<maven.compiler.release>21</maven.compiler.release>`).
- Framework: Spring Boot 4.1.2.
- Clean Break: Seluruh file Gradle (`build.gradle.kts`, `settings.gradle.kts`, `gradlew`, `gradlew.bat`, `gradle/`) dihapus setelah verifikasi parity selesai.
- Zero Git Auto-Commit: Jangan jalankan `git commit` atau `git push` otomatis.
- JaCoCo DoD Rule: Minimal 30% LINE coverage pada bundle counter dengan exclusion file DTO, Entity, Model, Config, Application, dan MapperImpl.

---

### Task 1: Generate `pom.xml` with Full Dependency & Plugin Parity

**Files:**
- Create: `pom.xml`

**Interfaces:**
- Consumes: `build.gradle.kts` dependency tree and plugin definitions.
- Produces: Valid Maven POM model with Spring Boot 4.1.2 parent, all starters, third-party libraries (YAVI, OTel, Sentry, Redis, Caffeine, Bucket4j, ArchUnit), annotation processors, Spotless, Checkstyle, and JaCoCo plugins.

- [ ] **Step 1: Write `pom.xml`**
Create `pom.xml` with complete configuration:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.2</version>
        <relativePath/>
    </parent>

    <groupId>com.hyperdata</groupId>
    <artifactId>proms-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>proms-api</name>
    <description>PROMS API Platform</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- Dependencies versions -->
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.36</lombok.version>
        <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
        <yavi.version>0.14.1</yavi.version>
        <opentelemetry.version>1.44.1</opentelemetry.version>
        <sentry.version>8.0.0</sentry.version>
        <bucket4j.version>8.10.1</bucket4j.version>
        <springdoc.version>2.8.5</springdoc.version>
        <archunit.version>1.3.0</archunit.version>
        <spotless.version>2.44.3</spotless.version>
        <checkstyle.plugin.version>3.6.0</checkstyle.plugin.version>
        <checkstyle.version>10.12.5</checkstyle.version>
        <jacoco.version>0.8.12</jacoco.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Database & Migration -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
        </dependency>

        <!-- Caching & Rate Limiting -->
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-core</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>
        <dependency>
            <groupId>com.bucket4j</groupId>
            <artifactId>bucket4j-redis</artifactId>
            <version>${bucket4j.version}</version>
        </dependency>

        <!-- Validation & DTO Mapping -->
        <dependency>
            <groupId>am.ik.yavi</groupId>
            <artifactId>yavi</artifactId>
            <version>${yavi.version}</version>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- OpenAPI / Swagger -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Observability & Tracing -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <version>${opentelemetry.version}</version>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk</artifactId>
            <version>${opentelemetry.version}</version>
        </dependency>
        <dependency>
            <groupId>io.sentry</groupId>
            <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
            <version>${sentry.version}</version>
        </dependency>

        <!-- Testing & Quality Assurance -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler & Annotation Processor -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <release>${maven.compiler.release}</release>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok-mapstruct-binding.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Spring Boot Executable & Layered JAR Packaging -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>

            <!-- Spotless Code Formatter Plugin -->
            <plugin>
                <groupId>com.diffplug.spotless</groupId>
                <artifactId>spotless-maven-plugin</artifactId>
                <version>${spotless.version}</version>
                <configuration>
                    <java>
                        <toggleOffOn/>
                        <removeUnusedImports/>
                        <trimTrailingWhitespace/>
                        <endWithNewline/>
                        <eclipse>
                            <file>${project.basedir}/.vscode/eclipse-java-formatter.xml</file>
                        </eclipse>
                    </java>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <phase>verify</phase>
                    </execution>
                </executions>
            </plugin>

            <!-- Checkstyle Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <version>${checkstyle.plugin.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>com.puppycrawl.tools</groupId>
                        <artifactId>checkstyle</artifactId>
                        <version>${checkstyle.version}</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <configLocation>${project.basedir}/config/checkstyle/checkstyle.xml</configLocation>
                    <consoleOutput>true</consoleOutput>
                    <failsOnError>true</failsOnError>
                    <linkXRef>false</linkXRef>
                </configuration>
                <executions>
                    <execution>
                        <id>validate</id>
                        <phase>validate</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- JaCoCo Code Coverage Plugin -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <configuration>
                    <excludes>
                        <exclude>**/dto/**</exclude>
                        <exclude>**/entity/**</exclude>
                        <exclude>**/model/**</exclude>
                        <exclude>**/config/**</exclude>
                        <exclude>**/*Application*</exclude>
                        <exclude>**/*MapperImpl*</exclude>
                    </excludes>
                </configuration>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>0.30</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Setup Maven Wrapper (`mvnw`)**
Run: `mvn wrapper:wrapper -Dmaven=3.9.9` or copy wrapper binaries (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`).
Expected: Executable `mvnw` ready in root directory.

- [ ] **Step 3: Verify compilation & annotation processing**
Run: `./mvnw clean compile -DskipTests`
Expected: `BUILD SUCCESS` with generated MapStruct mappers inside `target/generated-sources/annotations`.

---

### Task 2: Verify Tests, Architecture Guardrails, and Quality Gates

**Files:**
- Modify: `pom.xml` (adjust plugin bindings if needed)

**Interfaces:**
- Consumes: JUnit 5 tests, `ArchitectureTest`, Checkstyle config, Eclipse Formatter XML.
- Produces: Passing test reports in `target/surefire-reports`, JaCoCo coverage reports in `target/site/jacoco`, and clean Checkstyle/Spotless verification.

- [ ] **Step 1: Run Unit & Targeted Tests**
Run: `./mvnw test`
Expected: `BUILD SUCCESS`, all tests pass including `ArchitectureTest`.

- [ ] **Step 2: Verify Spotless Formatter & Checkstyle**
Run: `./mvnw spotless:check checkstyle:check`
Expected: Clean formatting without violations. If formatting is needed, run `./mvnw spotless:apply`.

- [ ] **Step 3: Run Full Verification Phase (including JaCoCo Coverage Check)**
Run: `./mvnw clean verify`
Expected: Coverage minimum 30% met, reports generated in `target/site/jacoco/index.html`.

---

### Task 3: Packaging & Spring Boot Layer Extraction Verification

**Files:**
- Test Output: `target/*.jar`, `target/extracted/`

**Interfaces:**
- Consumes: Executable jar generated by `spring-boot-maven-plugin`.
- Produces: Separated Spring Boot layers in `target/extracted/` (`dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`).

- [ ] **Step 1: Package Executable JAR**
Run: `./mvnw clean package -DskipTests`
Expected: Uber JAR created in `target/proms-api-0.0.1-SNAPSHOT.jar`.

- [ ] **Step 2: Test Layer Extraction via jarmode=tools**
Run:
```bash
mkdir -p target/extracted
java -Djarmode=tools -jar target/proms-api-0.0.1-SNAPSHOT.jar extract \
  --layers dependencies,spring-boot-loader,snapshot-dependencies,application \
  --launcher \
  --destination target/extracted \
  --force
ls -lh target/extracted/
```
Expected: Directories `dependencies/`, `spring-boot-loader/`, `snapshot-dependencies/`, and `application/` extracted cleanly.

---

### Task 4: Update Dockerfiles and `.dockerignore`

**Files:**
- Modify: `docker/Dockerfile`
- Modify: `docker/Dockerfile.fast`
- Modify: `.dockerignore`

**Interfaces:**
- Consumes: Extracted layers in `target/extracted/` and Maven build outputs.
- Produces: Docker images built via Maven-based layers.

- [ ] **Step 1: Update `docker/Dockerfile`**
Update multi-stage builder to use Maven:
```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
RUN apk add --no-cache bash

COPY mvnw pom.xml ./
COPY .mvn ./.mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline || true

COPY src ./src
RUN ./mvnw clean package -DskipTests && \
    UBER_JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" | head -n 1) && \
    java -Djarmode=tools -jar "$UBER_JAR" extract --layers dependencies,spring-boot-loader,snapshot-dependencies,application --launcher --destination /workspace/extracted --force

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
RUN apk add --no-cache curl tzdata
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --chown=appuser:appgroup --from=builder /workspace/extracted/dependencies/ /app/
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/spring-boot-loader/ /app/
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/snapshot-dependencies/ /app/
COPY --chown=appuser:appgroup --from=builder /workspace/extracted/application/ /app/

USER appuser
ENV SERVER_PORT=8080 SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=5 --start-period=25s \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher \"$@\"", "--"]
```

- [ ] **Step 2: Update `docker/Dockerfile.fast`**
Update paths from `build/extracted/` to `target/extracted/`:
```dockerfile
COPY --chown=appuser:appgroup target/extracted/dependencies/ /app/
COPY --chown=appuser:appgroup target/extracted/spring-boot-loader/ /app/
COPY --chown=appuser:appgroup target/extracted/snapshot-dependencies/ /app/
COPY --chown=appuser:appgroup target/extracted/application/ /app/
```

- [ ] **Step 3: Update `.dockerignore`**
Replace Gradle ignore patterns with Maven patterns:
```dockerignore
target/*
!target/extracted
!target/extracted/**
.mvn
```

- [ ] **Step 4: Verify Docker Build locally**
Run: `docker build -t proms-api-test:mvn -f docker/Dockerfile.fast .`
Expected: `naming to docker.io/library/proms-api-test:mvn done`, build succeeds.

---

### Task 5: Update GitHub Actions Workflows (`ci.yml` & `cd.yml`)

**Files:**
- Modify: `.github/workflows/ci.yml`
- Modify: `.github/workflows/cd.yml`

**Interfaces:**
- Consumes: `./mvnw` build command and `target/` artifacts.
- Produces: Passing CI verification workflow and automated CD container image publishing.

- [ ] **Step 1: Update `.github/workflows/ci.yml`**
1. Remove `gradle/actions/setup-gradle@v4`.
2. Configure `cache: 'maven'` in `actions/setup-java@v4`:
   ```yaml
   - name: Set up JDK 21 (Temurin)
     uses: actions/setup-java@v4
     with:
       java-version: '21'
       distribution: 'temurin'
       cache: 'maven'

   - name: Grant execute permission for mvnw
     run: chmod +x mvnw

   - name: Run verification (Spotless, JUnit, ArchUnit, JaCoCo)
     id: verification
     run: ./mvnw clean verify
   ```
3. Update upload artifact paths from `build/reports/...` to:
   ```yaml
   path: |
     target/surefire-reports/
     target/site/jacoco/
   ```

- [ ] **Step 2: Update `.github/workflows/cd.yml`**
1. Remove `gradle/actions/setup-gradle@v4` in `build-and-push` job, use `cache: 'maven'`.
2. Update build and extraction step:
   ```yaml
   - name: Build and Extract Spring Boot Application Layers
     run: |
       echo "🔨 Building executable Spring Boot JAR with Maven..."
       ./mvnw clean package -DskipTests

       echo "📦 Extracting Spring Boot layers with layertools..."
       UBER_JAR=$(find target -maxdepth 1 -name "*.jar" ! -name "*-sources.jar" | head -n 1)
       java -Djarmode=tools -jar "$UBER_JAR" extract \
         --layers dependencies,spring-boot-loader,snapshot-dependencies,application \
         --launcher \
         --destination target/extracted \
         --force

       echo "✅ Layers successfully extracted to target/extracted:"
       ls -lh target/extracted/
   ```

---

### Task 6: Clean Break - Remove Gradle Files & Update Documentation

**Files:**
- Delete: `build.gradle.kts`
- Delete: `settings.gradle.kts`
- Delete: `gradlew`
- Delete: `gradlew.bat`
- Delete: `gradle/`
- Modify: `AGENTS.md`
- Modify: `README.md` (if exists)

**Interfaces:**
- Consumes: Finished Maven transition.
- Produces: Pure Maven repository with clean git status and updated developer documentation.

- [ ] **Step 1: Remove all Gradle files**
Run: `rm -rf build.gradle.kts settings.gradle.kts gradlew gradlew.bat gradle/ build/`
Verify: `git status` shows Gradle files deleted.

- [ ] **Step 2: Update `AGENTS.md`**
Update Section 1 (Core Tech Stack), Section 5 (Daily Development Commands), and Section 6 (DoD Checklist) replacing `./gradlew` with `./mvnw` commands:
- `./gradlew test --tests "..."` $\to$ `./mvnw test -Dtest=...`
- `./gradlew check` $\to$ `./mvnw clean verify`
- `./gradlew spotlessApply` $\to$ `./mvnw spotless:apply`
- `./gradlew spotlessCheck` $\to$ `./mvnw spotless:check`

- [ ] **Step 3: Full End-to-End Local Sanity Check**
Run: `./mvnw clean verify`
Expected: `BUILD SUCCESS`, 0 errors, 100% tests passing, Spotless clean, JaCoCo $\ge$ 30%.
