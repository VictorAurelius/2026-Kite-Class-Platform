---
audience: dev
title: Session handoff 2026-05-29 — demo-trio RST walk fix-forward (PR #1965)
status: complete
created: 2026-05-29
---

# Session handoff 2026-05-29 — demo-trio walk fix-forward

**Tóm tắt:** RST walk GAP-804/805/807 (Sky Education demo tenant) trên local stack. 3 gap shipped PARTIAL chưa từng walk live → walk surface ~20 bug → fix-forward → 3 gap DONE (verified 3-layer). + Trang chủ public branded (GAP-808 chain 6-fix). PR **#1965** (branch `fix/demo-trio-walk-gap804-805-807`, 2 commit, **CI đang chờ**).

## ✅ Đã ship (PR #1965 — chưa merge)

| Gap | Trạng thái | Nội dung |
|---|---|---|
| GAP-804 | DONE | logo multipart upload + S3 brandingPresigner public-endpoint (localhost:9100 path-style) + MinIO bucket `kite-branding-assets` provision |
| GAP-805 | DONE | KPI real 78 HV/5 khóa + 8 seed/schema bug fix (teacher_id UUID, status enum, heredoc, grades V74) + 77 HS tên VN + enrich (450 điểm danh/300 điểm/75 HĐ/56 TT) |
| GAP-807 | DONE | **core trust-pass**: brandingApi.get() không unwrap envelope → theme chưa từng apply + crash dashboard (vitest mock che). Fix unwrap 4 method + theme null-guards |
| GAP-808 | DONE | trang chủ public branded: V75 landing_pages + getOrCreateDefault inherit branding + writable tx + gateway public route + public.ts SSR baseURL + ThemeSync HSL + layout nav tenant-branded |
| GAP-809 | OPEN P2 | follow-up: FE↔BE `/classes` + `/invoices` flat-list 404 (BE course/student-scoped only) |

## 🔑 Context quan trọng

- **Tenancy canonical = shared-DB + RLS** (ADR-023, audit-confirmed). Demo data re-pointed `instance_id` `a5e00000` (UUID bịa) → **`e8ff87e1-69fc-4842-a263-7385c68b4ffb`** (gateway instance thật cho subdomain `sky-education`). Seeders + BrandingDataSeeder.SKY_TENANT_ID cập nhật e8ff87e1.
- **Owner demo**: `owner.sky@test.vn` / `Test@1234` (tenant e8ff87e1, seeded trong kitehub `users` table).
- **Gateway tenant resolution**: subdomain hoặc `X-Instance-Subdomain` header (dev). Demo browser/Playwright dùng header. `NEXT_PUBLIC_TENANT_ID=e8ff87e1` set trong compose (dev FE default = Sky deploy).
- **Evidence screenshots LOCAL-ONLY** `documents/08-thesis/evidence/demo-trio/` (gitignored — **KHÔNG commit lên remote** per user). Regen: `node kiteclass/kiteclass-frontend/e2e/capture-thesis-evidence.mjs`.

## 🎯 NEXT SESSION

1. **PR #1965 merge** khi CI green (V74/V75 migrations + FE build + mvn test). KHÔNG `--admin` — chờ CI.
2. **GAP-809**: quyết định BE flat-list endpoints (`GET /api/v1/classes`, `/invoices` tenant-scoped) HOẶC FE bỏ flat-list calls.
3. Minor: MinIO `kite-branding-assets` auto-create on startup (hiện provision thủ công); logo re-upload real file (preview 1×1 test PNG); `STORAGE_S3_PUBLIC_ENDPOINT` production CDN value khi GA.

## CI/state
- Local Docker UP (kiteclass-core + frontend rebuilt nhiều lần phiên này). AWS stack stopped.
- 2 commit: b22a00cd (demo-trio fix-forward) + dc403892 (layout nav tenant-branded).
- Findings doc đầy đủ: `documents/04-quality/audits/rst-html/2026-05-29-demo-trio-walk-findings.md`.
