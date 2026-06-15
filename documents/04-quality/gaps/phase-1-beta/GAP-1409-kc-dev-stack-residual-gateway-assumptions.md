# GAP-1409: KiteClass dev-stack giữ giả định gateway-era sau khi kiteclass-gateway bị xóa

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** DevOps
**Found:** 2026-06-15 (GAP-1408 ADR-032 cleanup agent — out-of-scope discoveries)
**Affects:** `kiteclass/docker-compose.dev.yml`, `kiteclass/scripts/dev-start.sh` (dev sandbox only — non-production)

## Problem

Trong lúc dọn ADR-032 (GAP-1408), agent phát hiện 2 chỗ dev-stack KiteClass còn giả định `kiteclass-gateway` tồn tại (không chứa literal token nên không bị scrub, nhưng logic đã sai sau khi gateway removed):

1. **`docker-compose.dev.yml` core service: `SPRING_FLYWAY_ENABLED: false`** với comment "Gateway manages all migrations" — nhưng dedicated gateway đã bị xóa per ADR-032. → dev standalone stack có thể **không chạy Flyway migration** (không service nào còn gánh việc đó). Latent migration-architecture gap cho dev sandbox.
2. **`dev-start.sh`: `NEXT_PUBLIC_API_URL=http://localhost:8080`** (port gateway cũ) — kiteclass-core nay bind `:8081`. FE dev trỏ sai port.

Impact thấp: cả 2 chỉ ảnh hưởng **dev sandbox standalone** (`docker-compose.dev.yml`), KHÔNG phải canonical `docker-compose.kitehub.yml` hay production. Per ADR-032 standalone mode "không có active use case ngoài dev sandbox".

## Proposed Fix

1. `docker-compose.dev.yml`: đổi core `SPRING_FLYWAY_ENABLED: true` (core tự gánh migration) HOẶC document rõ ai chạy Flyway trong standalone mode + sửa comment "Gateway manages..." (stale).
2. `dev-start.sh`: `NEXT_PUBLIC_API_URL` → `http://localhost:8081` (core port) HOẶC qua shared kite-gateway nếu dev dùng integrated mode.

## Acceptance Criteria

- [ ] `docker-compose.dev.yml` Flyway ownership rõ ràng post-gateway-removal (core gánh OR documented)
- [ ] `dev-start.sh` API URL port đúng (8081 core hoặc gateway)
- [ ] Comment "Gateway manages all migrations" stale sửa

## Related

- Discovered in: GAP-1408 ADR-032 cleanup agent (branch worktree-agent), 2026-06-15
- Parent: GAP-1408 (ADR-032 kiteclass-gateway removal) + ADR-032
- Per `discovery-to-gap-inline-filing.md` (out-of-scope discovery during cleanup work)
