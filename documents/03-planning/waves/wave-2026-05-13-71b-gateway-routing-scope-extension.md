---
title: Wave 71b — Gateway routing scope extension (GAP-512)
status: active
created: 2026-05-13
updated: 2026-05-13
waves: [71b]
gaps: [GAP-512]
parent: wave-2026-05-13-71-pre-launch-hardening.md
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 71b — Gateway routing scope extension

**Trigger:** Wave 71 Bucket E `scripts/audit-gateway-routes.sh` reported 22 wrong-service routings + 1 orphan after Wave 71 SHIPPED. Wave 71 Bucket A unblocked only Plan 1 Bước 2 (auth); Plan 1 Bước 4 (admin approve) + Bước 6/7 (consent/branding/notification) still blocked.

## 1. Brainstorm

**Q1 — Why scope ngắn?** Single-file edit `kitehub-gateway/src/main/resources/application.yml`. Self-test driven (audit script exit 0). Risk thấp — không touch BE code, không touch Java. Solo agent đủ.

**Q2 — Trade-offs:**
- Option A — Replace `/api/v1/**` catch-all entirely với explicit prefix list: cleaner long-term nhưng risk break kiteclass instance APIs nào không tên trùng pattern
- Option B (✅ chosen) — Keep catch-all BUT add kitehub-specific specific routes BEFORE it (route ordering top-down): minimal blast radius, mirror Bucket A pattern

**Q3 — Verification:** `bash scripts/audit-gateway-routes.sh` exit 0 + live verify ≥3 kitehub paths đi đúng service.

## 2. State-Check Evidence

| Symbol | Type | Verify command | Result | Verdict |
|---|---|---|---|---|
| `kitehub-gateway/src/main/resources/application.yml` | YAML config | `ls` | exists | ✅ |
| `scripts/audit-gateway-routes.sh` | Detector | run | exit 1 with 23 findings | ✅ failing as expected |
| `BetaAccessController` | kitehub-subscription | `grep "/api/v1/admin/beta-requests"` | found | ✅ |
| `ConsentController` | kitehub-subscription | `grep "/api/v1/consent"` | found | ✅ |
| `DsarController` | kitehub-subscription | `grep "/api/v1/dsar"` | found | ✅ |
| `NotificationPreferenceController` | kitehub-subscription | `grep "/api/v1/notification-preferences"` | found | ✅ |
| `BrandingJobV1Controller` + `LifecycleEventsController` | kitehub-branding | `grep "/api/v1/branding"` | found | ✅ |
| Admin email + instance controllers | kitehub-subscription | `grep "/api/platform/admin/emails\|/api/platform/admin/instances"` | found | ✅ |
| `/api/instances/{id}/domain/verify` | kitehub-subscription orphan | `grep` | found | ✅ |

## 3. Scope

### Bucket A — Single agent, single file edit

**Acceptance:** `bash scripts/audit-gateway-routes.sh` exit 0 (no FAIL + no orphan).

**Edits to `kitehub/kitehub-gateway/src/main/resources/application.yml`:**

1. **Split `kitehub-admin-v1` route** (current sends ALL `/api/v1/admin/**` to kitehub-admin):
   - New route `kitehub-admin-beta-requests-v1` BEFORE `kitehub-admin-v1`:
     - Path: `/api/v1/admin/beta-requests/**`
     - URI: `http://kitehub-subscription:8080` (BetaAccessController lives here)
     - CircuitBreaker: `authCircuitBreaker`
   - Keep `kitehub-admin-v1` (`/api/v1/admin/**` → kitehub-admin) AFTER split

2. **Add 4 specific routes BEFORE `instance-apis` catch-all** for kitehub-subscription controllers under `/api/v1/`:
   - `kitehub-consent-v1` → `/api/v1/consent/**` → kitehub-subscription, CircuitBreaker `subscriptionCircuitBreaker`
   - `kitehub-dsar-v1` → `/api/v1/dsar/**` → kitehub-subscription, CircuitBreaker `subscriptionCircuitBreaker`
   - `kitehub-notification-preferences-v1` → `/api/v1/notification-preferences/**` → kitehub-subscription, CircuitBreaker `subscriptionCircuitBreaker`
   - `kitehub-branding-v1` → `/api/v1/branding/**` → kitehub-branding, CircuitBreaker `brandingCircuitBreaker`

3. **Split `platform-admin` route** (current sends ALL `/api/platform/admin/**` to kitehub-admin but 2 sub-paths owned by subscription):
   - New route `platform-admin-emails-subscription` BEFORE `platform-admin`:
     - Path: `/api/platform/admin/emails/**`
     - URI: `http://kitehub-subscription:8080`
     - CircuitBreaker: `subscriptionCircuitBreaker`
   - New route `platform-admin-instances-subscription` BEFORE `platform-admin`:
     - Path: `/api/platform/admin/instances/**`
     - URI: `http://kitehub-subscription:8080`
     - CircuitBreaker: `instancesCircuitBreaker`
   - Keep `platform-admin` AFTER splits

4. **Add orphan route** `kitehub-instance-domain-verify`:
   - Path: `/api/instances/{id}/domain/verify` — actually `/api/instances/**` per Spring matcher; verify in BE controller
   - URI: `http://kitehub-subscription:8080`
   - CircuitBreaker: `instancesCircuitBreaker`
   - Place BEFORE any potential catch-all on `/api/instances/**`

**Verify steps post-edit:**
1. `python3 -c "import yaml; yaml.safe_load(open('kitehub/kitehub-gateway/src/main/resources/application.yml'))"` exit 0
2. `cd kitehub && ./mvnw -pl kitehub-gateway compile` exit 0
3. `bash scripts/audit-gateway-routes.sh` exit 0
4. Push branch, create PR.

### Bucket B — N/A

GAP-513 (Resend manual provisioning) — pure user-action; không có Claude work cho 71b. Tracked separately in GAP-513.

## 4. Closure protocol

After Bucket A merges:
1. Live verify: `curl -i POST https://api.kitehub.me/api/v1/consent/record -H "Origin: https://kitehub.me" -d '{...}'` reaches kitehub-subscription (not kiteclass-core)
2. Live verify: `curl -i GET https://api.kitehub.me/api/v1/admin/beta-requests` reaches kitehub-subscription
3. Live verify: `curl -i POST https://api.kitehub.me/api/v1/branding/slug-availability` reaches kitehub-branding
4. Flip GAP-512 → DONE in gap-status.csv
5. Update ROADMAP §🚀 → "Plan 1 Bước 3-7 execute (depends on GAP-513 user-action for Bước 5)"
6. Append wave-history.jsonl Wave 71b entry
7. Run `bash scripts/prune-merged-worktrees.sh --yes`

## 5. Risk

- Route ordering bug (specific must be BEFORE catch-all) — already established pattern Bucket A
- CircuitBreaker bean missing — `subscriptionCircuitBreaker` + `brandingCircuitBreaker` + `instancesCircuitBreaker` already exist in resilience4j config (verified line 251+ of application.yml)
- Live verify needs new deploy — bump `v0.9.0-beta-staging.13`

## 6. Estimated wall-clock

- Plan PR + agent spawn: 5 min
- Agent edit + verify: 15-20 min
- Plan PR review + merge: 5 min
- Bucket A PR review + merge: 5 min
- Tag + docker build: 5 min
- Deploy + verify: 10 min
- Closure: 5 min

**Total ~50-60 min** (vs Wave 71 ~75 min for 5 buckets).
