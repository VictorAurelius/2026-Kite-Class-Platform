# KiteHub Gateway Service
# Extends kitehub-base for fast builds with cached dependencies
# Build context: kitehub/ (parent directory)

# ============================================================================
# BUILD STAGE - Uses base image with cached dependencies
# ============================================================================
FROM kitehub-base:latest AS builder
WORKDIR /build

# Copy source code for this module and its dependencies
COPY kitehub-platform/src ./kitehub-platform/src
COPY kitehub-gateway/src ./kitehub-gateway/src

# Build only this module and dependencies (fast - deps already cached)
RUN mvn clean package -pl kitehub-gateway -am -DskipTests -B

# ============================================================================
# RUNTIME STAGE - Minimal JRE image
# ============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 spring

# Copy JAR from builder
COPY --from=builder /build/kitehub-gateway/target/*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

USER spring

# Gateway runs on port 9000
EXPOSE 9000

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9000/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
