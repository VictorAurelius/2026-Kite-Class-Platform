# GAP-408: JVM Heap Cap trong Dev Profile

**Status:** 🔵 OPEN
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

- [ ] Dev `docker-compose.kitehub.yml` heap cap cho 8 services
- [ ] Production overlay `docker-compose.prod.yml` (HOẶC Helm values) KHÔNG override (production tune separately)
- [ ] Smoke test: `./kitehub/scripts/up.sh full` chạy stable trong 27GB
- [ ] Document trade-off: dev cap có thể trigger GC pressure ở edge cases — nếu OOM exception, increase per-service

## Related

- GAP-407 (parent profiles)
- Spring Boot memory tuning best practice
