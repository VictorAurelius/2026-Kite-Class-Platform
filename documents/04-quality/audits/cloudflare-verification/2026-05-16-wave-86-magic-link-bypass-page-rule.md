---
title: Cloudflare Verification — Magic-link + invite endpoint cache bypass Page Rules
status: complete
created: 2026-05-16
applied_at: 2026-05-16T04:32:00Z
phase: phase-1-beta
wave: 86
bucket: E-AC4
gaps: [GAP-584]
---

## 🟢 APPLIED 2026-05-16

**Apply method:** Local `terraform apply` trong `infrastructure/terraform-cloudflare/` (cross-workspace; `terraform-apply.yml` workflow chỉ chạy `terraform-aws/`).

**Pre-flight checks per `dev-authorized-terraform-trigger.md` §2:**
- §2.1 Concurrent ops: empty ✅
- §2.2 Audit artifact: this doc ✅
- §2.3 Targeted plan: `terraform plan -target=cloudflare_page_rule.magic_link_bypass_cache -target=cloudflare_page_rule.invite_bypass_cache` → `Plan: 2 to add, 0 to change, 0 to destroy` ✅ match prediction
- §2.4 Real apply: `terraform apply tfplan.cf-pagerules` → `Apply complete! Resources: 2 added, 0 changed, 0 destroyed.` ✅
- §2.5 Post-apply Tier 1 verify (below) ✅

**Reconciliation table per `pre-mutation-state-check.md` §3.5:**

| Resource | Plan action | Wave-source | Intent | Decision |
|---|---|---|---|---|
| cloudflare_page_rule.magic_link_bypass_cache | create | Wave 86 (PR #1437) | Real | ✅ Apply |
| cloudflare_page_rule.invite_bypass_cache | create | Wave 86 (PR #1437) | Real | ✅ Apply |

Other resources in workspace (DNS records / DMARC) NOT in scope: Wave 77 Bucket A with placeholder DKIM selectors — defer to that wave's apply.

**Credential source:** AWS Secrets Manager `kitehub/production/cloudflare-api-token` (Tier 2 read) + zone ID resolved via Cloudflare API.

**Post-apply Tier 1 verify (Cloudflare API):**

```bash
curl -H "Authorization: Bearer $CF_TOKEN" \
  "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/pagerules" | \
  python3 -c "..."
```

Result:
```
id=1240d9cd935a prio=2 status=active target=*kitehub.me/auth/invite/*  actions=cache_level=bypass
id=9da36ffad57f prio=1 status=active target=*kitehub.me/auth/magic*    actions=cache_level=bypass
```

✅ Both Page Rules **active** + `cache_level=bypass`.

**Open follow-ups:**
1. State backend migration — `providers.tf` backend block commented out → state currently local-only at `infrastructure/terraform-cloudflare/terraform.tfstate` (gitignored per `.gitignore`). Risk: state loss → next plan re-create resources. File follow-up gap to uncomment backend + `terraform init -migrate-state`.
2. `curl -sI` smoke test khi EC2 backend back up → verify `CF-Cache-Status: BYPASS` from origin response (currently EC2 stopped; redirect chain not testable until cohort window).
3. Defense-in-depth B-layer (`Cache-Control: no-store` header trong Spring Boot AuthController) — separate GAP-584 AC #2 follow-up.

---

# Cloudflare Verification — Magic-link + Invite Cache Bypass Page Rules (Wave 86 Bucket E-AC4)

## Scope

Bucket E-AC4 (P0 BLOCKER chặn Wave 86 Bucket G invite) yêu cầu Cloudflare Page Rule `cache_level = bypass` cho 2 URL pattern:

1. `*kitehub.me/auth/magic*` — magic-link authentication endpoints
2. `*kitehub.me/auth/invite/*` — invite acceptance flow

**Risk eliminated:** Sim cell 19 audit (`2026-05-15-pre-wave-86-simulation-3axis.md`) chỉ ra Cloudflare aggressive caching mặc định cache query-string URLs ~2h TTL. Magic-link URLs chứa single-use token trong query → tenant A click invite → tenant A's redirect cached → tenant B click khác minute đó → CF serve cached redirect → tenant B authenticated as tenant A → **cross-tenant onboarding breach + PDPL violation**.

**Defense-in-depth note:** Page Rule là edge-layer hard guard (Layer A). Spring Boot response header `Cache-Control: no-store` (Option B per GAP-584) là origin-layer defense (Layer B) — tracked separately, không chặn scope này.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2 + Cloudflare API Tier 1 equivalent)

**Pre-apply baseline (operator MUST run trước khi trigger apply):**

```bash
# 1. Baseline current cache behavior cho magic-link endpoint (KHÔNG có Page Rule)
curl -sI "https://kitehub.me/auth/magic?token=test-baseline-$(date +%s)" 2>&1 \
  | grep -iE '(cf-cache-status|cache-control|cf-ray)'
# Expected baseline (pre-fix): có thể thấy "cf-cache-status: HIT" hoặc "MISS" — đây chính là risk

# 2. List existing Page Rules trên zone (verify không có conflict với priority 1 + 2)
curl -sX GET "https://api.cloudflare.com/client/v4/zones/${CLOUDFLARE_ZONE_ID}/pagerules" \
  -H "Authorization: Bearer ${CLOUDFLARE_API_TOKEN}" \
  | jq '.result[] | {id, priority, status, targets, actions}'

# 3. Check zone status + plan (Page Rule quota: Free=3, Pro=20)
curl -sX GET "https://api.cloudflare.com/client/v4/zones/${CLOUDFLARE_ZONE_ID}" \
  -H "Authorization: Bearer ${CLOUDFLARE_API_TOKEN}" \
  | jq '.result | {name, status, plan: .plan.name, page_rule_quota}'
```

## Findings

### Real changes (planned apply will create)

| # | Resource | Action | Root cause | Risk |
|---|----------|--------|-----------|------|
| 1 | `cloudflare_page_rule.magic_link_bypass_cache` | CREATE | GAP-584 P0 BLOCKER — eliminate cross-tenant cache leak vector | Zero risk on prod traffic — `cache_level=bypass` chỉ disable CF edge cache, KHÔNG ảnh hưởng routing/origin/SSL. Worst-case: minor latency increase cho endpoint vốn đã uncacheable. |
| 2 | `cloudflare_page_rule.invite_bypass_cache` | CREATE | Same as #1 — invite flow shares magic-link risk class | Same risk profile as #1 |

### Phantom updates

None — clean create, no state metadata refresh.

### Verdict

**SAFE TO APPLY.** Both Page Rules là additive resources, không modify/replace existing Cloudflare resources (dns.tf SPF/DKIM/DMARC records untouched). `cache_level = bypass` là well-documented Cloudflare action; no rollback complexity (DELETE Page Rule = instant revert).

**Production-data risk:** ZERO — endpoints affected currently có 0 production traffic (Phase 1 BETA pre-launch). Even nếu có traffic, bypass cache cho auth endpoint là correct security posture industry-wide.

**Quota check:** Free plan Cloudflare = 3 Page Rules max. Current usage (verify via §Commands command 2): assumed 0 (no Page Rules created prior to this PR). 2 new rules + existing 0 = 2/3 total — fits Free tier. Operator MUST verify via command 2 trước apply.

## Prior actions verified (per `audit-to-gap-pipeline.md` §2.8)

| Action | When | Where verified |
|--------|------|----------------|
| Cloudflare zone setup `kitehub.me` | Wave 77 Bucket A | `infrastructure/terraform-cloudflare/dns.tf` + `2026-05-13-resend-dns-prep.md` |
| Cloudflare provider v4.40 configured | Wave 77 Bucket A | `infrastructure/terraform-cloudflare/providers.tf` |
| Variable `cloudflare_zone_id` defined + populated | Wave 77 Bucket A | `infrastructure/terraform-cloudflare/variables.tf` line 9-12 |
| Cloudflare API token với DNS:Edit scope | Wave 77 Bucket A | Per `terraform.tfvars.example` — operator-managed, NOT committed |
| GAP-584 P0 BLOCKER filed | 2026-05-15 | `documents/04-quality/gaps/GAP-584-magic-link-cloudflare-cache-bypass.md` (Wave 86 Bucket A simulation cell 19) |
| Wave 86 plan E-AC4 scope locked | 2026-05-15 | `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md` §3 |

## Pending (this op)

| Action | Owner | Notes |
|--------|-------|-------|
| Baseline curl test (pre-apply) | Operator (user) | Run §Commands #1 trước trigger apply để capture baseline `cf-cache-status` header |
| Page Rule quota check | Operator (user) | Run §Commands #2 + #3 — verify Free tier 2/3 quota usage |
| Concurrent op check (per `concurrent-production-mutation-ops.md`) | Agent + operator | Confirm zero in-flight terraform-apply.yml / DNS PATCH ops trên kitehub.me zone trước trigger |
| `terraform plan -out=tfplan` cho page_rules.tf | Operator local | Capture plan output cho PR body (this PR) |
| `terraform apply tfplan` via human-triggered workflow | Operator (user) | Per `release-deploy-standard.md` §9 — agent KHÔNG tự apply |
| Post-apply self-test (§Self-test below) | Operator (user) | Verify `CF-Cache-Status: BYPASS` post-deploy |
| GAP-584 Status flip → DONE post-verify | Agent (next session) | Per `gap-done-discipline.md` §2 — AC #1 verified |
| Cross-link `pre-launch-infra-hardening-checklist.md` Cat 5 | Agent (next session) | Add row "Magic-link endpoints bypass CF cache verified" |

## Recommendations

1. **APPLY** — Page Rules là additive, low-risk, P0 BLOCKER critical path
2. **Verify post-apply** via §Self-test commands below
3. **Watch-for:**
   - If `CF-Cache-Status` shows `HIT` post-apply → Page Rule pattern mismatch (e.g., target `*kitehub.me/auth/magic*` doesn't match `https://www.kitehub.me/auth/magic/...`). Adjust target hoặc add www variant.
   - If quota exceeded (Free tier 3 max) → use Cloudflare Rules → Cache Rules (newer engine, higher quota) thay cho Page Rules. Tracked future migration.
4. **Defense-in-depth follow-up:** GAP-584 AC #2 (Spring Boot `Cache-Control: no-store` header) — file follow-up gap nếu chưa có; tracked next session.

## Self-test (post-apply — operator runs)

```bash
# 1. Verify Page Rule deployed (Cloudflare API)
curl -sX GET "https://api.cloudflare.com/client/v4/zones/${CLOUDFLARE_ZONE_ID}/pagerules" \
  -H "Authorization: Bearer ${CLOUDFLARE_API_TOKEN}" \
  | jq '.result[] | select(.targets[0].constraint.value | contains("/auth/magic") or contains("/auth/invite")) | {id, status, target: .targets[0].constraint.value, cache: .actions[0].value}'

# Expected output: 2 rules với status="active", cache="bypass"
# {
#   "id": "...",
#   "status": "active",
#   "target": "*kitehub.me/auth/magic*",
#   "cache": "bypass"
# }
# {
#   "id": "...",
#   "status": "active",
#   "target": "*kitehub.me/auth/invite/*",
#   "cache": "bypass"
# }

# 2. Verify cache bypass via response headers (2 sequential requests)
curl -sI "https://kitehub.me/auth/magic?token=verify1-$(date +%s)" \
  | grep -iE '(cf-cache-status|cache-control|cf-ray)'

sleep 2

curl -sI "https://kitehub.me/auth/magic?token=verify2-$(date +%s)" \
  | grep -iE '(cf-cache-status|cache-control|cf-ray)'

# Expected post-apply: both responses show "CF-Cache-Status: BYPASS" (hoặc DYNAMIC)
# FAIL signal: "CF-Cache-Status: HIT" — Page Rule không match, debug target pattern

# 3. Verify invite endpoint
curl -sI "https://kitehub.me/auth/invite/test-token-$(date +%s)" \
  | grep -iE 'cf-cache-status'
# Expected: CF-Cache-Status: BYPASS hoặc DYNAMIC
```

## References

- Workflow run: TBD (operator triggers post-PR-review)
- PR: Wave 86 Bucket E-AC4 — `wave-86-bucket-e-ac4-cf-magic-link-bypass`
- GAP-584: `documents/04-quality/gaps/GAP-584-magic-link-cloudflare-cache-bypass.md`
- Sim audit cell 19: `documents/04-quality/audits/persona-review/2026-05-15-pre-wave-86-simulation-3axis.md`
- Wave 86 plan E-AC4: `documents/03-planning/waves/wave-2026-05-15-86-rc1-tag-preflight.md` §3
- Prior CF audit: `documents/04-quality/audits/cloudflare-verification/2026-05-13-resend-dns-prep.md`
- Rules applied:
  - `pre-mutation-state-check.md` §3 (this audit artifact)
  - `release-deploy-standard.md` §9 (human-triggered apply)
  - `agent-aws-access.md` §2 Tier 1 read-only commands
  - `concurrent-production-mutation-ops.md` (no in-flight mutation conflict)
  - `gap-done-discipline.md` §3 PARTIAL exit ramp (DONE flip gated on operator apply + post-verify)
