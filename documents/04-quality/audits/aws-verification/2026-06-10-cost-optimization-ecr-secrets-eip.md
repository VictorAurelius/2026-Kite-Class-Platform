---
title: AWS Verification — Cost optimization (ECR + Secrets Manager + EIP idle)
status: complete
created: 2026-06-10
phase: Phase 1 BETA (stack STOPPED)
audience: mixed
---

# AWS Verification Report — Cost optimization (ECR / Secrets / EIP)

## Scope

User-flagged: AWS vượt credit, bill ~$5.78/tháng (ECR $2.08 + Secrets Manager $1.60 + VPC/IPv4 idle $1.03). Yêu cầu check ECR + dọn dẹp giảm chi phí. Account `906286017800` / `ap-southeast-1`, stack hiện STOPPED.

## Commands run (Tier 1 read-only per `agent-aws-access.md` §2.1)

| Command | Purpose |
|---|---|
| `aws sts get-caller-identity` | xác thực profile dev-admin |
| `aws ecr describe-repositories` + `describe-images` (10 repo) | đếm image + size + tag |
| `aws ecr get-lifecycle-policy` | đọc policy hiện tại |
| `aws ec2 describe-addresses` | trạng thái EIP |
| `aws ec2 describe-nat-gateways` | nguồn public IP khác (none) |
| `aws secretsmanager list-secrets` (metadata only, KHÔNG get-secret-value) | đếm secret |

## Results

### ECR — không phải 176GB, mà ~20GB billed (layer dedup)

`describe-images` cộng dồn ~176GB nhưng AWS bill theo **unique layer (dedup)** ≈ 20GB → $2.08. Vấn đề thật = **số lượng image quá lớn**:

| Repo | imgs | untagged | sig/att (`sha256-*.sig/.att`) | latest/main |
|---|---:|---:|---:|---:|
| kitehub-gateway | 513 | 134 | 346 | 1 |
| kitehub-email | 523 | 142 | 348 | 1 |
| kitehub-branding | 513 | 140 | 340 | 1 |
| kitehub-subscription | 507 | 134 | 340 | 1 |
| kitehub-admin | 509 | 132 | 344 | 1 |
| kiteclass-core | 505 | 132 | 340 | 1 |
| kiteclass-frontend | 511 | 134 | 344 | 1 |
| kitehub-frontend | 429 | 126 | 274 | 1 |
| kiteclass-gateway | 200 | 64 | 104 | 1 |
| kitehub-platform | 0 | 0 | 0 | 0 |
| **Tổng** | **~4,210** | **~1,238** | **~2,780** | 9 |

### Lifecycle policy — rò ở signature/attestation

Policy (`ecr.tf` `aws_ecr_lifecycle_policy.cleanup`, áp cho tất cả repo):
1. Xóa untagged >7 ngày ✅
2. Giữ 20 tag `sha-*` ✅
3. Giữ 10 tag `main/test/latest/pr-*` ✅

→ **Lỗ hổng:** cosign `sha256-*.sig`/`.att` là *tagged* với prefix `sha256-` (≠ `sha-`) → KHÔNG khớp rule nào → tích vô hạn (~2,780 image). Version tag `0.x/v*` cũng kept-forever (hiện chưa có → OK).

### Secrets Manager — thực tế 14 secrets (bill snapshot cũ chỉ 4)

Tất cả `kitehub/production/*`, terraform-managed (`secrets.tf` for_each). 14 × $0.40 = **~$5.60/mo** (sẽ là khoản lớn nhất, vượt ECR). 4 secret CHƯA BAO GIỜ access (`LastAccessed: null`):
- `ses-smtp-credentials` — SES bị thay bằng Resend (`resend-api-key` access 2026-05-26) → **drop an toàn**
- `ai-anthropic-api-key`, `ai-openai-api-key` — AI Phase 1 = TEMPLATE-mock, chưa dùng (sẽ cần FULL_AI Phase 1.5+) → borderline
- `internal-api-secret` — chưa access, cần verify code reference trước khi drop

### EIP `52.221.161.175` — KHÔNG phải "idle EIP rảnh"

Associated vào EC2 `kitehub-kc-app-fe` (STOPPED), terraform-managed (`vpc.tf`/`ec2-kc-app.tf`, GAP-573), **Cloudflare apex DNS trỏ vào IP này**. Charge $1.03 = idle-vì-instance-stopped, KHÔNG phải unassociated. Release = vỡ DNS + terraform drift → **KHÔNG dọn**.

### Đã sạch sẵn

- **ALB: 0** (khoản $18/mo lớn nhất trong audit 2026-05-11 §F3 đã bị xóa rồi ✅)
- NAT gateway: 0
- EBS/S3/Route53: nhỏ

## Findings

| Khoản | Bill | Lever | Rủi ro | Hành động |
|---|---:|---|---|---|
| ECR backlog | $2.08 | one-time delete ~2,780 sig/att + untagged cũ; giữ latest/main + 20 sha | THẤP (terraform-safe — quản repo+policy, không quản image) | prune CLI + sửa `ecr.tf` thêm rule xóa `sha256-` |
| Secrets Manager | ~$5.60 (đang tăng) | drop `ses-smtp` (chắc) ± ai keys; sửa `secrets.tf` for_each | TRUNG (terraform + apply; có recovery window 7-30d) | terraform edit → user trigger apply |
| EIP idle | $1.03 | giữ nguyên (DNS-pinned) | CAO nếu release | KHÔNG dọn |

## Execution log (2026-06-10)

### ECR prune — DONE ✅

User approved prune. `scripts/aws/ecr-prune.sh --apply` (multi-pass, keep-set by digest = latest/main + 20 sha + cosign sig/att of kept + manifest-list children):

| | Trước | Sau |
|---|---:|---:|
| Tổng images | ~4,210 | ~895 |
| Đã xóa | — | ~3,715 (−79%) |
| latest/main/sha-e7444b4 | ✅ | ✅ verified còn nguyên |

Tail ~30 (manifest-cascade orphan) để lifecycle policy "expire untagged >7d" quét nốt. Mọi delete-set còn lại là child manifest của kept index (AWS chặn xóa đúng — `ImageReferencedByManifestList`).

### Root cause — KHÔNG phải lỗi CI push

CI (`docker-build-push.yml`) push + cosign sign + SBOM/provenance attest mỗi merge = đúng/ý đồ, không phải bug. Dư thừa = **lifecycle policy gap**: `ecr.tf` chỉ match prefix `sha-`/`main`/`test`/`latest`/`pr-` + expire untagged; cosign tag `sha256-*.sig/.att` (prefix `sha256-` ≠ `sha-`) KHÔNG khớp rule nào → tích vô hạn (~2,780). Buildx attestation cũng tạo OCI index → untagged children nhân số image.

### ecr.tf patch — SHIPPED (chờ user trigger apply)

Thêm `rulePriority = 4` expire `sha256-*` giữ 40 gần nhất (= 20 build × 2 artifact). `terraform fmt` OK; prefix `sha256-` disjoint `sha-` (ECR validate sẽ pass). Apply qua `terraform-apply.yml` (user trigger per `dev-authorized-terraform-trigger.md`).

### secrets.tf — HOLD (cần user chốt)

`ses-smtp-credentials` (placeholder rỗng, `LastAccessed=null`, `fetch-secrets.sh` không fetch) → drop được $0.40/mo. NHƯNG `application-production.yml:30` vẫn default `EMAIL_PROVIDER:ses` + `SESEmailService.java` còn tồn tại → migration sang Resend (ADR-025 Stream A) chưa chốt hẳn. KHÔNG tự drop; flag cho user. 14 secrets còn lại đa số cần thật (db/jwt/jwt-challenge/encryption/seed-admin/resend/sepay/zalo/totp/staff-invite/internal-api/cloudflare/rabbitmq).

### EIP — giữ nguyên

`52.221.161.175` associated kc-app-fe (stopped), Cloudflare apex DNS-pinned. Release = vỡ DNS. KHÔNG dọn.

## Compliance

- ✅ `agent-aws-access.md` §2.1 Tier 1 read-only only; §2.2 banned `get-secret-value` KHÔNG gọi (chỉ list-secrets metadata)
- ✅ §5 audit artifact: file này
- ⏳ Tier 3 deletes (ECR batch-delete / terraform apply): chờ user explicit approval

## References

- `documents/04-quality/audits/aws-verification/2026-05-11-actual-cost-vs-estimate.md` §F3 stopped-stack burn
- `infrastructure/terraform-aws/ecr.tf` lifecycle policy
- `infrastructure/terraform-aws/secrets.tf` + `ec2-kc-app.tf` EIP
- `.claude/rules/agent-aws-access.md` Tier 1/2/3
