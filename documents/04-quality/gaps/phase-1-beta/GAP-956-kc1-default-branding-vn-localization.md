# GAP-956: Default branding hardcoded English "KiteClass" — vi phạm VN-localization audit checklist

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Backend
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Tenant default settings) — VN-localization compliance
**Defer-to:** After Wave flow-kh3 finish

## Problem

`tenant-settings/rules.md` BR-SET-03: default display_name = `"KiteClass"`. BR-SET-04: tagline = `"Nen tang quan ly trung tam dao tao"` (no diacritics — UTF-8 mojibake risk hoặc cố tình ASCII). Owner Tuấn vào admin lần đầu thấy header "KiteClass — Nen tang quan ly..." → cảm giác "đây là sản phẩm KiteClass, không phải trung tâm của tôi" → trust drop + confusion. Vi phạm `vn-localization-audit-checklist.md` §2 4-section checklist. Per benchmark B2 (Vietnamese persona role labels). Surfaced: persona Finding 1.4 + benchmark B2.

## Proposed Fix

Pre-fill `display_name` từ `tenantName` field owner đã provide tại KH-2b signup (thay vì hardcoded "KiteClass"). Tagline default Vietnamese-with-diacritics: "Nền tảng quản lý trung tâm đào tạo". Verify UTF-8 encoding end-to-end.

## Acceptance Criteria

- [ ] BR-SET-03 default = signup-provided `tenantName` không phải "KiteClass" hardcoded
- [ ] BR-SET-04 tagline có VN diacritics
- [ ] Walk fresh signup: admin header shows actual tenant name with VN diacritics

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-{tenant-provisioning,external-benchmark}.md
- Sister rule: `.claude/rules/vn-localization-audit-checklist.md` v1.0.0
- Flow Verification Campaign §4 row KC-1
