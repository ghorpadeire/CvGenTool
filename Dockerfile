# =============================================================================
# Dockerfile - CV Generator Application
# =============================================================================
#
# Multi-stage build for optimized image size:
# 1. Build stage: Compiles the application using Maven
# 2. Runtime stage: Runs the compiled JAR with minimal footprint
#
# @author Pranav Ghorpade
# @version 1.0
#
# Interview Note:
# Multi-stage builds are a Docker best practice. They keep the final image
# small by only including runtime dependencies, not build tools.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build Stage
# -----------------------------------------------------------------------------
# Using Eclipse Temurin (formerly AdoptOpenJDK) as the base image
# Temurin is the recommended open-source JDK distribution
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Install Maven (Alpine package)
RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
# This layer is cached unless pom.xml changes
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
# Tests should be run in CI pipeline before Docker build
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2: Runtime Stage
# -----------------------------------------------------------------------------
# Using JRE-only image for smaller footprint
FROM eclipse-temurin:17-jre-alpine

# Install wget for health checks (not included in alpine by default)
RUN apk add --no-cache wget

# Add metadata labels
LABEL maintainer="Pranav Ghorpade <pranav.ghorpade3108@gmail.com>"
LABEL description="CV Generator - Powered by Claude AI"
LABEL version="1.0"

# Create non-root user for security
# Running as root in containers is a security risk
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
# Using explicit pattern to avoid matching .jar.original files
COPY --from=builder /app/target/cv-generator-*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the application port (Railway uses PORT env variable)
EXPOSE 8055

# Health check endpoint
# Checks if the application is responding
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8055}/ || exit 1

# JVM options for containerized environments
# -XX:+UseContainerSupport: Detect container memory limits
# -XX:MaxRAMPercentage=75: Use 75% of available memory
# -Djava.security.egd: Faster random number generation
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Entry point with shell form for variable expansion
# Railway provides PORT environment variable
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8055} -jar app.jar"]
