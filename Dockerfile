# Stage 1: Build
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Run - Volver a la imagen base que SÍ funciona
FROM eclipse-temurin:21-jre

# Instalar solo las librerías mínimas necesarias para Chromium
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2 \
        libdrm2 libxkbcommon0 libxcomposite1 libxdamage1 libxrandr2 \
        libgbm1 libxshmfence1 libasound2t64 curl && \
    rm -rf /var/lib/apt/lists/*

# Instalar solo Chromium con Playwright
RUN curl -sL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    npx playwright install chromium

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]