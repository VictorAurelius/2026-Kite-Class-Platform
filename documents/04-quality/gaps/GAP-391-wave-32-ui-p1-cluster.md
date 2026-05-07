# GAP-391: Wave 32/34 UI P1 cluster — RegenerateCounter quota stale-check + i18n migration deferred

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (2 sub-issues — UI polish, ship post-P0)
**Domain:** Frontend
**Found:** 2026-05-07 (UI /128 audit Wave 32+34 — agent a1ffe560)
**Affects:** `kitehub-frontend/src/components/branding/wizard/`

## Problem (2 sub-issues)

### 391-A: RegenerateCounter quota stale-check
- `RegenerateCounter.tsx` accepts `regenerateQuotaText` prop (e.g., "3/3")
- Wave 34 Bucket D wires quota fetch trong parent — KHÔNG re-fetch sau POST `/jobs/{id}/regenerate`
- User regenerate × 4 trên FREE tier (quota=3) → sees stale "3/3" cho ~1-2 seconds trước khi server reject với 429
- Cosmetic UX issue, không blocking nhưng confusing

### 391-B: i18n migration deferred (P0 macro)
- 77 wizard tests + components 100% hardcoded VN strings
- KHÔNG có `useTranslation` hook imported anywhere trong `wizard/`
- Phase 1 BETA chỉ VN-only OK; Phase 2 sang nội dung K-12 có thể yêu cầu English/multi-lang
- Migration estimate: ~20 hours (wholesale `t()` wrap + extract message catalogue)
- **Scope decision**: defer post-BETA per memory `feedback_release_1_first_session_priority.md` Phase 1 priority — KHÔNG block launch

## Proposed Fix

### 391-A
```tsx
// In wizard parent
const { mutate: regenerate } = useRegenerate({
  onSuccess: () => {
    queryClient.invalidateQueries(['regenerate-quota', jobId]);
  }
});
```
Or invalidate via React Query / SWR cache key.

### 391-B (defer)
- Phase 1: do nothing (VN-only acceptable per memory)
- Phase 2 trigger: when K-12 launch approaches, file separate gap để execute migration
- Capture decision in `documents/00-brd/i18n-strategy.md` (file follow-up doc-only gap nếu chưa tồn tại)

## Acceptance Criteria

- [ ] **391-A**: RegenerateCounter re-fetch quota post-regenerate; integration test (mock 3 regenerates → 4th shows 0/3 + disabled button)
- [ ] **391-B**: Deferral decision documented in BRD/strategy doc; explicit "Phase 1 = VN-only acceptable" memo
- [ ] Re-run UI /128 audit delta: 97/128 → maintain ≥97 (no regression)

## Related

- Source audit: `documents/04-quality/audits/ui/2026-05-07-wave-32-rework-and-wave-34-ai-branding-wizard.md` (Findings F2, F4 + GAP-272p/272q recommendations)
- Sister gap: GAP-272o (lifecycle orchestrator) — file already
- Memory: `feedback_release_1_first_session_priority.md` — Phase 1 priority

## Log

- **2026-05-07** Filed from UI /128 audit Wave 32+34. State-check: 0 existing gaps cover RegenerateCounter quota stale (grep returned 0 matches). i18n migration: file as deferral-decision artifact rather than work gap (won't ship Phase 1).
