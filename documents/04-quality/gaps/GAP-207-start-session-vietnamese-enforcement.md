# GAP-207: `/start-session` output vi phạm CLAUDE.md §CRITICAL Communication Language

**Status:** 🟢 DONE 2026-04-24
**Priority:** 🟠 P1 Meta (skill output không tuân CLAUDE.md — force multiplier per `meta-gap-priority.md` §5.1)
**Domain:** Workflow / Meta (skill `/start-session`)
**Detected:** 2026-04-24 (session 2 sau khi ship GAP-206; user flagged "nó không giao tiếp như claude.md yêu cầu nhỉ")
**Related PRs:** fix PR incoming
**Related Docs:**
- `.claude/skills/workflow/start-session/SKILL.md`
- `.claude/skills/workflow/start-session/scripts/collect-state.sh`
- `CLAUDE.md` §CRITICAL Communication Language
- `documents/04-quality/gaps/GAP-206-start-session-skill-accuracy.md` (predecessor — fixed data accuracy but not language)

## Current State (verified 2026-04-24)

| Piece | Path / Value | Status |
|-------|--------------|--------|
| CLAUDE.md language rule | §CRITICAL "ALWAYS communicate in Vietnamese" | ✅ Rule exists |
| `collect-state.sh` field labels | "Branch", "Repo level", "Recent merges", "Notes"... | ❌ All English |
| SKILL.md output template | English labels + English prose example | ❌ English |
| Claude's summary output (user session) | Full English despite CLAUDE.md §CRITICAL | ❌ Violates rule |
| GAP-206 (predecessor) | Fixed data accuracy, ignored language | 🟡 Partial — completed objectives but missed this |

**Sample violation (2026-04-24 user /start-session):**
```
Wave: Wave 5 — GAP-047 document generation (top meta P0; splits into 5.1 PDF+Excel, 5.2 Word, …)
Branch: main (clean — only documents/action-2.md scratchpad modified)
Open PRs: 0
CI: main GREEN · 0 critical/high CVEs · 0 stale branches · 0 audit P0 → repo level GREEN
...
Recommended next:
  - Resume Wave 5 / GAP-047 Sub-PR 5.1 (PDF + Excel generation skill) — top-of-stack meta P0
  - Or /repo-status for fuller health drill-down
```

CLAUDE.md line 7:
> **ALWAYS communicate in Vietnamese (tiếng Việt)**
> - All responses, explanations, and documentation should be in Vietnamese
> - Code comments can be in English (standard practice)
> - Commit messages should be in English (git convention)

Output field labels ARE "responses" (user-facing). Should be Vietnamese.

## Problem

Skill output bằng English vi phạm CLAUDE.md §CRITICAL. User phải context-switch mental model khi đọc mixed-language session kickoff. Ngoài ra:

- Skill mô tả trong SKILL.md nói "giao tiếp tiếng Việt" (rule copied correctly) nhưng output template example lại English → mixed signal cho future skill readers
- `collect-state.sh` raw output cũng English → Claude khi render summary dễ rơi vào English matching
- GAP-206 fixed accuracy bugs (wave/blockers/recent) but didn't notice language layer

## Context

GAP-206 session 2026-04-24 sửa 4 bugs về accuracy (wave detection, blockers, recent context, repo-status integration). Script test output post-fix đã English. Lần session tiếp theo (user `/clear` + `/start-session` ở session 2), Claude render English summary → user flagged.

Gap đã tồn tại từ đầu khi skill được tạo (GAP-193) nhưng chỉ surface sau khi GAP-206 fix data — accuracy bug masked language bug trước đây.

## Evidence

**collect-state.sh English labels:**
```bash
$ grep -E '^Branch:|^Repo level:|^Open PRs:|^Current wave:|^Blocker gaps:' \
    .claude/skills/workflow/start-session/scripts/collect-state.sh
Branch:          $BRANCH ($BRANCH_STATE)
Repo level:      $RS_LEVEL
Open PRs:        $OPEN_PRS  ${TOP_PRS:+— $TOP_PRS}
Current wave:    ${CURRENT_WAVE:-<none — check ROADMAP.md manually>}
Blocker gaps:    ${BLOCKERS:-<none>}
```

**SKILL.md output example:**
```markdown
## Session Context (2026-04-24 04:45)
**Wave:** Wave 5 — GAP-047 document generation (per ROADMAP §Next recommended)
**Branch:** main (clean, scratchpad: action-2.md) / worktrees: 0
...
```
All English labels + mixed-language content. `Wave:`, `Branch:`, `Blocker gaps:`, `Recommended next:` phải VN.

## Proposed Fix

### Stage A — translate field labels (this PR)

1. **collect-state.sh output** — translate section labels to VN:
```
Branch         → Nhánh
Repo level     → Mức repo
CI main        → CI main (unchanged — CI is technical term)
CVE            → CVE (unchanged — abbreviation)
Stale brs      → Branches cũ
Audit P0       → Audit P0 (unchanged — abbreviation)
Open PRs       → PRs đang mở
Current wave   → Wave hiện tại
Blocker gaps   → Gaps blocker
Active locks   → Session locks active
Recent merges  → Merges gần đây
Notes          → Ghi chú
```

2. **SKILL.md output template** — update example to use VN labels + rewrite "Recommended next" prose to VN:
```markdown
## Session Context (2026-04-24 04:45)
**Wave:** Wave 5 — GAP-047 document generation (theo ROADMAP §Next recommended)
**Nhánh:** main (clean, scratchpad: action-2.md) / worktrees: 0
**Mức repo:** GREEN (CI green, 0 CVE, 0 branches cũ, 0 audit P0)
**PRs đang mở:** 0
**Gaps blocker (top 6):** GAP-047, GAP-046, GAP-016, GAP-011, GAP-014, GAP-005
**Merges gần đây (3d):** #468 ..., #467 ..., ...
**Context health:** fresh session
**Đề xuất tiếp theo:** bắt đầu Wave 5 sub-PR 5.1 (PDF+Excel doc generation)
```

3. **SKILL.md Rules section** — thêm rule rõ ràng "OUTPUT BẮT BUỘC tiếng Việt per CLAUDE.md §CRITICAL":
```markdown
## Rules
- **TUYỆT ĐỐI giao tiếp bằng tiếng Việt** (per CLAUDE.md §CRITICAL). Field
  labels, prose, recommendations — tất cả tiếng Việt. Chỉ giữ English cho:
  technical terms (CI, CVE, PR, gap, wave, branch — đã là loanwords),
  file paths, command output, code.
- LUÔN chạy script trước — không tự suy diễn status
- Nếu script fail → báo rõ, không đoán
```

### Stage B — follow-up (future PRs)

- Audit TẤT CẢ workflow skills (`/repo-status`, `/continue`, `/check-pr`, ...) để ensure output VN compliance
- Có thể extract reusable VN-label function vào shared script

## Acceptance Criteria

- [x] `collect-state.sh` field labels dịch sang VN (CI/CVE/PR/gap/branch giữ)
- [x] SKILL.md output template cập nhật example VN
- [x] SKILL.md §Rules thêm VN enforcement
- [x] Test: chạy script → thấy labels VN
- [x] Log trong gap file: session sau `/start-session` sẽ output VN

## Related

- Predecessor: GAP-206 (fixed data accuracy, missed language layer)
- `meta-gap-priority.md` §5.1 — skill blindspots = force-multiplier debt
- Skill conventions: `.claude/rules/skill-conventions.md` (should reference CLAUDE.md language rule)

## Log

- **2026-04-24** — Session 2 `/start-session` test post-GAP-206. User flagged: "không giao tiếp như claude.md yêu cầu nhỉ (bằng tiếng việt)". Script + SKILL.md output template English despite CLAUDE.md §CRITICAL rule. Bug pre-existing since skill creation (GAP-193) — masked by GAP-206 accuracy bugs previously. Fix in same session 2026-04-24.
- **2026-04-29 (status sync)** — Truth-up: PR #470 merged 2026-04-24 (initial VN translation of collect-state.sh labels + SKILL.md template + §Rules); follow-up PR #526 merged 2026-04-25 (context-template.md labels to Vietnamese). Status header drifted from reality. Per memory feedback_post_merge_doc_sync.md, gap closure doc-sync should happen in same PR as the closing merge — backfilled here under Wave Meta-Gov 2 Agent C housekeeping. All 5 ACs verified shipped via live script output (Nhánh / Mức repo / Wave hiện tại / Gaps blocker / Merges gần đây visible).
