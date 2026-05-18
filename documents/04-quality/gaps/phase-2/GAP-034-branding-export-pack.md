# GAP-034: Branding Export Pack (ZIP + PDF Style Guide)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Product / Backend
**Detected:** 2026-04-14 (simulation: Owner × Termination × C3 Data)

## Problem

Tenant có branding trong KiteClass, nhưng:
- ❌ Cần logo cho offline marketing (flyer, banner in)
- ❌ Muốn share style guide với designer outsource
- ❌ Export trước khi churn để keep assets
- ❌ GDPR portability — export all data

## Proposed Fix

### 1. Export ZIP

Endpoint: `GET /api/v1/branding/{tenantId}/export.zip`

Contents:
```
branding-export-{tenantId}-{date}.zip
├── logo/
│   ├── logo.png (original upload)
│   ├── logo.svg (if SVG)
│   ├── logo-white.png (variant)
│   ├── favicon-16.png
│   ├── favicon-32.png
│   └── favicon-512.png
├── images/
│   ├── banner-desktop.png
│   ├── banner-mobile.png
│   ├── hero.png
│   └── thumbnails/*.png
├── style-guide.pdf
├── brand-config.json  (theme + metadata)
└── README.md
```

### 2. PDF Style Guide Generator

Using Apache Batik / iText:
- Cover page với logo
- Color palette với hex codes + usage
- Typography (fonts, sizes)
- Logo clear space, min size rules
- Image examples (banner, hero, thumbnails)
- Do's and Don'ts

### 3. Multiple Logo Formats

- PNG @1x, @2x, @3x
- SVG (vector)
- Favicon sizes (16, 32, 180, 192, 512)
- Black + white variants
- Transparent background

### 4. Tier-gating

- FREE: Basic ZIP (logo + brand-config.json)
- PRO: + multiple logo formats
- PREMIUM: + full PDF style guide
- ENTERPRISE: + vector sources + source files

### 5. GDPR Export

Separate "Data Export" button:
- All branding data in machine-readable format
- Includes version history
- Includes usage logs
- Comply with GDPR Article 20 (portability)

## Acceptance Criteria

- [ ] Export ZIP endpoint với tier-appropriate content
- [ ] PDF style guide generation (PREMIUM+)
- [ ] Multiple logo format conversion
- [ ] GDPR data export (all formats)
- [ ] Async job for large exports (via queue)
- [ ] Email download link when ready
- [ ] 7-day download expiry

## Dependencies

- MinIO for asset source
- PDF generation library (Apache Batik or similar)

## Log

- 2026-04-14 — Owner Termination stage uncovered
