# GAP-641: Admin Revenue page scaffold-only Wave 35 carry

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Detected:** 2026-05-18 (Wave 92 post-wave audit suite per GAP-619)
**Related Audits:** [documents/04-quality/audits/ui/2026-05-18-wave-92-bucket-d-admin-v1-ui-audit.md](../audits/ui/2026-05-18-wave-92-bucket-d-admin-v1-ui-audit.md)

## Current State (verified 2026-05-18)

| Piece | File / Path | Status |
|---|---|---|
| Admin Revenue page route | `kitehub/kitehub-frontend/src/app/admin/revenue/page.tsx` | 🟡 partial — page exists nhưng hardcoded `0đ` literal |
| `useAdminRevenue` hook | `kitehub/kitehub-frontend/src/hooks/useAdminRevenue.ts` (hoặc tương đương) | ✅ shipped — hook tồn tại nhưng UI không wire |
| Backend `/api/v1/admin/revenue/*` endpoints | `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/AdminRevenueController.java` | ✅ shipped Wave 92 Bucket D |
| Loading state | Revenue page component | ❌ missing |
| Error state | Revenue page component | ❌ missing |

**Grep commands run:**

```bash
grep -rn "0đ\|0 ₫\|useAdminRevenue" kitehub/kitehub-frontend/src/app/admin/revenue/
grep -rn "useAdminRevenue" kitehub/kitehub-frontend/src/hooks/ kitehub/kitehub-frontend/src/app/
find kitehub/kitehub-frontend/src/app/admin/revenue -type f
```

## Problem

Audit UI Wave 92 Bucket D (2026-05-18) phát hiện finding P1: Admin Revenue page (`/admin/revenue`) hiện tại **scaffold-only** từ Wave 35 carry-forward chưa được wire vào real data:

1. **Hardcoded literal:** Page hiển thị `0đ` hardcoded thay vì call backend endpoint `/api/v1/admin/revenue/total` + `/api/v1/admin/revenue/by-period`
2. **Hook orphan:** `useAdminRevenue` hook đã ship (Wave 35 hoặc earlier) nhưng UI page không consume — dead code risk
3. **Missing loading/error states:** Khi wire xong, page cần loading spinner + error toast khi API fail

Wave 35 ship UI scaffold expectation "wire later when backend ready". Backend ready Wave 92 Bucket D nhưng UI wiring không trong scope → carry-forward surfaced trong audit.

## Context

Phase 1 BETA cần admin dashboard cho center owner view revenue. Hardcoded `0đ` literal hiện tại = misleading UI (suggests "0 doanh thu" thay vì "chưa có dữ liệu"). Affects:
- Admin user trust (sees 0đ mỗi lần load)
- Beta tenant onboarding feedback (sees broken admin tool)
- Phase 1 BETA gate UI audit /128 score

## Proposed Fix

### Step 1: Wire useAdminRevenue hook

```tsx
// kitehub-frontend/src/app/admin/revenue/page.tsx
'use client';
import { useAdminRevenue } from '@/hooks/useAdminRevenue';

export default function AdminRevenuePage() {
  const { data, isLoading, error } = useAdminRevenue();

  if (isLoading) return <RevenueLoadingSkeleton />;
  if (error) return <RevenueErrorState error={error} />;

  return <RevenueDashboard totalAmount={data.total} byPeriod={data.byPeriod} />;
}
```

### Step 2: Format VND currency

Per `.claude/rules/user-manual-content-standard.md` §2 row 8 + Vietnamese locale convention:

```tsx
const formatVND = (amount: number) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
```

### Step 3: Add loading + error states

- Loading: skeleton component cho 3-card layout (total / by period / chart)
- Error: toast notification + retry button
- Empty state (data returned `total: 0`): "Chưa có giao dịch nào" thay vì `0đ`

## Acceptance Criteria

- [ ] `/admin/revenue` page consume `useAdminRevenue` hook
- [ ] Hardcoded `0đ` literal removed; data fetched from `/api/v1/admin/revenue/*`
- [ ] Loading state hiển thị skeleton component
- [ ] Error state hiển thị toast + retry button
- [ ] Empty state ("0 transactions") hiển thị message thay vì `0đ`
- [ ] Currency format VND per `Intl.NumberFormat('vi-VN')`
- [ ] Component tests: loading / error / data-loaded / empty paths
- [ ] Pre-handoff self-test per `pre-handoff-self-test-completeness.md` §2.4 admin-flow checklist

## Related

- **Audit origin:** [documents/04-quality/audits/ui/2026-05-18-wave-92-bucket-d-admin-v1-ui-audit.md](../audits/ui/2026-05-18-wave-92-bucket-d-admin-v1-ui-audit.md)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-94c-gap-619-wave-92-audit-suite.md`
- **Parent gap:** [GAP-619](GAP-619-wave-92-post-wave-audit-suite.md)
- **Sister gap:** [GAP-637](GAP-637-admin-v1-controllers-preauthorize-missing.md) (backend security cùng /api/v1/admin scope)
- **Sister gap:** [GAP-638](GAP-638-admin-v1-api-contract-docs-typed-dtos.md) (API contract docs)
- **Backend:** `kitehub/kitehub-admin/src/main/java/com/kitehub/admin/api/v1/AdminRevenueController.java`
- **Rules:**
  - `.claude/rules/user-manual-content-standard.md` §2 row 8 (VND currency convention)
  - `.claude/rules/pre-handoff-self-test-completeness.md` §2.4

## Log

- **2026-05-18** — Initial write-up. Filed từ Wave 92 post-wave audit suite (GAP-619) UI audit finding P1. State-check confirmed `grep "0đ" kitehub-frontend/src/app/admin/revenue/` returns hardcoded literal in page.tsx — hook exists nhưng không wire. Wave 35 carry-forward surfaced Wave 92 audit. Phase 1 BETA UI gate /128 affected.
