# Runbook: TLS Certificate Expiry Imminent

**Alert:** `CertExpiryImminent`
**Severity:** critical
**Last updated:** 2026-04-28

## What does this alert mean?

A TLS certificate in the platform ingress chain will expire in **<14 days** (warning) or **<3 days** (critical-bump). Once a cert expires, every browser hits a TLS warning page and most API clients refuse the handshake — effectively a platform-wide outage. The metric source is the Prometheus `blackbox_exporter` `probe_ssl_earliest_cert_expiry` minus current time, scoped to monitored ingresses (`*.kitehub.me`, `*.kitehub.com`, `app.kitehub.me`, etc.). The `{{ $labels.target }}` tells you which hostname is at risk.

## Note

> Alert depends on `blackbox_exporter` being scheduled and configured with the production hostnames. If `blackbox_exporter` is not yet deployed, this rule is **metric-pending** — wire it via Helm values (`infrastructure/helm/kitehub/values.yaml` → `blackbox.targets`) before relying on the alert.

## Immediate checks (0-5 min)

1. **Confirm actual expiry from outside** — don't trust internal scrape alone:
   ```bash
   echo | openssl s_client -connect <hostname>:443 -servername <hostname> 2>/dev/null \
     | openssl x509 -noout -dates -subject
   ```
2. **Check cert-manager status** (production K8s ingress):
   ```bash
   kubectl get certificate -A
   kubectl describe certificate <name> -n kitehub | tail -40
   kubectl get certificaterequest -A | grep -v True
   ```
3. **ACME issuer health** — Let's Encrypt rate limits, DNS-01 challenge state:
   ```bash
   kubectl describe issuer letsencrypt-prod -n kitehub
   kubectl logs -n cert-manager deploy/cert-manager --tail=200 | grep -E 'ERROR|failed|<hostname>'
   ```
4. **Wildcard vs SAN** — note whether the failing cert is a wildcard (`*.kitehub.me`) or a hostname-specific SAN; renewal path differs.

## Likely causes

- **DNS-01 challenge failing** → cert-manager cannot create the `_acme-challenge` TXT record because the DNS provider credential rotated or the `Issuer`/`ClusterIssuer` ref is stale. **Fix:** rotate DNS API token in the secret cert-manager reads (`kubectl get secret <dns-credentials> -n cert-manager -o yaml`), re-trigger renewal.
- **HTTP-01 challenge blocked by reverse proxy** → ingress redirects `/.well-known/acme-challenge/` to HTTPS before LE can serve over HTTP. **Fix:** add ingress annotation to bypass HTTPS redirect for ACME path or switch to DNS-01.
- **Let's Encrypt rate-limit hit** → too many failed renewals tripped the 5-failures-per-account-per-hostname-per-hour limit. **Fix:** wait the rate-limit window OR switch to staging issuer for testing then back; do NOT spam retries.
- **Manual cert in a Kubernetes Secret expired without cert-manager managing it** → check whether the `Certificate` resource exists at all; some legacy secrets predate cert-manager adoption. **Fix:** import into cert-manager or replace with managed issuance.
- **Outage during scheduled renewal window** → cert-manager renewal job ran while cluster was draining or DNS resolver flaky. **Fix:** rerun renewal manually (see Mitigation).

## Mitigation

```bash
# 1. Force cert-manager to re-attempt renewal (delete the certificate-request, controller re-reconciles)
kubectl delete certificaterequest -n kitehub <name>
# Or annotate the Certificate to force renewal:
kubectl annotate certificate <name> -n kitehub \
  cert-manager.io/issue-temporary-certificate="true" --overwrite
kubectl annotate certificate <name> -n kitehub \
  cert-manager.io/renewBefore="720h" --overwrite

# 2. If DNS-01 stuck, manually verify TXT record can be created
dig +short TXT _acme-challenge.<hostname>

# 3. Last resort — issue cert via certbot manually, store in Secret, point ingress at it
certbot certonly --manual --preferred-challenges dns -d "<hostname>"
kubectl create secret tls <name>-tls --cert=fullchain.pem --key=privkey.pem -n kitehub --dry-run=client -o yaml | kubectl apply -f -
```

After renewal, verify `openssl s_client` shows the new `notAfter` date and Prometheus probe metric updates within the next scrape interval (~30s).

## When to escalate

- **<24h to expiry, renewal still failing** → escalate to backup on-call + DNS provider support; likely needs human intervention at registrar level
- **Wildcard cert affecting all tenants** → broader blast radius, treat as P0; loop in customer-success for proactive notification
- **Repeated renewal failures across multiple certs** → cert-manager controller/issuer config issue; escalate to platform/infra lead

## Related

- Alert rule: `kitehub/docker/prometheus/alert-rules.yml` (kitehub-platform-alerts group), `infrastructure/helm/kitehub/templates/prometheusrule.yaml`
- Architecture: `infrastructure/helm/kitehub/values.yaml` (cert-manager + issuer config), `documents/05-guides/infrastructure/SECRET-MANAGEMENT.md`
- Related runbooks: [`service-down.md`](./service-down.md), [`../../deployment-procedures.md`](./deployment-procedures.md)
