# GAP-1243: CSP report-only thiếu local origins — console noise mỗi local walk

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Frontend
**Found:** 2026-06-12 (G2★ walk branding-100 — user console flood CSP violations)
**Affects:** KH `next.config.js` CSP report-only (`connect-src` thiếu `http://localhost:9000`; `frame-src` thiếu `http://localhost:3000`) + KC tương tự

## Problem

Walk local: console flood "Connecting to http://localhost:9000/api/auth/login violates
connect-src..." + "Framing http://localhost:3000 violates frame-src..." — đều **report-only**
(không chặn) nhưng nhiễu lớn khi debug + che vi phạm thật. Policy hardcode production origins,
không env-driven cho local (cùng class GAP-1238 frame-ancestors đã fix env-driven).

## Proposed Fix

CSP origins env-driven như pattern GAP-1238: local default thêm `http://localhost:9000`
(connect-src KH) + `http://localhost:3000` (frame-src KH cho preview iframe) + ws tương ứng;
production giữ nguyên danh sách kitehub.me.

## Acceptance Criteria

- [ ] Local walk wizard end-to-end: 0 CSP report-only violation trong console
- [ ] Production CSP header không đổi (chỉ thêm origins khi env local set)

## Related

- Discovered in: G2★ walk branding-100 2026-06-12
- GAP-1238 (CSP frame-ancestors env-driven — same class, fixed #2367)
