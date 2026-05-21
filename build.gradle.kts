plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.4"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "com.wannabe"
version = "0.0.1-SNAPSHOT"
description = "wallet"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("com.mysql:mysql-connector-j")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

openapi3 {
    setServer("http://localhost:8080")
    title = "Wallet API"
    description = "동시 출금, 멱등성, 거래내역 조회를 제공하는 월렛 API"
    version = "v1"
    format = "yaml"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "standardOut", "standardError")
        showStandardStreams = true
    }
}

tasks.matching { it.name == "openapi3" }.configureEach {
    dependsOn(tasks.test)
}
