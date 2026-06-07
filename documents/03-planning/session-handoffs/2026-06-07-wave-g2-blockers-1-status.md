---
title: Session handoff — Wave g2-blockers-1 (clear OPEN P1 flow-blockers before G2)
audience: dev
created: 2026-06-07
scope: Wave g2-blockers-1 status — 7/10 gaps shipped inline, 3 remaining + G3 re-walk batch
---

# Session handoff — 2026-06-07 — Wave g2-blockers-1

**Goal:** Đóng 10 OPEN P1 gap chặn G2 human-walk trên 5 flow (KH-9 / KH-6 / KH-5 / KC-7 / KC-6), không gap nào cần SePay, không AWS-gated. Plan: [`wave-2026-06-07-g2-blockers-1-flow-p1-clear.md`](../waves/wave-2026-06-07-g2-blockers-1-flow-p1-clear.md).

**Mode note:** Agents spawn FAILED 2× với "Server is temporarily limiting requests (not your usage limit)" — server-side throttle persistent cả phiên này. Buckets làm **inline** thay agents (user directive). Retry agents phiên sau có thể work khi throttle clear.

## Shipped inline (7/10 gaps, compile PASS, tất cả 🟡 PARTIAL chờ G3 re-walk)

| PR | Bucket | Flow | Gaps | Tóm tắt |
|----|--------|------|------|---------|
| #2244 | plan | — | — | Wave plan (draft) |
| #2245 | C | KH-5 lifecycle | GAP-1016, GAP-1017 | manualRenewal → PENDING payment (no free extend) + applyConfirmedRenewal branch; cancel → suspend instance (immediate) + scheduler findCancelledExpiredSubscriptions (end-of-cycle) |
| #2246 | D+E | KC-7, KC-6 | GAP-1004, GAP-1005, GAP-1000, GAP-1002 | overpayment→400 + idempotency(PAYMENT_RECORD scope); InvoiceController @PreAuthorize ×10; finalize teacherId-from-JWT + ADMIN bypass; DefaultGradingScaleProvisioner per-tenant seed |
| #2247 | A | KH-9 admin | GAP-1028 | audit-log search → JpaSpecificationExecutor dynamic Specification (no null-param bind → no 500) |

**Tất cả 7 gap = 🟡 PARTIAL ~80-85%, completion_pct trong CSV.** Compile PASS từng module. Residual chung: IT + **G3 gateway :9000 re-walk** trước DONE flip (per `pre-handoff-self-test-completeness.md` §3).

## Remaining (3 gaps — investigation đã ghi trong gap Log, để OPEN)

| Gap | Flow | Tại sao defer |
|-----|------|---------------|
| **GAP-1029** | KH-9 admin | @Auditable suspend/activate (`kitehub-admin AdminController`) — RISK: `AdminAuditAspect` (kitehub-subscription) có active trong admin Spring context không (silent no-op?). + table drift V36 singular vs V50 plural → PDPL-immutability migration. Security-sensitive. |
| **GAP-1020** | KH-6 branding | RLS GUC: 0 pattern kitehub để mirror (cần thiết lập cơ chế set GUC, sai = cross-tenant leak). Tier server-side: 0 subscription-tier client trong branding (cần build cross-service call). Cả 2 = infra-design tasks. |
| **GAP-1021** | KH-6 branding | job approve/apply endpoint (BrandingJobV1Controller chỉ có getJob) + SSE token-in-query (SecurityConfig surgery — sai = mở endpoint). |

Lý do defer: cả 3 cần infrastructure mới / security-config surgery, không phải quick edit — không rush security-sensitive code vào high-context (session này đạt 68%). Investigation findings đầy đủ trong từng gap `## Log` để fresh session bắt đầu nhanh.

## Next session

1. **G3 re-walk batch** cho 7 gap PARTIAL: stack up production-equivalent + gateway :9000, walk từng flow (KH-5 renew/cancel, KC-7 overpayment/InvoiceController 403, KC-6 finalize+new-tenant grade, KH-9 audit-log GET 200), verify → flip DONE. Nhiều gap có thể cần `SEPAY_API_KEY` KHÔNG — các fix này không phải SePay.
2. **GAP-1029** (clear-ish): verify AdminAuditAspect active trong kitehub-admin → @Auditable suspend/activate + table reconcile migration.
3. **GAP-1020 + GAP-1021** (infra): RLS GUC mechanism + tier client (1020); job-approve endpoint + SSE query-token (1021).
4. **Merge** open PRs #2244/2245/2246/2247 sau CI green (per `docs-only-pr-auto-merge` cho plan; code PRs cần CI).

## Stack state
Local prod-parity: kitehub-subscription + kiteclass-core + kitehub-branding compiled OK (per-module). AWS STOPPED.
