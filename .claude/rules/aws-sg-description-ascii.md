# AWS Security Group Description ASCII-Only

**Priority:** 🟠 MANDATORY — terraform pre-apply guard
**Version:** 1.0.0
**Created:** 2026-05-07
**Last-Reviewed:** 2026-05-07
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement; no constraint loosening for prior work; migrated from session memory `feedback_aws_sg_description_ascii_only.md` for git-tracked durability)
**Applies to:** Every `aws_security_group` resource in `infrastructure/terraform-aws/**`, plus copy-paste sites where `description` arguments enter terraform from architecture docs / markdown

---

## 1. The Rule

> **The `description` argument on `aws_security_group` MUST contain ASCII characters only.** AWS EC2 API rejects non-ASCII (em-dash, en-dash, smart quotes, Vietnamese diacritics) with `InvalidParameterValue: Character sets beyond ASCII are not supported`.

This restriction is unique to `aws_security_group.description` — RDS, S3, IAM, EC2 tags all accept Unicode. AWS Console UI silently strips non-ASCII; terraform passes through verbatim → API failure mid-apply.

---

## 2. Concrete patterns

| ❌ BANNED character | ✅ ASCII replacement | Unicode |
|---|---|---|
| `—` em-dash | `-` hyphen-minus | U+2014 → U+002D |
| `–` en-dash | `-` hyphen-minus | U+2013 → U+002D |
| `"` left smart quote | `"` straight quote | U+201C → U+0022 |
| `"` right smart quote | `"` straight quote | U+201D → U+0022 |
| `'` `'` smart apostrophes | `'` straight apostrophe | U+2018/U+2019 → U+0027 |
| Vietnamese diacritics (`ạ`, `ố`, `ư`, etc.) | English ASCII paraphrase | — |

Em-dash is the most common offender because Vietnamese tech writing uses it freely; copy-paste from architecture docs / markdown carries it into terraform.

---

## 3. How to apply

### 3.1 Pre-apply detection

Run before every `terraform apply` touching SG resources:

```bash
grep -nE "description.*[^[:ascii:]]" infrastructure/terraform-aws/*.tf
```

Exit code 0 + zero matches = clean. Any match = fix to ASCII before apply.

### 3.2 Where the constraint binds

Only `aws_security_group.description` (and the legacy `aws_security_group_rule.description` field). Other tf fields are NOT restricted — keep Unicode in:
- `tags = { ... }` values
- `aws_db_instance` description-shaped fields
- Resource comment lines (`# em-dash OK here`)
- Outputs / locals descriptions

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Copy SG description from architecture markdown without scrubbing | Run grep `[^[:ascii:]]` first; replace em-dash → `-` |
| Assume "if Console UI accepts it, terraform will" | Console silently strips; terraform passes through; API rejects |
| Add Vietnamese diacritics to SG description "for clarity" | Use English ASCII; comments above can carry Vietnamese |
| Re-apply after fix without checking other SG resources in same file | Grep ALL `*.tf` once; fix every match before resume |

---

## 5. Enforcement

### 5.1 Reviewer manual (active now)

Pre-merge PR review for any diff touching `infrastructure/terraform-aws/*.tf` with new/changed `aws_security_group` resources: reviewer runs `grep -nE "description.*[^[:ascii:]]" infrastructure/terraform-aws/*.tf` and confirms zero matches.

### 5.2 Memory auto-load (per-session)

Memory entry `feedback_aws_sg_description_ascii_only.md` (now a pointer to this rule) loads at session start, reminding Claude before any terraform-aws edit.

### 5.3 Pre-commit hook (deferred)

Future automated guard — `.husky/pre-commit` step running the §3.1 grep against staged `infrastructure/terraform-aws/*.tf` files. Tracked as future enhancement; reviewer manual + memory auto-load sufficient for solo-dev mode pending recurrence.

---

## 6. Self-test (worked example — Phase 2.3 production apply 2026-05-08)

**Scenario:** Terraform apply for full Architecture B stack (71 resources) on AWS account 906286017800.

**At decision time** (pre-apply): rule §3.1 grep was NOT run. `security-groups.tf:83` + `staging.tf:69,114` contained em-dash `—` carried in from architecture markdown.

**Outcome without rule:** Apply #1 failed mid-run at first SG resource → `InvalidParameterValue` → fix em-dash → resume → succeeded. Cost: 1 wasted apply attempt + partial-state recovery overhead.

**Counterfactual with rule:** Pre-apply grep would have surfaced 3 matches → fix to ASCII before apply → apply succeeds first time. Zero partial-state risk.

**Verdict:** rule fires correctly on the original incident. Self-test PASS.

---

## 7. Override mechanism

No legitimate override — AWS API enforces this hard. If a SG description genuinely needs non-ASCII semantic content (extremely rare), document the constraint inline:

```hcl
# AWS_SG_DESCRIPTION_ASCII_OVERRIDE: <reason — must be impossible>
# (no known case; this comment is a placeholder)
```

Pattern frequency >0% per quarter triggers meta-review (rule should have caught it).

---

## 8. Relationship to other rules

- **`release-deploy-standard.md`** §3 — terraform apply is part of production deploy artifact set; this rule is a pre-flight check
- **`agent-aws-access.md`** — production AWS operations; this rule is one of the pre-apply guards alongside `terraform-apply-retry-reconfirm.md`
- **`incident-to-rule-pipeline.md`** — this rule originated from a Phase 2.3 mid-apply failure; codified per 5-stage pipeline (Detect: apply failed → Classify: no rule covers → Rule+Enforce: this file + memory pointer → Self-Test: §6 → Retro Log: §9)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + memory auto-load enforcement land same PR
- **`feedback_aws_sg_description_ascii_only.md`** (memory pointer to this rule)

---

## 9. Log

- **2026-05-07 (v1.0.0):** Migrated from session memory `feedback_aws_sg_description_ascii_only.md` per user request "memory persistence strategy = migrate to .claude/rules/ for git-tracked durability". Original incident: 2026-05-08 Phase 2.3 production apply (account 906286017800) failed mid-run at `security-groups.tf:83` em-dash + 2 more matches in `staging.tf`; pattern confirmed AWS API enforces ASCII-only on SG description. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5). Enforcement: reviewer manual + memory auto-load now; pre-commit hook deferred.
