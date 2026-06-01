---
id: GAP-829
title: Sanitize-on-write sister sites — Branding write-path + kiteclass JsonLd </script> escape
status: OPEN
priority: P1
phase: phase-1-beta
domain: Mixed
created: 2026-06-01
---

# GAP-829 — Sanitize-on-write sister sites (GAP-827 cross-flow sweep DEFER)

> Surfaced bởi GAP-827 cross-flow sweep (per `cross-flow-bug-class-sweep.md` §3). GAP-827 đóng landing write-path sanitize; sweep tìm 2 sister site cùng bug-class signature ("tenant free-text persisted → reused on non-auto-escape render surface") nhưng khác scope → DEFER theo `cross-flow-bug-class-sweep.md` §5.

## Problem

GAP-827 fix (`LandingPageContentSanitizer` + wire vào `LandingPageServiceImpl.updateLandingPage`) đóng landing write-path. Cross-flow sweep surfaced 2 sister site:

1. **`kiteclass-frontend/src/components/seo/JsonLd.tsx` thiếu `</script>` escape (FE-side, P1):**
   `kitehub-frontend` JsonLd có `escapeScriptContent()` (escape `</script>` + `<!--`) chống JSON-injection breakout. `kiteclass-frontend` JsonLd KHÔNG có defense này — chỉ `JSON.stringify`. Với GAP-827 backend sanitize-on-write strip `<`/markup ở source, vector này coi như đóng tại source, NHƯNG defense-in-depth FE-side vẫn nên match kitehub pattern (2 site cùng component, drift).

2. **`Branding` write-path (`BrandingMapper` → `Branding` entity, BE-side, P2):**
   `Branding.displayName`/`tagline`/`contactEmail` = tenant free-text persisted qua `BrandingMapper` (partial-update MapStruct, không sanitize). Hiện feed CSS vars + scalar JSX (auto-escaped) nên risk thấp; NHƯNG `getOrCreateDefault` copy `Branding.displayName` → `LandingPage.heroTitle` + `tagline` → landing — sanitize chỉ chạy khi tenant update landing, KHÔNG khi seed-from-branding. Branding write-path nên có cùng sanitize layer cho consistency.

## Acceptance Criteria

- [ ] `kiteclass-frontend` JsonLd.tsx thêm `escapeScriptContent()` match kitehub pattern (escape `</script>` + `<!--`)
- [ ] `Branding` write-path sanitize tenant free-text (`displayName`/`tagline`) — reuse `LandingPageContentSanitizer.sanitizeText` hoặc generic `TenantTextSanitizer` extract
- [ ] IT verify Branding sanitize + VN diacritic roundtrip
- [ ] Cross-flow re-sweep confirm no other `dangerouslySetInnerHTML` surface reads unsanitized tenant text

## Related

- GAP-827 (parent) — landing input safety; this gap = sweep DEFER sites
- `cross-flow-bug-class-sweep.md` §5 — DEFER verdict requires follow-up gap (this file)
- `vn-localization-audit-checklist.md` §5 — sanitize must preserve VN diacritics (reuse GAP-827 sanitizer)
- GAP-815 (landing editor UI) — Branding + landing self-service input both should be sanitize-gated
