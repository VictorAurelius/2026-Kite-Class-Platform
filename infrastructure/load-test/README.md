# Load Test Scripts

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../.claude/rules/docs-folder-structure.md)

K6 load test scripts cho Phase 1 BETA / Phase 1.5 / Phase 2 baseline + regression validation. Scripts đo P95 latency + error rate + throughput dưới concurrent user load.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `k6-cohort-50-signup.js` | Cohort 50 signup baseline (Wave 86 Bucket H H-AC1) | 50 concurrent VUs, 2min ramp + 5min hold + 2min ramp-down |
| `results/` | Per-run output JSON (gitignored) | `cohort-50-YYYY-MM-DD.json` |

---

## File Placement Rules

- ✅ **Belongs here:** K6/JMeter/Gatling/Locust scripts; load-test result schemas; performance baseline configs
- ❌ **Does NOT belong here:** Performance audit reports (`documents/04-quality/audits/performance/`), runbooks (`documents/05-guides/operations/`), unit/integration tests (in source tree)
- Naming: `<tool>-<scenario>-<scope>.<ext>` — e.g. `k6-cohort-50-signup.js`, `k6-bulk-import-100k.js`

---

## Archive Policy

- Result JSONs >30 days old → archive to `documents/07-archived/load-test-results-YYYY/` for trend baseline reference
- Scripts: stable; rev via git history, not filename versioning

---

## Run procedure

See [`documents/05-guides/operations/load-test-k6-cohort-50-baseline.md`](../../documents/05-guides/operations/load-test-k6-cohort-50-baseline.md) §3 for K6 install + env vars + execution.

---

## Key Documents

- [Cohort 50 Signup Baseline](../../documents/05-guides/operations/load-test-k6-cohort-50-baseline.md) — Wave 86 Bucket H H-AC1 runbook
