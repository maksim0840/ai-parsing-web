plugins {
    id("java-library") // модуль-библиотека для подключения из других модулей
    id("com.google.protobuf") version "0.9.6" // автоматизация пайплайна работы с protobuf
}

group = "io.github.maksim0840"
version = "1.0.0"

// Преобразование proto файлов в java классы
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.78.0"
        }
    }
    generateProtoTasks {
        all().configureEach {
            plugins {
                create("grpc")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

    // Экспортируем зависимости наружу
    api(platform("io.grpc:grpc-bom:1.78.0"))
    api("io.grpc:grpc-stub")
    api("io.grpc:grpc-protobuf")
    api(platform("com.google.protobuf:protobuf-bom:3.25.5"))
    api("com.google.protobuf:protobuf-java")
    api("com.google.protobuf:protobuf-java-util")
}
