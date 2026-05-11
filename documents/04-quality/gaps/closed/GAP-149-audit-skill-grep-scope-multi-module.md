# GAP-149: Audit Skill Grep Scope Too Narrow for Multi-Module Projects

**Status:** 🟢 DONE
**Priority:** 🔴 P0 (meta — skills/workflow)
**Domain:** Meta / Audit Skills / Quality Governance
**Found:** 2026-04-20 (triggered by GAP-107 retraction 2026-04-20, formalized in Part C Sprint 0)
**Resolved:** 2026-04-20 (this PR)
**Affects:** All 5 audit skills (business-logic, performance, ops-readiness, security, api-contract)

## Problem

Audit skills documented grep scope as `grep -r "pattern" kitehub/ kiteclass/` — narrow scope that silently missed hits in multi-module submodules (`kiteclass-core/`, `kiteclass-gateway/`, `kitehub-branding/`, etc.). Caused at least 1 confirmed false positive:

- **GAP-107** (filed 2026-04-19): claimed `ResilientAIClient`/`MockAIClient`/`OllamaAIClient` don't exist because `grep -r "ResilientAIClient" kitehub/ kiteclass/` returned 0 hits. Re-audit 2026-04-20 (PR #379) found all 3 classes at `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/ai/client/`. Gap retracted as false positive.

Root-cause: narrow grep scope + optimistic "no hits = class doesn't exist" inference. Similar pattern risks:
- Future audits misreading submodule absence as feature absence
- Rules.md ↔ code drift claims unfounded
- Trust in audit reports erodes

Meta impact: affects every subsequent audit that uses these skills (force multiplier).

## Root Cause

Audit skills evolved when repo had fewer submodules. Scope conventions (`kitehub/ kiteclass/`) matched the layout at writing time but drifted as Maven/monorepo structure expanded. No governance loop to re-verify grep scope when new `{proj}-core` / `{proj}-*` modules added.

## Fix Applied (this PR)

### 1. `business-logic-audit/SKILL.md`
- Added "Grep Scope — CRITICAL" section with safe patterns (broad + explicit submodule glob)
- Added false-positive guard: sanity re-grep before filing "X doesn't exist" gap
- Updated Gotchas to reference multi-module trap

### 2. `business-logic-audit/reference/scoring-guide.md`
- Category 1 (Rule Coverage): inline ✅/❌ grep examples + false-positive guard note
- Category 2 (Config Accuracy): broadened scope example

### 3. `performance-audit/SKILL.md`
- Redis caching grep: `| kiteclass/ kitehub/` → broad `--include="*.java"` from root
- Resource config grep: `| kiteclass/ kitehub/` → broad `--include="*.yml"` from root
- Added `| grep -v target` to exclude compiled duplicates
- Gotchas: multi-module scope note

### 4. `ops-readiness-audit/SKILL.md`
- Monitoring endpoints grep: broadened
- Logging config grep: broadened
- Gotchas: multi-module scope note

### 5. `security-audit/SKILL.md`
- Secret patterns grep: broadened scope
- Hardcoded IPs grep: explicit `kiteclass/*/src/main/` + `kitehub/*/src/main/` glob
- Gotchas: multi-module scope note

### 6. `api-contract-audit/SKILL.md`
- Already uses `kitehub/*/src/main/` glob (decent); added Gotchas note for future service expansion

### 7. Retroactive verification (3 gaps from same audit batch)
- GAP-106 (branding.routing config missing): VALID — broad grep confirms 0 hits, no false positive
- GAP-108 (payment/invoice config hardcoded): VALID — broad grep confirms 12 keys absent, `LATE_FEE_RATE` still hardcoded
- GAP-110 (ollama model inconsistency): VALID — broad grep confirms `text-model` vs `default-model` split real

All 3 gaps survive broader-scope verification — no retroactive retraction needed.

## Acceptance Criteria

- [x] `business-logic-audit/SKILL.md` has "Grep Scope" section documenting safe patterns
- [x] `business-logic-audit/reference/scoring-guide.md` Category 1 + 2 updated with safe grep examples
- [x] `performance-audit/SKILL.md` bash examples use broad or explicit-glob scope
- [x] `ops-readiness-audit/SKILL.md` bash examples use broad scope
- [x] `security-audit/SKILL.md` bash examples use broad or explicit-glob scope
- [x] `api-contract-audit/SKILL.md` has multi-module scope note in Gotchas
- [x] 3 related gaps (GAP-106/108/110) retroactively verified — no false positives among them
- [x] ROADMAP.md updated with GAP-149 entry
- [x] Gap closed (this file status DONE)

## Related

- Retraction source: `documents/04-quality/gaps/closed/GAP-107-ai-provider-rules-reference-nonexistent-classes.md` §Resolution
- Rule: `.claude/rules/audit-to-gap-pipeline.md` §2 Step 2 Duplicate Check (grep scope affects this step)
- Memory: `feedback_audit_grep_scope.md` (source feedback)
- Memory: `feedback_audit_calibration.md` (related — audit honesty)
- Master plan Part C Sprint 0: `documents/03-planning/plans/prompt-meta-gaps-first-2026-04-20.md` §3.1
- Meta priority rule: `.claude/rules/meta-gap-priority.md` — this gap force-multiplies every future audit
