# GAP-1221: ContactMessage BE đòi email @NotBlank trong khi form public VN cho email optional → 400

**Status:** 🟡 PARTIAL (90% — code+tests shipped, chờ runtime curl verify post-merge rebuild)
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-11 (GAP-274 port — contact form wire endpoint thật)
**Affects:** `kiteclass-core` `CreateContactMessageRequest` (`email @NotBlank @Email`, `subject @NotBlank`) vs `(public)/contact` form

## Problem

Phụ huynh VN quen để SĐT, email optional (kit spec + persona). BE validate `email @NotBlank` → submit không email = 400. `subject` cũng @NotBlank — FE đang synthesize tạm `"Liên hệ từ {tên}"`.

## Proposed Fix

Nới `email` → optional (@Email khi present); `subject` optional default server-side. Sync api-contract.md + test.

## Acceptance Criteria

- [ ] Submit chỉ tên+SĐT+lời nhắn → 201 (runtime curl sau rebuild — bean-validation test đã PASS)
- [x] Email sai format vẫn 400 (khi có) — `CreateContactMessageRequestValidationTest.invalidEmailFormat_shouldStillFail` PASS

## Fix shipped (2026-06-12)

DTO bỏ @NotBlank email/subject (giữ @Email + @Size) · Entity email nullable + V97 migration DROP NOT NULL · Service default subject "Liên hệ từ {name}" + placeholder email cho notify · 3-layer docs sync (BR-MKT-001 v2 + use-case + api-contract) · 5 test mới PASS (2 service + 3 bean-validation).

## Related

- GAP-274 port (FE đã wire `/api/v1/contact` thật); kit kiteclass-public contact spec
