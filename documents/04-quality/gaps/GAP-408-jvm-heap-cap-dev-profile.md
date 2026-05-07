# GAP-408: JVM Heap Cap trong Dev Profile

**Status:** 🟡 PARTIAL 2026-05-07 (Wave 37 Bucket D PR pending — smoke test 27GB blocked by GAP-244 dev stack)
**Priority:** 🟡 P2
**Domain:** DevOps / Local dev / JVM
**Found:** 2026-05-07 (Wave 37 — Layer 4)
**Affects:** Local 27GB RAM — giảm 30% memory footprint

## Problem

JVM default heap = 25% RAM máy → mỗi service Spring Boot có thể grab 4-6GB. 8 services × 4GB = 32GB không đủ 27GB.

## Proposed Fix

Dev compose profile inject `JAVA_TOOL_OPTIONS`:

```yaml
services:
  kitehub-branding:
    environment:
      JAVA_TOOL_OPTIONS: "-Xmx512m -Xms256m -XX:MaxMetaspaceSize=192m -XX:MaxRAM=600m"
```

Saves: 8 services × (4GB → 600MB) = **27GB → 5GB** for backend services.

Production deploy KHÔNG dùng cap này (production EC2 đủ RAM, để JVM tự tune).

## Acceptance Criteria

- [x] Dev `docker-compose.kitehub.yml` heap cap cho 6 Java services (subscription, branding, email, admin, gateway, kiteclass-core) — `JAVA_TOOL_OPTIONS` với defaults overrideable per-service env var
- [x] Production isolation — JVM cap chỉ áp ở compose dev; Helm values cho production deploy KHÔNG inject `JAVA_TOOL_OPTIONS`
- [ ] Smoke test 27GB stable — blocked by GAP-244 dev stack boot; verify-by-config thay smoke
- [x] Trade-off documented trong `documents/05-guides/dev/wsl2-config.md` §"Windows 11 OOM Killer" + per-service override path

## Log

- **2026-05-07 (Wave 37 Bucket D):** PARTIAL ship. 6 services có `JAVA_TOOL_OPTIONS` defaults; subscription/email/admin/gateway dùng 512m, branding/kiteclass-core dùng 768m (heavier). Override per-service qua env vars (`SUBSCRIPTION_JAVA_OPTS` etc.). Smoke test 27GB stable deferred → GAP-244 (dev stack boot fix). Coordinator-applied sau Sonnet agent thrash.

## Related

- GAP-407 (parent profiles)
- Spring Boot memory tuning best practice
