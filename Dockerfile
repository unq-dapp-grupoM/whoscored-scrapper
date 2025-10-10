# Stage 1: Build the application
FROM mcr.microsoft.com/playwright:v1.56.0-jammy AS builder

# Instala el JDK 21 (para compilar tu código Java) y herramientas necesarias (curl para Gradle).
# La imagen de Playwright no incluye el JDK por defecto.
RUN apt-get update && \
    apt-get install -y openjdk-21-jdk curl unzip && \
    rm -rf /var/lib/apt/lists/*

# Instala Gradle manualmente, ya que la imagen de Playwright no lo trae (al contrario que 'gradle:8.5.0-jdk21').
ENV GRADLE_VERSION="8.5"
ENV GRADLE_HOME="/opt/gradle"
RUN curl -L https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o /tmp/gradle.zip && \
    unzip /tmp/gradle.zip -d /opt/ && \
    mv /opt/gradle-${GRADLE_VERSION} ${GRADLE_HOME} && \
    rm /tmp/gradle.zip
ENV PATH="${GRADLE_HOME}/bin:$PATH"

# Set the working directory
WORKDIR /app

# Copy the application code
COPY . .

# Given permissions to gradlew
RUN chmod +x ./gradlew

# Build the application (requires Maven or Gradle)
RUN ./gradlew clean build -x test

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

# 1. ACTUALIZAR Y AÑADIR LAS LIBRERÍAS DE SISTEMA OPERATIVO DE PLAYWRIGHT
# El JRE es basado en Debian/Ubuntu (por eso funciona apt-get).
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
        libasound2t64 && \
    # Limpia el cache de APT para mantener la imagen pequeña
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port the app will run on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]