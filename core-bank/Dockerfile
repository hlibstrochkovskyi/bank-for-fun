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
ARG OTEL_AGENT_VERSION=2.21.0
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app
WORKDIR /app
# OpenTelemetry Java agent: auto-instruments the app for distributed tracing. It is
# attached only when JAVA_TOOL_OPTIONS/OTEL_* env are set (see docker-compose), so
# the image runs fine without it (e.g. in tests/CI).
ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
COPY --from=build /workspace/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
