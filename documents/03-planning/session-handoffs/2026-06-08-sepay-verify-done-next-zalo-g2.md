# Session Handoff 2026-06-08 — SePay verify DONE; next: Zalo config → G2

**Ngày:** 2026-06-08
**Next-session focus (user-directed):** Hướng dẫn nốt **cấu hình Zalo** (OA/ZNS) để bắt đầu **G2**.

---

## 1. Session này đã ship

### Merged (main)
| PR | Nội dung |
|---|---|
| #2258 / #2259 / #2262 | Dependabot Maven deps (clean) |
| #2261 / #2260 | Frontend dep bumps — regen pnpm-lock + build verify; #2260 kèm bundle-budget override 270KB (students/teachers) |
| #2268 | `rebuild.sh all` + `build-all.sh` fix (profile + kiteclass) — GAP-1060 |

### PR #2269 OPEN — SePay webhook hardening (code PR, chờ CI/review)
Gói trọn verify SePay (KiteHub subscription billing) qua **Approach B** (SePay Test Mode simulator → cloudflared tunnel → gateway → subscription, end-to-end thật):
- **GAP-1061** (P1 FIXED): SecurityConfig whitelist `/api/platform/webhooks/**` (thiếu → 401 trước Apikey auth → prod không reconcile)
- **GAP-1063** (P1 FIXED): ACK body `{"success":true}` (SePay đòi; thiếu → SePay đánh failed + retry 7× dù payment OK) — chỉ real-vendor test bắt được
- **GAP-1062** (P1 FIXED, TDD): `applyPendingUpgrade` → `@Transactional(REQUIRES_NEW)` (rollback poisoning; regression IT `SepayWebhookRollbackIsolationIT` RED→GREEN; 127 affected tests Failures:0)
- GAP-1058 DONE (8/8 webhook branches verified); GAP-975/976 → 90% (logic verified, còn real-money smoke)
- Recipe `documents/05-guides/operations/sepay-webhook-local-verify-recipe.md` + runbook §4.5.5 field-level config
- **GAP-1064 filed (P2 OPEN)**: H2 `@SpringBootTest` ITs (SubscriptionBillingIT…) silently broken — RLS `set_config()` không có trên H2. Cần audit + migrate Testcontainers.

**Còn lại SePay:** real-money smoke (cần SePay merchant thật + GAP-612 prod) — defer.

## 2. Việc đầu session sau — Zalo config → G2

Làm tương tự cách đã làm SePay (hướng dẫn cấu hình vendor dashboard + verify local):
- **Gap liên quan:** GAP-063 (Zalo OA + SMS Notification, P0 phase-1-beta, **20%**) · GAP-063b (Zalo ZNS Phase 2) · zalo-integration-design.md (per `thesis-as-future-state-mandate.md` — Zalo = goal Phase 1.5).
- **Lưu ý thesis:** Ch1/Ch2 claim "đã kết nối Zalo OA" = goal state; Phase 1 BETA minimum = passive CTA (GAP-660 DONE); full = active ZNS push (Phase 1.5). Per `thesis-as-future-state-mandate.md`.
- **G2:** xem `documents/03-planning/project-management/project-schedule.md` (user highlight dòng "G2") + Flow Verification Campaign gates.
- **Pattern verify:** ưu tiên local decoupled trước (như SePay Approach A), real-vendor (Zalo OA dashboard) sau nếu cần — **real-vendor test bắt được contract bug mà local miss** (bài học GAP-1063).

## 3. Trạng thái stack/env
- Local Docker stack: 14 container chạy (subscription đã rebuild với GAP-1061/1062/1063 fixes). cloudflared tunnel đã **kill** (cleanup). Test payment đã xóa.
- AWS: idle/stopped (GAP-612 restored; `bash scripts/aws/start-stack.sh` khi cần prod).
- Branch: `verify/sepay-975-976-test-mode-logic` (PR #2269) — chưa merge.

## 4. Sync state
- ✅ gap-status.csv (1058/1061/1062/1063 DONE + 1064 OPEN — trên branch PR #2269)
- ⏳ ROADMAP / wave-history: SePay work còn trên PR #2269 chưa merge → sync khi merge
- Handoff: file này
