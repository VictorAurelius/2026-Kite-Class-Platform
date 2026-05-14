---
name: business-logic-audit
description: "Dùng khi user nói 'business audit', 'logic check', 'kiểm tra business logic', 'code đúng rules chưa', hoặc trước GA release. Verify code implement đúng rules.md + use-cases.md. 5 categories /100."
user-invocable: true
---

# /business-logic-audit — Verify Code ↔ Business Rules

Score /100. Walk through every domain in `documents/01-business/`, verify code implements rules correctly.

## Process

### 1. Collect Domains

```bash
ls documents/01-business/kiteclass/ documents/01-business/kitehub/ | grep -v README
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface code-rules drift BEFORE production violates documented business policy (wrong pricing, wrong retention, wrong consent flow). A `/100` score is less actionable than the same score + list of unimplemented BR-xxx + config-key drifts with file:line evidence.** Per `.claude/rules/audit-skill-rubric-business-logic-audit.md` §4 (mirror of Wave 71c security-audit primacy pattern).

Rules for every audit run:
1. Enumerate ALL §3 sub-checks per category. NEVER skip.
2. Each sub-check returns: PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`. No partial credit.
3. Final output starts with **bug list** (every BR/config FAIL with `rules.md:line` + `*.java:line` evidence) BEFORE the score.
4. Score is descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. Cat 5 Stakeholder rules require human review — Claude flags FAIL, human decides closure.

### 3. Per-Domain: 5 Categories with per-check rubric

Per Wave 72b Bucket E (GAP-523 closure), every category binds to per-check pass/fail rule. For EACH domain folder, read `rules.md`, `use-cases.md`, `api-contract.md`, then verify in code:

| # | Category (20pts) | Per-check rubric file |
|---|-----------------|-----------------------|
| 1 | **Rule Coverage** | **`.claude/rules/audit-skill-rubric-business-logic-audit.md` §2.1 (6 sub-checks)** |
| 2 | **Config Accuracy** | **`.claude/rules/audit-skill-rubric-business-logic-audit.md` §2.2 (5 sub-checks)** |
| 3 | **Edge Case Tests** | **`.claude/rules/audit-skill-rubric-business-logic-audit.md` §2.3 (5 sub-checks)** |
| 4 | **Cross-Domain Consistency** | **`.claude/rules/audit-skill-rubric-business-logic-audit.md` §2.4 (5 sub-checks)** |
| 5 | **Stakeholder Alignment** | **`.claude/rules/audit-skill-rubric-business-logic-audit.md` §2.5 (5 sub-checks)** |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`.
3. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
4. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL.
5. Each FAIL surfaces in bug list per §2 primacy.

Legacy scoring narrative: `reference/scoring-guide.md` retained for backward-compat only.

### 4. Output

Save to `documents/04-quality/audits/business/business-logic-audit-[date].md`

### 5. Scripts

```bash
# Existing — checks 3-layer structure exists
scripts/verify-business-docs.sh

# Manual — verify each BR-xxx has code path
# Grep for config keys in application.yml
```

## Grep Scope — CRITICAL

**NEVER** scope greps to only `kitehub/ kiteclass/` — these are multi-module Maven projects, and classes/config may live in submodules (`kiteclass-core/`, `kitehub-branding/`, etc.). Narrow scope = silent false-positive ("class doesn't exist" when it does). GAP-107 false positive root cause.

**Safe patterns** (use one):

```bash
# Option 1 (broad, preferred) — project root, filter by extension
grep -rnE "ClassName|BR-ID" --include="*.java"
grep -rn "config.key.name" --include="*.yml"

# Option 2 (explicit submodules) — glob all module src dirs
grep -rn "ClassName" kiteclass/*/src/ kitehub/*/src/ --include="*.java"
grep -rn "config.key" kiteclass/*/src/main/resources/ kitehub/*/src/main/resources/ --include="*.yml"
```

**Sanity check before filing "X doesn't exist" gap:**

```bash
# If narrow grep returns 0 hits, re-run with broad scope before claiming absence
grep -rn "SuspectedMissingClass" --include="*.java" | head -5
```

Ref: `.claude/rules/audit-to-gap-pipeline.md`, memory `feedback_audit_grep_scope.md`, GAP-149.

## Context Management

Audit này có thể tốn 30-50K tokens nếu không kiểm soát. Tuân thủ:

1. **Output limiting** — LUÔN pipe grep results qua `| head -N`:
   - BR-xxx grep: `| head -30` (chỉ cần biết có/không, không cần xem hết)
   - Config key grep: `| head -20`
   - Test file count: dùng `wc -l` thay vì list full
2. **Per-domain staging** — Nếu >5 domains, score 2 domains đầy đủ rồi apply pattern cho còn lại. Chỉ individually score domains có cấu trúc ĐẶC BIỆT.
3. **Subagent delegation** — Nếu >8 domains hoặc codebase >500 Java files:
   - Agent 1: KiteClass domains
   - Agent 2: KiteHub domains
   - Parent: aggregate scores
4. **Skip known-good** — Domains không thay đổi từ audit trước → carry forward score, chỉ verify version match

## Gotchas

- Config keys are in `application.yml` AND `application-test.yml` — check both
- Some BR-xxx implemented in gateway (rate-limit rules) not core — search all modules
- Wave 4 added 6 new domains — don't miss `security-foundation/`, `content-moderation/`, etc.
- Category 5 (Stakeholder) always requires human review — Claude flags, human decides
- Grep output cho large codebase có thể 1000+ lines — LUÔN giới hạn
- **Multi-module scope trap** — `grep -r "X" kitehub/ kiteclass/` may silently miss `kiteclass/kiteclass-core/` hits (GAP-107 false-positive). Follow "Grep Scope" section above.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category with examples
- `data/eval-fixtures/` — 3 synthetic scenarios for self-test (GAP-253)

## Eval Fixtures

3 synthetic fixtures live under `data/eval-fixtures/` to keep this skill
honest when its body is edited (per Anthropic 2026 eval-first guidance —
GAP-253 pilot). Each fixture has a `# Expected: PASS|FAIL` header and a
`Which check fires` annotation.

- `good.md` — synthetic `attendance` domain where every BR-* maps to code +
  config aligns; expected output `100/100 Grade A`.
- `bad-rule-not-implemented.md` — `BR-ATT-005` declared in rules.md but no
  `@PreAuthorize` / service guard exists; Category 1 must report `-4`.
- `edge-config-key-renamed.md` — config key `late-threshold-minutes`
  renamed to `late-grace-minutes` in code, rules.md not updated; Category 2
  must catch the silent drift.

**Run:** open the fixture and walk through the audit process steps mentally
against the synthetic content; the `Expected audit-report excerpt` section
in each fixture is the regression contract. When extending this skill,
re-walk all 3 fixtures and confirm the expected outputs still hold.
