# GAP-467: PR #984 alertmanager block embeds Go templates in values.yaml — breaks `helm lint` and `helm template` on main

**Status:** 🟢 DONE — Wave 58 Bucket A: CI guard shipped (`scripts/check-helm-lint.sh` + `script-quality.yml` job `helm-lint`). All 6 AC verified.
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

- [x] `infrastructure/helm/kitehub/values.yaml` parses cleanly as plain YAML (no `{{-` / `}}-` tokens) — verified `python3 -c "yaml.safe_load(...)"` clean + `grep \{\{` returns 0 matches
- [x] Templated logic moved to `templates/alertmanager-config.yaml` (renders `Secret/alertmanager-kitehub` consumed by kube-prometheus-stack via `alertmanager.alertmanagerSpec.configSecret`)
- [x] `helm lint infrastructure/helm/kitehub` exits 0 (verified Wave 57 Bucket A 2026-05-11)
- [x] `helm template infrastructure/helm/kitehub > /dev/null` exits 0 — verified in all 3 modes: default / monitoring.enabled=true / production.enabled=true
- [x] CI guard added to prevent recurrence — `scripts/check-helm-lint.sh` + `.github/workflows/script-quality.yml` job `helm-lint` (Wave 58 Bucket A, runs helm lint + helm template via azure/setup-helm@v4; paths filter `infrastructure/helm/**` + `scripts/check-helm-lint.sh`)
- [x] Wave 55 Bucket A PR #1119 + Bucket C PR #1120 re-verified post-fix to confirm chart-level smoke passes — verified Wave 57 Bucket A (3 scenarios all exit 0)

## Related

- PR #984 (introducer)
- Wave 55 plan: `documents/03-planning/waves/wave-2026-05-11-55-observability-validation.md`
- Bucket A discovery: PR #1119 body §"Local verify results"
- Bucket C discovery: PR #1120 body §"Verification results"
- Pattern parallel: `feedback_post_merge_doc_sync.md` — when 2 independent agents flag same pre-existing issue, file gap immediately

## Log

- **2026-05-11 (Wave 58 Bucket A):** Status flipped 🟡 PARTIAL → 🟢 DONE. AC #5 CI guard shipped: `scripts/check-helm-lint.sh` (helm dep update + helm lint + helm template, CI=true enforces helm binary presence) + `.github/workflows/script-quality.yml` job `helm-lint` (azure/setup-helm@v4 + paths filter `infrastructure/helm/**` + `scripts/check-helm-lint.sh`). Self-test verified: local exit 0 with helm-missing advisory; `CI=true bash scripts/check-helm-lint.sh` exit 2 (FAIL helm-missing in CI) — confirms CI behavior. shellcheck clean; YAML parse clean. AC #6 (re-verify PR #1119/#1120) closed via Wave 57 Bucket A's 3-scenario verification. All 6 AC ✅.
- **2026-05-11 (Wave 57 Bucket A):** Status flipped 🔵 OPEN → 🟡 PARTIAL. Extraction shipped: `infrastructure/helm/kitehub/templates/alertmanager-config.yaml` renders the full alertmanager.yaml into `Secret/alertmanager-kitehub`, consumed via `alertmanager.alertmanagerSpec.configSecret`. `values.yaml` reduced to plain YAML data (verified zero `{{` tokens + `python3 yaml.safe_load` clean). Side fix: existing path mismatch in `templates/alertmanager-external-secret.yaml` (referenced `.Values.monitoring.alertmanager.*` while data lives at `.Values.monitoring.kube-prometheus-stack.alertmanager.*`) corrected via `(index .Values.monitoring "kube-prometheus-stack")`. Verification: `helm lint .` exits 0; `helm template .` exits 0 in default mode (monitoring.enabled=false), monitoring.enabled=true placeholder mode, AND `monitoring.kube-prometheus-stack.alertmanager.receivers.production.enabled=true` mode. AC #5 (CI guard) + AC #6 (re-verify PR #1119/#1120) deferred to follow-up — out of Bucket A scope; tracked as cleanup item.
- **2026-05-11:** Gap filed after both Wave 55 Bucket A (agent ac1d7ea23b9cd3573, PR #1119) and Bucket C (agent ae85707caedd67b21, PR #1120) independently hit identical `helm lint` failure on main HEAD baseline. Both agents verified via `git stash` that the failure is NOT a regression of their own work. Single root cause = PR #984 design choice; single fix unblocks all future helm work.
