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

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // gRPC server
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
    implementation(project(":internal-api")) // общие зависимости для proto контрактов
}
