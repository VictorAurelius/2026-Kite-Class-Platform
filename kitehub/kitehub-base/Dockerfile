# KiteHub Base Image
# Contains JDK, Maven, and cached dependencies for all services
#
# Usage: Child Dockerfiles use "FROM kitehub-base:latest AS builder"
# Build: docker build -t kitehub-base:latest -f kitehub-base/Dockerfile .

FROM eclipse-temurin:17-jdk-noble

LABEL maintainer="KiteHub Team"
LABEL description="Base image with JDK 17 and Maven dependencies for KiteHub services"

WORKDIR /build

# Install Maven and curl (for healthchecks in child images)
# GAP-442: Debian noble (Ubuntu 24.04) — replaces alpine 3.23 to clear gnutls CVE-2026-33845.
RUN apt-get update && apt-get install -y --no-install-recommends maven curl ca-certificates && rm -rf /var/lib/apt/lists/*

# Copy ALL pom files for dependency resolution
COPY pom.xml ./
COPY kitehub-platform/pom.xml ./kitehub-platform/
COPY kitehub-subscription/pom.xml ./kitehub-subscription/
COPY kitehub-branding/pom.xml ./kitehub-branding/
COPY kitehub-email/pom.xml ./kitehub-email/
COPY kitehub-admin/pom.xml ./kitehub-admin/
COPY kitehub-gateway/pom.xml ./kitehub-gateway/

# Create empty source directories (Maven requires them)
RUN mkdir -p kitehub-platform/src/main/java \
    kitehub-subscription/src/main/java \
    kitehub-branding/src/main/java \
    kitehub-email/src/main/java \
    kitehub-admin/src/main/java \
    kitehub-gateway/src/main/java

# Download ALL dependencies and cache them in this layer
# This is the expensive step - cached in base image
RUN mvn dependency:go-offline -B || true

# Clean up to reduce image size but keep Maven cache
RUN rm -rf /build/kitehub-*/src

# Set environment for child builds
ENV MAVEN_OPTS="-Xmx512m"
