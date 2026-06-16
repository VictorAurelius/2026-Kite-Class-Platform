# GAP-1431: KC-7 RecordPaymentModal mồ côi — affordance record-payment không wire vào page nào

**Status:** 🔵 OPEN
**Priority:** P1
**Domain:** Frontend
**Found:** 2026-06-15 (KC-5/6/7/8/11 browser re-walk — Workflow 5 Opus agent qua nip.io)

## Problem

components/billing/record-payment-modal.tsx + lib/api/payment-records.ts chỉ self-reference, không page nào import. BE record-payment sạch (201, GAP-1003/1004 verified) nhưng owner không record payment qua UI được. Cần wire modal vào billing/[id]/page. FE-completion, defer.

## Related
- Discovered in: KC browser re-walk Workflow 2026-06-15 (goal "run hết flow cho dev G2"). Per discovery-to-gap-inline-filing.md.
