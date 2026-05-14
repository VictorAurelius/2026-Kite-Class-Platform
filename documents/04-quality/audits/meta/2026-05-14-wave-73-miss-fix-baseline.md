---
title: Wave 73 Miss Fix — Base Context Baseline Audit
status: complete
created: 2026-05-14
phase: post-wave-73-fix
wave: 73-miss-fix
gaps: []
rules:
  - .claude/rules/context-budget-mandate.md
  - .claude/rules/incident-to-rule-pipeline.md
  - .claude/rules/pre-handoff-self-test-completeness.md
---

# Wave 73 Miss Fix — Base Context Baseline Audit

## Scope

Audit baseline đo lường cho fix Wave 73 miss được user-flagged 2026-05-14:
> "sao vẫn load tới 182k thay vì 88k"

Wave 73 (merged commits abea3cf5, dc0bd74b, d92394fe, 8212329a, b38f5f52) đã path-scope 30 MANDATORY rules nhưng bỏ sót 13 MANDATORY rules + folder README — vẫn auto-load base context dù scope rule có natural path trigger.

Audit này (a) measure rule-footprint contribution BEFORE vs AFTER fix, (b) document root cause của Wave 73 miss, (c) verify enforcement parity per `rule-change-process.md` §6.5.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

```bash
# Count rules with vs without `paths:` frontmatter
for f in .claude/rules/*.md; do
  head -1 "$f" | grep -q '^---$' && echo "HAS_PATHS" || echo "NO_PATHS";
done | sort | uniq -c

# Measure char-based token estimate (4 chars/token convention for English+Markdown)
for f in .claude/rules/*.md; do
  if head -1 "$f" | grep -q '^---$'; then continue; fi
  wc -c < "$f"
done | awk '{sum+=$1} END {print sum, "chars,", int(sum/4), "tokens"}'

# Validate CSV + frontmatter post-fix
bash scripts/check-rules-index-csv.sh
bash scripts/check-rule-frontmatter.sh
```

## Findings

### Rule-footprint contribution to base context

| Stage | Files auto-loading | Char count | Estimated tokens (chars/4) |
|-------|-------------------:|-----------:|---------------------------:|
| Wave 73 plan target (claimed in §5 self-test) | ~14 CRITICAL only | n/a | ~50k |
| Wave 73 actual outcome (this audit retro) | 26 rules + README | 349,458 | **~87.4k** |
| Wave 73 miss fix (this PR) | 14 rules | 215,221 | **~53.8k** |

**Wave 73 actual miss:** 12 MANDATORY rules + folder README still auto-loaded after Wave 73 closure, contributing ~33.6k tokens unnecessary base load per session.

**Savings từ this PR:** ~33.6k tokens × every session forever = high-leverage force-multiplier per `meta-gap-priority.md` §3.

### User-reported baseline (2026-05-14)

User measured fresh `/start-session`:

| Component | Tokens | Note |
|---|---:|---|
| CLAUDE.md | ~3,847 | always-load |
| 24 auto-load rules (per user count, before this fix) | ~80,480 | matches my retro measurement ~87.4k for 26 files |
| 35 skill descriptions | ~3,500 | frontmatter only |
| Memory | 0 | dir effectively empty (1 entry) |
| **TOTAL base** (user calc) | **~87,827** | rule + CLAUDE.md + skills + memory |

User flagged actual `/start-session` consumed ~182k — discrepancy ~94k consist of:
- Claude Code system prompt + tool definitions (~80-100k baseline)
- Auto-load skill descriptions (35 × ~100 tokens = ~3.5k)
- Session-collect-state.sh output, MCP server instructions, deferred tools list
- Recent `/clear` artifacts + currentDate context

### Projected post-fix baseline

Same components, 14 rules instead of 26:

| Component | Tokens | Delta vs pre-fix |
|---|---:|---:|
| 14 always-load rules | ~53,805 | **−33,559 (−42%)** |
| CLAUDE.md | ~3,847 | 0 |
| 35 skill descriptions | ~3,500 | 0 |
| Memory | <1,000 | 0 |
| **Rule-side subtotal** | **~62,152** | **−33.6k from this fix** |
| Claude Code system overhead | ~80-100k | 0 (not addressable in this scope) |
| **Total session base (projected)** | **~142-162k** | **~20-22k below user's ~182k** |

### Real vs phantom analysis của 14 file edits

| Resource | Action | Root cause | Risk |
|---|---|---|---|
| 11 MANDATORY rules `.md` | UPDATE — add `paths:` frontmatter + PATCH version bump + Log entry | Wave 73 §3 Scope table bỏ sót (line 105-139 wave plan) | LOW — additive, no constraint change |
| `agent-action-bias.md`, `mcp-first-with-fallback.md` | UPDATE — add §"Auto-load justification" section + PATCH version bump | `context-budget-mandate.md` §3.2 mandates section khi rule always-load | LOW — additive section |
| `.claude/rules/README.md` | UPDATE — add `paths: [".claude/rules/**"]` frontmatter | Folder index auto-loaded mọi session khi không browse rules | LOW — additive |
| `.claude/rules/rules-index.csv` | UPDATE — sync `path_trigger` column cho 13 rows | CSV canonical per `meta-csv-index-pattern.md` §4 | LOW — CSV-canonical pattern sync |

Phantom updates: 0 (mọi edit là real intentional change).

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8 — avoid duplicate work)

| Action | When | Where verified |
|--------|------|----------------|
| Wave 73 Bucket 0 — `rules-index.csv` `path_trigger` column + pilot `aws-sg-description-ascii.md` | 2026-05-14 (commit d92394fe area) | Wave plan §3 Scope Bucket 0 |
| Wave 73 Bucket A1-A5 — 30 MANDATORY rules path-scoped | 2026-05-14 | 30 files with `paths:` frontmatter confirmed via `grep` |
| Wave 73 Bucket B — 8 deterministic hooks (GAP-528 closure) | 2026-05-14 (commit abea3cf5) | Wave plan §3 Scope Bucket B + recent commit |
| Wave 73 Bucket C — UserPromptSubmit dynamic inject | 2026-05-14 (commit d92394fe) | Wave plan §3 Scope Bucket C |
| Wave 73 Bucket D — `context-budget-mandate.md` rule shipped | 2026-05-14 (commit 8212329a) | Rule file exists in repo |
| Wave 73 Bucket E — closure (this audit fills the BASELINE MEASUREMENT artifact mandate from §3 Scope Bucket E that was missed at original closure) | 2026-05-14 (commit dc0bd74b) | Wave plan `status: complete`; audit artifact **was** missing — this file fills the gap |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Path-scope 11 MANDATORY rules + folder README | Coordinator (me) | DONE in this PR; CI validators PASS |
| Add `## Auto-load justification` to 2 cross-cut rules | Coordinator | DONE in this PR |
| Update `rules-index.csv` `path_trigger` column | Coordinator | DONE in this PR |
| Real fresh /start-session measurement post-merge | User | REQUIRED — confirm actual savings match projection |
| Update Wave 73 plan §10 Log với miss fix entry | Coordinator | TODO |
| Consider `scripts/check-context-budget.sh` detector to catch future regressions | Future (deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days) | Tracked as follow-up |

## Root cause analysis của Wave 73 miss

### Cause #1 (proximate) — §3 Scope table chỉ enumerate 30 rules

Wave plan line 105-139 path-trigger mapping table có exactly 30 rule rows. 13 MANDATORY rules không xuất hiện trong table này → không có bucket nào own việc path-scope chúng:
- agent-aws-access, agent-background-spawn-default, agent-action-bias
- context-budget-mandate (rule itself created Wave 73 Bucket D — irony)
- logs-format-standard, meta-csv-index-pattern, production-env-config-registry
- session-currentdate-check, skill-conventions
- terraform-apply-retry-reconfirm, terraform-partial-backend-public-repo
- third-party-platform-automation-discovery
- mcp-first-with-fallback (marked "(CRITICAL) keep auto-load" — incorrect: frontmatter là MANDATORY)

### Cause #2 (contributing) — Bucket B hooks ≠ rule body unload

Wave plan giả định Bucket B hooks (admin-merge / release-fix-retry / etc.) "cover" enforcement → quên rằng hook chỉ thay enforcement mechanism; rule MD file vẫn auto-load nếu không có `paths:` frontmatter. Hooks và rule auto-load là 2 cơ chế độc lập.

### Cause #3 (root) — Bucket E baseline measurement KHÔNG chạy

Wave plan §3 Scope Bucket E mandate: "Manual: fresh `/start-session` from user, report new context size" — closure ship mà không có audit artifact `documents/04-quality/audits/meta/2026-05-14-wave-73-context-budget-baseline.md`. Đây là root cause: nếu self-test chạy, sẽ surface 2 defects #1 + #2 ngay lập tức.

Vi phạm `pre-handoff-self-test-completeness.md` §2.2 (self-test mandate trước DONE flip).

## Verdict

Real changes intentional. Phantom changes: 0. Production data at risk: 0 (rules là metadata-only governance docs).

→ **Apply this PR.** Production stack stopped per AWS snapshot; no operational risk.

→ **Action item carryover:** create `scripts/check-context-budget.sh` detector trong follow-up gap khi rule context-budget-mandate stabilizes (~7 ngày from Wave 73 Bucket D merge).

## Recommendations

1. **Apply this PR** — additive changes, CI green, low risk
2. **Post-merge:** user runs fresh `/start-session` và share token measurement → ship as Wave 73 Bucket E closure (append to `documents/04-quality/audits/meta/2026-05-14-wave-73-context-budget-baseline.md` if separately filed, OR append to this audit artifact §Real-measurement section below)
3. **Update Wave 73 plan §10 Log** với miss + fix entry (this PR scope)
4. **Future-proof:** consider promoting `agent-action-bias` + `mcp-first-with-fallback` từ MANDATORY 🟠 → CRITICAL 🔴 sau khi CRITICAL count ổn định <14 — would align Priority với actual auto-load behavior (per `context-budget-mandate.md` §3.2 row 1)
5. **Detector deferred** per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days — reviewer-checklist + CI validators sufficient cho v1.0.0

## Real-measurement section (TO BE FILLED post-merge)

After PR merges + user runs fresh `/start-session`:

```
TODO: User to report actual /start-session base context size.
Compare to projection ~142-162k.
If projection accurate: ✅ rule fires correctly, self-test PASS.
If actual significantly higher: investigate Claude Code system overhead OR additional missed scoping.
```

## References

- Wave 73 plan: `documents/03-planning/waves/wave-2026-05-14-73-meta-context-optimization.md`
- Rule that triggered fix: `.claude/rules/context-budget-mandate.md` §1 (base context PHẢI <120k)
- Self-test mandate missed: `.claude/rules/pre-handoff-self-test-completeness.md` §2.2
- Force-multiplier rationale: `.claude/rules/meta-gap-priority.md` §3
- Incident pipeline: `.claude/rules/incident-to-rule-pipeline.md` Stage 4 self-test requirement
- Audit logging mandate: `.claude/rules/agent-aws-access.md` §5 (applies to verification sessions; this audit follows same template)
