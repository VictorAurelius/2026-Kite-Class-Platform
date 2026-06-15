# GAP-1407: kitehub-banner-renderer chạy local nhưng KHÔNG deploy production (parity gap)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-15 (deploy-parity investigation: CI/AWS deploy vs local stack)
**Affects:** `kitehub-banner-renderer` (Playwright banner sidecar), AI Branding banner output (KH-6 / KC-10 flows)

## Problem

`kitehub-banner-renderer` (Playwright sidecar rasterise banner HTML → WebP) chạy trong local stack (`kitehub/docker-compose.kitehub.yml`) nhưng KHÔNG được deploy lên production:

- KHÔNG có trong CI `docker-build-push.yml` build matrix (grep `banner-renderer` = 0 match) → không build/push image lên ECR.
- KHÔNG có trong `infrastructure/terraform-aws/ecr.tf` (không có ECR repo).
- KHÔNG có trong `docker-compose.production.yml` / `docker-compose.kc.yml` (không deploy trên EC2 nào).

Hệ quả (per `kitehub/kitehub-branding/src/main/resources/application.yml:190-196`): prod default `BANNER_RENDERER_URL` rỗng → `kitehub-branding` fallback `StubBannerRenderer` (placeholder), KHÁC local dùng `PlaywrightBannerRenderer` (@Primary) render WebP thật. Code đề cập "GAP-1135" nhưng gap đó KHÔNG tồn tại trong `gap-status.csv` (untracked discovery per `discovery-to-gap-inline-filing.md`).

**Tác động G2/G3:** walk KH-6 (AI branding wizard) / KC-10 (per-tenant branding) ở local thấy banner render thật, prod chỉ ra stub placeholder → G2-local PASS ≠ prod behavior cho banner image. Đây là G3-infra parity item (prod chưa reproduce local banner output).

## Proposed Fix

Hoặc (A) deploy banner-renderer lên prod: thêm vào CI build-push matrix + ECR repo (`ecr.tf`) + service trong `docker-compose.production.yml` (Node Playwright, RAM-aware trên t3.medium) + wire `BANNER_RENDERER_URL` qua secrets/env trên kitehub-branding EC2; HOẶC (B) accept stub-in-prod cho Phase 1 BETA + document explicit "banner = stub placeholder Phase 1, real render Phase 2" trong ADR + remove dangling GAP-1135 reference. Quyết định A vs B = scope question (RAM budget t3.medium + Playwright Chromium ~300-500MB).

## Acceptance Criteria

- [ ] Quyết định A (deploy) hoặc B (accept stub + document) — ghi trong ADR hoặc gap closure
- [ ] Nếu A: banner-renderer trong CI build-push + ECR + prod compose + `BANNER_RENDERER_URL` wired; G3 walk verify WebP render trên AWS
- [ ] Nếu B: ADR ghi rõ Phase 1 = stub; dangling `GAP-1135` reference trong `application.yml` cleaned/retargeted
- [ ] `gap-status.csv` reflect quyết định

## Related

- Discovered in: deploy-parity investigation 2026-06-15 (session walk-G2 prep), branch `feature/deploy-parity-gaps-2026-06-15`
- Sister: GAP-1408 (docker-compose.kc.yml stale kiteclass-gateway — same investigation)
- Code ref: `kitehub/kitehub-branding/src/main/resources/application.yml:190-196` (BANNER_RENDERER_URL + StubBannerRenderer fallback); dangling "GAP-1135"
- ADR-025 §150 "AWS terraform completeness audit" (service consolidation decision deferred)
- Flow: KH-6 / KC-10 (branding wizard) G3-infra parity
