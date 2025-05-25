# Stage 1: Build the application
FROM openjdk:17-jdk-slim AS build

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY libs.versions.toml . # Copy this if you use Gradle's versions catalog

COPY src ./src

RUN chmod +x ./gradlew

# Use 'assemble' to build the runnable JAR with Ktor plugin
RUN ./gradlew clean assemble --no-daemon

# Stage 2: Run the application
# CHANGE THIS LINE: Use 'openjdk:17-jre' which is a standard and available JRE image.
FROM openjdk:17-jre

WORKDIR /app

# Copy the JAR produced by the Ktor plugin (e.g., happinbackend-0.0.1.jar)
COPY --from=build /app/build/libs/*.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]