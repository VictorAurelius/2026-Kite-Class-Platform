# GAP-1064: H2 `@SpringBootTest` ITs fail to boot — RLS interceptor runs Postgres `set_config()`

**Status:** 🟢 DONE 2026-06-14
**Priority:** 🟡 P2 (test-infra rot — H2 full-context ITs silently broken; masks coverage)
**Domain:** Backend
**Found:** 2026-06-08 (GAP-1062 TDD — affected-test run)
**Affects:** `kitehub-subscription` H2 `@SpringBootTest @ActiveProfiles("test")` ITs (confirmed: `SubscriptionBillingIT`; likely others)

## Problem

`SubscriptionBillingIT` (và khả năng các H2 full-context IT khác) **không boot được ApplicationContext**:

```
Unable to build Hibernate SessionFactory; SQLGrammarException: Unable to open JDBC
Connection for DDL execution [Function "SET_CONFIG" not found; SQL statement:
SELECT set_config('app.current_tenant_id', '', false), set_config('app.is_platform_admin', 'false', false)]
```

Root cause: tenant-aware DataSource interceptor (RLS context — Wave RLS sweep V34/V58) chạy Postgres-specific `set_config()` trên MỖI connection. H2 không có function `set_config` → SessionFactory build fail → mọi test trong class ERROR (context-load fail, 0.001s) thay vì chạy.

Hệ quả: các IT này **silently broken** — chúng "chạy" trong CI nhưng chỉ context-fail (4 ERROR), không thực sự test logic. Coverage tưởng có nhưng không. Phát hiện khi chạy affected-test cho GAP-1062 (4 errors `SubscriptionBillingIT`, tách biệt khỏi fix REQUIRES_NEW).

## Proposed Fix (chọn 1)

1. **Migrate H2 ITs → Testcontainers Postgres** (per `postgres-specific-type-testcontainers.md`) — real Postgres có `set_config`. Pattern: `OauthAttemptsRlsPostgresIT` / `SepayWebhookRollbackIsolationIT` (GAP-1062). Đúng nhất nhưng nặng (mỗi IT +container).
2. **H2 `CREATE ALIAS set_config`** trong test bootstrap (H2 compatibility shim) — rẻ nhưng H2 ≠ prod (vẫn miss Postgres-specific behavior).
3. **Skip RLS interceptor trong `test` profile** (conditional on profile) — nhưng mất coverage của interceptor.

Khuyến nghị: (1) cho IT chạm tenant-scoped/RLS tables; audit toàn bộ H2 `@SpringBootTest` ITs xem bao nhiêu đang silently broken.

## Acceptance Criteria

- [x] Audit: liệt kê mọi H2 `@SpringBootTest` IT fail set_config boot (grep + run)
- [x] Mỗi IT: migrate Testcontainers HOẶC document tại sao H2 đủ
- [x] CI gate: H2 boot-fail không còn ẩn dưới dạng "passed"

## Related

- Discovered in: GAP-1062 TDD affected-test run 2026-06-08 (verify branch)
- Pattern: `postgres-specific-type-testcontainers.md` (Testcontainers mandate)
- Reference IT: `OauthAttemptsRlsPostgresIT`, `SepayWebhookRollbackIsolationIT`

## Closure — wave-kitehub-biz-100 (2026-06-14)

🟢 DONE — engineering-complete + G3 production-parity walk verified. Test infra — H2 @SpringBootTest set_config boot-fail fixed (commit bbba1581f).

- G3 walk: `documents/04-quality/audits/persona-review/2026-06-13-g3-walk-kitehub-biz-100.md` (8 PASS / 1 PASS-with-P1 closed via GAP-1273 / 1 win-back async by-design).
- Tests: 963 backend (kitehub-subscription) + 906 frontend green.
- Consolidated into PR branch `wave/kitehub-biz-100`.
