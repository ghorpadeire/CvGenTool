# Multi-stage build for CV Generator
# 1. Build stage: Compiles the application using Maven
# 2. Layer-extract stage: Splits JAR into cacheable layers
# 3. Runtime stage: Runs with minimal footprint + AppCDS for faster startup

# --- Build Stage ---
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

RUN apk add --no-cache maven

# Copy pom.xml first for dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Layer Extract Stage ---
# Splits the fat JAR into layers: dependencies, spring-boot-loader, snapshot-deps, application
# Dependencies layer (~60MB) rebuilds only when pom.xml changes
# Application layer (~200KB) rebuilds on every code change
FROM eclipse-temurin:17-jre-alpine AS layer-extractor
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# --- AppCDS Archive Stage ---
# Pre-builds a shared class data archive to cut JVM startup time by ~30-40%
FROM eclipse-temurin:17-jre-alpine AS cds-builder
WORKDIR /app
COPY --from=layer-extractor /app/dependencies/ ./
COPY --from=layer-extractor /app/spring-boot-loader/ ./
COPY --from=layer-extractor /app/snapshot-dependencies/ ./
COPY --from=layer-extractor /app/application/ ./
# Run once to generate CDS archive (exits immediately after class loading)
RUN java -XX:ArchiveClassesAtExit=/app/app-cds.jsa \
         -Dspring.context.exit=on-refresh \
         -Dspring.main.lazy-initialization=false \
         org.springframework.boot.loader.JarLauncher 2>/dev/null || true

# --- Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Pranav Ghorpade <ghorpade.ire@gmail.com>"
LABEL description="CV Generator"
LABEL version="1.0"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy layered JAR contents (dependency layers cached separately)
COPY --from=layer-extractor /app/dependencies/ ./
COPY --from=layer-extractor /app/spring-boot-loader/ ./
COPY --from=layer-extractor /app/snapshot-dependencies/ ./
COPY --from=layer-extractor /app/application/ ./

# Copy CDS archive for faster startup
COPY --from=cds-builder /app/app-cds.jsa ./app-cds.jsa

# Create writable data directory for runtime-editable resources
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8055

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8055}/ || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom \
               -XX:SharedArchiveFile=/app/app-cds.jsa \
               -Xshare:on \
               -XX:+UseZGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8055} org.springframework.boot.loader.JarLauncher"]
