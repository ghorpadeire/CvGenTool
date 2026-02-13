# Multi-stage build for CV Generator
# 1. Build stage: Compiles the application using Maven
# 2. Runtime stage: Runs the compiled JAR with minimal footprint

# --- Build Stage ---
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Pranav Ghorpade <ghorpade.ire@gmail.com>"
LABEL description="CV Generator"
LABEL version="1.0"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Create writable data directory for runtime-editable resources
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8055

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8055}/ || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8055} -jar app.jar"]
