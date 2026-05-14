# Post-Wave Audit Mandate

**Priority:** 🔴 MANDATORY — governance for wave/feature delivery
**Version:** 1.1.0
**Created:** 2026-04-19
**Last-Reviewed:** 2026-05-14
**Reviewer-Approver:** @nguyenvankiet (starter-kit upstream maintainer)
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
| `your-frontend-b/`, `your-frontend-a/src/` | UI /128 | `quality/ui-review/SKILL.md` |
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

### 2.4 Domain-Milestone Audit Cadence (added v1.1.0 — solo-dev pragmatic exception)

> **Solo-dev mode tradeoff:** §2.1 file-pattern matrix triggers audit per wave; for waves clustering within a single isolated domain (e.g. multi-wave `packages/shared-ui/` component port across Wave 27→28→29), per-wave audits produce repetitive low-signal findings. Defer audit suite to **domain milestone** — the wave that closes the cluster — IF AND ONLY IF every wave in the cluster commits the `AUDIT_DEFER_DOMAIN_MILESTONE: <domain>` trailer.

**Eligibility:** A wave qualifies for domain-milestone deferral when ALL these hold:
1. Wave touches files **only within a single domain** (per §2.4.1 domain registry below)
2. The cluster has a **declared milestone wave** (the wave intended to close the domain — must be named in trailer reason)
3. Wave's solo deliverable is **isolated** (no integration with other-domain code in same wave)
4. **Risk profile LOW** (component port, doc port, config refactor — NOT new feature with cross-cutting impact)

#### 2.4.1 Domain registry

| Domain key | Path scope | Audit suite at milestone |
|---|---|---|
| `track-2-shared-ui` | `packages/shared-ui/**` only | UI /128 (sample 3 components) + Security (deps) + Quality /100 |
| `phase-4-kit-ports` | `your-product-b/your-frontend-b/**` + `your-product-a/your-frontend-a/**` (production kit ports) | UI /128 (per kit) + Quality + Performance |
| `release-deploy-artifacts` | `infrastructure/**` + `helm/**` + `terraform/**` + secrets config | Security + Ops Readiness |
| `backend-domain-{name}` | `your-core/**` + `your-product-a/{module}/**` (one BE module cluster) | Business Logic + API Contract + Security |
| `meta-governance` | `.claude/rules/**` + `.claude/skills/**` (rules/skills changes) | NO AUDIT REQUIRED (governance is its own quality gate) |

New domain key requires same-PR addition here (treat as MINOR rule edit per `rule-change-process.md` §5).

#### 2.4.2 Milestone audit obligations (when cluster closes)

At the milestone wave's closure PR:
1. Run audit suite per §2.4.1 row for the domain
2. File audit reports in `documents/04-quality/audits/{category}/`
3. File gaps per `audit-to-gap-pipeline.md` §3 for issues found
4. Update `output-review-mandate.md` §3 matrix rows if any drop status
5. Closure PR's commit body includes `DOMAIN_MILESTONE_AUDIT: <domain> <reports-list>` trailer

**Failure to run audit at milestone:** rule violation. Treat as P1 follow-up gap blocking next domain wave.

#### 2.4.3 Trailer format

Each wave closure in the cluster (except milestone wave) MUST include:

```
git commit -m "...
AUDIT_DEFER_DOMAIN_MILESTONE: track-2-shared-ui — milestone Wave 29 closes Track 2 Phase 3"
```

The trailer:
- Names the domain key from §2.4.1 registry
- Names the planned milestone wave (must be a future wave that hasn't shipped yet)
- One line, semicolon-free in the value (parser splits on `:` once)

**Milestone wave** does NOT use the trailer — instead uses `DOMAIN_MILESTONE_AUDIT:` trailer (per §2.4.2).

#### 2.4.4 Why this is net stricter (not looser)

This rule **looks** like it loosens the 3-day window from §2.2. It does not:
- §2.2 still enforces 3 days for waves that touch multiple domains OR don't qualify for §2.4 deferral
- §2.4 transfers obligation to MILESTONE wave with HARDER requirement: audit must run + reports filed + matrix updated + gaps filed
- Net effect: audit happens once per domain cluster instead of per wave, but it MUST happen at milestone, blocking next cluster

If solo-dev silently skips milestone audit → rule violation, P1 gap blocks next cluster work.

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

### Domain-milestone deferral trailer (added v1.1.0)

Per §2.4 Domain-Milestone Audit Cadence, waves within a single-domain cluster can defer audit to milestone:

```
git commit -m "... AUDIT_DEFER_DOMAIN_MILESTONE: <domain-key> — milestone Wave NN closes <domain>"
```

Hook behavior on this trailer:
1. Validate `<domain-key>` is in §2.4.1 registry (else FAIL — typo prevention)
2. Validate diff touches ONLY paths within the domain's path scope (else FAIL — wave touches outside domain, ineligible)
3. If both pass → silent pass (audit deferred to milestone)
4. Hook also tracks deferred-cluster state: if milestone wave doesn't close within 14 days of first deferral → WARN (cluster stalled; risk of forgotten audit obligation)

Milestone wave uses different trailer:
```
git commit -m "... DOMAIN_MILESTONE_AUDIT: <domain-key> documents/04-quality/audits/ui/2026-..., documents/04-quality/audits/security/2026-..."
```

Hook validates: trailer present + at least 1 audit report file path listed + each path exists in diff or already in repo. Else FAIL.

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

- **2026-05-06** (v1.1.0): MINOR — added §2.4 Domain-Milestone Audit Cadence + §3 audit-gate trailer detector (`AUDIT_DEFER_DOMAIN_MILESTONE`, `DOMAIN_MILESTONE_AUDIT`). Triggered by user flagging "11 days no audit since 2026-04-25, 22 wave merged" — rule enforced per file-pattern matrix but solo-dev mode skipped per-wave audit gate informally. v1.1.0 codifies practical "audit at domain milestone" pattern WITH stricter milestone obligation (audit MUST run + reports + matrix + gaps at milestone). Net stricter for solo-dev mode (deferred ≠ skipped). Domain registry §2.4.1 covers Track 2 shared-ui + Phase 4 kit ports + Release deploy artifacts + Backend domain clusters + Meta-governance. Paired same-PR with `audit-gate.py` AUDIT_RULES update + memory `feedback_domain_milestone_audit.md` + self-test on Wave 27/28 retrospectively per `rule-change-process.md` §6.5 Enforcement Parity Mandate. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — no constraint loosening; transfers obligation from per-wave to per-cluster with stricter milestone enforcement).
- **2026-04-28** (v1.0.0 backfill): Frontmatter backfill per GAP-249 — added Version + Last-Reviewed + Reviewer-Approver fields. No content change. Solo-dev PATCH self-approve per `rule-change-process.md` §5.
- **2026-04-19 (later same day):** Part A catch-up 5/5 COMPLETE — business-logic 65/100 (PR #366), ops-readiness 49/100 first-ever (PR #365), performance 58/100 first-ever (PR #364), ui-review KC 81 / KH 59 out of 128 (PR #368), quality-audit refresh 77/100 C+ (PR #369, honest baseline vs 95 self-audit). 39 new gaps GAP-104 → GAP-142. §5 baselines for ops + performance now captured — hook enforcement fully active for future PRs touching those patterns.
- **2026-04-19:** Rule created after user flagged audit drift — Wave 1-4b merged without fresh audits, business audit 27 days stale, ops + performance audits never run. Coupled with `audit-gate.py` hardening (warn → block) and first-run baseline audits (catch-up).
