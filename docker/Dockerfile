# ===== Builder Stage =====
# Use specific JDK version for reproducible builds
FROM eclipse-temurin:21-jdk-alpine as builder

# Set working directory
WORKDIR /app

# Copy Gradle files first to optimize Docker layer caching
# These layers won't change unless dependencies change
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies - this will be cached unless dependencies change
RUN ./gradlew dependencies --no-daemon || return 0

# Copy the source code
COPY src ./src

# Build the application (skip tests as they should be run before Docker build)
RUN ./gradlew build -x test --no-daemon

# ===== Runtime Stage =====
# Use smaller JRE image for runtime
FROM eclipse-temurin:21-jre-alpine

# Add labels for better maintainability
LABEL maintainer="team@example.com"
LABEL application="testwigr"
LABEL version="1.0.0"

# Set timezone
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install additional runtime dependencies
RUN apk --no-cache add curl bash

# Create a dedicated user for the application (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built artifact from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set directory permissions
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Configure JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod"

# Add a health check
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set entry point with proper exec form to ensure signal propagation
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
