# 05-guides/user-manual — Tài liệu Hướng dẫn sử dụng KiteHub (tenant-facing)

**Last Updated:** 2026-05-14
**Rules:** [`.claude/rules/user-manual-content-standard.md`](../../../.claude/rules/user-manual-content-standard.md) · [`.claude/rules/dev-readable-doc-language.md`](../../../.claude/rules/dev-readable-doc-language.md) · [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Folder này chứa toàn bộ user manual tenant-facing (P1 Solo Teacher / P2 Center Owner / P3 Center Manager / Anonymous Prospect). Mỗi page phải tuân thủ 15-item checklist trong `user-manual-content-standard.md` §2. Nội dung tiếng Việt; render Next.js MDX qua route `kitehub/kitehub-frontend/src/app/help/**`.

---

## Directory Map

| Path | Persona | Trạng thái | Số trang |
|---|---|---|---|
| `README.md` | (index folder) | ✅ Wave 79 Bucket F1 | 1 |
| `anonymous/` | Anonymous Prospect (Vy + sếp Vy) | ✅ Wave 79 Bucket F1 sample | 5 (`index`, `pricing`, `beta-access`, `terms`, `faq`) |
| `anonymous/screenshots/` | Annotated screenshots cho anonymous | 🟡 placeholder Wave 79; capture Wave 80+ | 0 (placeholders inline) |
| `p2-owner/` | P2 Center Owner (chị Hằng) | ⏳ Wave 80+ Bucket F2 (gated F1 review) | (~10 pages planned) |
| `p3-manager/` | P3 Center Manager (anh Tâm) | ⏳ Wave 80+ Bucket F2 | (~5-7 pages planned) |
| `p1-solo-teacher/` | P1 Solo Teacher | ⏳ Wave 80+ Bucket F2 | (~5 pages planned) |

Platform Admin (Mai) NOT trong scope folder này — internal runbook ở `documents/05-guides/operations/` (per outside-in audit Persona 4 finding).

---

## File Placement Rules

- ✅ **Belongs here:** tenant-facing help content user (Hằng / Tâm / Vy / Solo Teacher) đọc khi cần hỗ trợ
- ✅ **Belongs here:** annotated screenshots tenant-facing (`{persona}/screenshots/{topic}-{step}.png`)
- ❌ **Does NOT belong here:** internal runbook → `documents/05-guides/operations/`
- ❌ **Does NOT belong here:** deploy/secrets-seeding runbook → `documents/05-guides/deploy/`
- ❌ **Does NOT belong here:** admin self-onboarding → `documents/05-guides/operations/` (Mai persona)
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

### Next.js render route (Wave 79 Bucket F1)

- `kitehub/kitehub-frontend/src/app/help/anonymous/[slug]/page.tsx` — MDX render route với TOC sidebar + Fuse.js search

### PDF render script (Wave 79 Bucket F1)

```bash
bash scripts/render-user-manual-pdf.sh anonymous
# → documents/05-guides/user-manual/anonymous/anonymous-manual.pdf (gitignored)
```
