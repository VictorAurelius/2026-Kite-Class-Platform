# GAP-600: beta_requests abort mid-walkthrough → stale PENDING + unique-constraint fail

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Phase:** phase-1-beta
**Found:** 2026-05-17 (Wave 87 outside-in audit #3 — failure-mode matrix)
**Affects:** Dev iteration cycle khi self-test fail mid-flow + re-run cost (10 phút `docker compose down -v` + re-seed)

## Problem

Khi developer abort flow `beta_request submit → admin approve → tenant provision` giữa chừng (Ctrl+C terminal, browser close, hoặc walkthrough lỗi step N), database state lại:

1. `beta_requests` row status `PENDING` (chưa được admin approve)
2. `admin_audit_logs` half-row (nếu admin đã start approve nhưng chưa commit)
3. RabbitMQ email queue có message stuck (welcome email chưa gửi nhưng đã enqueue)

Re-run walkthrough từ đầu với cùng email (`hang@sky-education.vn`) → backend trả `409 Conflict` vì `UNIQUE (email)` constraint trên `beta_requests` table. Developer phải:

- `docker compose down -v` → wipe toàn bộ volume (mất ~10 phút restart + re-seed)
- HOẶC manually `DELETE FROM beta_requests WHERE email='...'` qua psql (rủi ro cao, dễ wipe sai)

Trong context Phase 1 BETA acceptance walkthrough 126 rows, mỗi lần dev abort + re-run là 10 phút wasted. Bucket B đã ship `scripts/dev/self-test-reset.sh` (partial mitigation — reset DB script), nhưng không cover idempotent retry case.

## Root Cause

Backend `kitehub-subscription` `BetaRequestService.submit(...)` không có:

- **Scheduled cleanup job** cho stale `PENDING` rows (vd cron mỗi giờ xóa rows `PENDING AND created_at < NOW() - INTERVAL '1 hour'`)
- **Idempotent retry mechanism** — submit lại với cùng email → INSERT thay vì UPSERT → unique violation
- **Atomic transaction boundary** — admin approve hiện không atomic với audit log + email enqueue → nếu fail giữa chừng → partial state

**State-check evidence** (2026-05-17):

```
$ grep -rnE "beta_requests.*stale|cleanup.*pending|cleanup.*beta" kitehub 2>/dev/null --include="*.java" --include="*.sql"
(zero hits)
```

→ Không có cleanup logic nào trong codebase cho `beta_requests` stale rows. Confirmed gap.

## Proposed Fix

**Option A — Scheduled cleanup job** (preferred):
- `kitehub-subscription` `@Scheduled(cron = "0 0 * * * *")` job chạy mỗi giờ
- Query: `DELETE FROM beta_requests WHERE status='PENDING' AND created_at < NOW() - INTERVAL '1 hour'`
- Cascade clean `admin_audit_logs` orphan rows (nếu FK setup) + DLQ stuck messages
- Audit log every cleanup batch (count + reason) cho observability

**Option B — Idempotent UPSERT cho re-submit**:
- `POST /api/v1/beta-requests` schema: nếu email exists VÀ status `PENDING` → UPDATE timestamp + return same row (idempotent)
- Nếu email exists VÀ status `APPROVED` → 409 Conflict (legitimate dup)
- Trade-off: complicates business logic; cần thêm test coverage abort scenarios

**Option C — Bucket B partial mitigation đã ship**:
- `scripts/dev/self-test-reset.sh` (Wave 87 Bucket B) — reset DB nhanh không cần `docker compose down -v`
- Acceptable cho dev local; KHÔNG đủ cho production scenarios.

Recommended: Option A + Option C cho Phase 1 BETA. Option B defer khi production load tăng.

## Acceptance Criteria

- [ ] Scheduled cleanup job shipped trong `kitehub-subscription` (`@Scheduled` Spring Boot)
- [ ] Stale `beta_requests` rows status `PENDING > 1h` được auto-cleaned mỗi giờ
- [ ] Cleanup count + timestamp logged vào `admin_audit_logs` (observability)
- [ ] Re-submit cùng email sau cleanup → INSERT thành công (không bị 409)
- [ ] Test coverage abort scenarios: integration test simulate Ctrl+C giữa chừng + re-run → pass
- [ ] DLQ cleanup cho RabbitMQ stuck messages (companion job hoặc TTL on queue)
- [ ] Documentation: `documents/05-guides/operations/runbooks/beta-request-cleanup-runbook.md` mô tả cleanup cadence + manual override

## Related

- Audit: Wave 87 outside-in audit #3 (failure-mode matrix) — `documents/03-planning/waves/wave-2026-05-17-87-dev-self-test-enablement.md` §3 Bucket E
- Sister: GAP-525 (invite E2E P0), Bucket B `scripts/dev/self-test-reset.sh` (Wave 87 partial mitigation)
- Rule applied: `.claude/rules/pre-handoff-self-test-completeness.md` §2.9 background job/async — DLQ + retry mechanism
- Business doc: `documents/01-business/kitehub/beta-access/rules.md` (cleanup retention policy candidate)

## Log

- **2026-05-17:** Gap filed Wave 87 Bucket E. Outside-in audit #3 failure-mode matrix phát hiện class này khi simulate "dev Ctrl+C giữa chừng walkthrough". State-check confirmed zero cleanup logic cho stale `beta_requests`. P1 (không chặn cứng vì Bucket B reset script đã giảm thiểu friction; nhưng vẫn cần production-grade cleanup cho cohort scale). Defer code fix Wave 88+.
