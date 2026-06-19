# syntax=docker/dockerfile:1

# --- Build stage: compile the Spring Boot fat jar -----------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Warm the Gradle distribution in its own layer for better caching.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version

# Build the application (tests run in CI, not in the image build).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- Runtime stage: small, non-root image -------------------------------------
FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
