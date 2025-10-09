# Stage 1: Build the application
FROM gradle:8.5.0-jdk21 AS builder

# Set the working directory
WORKDIR /app

# Copy the application code
COPY . .

# Given permissions to gradlew
RUN chmod +x ./gradlew

# Install Node.js and Playwright with browser dependencies
RUN apt-get update && apt-get install -y nodejs npm && \
    npm install -g npx && \
    npx playwright@1.45.0 install --with-deps chromium

# Build the application (requires Maven or Gradle)
RUN ./gradlew clean build -x test

# Stage 2: Run the application
FROM eclipse-temurin:21-jre

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port the app will run on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]