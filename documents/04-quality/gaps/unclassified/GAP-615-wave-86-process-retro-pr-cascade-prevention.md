# GAP-615: Wave 86 process retro — codify PR cascade prevention + state-check before fix-spawn

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (meta — force-multiplier per `meta-gap-priority.md` §3)
**Domain:** Meta
**Phase:** n/a
**Found:** 2026-05-18 (Wave 86 closure retro flagged by user mid-session 2026-05-16)
**Affects:** Every multi-agent wave session; rule infrastructure `feedback_parallel_agent_strategy.md`, `release-fix-retry-budget.md`, `audit-to-gap-pipeline.md`

## Problem

Wave 86 closure session (2026-05-16) ship 13 PRs trong 1 session với 6 force-push rebases cascade — ~150-200 CI invocations (vượt 3-4× solo-dev target 50/total per CLAUDE.md). User flagged anti-pattern mid-session: "lần đầu bot raise nhiều CI thế có đúng không?". Coordinator thừa nhận anti-pattern nhưng KHÔNG codify thành rule extension.

### 5 anti-patterns đã verify trong Wave 86 closure:

1. **Quá nhiều PRs / 1 wave** — 13 PRs (4 Batch 1 + 2 fix + 5 audit/workflow) vs `feedback_parallel_agent_strategy.md` recommendation 3-5 parallel. Mỗi agent ship PR riêng thay vì 1 PR batched per cluster.

2. **Sequential rebase cascade** — Agents push parallel touch shared CSV (`audits-index.csv` + `gap-status.csv`). Mỗi merge → others DIRTY → force-push → fresh CI ×10-15 checks. 6 cascade rebases trong session = ~90 wasted CI checks.

3. **Vercel kiteclass external fail repeat 13× không pivot** — Mọi PR fail Vercel kiteclass preview (external, unrelated docs PR). Per `release-fix-retry-budget.md` v1.1.0 §3 retry ≥2 same gate = pivot trigger — không kích hoạt vì rule chưa scope cho non-blocking external fails.

4. **Agent F STALE finding wasted run** — Agent D's Bucket E sweep claim "no account lockout" → Agent F spawned to ship lockout work → Agent F state-check phát hiện lockout đã ship Wave 72a GAP-515. ~15 min wasted; should have state-checked sweep findings BEFORE spawn fix-agent.

5. **Audit doc gitleaks chicken-egg** — Agent F ship `2026-05-16-wave-86-gitleaks-baseline.md` list AKID tokens + cùng PR add gitleaks workflow → workflow scan new file → BLOCK self. Need `.gitleaks.toml` allowlist template với `documents/04-quality/audits/security/*.md` pre-exempt.

## Root Cause

3 missing rule constraints + 1 enforcement gap:

- **Missing constraint 1:** No "max N agent-spawned PRs per wave" cap → unlimited parallel agent shipping
- **Missing constraint 2:** No "state-check sweep findings before fix-agent spawn" — fix-agents trust sweep claims without verification
- **Missing constraint 3:** No "non-blocking external CI check" classification — every fail blocks merge eligibility
- **Enforcement gap:** Audit-doc/secret-listing chicken-egg pattern not in `.gitleaks.toml` template

## Proposed Fix

### Rule extensions (codify, ship in 1 docs-only PR)

1. **`feedback_parallel_agent_strategy.md` extension** (memory): add rule #12 + #13
   - Rule #12: **Max 5 agent-spawned PRs per wave**. >5 = file follow-up gap to consolidate via single batch PR strategy
   - Rule #13: **State-check sweep findings before spawn fix-agent**. Sweep audit shipped → fix-agent MUST run `grep -rl "<symbol>"` per finding before implementing. STALE finding = update audit + skip; no implementation work

2. **`release-fix-retry-budget.md` v1.2.0 extension**: add §5 row "External CI gate non-blocking"
   - Trigger: same external CI check (Vercel preview / 3rd-party SaaS scan) fails repeat ≥2 PRs trong 1 wave + check NOT in repo's `.github/workflows/` source
   - Action: classify as `EXTERNAL_NON_BLOCKING` — merge eligibility ignores; file infra gap for root cause investigation
   - Override trailer: `EXTERNAL_FAIL_IGNORE: <check-name> — <reason + investigation gap link>`

3. **`audit-to-gap-pipeline.md` §2.5 extension** (state-check at file-time): add row to hardened protocol matrix
   - Pattern: audit doc cites secret-shaped tokens (AKID / JWT / API key samples)
   - Required: same PR add path to `.gitleaks.toml` `allowlist.paths` entry
   - Detector: CI grep for new file matching `documents/04-quality/audits/security/.*\.md` without paired `.gitleaks.toml` diff

4. **`.gitleaks.toml` baseline template extension**: pre-exempt `documents/04-quality/audits/{security,aws-verification,cloudflare-verification}/.*\.md` permanent (eliminates chicken-egg)

### CSV cascade prevention pattern (optional follow-up)

Investigate: ship 1 "wave-closure batched PR" pattern where parallel agents NOT push directly to feature branches but COMMIT to a shared `wave-NN-closure` branch + coordinator squash-merges 1 PR at end. Reduces from N PRs to 1 PR. Trade-off: serializes review.

Tracked as separate gap if Wave 92+ shows recurrence.

## Acceptance Criteria

- [ ] `feedback_parallel_agent_strategy.md` rule #12 + #13 added with worked example from Wave 86
- [ ] `release-fix-retry-budget.md` v1.2.0 §5 external non-blocking gate
- [ ] `audit-to-gap-pipeline.md` §2.5 hardened protocol matrix row for audit-doc-secret pattern
- [ ] `.gitleaks.toml` allowlist extended with audit doc folders pre-exempt
- [ ] Self-test: apply 4 rule extensions retroactively to Wave 86 — verify each fires correctly on the originating incident
- [ ] `rules-index.csv` sync 1 row updated (`release-fix-retry-budget`)
- [ ] Memory `feedback_parallel_agent_strategy.md` last_reviewed date bumped
- [ ] gap-status.csv row GAP-615 OPEN→DONE flip per `gap-done-discipline.md` §2 closure

## Related

- Session context: Wave 86 closure 2026-05-16 (13 PRs #1437-#1463)
- User flag: "lần đầu bot raise nhiều CI thế có đúng không?" + "cancel ci thừa và fix luôn"
- Rules to extend: `feedback_parallel_agent_strategy.md`, `release-fix-retry-budget.md`, `audit-to-gap-pipeline.md`, `.gitleaks.toml`
- Sister anti-pattern: GAP-451 (Spring Boot upstream block) — different class but same "fix-agent before state-check" pattern
- Parent rule: `meta-gap-priority.md` §3 — meta gap force-multiplier

## Log

- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE: 4 proposed rule extensions NOT shipped per-line per GAP-615 §Proposed Fix. release-fix-retry-budget did get v1.2.0 (Investigation phase mandate) — partial overlap with original Wave 86 retro spirit. Re-frame gap with concrete ship targets next wave OR mark as superseded if Wave meta-3  CSV completion_pct adjusted to 10%; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-c-p1-open-2.md`.

- **2026-05-18** Filed retro gap codifying 5 anti-patterns observed Wave 86 closure session 2026-05-16. User-flagged mid-session but coordinator didn't codify before session ended. WSL shutdown interrupted; resumed session reviewed 37 newer commits — none of Waves 87-92 cover this process retro. Drafted as standalone gap ready for review (NOT committed to gap-status.csv yet pending user approval).
