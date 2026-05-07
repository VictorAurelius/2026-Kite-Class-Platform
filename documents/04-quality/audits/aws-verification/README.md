# AWS Verification Audits

**Rule:** [`.claude/rules/agent-aws-access.md`](../../../../.claude/rules/agent-aws-access.md)

Per-session audit artifacts for AWS resource verification (post-deploy smoke tests, resource inventory, endpoint accessibility checks). Each artifact captures the commands run + outputs + findings + next steps.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `YYYY-MM-DD-<topic>.md` | Per-session verification report | One per non-trivial AWS verification session |

---

## File Placement Rules

- ✅ **Belongs here:** AWS resource state inventories, endpoint accessibility checks, post-apply verification reports, secret-populate confirmations (metadata only — never secret values)
- ❌ **Does NOT belong here:**
  - Application-level smoke tests (use `documents/04-quality/audits/ops-readiness/` or app-test framework)
  - Quality audit reports (`audits/quality/`)
  - Security audits (`audits/security/`)
  - Performance audits (`audits/performance/`)
- Naming: `YYYY-MM-DD-<topic-kebab-case>.md` — date in ISO format + topic from `<phase-2-3-post-apply | secrets-populate-check | pre-deploy-smoke | etc>`

---

## Required artifact sections

```markdown
---
title: AWS Verification — <topic>
status: complete
created: YYYY-MM-DD
phase: <e.g. 2.3, 4>
---

# AWS Verification Report — <topic>

## Scope
<What was checked, why>

## Commands run
<Each command + brief purpose; verify Tier 1 per agent-aws-access.md §2>

## Results
<Per-resource state + endpoint check outputs>

## Findings
<Anomalies, concerns, follow-ups>

## Next steps
<Recommended actions>
```

---

## Archive Policy

Move to `documents/07-archived/aws-verification-YYYY/` when:
- Phase referenced is closed (e.g. Phase 1 BETA archived after Phase 2 PAID launch)
- Doc >180 days old AND no recent reference

Keep in active area for ongoing phases.

---

## Key Documents

- [2026-05-08 Phase 2.3 post-apply](2026-05-08-phase-2-3-post-apply.md) — first verification artifact; inventory of 94 resources after Phase 2.3 production apply
