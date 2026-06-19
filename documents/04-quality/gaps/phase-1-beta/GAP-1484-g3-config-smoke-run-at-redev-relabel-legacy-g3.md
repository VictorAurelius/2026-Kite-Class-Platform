# GAP-1484: Chạy G3-config smoke tại redev + re-label legacy "G3 ✅" rows

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** DevOps
**Phase:** phase-1-beta
**Found:** 2026-06-19 (PR #2497 G3-config gate split — follow-up)
**Affects:** `documents/03-planning/roadmap/flow-verification-campaign.md` §4 (22 flow rows) + `scripts/smoke-prod-config.sh` + `documents/05-guides/deploy/prod-deploy-config-registry.md`

## Problem

PR #2497 tách **G3 → G3-config** (config-parity đa-host, deploy-gated) **+ G3-infra** (TLS/LB/cert/DNS). Hai việc còn lại, đều **AWS-gated** (stack teardown #2496 → smoke chạy qua SSM cần EC2 live):

1. **G3-config chưa từng verify cho flow nào.** Các row campaign §4 ghi "G3 ✅ 2026-06-07" thực ra là curl-via-gateway-`:9000` trên local single-host = **functional-parity** (giờ thuộc G2★ theo model mới), KHÔNG phải G3-config (đa-host topology) hay G3-infra (TLS/DNS). G3-config chỉ được fix reactively khi deploy (#2489-2496), chưa có systematic pass.

2. **Legacy "G3 ✅ 2026-06-07" labels stale** dưới model mới. Legend đã có note grandfather, nhưng row-level label chưa re-phân tầng (functional vs config vs infra).

Lưu ý quan trọng: **G3-config là deploy-scoped, KHÔNG per-flow** — config topology dùng chung mọi flow → 1 lần `smoke-prod-config.sh` cover hết, KHÔNG cần re-audit 22 flow riêng.

## Proposed Fix

Tại **lần redev kế** (`terraform apply` + push image):
1. Chạy `bash scripts/smoke-prod-config.sh --eip <new-ip> --tenant <slug>` 1 lần → bảng PASS/FAIL.
2. Batch-fix mọi FAIL (+ cập nhật registry §3 hàng `derive=hardcoded` với IP/ID mới per §4 redev checklist).
3. Smoke PASS = **G3-config ✅ cho toàn bộ flow cùng lúc**.
4. Verify 5 assumption agent đoán (courses/login endpoint, S3 bucket names, secret keys, private IP `10.0.0.129`/`10.0.0.155`).
5. Re-label campaign §4 legacy "G3 ✅" → tách rõ G3-functional (✅ local) / G3-config (smoke result) / G3-infra (AWS GAP-612).

## Acceptance Criteria

- [ ] `smoke-prod-config.sh` chạy live tại redev, exit 0 (hoặc batch-fix tới 0 FAIL)
- [ ] 5 assumption verified + registry/script cập nhật IP/endpoint thật
- [ ] Campaign §4 rows re-labeled phân tầng G3-functional/config/infra
- [ ] (Optional) Layer 2 de-hardcode: thêm `*_private_ip` + `eip_public` terraform output (registry §5 đề xuất)

## Related

- Filed in: PR #2497 (`feature/g3-config-gate-parity-2026-06-19`)
- Cluster AWS-gated redev: [[GAP-1455]] (INTERNAL_API_URL live-verify) + GAP-612 (AWS restore blocker)
- Gate model: `flow-verification-campaign.md` §1 (G3-config/G3-infra split) + `local-fix-production-parity-check.md` §2.5
