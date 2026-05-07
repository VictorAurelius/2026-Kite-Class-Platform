# GAP-398: Docker Build kitehub Services Production Images (6 modules)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 v0.9.0-beta
**Domain:** Docker / Release artifacts
**Found:** 2026-05-07 (Wave 37 — release-hardening Layer 2)
**Affects:** Phase 1 BETA deploy — KH services không thể deploy AWS nếu không có production images

## Problem

`.github/workflows/docker-build-push.yml` hiện chỉ build 3 image kiteclass (core/gateway/frontend). KiteHub services (kitehub-subscription, kitehub-branding, kitehub-email, kitehub-admin, kitehub-gateway, kitehub-platform) chưa có Dockerfile production-ready trong CI build matrix.

## Proposed Fix

1. Audit từng module `kitehub/kitehub-{subscription,branding,email,admin,gateway,platform}/` xem có Dockerfile production chưa
2. Tạo Dockerfile multi-stage cho module thiếu (jib alternative — Spring Boot Buildpack `./mvnw spring-boot:build-image` cũng OK)
3. Extend `docker-build-push.yml` strategy matrix với 6 KH services
4. Tag pattern: `kite-${service}:vX.Y.Z` + `latest` + git SHA
5. Push ECR repo per service (yêu cầu GAP-395 ECR repos provisioned)

## Acceptance Criteria

- [ ] 6 KH service Dockerfile production-ready (multi-stage, alpine/distroless base, non-root user)
- [ ] `docker-build-push.yml` matrix includes 6 KH services + 3 KC services + 2 frontends = 11 images
- [ ] Each image build <5 min wall-clock CI runner
- [ ] Tag `v*.*.*` push triggers ECR push for all 11
- [ ] Per-service `.dockerignore` excludes `target/`, `node_modules/`, `.git`

## Related

- GAP-395 (ECR repos provisioned via Terraform)
- GAP-399 (region pin ap-southeast-1)
- GAP-400 (Trivy scan)
- ADR-025 AWS Singapore platform decision
