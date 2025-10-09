# Usa una imagen base que ya contiene una versión de Java (JDK 21)
FROM eclipse-temurin:21-jdk-jammy

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Instala Node.js y npm para poder usar 'npx' para Playwright
# El '&&' encadena comandos y 'apt-get clean' limpia la caché para mantener la imagen pequeña
RUN apt-get update && \
    apt-get install -y nodejs npm && \
    apt-get clean

# Copia los archivos de la aplicación al contenedor
# Copiamos primero los archivos de build para aprovechar el cache de Docker
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

# Copia el resto del código fuente de la aplicación
COPY src ./src

# Expone el puerto en el que correrá la aplicación (Render usará la variable de entorno)
EXPOSE 10000