# Stage 1: Build the application
# We use a JDK image to compile your Kotlin code.
FROM openjdk:17-jdk-slim AS build

# Set the working directory inside the container
WORKDIR /app

# Copy Gradle wrapper and essential build files first to leverage Docker cache
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY libs.versions.toml .

# Copy your source code
COPY src ./src

# Make the Gradle wrapper script executable
RUN chmod +x ./gradlew

# Build your Ktor application. The 'assemble' task (or 'build') will create the runnable JAR.
# The Ktor plugin handles bundling all dependencies.
RUN ./gradlew clean assemble --no-daemon

# Stage 2: Run the application
# Use a lightweight JRE image for running the compiled application.
FROM openjdk:17-jre-slim

# Set the working directory for the runtime stage.
WORKDIR /app

# Copy the runnable JAR from the 'build' stage into this new, smaller runtime image.
# The Ktor plugin typically names the JAR: <your-project-name>-<version>.jar
# Based on your build.gradle.kts, your project name is 'happinbackend' and version is '0.0.1'.
# So the JAR will be 'happinbackend-0.0.1.jar'.
# We use a wildcard `*.jar` to ensure it picks the correct one if the name changes slightly.
COPY --from=build /app/build/libs/*.jar ./app.jar

# Inform Docker that the container listens on port 8080.
# Render will inject the 'PORT' environment variable.
EXPOSE 8080

# Define the command that will be executed when the Docker container starts.
CMD ["java", "-jar", "app.jar"]