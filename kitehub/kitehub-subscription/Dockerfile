# KiteHub Subscription Service — Production Image (CI build)
# Self-contained multi-stage build for ECR push.
# Build context: kitehub/ (parent directory)
#
# Local dev fast-path: see kitehub-base/Dockerfile + kitehub/scripts/build-all.sh
# This Dockerfile is for CI/production where kitehub-base is unavailable.

# ============================================================================
# BUILD STAGE — JDK + Maven, build target module + deps
# ============================================================================
FROM maven:3.9-eclipse-temurin-26-noble AS builder
WORKDIR /build

# Copy parent + ALL sibling module pom stubs first (required for parent pom <modules> validation).
# Source for non-target modules is NOT copied — `mvn -pl <target> -am` only builds target + transitive deps.
COPY pom.xml ./
COPY kitehub-platform/pom.xml ./kitehub-platform/
COPY kitehub-subscription/pom.xml ./kitehub-subscription/
COPY kitehub-branding/pom.xml ./kitehub-branding/
COPY kitehub-email/pom.xml ./kitehub-email/
COPY kitehub-admin/pom.xml ./kitehub-admin/
COPY kitehub-gateway/pom.xml ./kitehub-gateway/

# Pre-download deps (cached layer when source changes)
RUN mvn -B -pl kitehub-subscription -am dependency:go-offline || true

# Copy source for target module + dependencies
COPY kitehub-platform/src ./kitehub-platform/src
COPY kitehub-subscription/src ./kitehub-subscription/src

# Build only this module + its inter-module deps
RUN mvn -B clean package -pl kitehub-subscription -am -DskipTests

# ============================================================================
# RUNTIME STAGE — Minimal JRE, non-root user, healthcheck
# ============================================================================
FROM eclipse-temurin:17-jre-noble AS runtime
WORKDIR /app

ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=dev

LABEL org.opencontainers.image.title="kitehub-subscription" \
      org.opencontainers.image.description="KiteHub Subscription Service" \
      org.opencontainers.image.source="https://github.com/VictorAurelius/2026-Kite-Class-Platform" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.vendor="Kite Platform"

# Curl for HEALTHCHECK
# GAP-442: Debian noble (Ubuntu 24.04) — replaces alpine 3.23 to clear gnutls CVE-2026-33845.
# GAP-456: apt-get upgrade applies Ubuntu 24.04.1 patches for dpkg/libcap2/sed
# (CVE-2026-2219 / CVE-2026-4878 / CVE-2026-5958 — Group A OS package bundle).
RUN apt-get update && \
    apt-get upgrade -y --no-install-recommends && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Non-root user
RUN groupadd --system --gid 1001 spring && \
    useradd --system --uid 1001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

# kitehub-subscription pom uses classifier=exec for executable jar
COPY --from=builder /build/kitehub-subscription/target/*-exec.jar app.jar
RUN chown -R spring:spring /app

USER spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
