# Stage 1: Build the application
FROM openjdk:17-jdk-slim AS build

WORKDIR /app

# Copy Gradle wrapper script
COPY gradlew .
# Copy the entire gradle directory. This line *should* copy gradle/libs.versions.toml
COPY gradle ./gradle
# Copy Gradle build configuration files
COPY build.gradle.kts .
COPY settings.gradle.kts .
# IMPORTANT: Ensure there is NO 'COPY libs.versions.toml .' line here or anywhere else.
# It's not needed because 'COPY gradle ./gradle' handles it.

# Copy your source code
COPY src ./src

# Make the Gradle wrapper executable
RUN chmod +x ./gradlew

# Use 'assemble' to build the runnable JAR with Ktor plugin
RUN ./gradlew clean assemble --no-daemon

# Stage 2: Run the application
FROM openjdk:17-slim

WORKDIR /app

# Copy the JAR produced by the Ktor plugin
COPY --from=build /app/build/libs/*.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]