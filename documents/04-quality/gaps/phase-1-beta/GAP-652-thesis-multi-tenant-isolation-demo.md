# GAP-652: Thesis multi-tenant isolation demo script — 5-phút secondary demo

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend + Demo)
**Phase:** phase-1-beta
**Found:** 2026-05-18
**Related Audits:** [thesis-vn-saas-benchmark](../../audits/persona-review/2026-05-18-thesis-vn-saas-benchmark.md)

## Current State (verified 2026-05-18)

| Piece | Status |
|---|---|
| Multi-tenant data isolation BE | ✅ PostgreSQL RLS implemented (V60 migration) |
| Per-tenant JWT scoping | ✅ wave 92 — tenant switcher |
| 5-phút demo script | ❌ missing |
| Pre-seeded 2 demo tenants | ❌ missing — không có `scripts/seed-thesis-demo-tenants.sh` |
| Demo evidence (DB query proof) | ❌ no SQL/screenshot showing cross-tenant separation |

## Problem

VN benchmark §4 explicit: "Multi-tenant isolation demo script — 5-phút secondary demo proof (log in as 2 different tenants, show data separation). Đây là điểm khác biệt không thesis nào khác có được."

Hội đồng VN CS thesis 2026 lần đầu thấy true multi-tenant SaaS — đây là unique differentiator. Demo phải LIVE, không slides-only, để prove implementation thực sự.

## Proposed Fix

### Step 1: Pre-seed 2 demo tenants

`scripts/seed-thesis-demo-tenants.sh`:
- Tenant A: "Trung tâm Anh ngữ Sky Education" (Owner: chị Hằng, 5 GV, 60 HS)
- Tenant B: "Trung tâm Toán Quang Minh" (Owner: anh Tâm, 3 GV, 40 HS)
- Mỗi tenant: 5-10 classes, 10-15 invoices, 20-30 attendance records
- Idempotent: drop + recreate cho repeatable demo
- Per `dev-readable-doc-language.md` §2 — VN-friendly sample data

### Step 2: 5-phút demo script

`documents/08-thesis/demo-scripts/multi-tenant-isolation-demo.md`:

```
PHÚT 1: Browser tab 1 — login admin@skyeducation.kitehub.me / pwd
  → Show: list 5 classes (Sky Education only)
  → Show: list 60 students (Sky Education only)
  → DevTools network tab → JWT decoded: tenantId=sky-education

PHÚT 2: Browser tab 2 (incognito) — login admin@quangminh.kitehub.me / pwd
  → Show: list 5 classes (Quang Minh only, different from Sky)
  → Show: list 40 students (Quang Minh only)
  → DevTools network tab → JWT decoded: tenantId=quang-minh

PHÚT 3: Backend proof — SSH to RDS read-only
  → psql: SELECT tenant_id, COUNT(*) FROM students GROUP BY tenant_id;
  → Show: 60 sky-education + 40 quang-minh rows
  → psql: SET request.jwt.claim.tenant_id = 'sky-education';
  → psql: SELECT COUNT(*) FROM students; → Returns 60 (RLS enforced)
  → psql: SET request.jwt.claim.tenant_id = 'quang-minh';
  → psql: SELECT COUNT(*) FROM students; → Returns 40 (RLS enforced)

PHÚT 4: Attempted cross-tenant access (security proof)
  → Sky Education admin attempts curl /api/v1/students với Quang Minh JWT
  → 403 Forbidden (gateway + RLS double-defense)
  → Show server log: "Cross-tenant access attempt blocked tenant_id=sky-education accessing tenant_id=quang-minh"

PHÚT 5: Branding isolation
  → Tab 1: Sky Education logo + blue theme
  → Tab 2: Quang Minh logo + green theme
  → CSS variables loaded per-tenant từ `/api/v1/branding`
  → Switch tabs → UI updates without page refresh
```

### Step 3: Demo evidence capture

Per GAP-651 thesis-image-curation:
- Screenshot 6-8 key moments (2 tab login, DevTools JWT diff, psql output, 403 attempt, branding diff)
- Caption Vietnamese per template
- Add Figure 4.X entries `figures/INDEX.md`

### Step 4: Backup recording

Per Failure-mode A3 risk (live demo bug):
- Pre-record demo 5-phút video (screen capture + voiceover)
- Fallback nếu live demo có issue
- Store `documents/08-thesis/demo-scripts/multi-tenant-isolation-demo.mp4`

## Acceptance Criteria

- [ ] `scripts/seed-thesis-demo-tenants.sh` idempotent + VN sample data
- [ ] Demo script document committed
- [ ] 6-8 demo screenshots captured per GAP-651
- [ ] Backup video recorded
- [ ] Local rehearsal: full 5-phút demo runs without bug
- [ ] Multi-tenant isolation evidence section ready Chapter 4

## Related

- GAP-646 thesis-docx-pipeline (Chapter 4 isolation section injection)
- GAP-651 thesis-image-curation (figure capture)
- V60 migration RLS implementation (existing)
- Wave 92 tenant switcher (existing)
- `agent-aws-access.md` Tier 1 read-only cho RDS query proof

## Log

- **2026-05-18 (created):** Filed per VN benchmark §4 "unique differentiator" recommendation.
