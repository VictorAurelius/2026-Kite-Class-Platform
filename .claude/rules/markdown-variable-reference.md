---
paths:
  - documents/**/*.md
  - documents/02-architecture/env-reference.yaml
  - scripts/render-env-vars.sh
  - scripts/check-unresolved-env-vars.sh
---

# Markdown Variable Reference — single source for env-specific values in docs

**Priority:** 🟠 MANDATORY — docs scalability + multi-env governance
**Version:** 1.0.0
**Created:** 2026-05-20
**Last-Reviewed:** 2026-05-20
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (CI validator + render script + self-test fixture + paired `documents/02-architecture/env-reference.yaml` canonical store + terraform variables.tf domain_name fix) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds previously-uncovered class for env-specific value references trong docs; Phase 1 ships tooling only, KHÔNG migrate existing docs)

**Applies to:** Mọi markdown doc dưới `documents/**` muốn reference env-specific values (domain, AWS account ID, support email, secret prefix, region, etc.). Source-of-truth = `documents/02-architecture/env-reference.yaml`. KHÔNG áp dụng cho code source (`*.ts`/`*.tsx`/`*.java`/`*.yml`/`*.tf`) — code dùng standard env-var loaders (Spring config, Next.js env, terraform vars).

---

## 1. The Rule

> **Khi markdown doc cần reference env-specific value (vd AWS account ID, apex domain, support email, secret prefix), tác giả dùng mkdocs-macros syntax `{{var_name}}` thay vì hardcode literal value. Source-of-truth = `documents/02-architecture/env-reference.yaml`. CI validator `scripts/check-unresolved-env-vars.sh` catch unresolved references trong rendered output.**

Mục tiêu: chuyển model "1 doc viết tay cho prod" → "1 doc source, render N flavor (prod/test/dev) qua tooling". Force-multiplier: rebuild AWS account hoặc swap domain → cập nhật 1 entry trong `env-reference.yaml` thay vì grep + edit hàng chục refs across docs.

Phase 1 scope (this PR / GAP-692): **tooling-only — ship rule + scripts + canonical YAML + self-test**. KHÔNG migrate existing docs. Phase 2+: opportunistic refactor khi docs touched. Phase 3+: pre-commit forbid new hardcoded literal khi `{{var}}` syntax có thể dùng.

---

## 2. Syntax + escape

### Substitution syntax

`{{var_name}}` — render script thay thế bằng giá trị từ `env-reference.yaml` cho env chỉ định.

```markdown
Production AWS account: `{{aws_account_id}}` (per `env-reference.yaml`).
Apex domain: https://{{domain_root}}/
```

Render với `prod`:

```markdown
Production AWS account: `906286017800` (per `env-reference.yaml`).
Apex domain: https://kitehub.me/
```

### Escape sequence

Backslash escape `\{{var_name}}` giữ literal (không substitute). Dùng khi cần document chính syntax:

```markdown
Doc fence ví dụ: `\{{aws_account_id}}` stays literal sau render.
```

Render output:

```markdown
Doc fence ví dụ: `{{aws_account_id}}` stays literal sau render.
```

(Note: rendered literal sau escape unwrap sẽ trigger validator FAIL — vì vậy escape pattern phù hợp cho rule body / template, KHÔNG cho doc consumer cuối-stream.)

### Unknown var behavior

Nếu `{{var_name}}` chưa khai báo trong `env-reference.yaml`:
- Render script: LEAVE AS-IS (no substitution)
- Validator: FAIL với hint "add var to env-reference.yaml hoặc escape literal as \\{{var}}"

---

## 3. Rendering

```bash
bash scripts/render-env-vars.sh <env> <source.md> [<output.md>]
```

- `env` ∈ `prod | test | dev` per `env-reference.yaml` `envs:` field
- `source.md` — markdown source chứa `{{var}}` references
- `output.md` — optional; default stdout

Ví dụ:

```bash
# Render prod flavor
bash scripts/render-env-vars.sh prod documents/02-architecture/sample-deploy-runbook.md /tmp/runbook-prod.md

# Render dev flavor cho local testing
bash scripts/render-env-vars.sh dev documents/02-architecture/sample-deploy-runbook.md /tmp/runbook-dev.md

# Render to stdout (preview)
bash scripts/render-env-vars.sh prod documents/02-architecture/sample-deploy-runbook.md
```

Tool dependencies: `yq` v4 (preferred) HOẶC Python 3 với PyYAML (fallback cho local dev convenience).

---

## 4. Validation

```bash
bash scripts/check-unresolved-env-vars.sh <path>
```

- `path` — file hoặc directory (recursive `*.md`)
- Exit 0: scanned clean (zero unresolved `{{var}}` references)
- Exit 1: ≥1 unresolved reference found; CI fails

Mục đích: gate post-render output (vd `build/rendered-docs/`) đảm bảo không có `{{var}}` literal leak vào published doc. CI job `env-vars-render-validator` (`.github/workflows/script-quality.yml`) tự render self-test fixture + validate output.

Validator KHÔNG scan source docs trực tiếp — source được phép chứa `{{var}}` (đó là design). Validator scan rendered output OR docs đã được migrate sang `{{var}}` syntax + đã render.

---

## 5. Migration policy

### Phase 1 — Tooling only (this PR — GAP-692)

- Ship rule + 2 scripts (`render-env-vars.sh` + `check-unresolved-env-vars.sh`) + `env-reference.yaml` + self-test fixture
- Fix terraform `variables.tf` domain_name default `kiteclass.com` → `kitehub.me` (canonical drift)
- Add new variables `aws_account_id` + `secrets_prefix` (alignment với YAML schema)
- **KHÔNG migrate existing docs** — Phase 2 future scope

### Phase 2 — Opportunistic refactor

- Khi PR sửa doc đã có hardcoded env-specific value → refactor sang `{{var}}` syntax cùng PR
- Reviewer-prompted, không mass-migrate

### Phase 3 — Pre-commit enforcement

- Pre-commit hook scan PR diff cho new hardcoded literal pattern match `env-reference.yaml` value (vd `906286017800`, `kitehub.me`, `support@kitehub.me`) trong file `documents/**/*.md`
- WARN initially, BLOCK sau khi >80% existing docs migrated

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Hardcode `https://kitehub.me/api` trong runbook docs | `https://{{domain_root}}/api` + render per-env |
| Hardcode AWS account ID `906286017800` trong markdown | `{{aws_account_id}}` reference |
| Tạo `runbook-prod.md` + `runbook-test.md` + `runbook-dev.md` tách rời | 1 source `runbook.md` + render 3 flavors |
| Skip canonical YAML — invent ad-hoc syntax `${{ ENV.account_id }}` | Stick to mkdocs-macros `{{var_name}}` + `env-reference.yaml` |
| Migrate existing docs cùng PR ship rule | Phase 1 = tooling only; Phase 2 = opportunistic |
| Edit `env-reference.yaml` mà không bump schema_version khi breaking change | Update schema_version field + document migration in commit body |
| Reference internal config keys (`kite.trial.duration-days`) via `{{var}}` | This rule scope = env-specific values only (domain/account/email/etc.); config keys stay code-canonical |

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/**/*.md`:

- [ ] New doc references env-specific value? → use `{{var_name}}` syntax + verify var exists in `env-reference.yaml`
- [ ] Doc updated với new env value? → update `env-reference.yaml` cùng PR
- [ ] Existing hardcoded literal touched? → opportunistic Phase 2 refactor encouraged (not required)

### 7.2 CI validator (active — paired same PR)

`.github/workflows/script-quality.yml` job `env-vars-render-validator`:
1. Render self-test fixture `.claude/rules/_examples/env-reference-self-test.md` với env=prod
2. Run `check-unresolved-env-vars.sh` trên rendered output
3. Exit 1 nếu unresolved found

Job runs trên mọi PR touching `scripts/render-env-vars.sh`, `scripts/check-unresolved-env-vars.sh`, `documents/02-architecture/env-reference.yaml`, hoặc rule itself.

### 7.3 Pre-commit hook (deferred Phase 3)

Future: scan PR diff cho new hardcoded literal matching `env-reference.yaml` values trong `documents/**/*.md`. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày — wait for Phase 2 opportunistic refactor coverage ≥80% trước khi enforce.

### 7.4 Override mechanism

Genuine exception (vd doc captures historical state với explicit literal cho audit/compliance):

```
git commit -m "...
MARKDOWN_VAR_REFERENCE_OVERRIDE: <file path> — <reason — e.g., 'audit doc captures literal account-ID at point-of-audit for forensic trail'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review.

---

## 8. Self-test

Apply rule to `.claude/rules/_examples/env-reference-self-test.md`:

```bash
# Step 1: Render prod flavor
bash scripts/render-env-vars.sh prod .claude/rules/_examples/env-reference-self-test.md /tmp/rendered-prod.md

# Step 2: Validate no unresolved references in rendered output
bash scripts/check-unresolved-env-vars.sh /tmp/rendered-prod.md
# Expected: PASS: scanned 1 file(s); no unresolved env-var references.

# Step 3: Spot-check substitutions
grep "906286017800\|kitehub.me\|kitehub/production" /tmp/rendered-prod.md
# Expected: lines with substituted values
```

**Expected output (verified 2026-05-20):**

```
Rendered: .claude/rules/_examples/env-reference-self-test.md → /tmp/rendered-prod.md (env=prod)
PASS: scanned 1 file(s); no unresolved env-var references.
```

**Counterfactual without rule:**
- Each doc cần env-specific value hardcode trực tiếp → mọi rebuild AWS account / domain swap = grep + edit N files manually
- Drift example: terraform `variables.tf` default `domain_name = "kiteclass.com"` (stale from pre-Wave-43 branding decision) vs canonical `kitehub.me` per ADR-027 — fixed same PR.

**Self-test verdict:** rule fires correctly + render + validator round-trip clean. ✅

---

## 9. Relationship to other rules

- **`production-env-config-registry.md`** v1.1.0 — sister rule covering runtime YAML config registry (Spring `application*.yml`, Helm values, terraform vars). This rule covers **markdown doc** scope; sister covers **runtime config** scope. No overlap — distinct atomic responsibilities per `rule-change-process.md` §5.1 atomic-unique bar.
- **`audit-to-gap-pipeline.md`** §2.7 Decision-Doc Code-Sync — when env-reference.yaml changes, code-sync mandate triggers (grep stale refs trong code + infra + scripts). Rule này extends sync surface tới `documents/**/*.md`.
- **`meta-csv-index-pattern.md`** §3 — `rules-index.csv` adds row cho rule này (paired same PR).
- **`output-review-mandate.md`** §3 — review standard matrix; future row "Markdown env-var references" deferred (rule mới + Phase 1 tooling-only; revisit Phase 2 stable).
- **`docs-folder-structure.md`** §3 — folder convention; env-reference.yaml lives under `documents/02-architecture/` (architecture-scoped canonical).
- **`docs-only-pr-auto-merge.md`** — env-reference.yaml + scripts/.sh files NOT trong auto-merge scope (scripts are executable); manual confirmation flow.
- **`dev-readable-doc-language.md`** §2 — Vietnamese narrative + English identifier per convention; rule body theo split này.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + CI validator + self-test fixture + canonical YAML + scripts + terraform fix all ship same PR.
- **`incident-to-rule-pipeline.md`** — rule này direct output GAP-692 Phase 1 ship request applied through 5-stage pipeline: Detect ✓ (user-flagged multi-env refactor need) → Classify ✓ (no existing rule codifies markdown env-var reference convention) → Rule+Enforce ✓ (this file + 2 scripts + canonical YAML + self-test + CI wire) → Self-Test ✓ (§8 worked example) → Retro Log ✓ (§10 below).
- **`gap-architecture-v2.md`** — CSV canonical for gap status; GAP-692 status PARTIAL post-Phase-1 ship.

---

## 10. Log

- **2026-05-20 (v1.0.0):** Rule created Phase 1 of GAP-692 (env-reference YAML multi-env refactor). Triggered by user direction 2026-05-21 "ship tooling chuẩn bị multi-env refactor — Phase 1 cost ~1 dev-day; force-multiplier kicks in từ Phase 2". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (planned multi-env support emergence) → Classify ✓ (no existing rule codifies markdown env-var reference convention; `production-env-config-registry.md` covers runtime YAML scope only — distinct atomic scope per `rule-change-process.md` §5.1 atomic-unique bar) → Rule+Enforce ✓ (this file + `documents/02-architecture/env-reference.yaml` canonical store + `scripts/render-env-vars.sh` + `scripts/check-unresolved-env-vars.sh` validator + `.claude/rules/_examples/env-reference-self-test.md` fixture + `.github/workflows/script-quality.yml` job `env-vars-render-validator` + terraform `variables.tf` domain_name fix `kiteclass.com` → `kitehub.me` + new vars `aws_account_id` + `secrets_prefix` paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§8 worked example trên self-test fixture — render prod flavor + validator clean exit 0) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered class for markdown env-var references; no constraint loosening; existing hardcoded literal in docs grandfathered per §5 Phase 1 tooling-only policy; rule applies prospectively cho new docs OR opportunistic refactor Phase 2). Path-scoped per `context-budget-mandate.md` §3.1 — `paths:` frontmatter deferred-load chỉ khi PR touch matching files. Pre-commit hook Phase 3 enforcement deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày + wait for Phase 2 opportunistic refactor coverage ≥80% trước khi BLOCK.
