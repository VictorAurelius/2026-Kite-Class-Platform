# Wave 12 — Phase A Audit Report

**Ngày:** 2026-03-24
**Script:** `scripts/verify-business-docs.sh`
**Chain:** BR → UC → api-contract → Controller code

---

## Summary

| Metric | Count |
|--------|-------|
| ✅ PASS | 36 |
| ⚠️ WARN | 29 |
| ❌ FAIL | 0 |

**0 phantom endpoints, 0 missing 3-layer files — hệ thống docs nhất quán.**

---

## KiteClass — Warnings (22)

### BR Orphans — BR defined in rules.md nhưng không được reference trong use-cases.md

| Domain | BR ID | Action cần làm |
|--------|-------|---------------|
| course-class | BR-CLS-003 | Add reference vào use-cases.md |
| lms | BR-LMS-003 | Add reference vào use-cases.md |
| marketing | BR-MKT-002 | Add reference vào use-cases.md |
| marketing | BR-MKT-015 | Add reference hoặc xóa nếu không dùng |
| marketing | BR-MKT-016 | Add reference hoặc xóa nếu không dùng |
| marketing | BR-MKT-017 | Add reference hoặc xóa nếu không dùng |
| marketing | BR-MKT-020 | Add reference hoặc xóa nếu không dùng |
| notification-email | BR-MKT-002 | Cross-domain rule — check nếu nên dùng BR-NOTIF prefix |
| notification-email | BR-MKT-003 | Cross-domain rule — check nếu nên dùng BR-NOTIF prefix |
| notification-email | BR-MKT-004 | Cross-domain rule — check nếu nên dùng BR-NOTIF prefix |
| payment-invoice | BR-INV-007 | Add reference vào use-cases.md |
| storage | BR-STR-001 | Add reference vào use-cases.md |
| storage | BR-STR-010 | Add reference vào use-cases.md |
| storage | BR-STR-012 | Add reference vào use-cases.md |
| storage | BR-STR-013 | Add reference vào use-cases.md |
| storage | BR-STR-014 | Add reference vào use-cases.md |
| storage | BR-STR-016 | Add reference vào use-cases.md |
| teacher | BR-TCH-004 | Add reference vào use-cases.md |

### UC Orphans — UC trong use-cases.md không có trong api-contract.md

| Domain | UC IDs | Ghi chú |
|--------|--------|---------|
| teacher | UC-TCH-07, UC-TCH-08, UC-TCH-09 | Cần thêm vào api-contract.md |

### Missing Rules Format

| Domain | Issue |
|--------|-------|
| tenant-settings | rules.md không có BR-xxx IDs — cần format chuẩn |

---

## KiteHub — Warnings (7)

### UC Orphans — UC trong use-cases.md không được reference trong api-contract.md

| Domain | UC IDs | Ghi chú |
|--------|--------|---------|
| data-retention | UC-RET-01, UC-RET-02, UC-RET-03 | Scheduler-based domain, không có endpoints — cần note trong api-contract.md |
| instance-provisioning | UC-INS-03 | Suspend instance — có thể là automated action |
| subscription-billing | UC-SUB-06 | Cần thêm vào api-contract.md |
| trial-lifecycle | UC-TR-02, UC-TR-03 | Automated lifecycle transitions — cần note |

---

## Domains hoàn toàn clean (0 warnings)

### KiteClass ✅
- attendance (9 BR, 7 UC, 9 endpoints — all match)
- gamification-points (3 UC, 3 endpoints — all match)
- grade-assignment (14 BR, 9 UC via range, 20 endpoints — all match)
- lms — UC/endpoint checks pass (1 BR orphan)
- student-enrollment (12 BR, 8 UC, 11 endpoints — all match)

### KiteHub ✅
- ai-branding (6 UC, 7 endpoints — all match)
- domain-management (4 UC, 4 endpoints — all match)
- email-lifecycle (4 UC, 3 endpoints — all match)

---

## Phase B Fix Plan

### Priority 1 — Docs fix (thêm BR references vào use-cases.md)

| Domain | Items | Effort |
|--------|-------|--------|
| storage | 6 BR orphans (STR-001, 010, 012-014, 016) | 30 min |
| marketing | 5 BR orphans (MKT-002, 015-017, 020) | 30 min |
| notification-email | 3 cross-domain BR (MKT-002-004) | 20 min |
| teacher | 1 BR orphan (TCH-004) + 3 UC orphans (TCH-07-09) | 30 min |
| payment-invoice | 1 BR orphan (INV-007) | 10 min |
| course-class | 1 BR orphan (CLS-003) | 10 min |
| lms | 1 BR orphan (LMS-003) | 10 min |
| tenant-settings | Add BR-xxx format to rules.md | 20 min |

### Priority 2 — KiteHub UC alignment

| Domain | Action | Effort |
|--------|--------|--------|
| data-retention | Add note: "UC-RET-01/02/03 = scheduler-triggered (no endpoint)" | 10 min |
| trial-lifecycle | Add note: "UC-TR-02/03 = automated lifecycle (no endpoint)" | 10 min |
| instance-provisioning | Add UC-INS-03 to api-contract.md | 15 min |
| subscription-billing | Add UC-SUB-06 to api-contract.md | 15 min |

### Total Phase B estimate: ~3.5 hours

---

## Notes

- KiteHub domains dùng ID format `RET-01`, `SUB-01` (không có `BR-` prefix) — đây là acceptable vì KiteHub là SaaS platform, không phải business rules theo nghĩa narrow. Script tự động detect và skip BR→UC check cho format này.
- Range notation `UC-GRD-01 → UC-GRD-09` trong api-contract.md được script nhận dạng và không coi là orphan.
- 0 phantom endpoints — toàn bộ endpoints trong docs đều có code implementation.
