plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

group = "io.github.maksim0840"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// Для корректного выбора версии testcontainers
extra["testcontainers.version"] = "2.0.3"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // gRPC server
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
    implementation(project(":internal-api")) // общие зависимости для proto контрактов

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:2.0.3"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    testImplementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")

}

tasks.test {
    useJUnitPlatform()
}
