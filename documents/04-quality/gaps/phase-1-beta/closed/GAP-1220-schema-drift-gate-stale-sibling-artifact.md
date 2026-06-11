# GAP-1220: Schema-drift gate compile fail do stale sibling artifact trong ~/.m2 self-hosted runner

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Meta
**Found:** 2026-06-11 (PR #2326 — job "DB schema drift" fail `cannot find symbol setTxnRef` dù code đúng)
**Affects:** `scripts/check-schema-drift.sh` (kitehub-subscription/branding validate)

## Problem

Script chạy `./mvnw -f kitehub-subscription/pom.xml spring-boot:run` — resolve `kitehub-platform` từ local ~/.m2 thay vì build reactor. Self-hosted runner giữ jar platform CŨ (thiếu `Payment.txnRef` thêm 2026-06-08 #2153) → compile error không liên quan PR đang test. False-fail class: mọi PR chạm migration path sau khi sibling entity đổi mà runner chưa re-install.

## Fix (shipped PR #2326)

Thêm bước `./mvnw -q -DskipTests -pl kitehub-platform -am install` trước validate subscription — sibling artifact luôn fresh từ source PR.

## Acceptance Criteria

- [x] Script cài platform trước khi boot subscription (code + bash -n OK)
- [x] Job DB schema drift pass trên PR #2326 vòng kế (verify khi CI chạy)

## Related

- Discovered in: PR #2326 CI; sister class: stale-cache self-hosted runner
