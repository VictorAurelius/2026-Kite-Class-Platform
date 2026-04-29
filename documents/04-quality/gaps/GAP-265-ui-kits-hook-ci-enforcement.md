# GAP-265: UI Kits Hook + CI + Lefthook Enforcement (Phase 3 of GAP-263)

**Status:** 🔵 OPEN — placeholder filed by foundation PR; full content shipped by Tier 3 agent in same wave (Wave Review Process Improvement)
**Priority:** 🟠 P1 Meta — automated enforcement
**Domain:** Meta / DevOps / Quality / UI Review
**Found:** 2026-04-29 (Wave Review Process Improvement, Tier 3 — paired with GAP-263 Phase 1 + GAP-264 Phase 2)
**Affects:** All future HTML/JSX prototype PRs under `documents/02-architecture/design-system/ui_kits/**`

---

## Problem

Tier 2 (GAP-264) ships callable `ui-review-prototype` skill scripts. Tier 3 needs to **automate** enforcement so reviewers can't accidentally merge a UI kits PR that breaks landing parity OR fails state coverage:

1. **`audit-gate.py` AUDIT_RULES** — block PR touching `ui_kits/**` if integration check evidence missing in PR description / commit trailers
2. **`.github/workflows/ui-kits-integration.yml`** — CI job that runs Tier 1 + Tier 2 scripts on every PR; failure blocks merge
3. **`lefthook.yml` pre-commit hook** — run scripts locally before commit; catch errors before push

Without Tier 3, reviewers must remember to run scripts manually — same drift risk that caused 2026-04-29 incident.

## Root Cause

Phase 1 of GAP-263 (rule) + Phase 2 GAP-264 (skill) provide the standard + tooling. Phase 3 wires enforcement so the standard runs automatically.

## Proposed Fix

### Components

**A. `audit-gate.py` AUDIT_RULES extension:**
```python
{
  "rule": "ui-kits-integration-required",
  "trigger": "files matching documents/02-architecture/design-system/ui_kits/**",
  "check": "PR description contains 'Integration smoke test:' line OR PR has trailer 'INTEGRATION_OK_NO_LANDING_CHANGE: <reason>'",
  "severity": "block",
  "rationale": "GAP-265 — incident 2026-04-29 landing-page parity miss"
}
```

**B. `.github/workflows/ui-kits-integration.yml`:**
- Trigger: `pull_request` on paths `documents/02-architecture/design-system/ui_kits/**`
- Steps:
  1. Checkout
  2. Run Tier 1 script `_shared/scripts/check-ui-kits-landing.sh`
  3. Run Tier 2 scripts (link-checker, landing-parity, state-coverage)
  4. Aggregate results → fail job if any script exits non-zero
  5. Comment on PR with summary table

**C. `lefthook.yml` pre-commit hook:**
```yaml
pre-commit:
  commands:
    ui-kits-parity:
      glob: "documents/02-architecture/design-system/ui_kits/**/*"
      run: bash documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh
```

### Acceptance Criteria

- [ ] `audit-gate.py` extended with `ui-kits-integration-required` rule
- [ ] `.github/workflows/ui-kits-integration.yml` created + green on synthetic PR test
- [ ] `lefthook.yml` extended with `ui-kits-parity` pre-commit command
- [ ] **Self-test 1 (positive):** Synthetic PR touching `ui_kits/**` WITH "Integration smoke test:" line in body → audit-gate passes, CI green
- [ ] **Self-test 2 (negative — landing parity break):** Synthetic PR removes a card from `index.html` → CI workflow runs landing-parity script → exits non-zero → CI red → PR blocked
- [ ] **Self-test 3 (override):** Commit with trailer `INTEGRATION_OK_NO_LANDING_CHANGE: docs-only edit to README` → audit-gate passes (override mechanism works)
- [ ] Documentation updated: `audit-gate.py` AUDIT_RULES table + `.github/workflows/` README index
- [ ] GAP-263 Log entry appended noting Phase 3 SHIPPED via this gap

### Override mechanism

If a PR genuinely doesn't change kit content but touches `ui_kits/**` (e.g. typo fix in shared README), reviewer can:
```
git commit -m "...
INTEGRATION_OK_NO_LANDING_CHANGE: <reason>"
```
Hook detects trailer → downgrades block to warn. Pattern frequency monitored quarterly.

## Related

- **Phase 1:** GAP-263 (output-review-mandate v1.2.0 row + this gap's parent)
- **Phase 2:** GAP-264 (ui-review-prototype skill)
- **Wave plan:** `documents/03-planning/waves/wave-2026-04-29-review-process-improvement.md`
- **Existing audit-gate:** `.claude/hooks/audit-gate.py` AUDIT_RULES
- **Memory:** `feedback_post_merge_doc_sync.md` extended 2026-04-29 with landing-page parity lesson
- **Pattern:** `rule-change-process.md` §6.5 Enforcement Parity Mandate (rule + detection same PR)

## Log

- **2026-04-29 (placeholder filed):** Created by foundation PR of Wave Review Process Improvement. Full enforcement components shipped by Tier 3 agent in same wave. Status will flip 🔵 OPEN → 🟢 DONE upon Agent B merge.
