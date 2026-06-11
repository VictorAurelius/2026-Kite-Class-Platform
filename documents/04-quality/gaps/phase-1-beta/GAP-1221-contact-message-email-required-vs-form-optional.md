# GAP-1221: ContactMessage BE đòi email @NotBlank trong khi form public VN cho email optional → 400

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-11 (GAP-274 port — contact form wire endpoint thật)
**Affects:** `kiteclass-core` `CreateContactMessageRequest` (`email @NotBlank @Email`, `subject @NotBlank`) vs `(public)/contact` form

## Problem

Phụ huynh VN quen để SĐT, email optional (kit spec + persona). BE validate `email @NotBlank` → submit không email = 400. `subject` cũng @NotBlank — FE đang synthesize tạm `"Liên hệ từ {tên}"`.

## Proposed Fix

Nới `email` → optional (@Email khi present); `subject` optional default server-side. Sync api-contract.md + test.

## Acceptance Criteria

- [ ] Submit chỉ tên+SĐT+lời nhắn → 201
- [ ] Email sai format vẫn 400 (khi có)

## Related

- GAP-274 port (FE đã wire `/api/v1/contact` thật); kit kiteclass-public contact spec
