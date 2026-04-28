# Self-test: `analyze-overlap.sh` on Wave Observability gaps

**Date run:** 2026-04-28
**Operator:** Wave-pack-planner Agent C (skill creation wave)
**Script under test:** `.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh`
**Reference:** `documents/03-planning/waves/wave-2026-04-29-observability.md` §"File overlap analysis"

## Command

```bash
./.claude/skills/quality/wave-pack-planner/scripts/analyze-overlap.sh \
  GAP-121 GAP-143 GAP-144
```

## Stdout (captured verbatim)

```
# File overlap analysis

Gaps analyzed: GAP-121,GAP-143,GAP-144

| File | Touched by | Conflict risk |
|------|-----------|:-------------:|
| `../../02-architecture/adr/ADR-022-alertmanager-secret-strategy.md` (NEW) | GAP-144 | None |
| `.claude/rules/meta-gap-priority.md` | GAP-121 | None |
| `/etc/alertmanager/secrets/alertmanager-receivers/` (NEW) | GAP-144 | None |
| `documents/03-planning/waves/wave-2026-04-29-observability.md` | GAP-143,GAP-144 | *SOFT* |
| `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` | GAP-121,GAP-143,GAP-144 | *SOFT* |
| `documents/05-guides/operations/runbooks/` | GAP-121 | None |
| `documents/05-guides/operations/runbooks/README.md` | GAP-121 | None |
| `infrastructure/helm/README.md` | GAP-144 | None |
| `infrastructure/helm/kitehub/dashboards/` | GAP-143 | None |
| `infrastructure/helm/kitehub/templates/` | GAP-144 | None |
| `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` | GAP-144 | None |
| `infrastructure/helm/kitehub/templates/grafana-dashboards/` | GAP-143 | None |
| `infrastructure/helm/kitehub/templates/prometheusrule.yaml` | GAP-121 | None |
| `infrastructure/helm/kitehub/values.yaml` | GAP-143,GAP-144 | *SOFT* |
| `kiteclass/docker/prometheus/alert-rules.yml` | GAP-121 | None |
| `kitehub/docker/prometheus/alert-rules.yml` | GAP-121 | None |
| `runbooks/db-pool-exhausted.md` (NEW) | GAP-121 | None |
| `runbooks/high-error-rate.md` (NEW) | GAP-121 | None |
| `terraform-aws/secrets.tf` (NEW) | GAP-144 | None |

## Summary

- Files only touched by 1 gap (None): 16
- Files with SOFT conflict risk: 3
- Files with HARD conflict risk: 0

## Note

SOFT conflicts present — git usually auto-merges different sections.
Coordinator must instruct each agent: edit only your section, do not reformat the whole file.
```

**Exit code:** `0` (no HARD conflicts → wave-pack proceeds)

## Comparison with canonical wave plan

The wave plan §"File overlap analysis" lists 8 file rows. Compare row-by-row:

| Canonical wave plan row | Script row | Match? | Notes |
|------------------------|-----------|:------:|-------|
| `documents/05-guides/operations/runbooks/*.md` (A only, None) | `documents/05-guides/operations/runbooks/`, `runbooks/README.md`, `runbooks/db-pool-exhausted.md (NEW)`, `runbooks/high-error-rate.md (NEW)` (GAP-121 only, None) | ✅ Match (over-decomposed) | Script splits the glob into individual files mentioned. Same conclusion (only A touches). |
| `kitehub/docker/prometheus/alert-rules.yml` + `kiteclass/docker/prometheus/alert-rules.yml` (A only, None) | Both files present, GAP-121 only, None | ✅ Match | Script lists each path explicitly. |
| `infrastructure/helm/kitehub/templates/prometheusrule.yaml` (A only, None) | Same row, GAP-121 only, None | ✅ Match | Exact. |
| `infrastructure/helm/kitehub/values.yaml` Grafana section (B only, None) | — | ⚠️ Diff (granularity) | Script has no awareness of YAML sub-sections; rolls up into the file-level row below. |
| `infrastructure/helm/kitehub/values.yaml` Alertmanager section (C only, None) | — | ⚠️ Diff (granularity) | Same as above — rolled up. |
| `infrastructure/helm/kitehub/values.yaml` (whole file) (B + C, **SOFT**) | `infrastructure/helm/kitehub/values.yaml` (GAP-143, GAP-144, *SOFT*) | ✅ Match | This is the load-bearing row: SOFT classification correct. **NOTE:** required the bare-filename sentinel-resolution post-process to detect, since GAP-144 mentions only bare `values.yaml` while GAP-143 uses the fully-qualified path. Without that step, the script missed the most important row of the analysis (caught during self-test, fixed before this report). |
| `infrastructure/helm/kitehub/templates/grafana-dashboards/*.yaml` (NEW) (B only, None) | `infrastructure/helm/kitehub/templates/grafana-dashboards/` (GAP-143, None) | ✅ Match | Script lists the directory; equivalent. |
| `infrastructure/helm/kitehub/templates/alertmanager-external-secret.yaml` (NEW) (C only, None) | Same row, GAP-144, None | ✅ Match | Exact. |

### Extra rows in script output not in canonical plan

The script picked up several cross-reference paths the canonical plan omitted as noise:

| Script row | Risk | Reasoning |
|-----------|:----:|-----------|
| `../../02-architecture/adr/ADR-022-alertmanager-secret-strategy.md (NEW)` | None | Relative-path link from gap file to ADR; not a real "touched file" — gap *references* the ADR, doesn't edit it. Script over-detection. |
| `.claude/rules/meta-gap-priority.md` | None | Cited in GAP-121 §Meta-Boost Justification; not edited. Over-detection. |
| `/etc/alertmanager/secrets/alertmanager-receivers/` (NEW) | None | A runtime mount path (not source-tree path), shouldn't be in a file-overlap matrix at all. Over-detection — would benefit from a `^/etc/`/`^/var/` filter. |
| `documents/03-planning/waves/wave-2026-04-29-observability.md` | SOFT | Both GAP-143 + GAP-144 mention the wave plan in their §Related sections. Real cross-reference, but the doc was written by the coordinator and isn't co-edited by agents — false-positive SOFT. |
| `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` | SOFT | All 3 gaps cite the audit as their origin. Same false-positive: the audit doc isn't being edited, just referenced. |
| `infrastructure/helm/README.md` | None | GAP-144 edits it (operator runbook section); not in canonical plan but legitimately edited. **Real positive** — canonical plan under-specifies. |
| `infrastructure/helm/kitehub/dashboards/` | None | GAP-143 ships dashboard JSON files here. **Real positive** — under-specified in canonical plan. |
| `infrastructure/helm/kitehub/templates/` | None | Bare `templates/` directory mention from GAP-144 narrative; redundant given templates/alertmanager-external-secret.yaml already listed. Over-detection. |
| `terraform-aws/secrets.tf` (NEW) | None | GAP-144 references the existing terraform pattern; not edited in this wave. Over-detection. |

## Verdict

**PARTIAL** — script produces a correct + actionable matrix for the load-bearing decision (does any HARD conflict exist? — no, exit 0; is values.yaml a SOFT B+C overlap? — yes, correctly classified), and exit 0 reflects the canonical wave plan's go-ahead conclusion. However the matrix is noisier than the human-curated canonical version: ~50% of rows are §Related-section cross-references the script can't distinguish from real edit targets.

For first-iteration use this is acceptable — the coordinator reviews the matrix and prunes noise. The script's value is the HARD/SOFT classification + exit-code gate, not pixel-perfect output.

## Follow-ups (TODO inline, NOT new gaps unless serious)

1. **TODO (script v1.1):** Add a `^/etc/`, `^/var/`, `^/tmp/`, `^/usr/` filter to skip runtime mount paths — caught the `/etc/alertmanager/...` row.
2. **TODO (script v1.1):** Add a heuristic to deprioritize tokens that appear ONLY inside `## Related` / `## Audit` / `## Depends:` lines — those are cross-references, not edit targets. Could greatly reduce false-positive SOFT rows.
3. **TODO (script v1.1):** Detect when one path is a strict prefix of another (e.g. `templates/` vs `templates/alertmanager-external-secret.yaml`) and roll the parent up into a "(see children)" row instead of duplicating.
4. **TODO (gap-template, NOT this script):** GAP-144 mentions `values.yaml` bare without the `infrastructure/helm/kitehub/` prefix in 3 places. The script's bare-filename sentinel rescued this case, but a stricter gap template could require fully-qualified paths in §Current State / §Proposed Fix tables. Consider a lint check on gap files. Borderline-serious — file as a follow-up gap if Wave-pack methodology gains adoption and false-positives compound across more waves.
5. **TODO (script v1.1):** Add a `--quiet` flag that suppresses the matrix and just prints "OK / N HARD conflicts" — useful when called from a wave-plan validation hook.

None of these block first usage of the script. Coordinator-pruned matrix is good enough.
