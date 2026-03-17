# KiteHub Admin Service Dockerfile
# Build context: kitehub/ (parent directory)

# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Copy ALL pom files for dependency resolution
COPY pom.xml ./
COPY kitehub-platform/pom.xml ./kitehub-platform/
COPY kitehub-subscription/pom.xml ./kitehub-subscription/
COPY kitehub-branding/pom.xml ./kitehub-branding/
COPY kitehub-email/pom.xml ./kitehub-email/
COPY kitehub-admin/pom.xml ./kitehub-admin/
COPY kitehub-gateway/pom.xml ./kitehub-gateway/

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -B || true

# Copy ALL source code (kitehub-admin depends on kitehub-subscription)
COPY kitehub-platform/src ./kitehub-platform/src
COPY kitehub-subscription/src ./kitehub-subscription/src
COPY kitehub-admin/src ./kitehub-admin/src

# Create empty src directories for modules not needed
RUN mkdir -p kitehub-branding/src/main/java \
    kitehub-email/src/main/java \
    kitehub-gateway/src/main/java

# Build the target module and all its dependencies
RUN mvn clean package -pl kitehub-admin -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup --system --gid 1001 spring && \
    adduser --system --uid 1001 spring

# Copy jar from builder
COPY --from=builder /build/kitehub-admin/target/*.jar app.jar

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
