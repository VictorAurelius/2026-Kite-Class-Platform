# GAP-1408: ADR-032 kiteclass-gateway removal cleanup INCOMPLETE (~19 active files still reference removed service)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-15 (deploy-parity investigation: CI/AWS deploy vs local stack → design-first traced to ADR-032 partial execution)
**Affects:** `docker-compose.kc.yml`, `kiteclass/kiteclass-gateway/` source, `ecr.tf`, `iam.tf`, ~19 active files; KiteClass deploy config integrity

## Problem

ADR-032 (ACCEPTED) chọn **Option A — remove `kiteclass-gateway` entirely** (shared `kite-gateway` đã cover routing per ADR-023). ADR liệt kê 11-step cleanup (154 file) định làm "Wave 96 PR2". **Execution dở dang — chỉ ~30% done:**

| ADR-032 step | Status |
|---|---|
| 3. `gateway-ci.yml` workflow deleted | ✅ done |
| 4. `docker-build-push.yml` build entries removed | ✅ done (comment-only ref) |
| 8. k8s `gateway-deployment.yaml` deleted | ✅ done |
| **1. `kiteclass/kiteclass-gateway/` folder** | ❌ còn (3 tracked file) |
| **2. `kiteclass/docker-compose.dev.yml` gateway block** | ❌ còn reference |
| **6. `infrastructure/terraform-aws/ecr.tf` ECR repo** | ❌ còn declared |
| **7. `infrastructure/terraform-aws/iam.tf` comment** | ❌ còn ref |
| **+ ~13 file khác** (docker-compose.kc.yml, k8s ingress/frontend-deployment, 8 scripts, prometheus.yml, dependabot.yml, .codecov.yml) | ❌ còn reference |

Nguyên gốc phát hiện: `docker-compose.kc.yml:86-89` pull ECR image `kite/kiteclass-gateway:${KITE_VERSION}` mà CI không còn build → `docker compose pull` fail/stale. Nhưng design-first trace ra root = ADR-032 cleanup chưa xong, không phải 1 dòng lẻ.

**Bối cảnh topology Phase 1 (alb-architecture.md):** ALB target group `kc_app` đã gỡ (Wave 68/GAP-501), KC backend dời **Phase 7** (GAP-445). Phase 1 BETA: Cloudflare → thẳng IP EC2 kc-app; nginx proxy `/api/` → kh-backend nội bộ → shared `kite-gateway`. → `docker-compose.kc.yml` có thể không còn active-path Phase 1 (cần verify trước khi xóa block lẻ). Header kc.yml line 4 "Frontend served by Vercel" cũng stale (ec2-kc-app.tf self-host 2 FE).

## Proposed Fix

Thực thi nốt ADR-032 11-step cleanup (sweep ~19 file còn reference per `cross-flow-bug-class-sweep.md` §4.1 — class detect được tĩnh nên cân nhắc CI detector grep `kiteclass-gateway` active-path). Wave-sized — disjoint buckets: (A) source folder + dev compose, (B) terraform ecr.tf+iam.tf, (C) k8s manifests, (D) scripts ×8 + prometheus/dependabot/codecov, (E) docker-compose.kc.yml (gắn với Phase 7 KC-backend deferral — quyết định kc.yml còn dùng Phase 1 không). KHÔNG xóa block lẻ — sửa atomic theo ADR.

## Acceptance Criteria

- [ ] `grep -rlE "kiteclass-gateway" --include="*.yml" --include="*.tf" --include="*.sh" .` (excl docs/adr/git-history) = 0 active reference
- [ ] `kiteclass/kiteclass-gateway/` folder removed (3 tracked file)
- [ ] `ecr.tf` ECR repo + `iam.tf` ref removed (terraform plan no orphan)
- [ ] docker-compose.kc.yml: kiteclass-gateway block removed OR file đánh dấu Phase-7-deferred + header "Vercel"/gateway stale comment sửa
- [ ] ADR-032 §Consequences cập nhật "cleanup completed Wave X"; GAP-001 link
- [ ] CI detector grep `kiteclass-gateway` active-path (cân nhắc per `cross-flow-bug-class-sweep.md` §4.1)

## Related

- Discovered in: deploy-parity investigation 2026-06-15 (session walk-G2 prep), branch `feature/deploy-parity-gaps-2026-06-15`
- Sister: GAP-1407 (banner-renderer prod-deploy — same investigation)
- **ADR-032** (kiteclass-gateway removal, ACCEPTED, Option A 11-step — execution incomplete) + ADR-023 (shared gateway) + GAP-001 (original decision request)
- `alb-architecture.md` (ALB target kc_app removed Wave 68 + KC backend Phase 7 defer GAP-445/GAP-501)
- `cross-flow-bug-class-sweep.md` §4.1 (statically-detectable class → persistent detector)
- `audit-to-gap-pipeline.md` §2.7 (decision-doc → code-sync direction)
