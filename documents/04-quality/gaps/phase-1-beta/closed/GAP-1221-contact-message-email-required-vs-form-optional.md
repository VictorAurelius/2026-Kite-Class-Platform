# GAP-1221: ContactMessage BE đòi email @NotBlank trong khi form public VN cho email optional → 400

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-11 (GAP-274 port — contact form wire endpoint thật)
**Affects:** `kiteclass-core` `CreateContactMessageRequest` (`email @NotBlank @Email`, `subject @NotBlank`) vs `(public)/contact` form

## Problem

Phụ huynh VN quen để SĐT, email optional (kit spec + persona). BE validate `email @NotBlank` → submit không email = 400. `subject` cũng @NotBlank — FE đang synthesize tạm `"Liên hệ từ {tên}"`.

## Proposed Fix

Nới `email` → optional (@Email khi present); `subject` optional default server-side. Sync api-contract.md + test.

## Acceptance Criteria

- [x] Submit chỉ tên+SĐT+lời nhắn → 201 (runtime curl verified 2026-06-12 post-rebuild)
- [x] Email sai format vẫn 400 (khi có) — `CreateContactMessageRequestValidationTest.invalidEmailFormat_shouldStillFail` PASS

## Fix shipped (2026-06-12)

DTO bỏ @NotBlank email/subject (giữ @Email + @Size) · Entity email nullable + V97 migration DROP NOT NULL · Service default subject "Liên hệ từ {name}" + placeholder email cho notify · 3-layer docs sync (BR-MKT-001 v2 + use-case + api-contract) · 5 test mới PASS (2 service + 3 bean-validation).

## Related

- GAP-274 port (FE đã wire `/api/v1/contact` thật); kit kiteclass-public contact spec

## Walk evidence (per feature-ship-runtime-walk-mandate.md §3)

Stack local rebuild từ main `31dd08aab` (kiteclass-core healthy), gateway `:9000` Host-based tenant resolution:
- **Happy path:** `POST /api/v1/contact` (Host co-ha-toan.127.0.0.1.nip.io) body chỉ name+phone+message → **HTTP 201**, response `email:null`, `subject:"Liên hệ từ Chị Trần Thị Hồng"` (server default đúng), DB row id=1 created.
- **Sad path:** cùng endpoint, `email:"not-an-email"` → **HTTP 400** (@Email format check giữ nguyên khi email có).
- Tests: 13/13 PASS local + CI core-ci green (#2350).
