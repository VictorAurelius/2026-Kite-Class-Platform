# GAP-221: GitNexus Pilot — Evaluate Code-Intelligence MCP for Multi-Module Audits

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 Meta (force-multiplier candidate; pilot before adoption)
**Domain:** Tooling / Skills / Workflow
**Found:** 2026-04-25 (session B' research after user note in `documents/action-2.md`: "tìm hiểu về gitnexus")
**Affects:** Audit skills that grep across multi-module repo (KiteHub 6 services + KiteClass core/gateway/frontend) — particularly `quality/business-logic-audit`, `quality/api-contract-audit`, design-pattern audit (GAP-046 follow-up)

## Problem

Current audit skills repeatedly grep the same repo to answer architectural questions (call graph, dependency direction, blast radius). Examples:
- `quality/api-contract-audit` greps controllers + DTOs across 9 services to verify endpoint↔doc sync
- `quality/business-logic-audit` greps `rules.md` keys against `application.yml` + entity validation across services
- Design-pattern audit (GAP-046 remaining work) needs cross-cutting view of which services use Outbox / State machines / Adapters

Token cost per audit run is significant (multiple Grep calls returning hundreds of matches). And answers like "if I change `BrandingService.getBranding()`, what's the blast radius across the repo?" require manual chained queries.

GitNexus (https://github.com/abhigyanpatwari/GitNexus, 28.6k stars 2026-04) addresses this: client-side knowledge-graph engine using Tree-sitter AST → exposes 7 MCP tools to Claude Code / Cursor. Pre-computed graph means 1 MCP call instead of 10+ greps.

## Root Cause

Audit skills evolved organically from grep-first patterns. Memory `feedback_audit_grep_scope.md` already documents one false-positive incident (GAP-107 missed `-core` submodule). MCP-first rule (`.claude/rules/mcp-first-with-fallback.md`) anticipates code-intelligence MCPs but none currently in stack.

## Proposed Pilot (mirror RTK pattern from PR #531)

**Single-developer, single-day, opt-in. Adopt-team-wide criteria gate any expansion.**

### Phase 1: Scaffolding (≤2 hours)
1. Add `documents/05-guides/gitnexus-pilot/` with `README.md` + `measurement-protocol.md`
2. Add `scripts/gitnexus-pilot/{install,uninstall,check}.sh` mirroring RTK pilot script structure
3. Wire GitNexus MCP server (config example in pilot README; do NOT add to `.mcp.json` until validated)

### Phase 2: Measurement (1 day)
Run 3 representative audits **with vs without** GitNexus MCP:
- `quality/api-contract-audit` on kitehub-subscription
- `quality/business-logic-audit` on kiteclass-core/branding
- Design-pattern hotspot scan (TrialToPaidService blast radius)

Capture for each:
- Tool calls count (MCP queries vs Grep/Glob calls)
- Token consumption (input + output)
- Answer accuracy vs hand-verified baseline
- Wall-clock time

### Phase 3: Adopt-team-wide gating
Mirror RTK criteria:
- ≥30% reduction in Grep/Glob calls per audit
- Zero answer-accuracy regression vs hand-verified
- Java + TypeScript Tree-sitter coverage verified (Kite uses both heavily)
- MCP server stable for 1 day (no crashes, no stale-graph errors)
- `mcp-first-with-fallback.md` updated to add GitNexus row to §2 selection matrix

If criteria miss → uninstall, document why, file follow-up gap.

## Acceptance Criteria

- [ ] Pilot scaffolding under `documents/05-guides/gitnexus-pilot/` + `scripts/gitnexus-pilot/`
- [ ] Measurement protocol matches RTK pilot rigor (`documents/05-guides/rtk-pilot/measurement-protocol.md` template)
- [ ] 3 audit runs completed with before/after telemetry
- [ ] Decision recorded: ADOPT / REJECT / DEFER with evidence
- [ ] If ADOPT → `mcp-first-with-fallback.md` §2 + §4 updated; gap closed
- [ ] If REJECT/DEFER → uninstall.sh runs clean; rationale logged in gap

## Risk / Tradeoffs

- **Tree-sitter coverage:** Java is well-supported; need to verify Spring annotations + Lombok-generated code are parsed correctly. TypeScript with Next.js + RSC may have edge cases.
- **MCP server overhead:** another local server to maintain; offset by reduced grep noise.
- **Stale graph:** Tree-sitter index needs refresh on git operations; behavior on uncommitted changes unclear (test in pilot).
- **Solo-dev cost:** 1 dev-day pilot is non-trivial; defer if Wave 6 (GAP-046) is higher priority.

## Related

- Memory: `reference_gitnexus.md` — capability summary + repo link
- Rule: `.claude/rules/mcp-first-with-fallback.md` — current MCP selection matrix (would add GitNexus row if ADOPT)
- Precedent: PR #531 (RTK pilot) — structural template for opt-in pilot pattern
- Memory: `feedback_audit_grep_scope.md` — example pain point (cross-module grep miss)
- Companion: GAP-046 design-pattern audit (potential consumer of GitNexus blast-radius queries)

## Log

- 2026-04-25 — Gap created after session B' research surfaced GitNexus as trending MCP code-intelligence tool. User noted "tìm hiểu về gitnexus" in `documents/action-2.md`. Pilot template lifted from RTK pilot (PR #531) for consistency.
