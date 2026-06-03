---
id: GAP-915
title: check-schema-drift.sh build downstream module không -am → false-positive compile khi shared entity đổi
status: OPEN
priority: P2
phase: phase-1-beta
audience: dev
found: 2026-06-04
last_verified: 2026-06-04
related: [GAP-914, GAP-743]
---

# GAP-915 — check-schema-drift.sh stale-platform false-positive

## Problem

`scripts/check-schema-drift.sh` (CI gate `db-schema-drift`, Wave 14) validate kitehub-subscription/branding bằng:

```bash
./mvnw -q -f kitehub-subscription/pom.xml -DskipTests spring-boot:run ...   # line 147
```

Dùng `-f <module-pom>` **KHÔNG `-am`** → compile module downstream against **kitehub-platform jar trong ~/.m2 (stale)**. Khi 1 PR đổi shared entity trong `kitehub-platform` (vd thêm field), check compile against platform CŨ → `COMPILATION ERROR: symbol method setX not found` → check FAIL (false-positive).

**Worked example (PR #2140, 2026-06-04):** GAP-914 thêm `Payment.instanceId` + setter. Job "Test KiteHub Subscription Service" (build `-am`) PASS; runtime walk trên Postgres thật PASS. NHƯNG `db-schema-drift` FAIL compile `setInstanceId not found` vì build stale platform. Gate WARN-mode (`continue-on-error: true`) nên không block merge, nhưng kêu giả với MỌI PR đổi shared `kitehub-platform` entity sau này.

## Root Cause

Build downstream-only không rebuild upstream shared module. Sister với GAP-743 (entity-mapper triad) — cùng họ "shared-module change cần rebuild dependents".

## Proposed Fix

Trong `check-schema-drift.sh`: install kitehub-platform trước (`./mvnw -q -pl kitehub-platform -am install -DskipTests`) HOẶC đổi mỗi validate sang `-pl <module> -am` thay vì `-f <module-pom>`.

## Acceptance Criteria

- [ ] check-schema-drift.sh rebuild/install kitehub-platform trước khi validate downstream
- [ ] PR đổi shared entity (vd Payment field mới) → gate không false-positive compile
- [ ] Vẫn bắt được real schema drift (Hibernate validate vs migrated schema)

## Related

- Found via [[GAP-914]] PR #2140 (Payment.instanceId)
- [[GAP-743]] entity-migration-mapper triad (same shared-module-rebuild class)
- Gate WARN-mode `continue-on-error: true` → non-blocking; P2 không urgent
