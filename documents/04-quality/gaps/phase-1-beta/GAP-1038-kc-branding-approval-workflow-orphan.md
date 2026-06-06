# GAP-1038: `rebrand_approvals` approval workflow orphaned — KC-10 "→ approval" là misnomer

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Backend (kiteclass-core) + docs/scope
**Found:** 2026-06-06 (KC-10 G1 walk, FM-6)
**Affects:** `rebrand_approvals` table (V34 migration) + KC-10 flow naming (`flow-verification-campaign.md` §4)

## Problem

KC-10 trong campaign §4 đặt tên "Per-tenant branding **wizard → approval**" — gợi ý có workflow submit → pending → approve/reject. Walk + code-read cho thấy **KHÔNG có approval workflow**:

- `BrandingServiceImpl.updateBranding` (`:139-144`) apply + snapshot **ngay lập tức** — không qua pending/approve.
- `rollback` IS cơ chế apply (revert về version cũ) — không phải approval.
- Table `rebrand_approvals` (V34, đầy đủ cột status/initiator/approver/requested_at/approved_at/rejected_at) **orphan** — grep không thấy service consumer nào đọc/ghi.

Tức flow KC-10 thực tế = "branding edit + version history + rollback", KHÔNG có approval gate như tên gọi.

## Root Cause

`V34__create_rebrand_approvals_table.sql` tạo schema cho approval workflow nhưng service layer chưa implement (scaffold-as-DONE pattern) HOẶC feature bị deprecate mà table chưa drop.

## Proposed Fix

Chọn 1:
1. **Document rollback-as-apply** — update campaign §4 KC-10 name → "Per-tenant branding wizard (edit + version + rollback)", confirm no approval gate intended Phase 1. Defer `rebrand_approvals` cleanup.
2. **Implement approval workflow** — nếu Phase 1 cần approval gate (vd OWNER submit → PLATFORM_ADMIN approve cross-tenant rebrand), wire service consumer cho `rebrand_approvals`.
3. **Drop orphan table** — nếu confirmed không dùng, V+1 drop `rebrand_approvals` (giảm schema noise).

Khuyến nghị Option 1 (document) cho Phase 1 BETA — approval gate không phải MVP; revisit khi có nhu cầu cross-tenant rebrand governance.

## Acceptance Criteria

- [ ] Quyết định: implement / document-as-rollback / drop table
- [ ] Campaign §4 KC-10 name phản ánh đúng flow thực tế
- [ ] Nếu drop: V+1 migration; nếu document: scope note trong branding domain rules.md

## Related

- Discovered in: KC-10 G1 walk (Wave flow-kc10), pre-walk FM-6
- Scaffold-as-DONE governance: GAP-225 umbrella pattern
