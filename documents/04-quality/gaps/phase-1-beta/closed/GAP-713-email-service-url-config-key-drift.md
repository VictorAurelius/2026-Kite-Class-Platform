---
id: GAP-713
title: Email service URL config key drift — `email.service.url` defaults to localhost vs `email-service.url` defaults to Docker DNS
status: OPEN
priority: P0
phase: phase-1-beta
found: 2026-05-22
audience: dev
related: [GAP-702, GAP-703, GAP-543, GAP-657, GAP-710]
---

# GAP-713 — Email service URL config key drift

## Problem

`kitehub/kitehub-subscription/src/main/resources/application.yml` chứa **HAI key duplicate** cho email service URL với default values KHÁC nhau:

```yaml
email-service:                                        # key 1 (kebab in middle)
  url: ${EMAIL_SERVICE_URL:http://kitehub-email:8080}  # ← đúng cho Docker network
  enabled: ${EMAIL_SERVICE_ENABLED:true}

email:                                                 # key 2 (nested dot path)
  service:
    url: ${EMAIL_SERVICE_URL:http://localhost:8083}    # ← sai trong Docker container
```

Spring binds 2 keys vào 2 different `@ConfigurationProperties` bindings. `EmailConsumer` reads key 2 → uses `localhost:8083` → request fails because `localhost` inside subscription container = subscription itself, NOT kitehub-email.

## Evidence

Runtime log (Wave 104 Bucket E live verify 2026-05-22):
```
ERROR c.k.s.consumer.EmailConsumer - Failed to send email via HTTP:
       type=beta-invite, to=h***@skyedu.vn,
       error=I/O error on POST request for "http://localhost:8083/api/platform/emails/send": null
ERROR c.k.s.consumer.EmailConsumer - Failed to send email via HTTP:
       type=admin-new-login-alert, to=a***@kitehub.com, ...
```

ALL email sends fail same way regardless of email type. Approval emails (Bucket B1 GAP-702), admin alerts (Bucket A side-effect), and all 5 email types Bucket B2 (GAP-703) blocked.

Mailhog inbox count post-approve: 0 (expected ≥1).

## Root Cause

Config drift accumulated over multiple waves:
- One wave introduced `email-service.url` (kebab-in-middle convention)
- Another wave introduced `email.service.url` (dot-nested convention) — likely Spring `@Value("${email.service.url}")` consumer
- Both kept; `EmailConsumer` reads the wrong one with wrong default

`EMAIL_SERVICE_URL` env var (if set) overrides both — but local dev profile likely doesn't set it.

## Proposed Fix

**Option A — Consolidate to single key (recommended):**
1. Pick canonical key (`email-service.url` follows Spring kebab convention better)
2. Update `EmailConsumer` + any other consumer to use canonical key
3. Delete duplicate key from `application.yml`
4. Update `application-dev.yml` / `-production.yml` overrides if any
5. Verify env var `EMAIL_SERVICE_URL` still overrides

**Option B — Fix default of wrong key (minimal):**
1. Change `email.service.url` default to `http://kitehub-email:8080`
2. Document deprecation; consolidate in follow-up

Option A preferred — eliminates future drift recurrence.

## Acceptance Criteria

- [ ] Single canonical config key for email service URL
- [ ] Default value points to Docker DNS name (`kitehub-email:8080`)
- [ ] `EmailConsumer` reads canonical key
- [ ] Live verify: admin approve beta-request → Mailhog +1 approval email arrives within 5s
- [ ] Live verify: all 5 email types (welcome / approval / verification / passwordReset / adminAlert) deliver successfully
- [ ] IT test: `WireMockServer` simulating email service, verify URL config resolves correctly
- [ ] Grep `application*.yml` zero duplicate email URL keys
- [ ] Env var `EMAIL_SERVICE_URL` override still works (test by exporting in container)

## Impact assessment

- **Currently broken on local stack:** approval emails (Bucket B1), admin login alerts, all 5 email types (Bucket B2)
- **Production impact?** unknown without AWS access (GAP-612 suspended) — possible if production config relies on default; mitigated if production has `EMAIL_SERVICE_URL` env var explicitly set
- **Priority P0:** blocks Wave 104 Bucket B end-to-end verify entirely + multi-email-type AC

## Related

- Triggered by: Wave 104 Bucket E live verify finding (`2026-05-22-wave-104-bucket-e-partial-verify.md` §5 Bug 3)
- Blocks: GAP-543 (email hardening), GAP-657 (Bucket B2 List-Unsubscribe), GAP-702 (approval email)
- Wave 105 candidate scope per Wave 104 Bucket E AC "if surfaced → file Wave 105"
- Code refs:
  - `kitehub/kitehub-subscription/src/main/resources/application.yml` (lines containing `email-service.url` and `email.service.url`)
  - `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/consumer/EmailConsumer.java`
