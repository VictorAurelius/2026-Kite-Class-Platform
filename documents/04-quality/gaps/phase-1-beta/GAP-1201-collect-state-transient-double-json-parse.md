# GAP-1201: collect-state.sh transient "0\n0" numeric vars + integer-expression error

**Status:** 🔵 OPEN
**Priority:** 🟢 P3
**Domain:** Meta
**Found:** 2026-06-11 (/start-session — discovery during non-audit work)
**Affects:** `.claude/skills/workflow/start-session/scripts/collect-state.sh` (+ tương tác với `scripts/repo-status.sh --json`)

## Problem

Chạy `/start-session` 2026-06-11 10:56: collect-state.sh in `line 515: [: 0\n0: integer expression expected` và nhiều field hiển thị đôi dòng ("Mức repo: RED\nunknown", "CVE: 0\n0 critical"). Mỗi biến `RS_*` chứa 2 dòng — tức stream JSON từ `repo-status.sh --json` lúc đó chứa 2 JSON document (jq emit value cho mỗi doc). Chạy lại cùng lệnh ngay sau đó → 1 JSON sạch, không tái hiện. Nghi transient: cold-cache code path hoặc race khi repo-status ghi cache song song.

Lưu ý: verdict RED/failing của lần đó là THẬT (docker-build-push fail 2× trên main) — bug chỉ là output bị duplicate/hỏng format, không phải false alarm logic.

## Proposed Fix

Defensive: collect-state.sh lấy `RS_JSON` qua `jq -s '.[0]'` (chỉ doc đầu) hoặc validate `jq -e type` trước khi extract; repo-status.sh audit code path cold-cache xem có chỗ nào in JSON 2 lần (vd cache-miss in cả stderr fallback lẫn stdout).

## Acceptance Criteria

- [ ] Tái hiện hoặc giải thích nguồn double-JSON (đọc repo-status.sh cold-cache path)
- [ ] collect-state.sh chống chịu multi-doc stream (không in "0\n0", không integer-expression error)

## Related

- Discovered in: session 2026-06-11 /start-session (per discovery-to-gap-inline-filing.md)
- Sister: GAP-1202 (query-gaps.sh keyword lookup — cùng session discovery)
