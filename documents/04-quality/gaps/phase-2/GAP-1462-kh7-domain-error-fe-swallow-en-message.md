# GAP-1462: KH-7 domain sad-path — FE nuốt backend reason + BE error message English

**Status:** 🔵 OPEN
**Priority:** 🟠 P2
**Domain:** Mixed
**Found:** 2026-06-16 (Flow Verification Campaign — KH-7 browser re-walk)
**Affects:** kitehub-frontend CustomDomainTab + kitehub-subscription domain controller

## Problem

KH-7 walk: submit reserved/invalid domain → FE hiển thị raw `"Request failed with status code 400"` (axios message EN) thay vì backend `ProblemDetail.detail`. FE đọc `error.message` thay vì `error.response.data.detail` (cùng class GAP-926). Thêm: BE `detail` = "Domain '...' is reserved by the platform" / "Invalid domain format" tenant-facing nhưng English (vi phạm vn-localization-audit-checklist §2). Batch 2 thành 1 wave-fix nhỏ.

## Acceptance Criteria

- [ ] FE CustomDomainTab đọc ProblemDetail.detail render message thật
- [ ] BE domain error message → tiếng Việt
- [ ] Browser re-walk sad-path confirm

## Related

- Discovered in: 2026-06-16 KH-7 browser walk · same class GAP-926
