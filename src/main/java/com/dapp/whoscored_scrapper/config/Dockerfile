# --- Etapa 1: Build ---
# Usamos una imagen que ya tiene Gradle y JDK 21
FROM gradle:8.5.0-jdk21 AS builder

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /home/gradle/src

# Copia todo el proyecto al contenedor
COPY . .

# Da permisos de ejecución al script de gradle
RUN chmod +x ./gradlew

# Instala Node.js y luego Playwright con sus dependencias de navegador
RUN apt-get update && apt-get install -y nodejs npm && \
    npm install -g npx && \
    npx playwright@1.45.0 install --with-deps chromium

# Construye la aplicación. El test se salta para acelerar el build en el pipeline.
RUN ./gradlew build -x test

# --- Etapa 2: Run ---
# Usamos una imagen ligera que solo tiene el Java Runtime Environment (JRE)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copia el JAR construido de la etapa anterior
COPY --from=builder /home/gradle/src/build/libs/*.jar app.jar

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]