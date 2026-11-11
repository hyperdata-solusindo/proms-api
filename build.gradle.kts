plugins {
    java
    jacoco
    checkstyle
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
    // Development Hot-Reload & Fast Restart
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    // Cache Provider (Caffeine L1 Cache)
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Observability: OpenTelemetry & Micrometer Tracing / Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Sentry Core & Logback Integration (Spring Boot 4 compatible)
    implementation("io.sentry:sentry:7.18.0")
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
    runtimeOnly("info.picocli:picocli:4.7.7")

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

// Checkstyle Quality Gate Configuration
checkstyle {
    toolVersion = "10.12.5"
    configFile = file("${rootDir}/config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
    maxErrors = 0
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(false)
        html.required.set(true)
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

// Local Environment Injection (.env) for Development & Testing
fun loadEnv(): Map<String, String> {
    val envFile = file(".env")
    if (!envFile.exists()) return emptyMap()

    return envFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val parts = line.split("=", limit = 2)
            val key = parts[0].trim()
            val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
            key to value
        }
}

val localEnv = loadEnv()

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    environment(localEnv)
}

tasks.withType<Test> {
    useJUnitPlatform()
    environment(localEnv)
}

tasks.test {
    outputs.dir(project.extra["snippetsDir"]!!)
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",
                    "**/entity/**",
                    "**/model/**",
                    "**/config/**",
                    "**/*Application*",
                    "**/*MapperImpl*"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            element = "BUNDLE"
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.30".toBigDecimal() // Minimal 30% baseline untuk CI Quality Gate
            }
        }
    }
    classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)
}

tasks.named("check") {
    dependsOn("installGitHooks")
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
}

tasks.register<JavaExec>("liquibase") {
    group = "database"
    description = "Runs Liquibase CLI using project runtime classpath"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("liquibase.integration.commandline.LiquibaseCommandLine")

    val defaultAppUser = localEnv["DB_APP_USER"] ?: localEnv["DB_USER"] ?: "platform_user"
    systemProperty(
        "app_user",
        providers.systemProperty("app_user")
            .orElse(providers.environmentVariable("DB_APP_USER").orElse(defaultAppUser))
    )
}
