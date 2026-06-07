# GAP-1040: Document generation SSRF — caller `data.logoUrl` override → server-side fetch arbitrary URL

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** Backend (kiteclass-core) — security (SSRF)
**Found:** 2026-06-06 (KC-11 G1 walk, FM-2)
**Closed:** 2026-06-07 (P3 G3 batch 2 fix — server-branding-wins + strip caller `*Url` keys + logoUrl host allowlist; walk-verified metadata-IP logoUrl stripped via :9000)
**Affects:** `DocumentBrandingAssembler:41-42` + `invoice.html:34` + `InvoiceRenderer:59-65` (OpenHTMLtoPDF) (kiteclass-core)

## Problem

KC-11 G1 walk: `POST /api/v1/documents/pdf/{preview,download}` cho phép caller inject `logoUrl` qua `data` map; URL này được server fetch khi render PDF → **SSRF** (server-side request forgery) tới internal/metadata endpoint.

**Walk evidence:**
```
POST /api/v1/documents/pdf/download (OWNER)
  {"templateId":"invoice","data":{"logoUrl":"http://169.254.169.254/latest/meta-data/","items":[]}}
→ 200, PDF 422KB generated; logs: DocumentGenerationRequestDto data={logoUrl=http://169.254.169.254/...}

POST same với logoUrl=http://kite-mailhog:8025/ (internal host)
→ 200, PDF generated; logoUrl accepted vào pipeline
```

Caller-supplied `logoUrl` override server branding → flow vào `<img th:src="${brand.logoUrl}">` (invoice.html:34) → OpenHTMLtoPDF (InvoiceRenderer:59-65) resolve URL server-side. Attacker (bất kỳ ADMIN/OWNER/TEACHER) point logoUrl tới cloud metadata (`169.254.169.254`), internal services, hoặc port-scan internal network.

## Root Cause

`DocumentBrandingAssembler:41-42` `merged.putAll(request.data())` — **caller data WIN** over server-injected branding. Không có allowlist host cho logoUrl. OpenHTMLtoPDF default fetch `http(s)://` URLs trong `th:src`. Server branding lẽ ra phải authoritative, không cho caller override fetch-able URL fields.

**Caveat:** caller text fields dùng `th:text` (escaped — safe, không HTML injection); CHỈ `th:src` logoUrl là SSRF vector.

## Proposed Fix

1. **Không cho caller override branding fetch-able fields** — `DocumentBrandingAssembler` áp branding server-side AFTER caller data (server wins cho logoUrl/faviconUrl/imageUrl), hoặc strip caller `logoUrl`/`*Url` keys.
2. **Allowlist host** cho logoUrl (chỉ MinIO/CDN domain: `cdn.kitehub.me`, `assets.kitehub.me`, `kite-minio`) — reuse `landing.allowed-image-hosts` (application.yml:149).
3. **Disable OpenHTMLtoPDF external URL fetch** — config URI resolver chỉ cho phép data-URI/classpath/allowlisted host.

## Acceptance Criteria

- [x] Caller `data.logoUrl=http://169.254.169.254/...` → KHÔNG fetch — walk via :9000: caller logoUrl stripped, log `Dropping caller-supplied fetch-able URL key 'logoUrl'... (SSRF guard, GAP-1040)`; server branding wins (closes null-branding bypass too)
- [x] logoUrl host ngoài allowlist → reject/skip (no egress) — `isAllowedLogoHost()` via `landing.allowed-image-hosts` (reuse GAP-827 sanitizer); disallowed → key dropped → `th:if` skips `<img>`
- [x] PDF render dùng server branding logoUrl, không caller-injected — assembler applies server branding first, caller `*Url`/`*url` keys stripped
- [x] Test: SSRF probe (internal host) → no server-side request — `DocumentBrandingAssemblerTest` 8 SSRF cases (caller logoUrl/branding.logoUrl/faviconUrl/backgroundImageUrl stripped; metadata-IP + non-allowlist skipped); 18 tests PASS. NOTE: Fix #3 (OpenHTMLtoPDF URI resolver belt-and-suspenders) DEFERRED — fixes #1+#2 fully close vector (only allowlisted hosts reach renderer); see follow-up note below

## Related

- Discovered in: KC-11 G1 walk (Wave flow-kc11), pre-walk FM-2
- Allowlist precedent: `landing.allowed-image-hosts` (application.yml:149)
- File-render security class. Batch Wave security-1.
- Note: caller text fields th:text escaped (safe); XLSX formula injection tested NEGATIVE (caller rows không map cells, data từ DB).

## Follow-up (optional defense-in-depth — NOT blocking, vector closed)

Proposed Fix #3 (OpenHTMLtoPDF `FSUriResolver` allowlist at render layer) DEFERRED — belt-and-suspenders only. The SSRF vector is fully closed by #1 (caller `*Url` stripped, server branding authoritative) + #2 (logoUrl host allowlist) — only allowlisted hosts (MinIO/CDN) can ever reach the renderer, and caller can no longer inject any fetch-able URL. #3 would additionally block egress to *allowlisted external* hosts (e.g. cdn.kitehub.me) at the render layer — marginal gain, cross-module coupling risk to font/data-URI resolution. Tracked as P3 hardening candidate; no separate gap filed (no open vector). Revisit if a future requirement forbids egress to allowlisted external hosts during PDF render.
