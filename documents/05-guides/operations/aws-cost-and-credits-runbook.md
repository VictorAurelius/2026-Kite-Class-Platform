---
title: AWS Cost & Credits Runbook — giảm chi phí + kiếm credits
audience: dev
created: 2026-06-15
scope: operations / cost-control
related:
  - .claude/rules/aws-cost-guard.md
  - .claude/rules/retention-policy-completeness.md
  - documents/04-quality/audits/aws-verification/2026-06-15-cost-reduction.md
---

# AWS Cost & Credits Runbook

Hướng dẫn xử lý chi phí AWS cho KiteHub (account `906286017800`, region `ap-southeast-1`).
Viết sau sự cố bill tăng đột biến 2026-06-15.

---

## 0. TL;DR

| Việc | Trạng thái |
|---|---|
| ECR 206GB → 0 (món tốn nhất $43) | ✅ đã dọn |
| CloudWatch (dashboard + alarm + log 3.1GB) | ✅ đã tắt |
| CI sửa: chỉ push ECR khi deploy | ✅ đã sửa |
| RDS + EC2 + EBS + EIP + secrets (~$15-18/mo) | ⏳ giữ stopped (chưa teardown) |
| Bill tháng 6 $40 (credit đã cạn) | ⏳ xin courtesy credit |

**Để bill các tháng sau ~$0:** hoặc (A) teardown toàn bộ phần còn lại, hoặc (B) giữ stopped + nhớ re-stop RDS mỗi tuần + xin credits che phần rỉ.

---

## 1. Bối cảnh sự cố (2026-06-15)

| Tháng | Gross | Sau credit | Ghi chú |
|---|---|---|---|
| May | $36 | **~$0** | credit AWS che hết |
| June (15 ngày) | $40.32 | **$40.32** | ❌ credit cạn → tiền thật |

**Thủ phạm June:** ECR $43 (storage 206GB / 2.746 image — CI push `push:main` mỗi merge) · CloudWatch $6.3 · RDS $6.2 (stopped vẫn tính storage) · EC2-Other $3.5 (EBS+EIP) · Secrets $2.7 · VPC $1.7 (IPv4/EIP).

Root cause chính: `docker-build-push.yml` có `push: branches:[main]` → mỗi merge push 10 multi-arch image → ECR tích vô hạn.

---

## 2. Cost-reduction đã thực thi

### 2.1 ECR (đã dọn — món lớn nhất)
```bash
# Xoá tất cả image (rebuildable từ CI khi deploy)
export AWS_PAGER="" AWS_PROFILE=dev-admin
for repo in $(aws ecr describe-repositories --region ap-southeast-1 --query 'repositories[].repositoryName' --output text); do
  ids=$(aws ecr list-images --region ap-southeast-1 --repository-name "$repo" --query 'imageIds[].imageDigest' --output text)
  [ -z "$ids" ] && continue
  args=""; for d in $ids; do args="$args imageDigest=$d"; done
  aws ecr batch-delete-image --region ap-southeast-1 --repository-name "$repo" --image-ids $args >/dev/null
done
```

### 2.2 CloudWatch (đã tắt)
```bash
aws cloudwatch delete-dashboards --region ap-southeast-1 --dashboard-names kitehub-phase-1-overview
A=$(aws cloudwatch describe-alarms --region ap-southeast-1 --query 'MetricAlarms[].AlarmName' --output text)
aws cloudwatch delete-alarms --region ap-southeast-1 --alarm-names $A
for lg in /aws/ec2/kite-prod /aws/ec2/kite-prod-kc /aws/lambda/kitehub-production-rotate-secret-handler /aws/lambda/kitehub-ec2-cost-report /aws/ssm/kite-deploy; do
  aws logs delete-log-group --region ap-southeast-1 --log-group-name "$lg" 2>/dev/null
done
# Giữ lại: CloudTrail trail kitehub-main (security baseline) + CW log group cloudtrail (61MB, rẻ)
```
> Khi bật lại stack: re-provision alarm/dashboard qua `terraform apply` (đừng tạo tay).

### 2.3 CI (đã sửa — chống tái phát)
`docker-build-push.yml`: bỏ `push: branches:[main]`. Push ECR chỉ trên `tag v*.*.*` HOẶC `workflow_dispatch` (lúc deploy). PR vẫn build-only (`push:false`) để validate Dockerfile. Quy tắc: `.claude/rules/aws-cost-guard.md`.

---

## 3. Phần còn rỉ tiền + maintenance (keep-stopped)

Sau khi dọn ECR+CloudWatch, vẫn còn ~$15-18/mo nếu giữ stack:

| Resource | $/mo | Ghi chú |
|---|---|---|
| RDS `kitehub-postgres` 20GB stopped | ~$2.3 | **TỰ BẬT LẠI sau 7 ngày** |
| EC2 ×3 stopped + EBS 80GB gp3 | ~$6.4 | EC2 stopped vẫn trả EBS |
| EIP `52.221.161.175` idle | ~$3.6 | IPv4 charge |
| Secrets Manager ×16 | ~$6.4 | $0.40/secret |

### ⚠️ Cảnh báo RDS 7-ngày
AWS **cưỡng bức bật** RDS stopped sau tối đa 7 ngày → tính compute tới khi stop lại. Với keep-stopped PHẢI re-stop định kỳ:
```bash
bash scripts/aws/stop-stack.sh --force   # chạy ~mỗi tuần
```
> Cân nhắc đặt reminder lịch (Google Calendar) "stop AWS RDS" mỗi 6 ngày.

### Kiểm tra trạng thái nhanh
```bash
export AWS_PAGER="" AWS_PROFILE=dev-admin
aws rds describe-db-instances --region ap-southeast-1 --query 'DBInstances[].{id:DBInstanceIdentifier,status:DBInstanceStatus}' --output table
aws ec2 describe-instances --region ap-southeast-1 --query 'Reservations[].Instances[].{id:InstanceId,state:State.Name}' --output table
```

---

## 4. Teardown về ~$0 (tuỳ chọn — destructive)

Khi quyết định không cần stack AWS một thời gian (dev/demo bằng local Docker):
```bash
export AWS_PAGER="" AWS_PROFILE=dev-admin; R=ap-southeast-1
# 1. Snapshot RDS giữ data TRƯỚC khi xoá
aws rds create-db-snapshot --region $R --db-instance-identifier kitehub-postgres \
  --db-snapshot-identifier manual-pre-teardown-$(date +%Y%m%d)
# 2. Xoá RDS instance (giữ snapshot)
aws rds delete-db-instance --region $R --db-instance-identifier kitehub-postgres --skip-final-snapshot
# 3. Terminate EC2 (EBS DeleteOnTermination tự xoá)
aws ec2 terminate-instances --region $R --instance-ids i-05d7af46d01436b96 i-01ad56b0067d0213b i-05cfda7c6c60b683f
# 4. Release EIP
aws ec2 release-address --region $R --allocation-id <alloc-id-của-52.221.161.175>
# 5. Xoá secrets (recovery window 7 ngày)
for s in $(aws secretsmanager list-secrets --region $R --query 'SecretList[].Name' --output text); do
  aws secretsmanager delete-secret --region $R --secret-id "$s" --recovery-window-in-days 7
done
```
> Re-provision khi cần: `cd infrastructure/terraform-aws && terraform apply` (per `release-deploy-standard.md` §9 — human-trigger).
> Monthly sau teardown: ~$0 (chỉ vài cent snapshot storage).

---

## 5. Kiếm credits (sinh viên = lợi thế)

Xếp theo độ dễ + giá trị:

| # | Nguồn | Giá trị | Điều kiện | Link / cách |
|---|---|---|---|---|
| 1 | **GitHub Student Developer Pack** | AWS credits + ~$200k perks | Email SV / thẻ SV (UTC) | https://education.github.com/pack |
| 2 | **AWS Educate** | Credits + lab, không cần thẻ TD | Sinh viên | https://aws.amazon.com/education/awseducate |
| 3 | **AWS Activate Founders** | **$1,000 credits** | "Startup" (KiteHub SaaS đủ), chưa nhận trước | https://aws.amazon.com/activate → Founders self-serve |
| 4 | **One-time courtesy credit** | ~$40 (hoá đơn tháng 6) | Account nhỏ, lần đầu vượt | AWS Console → Support → **Billing case**: "credits unexpectedly exhausted, request one-time courtesy credit for June ~$40" |

**Thứ tự khuyến nghị:**
1. **#4** ngay — xin waiver $40 đang lo (AWS hay duyệt lần đầu/số nhỏ).
2. **#3** Activate $1,000 — credit lớn nhất, đủ chạy beta + demo khóa luận nhiều tháng.
3. **#1** GitHub Student Pack — bền, dùng lâu dài.

**Lưu ý:**
- Các đăng ký này cần **tự làm** (xác thực danh tính/SV, OAuth) — không tự động hoá được.
- Credit **không chặn rỉ tiền**, chỉ "trả hộ". Vẫn nên keep-stopped + re-stop RDS, hoặc teardown.
- Nếu có Activate $1,000 → thoải mái bật full stack demo khóa luận không lo bill.

---

## 6. Monitoring định kỳ (chống tái phát)

### Mỗi session start
`collect-state.sh` đã snapshot ECR image count + EC2/RDS state. Nếu ECR > ~30 image → điều tra (CI lẽ ra chỉ push lúc deploy).

### Cost Explorer — theo tháng / theo service
```bash
export AWS_PAGER="" AWS_PROFILE=dev-admin
aws ce get-cost-and-usage --time-period Start=$(date +%Y-%m-01),End=$(date -d '+1 month' +%Y-%m-01) \
  --granularity MONTHLY --metrics UnblendedCost NetUnblendedCost \
  --group-by Type=DIMENSION,Key=SERVICE --output table
```
(NetUnblendedCost = sau credit; nếu = UnblendedCost → credit đã cạn.)

### Billing alarm (tùy chọn)
Đặt 1 budget alert AWS Budgets ~$5/tháng để cảnh báo sớm (Budgets có free tier cho 2 budget đầu) — nhẹ hơn nhiều so với CloudWatch alarm đã xoá.

---

## 7. Tham chiếu
- Rule: `.claude/rules/aws-cost-guard.md` (CI deploy-only push)
- Rule: `.claude/rules/retention-policy-completeness.md` (lifecycle cap)
- Audit: `documents/04-quality/audits/aws-verification/2026-06-15-cost-reduction.md`
- Start/stop stack: `scripts/aws/start-stack.sh` / `scripts/aws/stop-stack.sh`
