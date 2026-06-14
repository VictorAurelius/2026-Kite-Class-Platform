# GAP-1368: rollback.yml config-drift — stale ALB DNS + hardcoded EC2 instance ID (rollback path hỏng post-ALB-elimination)

**Status:** 🟢 DONE
**Priority:** 🟠 P1
**Domain:** DevOps
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §5.3 rollback drill)
**Resolved:** 2026-06-15 (branch `fix/audit-fixF-devops-2026-06-14`)
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

- [x] `rollback.yml` không còn reference ALB DNS; smoke gate dùng `api.kitehub.me` HTTPS (`PROD_HEALTH_URL`).
- [x] `rollback.yml` lookup EC2 instance ID động theo tag `kitehub-kh-backend` (step `ec2_lookup`, không hardcode).
- [x] `deploy-production.yml` bỏ dead `ALB_DNS` env var.
- [x] Dry-run `rollback.yml` (dry_run=true) plan step không còn lỗi reference — static verify: `python3 yaml.safe_load` PASS + grep `ALB_DNS|ROLLBACK_INSTANCE_ID_KH|elb.amazonaws.com` = 0 live refs (chỉ comment). Live dry-run gated trên GitHub Environment `production` reviewer approval (existing gate).

## Resolution (2026-06-15)

Synced `rollback.yml` to the `deploy-production.yml` Cloudflare-Tunnel + dynamic-lookup pattern:
- Removed `ALB_DNS` + `ROLLBACK_INSTANCE_ID_KH` env vars; added `PROD_HEALTH_URL=https://api.kitehub.me/actuator/health`.
- New step `Lookup current kh-backend instance ID (dynamic — GAP-1368/GAP-482)` resolves the instance via `aws ec2 describe-instances --filters tag:Name=kitehub-kh-backend,instance-state-name=running`. The rollback IAM role already grants `ec2:DescribeInstances` (`iam.tf` `github_rollback_inline` `Ec2DescribeRead`) — no IAM change needed.
- Plan/SSM-send/SSM-poll steps now consume `steps.ec2_lookup.outputs.instance_id`.
- Replaced "Wait for ALB target group health" + "Smoke test (gateway through ALB)" with a "Wait for containers to restart" + Cloudflare-Tunnel smoke (`curl https://api.kitehub.me/actuator/health`).
- `deploy-production.yml`: removed the dead `ALB_DNS` env var (poll step already smokes `api.kitehub.me`).

Verify: `yaml.safe_load` PASS on both workflows; 0 live `ALB_DNS|ROLLBACK_INSTANCE_ID_KH|elb.amazonaws.com` references (comments only).

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (`documents/04-quality/audits/ops-readiness/2026-06-14-ops-readiness-full-audit.md` OPS-002).
- GAP-477 (rollback.yml workflow tạo ra — DONE), GAP-482 (dynamic EC2 lookup — deploy đã fix, rollback miss), GAP-498/501 (ALB poll/TG drift history), GAP-257 (restore/rollback drill thật).
- `cross-flow-bug-class-sweep.md` §1 — fix-then-sweep: khi migrate deploy workflow phải sweep sister rollback workflow cùng class.
