# GAP-596: Landing form inline validation + draft preservation

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (defer Wave 87)
**Domain:** Frontend
**Phase:** phase-1-beta
**Found:** 2026-05-15 (Wave 86 Bucket A persona-outside-in audit cell 1.5)
**Affects:** Anonymous prospect signup recovery path

## Problem

Persona cell 1.5 (Vy failure recovery):
- Email typo → cần inline validation phát hiện trước submit
- Form submit fail (network error) → cần giữ data, không clear form
- UX hiện tại có thể submit-and-clear pattern = user re-enters từ đầu = abandonment

Wave 86 không cover landing form UX hardening.

## Root Cause

Form component có thể không có inline validation, hoặc validation chỉ trigger on submit. Local state không persisted localStorage cho draft recovery.

## Proposed Fix

1. **Inline validation** `kitehub-frontend/src/components/landing/signup-form.tsx`:
   - Email regex check on-blur + show inline error message
   - Phone format check (VN: 09xxx / 03xxx / 07xxx / 08xxx / 05xxx)
   - Tenant name length check (2-100 chars)
   - All fields show success checkmark khi valid
2. **Draft preservation**:
   - Form state persisted localStorage every keystroke (debounced 500ms)
   - Page reload → restore from localStorage
   - On successful submit → clear localStorage
3. **Submit failure handling**:
   - Network error → show toast "Lỗi mạng, vui lòng thử lại"
   - Form data preserved
   - Retry button
4. **Self-test**: simulate network failure (offline mode) → verify form preserved + retry works

## Acceptance Criteria

- [ ] Inline validation per field
- [ ] Draft localStorage persistence
- [ ] Network failure recovery với retry
- [ ] Self-test passing
- [ ] Defer to Wave 87

## Related

- Audit: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-persona-outside-in.md` §3.1 cell 1.5 + §6 NEW gap proposal #7
- Wave 87 scope (defer)
- GAP-595 landing CTA hierarchy (paired)

## Log
- **2026-06-09 DONE:** Wave landing-100 shipped (bucket 596) — G1-headless verified (FE build green + curl render 200 + ?tenant= data-binding proven). Full browser-G2 + subdomain resolution gated GAP-811/1077; BE per-tenant fields GAP-1083.
