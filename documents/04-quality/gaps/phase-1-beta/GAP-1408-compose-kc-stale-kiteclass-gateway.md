# GAP-1408: docker-compose.kc.yml reference ECR image kiteclass-gateway đã bỏ (ADR-032 deploy drift)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-15 (deploy-parity investigation: CI/AWS deploy vs local stack)
**Affects:** `docker-compose.kc.yml` (kc-app EC2 production compose), KiteClass deploy path

## Problem

`docker-compose.kc.yml:86-89` định nghĩa service `kiteclass-gateway` pull ECR image `906286017800.dkr.ecr.ap-southeast-1.amazonaws.com/kite/kiteclass-gateway:${KITE_VERSION}`:

```yaml
  kiteclass-gateway:
    image: ...ecr.../kite/kiteclass-gateway:${KITE_VERSION}
    container_name: kiteclass-gateway
```

Nhưng `kiteclass-gateway` đã bị **remove** per ADR-032 (ACCEPTED, Option A — "Remove kiteclass-gateway service entirely"; routing dùng shared `kite-gateway` per ADR-023). CI `docker-build-push.yml:52` comment xác nhận "kiteclass-gateway removed per ADR-032 / GAP-001" và service KHÔNG còn trong build matrix → ECR repo `kite/kiteclass-gateway` không còn được build/push.

**Hệ quả:** nếu kc-app EC2 chạy `docker compose -f docker-compose.kc.yml up -d`, `docker compose pull kiteclass-gateway` sẽ FAIL (image tag `${KITE_VERSION}` không tồn tại trong ECR cho repo đã ngừng build) HOẶC pull tag cũ stale. Deploy-config drift: ADR-032 implement ở CI nhưng `docker-compose.kc.yml` chưa update.

## Proposed Fix

Remove block `kiteclass-gateway` (lines 86-89 + comment refs lines 11, 16) khỏi `docker-compose.kc.yml`; verify routing KiteClass đi qua shared `kite-gateway` (per ADR-023/ADR-032) trong production topology. Sweep các compose/docs khác còn reference `kite/kiteclass-gateway` ECR image per `cross-flow-bug-class-sweep.md`.

## Acceptance Criteria

- [ ] `kiteclass-gateway` service + ECR image reference removed khỏi `docker-compose.kc.yml`
- [ ] Routing KiteClass production confirmed qua shared `kite-gateway` (no orphan gateway)
- [ ] Grep sweep `kite/kiteclass-gateway` toàn repo = 0 active deploy reference (chỉ ADR historical OK)
- [ ] `variables.tf:66` stale comment "kiteclass-core + gateway" cập nhật (gateway = shared, không phải kc-local)

## Related

- Discovered in: deploy-parity investigation 2026-06-15 (session walk-G2 prep), branch `feature/deploy-parity-gaps-2026-06-15`
- Sister: GAP-1407 (banner-renderer prod-deploy — same investigation)
- ADR-032 (kiteclass-gateway removal, ACCEPTED) + ADR-023 (shared gateway) + GAP-001
- CI ref: `.github/workflows/docker-build-push.yml:52` (removal confirmed in build matrix)
- Code ref: `docker-compose.kc.yml:11,16,86-89`; `infrastructure/terraform-aws/variables.tf:66` (stale "Vercel" + gateway comment)
