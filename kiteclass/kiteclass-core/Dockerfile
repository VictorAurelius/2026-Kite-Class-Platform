# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21-noble AS builder

WORKDIR /app

# Copy pom.xml, checkstyle config and download dependencies
COPY pom.xml checkstyle.xml ./
RUN mvn dependency:go-offline -B

# Copy source code and build (skip tests and checkstyle for faster builds)
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true -Dcheckstyle.skip=true -B

# GAP-218 — verify Vietnamese-safe DejaVuSans fonts (regular + bold) are bundled in the
# fat jar. Production PDF rendering throws IllegalStateException("Font resource not found...")
# if the TTFs are missing — fail the image build instead of letting a broken image ship.
# See documents/05-guides/operations/runbooks/pdf-generation-font-not-found.md.
RUN test "$(jar tf target/*.jar | grep -cE 'fonts/DejaVuSans(-Bold)?\.ttf$')" -eq 2 \
    || (echo "FAIL: DejaVuSans TTF (regular + bold) missing from app.jar — see GAP-218 runbook" >&2 && exit 1)

# Stage 2: Runtime
# GAP-442: Debian noble (Ubuntu 24.04) — replaces alpine 3.23 to clear gnutls CVE-2026-33845.
FROM eclipse-temurin:21-jre-noble

# Build arguments for version tracking
ARG BUILD_TAG="dev"
ARG COMMIT_HASH="unknown"
ARG BUILD_DATE="unknown"
ARG PR_NUMBER="N/A"
ARG BRANCH="unknown"

# Labels for image metadata
LABEL org.opencontainers.image.title="KiteClass Core Service"
LABEL org.opencontainers.image.description="Core business logic service"
LABEL org.opencontainers.image.version="${BUILD_TAG}"
LABEL com.kiteclass.commit-hash="${COMMIT_HASH}"
LABEL com.kiteclass.build-date="${BUILD_DATE}"
LABEL com.kiteclass.pr-number="${PR_NUMBER}"
LABEL com.kiteclass.branch="${BRANCH}"

WORKDIR /app

# wget for HEALTHCHECK (alpine had busybox-wget by default; noble doesn't)
# GAP-456: apt-get upgrade applies Ubuntu 24.04.1 patches for dpkg/libcap2/sed
# (CVE-2026-2219 / CVE-2026-4878 / CVE-2026-5958 — Group A OS package bundle).
RUN apt-get update && \
    apt-get upgrade -y --no-install-recommends && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# GAP-470 (Wave 61 Bucket E) — run as non-root UID 1001 to satisfy K8s `runAsNonRoot: true`.
# UID 1001 matches project convention (kitehub-subscription, kitehub-gateway, etc.);
# UID 1000 is reserved by Ubuntu noble base image (`ubuntu` user).
RUN groupadd --system --gid 1001 spring \
 && useradd --system --uid 1001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

# Copy jar from builder (owned by spring so the read-only-rootfs container can load it).
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar
RUN chown -R spring:spring /app

USER spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
