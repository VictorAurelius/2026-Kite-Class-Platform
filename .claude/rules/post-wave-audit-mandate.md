# Post-Wave Audit Mandate

**Priority:** 🔴 MANDATORY — governance for wave/feature delivery
**Created:** 2026-04-19
**Supersedes:** Strengthens `output-review-mandate.md` for waves specifically
**Applies to:** Every wave merge + every feature cluster PR that closes ≥1 gap

---

## 1. The Rule

> **After any wave merge OR gap-closing cluster merge, the required audit suite MUST run within 3 days. The audit-gate hook enforces this — non-compliant follow-up PRs get blocked, not just warned.**

Expanding `output-review-mandate.md` §3 — we had the audit skills, we had the review standard, but we lacked a **cadence rule**. Result: Waves 1-4b shipped without triggering fresh audits, business audit went 27 days stale, ops and performance audits never ran once.

---

## 2. Which audits, when

### 2.1 Required per change pattern (same as `audit-gate.py` AUDIT_RULES)

| File pattern changed | Required audit | Skill |
|---------------------|---------------|-------|
| `kiteclass-frontend/`, `kitehub-frontend/src/` | UI /128 | `quality/ui-review/SKILL.md` |
| `rules.md`, `use-cases.md`, `application.yml` | Business Logic /100 | `quality/business-logic-audit/SKILL.md` |
| `Controller.java`, `api-contract.md`, `Dto.java` | API Contract /100 | `quality/api-contract-audit/SKILL.md` |
| `pom.xml`, `package.json`, `pnpm-lock.yaml` | Security /100 | `quality/security-audit/SKILL.md` |
| `infrastructure/`, `docker-compose`, `Dockerfile`, `helm/`, `k8s/`, `terraform` | Ops Readiness /100 | `quality/ops-readiness-audit/SKILL.md` |
| Performance-critical path (DB query, API handler, bundle) | Performance /100 | `quality/performance-audit/SKILL.md` |

### 2.2 Freshness window

- **3 days** after wave/gap-cluster merge → audit suite MUST run
- **7 days** for general PR compliance (same as `audit-gate.py` AUDIT_FRESHNESS_DAYS)
- If wave merges Monday, full audit suite due by Thursday

### 2.3 Quality audit /100 frequency

Independent cadence (not per-wave):
- **Weekly** when active wave work in flight
- **After every wave merge** (mandatory post-wave checkpoint)
- **Monthly baseline** when in maintenance mode

---

## 3. Enforcement — hook behavior

`audit-gate.py` behavior change (2026-04-19 PR coupled with this rule):

| Condition | Before | After |
|-----------|--------|-------|
| Audit required, none in 7 days, code PR | Warn (systemMessage) | **BLOCK** (decision="block") |
| Audit required, none in 7 days, docs-only PR | Warn | Warn (docs-only exception) |
| Audit required, run <7 days ago | Silent pass | Silent pass |
| No audit required (e.g., README change) | Silent pass | Silent pass |

**Docs-only exception:** PR touches only `.md`, `.claude/rules/`, `.claude/skills/`, `documents/` — no audit required for those PRs even if `pom.xml` mass file pattern triggers.

### Override mechanism

If audit genuinely cannot run (e.g., staging DB down), reviewer can force-merge with:
```
git commit -m "... AUDIT_OVERRIDE: <reason> <link-to-followup-gap>"
```
Hook detects `AUDIT_OVERRIDE:` trailer → warns instead of blocks. Override MUST reference a gap that schedules the audit.

---

## 4. Post-wave audit runbook

After wave merge (e.g., Wave 5 merges):

**Day 0 (merge day):**
- [ ] Wave completion check (`workflow/wave-completion-check.md`)
- [ ] Quality audit /100 refresh

**Day 1-3 (audit window):**
- [ ] All required audits per §2.1 for wave's changed files
- [ ] Reports saved to `documents/04-quality/audits/{category}/`
- [ ] New gaps created per `audit-to-gap-pipeline.md` §3 for issues found
- [ ] ROADMAP updated if new GA blockers surface

**Day 4+ (enforcement):**
- Hook blocks any follow-up PR in wave's domain until audits present

---

## 5. First-run baseline for never-audited categories

Per `output-review-mandate.md` Section 4 VIOLATIONS:
- **Ops Readiness** — no audit ever run → baseline needed
- **Performance** — no audit ever run → baseline needed

These MUST have first-run baseline created before hook enforcement activates. Baseline PR scores the current state (likely 30-60/100 for first-time audits of never-audited categories) and identifies gap queue. Once baseline exists, subsequent PRs measure delta against it.

---

## 6. Integration with existing rules

- **`output-review-mandate.md`** — this rule provides the *cadence* (when); mandate provides the *standard* (what)
- **`audit-to-gap-pipeline.md`** — audit findings feed this pipeline; no direct fixes from audit
- **`meta-gap-priority.md`** — audit findings that touch skills/rules/workflow get meta-boost
- **`wave-completion-check.md` skill (Level 7)** — audit suite is Level 7 gate, this rule enforces it

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---------|------|
| Skip audit because "wave just ended, team tired" | Run within 3 days, document findings as gaps |
| Override without creating follow-up gap | AUDIT_OVERRIDE only with gap link |
| Run only the easy audits (quality /100) | Run ALL audits required per file patterns |
| Let audits go >7 days stale "because no breaking change lately" | Schedule refresh; staleness itself is violation |
| Fix audit findings in same audit PR | Audit creates gap, gap fixed in separate PR (per `audit-to-gap-pipeline.md`) |

---

## 8. Exceptions

| Case | Exception |
|------|-----------|
| Hotfix (CVE, data-loss bug) | Merge first, audit within 24h post-merge |
| Docs-only changes (no code) | No audit required (hook grants docs-only exception) |
| Skill/rule meta-changes | Still requires audit of IMPACT (does change touch code?) |
| Revert/rollback PR | No audit required (reverts prior state) |

Never skipped: security audit (always required when `pom.xml`/`package.json` changes), ops audit (always when infra changes).

---

## 9. Metrics

Track per quarter:
- **Audit latency:** days from wave merge to audit report committed (target: <3)
- **Audit coverage:** % of required audits present per merged PR (target: 100%)
- **Gap-to-audit ratio:** new gaps created from audit / PRs audited (informational)
- **Hook block rate:** % of PRs blocked by audit-gate (target: <5% after steady state)

---

## 10. Log

- **2026-04-19 (later same day):** Part A catch-up 5/5 COMPLETE — business-logic 65/100 (PR #366), ops-readiness 49/100 first-ever (PR #365), performance 58/100 first-ever (PR #364), ui-review KC 81 / KH 59 out of 128 (PR #368), quality-audit refresh 77/100 C+ (PR #369, honest baseline vs 95 self-audit). 39 new gaps GAP-104 → GAP-142. §5 baselines for ops + performance now captured — hook enforcement fully active for future PRs touching those patterns.
- **2026-04-19:** Rule created after user flagged audit drift — Wave 1-4b merged without fresh audits, business audit 27 days stale, ops + performance audits never run. Coupled with `audit-gate.py` hardening (warn → block) and first-run baseline audits (catch-up).
