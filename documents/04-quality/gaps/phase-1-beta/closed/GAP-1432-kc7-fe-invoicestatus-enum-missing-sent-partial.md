# GAP-1432: KC-7 FE InvoiceStatus enum thiếu SENT+PARTIAL → nút thao tác không render cho invoice SENT

**Status:** 🟢 DONE
**Priority:** P1
**Domain:** Frontend
**Found:** 2026-06-15 (KC-5/6/7/8/11 browser re-walk — Workflow 5 Opus agent qua nip.io)

## Problem

BE phát status SENT/PARTIAL nhưng FE enum chỉ có DRAFT/PENDING/PAID/OVERDUE/CANCELLED → status map fall-through, action buttons (record-payment...) không render. Fix: thêm SENT+PARTIAL vào enum + invoice-status-badge variants/labels ('Đã gửi'/'Thanh toán một phần').

## Related
- Discovered in: KC browser re-walk Workflow 2026-06-15 (goal "run hết flow cho dev G2"). Per discovery-to-gap-inline-filing.md.
