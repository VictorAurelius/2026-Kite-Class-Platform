# GAP-934: Cloudflare apex `kitehub.me` DNS managed manually — needs `terraform import`

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (DNS records exist + working; gap is IaC drift between Cloudflare dashboard state and `infrastructure/terraform-cloudflare/dns.tf`)
**Domain:** DevOps
**Found:** 2026-06-04 (Wave flow-kh1 G3 parity audit, row 2 of 4 checklist items)
**Affects:** `infrastructure/terraform-cloudflare/dns.tf`

## Problem

Wave flow-kh1 G3 parity audit verified the apex `kitehub.me` A/CNAME record + `app.kitehub.me` subdomain DNS exist in Cloudflare dashboard. The terraform file `dns.tf:108` has an inline comment:

> "NOTE: apex kitehub.me + app.kitehub.me records currently managed MANUALLY via Cloudflare dashboard (pre-terraform-cloudflare init). Import to terraform tracked Wave alb-terraform-cleanup-1 follow-up."

This is IaC drift — same class as Wave 105 GAP-717 (AWS Secrets Manager manual creation per runbook without terraform IaC declaration). Live DNS works today, but a destroy/recreate cycle of the terraform-cloudflare stack would not recreate apex records, and the dashboard state has no version control / review trail.

## Proposed Fix

1. Read current apex + `app.kitehub.me` record values from Cloudflare dashboard (or `dig kitehub.me +short` + `dig app.kitehub.me +short`)
2. Add `cloudflare_record.apex` + `cloudflare_record.app_subdomain` resource blocks in `dns.tf` mirroring the dashboard values
3. Run `terraform import cloudflare_record.apex <zone_id>/<record_id>` + same for `app_subdomain`
4. Verify `terraform plan` clean (no diff) post-import
5. Document import procedure in `documents/05-guides/deploy/cloudflare-terraform-import-runbook.md`

## Acceptance Criteria

- [ ] `dns.tf` declares `cloudflare_record.apex` + `cloudflare_record.app_subdomain` matching live values
- [ ] `terraform import` runbook documented
- [ ] `terraform plan` post-import → no diff
- [ ] Comment at dns.tf:108 removed (no longer "managed manually")

## Related

- Wave flow-kh1 wave plan §G3 row 2 (Cloudflare DNS verify-link reachable)
- Sister: Wave 105 GAP-717 (AWS Secrets Manager same IaC drift class)
- Per `local-fix-production-parity-check.md` §2 row 3 — manual creation = same-PR IaC follow-up
- Per `agent-aws-access.md` §4.3 Tier 3 — manual external state changes require terraform IaC reconciliation
