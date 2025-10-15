# Stage 1: Build
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Run - Usar imagen de Playwright con Java
FROM mcr.microsoft.com/playwright:v1.55.0-jammy

# Instalar Java en la imagen de Playwright (que ya tiene todas las dependencias)
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-21-jre-headless && \
    rm -rf /var/lib/apt/lists/*

# Limpiar browsers que no necesitamos, mantener solo Chromium
RUN rm -rf /root/.cache/ms-playwright/firefox* /root/.cache/ms-playwright/webkit*

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]