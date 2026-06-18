# Handoff — Production config-bug sweep campaign + AWS teardown

**Date:** 2026-06-18
**Goal (user-set):** "fix hết để production hết bug config" → sau khi live demo xong → "xóa hết stack đi … sẽ phát triển sau"
**Trạng thái cuối session:** ✅ Campaign DONE (toàn bộ config bug fixed + deployed + verified live) → ✅ AWS stack NUKED (giữ terraform-state cho redev).

---

## ✅ Phần 1 — Config-bug sweep campaign (DONE + verified live)

5 PR merged + deploy qua SSM tới 3 EC2, tất cả verified trước khi nuke:

| PR | Nội dung | Verify |
|---|---|---|
| **#2492** | `fetch-secrets.sh`: `OTEL_SDK_DISABLED=true` (Bug C — hết spam span-export) + SePay default key + KITECLASS_CORE_URL | services hết flood log |
| **#2493** | Cross-EC2 gateway→kiteclass-core: `docker-compose.kc.yml` host port `8081:8081` + `KITECLASS_CORE_URL=http://10.0.0.155:8081` + SG self-ref rule `sgr-035af9359bab821be` + FE `INTERNAL_API_URL=10.0.0.129:8080` | landing render 503→200 |
| **#2494** | fetch-secrets heredoc body-precompute fix (`set -u` unbound trên EC2 bash 5.2.15) | tee `.env` OK |
| **#2495** | FE `public.ts`/`auth.ts` port logic (`:9000`→443, hết CSP block) + nginx `/api` proxy cho tenant + app block | courses fetch landing OK |
| **#2489 (prior)** + **SG** | kiteclass-core S3 IAM-role; branding S3 wiring; payment thật 10.000đ (BETA override) + VietQR MB 0988269432 | upload + QR OK |

**Đã verify live trước nuke:** landing 2 demo tenant render đẹp (co-ha-toan + thay-nhi-hoa), courses fetch OK, SSO + 2-tenant isolation, payment QR 10k, full SSO seed (owner/teacher/student × 2 tenant — login 200 với `Demo@2026`).

**Bug A-E (handoff cũ) trạng thái:** A (branding S3) / B (NEXT_PUBLIC_KITECLASS_URL) / C (OTel) / D (payment QR) / E (version drift) — tất cả **đã xử lý** trong các PR trên. Bug D real-QR cần SePay merchant key + webhook tunnel (Phase 1.5, ngoài scope demo).

---

## ✅ Phần 2 — AWS teardown ("Nuke sạch tất cả", 2026-06-18)

Lý do: "tôi đã live demo xong rồi, sẽ phát triển sau". Thực thi qua AWS CLI (profile `dev-admin`), user authorized "Claude chạy hết".

| Resource | Trạng thái |
|---|---|
| 3 EC2 (kh-backend `i-05d7af46d01436b96` / kc-app `i-01ad56b0067d0213b` / kc-app-fe `i-05cfda7c6c60b683f`) | ✅ terminated |
| RDS `kitehub-postgres` | ✅ deleted (`--skip-final-snapshot --delete-automated-backups` — data xóa sạch) |
| EIP `52.221.161.175` (eipalloc-082dbd4253b2a01db) | ✅ released |
| 11 ECR repo `kite/*` (~130 image) | ✅ deleted |
| S3 `kitehub-assets-production` + `kiteclass-files-production` | ✅ deleted |
| 16 Secrets `kitehub/production/*` | ✅ deleted (no recovery) |
| CloudTrail trail `kitehub-main` | ✅ stop-logging + delete |
| S3 `kitehub-cloudtrail-logs` | ⏳ đang empty versions nền (trail đã xóa nên hội tụ) → delete-bucket |
| NAT Gateway / ALB / EBS volume mồ côi / snapshot / RDS backup | ✅ KHÔNG có (kiến trúc EIP+nginx, EC2 public subnet) |
| 🔒 S3 `kitehub-terraform-state-906286017800` | **GIỮ** (cho redev) |

Cost ongoing sau teardown ≈ **$0** (chỉ terraform-state S3 + DynamoDB lock vài cent; VPC/subnet/IGW/SG/IAM role free). Cloudflare DNS KHÔNG đụng (gần free; redev trỏ lại). DynamoDB lock table + IAM role + VPC fabric terraform-managed, giữ — redev `terraform apply` tái dùng.

### Việc đầu tiên khi pickup (redev)
1. `cd infrastructure/terraform-aws && terraform apply` (state còn trong S3 `kitehub-terraform-state` → re-create EC2/RDS/EIP/ECR/Secrets/CloudTrail). **Lưu ý:** instance ID + EIP + RDS endpoint sẽ MỚI → cập nhật mọi nơi hardcode (Cloudflare DNS A record, `KITECLASS_CORE_URL`, FE `INTERNAL_API_URL`, SG self-ref).
2. Push lại 10 image lên ECR (`docker-build-push.yml` workflow_dispatch hoặc tag `v*`).
3. Re-seed: `kitehub/scripts/seed-walk-tenant.sh` + demo-trio seeders (DemoTrioInstanceSeeder + BrandingDataSeeder + DemoAcademicSeeder); **provision lại password** owner/teacher/student (seeders KHÔNG tự set — xem [[demo-tenant-logins]] memory).

---

## 🔧 Deploy mechanism (giữ cho redev — quan trọng)

- **GAP-1482:** `/opt/kite-prod` KHÔNG phải git repo trên EC2 → deploy scripts không `git pull` được. Workaround session này: **curl file từ `raw.githubusercontent.com/.../main`** + `aws ssm send-command` (base64-encode script tránh escaping) + `docker compose recreate`. Mọi config-fix deploy phải làm thế HOẶC restore git trước.
- `scripts/fetch-secrets.sh` ghi `/etc/kite/.env` từ AWS Secrets Manager. **Pattern an toàn (PR #2494):** precompute biến với default Ở THÂN script TRƯỚC heredoc; heredoc chỉ dùng `VAR=${VAR}` (tránh `${VAR:-default}` trong heredoc → `set -u` unbound trên bash 5.2.15 EC2).
- **Topology (sẽ tái tạo, ID mới):** kh-backend EC2 (gateway+subscription+branding+email+admin) priv `10.0.0.129`; kc-app EC2 (kiteclass-core port `8081` + banner-renderer) priv `10.0.0.155`; kc-app-fe EC2 (cả 2 FE qua docker bridge, port-map 4701→3001 / 4700→3000, + nginx) priv `10.0.0.84` + EIP. RDS DB: `kitehub` (subscription) + `kiteclass_shared` (kiteclass-core).
- nginx (`infrastructure/fe-host/nginx-fe.conf`): tenant `*.kitehub.me` + `app.kitehub.me` block đều có `location /api/ { proxy_pass kh_backend_gateway }` (PR #2495).

## Loose ends
- **Cloudflare API token** đã cung cấp trong session trước — **rotate sau khi dùng** (nằm trong transcript).
- cloudtrail-logs bucket empty đang chạy nền — verify GONE khi quay lại (hoặc xóa thủ công nếu còn versions).
- GAP-1481 (wildcard nginx) AC met → flip DONE. GAP-1480/1482/1483 open (redev relevance).
- Defense-script cheat-sheet (`documents/08-thesis/defense/defense-speaker-script-20slide.md` Slide 16): `Demo@2026` đã redact (creds chết sau nuke + repo public). Điền tay khi redemo.
