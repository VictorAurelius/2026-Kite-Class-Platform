# KiteHub Subscription Service
# Extends kitehub-base for fast builds with cached dependencies
# Build context: kitehub/ (parent directory)

# ============================================================================
# BUILD STAGE - Uses base image with cached dependencies
# ============================================================================
FROM kitehub-base:latest AS builder
WORKDIR /build

# Copy source code for this module and its dependencies
COPY kitehub-platform/src ./kitehub-platform/src
COPY kitehub-subscription/src ./kitehub-subscription/src

# Build only this module and dependencies (fast - deps already cached)
RUN mvn clean package -pl kitehub-subscription -am -DskipTests -B

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

# Copy executable JAR (kitehub-subscription uses classifier=exec)
COPY --from=builder /build/kitehub-subscription/target/*-exec.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
