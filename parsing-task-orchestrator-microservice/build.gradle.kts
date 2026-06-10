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
    // Mapstruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")

    // Lombok
    compileOnly("org.projectlombok:lombok")

    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // LLM
    implementation("com.openai:openai-java:4.32.0")
    implementation("chat.giga:gigachat-java:0.1.14")

    // Json Mapping
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    
    // MongoDB
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // s3
    implementation(platform("software.amazon.awssdk:bom:2.44.4"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:auth")
    implementation("software.amazon.awssdk:regions")
    implementation("software.amazon.awssdk:url-connection-client")

    // gRPC server + client
    implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    implementation(project(":internal-api")) // общие зависимости для proto контрактов

    // RabbitMQ
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    testImplementation(enforcedPlatform("org.testcontainers:testcontainers-bom:2.0.3"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-rabbitmq:2.0.4")
    testImplementation("org.testcontainers:testcontainers-mongodb")
    testImplementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
    testImplementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
}

tasks.test {
    useJUnitPlatform()
}
