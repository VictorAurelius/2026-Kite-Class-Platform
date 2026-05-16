# KiteHub Admin Service — Production Image (CI build)
# Self-contained multi-stage build for ECR push.
# Build context: kitehub/ (parent directory)
# Note: kitehub-admin depends on kitehub-platform + kitehub-subscription.

# ============================================================================
# BUILD STAGE
# ============================================================================
FROM maven:3.9-eclipse-temurin-26-noble AS builder
WORKDIR /build

# Copy parent + ALL sibling module pom stubs first (parent <modules> validation).
COPY pom.xml ./
COPY kitehub-platform/pom.xml ./kitehub-platform/
COPY kitehub-subscription/pom.xml ./kitehub-subscription/
COPY kitehub-branding/pom.xml ./kitehub-branding/
COPY kitehub-email/pom.xml ./kitehub-email/
COPY kitehub-admin/pom.xml ./kitehub-admin/
COPY kitehub-gateway/pom.xml ./kitehub-gateway/

RUN mvn -B -pl kitehub-admin -am dependency:go-offline || true

COPY kitehub-platform/src ./kitehub-platform/src
COPY kitehub-subscription/src ./kitehub-subscription/src
COPY kitehub-admin/src ./kitehub-admin/src

RUN mvn -B clean package -pl kitehub-admin -am -DskipTests

# ============================================================================
# RUNTIME STAGE
# ============================================================================
FROM eclipse-temurin:17-jre-noble AS runtime
WORKDIR /app

ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=dev

LABEL org.opencontainers.image.title="kitehub-admin" \
      org.opencontainers.image.description="KiteHub Admin Service" \
      org.opencontainers.image.source="https://github.com/VictorAurelius/2026-Kite-Class-Platform" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="Kite Platform"

# GAP-442: Debian noble (Ubuntu 24.04) — replaces alpine 3.23 to clear gnutls CVE-2026-33845.
# GAP-456: apt-get upgrade applies Ubuntu 24.04.1 patches for dpkg/libcap2/sed
# (CVE-2026-2219 / CVE-2026-4878 / CVE-2026-5958 — Group A OS package bundle).
RUN apt-get update && \
    apt-get upgrade -y --no-install-recommends && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 1001 spring && \
    useradd --system --uid 1001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

COPY --from=builder /build/kitehub-admin/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
