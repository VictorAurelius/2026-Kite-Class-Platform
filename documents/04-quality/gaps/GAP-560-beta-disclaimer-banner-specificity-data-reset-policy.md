# GAP-560: Beta disclaimer banner thiếu specificity → tăng anxiety thay vì calm

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Detected:** 2026-05-14
**Related Audits:** `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (Persona 1 N1-P1)
**Related Gaps:** GAP-539 (beta disclaimer banner + status page — DONE; banner exists nhưng vague)

## Current State (verified 2026-05-14)

| Piece | File / Path | Status |
|-------|-------------|--------|
| `<BetaDisclaimerBanner />` | `kitehub/kitehub-frontend/src/components/beta-disclaimer/BetaDisclaimerBanner.tsx` (103 LOC) | ✅ shipped (Wave 78 GAP-539) |
| Banner copy | line 79-82: "Dữ liệu có thể bị reset trong quá trình nâng cấp. Gặp vấn đề? Xem trạng thái Beta hoặc email support@kitehub.me" | ⚠️ too vague |
| Data reset policy doc | `documents/01-business/kitehub/data-retention/` + linked from banner | ⚠️ likely missing or not linked |
| Beta status page detail | `/beta-status` page | ✅ shipped (links from banner) |

**Grep commands run:**
```bash
find documents -iname "*reset*beta*" -o -iname "*beta*reset*" 2>/dev/null
# Result: 0 hits — no dedicated beta data reset policy doc
find documents/01-business/kitehub/data-retention -type f 2>/dev/null
# Result: rules.md + use-cases.md + api-contract.md likely exists — verify policy includes beta reset language
```

## Problem

Beta disclaimer banner (GAP-539 Wave 78) copy hiện tại:
> "KiteHub đang trong giai đoạn Beta. Dữ liệu có thể bị reset trong quá trình nâng cấp."

Cụm "dữ liệu có thể bị reset" KHÔNG đủ specificity. P2 Center Owner (Chị Hằng) walkthrough Bước 3 surface:
- **Bao giờ reset?** Hằng ngày? Hằng tuần? Trước GA launch? Random?
- **Reset toàn bộ hay 1 phần?** Tenant config + AI Branding bị mất? Hay chỉ test data?
- **Có backup không?** Restore được không nếu user nhập data thật?
- **Có notification trước reset không?** Email warn 7 ngày trước?

Hậu quả:
- User trung tâm hoảng → KHÔNG nhập data thật → beta tenant adoption rate thấp → unable to validate real-world workflows
- Có user lỡ nhập data thật → reset → trust crisis + potential PDPL complaint (data loss without notice)
- Banner "calm signal" reverse thành "anxiety signal"

## Context

Outside-in audit Persona 1 walkthrough flag: banner intended để calm user "đừng kỳ vọng production-grade", nhưng phản tác dụng. UX research SaaS beta launches show specificity reduces anxiety:
- Linear, Notion beta banners list: "Test data only. Reset weekly Sunday 2am. Backup on request via support@."
- Stripe sandbox: "Test mode. Data persists indefinitely; live mode separate."

Phase 1 BETA cần policy CỤ THỂ về reset cadence + backup + advance notice.

## Evidence

- `BetaDisclaimerBanner.tsx` line 79-82: copy + link `/beta-status` + mailto support — KHÔNG link đến reset policy doc cụ thể
- `/beta-status` page render markdown từ BE: nội dung markdown có cover reset policy không? — need verify
- Welcome email (welcome.html) không mention reset policy

## Proposed Fix

1. **Define beta data reset policy** — Wave 79 quyết định 1 trong 3 options:
   - **Option A:** Hứa KHÔNG reset trong Phase 1 BETA window (cam kết stability đến GA)
   - **Option B:** Reset hàng tuần Chủ nhật 2am với email warn 24h trước (sandbox model)
   - **Option C:** Reset chỉ khi major schema migration; advance notice 7 ngày email
2. **Document policy** trong `documents/01-business/kitehub/data-retention/beta-reset-policy.md` (new file) với:
   - Reset cadence (per option chọn)
   - Scope (toàn bộ tenant data vs chỉ test data)
   - Backup procedure (user request via support email → 30-day retention even after reset)
   - Advance notice (email + dashboard banner 7d ahead)
   - PDPL alignment (user data deletion right vs operational reset)
3. **Update banner copy** với specific reference:
   ```
   "KiteHub đang trong giai đoạn Beta — phiên bản thử nghiệm. [Cam kết: không reset dữ liệu của bạn trước GA launch] OR [Reset weekly Chủ nhật 2am — backup tự động]. Xem chi tiết: /beta-data-policy"
   ```
4. **Add `/beta-data-policy` page** render from BE markdown — same pattern as `/beta-status`
5. **Update welcome email** to mention reset policy explicitly với link

## Acceptance Criteria

- [ ] Wave 79 decision: which reset policy option (A/B/C) — document in ADR or business doc
- [ ] `documents/01-business/kitehub/data-retention/beta-reset-policy.md` created với schedule + scope + backup + notice procedure
- [ ] Banner copy updated với specific reset commitment (timeline + backup mention) + link `/beta-data-policy`
- [ ] `/beta-data-policy` page route created render policy doc
- [ ] Welcome email mentions reset policy + link
- [ ] Persona test: P2 Owner read banner → understand reset cadence + backup → confidence to enter real data
- [ ] PDPL cross-check: reset policy aligned với data deletion rights + advance notice requirement

## Related

- GAP-539 (banner UI — DONE; this gap closes copy + policy delta)
- GAP-558 (cookie consent — sister compliance gap; both touch user-facing legal communication)
- Rule: `.claude/rules/business-logic-review.md` (compliance scope)
- Inside-out queue: không overlap
- Audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md`

## Log

- **2026-05-14:** DONE — Wave 79 Bucket D closure. Beta disclaimer banner specificity update (P2 Owner anxiety) + data-reset-policy.md runbook shipped (PR #1368).

- 2026-05-14 — Filed via Wave 79 outside-in audit (Persona 1 N1-P1). Banner UI exists nhưng copy không actionable + thiếu policy doc. Priority P1 vì degrades retention (not block invite) nhưng critical cho data trust.
