# Runbook: JWT Authentication Failure Spike

**Alert:** `JWTAuthFailureSpike`
**Severity:** warning
**Last updated:** 2026-04-28

## What does this alert mean?

`kite-gateway:9000`'s JWT authentication filter is rejecting tokens at a rate **>3x baseline over 5 minutes**. Source: `kite_auth_failed_total{reason}` counter (where reason ∈ `expired`, `invalid_signature`, `malformed`, `audience_mismatch`, `not_yet_valid`). A spike has two flavors: **legitimate traffic suddenly failing** (signing key rotated badly, clock skew, JWKS endpoint down) or **abuse** (credential stuffing, automated scanner, bot replay). Either is operational pain — first impacts every authenticated user, second risks brute-force success or rate-limit budget burn.

## Note

> Metric `kite_auth_failed_total` requires `JwtAuthenticationFilter` (or whichever filter validates tokens) to emit a Micrometer counter on each rejection. If not yet wired, alert may surface as `HighErrorRate{service="kite-gateway"}` filtered by status 401. Track instrumentation in follow-up gap.

## Immediate checks (0-5 min)

1. **Rejection reason breakdown** — is this one cause or many?
   ```bash
   kubectl logs -n kitehub deploy/kite-gateway --tail=500 \
     | grep -E 'JwtAuth|401|signature|expired|JWKS|invalid_token' \
     | awk '{print $NF}' | sort | uniq -c | sort -rn | head
   ```
2. **Source IP distribution** — many distinct IPs (likely real users) vs few IPs (likely abuse):
   ```bash
   kubectl logs -n kitehub deploy/kite-gateway --tail=2000 \
     | grep '401' | awk '{print $<ip-field>}' | sort | uniq -c | sort -rn | head -20
   ```
3. **Signing key health** — recent rotation? JWKS endpoint reachable?
   ```bash
   curl -fsS http://kite-gateway:9000/actuator/jwks | jq '.keys | length'
   # OR if external IdP:
   curl -fsS https://<idp>/.well-known/jwks.json | jq '.keys | length'
   ```
4. **Clock drift** — if NTP broken, tokens look expired/not-yet-valid:
   ```bash
   kubectl exec -n kitehub deploy/kite-gateway -- date -u
   # Compare to: date -u
   ```

## Likely causes

- **Signing key rotated, JWKS not propagated** → gateway still has the old key cached, all newly-signed tokens fail signature check. **Fix:** restart gateway pods, OR force JWKS cache refresh via actuator endpoint. Verify key rotation procedure included gateway redeploy.
- **`INTERNAL_API_SECRET` mismatch** → for service-to-service tokens (subscription → branding), recent secret rotation not synchronized across all services. **Fix:** sync the K8s secret across all consumers; see `feedback_dev_profile_schema_workaround.md` (which references this default).
- **IdP / Auth0 / Cognito outage** → JWKS endpoint returning 5xx, gateway can't fetch new keys; falls back to stale. **Fix:** check IdP statuspage; configure JWKS cache TTL + grace period to bridge brief outages.
- **Credential stuffing attack** → many distinct IPs, many rejected tokens with similar prefix or shape, often from one ASN. **Fix:** enable rate-limit at gateway (per [`rate-limit-breach-spike.md`](./rate-limit-breach-spike.md)), block offending CIDR temporarily via WAF.
- **Audience mismatch after env change** → recent change to `auth.audience` config doesn't match what client apps embed. **Fix:** verify configmap/env across kitehub-frontend, kiteclass-frontend, mobile clients.
- **Token TTL too short post-change** → admin shrunk TTL from 1h to 5min, all in-flight users see 401 during their session. **Fix:** revert TTL, communicate change before deploying.

## Mitigation

```bash
# 1. If rotation issue: refresh JWKS / restart gateway
curl -X POST http://kite-gateway:9000/actuator/auth/jwks/refresh \
  -H "Authorization: Bearer $INTERNAL_API_SECRET"
# Or hard restart:
kubectl rollout restart deployment/kite-gateway -n kitehub

# 2. If abuse pattern detected, deploy rate-limit overlay (per-IP) immediately
# Edit ConfigMap with stricter Bucket4j limits then trigger refresh:
kubectl edit configmap -n kitehub kite-gateway-config
curl -X POST http://kite-gateway:9000/actuator/refresh

# 3. Block confirmed-malicious IPs at WAF / ingress (CloudFront / NGINX ingress annotation)
kubectl annotate ingress kite-gateway -n kitehub \
  nginx.ingress.kubernetes.io/configuration-snippet="deny <CIDR>;"

# 4. Capture sample tokens for forensics (decode header only — do NOT log payload):
echo "<token>" | cut -d. -f1 | base64 -d 2>/dev/null  # header only, kid + alg
```

After mitigation, monitor 401 rate for 30 min. Goal: back to baseline. If abuse-driven, watch [`rate-limit-breach-spike.md`](./rate-limit-breach-spike.md) for related signal.

## When to escalate

- 401 rate >50% of all requests → critical-bump; platform login broken, page on-call lead immediately
- Confirmed credential-stuffing with successful logins (low rate but real) → security incident, treat per `documents/05-guides/SECRET-MANAGEMENT.md` rotation playbook + force password reset for affected accounts
- Cross-region 401 storm → may indicate IdP outage; switch to backup IdP if architecture supports it

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Memory: `feedback_dev_profile_schema_workaround.md` (INTERNAL_API_SECRET notes), `feedback_repo_status_security_coverage.md`
- Doc: `documents/05-guides/SECRET-MANAGEMENT.md`
- Related runbooks: [`rate-limit-breach-spike.md`](./rate-limit-breach-spike.md), [`multi-tenant-data-leak.md`](./multi-tenant-data-leak.md), [`high-error-rate.md`](./high-error-rate.md)
