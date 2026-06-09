# GAP-1087: SubscriptionServiceTest stale sau SePay walk — generateQRCode overload + memo assertion drift (Bug D adjacent)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-09 (Wave landing-tenant-1 — phát hiện khi verify fix Bug E/F; debt do SePay walk commit 4991da67/075de5e1 để lại)
**Affects:** `kitehub/kitehub-subscription/src/test/java/com/kitehub/subscription/service/SubscriptionServiceTest.java` (3 test: `shouldUpgradeSubscriptionByCreatingPendingPaymentOnly`, `shouldCreateVietQrPaymentForUpgrade`, `shouldCreatePendingSubscriptionForPaidTier`) + production `SubscriptionService:442` txnRef/memo logic

## Problem

`SubscriptionServiceTest` ở HEAD của `wave/landing-tenant-1` có **3 test fail (pre-existing, deterministic)** — verify bằng git stash baseline (3 errors trên cả state có lẫn không có fix Bug E/F). Sẽ block CI gate `Test Core Service` khi push branch.

Nguyên nhân: SePay walk (commit `4991da67` "QR memo==txnRef" + `075de5e1` "beta amount override") thay đổi contract VietQR payment **mà không sweep test** (đúng class `api-contract-change-caller-sweep.md` miss):

1. **Overload mới chưa sweep stub:** thêm `VietQRService.generateQRCode(UUID, Long, String memo)` cạnh bản cũ `(UUID, Long, UUID subscriptionId)`. Production `SubscriptionService:442` giờ gọi bản **String txnRef**. Test vẫn stub bản UUID (`eq(subscriptionId)`) → `PotentialStubbingProblem`. Đồng thời `generateQRCode(any(),any(),any())` (line 91) giờ **ambiguous** giữa 2 overload (IDE diagnostic; javac tie-break fragile).

2. **Memo/txnRef assertion drift:** sửa overload stub sang `anyString()` lại lộ tầng sâu hơn — production sinh memo dạng `KH3SUB<hash>` (txnRef tự sinh) thay vì `vietQRService.generatePaymentContent(subscriptionId)` (`"KITEHUB ABCD1234"`). Hệ quả:
   - `generatePaymentContent` stub trở thành **UnnecessaryStubbing**.
   - Assertion `capturedPayment.getPaymentContent()` expect `"KITEHUB ABCD1234"` nhưng actual `"KH3SUB..."`.

## Root Cause

Liên quan trực tiếp **Bug D** (handoff pickup #5): "`PaymentService.createPayment` upgrade path cùng class memo!=txnRef". SePay walk đổi memo = txnRef nhưng (a) chỉ sửa 1 path, (b) không sweep callers/tests, (c) không cập nhật assertion. Việc fix test ĐÚNG cần chốt contract memo/txnRef trước (Bug D decision) — **không nên** sửa assertion để khớp `KH3SUB...` một cách máy móc (rủi ro lock-in chính bug memo!=txnRef).

## Proposed Fix (defer to Bug D wave)

Gộp với Bug D fix:
1. Chốt contract txnRef/memo (1 nguồn sinh, áp dụng cả create + upgrade path) — đóng memo!=txnRef.
2. Sweep `generateQRCode` callers (prod + test) sang đúng overload; loại ambiguous `any(),any(),any()` → `anyString()`.
3. Cập nhật assertion `getPaymentContent()` theo contract memo đã chốt (không hardcode hash run-time).
4. Bỏ stub `generatePaymentContent` nếu không còn dùng (hoặc giữ nếu path khác dùng).
5. Chạy `./mvnw -pl kitehub-subscription test` (không chỉ compile) per `api-contract-change-caller-sweep.md` §3.3.

## Diagnosis evidence (session 2026-06-09)

- Baseline (git stash fix Bug E/F): `SubscriptionServiceTest` 13 tests, **3 errors** → pre-existing, không do Bug E/F.
- Sau thử fix stub `anyString()` (đã revert): 2 failures (`expected "KITEHUB ABCD1234" but was "KH3SUB..."`) + 1 UnnecessaryStubbing → lộ tầng memo drift.
- Reverted test về HEAD; PR Bug E/F **không** chứa fix này (giữ scope sạch).

## Impact on push

PR `wave/landing-tenant-1` sẽ đỏ CI ở `SubscriptionServiceTest` cho tới khi Bug D fix HOẶC dùng `ADMIN_MERGE_OVERRIDE: GAP-1087` per `admin-merge-discipline.md` §4 (pre-existing branch debt, separable concern).

## Acceptance Criteria

- [ ] memo/txnRef contract chốt (Bug D), áp dụng create + upgrade path
- [ ] `generateQRCode` overload ambiguity loại bỏ (line 91 + 288 + 327)
- [ ] 3 test PASS với assertion theo contract đã chốt
- [ ] `./mvnw -pl kitehub-subscription test` xanh toàn module

## Related

- Bug D (handoff pickup #5): `PaymentService.createPayment` memo!=txnRef
- SePay walk: commit 4991da67, 075de5e1
- Meta miss: `api-contract-change-caller-sweep.md` (overload swap không sweep test + không run test trước push)
- Discovered while fixing: GAP-1085 + GAP-1086
