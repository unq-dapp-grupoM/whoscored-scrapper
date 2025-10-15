# Stage 1: Build the application
FROM gradle:8.5.0-jdk21 AS builder

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Run la aplicación  
FROM mcr.microsoft.com/playwright:v1.56.0-jammy

# Usar el chromium que ya viene preinstalado y limpiar los demás
RUN rm -rf /root/.cache/ms-playwright/firefox* /root/.cache/ms-playwright/webkit*

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]