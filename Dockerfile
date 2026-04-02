# Multi-stage build optimised for Render free tier (512MB RAM, shared CPU)
#
# Stages:
#   builder        - compiles the fat JAR with Maven
#   layer-extractor - splits JAR into cacheable layers (deps / app code)
#   cds-builder    - generates AppCDS archive for faster JVM startup
#   runtime        - minimal JRE-alpine image (~150MB)
#
# Key free-tier choices:
#   - G1GC (default) instead of ZGC: more memory-efficient at 512MB
#   - AppCDS: shaves ~1-2s off every cold start
#   - Layered JARs: only the ~200KB application layer rebuilds on code changes
#   - -Xshare:auto: gracefully skips CDS if archive is missing or incompatible

# --- Build Stage ---
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

RUN apk add --no-cache maven

# Copy pom.xml first — Docker caches this layer until pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# --- Layer Extract Stage ---
# Splits the fat JAR into four layers so Docker only pushes changed layers:
#   dependencies/        ~60MB  — rebuilt only when pom.xml changes
#   spring-boot-loader/  ~1MB   — rebuilt only on Spring Boot version change
#   snapshot-dependencies/ varies — rebuilt on SNAPSHOT dep changes
#   application/         ~200KB — rebuilt on every code change
FROM eclipse-temurin:17-jre-alpine AS layer-extractor
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# --- AppCDS Archive Stage ---
# Runs the app once to capture loaded classes into a shared archive.
# Reduces JVM startup by ~30-40% on subsequent container starts.
#
# Notes:
#   - CLAUDE_API_KEY is not set here; the app starts fine without it
#     (key is only validated when generating a CV, not at startup)
#   - On failure the stage falls back to an empty file; runtime uses
#     -Xshare:auto so JVM silently ignores an empty/incompatible archive
FROM eclipse-temurin:17-jre-alpine AS cds-builder
WORKDIR /app
COPY --from=layer-extractor /app/dependencies/ ./
COPY --from=layer-extractor /app/spring-boot-loader/ ./
COPY --from=layer-extractor /app/snapshot-dependencies/ ./
COPY --from=layer-extractor /app/application/ ./
RUN java -XX:ArchiveClassesAtExit=/app/app-cds.jsa \
         -Dspring.context.exit=on-refresh \
         -Dspring.main.lazy-initialization=false \
         org.springframework.boot.loader.JarLauncher 2>/dev/null \
    || touch /app/app-cds.jsa

# --- Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Pranav Ghorpade <ghorpade.ire@gmail.com>"
LABEL description="CV Generator — optimised for Render free tier"
LABEL version="1.0"

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Layered JAR contents (dependency layer cached separately from app code)
COPY --from=layer-extractor /app/dependencies/ ./
COPY --from=layer-extractor /app/spring-boot-loader/ ./
COPY --from=layer-extractor /app/snapshot-dependencies/ ./
COPY --from=layer-extractor /app/application/ ./

# CDS archive (empty file if training run failed — auto mode handles it)
COPY --from=cds-builder /app/app-cds.jsa ./app-cds.jsa

# Writable data directory for runtime-editable resources (candidate JSON, prompts)
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

USER appuser

# Render injects PORT=10000; expose that default
EXPOSE 10000

# Render uses healthCheckPath in render.yaml, not this HEALTHCHECK,
# but keep it for local Docker runs and Railway deployments.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-10000}/ || exit 1

# JVM flags tuned for Render free tier (512MB RAM, shared CPU):
#   UseContainerSupport  — respect Docker memory limits (not host RAM)
#   MaxRAMPercentage=70  — 70% of 512MB = ~358MB heap; leaves room for
#                          G1GC overhead, Metaspace, and OS threads
#   SharedArchiveFile    — AppCDS archive for faster class loading
#   Xshare:auto          — use archive if valid, skip silently if not
#   security.egd         — non-blocking entropy (avoids /dev/random stalls)
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=70.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:SharedArchiveFile=/app/app-cds.jsa \
               -Xshare:auto \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} org.springframework.boot.loader.JarLauncher"]
