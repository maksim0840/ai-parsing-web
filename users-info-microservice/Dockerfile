# Сборка
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY settings.gradle.kts gradlew ./
COPY gradle ./gradle
COPY internal-api ./internal-api
COPY users-info-microservice ./users-info-microservice
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# Создаём второй минимальный образ с jar-файлом
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/users-info-microservice/build/libs/*.jar app.jar

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]
