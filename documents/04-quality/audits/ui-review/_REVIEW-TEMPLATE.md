---
title: "UI Kits Wave Review Report — TEMPLATE"
status: triage
audit_date: YYYY-MM-DD
auditor: "@<github-handle> (role)"
review_standard: ".claude/rules/output-review-mandate.md v{X.Y.Z} §3 row \"HTML/JSX prototypes\""
checklist: documents/02-architecture/design-system/dossier/10-acceptance-criteria.md
prs_reviewed: [<PR1>, <PR2>, ...]
verdict: APPROVE_FOR_MERGE | REQUEST_CHANGES | ESCALATE
---

# UI Kits Wave Review Report — TEMPLATE

> Carved from PR #677 (review report for Wave 1 add-ons) + extended with **Integration Smoke Test** mandatory section (per incident 2026-04-29 — landing-page parity miss).
>
> Use this template for every wave that ships HTML/JSX prototype kits. Save instance to `documents/04-quality/audits/ui-review/{YYYY-MM-DD}-{wave-name}-review.md`.

---

## 1. Review process disclosure

| Item | Status |
|------|--------|
| Review standard | ✅ `output-review-mandate.md` v{X.Y.Z} §3 row "HTML/JSX prototypes" |
| Review checklist | ✅ `dossier/10-acceptance-criteria.md` 100-item + Round 2 deliverable acceptance gate matrix |
| Reviewer role | {coordinator solo-dev / external pair / etc. — disclose limitations} |
| Review evidence | This document. |
| Tier 1 landing parity script | {pass/fail/skip — `_shared/scripts/check-ui-kits-landing.sh` exit code} |
| Tier 2 prototype-review skill (GAP-264) | {pass/fail/skip — `ui-review-prototype` skill scripts ran} |
| Tier 3 CI/hook enforcement (GAP-265) | {auto-blocked PR if non-compliant — citation of CI run URL} |
| Sign-off | {coordinator + user vibe-check + explicit merge GO} |

---

## 2. Round 2 Deliverable Acceptance Gate Matrix

Per `dossier/10-acceptance-criteria.md` §"Round 2 deliverable acceptance gate":

| Kit | PR | Avg /128 | Min /128 | States covered | Persona | Mock VN | Quality gate self-pass |
|-----|:--:|:--------:|:--------:|:--------------:|:-------:|:-------:|:----------------------:|
| `<kit-name>` | #<num> | <avg> | <min> | ✓ {N states list} | ✓ <persona> | ✓ | ✓ |
| **Wave aggregate** | — | <avg> | <min> | ✓ <total screens> | ✓ <N personas served> | ✓ | ✓ APPROVE / REQUEST_CHANGES / ESCALATE |

**Targets:**
- ☐ Each kit avg ≥ 105 (or higher per kit-specific target)
- ☐ Each kit min ≥ 95 floor
- ☐ All states present per dossier §"Per-screen acceptance §4 States"
- ☐ Component coverage documented (G1..G12 reuse mapping)
- ☐ Flow coverage documented (Flow 1..10 mapping)
- ☐ Pain-point screen lift quantified (vs production baseline)

---

## 3. Per-PR audit findings

For EACH PR in `prs_reviewed`, copy this section:

### 3.{N} PR #<num> — {kit-name}

**Files:** {N} ({breakdown: screens / supporting / baseline})
**LOC added:** {N}
**CI status:** {N/M green}

| Section | Pass / Total | Findings |
|---------|:------------:|----------|
| §1 Visual fidelity | {check or list issues} | |
| §2 Vietnamese UX | | |
| §3 Accessibility | | |
| §4 States coverage | | |
| §5 Persona alignment | | |
| §6 Data realism | | |
| §7 Component reuse | | |
| §8 Performance signals | | |
| §9 i18n readiness | | |
| §10 Documentation | | |

**Anomalies noted:** {none / list with impact assessment}
**Verdict:** ✅ APPROVE | ⚠️ REQUEST CHANGES | ❌ ESCALATE

---

## 4. Cross-PR integrity checks

| Check | Result |
|-------|:------:|
| `_shared/` untouched by all agents | {✓ / list violations} |
| `_v1-baseline/` (if present) untouched | {✓ / list} |
| `documents/02-architecture/design-system/dossier/` untouched | {✓ / list} |
| Production code `kitehub-frontend/src/**` + `kiteclass-frontend/src/**` untouched | {✓ / list} |
| Worktree contamination | {0 / list incidents} |
| File conflicts predicted (matrix) | {0 HARD, N SOFT} |
| File conflicts actual | {match prediction or note divergence} |
| Branch naming convention | {✓ / list violations} |
| No `Co-Authored-By` trailer | {✓ / list violations} |
| CI status all PRs | {N/M green} |

---

## 5. **Integration Smoke Test (MANDATORY — added 2026-04-29 GAP-263 v1.3.0)**

> This section was ADDED after Wave UI Kits Round 2 incident: closure PR #678 updated `README.md` Status table but missed `index.html` landing page. User catch via browser → 3/6 cards visible. Process gap: ad-hoc spot-check ≠ formal integration test.

### 5.1 Landing parity script (Tier 1)

```bash
bash documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh
```

| Field | Value |
|-------|-------|
| Exit code | {0 PASS / 1 FAIL / 2 ERROR} |
| Kit folders found | {N} |
| Cards in landing | {N} |
| Missing cards | {none / list} |
| Orphan cards | {none / list} |
| Run timestamp | {YYYY-MM-DD HH:MM:SS} |

### 5.2 Browser walk-through (reviewer-performed, MANDATORY)

Reviewer opens `http://127.0.0.1:PORT/documents/02-architecture/design-system/ui_kits/` and clicks through every kit card. For each kit, verify:

| Kit | Landing card | First screen loads | Sample 3 screens load | Dark variant | Notes |
|-----|:------------:|:------------------:|:---------------------:|:------------:|-------|
| {kit-1} | ✓ / ✗ | ✓ / ✗ | {N/3} | ✓ / ✗ | |
| {kit-2} | ✓ / ✗ | ✓ / ✗ | {N/3} | ✓ / ✗ | |
| ... | | | | | |

### 5.3 Tier 2 prototype-review skill (GAP-264) — when available

```bash
# placeholder until Tier 2 ships
.claude/skills/quality/ui-review-prototype/scripts/link-checker.sh
.claude/skills/quality/ui-review-prototype/scripts/landing-parity.sh   # stricter than Tier 1
.claude/skills/quality/ui-review-prototype/scripts/state-coverage.sh
```

| Script | Exit code | Findings |
|--------|:---------:|----------|
| link-checker | | |
| landing-parity | | |
| state-coverage | | |

### 5.4 Tier 3 CI/hook (GAP-265) — when available

| Check | Status |
|-------|:------:|
| `audit-gate.py` AUDIT_RULES `ui-kits-integration-required` triggered | {✓ / ✗ / N/A} |
| `.github/workflows/ui-kits-integration.yml` job result | {green/red/N/A} |
| `lefthook.yml` pre-commit hook ran locally | {✓ / ✗ / N/A} |

---

## 6. Anomalies + follow-up items

| # | Anomaly | Impact | Action |
|:-:|---------|--------|--------|
| 1 | {description} | {none/low/medium/high} | {ticket/memory/inline-fix/defer} |

---

## 7. Quality gate self-report aggregate

Per `dossier/prompts.md` §7 Acceptance check format:

```
| Section | Score /10 (avg across N kits) |
|---------|:---:|
| 1 Visual fidelity | X/10 |
| 2 VN UX | X/10 |
| 3 Accessibility | X/10 |
| 4 States | X/10 |
| 5 Persona | X/10 |
| 6 Data realism | X/10 |
| 7 Component reuse | X/10 |
| 8 Performance | X/10 |
| 9 i18n | X/10 |
| 10 Documentation | X/10 |
| **TOTAL** | **XX/100** |

Per-screen score /128:
- Wave aggregate: XXX/128 across N screens
- Lift vs Round 1 baseline ~73/128: +X%

Self-verdict: SHIP / FIX-BEFORE-SHIP / ESCALATE
```

---

## 8. Approval

**Coordinator review:** {APPROVE / REQUEST CHANGES / ESCALATE}.

**Integration smoke test:** {PASS / FAIL with specific failure list}.

**User vibe-check status:** {COMPLETE / PENDING — preview server URL}.

**Merge order** (per `feedback_parallel_agent_strategy.md`):
1. PR #X — {reason}
2. PR #Y — {reason}
3. ...

**Cleanup post-merge:**
- Worktrees to remove
- Branches local + remote to delete
- Single closure PR fixes (root README + wave plan + ROADMAP + wave-history.jsonl)

**Merge gate:** explicit user "merge" or equivalent GO signal required.

---

## 9. Related

- Standard: `.claude/rules/output-review-mandate.md` v{X.Y.Z} §3 row "HTML/JSX prototypes"
- Checklist: `documents/02-architecture/design-system/dossier/10-acceptance-criteria.md`
- Wave plan: `documents/03-planning/waves/wave-{date}-{theme}.md`
- Tier 1 script: `documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh`
- Tier 2 skill (when shipped): `.claude/skills/quality/ui-review-prototype/SKILL.md` (GAP-264)
- Tier 3 enforcement (when shipped): `audit-gate.py` rule + `.github/workflows/ui-kits-integration.yml` (GAP-265)

---

## 10. Log

- **YYYY-MM-DD:** Review report written by {auditor}. Verdict: {APPROVE/REQUEST CHANGES/ESCALATE}. Standard-of-care evidence saved as this document; user vibe-check + explicit merge GO required before squash merges.
