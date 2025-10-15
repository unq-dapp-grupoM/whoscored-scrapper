# Stage 1: Build the application
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Run the application  
FROM eclipse-temurin:21-jre

# Instalar solo las dependencias necesarias para Chromium
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        libglib2.0-0t64 \
        libnss3 \
        libnspr4 \
        libdbus-1-3 \
        libatk1.0-0t64 \
        libatk-bridge2.0-0t64 \
        libcups2t64 \
        libdrm2 \
        libatspi2.0-0t64 \
        libx11-6 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxrandr2 \
        libgbm1 \
        libxcb1 \
        libxkbcommon0 \
        libpango-1.0-0 \
        libcairo2 \
        libasound2t64 \
        libxcursor1 \
        fonts-liberation \
        libnss3-tools \
        ca-certificates \
        curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Instalar solo Chromium con Playwright
RUN curl -sL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    npx playwright install chromium

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]