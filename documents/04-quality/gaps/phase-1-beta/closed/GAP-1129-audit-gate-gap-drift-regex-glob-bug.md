# GAP-1129: audit-gate.py gap-drift check — `GAP-\d{3}` substring + non-recursive glob false positives

**Status:** 🟢 DONE
**Priority:** 🟡 P2
**Domain:** DevOps
**Found:** 2026-06-10 (discovered while merging PR #2292 — audit-gate hook false-flagged GAP-112)
**Affects:** `.claude/hooks/audit-gate.py` `check_gap_doc_drift()` (every PR-merge gate run, all sessions)

## Problem

Khi merge PR #2292 (đóng GAP-1122/1127/1128), `audit-gate.py` PostToolUse hook báo violation **"Gap doc drift — GAP-112 (gap file touched but Log doesn't mention #2292)"** — nhưng GAP-112 (distributed tracing) KHÔNG nằm trong diff. False positive.

Hai bug trong cùng `check_gap_doc_drift()`:

1. **Line 507 — unanchored regex.** `re.findall(r"GAP-\d{3}", combined)` khớp đúng 3 chữ số, không neo biên → `GAP-1122` cắt thành `GAP-112`, `GAP-1127`→`GAP-112`, `GAP-1128`→`GAP-112`. Mọi gap 4-chữ-số có 3 số đầu trùng 1 gap thật → drift nhầm vào gap prefix. Force-multiplier: ảnh hưởng MỌI PR/session đụng gap GAP-1xxx.

2. **Line 518 — non-recursive glob.** `gaps_dir.glob(f"{gap_id}-*.md")` không đệ quy → không tìm thấy gap file trong subdir `phase-1-beta/`, `closed/`... (layout chuẩn per `gap-folder-organization.md` v2.0.0) → `log_has_pr` luôn False → false "Log doesn't mention #N" cho mọi gap đã chuyển subdir.

Cùng class với bug #515 (`gap_id in f` substring trong `touched` check).

## Fix (this PR)

- Line 507: `r"GAP-\d{3}"` → `r"GAP-\d{3,}"` (khớp full ID, 3-digit gaps như GAP-402 vẫn match).
- Line 515: `gap_id in f` → `Path(f).name.startswith(f"{gap_id}-")` (khớp filename prefix chính xác, không substring).
- Line 518: `glob(f"{gap_id}-*.md")` → `glob(f"**/{gap_id}-*.md")` (đệ quy qua subdir + closed/).

## Acceptance Criteria

- [x] `re.findall(r"GAP-\d{3,}", "GAP-1122 GAP-1127 GAP-1128")` → `[GAP-1122, GAP-1127, GAP-1128]` (không còn collapse về GAP-112)
- [x] 3-digit gap (GAP-402) vẫn match đúng (no regression)
- [x] `touched` check dùng filename-prefix match, không substring
- [x] glob đệ quy tìm được gap file trong `phase-1-beta/closed/`
- [x] `python3 -m py_compile audit-gate.py` PASS + self-test PASS

## Related

- Discovered in: PR #2292 merge (commit 3cad485e) post-merge audit-gate hook
- Sister bug class: `cross-flow-bug-class-sweep.md` §1 (same regex/substring class — swept all 3 sites in this function)
- Statically-detectable per `cross-flow-bug-class-sweep.md` §4.1 — but scope is 1 function, swept inline (no persistent detector warranted for a 1-function regex fix)
