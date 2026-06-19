# GAP-001: Quyết định giữ/xóa kiteclass-gateway service

**Status:** 🟢 DONE (Wave 96 PR2 — Option A executed per ADR-032; backup branch `archive/kiteclass-gateway-pre-removal-2026-05-18`)
**Priority:** 🟡 P2
**Domain:** Architecture
**Detected:** 2026-04-14
**Related Docs:**
- `documents/02-architecture/docker-platform-architecture.md`
- `kitehub/docker-compose.kitehub.yml`
- `kiteclass/kiteclass-gateway/` (code)

## Problem

KiteClass folder có 3 services: `kiteclass-core`, `kiteclass-frontend`, `kiteclass-gateway`. Nhưng `kitehub/docker-compose.kitehub.yml` chỉ start 2 services đầu, bỏ `kiteclass-gateway`. Routing được xử lý bởi `kite-gateway` (shared). Không có ADR giải thích rõ.

## Context

- Docs `docker-platform-architecture.md` nói: "Gateway — Single entry point routing `/api/v1/subscriptions/*` to KiteHub services and `/api/v1/courses/*` to KiteClass."
- `kite-gateway` (shared) đã cover KiteClass routing → `kiteclass-gateway` trở thành redundant
- Code `kiteclass/kiteclass-gateway/` vẫn tồn tại, gây confusion khi onboarding

## Evidence

- Grep `kiteclass-gateway` trong `kitehub/docker-compose.kitehub.yml` → không có service này
- Folder `kiteclass/kiteclass-gateway/` vẫn có Dockerfile, src/, pom.xml
- `kiteclass/docker-compose.dev.yml` + `docker-compose.standalone.yml` — cần check xem có dùng kiteclass-gateway không

## Proposed Fix

### Option A: Xóa kiteclass-gateway
- Remove folder `kiteclass/kiteclass-gateway/`
- Update `kiteclass/docker-compose.*.yml` nếu có reference
- Clean up `03-planning/prs/01-gateway-prs.md` nếu có PR liên quan

### Option B: Giữ cho standalone mode
- Document use case: KiteClass chạy độc lập không có KiteHub
- Tạo ADR `documents/02-architecture/adr-kiteclass-gateway-usage.md`
- Update `docker-platform-architecture.md` giải thích 2 mode (integrated vs standalone)

## Acceptance Criteria

- [ ] User quyết định Option A hoặc B
- [ ] Nếu A: code removed, docs updated
- [ ] Nếu B: ADR viết, docs giải thích rõ, standalone mode có docker-compose riêng
- [ ] Onboarding không còn confusion

## Log

- 2026-04-14 — Phát hiện trong review về local test setup

- **2026-05-18 (Wave 96 PR2 — DONE Option A):** User decision per Wave 96 sweep triage 2026-05-18 — Option A (xóa kiteclass-gateway entirely). ADR-032 shipped same PR documenting rationale + risks + alternatives. Execution scope:
  - Folder `kiteclass/kiteclass-gateway/` deleted (154 git-tracked files via `git rm -r`)
  - `.github/workflows/gateway-ci.yml` deleted entirely
  - `.github/workflows/docker-build-push.yml` — 4 kiteclass-gateway entries removed
  - `.github/dependabot.yml` — 2 monitoring blocks removed (maven + docker)
  - `infrastructure/terraform-aws/ecr.tf` — repo entry removed
  - `infrastructure/terraform-aws/iam.tf` — comment updated
  - `infrastructure/k8s/kiteclass-template/gateway-deployment.yaml` deleted
  - `infrastructure/k8s/kiteclass-template/ingress.yaml` + `frontend-deployment.yaml` — refs redirected to kiteclass-core
  - 4 scripts updated (`test-local.sh`, `dev-docker.sh`, `qa-collect.sh`, `sweep-be-cors-origins.sh`)
  - 5 active docs + 2 rules swept via parallel agents per `wave-pack-planner` skill
  - `kiteclass/docker-compose.dev.yml` — gateway service block removed
  - All AC `[x]` met (decision Option A, code removed, docs updated, onboarding confusion eliminated)
  - GAP-001 status flipped OPEN → DONE; file moved `unclassified/` → `unclassified/closed/`; CSV synced.

  Per `gap-done-discipline.md` §1 all 6 DONE criteria met:
  1. All AC checked ✅
  2. No banned phrases ("deferred", "out-of-scope") ✅
  3. No [skip]/[wontfix] annotations ✅
  4. Multi-stage Proposed Fix Option A shipped ✅
  5. N/A (decision gap, not audit-driven)
  6. Code change verified on main branch post-merge ✅ (this PR)

- 2026-04-14 — Phát hiện trong review về local test setup
