# EC2 Cost Review — quy trình rà soát right-sizing hàng tháng

**Cập nhật:** 2026-05-15
**Phụ trách:** Solo dev (Phase 1 BETA) → SRE (Phase 1.5+)
**Cadence:** Hàng tháng (ngày 1, sau khi nhận email digest từ Lambda)
**Liên quan:** GAP-414 right-sizing automation · `infrastructure/terraform-aws/cost-monitoring.tf` · `documents/04-quality/cost-reports/`

---

## 1. Mục tiêu

Tiết kiệm chi phí EC2 trong Phase 1 BETA bằng cách downsize các instance over-provisioned + bảo vệ uptime bằng cách upsize các instance under-provisioned. Mọi quyết định size đều dựa trên dữ liệu CloudWatch + Cost Explorer chứ không phải estimate ban đầu.

Hai nguồn tín hiệu chính:

1. **CloudWatch alarm tự động**: per-EC2 alarm fire khi avg CPU ≤ 20% suốt 7 ngày → gửi SNS notify đến `vannkite@outlook.com`. Đây là tín hiệu real-time, mỗi instance có alarm riêng (`kitehub-kh-backend-low-cpu-7d`, `kitehub-kc-app-low-cpu-7d`, `kitehub-kc-app-fe-low-cpu-7d`).
2. **Lambda monthly digest**: ngày 1 hàng tháng 08:00 UTC (15:00 ICT), Lambda `kitehub-ec2-cost-report` chạy → fetch Cost Explorer + CloudWatch metrics 30 ngày → publish HTML digest lên cùng SNS topic. Mỗi instance có khuyến nghị Downsize / OK / Upsize.

---

## 2. Trigger — khi nào chạy quy trình này

| Tín hiệu | Hành động |
|---|---|
| Nhận email digest hàng tháng từ SNS `kitehub-cost-alerts` | Đọc §3 — xử lý từng instance theo khuyến nghị |
| Nhận email alarm `*-low-cpu-7d in ALARM state` giữa tháng | Đọc §4 — đánh giá adhoc, có thể downsize trước digest tháng |
| `*-low-cpu-7d` chuyển từ ALARM → OK | Bỏ qua — instance đã quay lại workload bình thường |
| Cost forecast tăng > 30% so với tháng trước | Đọc §5 — root-cause analysis trước khi downsize |

---

## 3. Quy trình đọc digest hàng tháng

### 3.1 Cấu trúc digest

Email subject: `[kitehub] EC2 cost + right-sizing report — YYYY-MM`

Body chứa bảng HTML với 7 cột: Instance / Type / State / Cost (last month) / Avg CPU (30d) / Avg Mem (30d) / Recommendation.

Có 4 nhãn khuyến nghị:

| Recommendation | Ý nghĩa | Hành động đề xuất |
|---|---|---|
| `Downsize candidate` | Avg CPU < 20% trên 30 ngày | Cân nhắc giảm instance type 1 bậc (xem §6) |
| `OK` | 20% ≤ CPU ≤ 60%, mem < 85% | Không cần hành động |
| `Upsize candidate (CPU pressure)` | Avg CPU > 60% | Cân nhắc tăng instance type 1 bậc |
| `Upsize candidate (memory pressure)` | Avg mem > 85% (cần CloudWatch agent) | Upsize ưu tiên — risk OOM |
| `Insufficient data` | CW không có datapoint (instance mới hoặc stopped) | Chờ thêm 30 ngày, không hành động |

### 3.2 Lưu ý về cột Cost

- Cost Explorer group theo `INSTANCE_TYPE` chứ không theo từng instance-id riêng (limitation của AWS Cost Explorer API)
- Nếu 2 instance cùng `t3.medium`, cột Cost sẽ hiển thị tổng cost của cả 2 cho cùng type
- Cho Phase 1 BETA với 3 EC2 (3 type khác nhau) hiện tại = đủ độ chính xác

### 3.3 Lưu ý về cột Avg Mem

CloudWatch agent (CWAgent namespace, `mem_used_percent` metric) hiện tại **chưa được cấu hình** trên các EC2 — xem comment header trong `infrastructure/terraform-aws/cloudwatch.tf`. Khi chưa config agent, cột Avg Mem sẽ là `n/a` và recommendation chỉ dựa vào CPU.

Khi triển khai CW agent (tracked riêng), cột này sẽ tự động có dữ liệu mà không cần thay đổi Lambda.

---

## 4. Xử lý alarm low-CPU adhoc

Khi nhận email từ SNS với subject chứa `ALARM` + tên alarm `kitehub-{kh-backend|kc-app|kc-app-fe}-low-cpu-7d`:

1. **Verify alarm còn tồn tại**:
   ```bash
   aws cloudwatch describe-alarms \
     --alarm-names kitehub-kh-backend-low-cpu-7d \
     --query 'MetricAlarms[*].[AlarmName,StateValue,StateReason]' \
     --output table
   ```
2. **Kiểm tra CPU thực tế 7 ngày qua**:
   ```bash
   aws cloudwatch get-metric-statistics \
     --namespace AWS/EC2 \
     --metric-name CPUUtilization \
     --dimensions Name=InstanceId,Value=<instance-id> \
     --start-time $(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%S) \
     --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
     --period 86400 \
     --statistics Average \
     --output table
   ```
3. **Quyết định**:
   - Nếu confirm CPU thực sự thấp + workload không spike vào cuối tháng / dịp đặc biệt → tiến hành §6 downsize
   - Nếu nghi ngờ workload sẽ tăng (sắp invite beta cohort mới, demo, kiểm thử tải) → giữ size, chờ digest tháng kế

---

## 5. Cost forecast tăng đột biến

Nếu cost forecast tăng > 30% so với tháng trước:

1. Mở AWS Cost Explorer console → filter `Project=Kite` → group by `Service`
2. Identify service tăng cost nhất (thường EC2, RDS, hoặc S3 transfer)
3. Cross-reference với deployment timeline: có wave nào tăng resource (storage, replica, ...) không?
4. Nếu legitimate growth → ghi nhận vào `documents/04-quality/cost-reports/YYYY-MM.md` template
5. Nếu unexpected → file gap mới, root-cause trước khi action

---

## 6. Quy trình downsize (terraform var change)

**KHÔNG bao giờ resize EC2 qua AWS Console** — terraform state sẽ drift. Luôn dùng terraform.

### 6.1 Chuẩn bị

1. Đọc lại `documents/05-guides/deploy/right-size-stress-test.md` (GAP-447 prerequisite) — đảm bảo đã test memory headroom cho t3.medium → t3.small chưa
2. Verify CW agent cấu hình + memory alarms `kitehub-{kh-backend|kc-app}-memory-high` không đang ALARM
3. Chọn cửa sổ maintenance: tối thứ 7 hoặc sáng chủ nhật (low traffic Phase 1 BETA)

### 6.2 Thực hiện

1. Edit `infrastructure/terraform-aws/variables.tf` hoặc instance-specific variable trong `ec2.tf` / `ec2-kc-app.tf`:
   ```hcl
   # Ví dụ kh-backend t3.medium → t3.small
   instance_type = "t3.small"  # was t3.medium per GAP-414 downsize 2026-MM-DD
   ```
2. Chạy plan trên branch riêng:
   ```bash
   cd infrastructure/terraform-aws
   AWS_PROFILE=dev-admin terraform plan -out=downsize.tfplan
   AWS_PROFILE=dev-admin terraform show downsize.tfplan | grep -E '(must be replaced|will be updated|aws_instance)'
   ```
3. **Quan trọng**: `instance_type` thay đổi sẽ trigger stop → modify → start (NOT replace), giữ nguyên data root volume + EIP. Đọc kỹ output plan để confirm `must be replaced` KHÔNG xuất hiện.
4. Trước apply, đảm bảo serialize theo `concurrent-production-mutation-ops.md` §3.1:
   - Không có `deploy-production.yml` đang chạy
   - Không có SSM SendCommand đang chạy trên instance này
5. File pre-apply audit theo `pre-mutation-state-check.md` §3:
   ```
   documents/04-quality/audits/aws-verification/YYYY-MM-DD-ec2-downsize-<instance>.md
   ```
6. Apply qua workflow_dispatch (không local apply):
   ```bash
   gh workflow run terraform-apply.yml -f confirm=APPLY -f dry_run=false
   ```
7. Đợi terraform complete → verify instance state `running` + `instance_type` mới:
   ```bash
   aws ec2 describe-instances --instance-ids <id> \
     --query 'Reservations[].Instances[].[InstanceId,InstanceType,State.Name]'
   ```
8. Smoke test: SSH / SSM vào instance → check docker services up, memory usage:
   ```bash
   aws ssm start-session --target <instance-id>
   # trong session:
   docker ps
   free -h
   uptime
   ```
9. Watch CloudWatch 2-3 giờ tiếp theo:
   - `mem_used_percent` < 85% (nếu CW agent đã config)
   - HTTP health endpoint `/actuator/health` returns 200
   - Không có ALARM `*-memory-high` fire

### 6.3 Rollback nếu memory pressure xuất hiện

1. Edit lại `*.tf` ngược lại instance type cũ
2. `terraform apply` qua workflow_dispatch
3. Mất ~3-5 phút downtime; documented trong incident response runbook

---

## 7. Quy trình upsize

Tương tự §6 nhưng theo chiều ngược lại. Lưu ý:

- Upsize **luôn** đi kèm rebuild base AMI metadata, có thể cần re-run cloud-init scripts. Verify `user_data` không có blocking step
- Cost tăng ngay lập tức — ghi nhận vào cost report tháng đó
- Sau upsize, monitor 1 tuần để confirm CPU/memory drop về vùng OK trước khi quyết định "size mới là baseline"

---

## 8. Log + tracking

Mọi quyết định downsize / upsize đều log vào:

- `documents/04-quality/cost-reports/YYYY-MM.md` — monthly review template (xem 2026-06-template.md)
- `documents/04-quality/audits/aws-verification/YYYY-MM-DD-ec2-{downsize|upsize}-<instance>.md` — pre/post-apply state
- ROADMAP `### 🚀 Next Action` — nếu là action lớn, surface lên top-level

---

## 9. Liên quan

- **Rule:** `.claude/rules/concurrent-production-mutation-ops.md` §3.1 (serialize terraform + SSM)
- **Rule:** `.claude/rules/pre-mutation-state-check.md` §3 (pre-apply audit artifact)
- **Rule:** `.claude/rules/terraform-apply-retry-reconfirm.md` (apply retry discipline)
- **Runbook:** `documents/05-guides/deploy/right-size-stress-test.md` (memory headroom test trước downsize)
- **Gap:** [GAP-414](../../04-quality/gaps/GAP-414-ec2-right-sizing-monthly-review.md)
- **Gap:** [GAP-447](../../04-quality/gaps/GAP-447-rds-storage-encryption-cmk-evaluation.md) (memory safety net — sister scope)
- **Terraform:** `infrastructure/terraform-aws/cost-monitoring.tf` (alarms + Lambda)
- **Lambda source:** `infrastructure/terraform-aws/lambdas/ec2-cost-report/handler.py`
- **Template:** `documents/04-quality/cost-reports/2026-06-template.md` (monthly review fill template)
