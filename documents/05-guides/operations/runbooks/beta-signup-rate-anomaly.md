# Runbook — BetaSignupRateAnomaly

**Alert:** `BetaSignupRateAnomaly`
**Severity:** warning
**Trigger:** `rate(beta_signup_requests_total[5m]) > 50/60` sustained 5 minutes
**Closes:** GAP-387 alert observability requirement

## Symptoms

- Operator sees beta-signup rate anomaly alert
- `/actuator/prometheus` shows elevated `beta_signup_requests_total` rate per persona
- Coordinator inbox `GET /api/v1/admin/beta-requests?status=PENDING` may show fast-growing PENDING list

## Likely causes

1. **Marketing campaign launched** — expected uptick, validate with marketing team
2. **Bot ring bypassing honeypot** — check `beta_honeypot_rejections_total` for paired spike (if present, honeypot working; if not, attackers may have learned the field name)
3. **Public referral / news mention** — viral inbound, validate via referrer logs
4. **Misuse of public endpoint** — single IP spamming, gateway rate-limit may need tightening

## Diagnostic steps

1. Confirm the rate trend:
   ```bash
   curl -s http://kitehub-subscription:8080/actuator/prometheus \
     | grep '^beta_signup_requests_total'
   ```
2. Break down by `persona` label — is one persona disproportionate?
3. Cross-check gateway access logs for IP/UA distribution (5xx/429 count, top IPs)
4. Check `beta_honeypot_rejections_total` — is bot detection still firing proportionally?
5. Verify recent marketing/PR activity (Slack #marketing, blog publish dates)

## Remediation

| Cause | Action |
|---|---|
| Legitimate marketing spike | Coordinator capacity check — add reviewers if PENDING > 50; no code change |
| Bot ring | Tighten gateway rate-limit per IP; rotate honeypot field name in `BetaRequestDto` |
| Single-IP spam | Block IP at gateway / WAF; file follow-up for adaptive rate-limit |
| Persona imbalance | Investigate source of one-persona traffic; possible referral campaign |

## Escalation

If sustained > 30 minutes despite mitigations, page on-call backend engineer.

## References

- Source GAP: `documents/04-quality/gaps/GAP-387-beta-signup-metric-counters-missing.md`
- Service: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java`
- Alert rule: `infrastructure/helm/kitehub/templates/prometheusrule.yaml` + `kitehub/docker/prometheus/alert-rules.yml`
