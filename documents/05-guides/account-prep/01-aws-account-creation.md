# 01 — AWS Account Creation Runbook

**Audience:** Solo dev tạo AWS account lần đầu cho Phase 1 BETA deploy.
**Standards:** AWS Well-Architected (Security + Operational Excellence) · ADR-025 (AWS Singapore `ap-southeast-1`) · `release-deploy-standard.md` §3.4.
**Cross-link:** Blocks `secrets-management-runbook.md` (AWS Secrets Manager) + `email-ses-setup-runbook.md` (SES SMTP) + ECR image push (Wave 37 GAP-398..402) + CloudWatch alarms (Wave 37 GAP-400).
**Estimated time:** ~1.5h (chưa kể đợi billing verification 1-24h).

---

## 1. Trước khi bắt đầu — chuẩn bị

| Item | Yêu cầu |
|------|---------|
| Email | Email mới riêng cho AWS root (KHÔNG dùng email cá nhân chính, KHÔNG dùng workspace email — vì root account không thể đổi email dễ dàng). Recommend: `aws-root@kitehub.vn` (cần domain đã active per `02-domain-registrar.md`) hoặc `kitehub.aws@gmail.com` riêng. |
| Số điện thoại | Số VN active (nhận SMS verify). Phải khác số dùng cho Oracle/GitHub/registrar (tránh recovery lock-out). |
| Thẻ tín dụng | Visa/MasterCard quốc tế (debit OK với một số bank — Techcombank/VCB debit work, Vietcombank debit có khi reject). USD billing. **Cần ≥$1 sẵn** (AWS charge $1 verify rồi refund). |
| KYC giấy tờ | Nếu AWS verification triggers (rare ~5% VN): scan CMND/CCCD/passport + bill điện/nước (proof of address). |
| Activate Founders Pack | Nếu apply credits, cần pitch deck + công ty profile (nếu có). Solo-dev có thể apply qua AWS Activate Portfolio. |

⚠️ **Critical:** AWS root credentials = master key. Nếu mất sẽ KHÔNG khôi phục được nếu chưa setup MFA + recovery phone.

---

## 2. Step-by-step

### 2.1 Root signup (~10 min)

1. Mở [aws.amazon.com](https://aws.amazon.com) → "Create an AWS Account" (góc trên phải).
2. Email: nhập email AWS root chuẩn bị §1.
3. Password: ≥12 chars, mix upper/lower/digit/special. **Lưu vào password manager NGAY** (`03-password-manager.md` chưa setup? — viết tạm vào giấy + đốt sau khi vault ready).
4. AWS Account name: `kitehub-prod` (sẽ rename sau qua Billing Console nếu cần).
5. Contact info: chọn **Personal** (Phase 1 BETA solo dev). Có thể đổi lên Business sau khi có MST.
6. Address: VN địa chỉ thật (KHÔNG dùng địa chỉ giả — billing verification có thể require proof of address).
7. Phone: số VN, click "Send SMS" → nhập 6-digit code.
8. Payment: nhập thẻ tín dụng. AWS sẽ charge $1 verify (thường refund trong 3-5 ngày).
9. Identity verification: nhập số phone lần nữa, AWS gọi automated voice (English) đọc 4-digit PIN → nhập vào browser. Có thể chuyển qua SMS verification nếu voice không nghe được.
10. Support plan: **Basic (Free)** cho Phase 1. Upgrade Developer ($29/month) nếu cần human support.
11. Sign in to console → confirm successful.

### 2.2 Region default lock to ap-southeast-1 (~2 min)

Per ADR-025 Phase 1 BETA = AWS Singapore.

1. Console top-right region selector → **Asia Pacific (Singapore) ap-southeast-1**.
2. Bookmark URL containing `region=ap-southeast-1` để tránh tab mở random region khác.
3. AWS CLI sẽ config sau §2.5 với `region = ap-southeast-1` default.

### 2.3 MFA cho root user (BẮT BUỘC, ~5 min)

⚠️ **DO THIS BEFORE creating any IAM user or resource.** Root without MFA = single factor = AWS recommendation 5/5 critical violation.

1. Console search "IAM" → IAM service → bottom right "Add MFA" cho root user (sẽ có warning banner).
2. Chọn **Authenticator app** (TOTP) — Google Authenticator / 1Password / Bitwarden Authenticator. **DO NOT** chọn SMS (SIM swap risk).
3. Scan QR code bằng app → nhập 2 consecutive 6-digit codes.
4. **Lưu recovery codes vào password manager + giấy printed lưu offline** (két sắt / nhà mẹ / cloud encrypted).
5. Sign out + sign back in để verify MFA prompt fires.

### 2.4 Billing alarm $5 / $50 / $200 (BẮT BUỘC, ~10 min)

⚠️ **AWS bill shock = #1 nỗi lo solo dev.** 3 thresholds catch các loại lỗi khác nhau.

1. Console → Billing & Cost Management → Budgets → Create budget.
2. Budget type: **Cost budget**.
3. Tạo 3 budgets riêng:

| Budget | Threshold | Alert at | Notify |
|--------|-----------|----------|--------|
| `aws-bill-low` | $5/month | 100% actual | aws-root@... |
| `aws-bill-medium` | $50/month | 80% actual + 100% actual | aws-root@... + aws-root@... |
| `aws-bill-high` | $200/month | 50% forecast + 80% actual + 100% actual | aws-root@... + ops-admin@... (sau khi tạo) |

4. CloudWatch billing alarm (legacy backup): Region `us-east-1` ONLY (AWS billing metrics chỉ ở us-east-1) → CloudWatch → Alarms → Create alarm → Metric "EstimatedCharges" → threshold $5 → SNS topic email.
5. Test: gửi email test từ Budget alert config "Send test email" → verify nhận được trong 1-2 min.

⚠️ **Billing alerts không stop spend automatically** — chỉ notify. Để kill spend cần shutdown manual hoặc Service Control Policy (Phase 2 với AWS Organizations).

### 2.5 1st IAM admin user (BẮT BUỘC, ~10 min)

⚠️ **NEVER use root user for daily ops.** Root chỉ dùng cho: account close, billing/payment changes, IAM root account changes, support plan changes.

1. IAM service → Users → Create user.
2. User name: `solo-dev-admin` (hoặc tên thật của bạn — chỉ ASCII, hyphens OK, no spaces).
3. Provide user access to AWS Management Console: ✅ check. Auto-generated password OR custom (chọn auto + email).
4. Permissions: Attach policies directly → **AdministratorAccess** AWS-managed policy.
5. Tags (optional): `purpose=daily-admin`, `created-by=root-solo`.
6. Create user → ghi xuống console URL (có format `https://<accountId>.signin.aws.amazon.com/console`) + temporary password.
7. **Sign out root, sign in với `solo-dev-admin`.** Đổi password → enable MFA TOTP cho user này (separate device hoặc separate entry trong cùng app — tag rõ ràng "AWS solo-dev-admin").
8. Verify access: console → S3 / EC2 / IAM list pages → load không bị 403.
9. Bookmark IAM console URL trong browser cho daily use.

⚠️ **Disable root access keys** (nếu có). Root nên KHÔNG có programmatic access keys. IAM → root user → Security credentials → Delete root access keys nếu present.

### 2.6 IAM access key cho CI/CD (~5 min)

CI cần programmatic access cho Terraform + ECR push (Wave 37). Tạo IAM user thứ 2:

1. IAM → Users → Create user `ci-deploy`.
2. ❌ KHÔNG provide console access (programmatic only).
3. Permissions: tạm Attach `AdministratorAccess` cho Phase 1; refine Phase 2 với least-privilege policy theo Wave 37 GAP-411 sizing matrix.
4. Create access key: type "Application running outside AWS" → Next → Create.
5. **Download .csv NGAY** + lưu vào `Kite-Production` vault (`03-password-manager.md`). KHÔNG email, KHÔNG commit, KHÔNG paste vào Slack.
6. Setup GitHub Actions secret: GitHub repo → Settings → Secrets → Actions → New: `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` (per Wave 37 GAP-396 OIDC; có thể migrate sang OIDC sau Phase 1 stable).

### 2.7 AWS CLI config (~5 min)

```bash
# Cài AWS CLI v2 trên dev machine (WSL kite-dev)
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install

# Config profile cho daily admin
aws configure --profile kitehub-admin
# AWS Access Key ID: <từ solo-dev-admin user, tạo access key tương tự §2.6 nhưng cho admin>
# AWS Secret Access Key: <paste>
# Default region: ap-southeast-1
# Default output: json

# Test
aws sts get-caller-identity --profile kitehub-admin
# → Account: <12-digit>, UserId: AIDA..., Arn: arn:aws:iam::<account>:user/solo-dev-admin
```

⚠️ Access key cho admin user lưu vault `Kite-Personal` (separate from CI). Rotate mỗi 90 ngày per `secrets-management-runbook.md` §rotation.

### 2.8 Activate Founders Pack apply (optional, ~30 min — nếu apply)

AWS Activate cung cấp $1k-$25k credits cho startup VN. Founders pack ($1k) requirements thấp.

1. [aws.amazon.com/activate](https://aws.amazon.com/activate/) → Apply Founders.
2. Form: company name (có thể dùng tên cá nhân nếu chưa có MST), website (`kitehub.vn` or kite GitHub repo URL), description (paste pitch deck Wave 37 GAP-413 1-line summary + 5 bullets).
3. AWS Activate Console URL phải gắn với account đã tạo §2.1.
4. Approval thường 5-10 ngày. Credits expire 1-2 năm.
5. Check status: AWS console → Activate → Credits.

---

## 3. Verification checklist

Sau khi xong tất cả §2:

- [ ] Root user MFA enabled + recovery codes lưu offline
- [ ] 3 billing budgets created + test email received
- [ ] CloudWatch billing alarm `us-east-1` armed
- [ ] IAM `solo-dev-admin` user có MFA + console access verified
- [ ] IAM `ci-deploy` user có access key lưu password manager + GitHub Actions secret set
- [ ] Root access keys deleted (nếu trước đó có)
- [ ] AWS CLI `kitehub-admin` profile pass `sts get-caller-identity`
- [ ] Region default lock `ap-southeast-1` trong console + CLI

---

## 4. What can go wrong

| Symptom | Root cause | Fix |
|---------|-----------|-----|
| Billing verification stuck >24h | VN debit card không được accept | Đổi sang Visa credit, hoặc liên hệ AWS Support (basic plan vẫn có ticket) |
| Phone verification voice không nghe được | Carrier block international | Switch sang SMS verification ở step §2.1.9 |
| MFA TOTP code reject sau setup | Time skew on device | Sync device time qua NTP, retry |
| "Account verification in progress" >7 ngày | KYC trigger | Scan CMND/CCCD + utility bill → upload qua Support case |
| `aws sts get-caller-identity` returns `InvalidClientTokenId` | Access key not activated yet (5-10s lag) hoặc copy-paste sai | Wait 30s, retry. Re-download .csv nếu vẫn lỗi |
| Billing alarm không fire dù bill > threshold | Alarm region không phải `us-east-1` | Re-create CloudWatch alarm trong `us-east-1` |
| AWS Activate apply rejected | Solo-dev không match "startup with traction" | Apply lại sau 30 ngày với MVP screenshots + 5+ users feedback |

---

## 5. Out-of-scope (Phase 2+)

- AWS Organizations multi-account (sandbox / staging / prod separate accounts)
- AWS SSO + Identity Center cho team mở rộng
- Service Control Policies enforce region/service restrictions
- AWS Control Tower auto-baseline
- Cost Anomaly Detection (Phase 1 budget alert đủ; Anomaly Detection cần ≥30 ngày data)

---

## 6. Cross-link

- `documents/05-guides/deploy/secrets-seeding-runbook.md` — assumes admin IAM user + CLI ready (first-time seed); rotation runbook lives in `documents/05-guides/operations/secrets-rotation-runbook.md`
- `documents/05-guides/deploy/email-ses-setup-runbook.md` — needs SES domain verification post §2.1
- `documents/02-architecture/adr/ADR-025-aws-singapore-free-tier.md` — region rationale
- `02-domain-registrar.md` — domain → SES domain identity → MX records (Bước 5 dns-setup-runbook)
- `03-password-manager.md` — vault structure cho 5+ AWS credentials (root, admin, ci-deploy access keys, MFA recovery codes)
- Wave 37 GAP-411 sizing matrix — IAM least-privilege Phase 2 refine

---

## 7. Log

- **2026-05-07** — Runbook created. Phase 1 GAP-394 sub-runbook 1/4. Standards: AWS Well-Architected Security + Operational Excellence pillars; ADR-025 region lock. Out-of-scope: AWS Organizations + SSO + Control Tower → Phase 2.
