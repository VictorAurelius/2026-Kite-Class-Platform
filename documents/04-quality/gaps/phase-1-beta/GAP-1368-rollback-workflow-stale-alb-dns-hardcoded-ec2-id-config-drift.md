# GAP-1368: rollback.yml config-drift — stale ALB DNS + hardcoded EC2 instance ID (rollback path hỏng post-ALB-elimination)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §5.3 rollback drill)
**Affects:** `.github/workflows/rollback.yml` (+ dead `ALB_DNS` env var trong `deploy-production.yml`)

## Problem

`deploy-production.yml` đã được migrate đúng theo Wave aws-restore-1 (ALB eliminated → Cloudflare Tunnel ingress) + GAP-482 (dynamic EC2 lookup theo tag thay hardcoded ID) + GAP-491 (CloudWatch SSM log streaming). Nhưng **`rollback.yml` KHÔNG được sync cùng**:

- `env.ALB_DNS: kitehub-alb-224105328.ap-southeast-1.elb.amazonaws.com` — ALB này **đã bị eliminate** Wave aws-restore-1 (xác nhận trong comment của `cloudwatch-p0-alarms.tf` + `deploy-production.yml` "ALB decommissioned 2026-06").
- Step "Wait for ALB target group health" + "Smoke test (gateway through ALB)" → `curl http://${ALB_DNS}/actuator/health` → ALB DNS không còn route được → **smoke gate FAIL** → rollback workflow không thể tự xác nhận healthy (báo lỗi sai hoặc treo).
- `env.ROLLBACK_INSTANCE_ID_KH: i-0b65c3947d36cae61` hardcoded — `deploy-production.yml` đã bỏ hardcode (GAP-482: ID stale sau khi EC2 bị replace qua AMI bump). EC2 thực tế đã đổi (GAP-566 nhắc i-05cfda7c6c60b683f). Hardcoded ID = rollback gửi SSM tới instance sai/không tồn tại.

Hệ quả: rollback (năng lực P0 của deploy pipeline per rubric §5.3) **thực chất broken** — không chỉ là "drill chưa chạy" (GAP-257) mà workflow có config drift làm nó fail nếu trigger. GAP-477 (DONE) tạo ra workflow; drift này phát sinh SAU đó khi aws-restore-1 + GAP-482 migrate deploy-production nhưng quên rollback.

Phụ: `deploy-production.yml:35` vẫn khai `ALB_DNS` env var nhưng KHÔNG dùng (poll step đã chuyển sang `api.kitehub.me`) — dead var, cùng class cleanup.

## Proposed Fix

Sync `rollback.yml` theo pattern `deploy-production.yml`: (1) bỏ `ALB_DNS`, thay smoke gate bằng `curl https://api.kitehub.me/actuator/health` (Cloudflare Tunnel); (2) thay hardcoded `ROLLBACK_INSTANCE_ID_KH` bằng dynamic lookup `aws ec2 describe-instances --filters tag:Name=kitehub-kh-backend,instance-state-name=running` (per GAP-482); (3) xóa dead `ALB_DNS` khỏi `deploy-production.yml`. Đồng thời chạy 1 dry-run rollback để verify.

## Acceptance Criteria

- [ ] `rollback.yml` không còn reference ALB DNS; smoke gate dùng `api.kitehub.me` HTTPS.
- [ ] `rollback.yml` lookup EC2 instance ID động theo tag (không hardcode).
- [ ] `deploy-production.yml` bỏ dead `ALB_DNS` env var.
- [ ] Dry-run `rollback.yml` (dry_run=true) pass plan step không lỗi reference.

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (`documents/04-quality/audits/ops-readiness/2026-06-14-ops-readiness-full-audit.md` OPS-002).
- GAP-477 (rollback.yml workflow tạo ra — DONE), GAP-482 (dynamic EC2 lookup — deploy đã fix, rollback miss), GAP-498/501 (ALB poll/TG drift history), GAP-257 (restore/rollback drill thật).
- `cross-flow-bug-class-sweep.md` §1 — fix-then-sweep: khi migrate deploy workflow phải sweep sister rollback workflow cùng class.
