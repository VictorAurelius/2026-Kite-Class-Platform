---
title: Credential Rotation Runbook
status: active
created: 2026-05-14
last-reviewed: 2026-05-14
scope: KiteHub production credentials (admin passwords, JWT secrets, 3rd-party API tokens)
owner: solo-dev (acting platform-admin)
---

# Credential Rotation Runbook

General-purpose procedure for rotating production credentials managed by KiteHub. Per-incident artifacts (one rotation event = one file) live under [`documents/04-quality/audits/credential-rotation/`](../../04-quality/audits/credential-rotation/).

Related runbooks:
- [`jwt-rotation-runbook.md`](./jwt-rotation-runbook.md) — JWT-specific dual-key procedure (if shipped per GAP-520)
- [`secrets-rotation-runbook.md`](./secrets-rotation-runbook.md) — AWS Secrets Manager rotation cadence
- [`secrets-seeding-runbook.md`](../deploy/secrets-seeding-runbook.md) — initial production seeding (one-time)
- [`incident-response-runbook.md`](./incident-response-runbook.md) — incident comms + escalation patterns

---

## 1. When to rotate

| Trigger | Cadence | Priority |
|---|---|---|
| **Scheduled (quarterly)** | Every 90 days for production credentials | P2 — planned |
| **Leak detected** (transcript, log, screenshot, email, chat, public commit) | Immediately upon detection (≤24h) | P0 — emergency |
| **Personnel change** (ex-contractor, role removal) | Within 24h of personnel departure / role flip | P0 — emergency |
| **Audit trigger** (compliance review, security audit finding) | Per audit timeline | P1 — scheduled |
| **Vendor compromise** (vendor announces breach affecting issued tokens) | ≤24h after vendor disclosure | P0 — emergency |
| **Service decommission** (3rd-party service retired) | Revoke before vendor relationship ends | P1 |

**Solo-dev mode note:** In solo-dev mode, the "leak detected" trigger is the most common path — credentials surfacing in Claude Code session transcripts during operational work (Option B "paste in chat" patterns). Apply this runbook as soon as the work that needed the credential lands.

---

## 2. Credential class → rotation procedure

### 2.1 Admin password (PLATFORM_ADMIN seed account)

**Storage:** AWS Secrets Manager secret `kitehub/production/admin-seed-password` (or equivalent per `secrets-seeding-runbook.md`).

**Procedure:**

1. **Pre-rotation snapshot.** Note current secret ARN + version-id (`aws secretsmanager describe-secret --secret-id kitehub/production/admin-seed-password --query 'VersionIdsToStages'`).
2. **Generate new value.** Two options:
   - **Option A — Terraform re-roll** (preferred when seed value is `random_password` resource):
     ```bash
     cd infrastructure/terraform-aws
     terraform apply -target=random_password.seed_admin_password -replace=random_password.seed_admin_password
     terraform apply -target=aws_secretsmanager_secret_version.admin_seed_password
     ```
     This regenerates the random_password + bumps the secret version atomically.
   - **Option B — Manual put-secret-value** (when not Terraform-managed or emergency rotation outside business hours):
     ```bash
     NEW_PASSWORD=$(openssl rand -base64 24)  # never echo or log
     aws secretsmanager put-secret-value \
       --secret-id kitehub/production/admin-seed-password \
       --secret-string "$NEW_PASSWORD"
     unset NEW_PASSWORD
     ```
3. **Re-seed admin account.** Run `scripts/seed-direct-sql.sh` (or equivalent admin re-seeding script) which reads from Secrets Manager + updates the users table:
   ```bash
   bash scripts/seed-direct-sql.sh --target-env=production --user=admin@kitehub.me
   ```
4. **Verify login.** Sign in to `/login` with the new password. Expect successful login + redirect to admin dashboard.
5. **Verify old password rejected.** Attempt login with old password (if you remember the literal). Expect 401.
6. **File audit artifact.** See §5 below.

**Recovery if rotation fails:** previous secret version remains in AWS Secrets Manager (default 30-day retention). Use `aws secretsmanager update-secret-version-stage` to revert AWSCURRENT stage to previous version-id.

---

### 2.2 JWT signing secret

**Storage:** AWS Secrets Manager secret `kitehub/production/jwt-signing-key` (or `jwt-current` + `jwt-previous` slots per dual-key strategy in GAP-520).

**If `jwt-rotation-runbook.md` exists** (per GAP-520 Bucket B), follow that runbook — it implements the dual-key zero-downtime rotation. The procedure below is the inline fallback for the case where the dedicated runbook has not yet shipped.

**Inline procedure (until dedicated runbook lands):**

1. **Generate new signing key.** Recommended: RS256 keypair OR a 256-bit secret for HS256:
   ```bash
   # HS256 path:
   NEW_JWT_SECRET=$(openssl rand -base64 64)
   # RS256 path: generate a new RSA keypair under /tmp and upload public key to vendor
   ```
2. **Stage new key as `jwt-previous` first** (if dual-key slots exist):
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id kitehub/production/jwt-signing-key-previous \
     --secret-string "$NEW_JWT_SECRET"
   ```
3. **Deploy services with both keys honored** (current + previous) — `kitehub-subscription` AuthService should verify against both during the cutover window equal to refresh-token TTL (default 7 days).
4. **Promote new key to `jwt-current` slot** after deploy + smoke test:
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id kitehub/production/jwt-signing-key \
     --secret-string "$NEW_JWT_SECRET"
   unset NEW_JWT_SECRET
   ```
5. **Decommission old key** after refresh-token TTL window expires. Update `jwt-previous` to a placeholder so re-use is detected as `force logout all sessions` per `pre-launch-auth-hardening-checklist.md` §2.8.
6. **Verify token signing.** Issue a fresh login + inspect JWT header `kid` claim matches the new key id.

**Recovery:** if signing key rotation fails mid-deploy, revert services to the previous deploy SHA (rollback workflow per `release-deploy-standard.md` §4.4) — old key still in Secrets Manager.

---

### 2.3 3rd-party API tokens (generic flow)

This is the most common rotation class (Cloudflare, Resend, Stripe, Twilio, etc.). All follow the same shape:

**Procedure template:**

1. **Identify consumers.** `grep -r "<provider>_api_key\|<PROVIDER>_TOKEN" kitehub/ kiteclass/ scripts/ infrastructure/ documents/ --include="*.java" --include="*.yml" --include="*.yaml" --include="*.sh" --include="*.tf"` to list every service that reads this credential.
2. **Vendor portal — generate new token.** Log into the vendor's dashboard (Cloudflare API Tokens / Resend API Keys / Stripe Dashboard → API Keys / etc.). Generate a new token with the SAME scope as the existing token (read-only, sending access, etc.). Do NOT delete the old token yet.
3. **Store new token in AWS Secrets Manager:**
   ```bash
   aws secretsmanager put-secret-value \
     --secret-id kitehub/production/<provider>-api-key \
     --secret-string "$NEW_TOKEN"
   unset NEW_TOKEN
   ```
4. **Redeploy services consuming the credential** so they pull the new value from Secrets Manager. For the KiteHub stack, this is typically the rollback workflow's forward variant — `gh workflow run deploy-production.yml -f confirm=APPLY` (or equivalent per `release-deploy-standard.md` §4.2).
5. **Smoke test functionality.** Exercise a code path that uses the credential. Examples:
   - Cloudflare: `curl -H "Authorization: Bearer $TOKEN" https://api.cloudflare.com/client/v4/user/tokens/verify` — expect `200 OK`.
   - Resend: send a test transactional email via `EmailService` to a project-internal address; verify Resend dashboard shows `delivered`.
   - Stripe: trigger a test webhook or a test-mode charge; verify Stripe dashboard.
6. **Revoke old token at vendor.** Once smoke test passes, return to the vendor portal + revoke the old token. This is the irreversible step — do not skip.
7. **Verify old token rejected.** Curl with the old token; expect `401`.
8. **File audit artifact** per §5.

**Recovery:** if smoke test fails after step 4, revert by re-deploying the previous service SHA (which still has the old token cached) AND keep the old token alive at vendor until you can debug. Do NOT revoke the old token until smoke test passes.

#### 2.3.1 Cloudflare API token (specific example)

- **Vendor portal:** https://dash.cloudflare.com/profile/api-tokens
- **Common scopes:** "Read all resources" (audit), "Zone:DNS:Edit" (DNS modification), "Account:Workers:Edit" (Workers deploy)
- **AWS Secrets Manager secret-id:** `kitehub/production/cloudflare-api-token` (or per-scope variant)
- **Consumers:** DNS audit scripts under `scripts/`, Workers deploy CI workflows, Cloudflare MCP server config
- **Smoke test:** `curl -sH "Authorization: Bearer $TOKEN" https://api.cloudflare.com/client/v4/user/tokens/verify | jq '.success'` → expect `true`

#### 2.3.2 Resend API key (specific example)

- **Vendor portal:** https://resend.com/api-keys
- **Common scope:** "Sending access" restricted to `kitehub.me` domain
- **AWS Secrets Manager secret-id:** `kitehub/production/resend-api-key`
- **Consumers:** `kitehub-email` service `ResendClient`; transactional email path in `EmailService`
- **Smoke test:** send transactional email to `dev@kitehub.me` via internal email-test endpoint; verify Resend dashboard `Logs → delivered`
- **Redeploy required:** yes — `kitehub-email` reads the key at startup; container restart picks up new value (`docker compose restart kitehub-email` if SSH access, OR re-deploy workflow)

#### 2.3.3 Stripe API key (Phase 1.5+)

- **Vendor portal:** https://dashboard.stripe.com/apikeys
- **Common scopes:** restricted publishable key (FE) + restricted secret key (BE)
- **AWS Secrets Manager secret-id:** `kitehub/production/stripe-secret-key`, `kitehub/production/stripe-publishable-key`
- **Consumers:** payment processor (Phase 1.5 BLOCKING per GAP-228); webhook handlers
- **Smoke test:** create a test-mode PaymentIntent via API; verify Stripe dashboard log entry
- **Notes:** Stripe rotation requires updating BOTH FE (publishable) + BE (secret) keys together; FE rebuild needed because publishable key is embedded at build time

---

## 3. Pre-rotation checklist

Before initiating ANY rotation, confirm:

- [ ] **All consumers identified.** Run the `grep` from §2.3 step 1 — every service that reads the credential is named.
- [ ] **Downtime window estimated.** For zero-downtime rotations (dual-slot or version-bump approach), expected downtime is 0. For force-restart rotations, estimate restart time × number of services.
- [ ] **Rollback path noted.** Old credential value (or rollback-deploy SHA) recoverable for at least the duration of the rotation + smoke window.
- [ ] **Maintenance window communicated** (if downtime > 1 min). Status page entry + tenant notification per `incident-response-runbook.md`.
- [ ] **AWS Secrets Manager version retention confirmed.** Default 30-day previous-version retention is on; emergency revert is possible.
- [ ] **Audit artifact file path drafted** under `documents/04-quality/audits/credential-rotation/YYYY-MM-DD-<topic>.md`.

---

## 4. Verification post-rotation

After EVERY rotation:

| Check | Pass criterion |
|---|---|
| (a) New credential works | Smoke test per credential class succeeds |
| (b) Old credential rejected | Same smoke test with old value returns 401/403/access-denied |
| (c) No service errored on restart | Logs of every consumer service for 5 min post-rotation show zero auth failures |
| (d) Audit artifact filed | File exists under `documents/04-quality/audits/credential-rotation/`; status: `complete`; verification commands + outputs documented |
| (e) Memory entry created (if leak-driven) | Memory entry under `~/.claude/projects/.../memory/feedback_credential_leak_<topic>.md` describes how the leak surfaced + which rule now prevents it (per `incident-to-rule-pipeline.md` Stage 5) |
| (f) Follow-up gap filed (if pattern detected) | If rotation is the 2nd+ for the same credential class in a quarter, file a follow-up gap to investigate root cause |

---

## 5. Audit-log entry

**Mandatory** for every rotation regardless of trigger. File location:

```
documents/04-quality/audits/credential-rotation/YYYY-MM-DD-<scope-or-incident-id>.md
```

Examples:
- `2026-05-14-wave-72a-3-credentials.md` (incident-driven, per Wave 72a)
- `2026-08-15-quarterly-rotation-q3.md` (scheduled cadence)
- `2026-06-02-cloudflare-vendor-breach.md` (vendor-triggered)

**Required sections** (template):

```markdown
---
title: Credential Rotation — <scope>
status: pending | in-progress | complete
created: YYYY-MM-DD
trigger: scheduled | leak | personnel | audit | vendor
related-gaps: [GAP-XXX]
---

# Credential Rotation Audit — <scope>

## Scope
<Which credentials, why now>

## Credentials (no secret values — IDs only)
| # | Credential class | Secret reference / vendor | Scope | Sensitivity |
|---|---|---|---|---|

## Pre-rotation checklist
<§3 checklist results>

## Rotation status
| # | Credential | Status | Rotated_at | Rotated_by | New secret reference |
|---|---|---|---|---|---|

## Verification steps
<§4 verification results per credential>

## Notes
<Anomalies, follow-ups, links to incident-response-runbook entries>

## Next steps
<Pending user-action items, scheduled follow-ups>
```

---

## 6. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Echo the new credential to stdout or chat | Pipe into `aws secretsmanager put-secret-value --secret-string`; `unset` immediately |
| Skip vendor revocation step "to be safe" | Vendor revocation IS the rotation — otherwise old credential remains valid |
| Delete old AWS Secret version before smoke test passes | AWS auto-retains previous version 30 days — keep it until verified |
| Rotate during peak traffic without comms | Always coordinate with `incident-response-runbook.md` status-page pattern |
| File audit artifact post-hoc | Draft the artifact file BEFORE rotation; fill in as you go |
| Use `aws secretsmanager get-secret-value` to "verify it landed" | Use `describe-secret --query 'VersionIdsToStages'` (metadata only, no value reveal) |

---

## 7. Relationship to other rules / runbooks

- **`.claude/rules/agent-aws-access.md`** §2.2 — banned reads (Tier 2 `get-secret-value` requires explicit user-confirm per call). This runbook follows the discipline: rotation procedures use `put-secret-value` + `describe-secret`, never `get-secret-value` to verify.
- **`.claude/rules/pre-handoff-self-test-completeness.md`** §2.4 — admin-flow verification mandates credential availability + role-guard checks. Rotation completion includes the admin-login flow verify per §4 row (a)/(b).
- **`.claude/rules/release-fix-retry-budget.md`** §3 — if rotation requires 3+ retries, STOP and redesign per pivot matrix.
- **`secrets-rotation-runbook.md`** — AWS Secrets Manager cadence-driven rotation (this runbook is the credential-class-driven counterpart).
- **`incident-response-runbook.md`** — comms patterns for emergency rotations (leak / vendor breach / personnel change).
- **`jwt-rotation-runbook.md`** (per GAP-520 if shipped) — JWT-specific dual-key procedure.
- **`secrets-seeding-runbook.md`** (under `deploy/`) — initial production seeding (one-time setup, this runbook is the recurring counterpart).

---

## 8. Log

- **2026-05-14:** Runbook created (Wave 72a Bucket D, GAP-525). Covers 3 credential classes: admin password, JWT secret (with cross-link to GAP-520 dedicated runbook when available), 3rd-party API tokens (Cloudflare, Resend, Stripe). Paired with incident artifact `documents/04-quality/audits/credential-rotation/2026-05-14-wave-72a-3-credentials.md` for the 3 credentials surfaced in 2026-05-13 session transcript per GAP-525.
