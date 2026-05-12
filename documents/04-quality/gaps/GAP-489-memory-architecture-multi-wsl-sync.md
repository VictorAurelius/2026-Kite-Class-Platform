# GAP-489: Memory architecture — multi-WSL sync + relationship to rules

**Status:** 🔵 OPEN (design discussion)
**Priority:** 🟡 P2 (productivity / consistency; not blocking)
**Domain:** Meta / Governance / Infrastructure
**Found:** 2026-05-12 (user-flagged Wave 64 close)
**Affects:** Claude Code memory system, multi-machine workflow, rule-vs-memory boundary

## Problem

User raised 3 architectural questions about memory system:

1. **Tại sao memory local (not in repo)?** — Lost on machine change; can't sync to other WSL instances/laptops
2. **Tại sao không sync lên git để thao tác nhiều WSL?** — Solo-dev mới WSL switch frequently; no team-conflict concern with sync
3. **Tại sao không tạo memory thành rules?** — Some memory IS pattern that becomes rule (e.g. `feedback_pre_mutation_state_check.md` paired with `pre-mutation-state-check.md` rule)

Current state:
- Memory at `~/.claude/projects/-home-nguyenvankiet-projects-2026-Kite-Class-Platform/memory/` (outside repo, gitignored by default)
- ~50 memory files, growing
- 4 types: user / feedback / project / reference
- Auto-loaded at session start
- 7 orphan files NOT in MEMORY.md index (found Wave 64 audit)
- Pattern: ~10 feedback memories already paired with formal rules (manual cross-refs)

## Trade-off analysis

| Option | Pro | Con |
|--------|-----|-----|
| **Status quo (local)** | Lightweight, no review overhead | Lost on machine change; multi-WSL impossible |
| **Sync to repo** `documents/memory/` | Multi-WSL, git history, backup | Pollutes project repo with personal notes |
| **Promote all → rules** | Single source, formal governance | Heavy overhead for transient/personal notes |
| **Hybrid: project memory in repo, personal local** | Balance | Need clear rule for which goes where |
| **Symlink local→repo subdir** | Quick win | Same as "sync to repo" but via symlink |
| **Separate git repo `claude-memory-<user>`** | Sync without polluting project | Extra repo management |

## Proposed Fix (design phase)

### Option A — Hybrid (recommended for solo-dev multi-WSL)

1. **Sync memory into repo** at `documents/_memory/` (gitignore `.local` subdirectory for personal-only)
   - `documents/_memory/*.md` — project-relevant memory (sync to repo, multi-WSL works)
   - `documents/_memory/.local/*.md` — personal/transient (gitignored)
2. **Promotion pipeline** memory → rule:
   - When feedback memory recurs across multiple incidents → promote to formal rule via `rule-change-process.md`
   - Memory entry retained as cross-link pointer to rule
3. **Index sync** — Bucket A of Wave 65 already covers MEMORY.md index orphans

### Option B — Convert all memory → rules (heavyweight, not recommended)

- Promote every memory file to `.claude/rules/*.md`
- Lose lightweight capture path
- Every new observation requires rule-change-process

### Option C — Status quo + accept multi-WSL limitation

- Manual sync via `rsync` or `git` of `~/.claude/projects/...` dir between machines
- Document in `documents/05-guides/dev/multi-wsl-setup.md`
- Lowest effort, highest user-friction

## Acceptance Criteria

- [ ] Design decision (A/B/C/other) documented in ADR or this gap
- [ ] If Option A: directory structure shipped + .gitignore updated + 50 existing memory files migrated
- [ ] If Option C: multi-WSL sync runbook shipped
- [ ] Boundary rule: when does memory become rule? (codify in `rule-change-process.md`)
- [ ] Multi-WSL works (user can sync session between WSL distros)

## Out-of-scope

- Sync across non-WSL machines (Mac, Windows native) — Wave 66+ if needed
- Real-time sync (file watcher) — Wave 67+
- Memory pruning policy (old memory archival) — separate gap

## Related

- **Surfaced by:** User 2026-05-12 Wave 64 close
- **Reference:**
  - `~/.claude/RTK.md` user global instructions
  - `.claude/CLAUDE.md` "auto memory" section
  - 10+ feedback memories already paired with formal rules (e.g. `feedback_pre_mutation_state_check.md` ↔ `pre-mutation-state-check.md`)
- **Related gaps:** GAP-485 (CSV-canonical meta) + GAP-486 (post-merge sync) — same theme of meta-governance hygiene

## Log

- **2026-05-12:** Filed at user request during Wave 64 close. 3 design questions surfaced. Design discussion phase before implementation.
