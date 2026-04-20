# GAP-XXX: [Title]

**Status:** 🔵 OPEN | 🟡 PARTIAL | 🟠 IN_PROGRESS | 🟢 DONE | ⚫ WONTFIX
**Priority:** 🔴 P0 | 🟠 P1 | 🟡 P2 | 🟢 P3
**Domain:** [Architecture / Backend / Frontend / DevOps / AI / ...]
**Detected:** YYYY-MM-DD
**Related PRs:** [#N, #N, ...]
**Related Docs:** [paths]

## Current State (verified YYYY-MM-DD)

> **BẮT BUỘC** per `.claude/rules/audit-to-gap-pipeline.md` Step 2.5. Before filing a gap, grep the actual code paths + infra + docs that the gap would touch. Document what already exists here. If nothing exists, write "Nothing found in <paths grep'd>." If partial implementation exists, list files + line counts + symbols as evidence.

| Piece | File / Path | Status |
|-------|-------------|--------|
| [Piece name] | [Path:LOC or symbol] | ✅ shipped / 🟡 partial / ❌ missing |

**Grep commands run:**
```bash
# Example — adapt to gap scope
grep -rl "<keyword>" <service>/src --include="*.tsx"
find documents/05-guides -iname "*<topic>*"
```

## Problem

Describe the gap — what's missing, wrong, or inconsistent. For 🟡 PARTIAL status, state exactly the delta between Current State and target.

## Context

Background: how it was discovered, why it matters, impact.

## Evidence

Links to code/docs/screenshots proving the gap exists.

## Proposed Fix

Concrete plan to close the gap. Narrow to the delta identified in Current State — do NOT propose re-building pieces already shipped.

## Acceptance Criteria

- [ ] Criterion 1
- [ ] Criterion 2

## Related

- Cross-references to existing gaps (done or open) that overlap in scope
- Rules / skills invoked
- PR / audit report origins

## Log

- YYYY-MM-DD — Initial write-up (state-check completed)
