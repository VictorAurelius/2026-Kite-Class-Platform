---
name: ops-readiness-audit
description: "Dùng khi user nói 'ops audit', 'production ready?', 'kiểm tra ops', 'deploy checklist', 'monitoring check', hoặc trước GA release. Production operations readiness /100."
user-invocable: true
---

# /ops-readiness-audit — Production Operations Readiness

Score /100. Verify the platform is ready for production operations: monitoring, logging, backup, alerting, deployment.

## Process

### 1. Check Infrastructure Config

```bash
# Monitoring endpoints (broad scope — catches all submodules + infrastructure)
grep -rn "actuator\|prometheus\|metrics\|health" --include="*.yml" --include="*.yaml" \
  | grep -v node_modules | grep -v target | head -30

# Logging config (broad scope)
grep -rn "logging\.\|logback\|log4j\|winston\|pino" --include="*.yml" --include="*.xml" --include="*.ts" \
  | grep -v node_modules | grep -v target | head -20

# Backup/DR docs
ls documents/05-guides/*backup* documents/05-guides/*disaster* documents/05-guides/*recovery* 2>/dev/null

# Helm/k8s health probes
grep -rn "livenessProbe\|readinessProbe\|startupProbe" infrastructure/ | head -10

# Alert rules
ls infrastructure/*/alerting* infrastructure/*/alerts* 2>/dev/null
```

### 2. Primacy: bug-finding > scoring (BLOCKING)

> **An audit's purpose is to surface ops-operational gaps the dev team cannot trust other layers to catch. A `60/100 D` score with hidden P0 gaps (no restore drill, no PII scrubber, no alert rules) is WORSE than `40/100` listing every gap honestly.** Per `.claude/rules/audit-skill-rubric-ops-readiness-audit.md` §4 (mirror of Wave 71c security-audit primacy pattern).

Rules for every audit run:
1. Enumerate ALL §3 sub-checks per category. NEVER skip "obviously fine."
2. Each sub-check returns: PASS / FAIL / N/A-with-reason / `❓ UNCHECKED`. No partial credit.
3. Final output starts with **bug list** (every FAIL surfaces) BEFORE the score.
4. Score is descriptive only; audit-level verdict = FAIL if ANY P0 sub-check FAILS.
5. If audit time-budget runs out, mark `❓ UNCHECKED` — do NOT default to PASS.

### 3. Score 5 Categories with per-check rubric

Per Wave 72b Bucket E (GAP-523 closure), every category binds to per-check pass/fail rule. Within each 20-pt category: any P0/P1 sub-check FAIL caps category total ≤ 16/20.

| # | Category (20pts) | Per-check rubric file |
|---|-----------------|-----------------------|
| 1 | **Monitoring & Observability** | **`.claude/rules/audit-skill-rubric-ops-readiness-audit.md` §2.1 (6 sub-checks, per-check pass/fail)** |
| 2 | **Logging Standards** | **`.claude/rules/audit-skill-rubric-ops-readiness-audit.md` §2.2 (6 sub-checks, per-check pass/fail)** |
| 3 | **Backup & Recovery** | **`.claude/rules/audit-skill-rubric-ops-readiness-audit.md` §2.3 (6 sub-checks, per-check pass/fail)** |
| 4 | **Alerting** | **`.claude/rules/audit-skill-rubric-ops-readiness-audit.md` §2.4 (6 sub-checks, per-check pass/fail)** |
| 5 | **Deployment Pipeline** | **`.claude/rules/audit-skill-rubric-ops-readiness-audit.md` §2.5 (6 sub-checks, per-check pass/fail)** |

#### Per-check scoring (all 5 categories)

For each Category N:
1. Walk through every §2 sub-check in the bound rule.
2. Mark each sub-check PASS / FAIL / N/A-with-reason / `❓ UNCHECKED` (no partial credit).
3. Score = `20 - (failed_P0_count * 6) - (failed_P1_count * 3) - (failed_P2_count * 1)`, floor 0; cap 20 if all PASS.
4. If ANY P0 sub-check fails → category total CAPPED at 16/20 AND audit-level verdict = FAIL regardless of total score.
5. Each FAIL surfaces in the audit-report bug list per §2 primacy.

Legacy scoring narrative: `reference/scoring-guide.md` retained for backward-compat only — bound rule §2 IS the canonical rubric per Wave 72b Bucket E.

### 4. Output

Save to `documents/04-quality/audits/ops/ops-readiness-audit-[date].md`

## Context Management

Token budget ~25-35K. Kiểm soát:

1. **Grep infrastructure files** — `| head -20` per grep. Infrastructure files nhiều nhưng config sections lặp lại.
2. **Helm values** — Chỉ đọc security-relevant keys (`resources`, `probes`, `securityContext`), không đọc full values.yaml.
3. **Terraform** — Chỉ check state backend config + resource count, không đọc full .tf files.
4. **Doc existence check** — Dùng `ls` thay vì `cat` cho backup/DR docs. Chỉ đọc nội dung nếu cần verify chi tiết.

## Gotchas

- Spring Boot Actuator may be enabled but endpoints not exposed — check `management.endpoints.web.exposure`
- Prometheus metrics need `/actuator/prometheus` endpoint AND scrape config in k8s
- RabbitMQ monitoring: separate from app monitoring — check `rabbitmq_management` plugin
- MinIO: backup strategy different from PostgreSQL — object storage vs relational
- KiteHub has 6 microservices — each needs independent health check
- Terraform state: must be remote (S3/OCI bucket), not local
- **Multi-module scope** — narrow grep `kiteclass/ kitehub/` may miss submodule config files; prefer broad `--include="*.yml"` from root. Ref: GAP-149, memory `feedback_audit_grep_scope.md`.

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
