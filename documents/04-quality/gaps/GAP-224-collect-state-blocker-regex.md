# GAP-224: collect-state.sh blocker regex misses sub-IDs and prose cross-refs

**Status:** 🔵 OPEN
**Priority:** 🟡 P3 Meta (workflow / session orchestration) — minor accuracy fix, no GA impact
**Domain:** Skills / Workflow
**Found:** 2026-04-26 (session-start ROADMAP cleanup — option 3 follow-up after GAP-046 row removed)
**Affects:** Every `/start-session` invocation — blocker line trong session summary có thể lệch thực tế

## Problem

`.claude/skills/workflow/start-session/scripts/collect-state.sh` parse blockers từ ROADMAP.md "GA Blockers remaining" section bằng regex `GAP-[0-9]+` + `sort -u | head -6`. Hai khiếm khuyết:

1. **Sub-ID collapse:** GAP-222a, GAP-222b, GAP-222c đều rút gọn thành `GAP-222` qua regex (thiếu suffix `[a-z]?`), rồi `sort -u` gộp 3 entries thành 1.
2. **Cross-ref pollution:** prose như "BLOCKS GAP-006" hoặc "(unblocks GAP-222c)" trong cell title bị scrape thành blocker, mặc dù gap đó nằm ở row khác/không phải blocker thực.
3. **head -6 cắt theo alphabetical order** (qua `sort -u`), không theo thứ tự bảng — high-priority gap số lớn (GAP-223 P0 Meta) bị đẩy ra ngoài top-6 nếu các số nhỏ hơn đứng trước.

Kết quả thực tế (post ROADMAP cleanup 2026-04-26):
- Bảng có 6 blockers: GAP-005, GAP-011, GAP-014, GAP-016, GAP-222a, GAP-223
- Collector output: `GAP-005;GAP-006;GAP-011;GAP-014;GAP-016;GAP-222;` — sai 2/6 (GAP-006 false positive cross-ref, GAP-222 collapse, mất GAP-223)

## Current State (verified 2026-04-26)

| Aspect | State | Evidence |
|--------|:-----:|----------|
| Regex pattern | ❌ `GAP-[0-9]+` | `collect-state.sh` ~line 100 trong BLOCKERS block |
| Cross-ref filter | ❌ none | Mọi GAP-XXX trong row bị scrape |
| Order preservation | ❌ alphabetical | `sort -u` phá thứ tự priority bảng |
| Caps | `head -6` | Hardcoded, không config được |

## Root Cause

Regex viết khi sub-ID schema (GAP-222a/b/c) chưa tồn tại; cross-ref convention ("BLOCKS GAP-XXX") được áp dụng sau. Script chưa được điều chỉnh khi convention thay đổi.

## Proposed Fix

Single-file edit `.claude/skills/workflow/start-session/scripts/collect-state.sh`:

1. **Regex bump:** `GAP-[0-9]+` → `GAP-[0-9]+[a-z]?` để giữ sub-IDs.
2. **Skip cross-ref / prose:** chỉ lấy gap ID đầu tiên mỗi row trong table (column 2 sau cell `|`), không scan cell title chứa prose. Cách 1: parse pipe-table với `awk -F'|'` lấy `$3`. Cách 2: chỉ lấy `^| .* GAP-...` đầu mỗi line.
3. **Order preservation:** bỏ `sort -u`; dedupe bằng `awk '!seen[$0]++'` để giữ thứ tự bảng.
4. **head cap stays:** giữ `head -6` (or expose env var `MAX_BLOCKERS`).

Pseudocode:
```bash
BLOCKERS="$(awk '/GA Blockers remaining/,/Priority rule|Epics fully closed/' "$ROADMAP" 2>/dev/null \
  | awk -F'|' 'NF>=4 {print $3}' \
  | grep -oE 'GAP-[0-9]+[a-z]?' \
  | awk '!seen[$0]++' \
  | head -6 | tr '\n' ';')"
```

## Acceptance Criteria

- [ ] `./.claude/skills/workflow/start-session/scripts/collect-state.sh | grep "Gaps blocker"` trả về exact 6 IDs trong bảng GA Blockers, theo thứ tự bảng
- [ ] Sub-ID `GAP-222a` không bị collapse thành `GAP-222`
- [ ] Cross-ref `BLOCKS GAP-006` không xuất hiện trong output (GAP-006 không phải blocker row)
- [ ] Test: thêm 1 gap mới `GAP-999z` vào bảng → collector show full ID không truncate
- [ ] No regression: `MCP servers`, `Wave hiện tại`, `Recent merges` blocks không bị ảnh hưởng

## Related

- Sibling fixes: GAP-206 (wave parsing accuracy, PR #468), GAP-207 (Vietnamese output, PR #470)
- Cross-rule: `.claude/rules/meta-gap-priority.md` — collector lệch khiến meta-P0 (GAP-223) bị che, trái priority rule
- Discovered during: ROADMAP cleanup option 3 (GAP-046 row removed Wave 6 closure)

## Log

- **2026-04-26:** Filed during /start-session option-3 ROADMAP cleanup. Verification scrape sau khi remove GAP-046 row exposed regex limitation. P3 vì cosmetic/output-only — không block delivery, nhưng ảnh hưởng accuracy mọi session start.
