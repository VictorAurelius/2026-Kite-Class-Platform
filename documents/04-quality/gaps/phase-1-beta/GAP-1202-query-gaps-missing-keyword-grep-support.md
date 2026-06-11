# GAP-1202: query-gaps.sh thiếu keyword/--grep lookup mà nhiều rule cite + empty-result in dòng rác

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Meta
**Found:** 2026-06-11 (landing-100 G2 session — query GAP-811/1077 trả rỗng)
**Affects:** `scripts/query-gaps.sh` + mọi rule/skill cite keyword lookup

## Problem

1. **Thiếu capability được cite:** `design-first-investigation-order.md` §3 cite `bash scripts/query-gaps.sh <keyword>`; `discovery-to-gap-inline-filing.md` §4 + `audit-to-gap-pipeline.md` cite `query-gaps.sh --grep <keyword>`. Script thực tế CHỈ hỗ trợ positional priority/status/phase + `--count`/`--domain` — không có keyword/ID/grep mode. Gọi `query-gaps.sh GAP-811` → arg bị hiểu là PRIORITY → filter `$5=="GAP-811"` → 0 row. Drift rule-cite ↔ script capability (sister script `query-rules.sh` CÓ `--grep` — pattern không đồng đều).
2. **Empty-result UX:** khi 0 row, pretty-print awk vẫn in 1 dòng format rỗng (`| | | %`) thay vì thông báo "no match" — caller (Claude/dev) dễ hiểu nhầm CSV hỏng.

## Proposed Fix

Thêm `--grep <pattern>` (match toàn row, case-insensitive) + auto-detect arg dạng `GAP-NNN` → ID lookup; empty result → in `Không có gap khớp filter` + exit 0. Đồng bộ usage header. (~20 LOC, mirror `query-rules.sh --grep`.)

## Acceptance Criteria

- [ ] `query-gaps.sh GAP-811` trả đúng 1 row GAP-811
- [ ] `query-gaps.sh --grep middleware` trả các row chứa keyword
- [ ] 0 match → message rõ, không in dòng format rỗng
- [ ] Self-test fixtures trong Script tests CI (nếu có harness sẵn)

## Related

- Discovered in: session 2026-06-11 (per discovery-to-gap-inline-filing.md)
- Sister: GAP-1201 (collect-state transient), `meta-csv-index-pattern.md` §3 query helper convention
