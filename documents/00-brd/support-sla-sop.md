# Support SLA / SOP (Quy trình vận hành hỗ trợ) — KiteHub/KiteClass

**Audience:** mixed
**Status:** 🟡 SKELETON (draft — content TBD)
**Created:** 2026-06-21
**Owner:** Support Ops + PM
**Reviewer:** Tech Lead + Business Lead
**Legal basis:** **Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 (19/2023/QH15) (L3)** — nghĩa vụ tiếp nhận + giải quyết khiếu nại người tiêu dùng
**Related:** [`customer-sla-uptime.md`](customer-sla-uptime.md) §3 (response windows) · [`incident-response-breach-notification.md`](incident-response-breach-notification.md) · [`compliance-checklist.md`](../../.claude/skills/quality/marketing-legal-review/reference/compliance-checklist.md) §1.3 VN-CON

---

## 1. Phạm vi & mục đích

SOP nội bộ cho đội Support — quy trình tiếp nhận, phân loại, escalate, và đóng ticket. Khác với [`customer-sla-uptime.md`](customer-sla-uptime.md) (cam kết hướng khách hàng), tài liệu này là **quy trình vận hành nội bộ**.

Skeleton Phase 1: khung quy trình + ma trận severity; **số liệu staffing + response time thực tế cần Support-ops baseline Phase 2** — KHÔNG fabricate.

---

## 2. Kênh tiếp nhận (intake channels)

| Kênh | Phase | Ghi chú |
|---|:---:|---|
| Email (support@) | Phase 1 | đã có per GAP-540 |
| In-app ticket / widget | Phase 1 | discoverability per GAP-540 |
| Zalo OA | Phase 2 | depends GAP-063 live ZNS |
| Hotline | Phase 2+ | TBD staffing |

> TBD (Phase 2 — Support-ops): giờ hỗ trợ per tier (8x5 vs 24x7), SLA first-response chốt sau baseline.

---

## 3. Severity classification (ma trận phân loại)

Đồng bộ với [`customer-sla-uptime.md`](customer-sla-uptime.md) §3 (P1 down / P2 lỗi-có-workaround / P3 nhỏ / P4 cosmetic). First-response SLA per tier — xem doc đó.

## 4. Escalation path (luồng escalate)

L1 Support → L2 Engineer on-call → L3 Tech Lead → Incident commander (nếu P1 prod incident, cross-ref `incident-response-breach-notification.md`).

> TBD (Phase 2): roster on-call cụ thể, SLA escalate giữa các tầng, công cụ ticketing (Zendesk/Freshdesk — ADR pending).

## 5. Ticket lifecycle

NEW → TRIAGED → IN_PROGRESS → WAITING_CUSTOMER → RESOLVED → CLOSED. CSAT survey gửi sau CLOSED (TBD Phase 2).

## 6. Tuân thủ pháp lý (L3)

- VN-CON: tiếp nhận khiếu nại bằng tiếng Việt; lưu hồ sơ khiếu nại; phản hồi trong thời hạn luật định.

> TBD (Phase 2 — legal): thời hạn phản hồi khiếu nại theo luật; quy trình lưu trữ hồ sơ khiếu nại.

## 7. Out of Scope (this skeleton)

- Staffing model + shift roster (Phase 2 — Support-ops)
- Ticketing vendor selection (Phase 2 — ADR)
- CSAT/NPS measurement targets (Phase 2)

## 8. Log

- 2026-06-21 — Skeleton created (GAP-154 BRD scope expansion, P2 batch — G. Support SLA/SOP). Intake + severity + escalation + lifecycle structure; staffing/timing TBD Phase 2.
