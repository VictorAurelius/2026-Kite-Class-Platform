---
title: Cloudflare Verification - apex kitehub.me DNS flip to EIP (post-Wave-82 recovery)
status: draft
created: 2026-05-16
phase: phase-1-beta
wave: post-86
gaps: [GAP-573]
---

# Cloudflare Verification Report - apex kitehub.me DNS flip to EIP

## Scope

Re-flip apex `kitehub.me` A record to the newly-allocated Elastic IP for
`kitehub-kc-app-fe` (companion audit
`2026-05-16-kc-app-fe-eip-cf-cutover-role-pre-apply.md`).

Wave 82 handoff (2026-05-15) documents apex was previously cutover to
`A -> 54.179.70.37 (proxied=false)` via direct Cloudflare API call, with explicit
risk noted: "IP `54.179.70.37` is currently public IP auto-assigned (lost if
instance stop/start). Bind Elastic IP at next opportunity to lock DNS target."
(handoff line 135). Today's session surfaces that the auto-assigned IP did change
(new IP `13.228.25.147`), which fits the documented failure mode. This PR closes
GAP-573 by:

1. Provisioning EIP via terraform (separate audit).
2. Flipping apex DNS to the EIP via new workflow `cloudflare-apex-cutover.yml`
   (this audit).
3. Running smoke admin-login per `release-deploy-standard.md` §3.1 v1.2.0.

**Mode (per user choice):** flip to EIP (post-terraform-apply); proxied flag and
final TTL set at trigger time. Default: `proxied=false`, `ttl=60` (current
Wave 82 state).

## Pre-flight state-check commands

Required BEFORE workflow trigger (per `pre-mutation-state-check.md` §3 +
`concurrent-production-mutation-ops.md` §6):

```bash
# 1. Concurrent-op guard
gh run list --status in_progress --json name,workflowName

# 2. EIP terraform apply already landed (companion audit verified)
aws ec2 describe-addresses \
  --filters "Name=tag:Name,Values=kitehub-kc-app-fe-eip" \
  --query 'Addresses[].[AllocationId,PublicIp,InstanceId]' --output table
# Expected: 1 row, InstanceId populated, PublicIp = <NEW-EIP>

# 3. Current CF apex state (use new workflow in verify-only mode)
gh workflow run cloudflare-apex-cutover.yml \
  -f mode=verify-only -f confirm='' -f proxied=false
# Read run output: current A/CNAME records for kitehub.me

# 4. Smoke origin BEFORE DNS flip - test the EIP directly via Host header
#    This confirms the EC2 nginx + PM2 stack is healthy on the new IP.
curl -sI --resolve kitehub.me:443:<NEW-EIP> https://kitehub.me/api/health
# Expected: 200 OK

# 5. Verify cert wildcard *.kitehub.me still valid (Let's Encrypt exp 2026-08-13)
echo | openssl s_client -servername kitehub.me -connect <NEW-EIP>:443 2>/dev/null \
  | openssl x509 -noout -dates -subject
```

If step 4 fails -> STOP, debug origin before flip (don't point public DNS at a
broken backend).

## Expected mutation

Mode = `apply`:

- DELETE existing apex A/CNAME (whatever is in place at trigger time)
- CREATE A record `kitehub.me -> <EIP>` with `proxied=<choice>` and `ttl=60`

Atomic via `scripts/cloudflare-dns.sh set-apex <EIP> [--proxied|--dns-only] --ttl 60`.

## Reconciliation table (apply mode)

| Resource | Action | Wave-source | Intent | Decision |
|---|---|---|---|---|
| `kitehub.me` (existing apex A or CNAME) | delete | GAP-573 recovery | Real | Apply |
| `kitehub.me` (new apex A -> EIP) | create | GAP-573 recovery | Real | Apply |

If pre-flight reveals MX / TXT / NS records on apex, they are out of scope - the
`set-apex` command only touches A/CNAME/AAAA on the apex name and leaves all
other record types intact.

## Concurrent-op check per `concurrent-production-mutation-ops.md` §6

| Op 1 | Op 2 (this) | Concurrent OK? | Reason |
|---|---|---|---|
| Terraform EIP apply (companion) | DNS flip | NO - serial | EIP must land first; workflow auto-detects from tag |
| `deploy-production.yml` ECS update | DNS flip | YES if disjoint | DNS doesn't touch ECS / SSM |
| `cloudflare-apex-cutover.yml` itself parallel run | DNS flip | NO | Same CF zone apex; AWS / CF API rate limits |

Manual gate: dev runs `gh run list --status in_progress` and confirms empty before
triggering apply mode.

## Defense-in-depth

- **Cert validity**: Wave 82 wildcard `*.kitehub.me` Let's Encrypt (exp 2026-08-13).
  Valid for both `proxied=false` (terminate at EC2 nginx) and `proxied=true`
  (Cloudflare uses it as origin cert for Full strict). No cert change needed.
- **Cloudflare SSL mode**: pre-flight should `get-ssl-mode` -> expect `full` or
  `strict` (Wave 82 setup). If `flexible`, fix to `strict` before `proxied=true`
  flip (per `tier-3-cutover.yml` Next steps notes).
- **Origin firewall**: `kitehub-kc-app-fe-sg-prod` already permits `0.0.0.0/0:443`
  (per `pre-launch-infra-hardening-checklist.md` §2.7 - 443 is the documented
  carve-out). Whitelist Cloudflare IP ranges only is Phase 1.5+ scope.
- **Page rules cache bypass**: `auth/magic*` + `auth/invite/*` apex cache bypass
  shipped Wave 86 PR #1437 (audit
  `2026-05-16-wave-86-magic-link-bypass-page-rule.md`). Unaffected by apex
  A record swap.

## Smoke admin-login (post-flip; mandatory per `release-deploy-standard.md` §3.1 v1.2.0)

Workflow `cloudflare-apex-cutover.yml` step "Smoke - admin login" runs:

```bash
curl -X POST https://kitehub.me/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}"
```

Acceptable response shapes:

- `HTTP 200` + body has `token` (admin already 2FA-enrolled)
- `HTTP 200` + body has `requires2fa_enrollment` (first-login redirect)

Banned (regression class):

- `HTTP 500` (regression of GAP-517 INET binding / tx rollback poisoning)
- `HTTP 200` + empty body (no token, no 2FA signal)

If smoke fails -> manual rollback per `cloudflare-apex-cutover.yml` is NOT defined
(no rollback target IP cached); operator restores previous state via the same
workflow with `--target_ip <old-IP>` if old IP is still routable, OR re-runs DNS
toward Vercel CNAME for emergency revert.

Required GitHub secrets/vars for smoke step (set by dev pre-trigger):

- `vars.SMOKE_ADMIN_EMAIL` - seeded `PLATFORM_ADMIN` test account email
- `secrets.SMOKE_ADMIN_PASSWORD` - seeded password

If either is unset, the smoke step emits `::warning::` and exits 0 (skips).
**Strongly recommended to set both before first apply.**

## Prior actions verified

| Prior action | Date | Where verified |
|---|---|---|
| Wave 82 apex flip to 54.179.70.37 (proxied=false) | 2026-05-15 | Wave 82 handoff §"Production state (post-cutover)" |
| `*.kitehub.me` Let's Encrypt cert acquired | 2026-05-15 | Wave 82 handoff (exp 2026-08-13) |
| Wave 86 PR #1437 page rules (magic-link + invite) | 2026-05-16 | `documents/04-quality/audits/cloudflare-verification/2026-05-16-wave-86-magic-link-bypass-page-rule.md` |
| GAP-517 admin-login 500 fix | 2026-05-16 | `documents/04-quality/audits/aws-verification/2026-05-16-admin-login-500-rca.md` |
| FE rebuild on EC2 with `requires2fa_enrollment` handler | 2026-05-16 | User session context (commit 447e2167 main HEAD) |

## Pending (this op)

| Action | Owner | Notes |
|---|---|---|
| Land companion terraform EIP + IAM (PR merge + apply) | dev | Prerequisite |
| Set `vars.AWS_CLOUDFLARE_CUTOVER_ROLE_ARN` to new role ARN | dev | After IAM creates |
| Set `vars.SMOKE_ADMIN_EMAIL` + `secrets.SMOKE_ADMIN_PASSWORD` | dev | Optional but recommended |
| Trigger `cloudflare-apex-cutover.yml` mode=verify-only | dev | Read current state |
| Trigger mode=apply with `confirm=APPLY` (proxied as desired) | dev | Mutation |
| Trigger mode=smoke-only OR mode=apply-and-smoke | dev | Health + admin-login |
| File post-mutation audit `2026-05-16-apex-dns-flip-eip-cutover-post.md` | dev or agent | Per `pre-mutation-state-check.md` §3 |
| Verify DNS propagation `1.1.1.1` / `8.8.8.8` / `9.9.9.9` -> EIP (or CF anycast) | dev | TTL 60 -> ~1-2 min |
| Update Wave 82 handoff -> GAP-573 closed; risk note removed | dev | Living docs sync |

## Recommendations

1. **Default `proxied=false`** for first apply (matches Wave 82 state, lower
   blast radius; can flip to proxied via separate `toggle-proxy` invocation
   later).
2. **Apply during low-traffic window** (Phase 1 BETA, no public traffic yet, so
   any time OK).
3. **Always run mode=apply-and-smoke** (combined) so smoke runs before workflow
   exits; if smoke fails, you have immediate signal.
4. **Watch-for**: if smoke `502 Bad Gateway` from Cloudflare with `proxied=true`,
   likely cert mismatch -> revert to `proxied=false` until SSL mode/cert verified
   in `strict` mode.

## References

- Companion terraform audit:
  `documents/04-quality/audits/aws-verification/2026-05-16-kc-app-fe-eip-cf-cutover-role-pre-apply.md`
- Workflow: `.github/workflows/cloudflare-apex-cutover.yml`
- Helper: `scripts/cloudflare-dns.sh set-apex`
- Wave 82 handoff:
  `documents/03-planning/session-handoffs/2026-05-15-post-wave-82-handoff.md`
- GAP-573 source (open follow-up from Wave 82)
- Rule cross-refs:
  `pre-mutation-state-check.md` §3,
  `concurrent-production-mutation-ops.md` §6,
  `release-deploy-standard.md` §3.1 v1.2.0 (smoke admin-login),
  `pre-handoff-self-test-completeness.md` §2.4 (admin flow checklist).
