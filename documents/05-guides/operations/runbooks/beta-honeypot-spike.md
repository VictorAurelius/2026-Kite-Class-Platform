# Runbook — BetaHoneypotSpike

**Alert:** `BetaHoneypotSpike`
**Severity:** warning
**Trigger:** `rate(beta_honeypot_rejections_total[1h]) > 10/3600` sustained
**Closes:** GAP-387 alert observability requirement

## Symptoms

- `beta_honeypot_rejections_total` ramping rapidly
- Likely paired with elevated 400 responses on `POST /api/v1/auth/request-beta-access`
- Honest signups should have `honeypot=""` always; non-empty = bot

## Likely causes

1. **Automated bot scanning** — scanner submits all form fields including the trap; expected baseline ≈ 0/hour for healthy traffic
2. **Compromised crawler botnet** — distributed across many IPs, each low-volume
3. **Form field name leaked** — bots adapted; honeypot still triggers because they fill EVERY field
4. **Misconfiguration** — frontend accidentally sending honeypot value (false positives) — verify FE form handler

## Diagnostic steps

1. Confirm rate:
   ```bash
   curl -s http://kitehub-subscription:8080/actuator/prometheus \
     | grep '^beta_honeypot_rejections_total'
   ```
2. Cross-check `beta_signup_requests_total` rate — is legitimate flow also spiking? (suggests overall surge, not pure bot)
3. Inspect gateway logs for top IPs hitting `/api/v1/auth/request-beta-access` with 400 status
4. Verify FE form is NOT pre-populating honeypot field (regression test)

## Remediation

| Cause | Action |
|---|---|
| Pure bot traffic (no legit spike) | Tighten gateway rate-limit; consider IP-based blocklist via WAF |
| FE regression sending honeypot | Hotfix FE form handler; deploy promptly to stop false positives |
| Distributed botnet | Engage WAF rules (challenge JS for /request-beta-access path) |
| Field name leaked | Rotate honeypot field name in `BetaRequestDto` + FE form (paired change) |

## Escalation

Sustained > 1 hour with confirmed bot signature → page on-call security + backend engineers.

## References

- Source GAP: `documents/04-quality/gaps/closed/GAP-387-beta-signup-metric-counters-missing.md`
- DTO honeypot field: `BetaRequestDto.honeypot` (`@Size(max = 0)` Bean Validation)
- Counter: `BetaAccessService#recordHoneypotRejection()`
