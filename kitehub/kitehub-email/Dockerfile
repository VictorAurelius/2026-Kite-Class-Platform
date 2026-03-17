# KiteHub Email Service Dockerfile
# Build context: kitehub/ (parent directory)

# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Copy ALL pom files for dependency resolution (Maven multi-module requirement)
COPY pom.xml ./
COPY kitehub-platform/pom.xml ./kitehub-platform/
COPY kitehub-subscription/pom.xml ./kitehub-subscription/
COPY kitehub-branding/pom.xml ./kitehub-branding/
COPY kitehub-email/pom.xml ./kitehub-email/
COPY kitehub-admin/pom.xml ./kitehub-admin/
COPY kitehub-gateway/pom.xml ./kitehub-gateway/

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -pl kitehub-platform,kitehub-email -am -B || true

# Copy source code for required modules
COPY kitehub-platform/src ./kitehub-platform/src
COPY kitehub-email/src ./kitehub-email/src

# Create empty src directories for other modules (Maven requires them)
RUN mkdir -p kitehub-subscription/src/main/java \
    kitehub-branding/src/main/java \
    kitehub-admin/src/main/java \
    kitehub-gateway/src/main/java

# Build only the target module and its dependencies
RUN mvn clean package -pl kitehub-email -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 spring

# Copy jar from builder
COPY --from=builder /build/kitehub-email/target/*.jar app.jar

# Set ownership
RUN chown -R spring:spring /app

USER spring

# Expose port
EXPOSE 8080

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
