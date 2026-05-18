# GAP-483: EC2 user_data missing git + repo clone bootstrap

**Status:** 🟢 DONE 2026-05-12 (Wave 65 — `ec2_user_data` installs git + clones repo on boot; workflow comment synced)
**Priority:** 🔴 P0 BLOCKING (every EC2 replacement requires manual SSM bootstrap)
**Domain:** DevOps / Infrastructure
**Found:** 2026-05-12 (Wave 64 cutover Step F)
**Affects:** All future EC2 replacement (AMI bump, instance class change, etc.)

## Problem

EC2 user_data in `infrastructure/terraform-aws/ec2.tf` only installs:
- docker
- amazon-cloudwatch-agent

Missing:
- `git` (required to clone repo)
- Repo clone to `/opt/kite-prod` (required by `deploy-prod.sh`)
- Tag checkout (required to lock specific release)

When EC2 instance replaces (AMI bump as in Wave 64), new instance has NO `/opt/kite-prod` → `deploy-production.yml` workflow fails at SSM RunCommand: `bash: /opt/kite-prod/scripts/deploy-prod.sh: No such file or directory`.

Workflow comment acknowledges this:
> "SSM run-command: assume EC2 already bootstrapped with repo at /opt/kite-prod (one-time manual setup per Phase 7 prep §7.1.2)."

The "Phase 7 prep §7.1.2" assumption breaks on automatic instance replacement.

## Proposed Fix

Add to `infrastructure/terraform-aws/ec2.tf` `ec2_user_data` local:

```bash
# After existing docker + cloudwatch-agent install:
dnf install -y git
mkdir -p /opt/kite-prod
chown ec2-user:ec2-user /opt/kite-prod
sudo -u ec2-user git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git /opt/kite-prod
# Tag pinning: leave on main; deploy-prod.sh checks out KITE_VERSION at deploy time
```

Repo is public so no auth needed.

## Acceptance Criteria

- [x] `ec2_user_data` includes git install + repo clone
- [ ] New EC2 instance (terraform apply replacing instance) boots with `/opt/kite-prod/scripts/deploy-prod.sh` available
- [ ] `deploy-production.yml` workflow succeeds end-to-end without manual SSM bootstrap
- [x] Document removed: "one-time manual setup" assumption note in workflow comment (synced to point at `ec2_user_data`)

## Out-of-scope

AC#2 (new EC2 boot verification) and AC#3 (end-to-end deploy success) require a `terraform apply` that replaces the running EC2 instance. Per `.claude/rules/release-deploy-standard.md` §9 + `.claude/rules/pre-mutation-state-check.md`, `terraform apply` is **human-triggered workflow_dispatch only** — agent must not initiate. These two AC items are verified on the next user-triggered terraform-apply.yml run + first end-to-end deploy after the user_data change lands; both will happen naturally on next infra refresh. No follow-up gap needed (deferred-to-trigger semantics per `audit-to-gap-pipeline.md` §2.8 forward-looking exception).

## Related

- **Parent:** Wave 64 Step F deploy fail cascade
- **Sibling:** GAP-482 (IAM bugs), GAP-484 (OTel crash — next bug after this fixed)
- **Workflow caller:** `.github/workflows/deploy-production.yml`

## Log

- **2026-05-12:** Filed Wave 64 cutover Step F. Manual SSM bootstrap unblocked session but root cause persists.
- **2026-05-12 (Wave 65 Bucket E):** AC#1 + AC#4 shipped. `infrastructure/terraform-aws/ec2.tf` `ec2_user_data` local now installs `git`, creates `/opt/kite-prod`, and clones the public repo as `ec2-user` on boot. `.github/workflows/deploy-production.yml` SSM step comment rewritten — removed the "assume EC2 already bootstrapped" + "one-time manual setup per Phase 7 prep §7.1.2" claim; points at `ec2_user_data` as the bootstrap source. `terraform fmt -check` clean; `terraform validate` skipped in worktree (providers not cached locally — CI `terraform-plan` workflow validates on PR). AC#2 + AC#3 deferred to next human-triggered `terraform apply` per `release-deploy-standard.md` §9 (agent must not initiate apply); both verify naturally on next infra refresh — no follow-up gap needed (forward-looking exception per `audit-to-gap-pipeline.md` §2.8).
