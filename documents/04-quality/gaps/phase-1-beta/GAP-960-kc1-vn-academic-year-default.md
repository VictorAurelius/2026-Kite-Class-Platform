# GAP-960: VN Năm học default missing — grade rollup ambiguity

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant default settings) — VN K-12 calendar semantics
**Defer-to:** After Wave flow-kh3 finish

## Problem

Default tenant settings không có "Niên khóa hiện tại" picker. `documents/01-business/kiteclass/academic-year/` exists nhưng tenant provisioning saga chưa wire default. Bác Hùng tạo lớp Tiếng Anh tháng 6/2026 → hệ thống auto-assign niên khóa 2025-2026 hay 2026-2027? Logic ambiguous → grade rollup sai khi xuất báo cáo cuối năm cho phụ huynh. Per benchmark B1+C2: MISA QLTH R85+ simplified onboarding "chỉ cần khai báo năm học" — required field tại provision, default = current năm học (Sep YYYY → May YYYY+1). Year rollover = separate lifecycle event ≠ tenant cancel. Surfaced: persona Finding 3.2 + benchmark B1+C2.

## Proposed Fix

Wire Năm học default trong saga: provisioning auto-set `current_academic_year = compute_vn_academic_year(now)` (Sep YYYY → May YYYY+1). FE provision form pre-fill + editable. Year rollover lifecycle event triggered tháng 9 hàng năm.

## Acceptance Criteria

- [ ] `tenant_settings.academic_year_start` field populated post-provision
- [ ] Logic: tháng 1-5 → niên khóa cũ (Sep YYYY-1 → May YYYY); tháng 9-12 → niên khóa mới (Sep YYYY → May YYYY+1); tháng 6-8 = transition window with explicit user choice
- [ ] Grade rollup logic uses settings.academic_year_start (no ambiguity)

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04 (persona + benchmark)
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,external-benchmark}.md
- Sister: GAP-947 (TenantSettings entity — parent gap)
- Flow Verification Campaign §4 row KC-1
