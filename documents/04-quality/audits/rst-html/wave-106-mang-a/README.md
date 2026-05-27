---
audience: dev
title: RST HTML Dashboard — Wave 106 Mảng A (Khách ẩn danh)
created: 2026-05-27
wave: meta-6
gaps: []
---

# RST HTML Dashboard — Wave 106 Mảng A

**Last Updated:** 2026-05-27

**Mảng A — Khách ẩn danh (anonymous prospect):** 3 luồng walk-through KiteHub frontend tại `localhost:3001`.

## Tệp trong thư mục

| Tệp | Mô tả |
|---|---|
| `index.html` | Dashboard 1 trang — SHIP/HOLD verdict + 3 luồng drilldown + screenshot thumbnails |
| `screenshots/a1-trang-chu.png` | A1 — Trang chủ ẩn danh `/` (1440×900 desktop) |
| `screenshots/a2-bieu-mau-beta.png` | A2 — Biểu mẫu yêu cầu beta `/beta-status` |
| `screenshots/a3-trang-chinh-sach.png` | A3 — Trang chính sách / pricing `/pricing` |

## Cách dùng

### Mở dashboard

```bash
python3 -m http.server 8000 --directory documents/04-quality/audits/rst-html/wave-106-mang-a
# Mở browser http://localhost:8000/
```

### Capture lại screenshots

```bash
mkdir -p /tmp/rst-screenshots/wave-106-mang-a
cd kitehub/kitehub-frontend
pnpm test:e2e e2e/_rst-wave-106-mang-a.spec.ts --project=chromium
cp /tmp/rst-screenshots/wave-106-mang-a/*.png \
  documents/04-quality/audits/rst-html/wave-106-mang-a/screenshots/
```

### Annotate screenshots (mũi tên đỏ + viền vàng + số bước)

```bash
bash scripts/render-rst-screenshots.sh \
  --input /tmp/rst-screenshots/wave-106-mang-a \
  --output /tmp/rst-screenshots/wave-106-mang-a/annotated \
  --manifest documents/04-quality/audits/rst-html/wave-106-mang-a/annotations.yaml
```

(Manifest format + ImageMagick prerequisite: xem `scripts/render-rst-screenshots.sh` header.)

## Liên quan

- Wave 106 plan: [`documents/03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md`](../../../../03-planning/waves/wave-2026-05-23-106-rst-phase-1-beta-full-walk.md) §3 Scope (Mảng A 3 luồng A1+A2+A3)
- Wave meta-6 plan: [`documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md`](../../../../03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md) §2 Bucket C
- Playwright spec: [`kitehub/kitehub-frontend/e2e/_rst-wave-106-mang-a.spec.ts`](../../../../../kitehub/kitehub-frontend/e2e/_rst-wave-106-mang-a.spec.ts)
- Sister wave dashboard (Wave 107 anonymous walk): tham khảo `_rst-wave-107-anonymous.spec.ts` cùng pattern
- Layer boundary: [`/.claude/rules/e2e-rst-test-layer-boundary.md`](../../../../../.claude/rules/e2e-rst-test-layer-boundary.md) §3 RST→E2E promotion mandate
- Annotation tooling reuse cho user manual: [`/.claude/rules/user-manual-content-standard.md`](../../../../../.claude/rules/user-manual-content-standard.md) §2 row 6 (annotated screenshot mandate)

## Trạng thái

🟢 **PASS** — 3/3 luồng walk-through Mảng A completed; screenshots captured live `localhost:3001`; Vietnamese narrative + VND format verified (A3 `500.000₫/tháng` present).
