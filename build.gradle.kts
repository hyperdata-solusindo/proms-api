plugins {
    java
    id("org.springframework.boot") version "4.1.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
    id("com.diffplug.spotless") version "7.0.2"
}

group = "com.hyperdata.proms"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

val mapstructVersion = "1.6.3"
val lombokVersion = "1.18.36"
val lombokMapstructBindingVersion = "0.2.0"
val yaviVersion = "0.14.1"
val archunitVersion = "1.3.0"

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Observability: OpenTelemetry & Micrometer Tracing
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Sentry Spring Boot & Logback Integration
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.18.0")
    implementation("io.sentry:sentry-logback:7.18.0")

    // Structured JSON Logging for Production
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    // Spring AspectJ / AOP for AutoTraceAspect
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // Validation (YAVI)
    implementation("am.ik.yavi:yavi:$yaviVersion")

    // Object Mapping (MapStruct)
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    // Lombok & Annotation Processors
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // Database Driver (PostgreSQL)
    runtimeOnly("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restdocs")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")

    // ArchUnit Testing
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
}

// Spotless Code Formatter Configuration (Aligned with Eclipse / Red Hat Java)
spotless {
    java {
        target("src/**/*.java")
        eclipse().configFile(".vscode/eclipse-java-formatter.xml")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Git Hook Configuration for Seamless Cross-IDE Formatting
tasks.register<Exec>("installGitHooks") {
    description = "Configures git to use project pre-commit hooks"
    group = "verification"
    commandLine("git", "config", "core.hooksPath", ".githooks")
    onlyIf { file(".git").exists() }
}

tasks.named("check") {
    dependsOn("installGitHooks")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
}
