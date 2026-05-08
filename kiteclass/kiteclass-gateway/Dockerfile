# Multi-stage build for KiteClass Gateway

# Stage 1: Build
FROM eclipse-temurin:17-jdk-noble AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml checkstyle.xml ./

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build application (skip tests and checkstyle for faster builds)
RUN ./mvnw clean package -DskipTests -Dcheckstyle.skip=true -B

# Stage 2: Runtime
# GAP-442: Debian noble (Ubuntu 24.04) — replaces alpine 3.23 to clear gnutls CVE-2026-33845.
FROM eclipse-temurin:17-jre-noble

# Build arguments for version tracking
ARG BUILD_TAG="dev"
ARG COMMIT_HASH="unknown"
ARG BUILD_DATE="unknown"
ARG PR_NUMBER="N/A"
ARG BRANCH="unknown"

# Labels for image metadata
LABEL org.opencontainers.image.title="KiteClass Gateway Service"
LABEL org.opencontainers.image.description="API Gateway with authentication"
LABEL org.opencontainers.image.version="${BUILD_TAG}"
LABEL com.kiteclass.commit-hash="${COMMIT_HASH}"
LABEL com.kiteclass.build-date="${BUILD_DATE}"
LABEL com.kiteclass.pr-number="${PR_NUMBER}"
LABEL com.kiteclass.branch="${BRANCH}"

WORKDIR /app

# wget for HEALTHCHECK
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd --system spring && useradd --system -g spring --no-create-home --shell /usr/sbin/nologin spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
