---
parent_rule: pre-mutation-state-check.md
purpose: deferred-load §Self-test + §Concrete examples for context budget compliance
---

# pre-mutation-state-check — Examples / Self-test

Companion to `.claude/rules/pre-mutation-state-check.md`. Body moved here per Wave 76 Bucket E streamline.

## Concrete examples

### ✅ GOOD — Wave 64 Step E pre-apply (this PR's worked example)

Before triggering `terraform-apply.yml` workflow_dispatch:
1. Run `gh run download` to get plan output
2. Grep for `must be replaced` / `will be created` / phantom indicators
3. Run AWS describe-instances to verify current state
4. Search `documents/04-quality/audits/aws-verification/` for prior apply audits
5. Write `2026-05-12-wave-64-pre-apply-plan-investigation.md` documenting:
   - 11 add (real: HTTPS listener, 2 IAM roles, memory alarm; +cascades)
   - 14 change (real: 1 (lb_listener.http redirect flip); phantom: 13 (random_password ignore_changes, db_instance metadata, schedulers))
   - 4 destroy (real: 2 EC2 AMI bump pre-launch acceptable + 2 cascading)
6. Verdict: safe to apply (pre-launch, no data, all changes desired/beneficial)
7. THEN user triggers `dry_run=false`

### ❌ BAD — apply without investigation

```
agent: "Plan shows 11 add 14 change 4 destroy. Apply?"
user: "Yes"
→ agent triggers workflow_dispatch
```

Risk: drift hidden in "4 destroy" — could be EC2 replacement (data loss on local state), could be IAM role deletion (auth break), could be RDS replacement (DB loss). Without investigation, blind apply.

### ✅ GOOD — Cloudflare DNS PATCH on production

Before `curl PATCH /zones/{id}/dns_records/{id}` to modify existing SPF:
1. GET current record state (verify content + record_id)
2. Search `aws-verification/` audits for prior DNS changes
3. List CF Email Routing rules to confirm SPF still needed for those forwarders
4. Document in audit artifact:
   - Current SPF value, proposed merged value
   - 2 active Email Routing rules depend on `_spf.mx.cloudflare.net`
   - Merge keeps both routing + adds amazonses
5. PATCH

### ❌ BAD — DNS DELETE without state-check

```
agent: "Old SPF record, delete?"
→ DELETE /zones/{id}/dns_records/{id}
```

Risk: didn't check if CF Email Routing depends on it → routing breaks silently.

## Self-test (worked example — Wave 64 Step E)

**Scenario:** 2026-05-12 03:48 UTC — Wave 64 cutover Step E. User-triggered terraform-apply workflow_dispatch `dry_run=true` produced plan summary `11 to add, 14 to change, 4 to destroy`. Agent must decide: apply now (dry_run=false) or hold?

**Apply rule §3 mandate:**
1. ✅ Read current state — `aws ec2 describe-instances` confirmed actual IDs
2. ✅ Search prior actions — found `2026-05-08-wave-43-44-bootstrap-apply.md` + `2026-05-08-current-state.md` + `2026-05-11-wave-61-bucket-a-dns-state.md` + GAP-450 investigation logs
3. ✅ Document findings → `documents/04-quality/audits/aws-verification/2026-05-12-wave-64-pre-apply-plan-investigation.md` (this audit)
4. ✅ Sections present: Scope + Commands + Findings (real-vs-phantom 11/14/4 broken down) + Prior actions table (10 items) + Pending table + Recommendations + References

**Verdict:** all §3 sections present + decisions justified. Rule fires correctly. ✅

**Without this rule:** session 2026-05-12 would have run apply with shallow understanding of 4-destroy items + 14-change items, potentially missing the AMI replacement detail OR misinterpreting phantom updates as real rotations.
