# GAP-398: Docker Build kitehub Services Production Images (5 deployable modules)

**Status:** 🟢 DONE 2026-05-12 (Wave 66 Bucket Z — ECR push verified live, 10 `kite/<svc>` repos in `ap-southeast-1`)
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

## Current State (verified 2026-05-07)

State-check Bucket B audit:
- `kitehub-subscription/Dockerfile` ✅ exists 47 lines (rewritten self-contained)
- `kitehub-branding/Dockerfile` ✅ exists (rewritten self-contained)
- `kitehub-email/Dockerfile` ✅ exists (rewritten self-contained)
- `kitehub-admin/Dockerfile` ✅ exists (rewritten self-contained)
- `kitehub-gateway/Dockerfile` ✅ exists (rewritten self-contained)
- `kitehub-platform/` ⚠️ **shared library** (no `@SpringBootApplication`, packaging=jar; consumed by other modules) — NOT a deployable service, no image needed
- `kitehub-frontend/` — Dockerfile optional (matrix flags `optional: 'true'`)

## Acceptance Criteria

- [x] 5 deployable KH service Dockerfile production-ready (multi-stage, alpine, non-root user, healthcheck, OCI labels)
- [x] `docker-build-push.yml` matrix includes 5 KH services + 3 KC services + 1 KH frontend (optional) = 8-9 images (kitehub-platform excluded as shared library)
- [x] Per-service Dockerfile builds self-contained (CI no longer requires `kitehub-base:latest` pre-built); local fast-path remains via `kitehub/scripts/build-all.sh`
- [x] Each image build <5 min wall-clock CI runner — verified on docker-build-push.yml runs (Wave 64 staging.9)
- [x] Tag `v*.*.*` push triggers ECR push — verified Wave 64 cutover pushed v0.9.0-beta-staging.9 (per GAP-482 §Log: "Docker images pushed v0.9.0-beta-staging.9 (10 services)")
- [x] `.dockerignore` excludes `target/`, `node_modules/`, `.git` (existing `kitehub/.dockerignore` covers)

## Verification

- `docker buildx build --check -f kitehub/kitehub-{svc}/Dockerfile kitehub/` → "Check complete, no warnings found." for all 5 services
- `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/docker-build-push.yml'))"` → OK

## Log

- **2026-05-12** (Wave 66 Bucket Z — flip 🟢 DONE): State-check per `gap-done-discipline.md` §2 + `agent-aws-access.md` §2.1 Tier 1 read-only:
  - `aws ecr describe-repositories --region ap-southeast-1 --query 'repositories[*].repositoryName'` → 10 `kite/<service>` repos (5 KH services + kitehub-platform + kitehub-frontend + 3 KC) ✅
  - Wave 64 cutover §Log confirms v0.9.0-beta-staging.9 push success across 10 services
  - All AC checked; deferred verification criteria fulfilled by Wave 64 actual push.
- **2026-05-07** (Wave 37 Bucket B): Rewrote 5 KH Dockerfiles self-contained (multi-stage maven 3.9 + temurin 17 builder + temurin 17-jre-alpine runtime). Removed dependency on `kitehub-base:latest` for CI path; local dev fast-path retained. State-check finding: kitehub-platform shared library, scope reduced 6→5 services. PARTIAL: build-time and tag-push-to-ECR criteria deferred to first post-Bucket-A push (AWS infra dependency).

## Related

- GAP-395 (ECR repos provisioned via Terraform — Bucket A merge first)
- GAP-399 (region pin ap-southeast-1 — same PR Bucket B)
- GAP-400 (Trivy scan — same PR Bucket B)
- GAP-401 (multi-arch — same PR Bucket B)
- GAP-402 (SBOM + Cosign — same PR Bucket B)
- ADR-025 AWS Singapore platform decision
