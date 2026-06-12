# ADR-022: Alertmanager Secret Strategy — External Secrets Operator + AWS Secrets Manager

**Status:** ACCEPTED
**Date:** 2026-04-28
**Deciders:** @nguyenvankiet (solo-dev)
**Related Gap(s):** GAP-144 (closed by this ADR + paired Helm changes)

## Context

GAP-120 foundation work landed Alertmanager into the Helm chart with three
RECEIVER STUBS pointing at `*.invalid` placeholder hosts:

- `default-webhook` → `http://alertmanager-webhook-placeholder.invalid/default`
- `critical-webhook` → `http://alertmanager-webhook-placeholder.invalid/critical`
- `warning-email` → `ops@kitehub.me` via `smtp.placeholder.invalid:587`

Alertmanager starts cleanly but every alert delivery silently fails (DNS
unresolvable). On-call effectiveness is **zero** until production receivers are
wired — making this the only **P0** of the Wave Observability cluster
(2026-04-29).

Three secret values must reach the Alertmanager pod safely:

1. Slack webhook URL (full URL with token)
2. PagerDuty Events API routing key
3. SES SMTP password

Forces in tension:

- **Secret hygiene** — credentials must NEVER hit git (audit-gate, PR review,
  secret-scanning all triple-check this)
- **Existing AWS pattern** — `infrastructure/terraform-aws/secrets.tf` already
  provisions `aws_secretsmanager_secret` resources for `jwt`, `encryption`,
  `db_password`. The `secret_arns` output is explicitly labelled
  *"for External Secrets Operator"* (line 48-56) — operator adoption was
  pre-decided, never formally captured.
- **Solo-dev operational capacity** — must not introduce parallel secret tooling
  per environment (sealed-secrets in dev, ESO in prod) — drift cost compounds
- **Multi-tenant readiness** — KiteHub provisions Alertmanager once for the
  platform; cluster-level secret rotation must propagate without manual
  re-templating
- **Dev-cluster ergonomics** — local kind / minikube clusters running this
  chart must NOT require AWS credentials; the receiver wiring must be opt-in

Stakeholders affected: SRE (rotation), DevOps (deploy), Platform Engineers
(chart consumers), on-call (depends on alerts arriving).

## Decision

**We will use the External Secrets Operator (ESO) with AWS Secrets Manager
backend to materialize Alertmanager receiver credentials into a Kubernetes
`Secret` named `alertmanager-receivers`, referenced by Alertmanager via
`*_file:` directives.**

Concretely:

1. AWS Secrets Manager hosts three secrets under the existing
   `kitehub/<environment>/` naming convention:
   - `kitehub/<env>/alertmanager/slack-webhook`
   - `kitehub/<env>/alertmanager/pagerduty-key`
   - `kitehub/<env>/alertmanager/smtp-password`
2. A Helm-templated `ExternalSecret` resource at
   `templates/alertmanager-external-secret.yaml` references those keys,
   produces a k8s `Secret` (`alertmanager-receivers`) with three keys, and
   refreshes every 1h.
3. Alertmanager mounts the Secret as files at
   `/etc/alertmanager/secrets/alertmanager-receivers/<key>` via the
   subchart's `alertmanagerSpec.volumes` + `alertmanagerSpec.volumeMounts`.
4. Receiver config in `values.yaml` switches from raw URLs/passwords to
   `*_file:` directives (`slack_configs.api_url_file`,
   `pagerduty_configs.service_key_file`,
   `global.smtp_auth_password_file`).
5. The whole production-receivers track is gated behind
   `monitoring.alertmanager.receivers.production.enabled` — defaulted to
   `false` so dev/local installs work without AWS.

ESO becomes the canonical path for Helm-deployed secrets (terraform-aws
`secrets.tf` already aligned). Future credentials (e.g., Grafana admin
password) follow the same pattern when they migrate to ESO.

## Consequences

### Positive

- **Secret hygiene preserved** — no credential hits git; ESO pulls from AWS SM
  at runtime, k8s Secret only exists in cluster memory + etcd (with EBS
  encryption-at-rest from EKS defaults).
- **Rotation is one-touch** — update value in AWS SM, ESO refreshes within
  `refreshInterval` (1h default), Alertmanager picks up new file on next
  config reload. No Helm re-template, no pod restart for SMTP/Slack/PD secrets
  (Alertmanager re-reads `*_file:` paths on `SIGHUP`).
- **Aligns with existing Terraform** — secrets.tf line 49-56 explicitly
  exports `secret_arns` "for External Secrets Operator" — adoption was
  pre-decided, this ADR formalizes it.
- **Production-only gate** — dev/local installs run `monitoring.enabled=true`
  without AWS credentials; the ExternalSecret only renders when
  `receivers.production.enabled=true` is set explicitly.
- **No code path leaks credentials** — Alertmanager reads from disk, never
  from environment variables (which appear in `kubectl describe pod`).

### Negative

- **ESO is now a hard dependency** for production deploys — operators must
  install `external-secrets-operator` in the cluster before the chart can
  deliver alerts. Mitigated by clear README docs + helm dry-run that surfaces
  missing CRDs early.
- **AWS-only path** — Oracle Cloud (the primary deploy target per top of
  values.yaml) does NOT have AWS Secrets Manager. For OCI, operators either
  (a) run ESO with OCI Vault provider (supported by ESO upstream) and adjust
  the `secretStoreRef`, or (b) materialize the Secret manually. This ADR
  concerns AWS only; OCI-specific receiver wiring is out of scope (defer to
  follow-up gap if/when OCI deploy goes live).
- **One additional CRD** in cluster (`ExternalSecret`) — operators must learn
  ESO basics. Mitigated by single, simple resource type.
- **Refresh latency** — secret rotation propagates within 1h
  (`refreshInterval`). Not real-time. Acceptable for Slack/PD/SMTP creds
  which rotate quarterly at most.

### Neutral

- ESO ClusterSecretStore (`aws-secrets-manager`) must exist before the
  ExternalSecret reconciles — chart presumes this is provisioned per-cluster
  by platform team, not per-release.
- `alertmanager-receivers` Secret is namespace-scoped to the chart's
  install namespace (typically `kitehub` or `monitoring`).
- Existing `kitehub-secrets` Secret (used by `deployment.yaml` for db / jwt /
  encryption credentials) is unrelated and stays as-is until a future
  migration ADR adopts ESO for those too.

## Alternatives Considered

### Alternative A: Sealed Secrets (Bitnami)

**Pros:** GitOps-native — the encrypted ciphertext IS the artifact, lives in
Git, CI verifies it. No external dependency on AWS at runtime.

**Cons:**

- Requires kubeseal binary in CI + on every operator workstation
- Cluster-bound — sealed secret encrypted for cluster A cannot decrypt in
  cluster B (re-seal required per environment)
- Rotation requires re-encrypting + redeploying, not just AWS SM update
- Does NOT match existing `terraform-aws/secrets.tf` pattern (which is AWS SM)
- Splits secret tooling into two systems (terraform creates SM secrets, but
  Helm reads sealed secrets from git) — drift inevitable

**Rejected because:** doubles tool surface area without solving a problem ESO
+ AWS SM doesn't already solve cleanly.

### Alternative B: Raw Kubernetes Secret (committed Helm values)

Operator runs `helm install --set monitoring.alertmanager.slackWebhookUrl=...`
or maintains an uncommitted `values-prod.yaml`.

**Pros:** Zero dependencies. Trivial to implement.

**Cons:**

- Trivial to leak — `values-prod.yaml` ends up in CI artifacts, screenshots,
  shell history, error messages
- No rotation story — every rotation requires `helm upgrade` (which itself
  requires the new secret in plaintext on the operator's machine)
- Violates `output-review-mandate.md` §3 (no audit trail for credentials)
- Cannot be code-reviewed (the values file lives outside the repo)

**Rejected because:** the README already explicitly warns against committing
`values-prod.yaml` (line 107: *"NOT committed — uses real secrets"*) but
provides no enforcement mechanism. Codifying this as the strategy would
institutionalize the leak vector.

### Alternative C: Vault (HashiCorp) + Vault Injector

**Pros:** Best-in-class secret management. Mature, full audit log, policy
engine.

**Cons:**

- High operational overhead — Vault cluster, auto-unseal, backup story for
  solo-dev → out of scope at current team size
- Duplicates AWS SM (which is already provisioned)
- ESO + Vault is a possible future migration (ESO supports Vault as backend),
  not a different decision

**Rejected because:** AWS SM already exists; Vault would be net new infra
for solo-dev with no functional gain over ESO + AWS SM combination today.
Re-evaluate when team grows past 5 engineers OR multi-cloud demands it.

## Implementation Notes

**Migration strategy:** zero-downtime — current deployment uses placeholder
values that silently drop alerts. Switching to `*_file:` references with the
gating flag means:

1. Existing `monitoring.enabled=true` deploys continue to work (placeholders
   stay in the receivers section by default).
2. Production opt-in: operators set
   `monitoring.alertmanager.receivers.production.enabled=true` AFTER ensuring
   ESO + ClusterSecretStore + AWS SM secrets are provisioned.
3. Helm upgrade rolls Alertmanager pod with new mount; first reconcile reads
   the file, alerts route correctly within minutes.

**Rollback plan:** flip `production.enabled=false` and `helm upgrade` —
reverts to placeholder receivers (alerts silently drop again, but cluster
remains stable; no downtime).

**Feature flag:** `monitoring.alertmanager.receivers.production.enabled`
(false by default).

**Monitoring / success criteria:**

- ESO logs report `Synced` for the ExternalSecret resource within 60s of apply
- `kubectl get secret alertmanager-receivers -o jsonpath='{.data}'` returns
  three keys (base64-encoded; do NOT decode in scripts that log)
- Mock alert (e.g., `amtool alert add severity=critical alertname=TestAlert`)
  arrives in Slack within 2 min, PagerDuty within 2 min, email within 5 min
- Inhibition rules suppress dependent alerts when `ServiceDown` fires for the
  same job (verified via `amtool` simulation)

**Provisioning checklist** (per environment, one-time):

1. Apply `terraform-aws` — adds 3 new `aws_secretsmanager_secret` resources
   (separate PR, not in this ADR's scope; values populated manually via
   AWS Console or `aws secretsmanager put-secret-value`)
2. Install ESO chart in cluster: `helm install external-secrets external-secrets/external-secrets -n external-secrets --create-namespace`
3. Create `ClusterSecretStore` named `aws-secrets-manager` pointing at AWS SM
   in target region
4. `helm upgrade kitehub ... --set monitoring.alertmanager.receivers.production.enabled=true`

## References

- Related ADRs:
  - ADR-007 (Outbox Pattern) — unrelated; pattern reference for "secrets-as-files" approach (similar separation of read path from write path)
  - ADR-011 (Defense-in-Depth Security) — `*_file:` is the recommended pattern there too
- Related rules:
  - `.claude/rules/output-review-mandate.md` §3 (Logs Standard mandates no PII / secrets in logs — Alertmanager `*_file:` keeps creds off command line)
  - `.claude/rules/logs-format-standard.md` §2.4 (banned fields list — passwords / API keys in logs)
- Related gap: `documents/04-quality/gaps/GAP-144-alertmanager-production-receivers.md`
- External:
  - [External Secrets Operator docs](https://external-secrets.io/latest/)
  - [AWS Secrets Manager + ESO setup](https://external-secrets.io/latest/provider/aws-secrets-manager/)
  - [Alertmanager `*_file:` directives](https://prometheus.io/docs/alerting/latest/configuration/#file)

## Log

- 2026-04-28 — ADR proposed AND accepted same day. Solo-dev — no separate review cycle; design grounded in the pre-existing `terraform-aws/secrets.tf` ESO comment (line 48-56) which already documented the strategy intent. This ADR formalizes that intent + paired Helm changes (GAP-144 closure PR).
