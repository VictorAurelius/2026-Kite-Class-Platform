# 05-guides/user-manual — Tài liệu Hướng dẫn sử dụng KiteHub (tenant-facing)

**Last Updated:** 2026-05-15
**Rules:** [`.claude/rules/user-manual-content-standard.md`](../../../.claude/rules/user-manual-content-standard.md) · [`.claude/rules/dev-readable-doc-language.md`](../../../.claude/rules/dev-readable-doc-language.md) · [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Folder này chứa toàn bộ user manual tenant-facing (P1 Solo Teacher / P2 Center Owner / P3 Center Manager / Anonymous Prospect / Platform Admin). Mỗi page phải tuân thủ 15-item checklist trong `user-manual-content-standard.md` §2. Nội dung tiếng Việt; render Next.js MDX qua route `kitehub/kitehub-frontend/src/app/help/**`.

---

## Directory Map

| Path | Persona | Trạng thái | Số trang |
|---|---|---|---|
| `README.md` | (index folder) | ✅ Wave 79 Bucket F1 → Wave 80 Bucket D refresh | 1 |
| `anonymous/` | Anonymous Prospect (Vy + sếp Vy) | ✅ Wave 79 Bucket F1 | 5 (`index`, `pricing`, `beta-access`, `terms`, `faq`) |
| `anonymous/screenshots/` | Annotated screenshots cho anonymous | ✅ Wave 80 Bucket D capture (Playwright Tier 1 raw + Tier 2 annotation deferred) | 15 |
| `p2-owner/` | P2 Center Owner (chị Hằng) | ✅ Wave 80 Bucket D source-text | 5 (`index`, `pricing-billing`, `invite-staff`, `branding`, `settings`) |
| `p2-owner/screenshots/` | Annotated screenshots cho Owner | 🟡 Placeholder Wave 80 Bucket D; integration post Bucket B+C merge | 0 (placeholder comments) |
| `p3-manager/` | P3 Center Manager (anh Tâm) | ✅ Wave 80 Bucket D source-text | 5 (`index`, `daily-operations`, `reports`, `permissions`, `accept-invite`) |
| `p3-manager/screenshots/` | Annotated screenshots cho Manager | 🟡 Placeholder Wave 80 Bucket D; integration post Bucket B+C merge | 0 (placeholder comments) |
| `platform-admin/` | Platform Admin (em Mai) | ✅ Wave 80 Bucket D | 5 (`index`, `beta-approval`, `impersonation`, `monitoring`, `tenant-management`) |
| `platform-admin/screenshots/` | Annotated screenshots cho Admin | ✅ Wave 80 Bucket D capture | 15 |
| `p1-solo-teacher/` | P1 Solo Teacher | ⏳ Wave 81+ scope | (~5 pages planned) |

Platform Admin (Mai) — Wave 80 Bucket D mở rộng scope: ngoài internal runbook `documents/05-guides/operations/`, thêm 5 trang user manual tenant-facing để admin có self-onboarding nhất quán.

---

## File Placement Rules

- ✅ **Belongs here:** tenant-facing help content user (Hằng / Tâm / Vy / Solo Teacher / Mai admin) đọc khi cần hỗ trợ
- ✅ **Belongs here:** annotated screenshots tenant-facing (`{persona}/screenshots/{topic}-step-{N}.png`)
- ❌ **Does NOT belong here:** internal deep ops runbook → `documents/05-guides/operations/`
- ❌ **Does NOT belong here:** deploy/secrets-seeding runbook → `documents/05-guides/deploy/`
- Naming: `{persona-slug}/{topic-slug}.md` (lowercase kebab-case)

---

## Required content (per page)

Mỗi `.md` page PHẢI satisfy 15-item checklist trong `user-manual-content-standard.md` §2:

1. Foundation (5 items): frontmatter + TL;DR + persona landing + Vietnamese narrative + support footer
2. Visual (3 items): annotated screenshots + VN sample data + VND currency + date VN
3. Trust (4 items): last-updated badge + ≥3 discoverability entry points + WCAG AA + Fuse.js search
4. Format (3 items): print CSS + mobile responsive + PDF auto-gen script

Pre-merge reviewer enforces per `user-manual-content-standard.md` §5.1.

---

## Archive Policy

Move trang sang `documents/07-archived/user-manual-YYYY/` khi:
- Feature underlying trang bị deprecate (vd: pricing tier rebrand)
- App version major bump rewrite (vd: v1.0.0 → v2.0.0 KiteHub Track 3 launch)
- Trang ≥180 ngày stale + no recent reference (per `docs-folder-structure.md` §3)

---

## Key Documents

### Anonymous Prospect (Wave 79 Bucket F1 sample)

- [`anonymous/index.md`](anonymous/index.md) — Chào mừng đến KiteHub: tổng quan + use case
- [`anonymous/pricing.md`](anonymous/pricing.md) — Bảng giá KiteHub: gói FREE/PRO/PREMIUM/ENTERPRISE
- [`anonymous/beta-access.md`](anonymous/beta-access.md) — Tham gia chương trình Beta: cách yêu cầu truy cập
- [`anonymous/terms.md`](anonymous/terms.md) — Điều khoản dịch vụ: tóm tắt
- [`anonymous/faq.md`](anonymous/faq.md) — Câu hỏi thường gặp dành cho anonymous prospect

### P2 Center Owner (Wave 80 Bucket D)

- [`p2-owner/index.md`](p2-owner/index.md) — Tổng quan Chủ trung tâm
- [`p2-owner/pricing-billing.md`](p2-owner/pricing-billing.md) — Bảng giá + Thanh toán
- [`p2-owner/invite-staff.md`](p2-owner/invite-staff.md) — Mời Manager + Giáo viên
- [`p2-owner/branding.md`](p2-owner/branding.md) — Tuỳ chỉnh logo + màu
- [`p2-owner/settings.md`](p2-owner/settings.md) — Cấu hình chung

### P3 Center Manager (Wave 80 Bucket D)

- [`p3-manager/index.md`](p3-manager/index.md) — Tổng quan Manager
- [`p3-manager/daily-operations.md`](p3-manager/daily-operations.md) — Vận hành hàng ngày
- [`p3-manager/reports.md`](p3-manager/reports.md) — Read-only báo cáo
- [`p3-manager/permissions.md`](p3-manager/permissions.md) — Quyền hạn STAFF
- [`p3-manager/accept-invite.md`](p3-manager/accept-invite.md) — Quy trình accept invite

### Platform Admin (Wave 80 Bucket D)

- [`platform-admin/index.md`](platform-admin/index.md) — Tổng quan Platform Admin
- [`platform-admin/beta-approval.md`](platform-admin/beta-approval.md) — Duyệt beta tenant
- [`platform-admin/impersonation.md`](platform-admin/impersonation.md) — Impersonation 30s TTL
- [`platform-admin/monitoring.md`](platform-admin/monitoring.md) — Theo dõi sức khoẻ hệ thống
- [`platform-admin/tenant-management.md`](platform-admin/tenant-management.md) — Quản lý tenant bulk operations

### Next.js render routes

- `kitehub/kitehub-frontend/src/app/help/anonymous/[slug]/page.tsx` (Wave 79 Bucket F1)
- `kitehub/kitehub-frontend/src/app/help/p2-owner/[slug]/page.tsx` (Wave 80 Bucket D)
- `kitehub/kitehub-frontend/src/app/help/p3-manager/[slug]/page.tsx` (Wave 80 Bucket D)
- `kitehub/kitehub-frontend/src/app/help/platform-admin/[slug]/page.tsx` (Wave 80 Bucket D)

### PDF + screenshot scripts (Wave 80 Bucket D)

```bash
# Render single persona PDF (Puppeteer headless → A4 portrait, gitignored)
bash scripts/render-user-manual-pdf.sh anonymous
bash scripts/render-user-manual-pdf.sh p2-owner
bash scripts/render-user-manual-pdf.sh p3-manager
bash scripts/render-user-manual-pdf.sh platform-admin

# Render all 4 personas
bash scripts/render-user-manual-pdf.sh --all

# Capture annotated screenshots (Playwright vi-VN 1440×900)
bash scripts/capture-user-manual-screenshots.sh anonymous
bash scripts/capture-user-manual-screenshots.sh platform-admin
```

PDFs gitignored (`*-manual.pdf` per `.gitignore`); screenshots checked in (visual reference).
