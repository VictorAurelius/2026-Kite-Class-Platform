---
id: GAP-697
phase: phase-1-beta
status: OPEN
priority: P1
domain: Meta
audience: dev
---

# GAP-697: Wave 102.7.6 thesis V1 final polish — jargon Ch.1 + pipeline + personas reframe + repo-jargon leaks

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (META — academic polish residual misses post Wave 102.7.5 docx grep audit)
**Domain:** Meta — thesis V1 polish final pass
**Found:** 2026-05-21 (Wave 102.7.5 closure docx grep verification)
**Affects:** thesis-v1.docx polish quality (Ch.1 + MỞ ĐẦU + KẾT LUẬN narrative + bibliography)

## Problem

Wave 102.7.5 closure (3-bucket parallel + coordinator re-bake) shipped GAP-696 8 items per scope (Bucket A Ch.2 + Bucket B pipeline Phụ lục/ABC + Bucket C figures + bìa verify). Coordinator post-merge docx grep audit phát hiện **17+ residual hits** thuộc 4 lớp lược-bỏ-miss:

1. **Project-jargon `giai đoạn beta` / `giai đoạn GA`** (Wave 102.7.4 scrub scope chỉ Ch.2+Ch.3+Ch.4, miss Ch.1 + pipeline inline strings):
   - `chapter-1-competitor-analysis.md` 5 hits (lines 14, 16, 18, 36, + 1 narrative §1.4)
   - `chapter-1-vn-law-methodology.md` 1 hit (line 16)
   - `create_thesis_v1.py` 10 inline strings (lines 1257, 1637, 1641, 1686, 1696, 1736, 1759, 1768, 1812)

2. **MỞ ĐẦU §3 Phạm vi listing anti-pattern** (same pattern Bucket A đã fix cho Ch.2 §2.3.5 trong Wave 102.7.5):
   - `create_thesis_v1.py` line 1648 `Kiến trúc hệ thống bao gồm 6 microservice backend (kitehub-admin, kitehub-branding, ...)` inline catalog listing

3. **Ch.1 Nhóm 1-5 personas pattern** (user-confirmed defer Wave 102.7.6 sau Wave 102.7.5 scope-disjoint constraint giữ Ch.1 untouched):
   - `chapter-1-competitor-analysis.md` lines 109-117 — 5 `*Nhóm N — Persona:*` separators
   - Different semantic từ Ch.2 capability groups (italic persona vs bold capability) nhưng same pattern violates văn-viết academic tone

4. **Repo jargon `Wave 103+` leak + Anthropic Claude bibliography**:
   - `create_thesis_v1.py` line 1468 `(Đang được bổ sung khi thêm hình minh hoạ — Wave 103+)` Mục lục/Danh mục hình section
   - `create_thesis_v1.py` bibliography `[15] Anthropic, "Claude API Documentation"` — user direction drop entirely per action-2.md §4 line 41 "tuyệt đối không claude" inside item

## Root Cause

Wave 102.7.4 jargon scrub scope explicitly limited Ch.2+Ch.3+Ch.4 only (3-bucket plan). MỞ ĐẦU + KẾT LUẬN + Ch.1 các phần khác không nằm scope. Wave 102.7.5 GAP-696 scope cũng narrow (Ch.2 only Bucket A + pipeline Phụ lục/ABC Bucket B + figures + bìa Bucket C). Comprehensive docx grep audit post Wave 102.7.5 closure mới expose 17+ residual hits previously uncovered.

User direction 2026-05-21 post Wave 102.7.5 closure docx review: "reframe Ch.1 personas in Wave 102.7.6, các phần yêu cầu lược bỏ vẫn chưa lược bỏ" — explicit defer + scope expansion.

## Proposed Fix

Wave 102.7.6 = 2 buckets parallel (file-disjoint mandate giới hạn từ 3 buckets xuống 2 do create_thesis_v1.py overlap):

### Bucket A — Ch.1 MD files scrub (jargon + personas)

**Files:** `documents/08-thesis/chapter-1-competitor-analysis.md` + `documents/08-thesis/chapter-1-vn-law-methodology.md`

- **A1 — Jargon scrub Ch.1** (6 hits): `giai đoạn beta` → `giai đoạn thử nghiệm` (Wave 102.7.4 pattern); `giai đoạn paid beta` → `giai đoạn thanh toán thử nghiệm`; `production launch` → `vận hành chính thức` (verify context-by-context)
- **A2 — Ch.1 personas reframe** (5 hits competitor-analysis.md lines 109-117): drop `Nhóm N — ` prefix, keep persona name + context. Pattern: `*Nhóm 1 — Chủ trung tâm (Owner):* ...` → `*Chủ trung tâm (Owner):* ...` (preserve italic persona styling, drop counter prefix)

### Bucket B — Pipeline scrub (jargon + listing + Wave 103+ + Claude bibliography)

**Files:** `documents/08-thesis/create_thesis_v1.py`

- **B1 — Jargon scrub pipeline inline strings** (10 hits lines 1257/1637/1641/1686/1696/1736/1759/1768/1812): same patterns as A1. Plus `giai đoạn GA (General Availability)` line 1768 → `giai đoạn vận hành chính thức`
- **B2 — MỞ ĐẦU §3 listing rewrite** (line 1648): apply same 3-layer narrative grouping template từ Bucket A Wave 102.7.5 cho Ch.2 §2.3.5. Drop inline `6 microservice backend (kitehub-admin, ...)` enumeration.
- **B3 — Wave 103+ repo jargon scrub** (line 1468): `(Đang được bổ sung khi thêm hình minh hoạ — Wave 103+)` → `(Đang được bổ sung khi thêm hình minh hoạ)` hoặc reframe neutral language
- **B4 — Anthropic Claude bibliography drop** (`[15] Anthropic, "Claude API Documentation"` entry): drop entirely + grep `[15]` cross-references trong narrative + renumber bibliography subsequent refs ([16] → [15], [17] → [16], ... [44] → [43]). Total bibliography 39 entries → 38 entries.

### Items NOT in scope Wave 102.7.6
- F9 manual Word fix (Wave 102.6 Bucket D defensive fallback)
- Persona văn viết refinement (defer Wave 102.7.7+)
- Figure attribution Ch.2+Ch.3 (defer Wave 102.7.7+)
- Acronym defined at first use sweep
- F-B2-02 leftover audit finding

## Acceptance Criteria

- [ ] Bucket A1: Ch.1 MD `giai đoạn beta` / `giai đoạn paid beta` / `production launch` 6 hits → scrubbed per Wave 102.7.4 patterns
- [ ] Bucket A2: Ch.1 personas 5 hits — `Nhóm N — ` prefix dropped (persona name + context preserved)
- [ ] Bucket B1: Pipeline 10 inline jargon hits → scrubbed
- [ ] Bucket B2: MỞ ĐẦU §3 listing rewritten với 3-layer narrative grouping
- [ ] Bucket B3: `Wave 103+` repo jargon → neutral language
- [ ] Bucket B4: Bibliography [15] Anthropic Claude DROP + renumber [16-44] → [15-43] + in-body `[15]` cross-refs updated
- [ ] thesis-v1.docx re-bake clean post 2 buckets merged + coordinator verify (grep `giai đoạn beta` = 0, grep `Nhóm [0-9] —` = 0, grep `Wave 10[3-9]\+` = 0, grep `Claude API` = 0)

## Related

- Wave 102.7.5 closure PR #1685 (this wave's predecessor) — docx grep audit phát hiện 17+ residual misses
- Wave 102.7.4 PR #1677 — jargon scrub Ch.2+Ch.3+Ch.4 scope (32/32 hits) — scope mandate limited
- GAP-696 closed: `documents/04-quality/gaps/phase-1-beta/closed/GAP-696-wave-102-7-5-deferred-items.md`
- action-2.md §4 line 41 "tuyệt đối không claude" — original inside-out item that B4 closes
- thesis-content-standard.md S3 v1.1.0 — narrative grouping pattern applied B2 + Wave 102.7.5 Bucket A precedent

## Log

- **2026-05-21 (filed):** Filed Wave 102.7.5 closure docx grep audit post-merge. 17+ residual hits across 4 lớp lược-bỏ-miss. User direction 2026-05-21 post-audit: "File Wave 102.7.6 plan ngay (3-bucket parallel)" + "Drop Anthropic Claude bibliography entirely". File-disjoint constraint giảm 3-bucket xuống 2 buckets (Ch.1 MD + pipeline). Estimated wall-clock ~30-40 min agent parallel longest-bucket B ~40 min (10 inline + bibliography renumber + listing rewrite).
