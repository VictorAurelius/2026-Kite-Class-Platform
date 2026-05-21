---
paths:
  - "documents/05-guides/deploy/**"
  - "documents/05-guides/operations/**"
  - "documents/02-architecture/env-reference.yaml"
  - "infrastructure/**"
  - ".claude/rules/markdown-variable-reference.md"
  - "scripts/render-env-vars.sh"
  - "scripts/check-unresolved-env-vars.sh"
---

# Markdown Variable Reference — `{{var_name}}` env-specific substitution syntax

**Priority:** 🟠 MANDATORY — docs/scripts/terraform env-specific value parameterization
**Version:** 1.0.0
**Created:** 2026-05-21
**Last-Reviewed:** 2026-05-21
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (render-env-vars.sh + check-unresolved-env-vars.sh tooling + reviewer-checklist + worked self-test trên `_examples/env-reference-self-test.md` roundtrip) per `rule-change-process.md` §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "env-specific value parameterization in docs/scripts/terraform" beyond `production-env-config-registry.md` runtime YAML scope; existing 5,500+ hardcoded values grandfathered per GAP-692 Phase 2 opportunistic refactor schedule)
**Applies to:** Mọi file under `documents/05-guides/deploy/**`, `documents/05-guides/operations/**`, `infrastructure/**` (excl. terraform .tf which uses native HCL `var.` syntax), `.github/workflows/**` markdown comments — chứa env-specific hardcoded value cross-multi-env (account ID, region, domain, secret prefix, email, ECR URI, etc.). Out-of-scope: single-env constants (project name `kitehub`), code identifiers (Java class names), HTTP protocol tokens, brand names.

---

## 1. The Rule

> **Khi doc/script/terraform/markdown chứa env-specific value (account ID, region, domain, email, secret prefix, etc.) sẽ thay đổi cross-env (production / test / dev / staging), PHẢI dùng `{{var_name}}` syntax với canonical source `documents/02-architecture/env-reference.yaml`. Render thông qua `scripts/render-env-vars.sh <env>`; validate via `scripts/check-unresolved-env-vars.sh`.**

GAP-458 → GAP-459 incident (2026-05-09): domain decision changed `kitehub.vn` → `kitehub.me` (Path C); 21 stale refs trong FE source caused AWS Activate Founder denial. Class 4 failure-mode (config-drift) recurrence pattern.

Force-multiplier per `meta-gap-priority.md` §3 — 1 canonical source + render tool → mọi future env-clone / domain rotation / account swap auto-comply → eliminate retroactive sweep cost across 95 files × 5,500+ occurrences.

Sister rule scope:
- **`production-env-config-registry.md` v1.1.1** — runtime `application*.yml` env-var defaults (Spring `${VAR:default}` pattern, scope `application*.yml + docker-compose*.yml + fetch-secrets.sh`)
- **This rule** — docs/scripts/terraform/markdown hardcoded values that need parameterization (scope broader: 5,500+ occurrences targeted GAP-692 Phase 2)

---

## 2. Syntax

### 2.1 Substitution

```markdown
KiteHub production deploy targets AWS account {{aws_account_id}} in region {{aws_region}}.
Domain: {{domain_root}}. Support: {{support_email}}.
```

After `bash scripts/render-env-vars.sh production env-reference.yaml input.md`:

```markdown
KiteHub production deploy targets AWS account 906286017800 in region ap-southeast-1.
Domain: kitehub.me. Support: support@kitehub.me.
```

### 2.2 Escape

Khi cần literal `{{var_name}}` trong docs (e.g., documenting the syntax itself), use backslash escape:

```markdown
Per markdown-variable-reference.md §2.1 syntax: use \{{var_name\}} for substitution.
```

After render: `\{{var_name\}}` → literal `{{var_name}}` in output. Render strips backslash + keeps braces.

### 2.3 Variable name convention

- `lowercase_snake_case` (per env-reference.yaml schema)
- Match canonical names: `aws_account_id`, `aws_region`, `domain_root`, `support_email`, `db_name`, `db_user`, `secret_prefix`, `ecr_uri`, `iam_deploy_role`, `cloudtrail_name`
- New variable name added to `env-reference.yaml` PHẢI thêm cùng PR với usage

---

## 3. When to use vs when NOT to use

### ✅ Use `{{var}}` for

| Value class | Example | Why |
|---|---|---|
| AWS account ID | `{{aws_account_id}}` (`906286017800` prod) | Different per multi-account split Phase 2 |
| AWS region | `{{aws_region}}` (`ap-southeast-1`) | Phase 3 may migrate region |
| Apex domain | `{{domain_root}}` (`kitehub.me`) | Test env may use subdomain; staging may use different domain |
| Email addresses | `{{support_email}}` (`support@kitehub.me`) | Multi-env split |
| Secret prefix | `{{secret_prefix}}` (`kitehub/production`) | Multi-env split |
| ECR URI | `{{ecr_uri}}` | Account ID + region embedded |
| IAM role ARN | `{{iam_deploy_role}}` | Account ID + role name multi-env |
| CloudTrail trail name | `{{cloudtrail_name}}` | Multi-env split |
| RDS db_name + db_user | `{{db_name}}`, `{{db_user}}` | Cross-env credential isolation |

### ❌ Do NOT use `{{var}}` for

| Value class | Example | Why |
|---|---|---|
| Single-env constants | `kitehub` (project name — all envs same) | No multi-env split needed |
| Code identifiers | Java class names, TypeScript imports | Not env-dependent |
| HTTP protocol tokens | `HTTP 201`, `JWT`, `POST /api/v1/...` | Standard lexicon, không thay đổi |
| Brand names | `KiteHub`, `KiteClass`, `AWS SES` | Proper nouns |
| CLI flags | `--dry-run`, `terraform apply` | Verbatim from tool |
| File paths | `documents/05-guides/operations/` | Repo-relative, không thay đổi |
| Version numbers | `Java 21`, `Spring Boot 3.2` | Tech stack constants |
| Quoted external sources | AWS docs quote | Verbatim source, render destroys meaning |

---

## 4. Examples (good vs bad)

### ✅ GOOD

```markdown
# Release 1 Deploy Plan

KiteHub production runs on AWS account `{{aws_account_id}}` in region `{{aws_region}}`.

Deploy via OIDC role: `{{iam_deploy_role}}`.

Verify CloudTrail post-apply:

\`\`\`bash
aws cloudtrail get-trail-status --name {{cloudtrail_name}} --query 'IsLogging'
# Expected: True
\`\`\`

Secrets fetched from prefix `{{secret_prefix}}/<service-name>`.
```

After `render-env-vars.sh production`:
- `906286017800` substituted for account
- `ap-southeast-1` substituted for region
- `arn:aws:iam::906286017800:role/kitehub-deploy-role` substituted for IAM
- `kitehub-main` substituted for CloudTrail
- `kitehub/production` substituted for secret prefix

After `render-env-vars.sh test`:
- Same template, different rendered values (test env account / region / IAM / CloudTrail / secret prefix)

### ❌ BAD

```markdown
# Release 1 Deploy Plan (anti-pattern)

KiteHub production runs on AWS account 906286017800 in region ap-southeast-1.
Deploy IAM: arn:aws:iam::906286017800:role/kitehub-deploy-role
```

Problems:
- Account ID hardcoded → multi-account split Phase 2 will require sweep
- Region hardcoded → Phase 3 region migration will require sweep
- IAM ARN embeds account ID twice → drift risk khi account swap
- No render path → cannot auto-generate test-env variant

### ❌ BAD — wrong variable name

```markdown
Account: {{accountId}}    <!-- camelCase, không match env-reference.yaml schema -->
Region: {{AWS_REGION}}    <!-- UPPER_CASE, không match -->
Domain: {{domain-root}}   <!-- kebab-case, không match -->
```

Problems: `render-env-vars.sh` won't substitute (name mismatch); `check-unresolved-env-vars.sh` may flag if pattern matches regex. Use `lowercase_snake_case` per §2.3.

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Tooling (active now)

| Script | Purpose | Trigger |
|---|---|---|
| `scripts/render-env-vars.sh` | Substitute `{{var}}` placeholders per env | Manual run before doc preview / CI render check |
| `scripts/check-unresolved-env-vars.sh` | Fail when rendered output contains unresolved `{{...}}` placeholder | CI job + manual pre-commit |
| `documents/02-architecture/env-reference.yaml` | Canonical source of env values per variable | Single source of truth |

### 5.2 CI gate (active — `script-quality.yml` job `env-vars-render`)

Workflow validates `_examples/env-reference-self-test.md` renders byte-identical với committed control file `_examples/env-reference-self-test-expected-production.md`. Fails if:
- Render output differs from control (env-reference.yaml drift not captured)
- Rendered output contains unresolved `{{...}}` placeholders

### 5.3 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/05-guides/deploy/**`, `documents/05-guides/operations/**`, `infrastructure/**`:

- [ ] New hardcoded env-specific value (account ID, region, domain, email, secret prefix, IAM ARN)?
- [ ] Nếu CÓ → use `{{var_name}}` syntax + variable in `env-reference.yaml`?
- [ ] Nếu chưa có variable trong yaml → add row trong cùng PR
- [ ] Rendered output passes `check-unresolved-env-vars.sh`?

### 5.4 CI grep detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày)

Future enhancement: pre-commit hook `.husky/check-hardcoded-env-values.sh`:

```bash
# Detect new hardcoded account ID / domain / secret prefix outside env-reference.yaml + 07-archived
grep -rnE "906286017800|kitehub\.me|kitehub/production" \
  documents/05-guides/ infrastructure/ scripts/ \
  --exclude-dir=07-archived \
  --exclude="env-reference.yaml" \
  2>/dev/null && {
    echo "WARN: hardcoded env-specific value detected — consider {{var}} per markdown-variable-reference.md §3";
    exit 0;
  }
```

WARN-mode initially per `incident-to-rule-pipeline.md` §3 advisory-rule guard (heuristic FP risk: legitimate references in audit reports, gap files, historical context). HARD STOP target Wave 103+ post-30-day grace period. Detector ship target: GAP-692 Phase 3.

### 5.5 Memory auto-load (deferred)

Memory entry `feedback_markdown_variable_reference.md` could remind tại session start trước khi touch deploy/runbook scope. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test §7 đủ cho v1.0.0.

---

## 6. Override mechanism

Genuine exception (vd historical audit reference, vendor-specific constant, regulator-required hardcoded value):

```
git commit -m "...
MARKDOWN_VAR_REF_OVERRIDE: <file path> — <reason — e.g., 'audit report 2026-05-08 historical reference must stay verbatim per immutable evidence convention'>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

Common valid override cases:
- `documents/04-quality/audits/**` historical evidence (immutable per `output-review-mandate.md` §3)
- `documents/07-archived/**` historical archive (immutable per `docs-folder-structure.md` archive policy)
- Regulator-required hardcoded value (VN PDPL specific account refs)
- Code source comments referencing prod ID (Java/TS scope handled separately by `production-env-config-registry.md`)

---

## 7. Worked self-test — render roundtrip on `_examples/env-reference-self-test.md`

**Pre-state:** `.claude/rules/_examples/env-reference-self-test.md` shipped same PR với 8 `{{var}}` placeholders (aws_account_id, aws_region, domain_root, support_email, secret_prefix x2, ecr_uri, iam_deploy_role, cloudtrail_name, db_name, db_user).

**Apply §2 syntax:**

1. **Render production env:**
   ```bash
   bash scripts/render-env-vars.sh production \
     documents/02-architecture/env-reference.yaml \
     .claude/rules/_examples/env-reference-self-test.md \
     /tmp/rendered.md
   ```
2. **Verify no unresolved:**
   ```bash
   bash scripts/check-unresolved-env-vars.sh /tmp/rendered.md
   # Expected: PASS: no unresolved {{...}} placeholders detected.
   ```
3. **Verify byte-identical vs committed control:**
   ```bash
   diff /tmp/rendered.md .claude/rules/_examples/env-reference-self-test-expected-production.md
   # Expected: zero diff
   ```

**Counterfactual without rule:** future env clone (test env spin-up) requires manual sweep + replace across all docs containing `906286017800` / `kitehub.me` / `kitehub/production` / `ap-southeast-1`. Mistake rate ≥5% per sweep (GAP-458 → GAP-459 evidence).

**With rule:** test env render = `bash scripts/render-env-vars.sh test ...` — zero manual sweep, zero drift risk.

**Verdict:** rule fires correctly on synthetic fixture. Self-test PASS ✅

---

## 8. Relationship to other rules

- **`production-env-config-registry.md`** v1.1.1 — sister rule (runtime `application*.yml` env-var scope); this rule extends to docs/scripts/terraform/markdown scope
- **`audit-to-gap-pipeline.md`** §2.7 Decision-Doc Code-Sync — rule mandate this rule operationalizes (decision-doc landing → grep sweep mandatory same PR)
- **`no-vercel-references.md`** v1.0.0 — sister pattern (decommission vendor sweep) using grep detector approach; this rule applies same detector pattern for env-specific values
- **`output-review-mandate.md`** §3 — adds row "Markdown variable reference" tracking review standard (future PR — defer per scope; this rule paired-rule-only enforcement)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test + rules-index.csv row + CI job all paired same PR
- **`incident-to-rule-pipeline.md`** — applied 5-stage: Detect ✓ (GAP-458 → GAP-459 + GAP-692 outside-in audit 2026-05-21) → Classify ✓ (existing `production-env-config-registry.md` covers runtime YAML scope only; docs/scripts/terraform broader scope uncovered) → Rule+Enforce ✓ (this file + tooling + CI job + self-test paired same PR) → Self-Test ✓ (§7 worked example on roundtrip render) → Retro Log ✓ (§9 below + GAP-692 Phase 1 flip OPEN → PARTIAL 33%)
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 syntax + tool → mọi future env-clone / domain rotation / account swap benefits)
- **`context-budget-mandate.md`** §3.1 — path-scoped frontmatter (`paths:`) chosen — rule relevant chỉ khi deploy/operations/infrastructure context
- **`dev-readable-doc-language.md`** §3-§4 — Vietnamese narrative + English identifier mixed pattern applies to rule body
- **GAP-692** Phase 1 (Wave 102.8 Bucket B 2026-05-21) — this rule = direct output

---

## 9. Log

- **2026-05-21 (v1.0.0):** Rule created. Triggered by GAP-692 Phase 1 (Wave 102.8 Bucket B 2026-05-21) outside-in audit synthesis 2026-05-21 — 3 parallel audit agents (A docs scope + B infra scope + C design pattern) surfaced 95 files × 5,500+ hardcoded env-specific occurrences cross docs/scripts/terraform. Existing `production-env-config-registry.md` v1.1.1 covers runtime `application*.yml` scope only (17/45 candidates indexed); broader scope uncovered → multi-env clone / account swap / domain rotation requires manual sweep + drift risk. GAP-458 → GAP-459 incident (2026-05-09) proves recurrence pattern: `kitehub.vn` → `kitehub.me` decision left 21 stale refs → AWS Activate Founder denied. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-692 outside-in audit) → Classify ✓ (no existing rule codifies markdown/docs/scripts variable syntax; `production-env-config-registry.md` sister scope only covers runtime YAML; `audit-to-gap-pipeline.md` §2.7 mandates sweep but doesn't provide mechanism) → Rule+Enforce ✓ (this file + `scripts/render-env-vars.sh` + `scripts/check-unresolved-env-vars.sh` + `documents/02-architecture/env-reference.yaml` canonical schema + `_examples/env-reference-self-test.md` fixture + CI job `env-vars-render` + TF mismatch fix `var.domain_name` STALE `kiteclass.com` → `kitehub.me` + add `var.aws_account_id` + `var.secrets_prefix` all paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example — render PASS + check-unresolved PASS + roundtrip byte-identical vs control file) → Retro Log ✓ (this entry + GAP-692 Phase 1 status flip OPEN 0% → PARTIAL 33%; Phase 2 opportunistic refactor top 10 high-leverage files + Phase 3 pre-commit hook defer Wave 103+). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered class "env-specific value parameterization in docs/scripts/terraform"; no constraint loosening; existing 5,500+ hardcoded values grandfathered per GAP-692 Phase 2 opportunistic refactor schedule; rule applies prospectively từ this PR forward). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: `{{var}}` substitution syntax) / ✅ unique (sister rule `production-env-config-registry.md` covers runtime YAML scope only — disjoint) / ✅ widely applicable (mọi PR touching deploy/operations/infrastructure docs) / ✅ body discipline (§1 ≤2 "and" conjunctions). Detector wiring (§5.4 CI grep + §5.5 memory auto-load) deferred per `incident-to-rule-pipeline.md` §3 premature-rule guard ≥7 ngày; v1.0.0 enforcement = tooling (render + check scripts) + CI job `env-vars-render` + reviewer-checklist + worked self-test sufficient.
