# Stage 1: Build
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Run - Imagen más ligera
FROM eclipse-temurin:21-jre-jammy

# Instalar SOLO las dependencias mínimas para Chromium
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libnss3 libnspr4 libatk1.0-0 libatk-bridge2.0-0 libcups2 \
        libdrm2 libxkbcommon0 libxcomposite1 libxdamage1 libxrandr2 \
        libgbm1 libxshmfence1 libasound2t64 libxfixes3 libcairo2 \
        libpango-1.0-0 fonts-liberation curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Instalar Chromium directamente (más ligero que Playwright)
RUN curl -LO https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb && \
    apt-get install -y ./google-chrome-stable_current_amd64.deb && \
    rm google-chrome-stable_current_amd64.deb

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Variables de entorno para reducir memoria
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xms128m"
ENV PLAYWRIGHT_BROWSERS_PATH="/tmp/playwright"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]