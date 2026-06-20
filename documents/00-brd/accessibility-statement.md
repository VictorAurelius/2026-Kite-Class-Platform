# Accessibility Statement (Tuyên bố về khả năng tiếp cận) — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** PM + Frontend Lead
**Reviewer:** Business Lead + UX
**Legal basis:** **Luật Người khuyết tật 2010 (51/2010/QH12)** — bảo đảm tiếp cận; chuẩn quốc tế **WCAG 2.1 AA** (best-practice, không bắt buộc luật VN nhưng cam kết tự nguyện)
**Related:** [`personas-catalog.md`](personas-catalog.md) · ui-review audits `04-quality/audits/ui-review/` (WCAG findings)

---

## 1. Phạm vi & mục đích

Tuyên bố cam kết khả năng tiếp cận (accessibility) của sản phẩm + mức độ tuân thủ WCAG hiện tại + lộ trình cải thiện. Minh bạch cho người dùng khuyết tật + due-diligence.

Skeleton Phase 1 (maturity P3): khung tuyên bố; **mức tuân thủ WCAG cụ thể tham chiếu ui-review audit gần nhất** — KHÔNG cam kết "fully accessible" trước khi đo.

---

## 2. Mục tiêu tuân thủ

- Mục tiêu: **WCAG 2.1 mức AA** cho user-facing flows (signup, login, dashboard, học/dạy).
- Trạng thái hiện tại: **partial** — ui-review audit 2026-06 surfaced WCAG findings (vd GAP-1373 skip-link, GAP-1374 KH login label-association, GAP-1378 chart SR-accessibility). Đang khắc phục dần.

> TBD (Phase 2 — UX/FE): mức tuân thủ đo bằng axe-core/Lighthouse a11y; chốt % conformance sau khi đóng cluster WCAG gaps.

## 3. Tính năng accessibility đã có

- `lang="vi"` toàn bộ FE (ui-review positive).
- Keyboard navigation cơ bản, semantic HTML, ARIA một phần.
- Rate-limit countdown UX (KH login).

## 4. Hạn chế đã biết (known limitations)

- Skip-link thiếu systemic (GAP-1373).
- Một số form thiếu label-association (GAP-1374).
- Chart chưa SR-accessible (GAP-1378).
- Chưa có dark-mode toggle (GAP-1376).

## 5. Phản hồi accessibility

Người dùng gặp rào cản tiếp cận có thể báo qua support@ (cross-ref [`support-sla-sop.md`](support-sla-sop.md)).

## 6. Tuân thủ pháp lý

- **Luật Người khuyết tật 2010:** khuyến khích tiếp cận dịch vụ; KiteHub cam kết tự nguyện WCAG 2.1 AA.

## 7. Out of Scope (this skeleton)

- Measured WCAG conformance % (Phase 2 — axe-core/Lighthouse)
- VPAT (Voluntary Product Accessibility Template) — Phase 3 enterprise
- Full remediation of WCAG cluster (tracked via ui-review gaps)

## 8. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P3 batch — P. Accessibility Statement). Conformance target + current-state + known-limitations structure; references ui-review WCAG gaps (GAP-1373/1374/1378/1376). Cites Luật Người khuyết tật 2010 + WCAG 2.1 AA voluntary.
