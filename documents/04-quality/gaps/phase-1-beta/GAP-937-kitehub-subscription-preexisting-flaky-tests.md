# GAP-937: kitehub-subscription preexisting flaky tests block strict-warnings CI

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 pre-walk batch-fix PR #2150 CI surfaced)
**Affects:** Every PR touching `kitehub/kitehub-subscription/**` — Test KiteHub Subscription Service CI job FAILs even on changes unrelated to flaky tests.

## Problem

Trên `main` HEAD (`c1a9deb0` + sau merge các wave gần đây), 12 test failures + 2 errors trong cluster `kitehub-subscription` Mockito UnnecessaryStubbingException:

| Test class | Test name | Lý do |
|---|---|---|
| `EmailServiceClientTest$OutboxFastPath` | `brokerDownDoesNotPropagate` (line 574) | UnnecessaryStubbing — strict warnings detect unused stub |
| `SubscriptionEventEmitterTest` | `emit_fast_path_failure_does_not_throw_outbox_still_saved` (line 104) | UnnecessaryStubbing |
| `SubscriptionOutboxDispatcher*` (per Agent A pre-walk report) | (chưa list cụ thể từng test) | UnnecessaryStubbing tương tự |
| `(7 cases khác)` | — chưa enumerate đầy đủ | Cùng class strict-warnings vi phạm Mockito.STRICT_STUBS |

Hậu quả: mọi PR touching `kitehub-subscription` đều fail CI ở job "Test KiteHub Subscription Service (strict-warnings — GAP-245)" dù change KHÔNG liên quan các test này. Cần dùng `ADMIN_MERGE_OVERRIDE` trailer per `.claude/rules/admin-merge-discipline.md` v1.0.3 §4 cho mỗi merge tới khi fix.

Tiền lệ: GAP-735 trong `kiteclass-core` đã đóng theo Wave meta-3 với cùng class. Đây là class lặp lại ở `kitehub-subscription`.

## Root Cause

Mockito strict-stubs mode (`@MockitoSettings(strictness = Strictness.STRICT_STUBS)` hoặc Mockito 5 default) phát hiện stub được khai báo nhưng method được tested không gọi tới — thường vì:
1. Code production thay đổi (vd RabbitTemplate.send vs convertAndSend signature swap per PR ac54a419 hoặc tương đương)
2. Test stub setup cho method cũ chưa cleanup
3. Outbox / event emit flow refactor đổi gọi method khác

Cần đọc từng test fail + cleanup `when(...)` unused stubs HOẶC swap sang `lenient()` cho test đó (nếu thực sự cần stub đó trong setup chung).

## Proposed Fix

Wave dedicated cho test cleanup (~1-2h effort):
1. Run `cd kitehub && ./mvnw -pl kitehub-subscription test -Dtest=EmailServiceClientTest,SubscriptionEventEmitterTest,SubscriptionOutboxDispatcher* 2>&1 | tee /tmp/flaky.log`
2. Cho mỗi test fail, đọc stack trace → identify unused stub line
3. Quyết định: (a) remove unused stub, hoặc (b) move to `lenient()` nếu stub vẫn cần cho test khác trong cùng class
4. Re-run cluster đến khi xanh
5. Verify full `mvn verify -pl kitehub-subscription` clean

Cân nhắc: nếu nhiều stubbing cố ý chung qua `@BeforeEach`, có thể switch class-level `@MockitoSettings(strictness = Strictness.LENIENT)` nhưng đó là regression với GAP-245 strict-warnings spirit — chỉ dùng khi thật sự cần.

## Acceptance Criteria

- [ ] `./mvnw -pl kitehub-subscription test` PASS 0 failures 0 errors trên `main` HEAD
- [ ] CI job "Test KiteHub Subscription Service (strict-warnings — GAP-245)" xanh trên PR mới động `kitehub-subscription` không cần `ADMIN_MERGE_OVERRIDE`
- [ ] Mỗi unused stub fix có 1-line comment giải thích why removed (audit trail cho test maintenance)

## Related

- Tiền lệ: GAP-735 (kiteclass-core preexisting flaky tests, đóng Wave meta-3 — same class)
- Triggered từ: PR #2150 (Wave flow-kh3 pre-walk batch-fix BE) CI fail 2026-06-04
- Recent commit có thể tác động cluster này: `ac54a419 feat(subscription): gate tier upgrade behind manual VietQR payment confirm` (refactor flow, có thể dịch chuyển emit signature)
- Sister gap: GAP-544 PARTIAL (kitehub-subscription IT require Postgres :5433 testcontainers) — khác class (IT setup), nhưng cùng module
- Rule cite: `.claude/rules/admin-merge-discipline.md` v1.0.3 §4 ADMIN_MERGE_OVERRIDE pattern
- Rule cite: `.claude/rules/release-fix-retry-budget.md` §3.5 investigation phase mandate (apply cho session fix sau này)
