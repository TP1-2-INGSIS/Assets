# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY config config
COPY src src

RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# ---- Run stage ----
FROM ghcr.io/tp1-2-ingsis/monitoring-config:latest
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
