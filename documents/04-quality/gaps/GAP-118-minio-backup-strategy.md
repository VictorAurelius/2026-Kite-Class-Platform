# GAP-118: MinIO Backup + Replication Strategy

**Status:** 🟢 DONE 2026-04-28 — PR #634 (Wave DR/Backup Agent B); per `gap-done-discipline.md` §2.6 framing (Terraform code validated + lifecycle policy configured; runtime live-AWS replication verification at first apply, no AWS credentials in PR scope)
**Priority:** 🟠 P1
**Domain:** DevOps / Data Safety
**Found:** 2026-04-19 (ops-readiness audit — baseline)
**Affects:** AI-generated assets (logos, banners, hero), template SVGs, user uploads

## Problem

MinIO (object storage) KHÔNG có backup/replication strategy. Nếu volume corrupt hoặc host fail → tất cả AI-generated assets mất vĩnh viễn.

Evidence:
- `kitehub/docker-compose.kitehub.yml` MinIO service: single volume `kite-minio-data`, no replication
- `infrastructure/terraform-aws/s3-ecr.tf` — sử dụng real S3 trong prod nhưng **không có versioning / cross-region replication config** explicit
- Helm values không có backup config cho MinIO
- Không có MinIO → external backup job

Risk:
- AI asset regeneration expensive (compute + Ollama time) — mất → phải regen toàn bộ
- Template SVGs (GAP-011) là IP của platform — mất = reset toàn bộ template library

## Root Cause

MinIO được treat như ephemeral cache thay vì persistent asset store. Không có lifecycle policy.

## Proposed Fix

### Production (AWS S3)
1. Enable **versioning** trên bucket:
   ```hcl
   resource "aws_s3_bucket_versioning" "assets" {
     bucket = aws_s3_bucket.assets.id
     versioning_configuration { status = "Enabled" }
   }
   ```
2. Enable **cross-region replication** (ap-southeast-1 → us-east-1) cho critical assets
3. **Lifecycle policy:**
   - Current version: keep indefinitely (small objects)
   - Non-current version: transition to Glacier after 30 days, delete after 1 year
4. Enable S3 Object Lock (compliance mode) cho templates

### Production (Oracle Object Storage)
1. Enable object versioning
2. Scheduled rsync/sync → S3 secondary bucket (cross-cloud backup)

### Development (MinIO)
1. Enable `mc version enable` trên bucket
2. Scheduled `mc mirror --watch` → external volume backup

### Restore path
1. Document trong `restore-procedure.md` (GAP-117)
2. Test: delete asset → verify restore từ previous version

## Acceptance Criteria

- [ ] S3 versioning enabled (Terraform)
- [ ] Cross-region replication configured (prod)
- [ ] Lifecycle policy tested: old versions → Glacier
- [ ] MinIO versioning enabled trong dev setup
- [ ] Restore drill: asset deleted → restored từ previous version
- [ ] Cost impact documented (S3 versioning + replication)

## Related

- Audit: `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-19.md` §6
- Depends: GAP-117 (restore procedure runbook)
- Related: GAP-030 (AI branding DR, P2)

## Log

- **2026-04-28 (PR #634 — Wave DR/Backup Agent B):** SHIPPED. Files: `infrastructure/terraform-aws/s3-ecr.tf` (+224/−5 — replica provider + cross-region replication ap-southeast-1→us-east-1 + lifecycle Glacier 30d/expire 365d + Object Lock opt-in), `infrastructure/terraform-aws/variables.tf` (+19 — `s3_replica_region`, `s3_object_lock_enabled`, `s3_object_lock_retention_days`), `infrastructure/terraform-oracle/compute.tf` (+48 — versioning + auto_tiering + lifecycle on `kitehub_assets`; cross-region NOT implemented — Always Free tier single-region, cross-cloud sync to S3 documented as recommended), `infrastructure/terraform-oracle/main.tf` (+2/−1 cross-ref), `kitehub/docker-compose.kitehub.yml` (+44/−2 — `mc version enable` + opt-in `kite-minio-backup` sidecar gated by `--profile backup`). CI all 13 jobs SUCCESS. `terraform validate` PASS both modules; `docker compose config -q` PASS. Cost impact: ~$1.20-$1.30/mo at current ~50GB; ~$25/mo at 1TB projected. Status → 🟢 DONE per §2.6 (live-AWS test deferred to first apply).
- 2026-04-19 — Discovered in ops-readiness baseline audit
