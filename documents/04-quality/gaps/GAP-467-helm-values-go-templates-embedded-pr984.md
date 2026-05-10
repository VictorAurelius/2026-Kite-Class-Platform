# GAP-467: PR #984 alertmanager block embeds Go templates in values.yaml — breaks `helm lint` and `helm template` on main

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (BLOCKS chart-level smoke test for any future helm work; surfaced as side-finding from Wave 55 Bucket A and Bucket C)
**Domain:** DevOps / Helm
**Found:** 2026-05-11 (Wave 55 Bucket A + Bucket C agents both hit identical failure on main HEAD baseline)
**Affects:** `infrastructure/helm/kitehub/values.yaml` — entire chart cannot pass `helm lint` until fixed; every future helm-touching PR loses chart-level smoke

## Problem

`infrastructure/helm/kitehub/values.yaml` (introduced/extended by PR #984) contains Go template syntax (`{{- if .Values.monitoring.alertmanager.receivers.production.enabled }}` ... `{{- end }}`) directly inside the `monitoring.alertmanager.config` block.

Helm v3 does NOT process Go templates in `values.yaml` — they are loaded as plain YAML first, then templates run on `templates/` files. The embedded `{{- if ... }}` causes plain-YAML parse failure:

```
Error: cannot load values.yaml: line 287: ...
```

Verification (both Wave 55 Bucket A agent and Bucket C agent independently reproduced):
- `git stash && helm lint infrastructure/helm/kitehub` on main HEAD baseline → SAME error → confirms NOT a Wave 55 regression
- Both agents documented this in their PR bodies (#1119, #1120) and proceeded with python YAML structural validation as substitute

## Root Cause

PR #984 author placed conditional rendering logic in the wrong file. The `{{- if ... }}` blocks belong in a `templates/alertmanager-config.yaml` file (where Helm runs the template engine), not in `values.yaml` (which must be plain YAML).

## Proposed Fix

1. Extract the templated alertmanager config block from `values.yaml` into a new `templates/alertmanager-config.yaml` (or similar template file).
2. In `values.yaml`, replace the templated block with a plain YAML structure that the new template file consumes via `.Values.monitoring.alertmanager.receivers.production`.
3. Verify `helm lint infrastructure/helm/kitehub` and `helm template infrastructure/helm/kitehub` both clean.
4. Add a guard test (extend `scripts/check-docs.sh` or new `scripts/check-helm-lint.sh`) that fails CI if Go template syntax appears in any `*.yaml` under `infrastructure/helm/*/values*.yaml`.

## Acceptance Criteria

- [ ] `infrastructure/helm/kitehub/values.yaml` parses cleanly as plain YAML (no `{{-` / `}}-` tokens)
- [ ] Templated logic moved to `templates/alertmanager-config.yaml`
- [ ] `helm lint infrastructure/helm/kitehub` exits 0 on a clean checkout of main
- [ ] `helm template infrastructure/helm/kitehub > /dev/null` exits 0
- [ ] CI guard added to prevent recurrence (new shell test OR extension of `scripts/check-docs.sh`)
- [ ] Wave 55 Bucket A PR #1119 + Bucket C PR #1120 re-verified post-fix to confirm chart-level smoke passes

## Related

- PR #984 (introducer)
- Wave 55 plan: `documents/03-planning/waves/wave-2026-05-11-55-observability-validation.md`
- Bucket A discovery: PR #1119 body §"Local verify results"
- Bucket C discovery: PR #1120 body §"Verification results"
- Pattern parallel: `feedback_post_merge_doc_sync.md` — when 2 independent agents flag same pre-existing issue, file gap immediately

## Log

- **2026-05-11:** Gap filed after both Wave 55 Bucket A (agent ac1d7ea23b9cd3573, PR #1119) and Bucket C (agent ae85707caedd67b21, PR #1120) independently hit identical `helm lint` failure on main HEAD baseline. Both agents verified via `git stash` that the failure is NOT a regression of their own work. Single root cause = PR #984 design choice; single fix unblocks all future helm work.
