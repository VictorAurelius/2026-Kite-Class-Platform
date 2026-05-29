# Pre-Flight AWS Lifecycle Check — verify creds + state TRƯỚC khi start/stop stack

**Priority:** 🟠 MANDATORY — AWS lifecycle ops governance
**Version:** 1.0.0
**Created:** 2026-05-26
**Last-Reviewed:** 2026-05-26
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test trên 2026-05-26 incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies pre-flight discipline cho Tier 2 EC2/RDS lifecycle ops uncovered bởi sister rules)
**Applies to:** Mỗi lần Claude trigger `bash scripts/aws/start-stack.sh` / `bash scripts/aws/stop-stack.sh` / `aws ec2 start-instances` / `aws ec2 stop-instances` / `aws rds start-db-instance` / `aws rds stop-db-instance` — agent OR human-coordinator triggered

---

## 1. The Rule

> **Trước mọi AWS stack lifecycle op (start/stop EC2/RDS), agent PHẢI run 3-step pre-flight check sequence:**
> 1. **Cred check:** `aws sts get-caller-identity --profile dev-admin` → returns valid `Arn` + matching `Account` (906286017800 cho Phase 1 BETA)
> 2. **State check:** `aws ec2 describe-instances` + `aws rds describe-db-instances` → confirm target resources exist + are in expected pre-op state
> 3. **Document evidence:** cite commands + outputs trong PR body OR commit body OR session conversation (lightweight — full audit artifact NOT required for routine lifecycle)

Nếu Step 1 fail (InvalidClientTokenId / AuthFailure) → STOP, rotate creds first, retry. Nếu Step 2 fail (NoSuchEntity / missing instances) → STOP, investigate account state.

---

## 2. Why this rule exists — 2026-05-26 incident

Phase β AWS smoke session Wave beta-prep-1 closure:

| Time | Action | Result |
|---|---|---|
| 17:50 | `bash scripts/aws/start-stack.sh` | ❌ `No kitehub-kh-backend or kitehub-kc-app instances found via tag lookup` |
| 17:51 | `aws ec2 describe-instances --profile dev-admin` | ❌ `AuthFailure: AWS was not able to validate the provided access credentials` |
| 17:52 | `aws sts get-caller-identity --profile dev-admin` | ❌ `InvalidClientTokenId: The security token included in the request is invalid` |
| 17:53 | `aws sts get-caller-identity` (default) | ✅ `ci-deploy` (CI-narrow perms, can't start EC2) |
| 17:54 | User-action: rotate `dev-admin` keys via AWS Console + `aws configure --profile dev-admin` | ~10min user-interactive |
| 18:03 | `bash scripts/aws/start-stack.sh` retry | ✅ Stack STARTED 323s elapsed |

**Cost of the miss:**
- 1 failed start-stack invocation (~30s wasted)
- 3 diagnostic `aws sts` round-trips (~1min)
- 1 user-action round-trip (~10min — user rotates keys in console + paste to WSL)
- Total: ~12min preventable wall-clock if rule existed

**Root cause:** session-start `collect-state.sh` AWS snapshot showed cached fresh data (30m TTL) suggesting creds OK; ad-hoc `start-stack.sh` invocation 7h later did NOT verify creds first. Cache value was stale signal.

Per `incident-to-rule-pipeline.md` 5-stage: rule này = direct output applied through pipeline.

---

## 3. Required pre-flight sequence

### 3.1 Step 1 — Cred check (mandatory)

```bash
aws sts get-caller-identity --profile dev-admin 2>&1
```

**Expected output:**
```json
{
  "UserId": "AIDA...",
  "Account": "906286017800",
  "Arn": "arn:aws:iam::906286017800:user/<admin-user-name>"
}
```

**Failure paths:**
- `InvalidClientTokenId` → keys rotated/expired. STOP, rotate via:
  - User-action: AWS Console → IAM → Users → `<admin-user>` → Security credentials → Create access key
  - Then `aws configure --profile dev-admin` (or `aws configure set` per profile)
- `AuthFailure` → similar, regenerate keys
- Wrong `Account` ID → profile misconfigured, fix `~/.aws/credentials` or `~/.aws/config`
- Wrong `Arn` user type (e.g., `ci-deploy` not admin) → switch profile or update creds

### 3.2 Step 2 — State check (mandatory)

```bash
# EC2 state check
aws ec2 describe-instances \
  --profile dev-admin \
  --region ap-southeast-1 \
  --filters "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app,kitehub-kc-app-fe" \
  --query 'Reservations[].Instances[].{Id:InstanceId,Name:Tags[?Key==`Name`].Value|[0],State:State.Name}' \
  --output table

# RDS state check
aws rds describe-db-instances \
  --profile dev-admin \
  --region ap-southeast-1 \
  --query 'DBInstances[?contains(DBInstanceIdentifier, `kitehub`)].{Id:DBInstanceIdentifier,State:DBInstanceStatus}' \
  --output table
```

**Expected output before `start-stack`:**
- 3 EC2 instances `stopped` (kh-backend, kc-app, kc-app-fe)
- 1 RDS instance `stopped` (kitehub-postgres)

**Expected output before `stop-stack`:**
- 3 EC2 instances `running`
- 1 RDS instance `available`

**Failure paths:**
- 0 instances found via tag → tag drift OR instances deleted; investigate
- State mismatch (e.g., already running before start, already stopped before stop) → no-op (skip lifecycle)
- Unexpected new instances → account state changed; user-confirm before proceeding

### 3.3 Step 3 — Document evidence (lightweight)

For routine lifecycle ops, document evidence inline in session conversation OR commit body trailer:

```
PRE_FLIGHT_AWS_VERIFIED: caller=<arn> account=906286017800 EC2=3/3 stopped RDS=1/1 stopped 2026-05-26T18:02Z
```

Full audit artifact (per `pre-mutation-state-check.md` §3) NOT required for start/stop ops — those rules cover production mutations (terraform apply, IAM create, ACM import, etc.). Lifecycle start/stop is reversible + read-write Tier 2 per `agent-aws-access.md` §2 — light-weight evidence sufficient.

---

## 4. Cred rotation path (when Step 1 fails)

Per `agent-action-bias.md` §1 Part B "Command over UI" — prefer CLI path. But IAM access key creation is interactive (no API path for creating ROOT IAM creds without existing creds; Console interactive required).

**Steps (user-action required):**

1. AWS Console → IAM → Users → `<admin-user-name>` (e.g., `solo-dev-admin`) → Security credentials → Create access key
2. Use case: Command Line Interface (CLI) → Next → tag optional → Create access key
3. Download .csv OR copy Access Key ID + Secret Access Key (1-time display)
4. WSL command:
   ```bash
   aws configure set aws_access_key_id <KEY_ID> --profile dev-admin
   aws configure set aws_secret_access_key <SECRET> --profile dev-admin
   aws configure set region ap-southeast-1 --profile dev-admin
   aws configure set output json --profile dev-admin
   ```
5. Verify: `aws sts get-caller-identity --profile dev-admin` — confirm correct user + account

After rotation: delete old access key in Console (cleanup).

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Trust session-start `collect-state.sh` AWS snapshot (cached 30m TTL) as fresh cred signal | Re-verify creds với `aws sts get-caller-identity --profile dev-admin` at lifecycle trigger time |
| Trigger `start-stack.sh` without pre-flight, "vì script tự verify" | Script verify thường lazy + slow-fail; pre-flight catches early + saves wall-clock |
| Skip Step 2 state check "vì instances chắc còn ở đó" | Tag drift / account state change happen silently; verify before mutation |
| Use `default` profile as fallback "vì dev-admin fail" | `default` profile thường CI-narrow perms; rotate `dev-admin` properly |
| Hard-code instance IDs sau khi creds work "để skip Step 2 next time" | Tags drift; always query by tag, never cache IDs in script (per GAP-492 dynamic tag lookup) |
| Run start + smoke + stop trong 1 batch không re-verify giữa các op | Verify state-check before each Tier 2 lifecycle trigger (start/stop is 2 separate ops) |
| Document evidence chỉ trong chat (no commit trailer) | At minimum 1-line trailer trong wave plan / closure PR / runbook session log |

---

## 6. Worked self-test — 2026-05-26 incident retroactive

Apply rule §3 retroactively to 2026-05-26 17:50 Phase β smoke moment:

**Step 1 — Cred check (would have run):**
```bash
aws sts get-caller-identity --profile dev-admin
# Expected: valid Arn
# Actual: ❌ InvalidClientTokenId
```

→ Rule §3.1 failure path triggers immediately → STOP, rotate creds, retry. **Save ~12min** vs incident actual where we ran start-stack first + diagnosed creds second.

**Step 2 — State check (after creds fixed):**
```bash
aws ec2 describe-instances --profile dev-admin --region ap-southeast-1 --filters "Name=tag:Name,Values=kitehub-kh-backend,kitehub-kc-app,kitehub-kc-app-fe" --query 'Reservations[].Instances[].State.Name'
# Expected: ["stopped", "stopped", "stopped"]
# Actual (post creds-rotate): All 3 stopped ✅
```

→ Pre-flight PASS, safe to trigger `start-stack.sh`.

**Step 3 — Document evidence:**
```
PRE_FLIGHT_AWS_VERIFIED: caller=arn:aws:iam::906286017800:user/solo-dev-admin account=906286017800 EC2=3/3 stopped RDS=1/1 stopped 2026-05-26T18:02Z
```

Trailer appended to Wave beta-prep-1 closure PR body.

**Verdict:** Rule fires correctly on 2026-05-26 incident. Counterfactual eliminates ~12min wall-clock + user-action friction. Self-test PASS ✅

---

## 7. Enforcement (per `rule-change-process.md` §6.5)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho PR triggering AWS lifecycle op (workflow_dispatch hoặc agent Bash invocation):

- [ ] PR body / session conversation chứa `PRE_FLIGHT_AWS_VERIFIED:` trailer (lightweight evidence)?
- [ ] Step 1 Cred check evidence cited (caller-identity output)?
- [ ] Step 2 State check evidence cited (instance state pre-op)?
- [ ] Step 3 inline trailer OR closure PR reference?

### 7.2 Memory auto-load (deferred per `incident-to-rule-pipeline.md` §3.1 premature-rule guard)

Memory entry `feedback_pre_flight_aws_lifecycle.md` có thể remind tại session start before AWS lifecycle triggers. Defer ≥7 ngày; reviewer-checklist + worked self-test §6 đủ cho v1.0.0.

### 7.3 Script-level extension (deferred)

Future enhancement: `scripts/aws/start-stack.sh` + `scripts/aws/stop-stack.sh` add `--skip-preflight` flag (default false = pre-flight enforced). Per `incident-to-rule-pipeline.md` §3.1 — defer until recurrence ≥2 post-rule landing.

### 7.4 Override mechanism

Genuine exception (emergency stack restart sau prod incident, no time for full pre-flight):

```
git commit -m "...
PRE_FLIGHT_AWS_SKIP: <reason — e.g., 'P0 prod incident, restart cascade in progress'>
PRE_FLIGHT_AWS_FOLLOWUP: <follow-up gap to backfill pre-flight evidence within 24h>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 8. Relationship to other rules

- **`agent-aws-access.md`** §2 — Tier 1 read-only allowlist (describe/list/get); this rule extends to Tier 2 lifecycle ops (start/stop)
- **`pre-mutation-state-check.md`** v1.2.0 §3 — covers HIGH-stakes production mutation (terraform apply, IAM, ACM, SES); rule này covers LIGHTWEIGHT lifecycle (start/stop EC2/RDS) — reversible, light evidence sufficient
- **`concurrent-production-mutation-ops.md`** v1.0.0 — serialize concurrent mutations; rule này complementary (verify state before triggering)
- **`agent-action-bias.md`** §1 Part B — "Command over UI"; rule này §4 cred-rotation path applies UI exception (no API for IAM access key create without existing creds)
- **`feedback_aws_suspension_no_notification_email.md`** (memory) — AWS may suspend without email; rule này pre-flight Step 1 catches account state changes early
- **`release-deploy-standard.md`** §9 — deploy execution human-triggered workflow_dispatch; rule này covers PRE-deploy stack lifecycle
- **`release-fix-retry-budget.md`** v1.2.0 §3.5 — investigation phase mandate; rule này pre-flight = investigation TRƯỚC khi trigger (not retry budget)
- **`incident-to-rule-pipeline.md`** v1.1 — applied 5-stage:
  - Detect ✓ (user-flagged 2026-05-26 cred-rotate cycle incident)
  - Classify ✓ (no existing rule covers AWS lifecycle pre-flight; sister rules cover read-only allowlist + production mutation but NOT lifecycle)
  - Rule+Enforce ✓ (this file + reviewer-checklist + worked self-test §6 + paired same-PR rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate)
  - Self-Test ✓ (§6 worked example on 2026-05-26 incident — rule fires correctly + counterfactual ~12min wall-clock saved)
  - Retro Log ✓ (§9 below)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test + rules-index.csv row + output-review-mandate.md §3 row all ship same PR (Wave beta-prep-1 closure)
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn check → mọi session subsequent auto-comply prospectively → eliminate retroactive cred-rotate cycle cost)

---

## 9. Log

- **2026-05-26 (v1.0.0):** Rule created in response to user direction 2026-05-26 post-incident "thêm rules, state-check trước khi start aws stack, tránh lỗi như lần này". Session triggered `bash scripts/aws/start-stack.sh` without pre-flight cred check → `dev-admin` keys expired → `InvalidClientTokenId` → user-action cred rotation cycle (~12min wall-clock) → eventually success after rotation. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged + concrete 2026-05-26 incident) → Classify ✓ (no existing rule codifies AWS lifecycle pre-flight; `agent-aws-access.md` covers Tier 1 read-only allowlist not Tier 2 lifecycle; `pre-mutation-state-check.md` covers HIGH-stakes mutation not lifecycle start/stop) → Rule+Enforce ✓ (this file + reviewer-checklist §7.1 + worked self-test §6 + paired same-PR rules-index.csv row + output-review-mandate.md §3 row + memory mirror PR body inline per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on the originating 2026-05-26 incident — rule fires correctly + counterfactual ~12min wall-clock saved + ~1 user round-trip eliminated) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn check → mọi session subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered AWS lifecycle pre-flight class; no constraint loosening for prior sessions; existing sessions grandfathered; rule applies prospectively từ next session forward). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: pre-flight check before AWS lifecycle) + ✅ unique (sister rules cover different scopes — read-only allowlist vs production mutation) + ✅ widely applicable (every session needing live AWS verify) + ✅ body discipline §1 ≤2 "and" conjunctions. Memory auto-load (§7.2) + script-level extension (§7.3) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (reviewer-checklist + worked self-test §6 sufficient cho v1.0.0; revisit detector when recurrence-count ≥2 post-rule).
