# Production Env Config Registry — single source of truth + coverage audit

**Priority:** 🟠 MANDATORY — production config governance
**Version:** 1.0.0
**Created:** 2026-05-13
**Last-Reviewed:** 2026-05-13
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement (registry doc + scan script + CI gate deferred + worked self-test on 2026-05-13 Plan 1 incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — adds coverage guard for previously-uncovered class)
**Applies to:** Every `application*.yml` Spring config + every `docker-compose*.yml` production env block + `scripts/fetch-secrets.sh` + AWS Secrets Manager production secret entries

---

## 1. The Rule

> **Every `${VAR:<default>}` env-var reference in production code whose default would not function in production (localhost, mock, noreply@localhost, dev-only host names) MUST be overridden in production AND tracked in a centralized registry.**

Multi-source production config (YAML defaults, compose env, /etc/kite/.env, GitHub Secrets, AWS Secrets Manager) without a single source of truth = silent gaps. Plan 1 self-test 2026-05-13 surfaced 6 P0 production-blocking missing overrides — CORS, VERIFICATION_BASE_URL, EMAIL_PROVIDER, RESEND_API_KEY, AWS_SES_FROM_EMAIL, CDN_DOMAIN. None caught by existing audit skills.

This rule mandates the registry + coverage check.

---

## 2. Suspect-default pattern

A `${VAR:default}` is "suspect" if its default contains:
- `localhost` (dev host)
- `mock` (no-op provider)
- `noreply@localhost` / `noreply@kitehub.local` (invalid sender)
- `kite-mailhog` / `kitehub-frontend` / `kiteclass-frontend` (dev docker hostnames)
- Any IP `127.0.0.1` or `0.0.0.0`

Such defaults are dev-only conveniences; production MUST override.

---

## 3. Required artifacts

### 3.1 Registry doc

`documents/02-architecture/env-vars-registry.md` — table listing:
- Service (yaml file path)
- Var name
- Yaml line + suspect default
- Production override mechanism (compose / fetch-secrets / GH Secret / ACCEPTED)
- Required state (yes/no)
- Notes

Updated SAME PR as any new `${VAR:default}` introduction in source code.

### 3.2 Audit script

`scripts/audit-env-coverage.sh` — scans `application*.yml` for suspect defaults, cross-checks `docker-compose.production.yml` + `fetch-secrets.sh` overrides. FAIL on missing.

### 3.3 CI gate (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

`.github/workflows/script-quality.yml` job `env-coverage` runs `audit-env-coverage.sh` on PRs touching `application*.yml`. Track wire as follow-up gap when registry stabilizes.

---

## 4. Override mechanisms (in priority order)

| # | Mechanism | When | Pros | Cons |
|---|---|---|---|---|
| 1 | `docker-compose.production.yml environment:` | Public config (URLs, feature flags, log levels) | Visible, version-controlled, no secrets | Not for secrets |
| 2 | `fetch-secrets.sh` → `/etc/kite/.env` | Secrets (DB pass, API keys, tokens) | Pull from AWS Secrets Manager runtime | Requires populate-secrets.sh + IAM perms |
| 3 | Spring `application-prod.yml` | Profile-specific defaults | Code-controlled, profile-aware | One per service, doesn't survive cross-service drift |
| 4 | EC2 instance profile env (rare) | Cloud-specific | Auto-injected | Limited use cases |

**Banned:** hardcoding production values in `application.yml` defaults (the `${VAR:hardcoded-prod}` anti-pattern — defeats env-var mechanism).

---

## 5. Acceptable-default exceptions

A suspect default MAY remain UN-overridden if:
1. **Feature deferred to future phase** — e.g., `AI_OLLAMA_BASE_URL` (Phase 2 per ADR-026), `PAYMENT_*` (Phase 1.5)
2. **Mechanism not used** — e.g., `SMTP_HOST` since Resend HTTP API used (per ADR-025)
3. **Observability not provisioned** — e.g., `OTEL_EXPORTER_OTLP_ENDPOINT` (no collector deployed)

All exceptions listed in:
- `scripts/audit-env-coverage.sh` `ACCEPTABLE_DEFAULTS` array
- `env-vars-registry.md` rows marked `ACCEPTED-default`

Adding to acceptable list requires same-PR registry update with rationale.

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Add `${VAR:localhost}` to new code without checking registry | Update registry SAME PR; pick override mechanism |
| Set production value directly in `application.yml` default | Keep dev-friendly default; override via §4 mechanism |
| Trust `fetch-secrets.sh` covers all production vars | fetch-secrets.sh writes specific vars only; check coverage |
| Add to ACCEPTABLE_DEFAULTS without rationale | Document why default is OK for production phase |
| Skip pre-launch coverage check "because deploy worked" | Working deploy ≠ all flows tested; run scan + manual flow |

---

## 7. Enforcement

### 7.1 Scan script (active now)

`bash scripts/audit-env-coverage.sh` — runs in developer environment OR CI. FAILs on missing overrides.

### 7.2 Reviewer-checklist

PR review for any diff touching `application*.yml`:
- New `${VAR:default}` added? → registry updated?
- Suspect default? → override or ACCEPTED?
- Scan output PASS?

### 7.3 Pre-deploy audit

Before production release tag (`v0.9.0-beta-staging.N`):
```bash
bash scripts/audit-env-coverage.sh
```
Required PASS. If FAIL → either add override OR accept-list with rationale.

### 7.4 Live production verification

Per `feedback_audit_of_trust_pass.md` — pre-launch live probe MUST include `docker exec <service> env | grep <vars>` to verify ACTUAL production state matches registry expectations. AC `[x]` ≠ production-verified.

### 7.5 Override mechanism (rare exception)

Genuine emergency (regulator deadline, P0 incident) where audit blocks merge:

```
git commit -m "...
PROD_ENV_REGISTRY_OVERRIDE: <reason — link to follow-up gap completing registry>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 8. Self-test (worked example — 2026-05-13 Plan 1 self-test incident)

**Scenario:** User executed Plan 1 self-test, POST `/api/v1/auth/request-beta-access` → CORS preflight 403. Inspection revealed:

| Var | Default | Production state | Impact |
|---|---|---|---|
| `CORS_ALLOWED_ORIGINS` | localhost only | NOT OVERRIDDEN | CORS 403 blocks all browser POST |
| `VERIFICATION_BASE_URL` | http://localhost:3001 | NOT OVERRIDDEN | Email verify links dead |
| `EMAIL_PROVIDER` | mock | NOT OVERRIDDEN | Emails never delivered |
| `RESEND_API_KEY` | (from GH Secret) | EMPTY | Even if provider fixed, no key |
| `AWS_SES_FROM_EMAIL` | noreply@localhost | NOT OVERRIDDEN | Invalid sender |
| `CDN_DOMAIN` | localhost:9100 | NOT OVERRIDDEN | Asset URLs broken |

**Apply §7.1 scan retroactively:**
```bash
bash scripts/audit-env-coverage.sh
# → FAIL: 6 production env overrides missing
```

→ Rule fires correctly. Counterfactual: scan run pre-launch would have caught all 6 → would block release → Plan 1 self-test would not have hit CORS surprise.

**Verdict:** Rule fires correctly on the originating incident. Self-test PASS ✅.

---

## 9. Relationship to other rules

- **`output-review-mandate.md`** §3 — Ops Readiness row; this rule extends with per-var env coverage check
- **`release-deploy-standard.md`** §3.1 — Secrets management; this rule covers public-config alongside secrets
- **`feedback_audit_of_trust_pass.md`** — recurrence #5 of "AC `[x]` ≠ production-verified" pattern; this rule mandates pre-launch live probe
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-13 Plan 1 CORS incident applied through 5-stage pipeline
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — this rule + registry doc + scan script + worked self-test all ship same PR
- **`audit-to-gap-pipeline.md`** §2.6 wave-plan state-check — this rule extends state-check to env coverage scope
- **`gap-architecture-v2.md`** — GAP-508 (this rule's parent meta gap) tracked in CSV canonical

---

## 10. Log

- **2026-05-13 (v1.0.0):** Rule created. Triggered by Plan 1 self-test CORS 403 incident — surface of 6 P0 production config gaps not caught by any audit skill. Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged CORS in browser console + 5 other gaps in same audit) → Classify ✓ (no rule covers env-var production coverage) → Rule+Enforce ✓ (this file + registry doc + scan script + 3 P0 env overrides in compose paired same-PR per `rule-change-process.md` §6.5) → Self-Test ✓ (§8 retro on 2026-05-13 incident — scan correctly identifies all 6 gaps) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening; existing services grandfathered until registry + scan PASS state achieved). CI gate deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 days; v1.0.0 enforcement = scan script + reviewer-checklist sufficient.
