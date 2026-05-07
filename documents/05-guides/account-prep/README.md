# Account Prep Runbooks — Phase 1 BETA

**Audience:** Solo dev / first-time deploy operator chuẩn bị các tài khoản upstream trước khi chạy first-deploy.
**Closes (PARTIAL):** GAP-394 — 4 missing runbooks blocking actual first-deploy execution.
**Cross-link:** Các runbook này là **prerequisite upstream** cho `documents/05-guides/operations/{secrets-management,dns-setup,email-ses-setup}-runbook.md`.

---

## 1. Mục đích

Phase 1 BETA deploy artifact cluster (Wave 33 SHIPPED 2026-05-07) đã ship code/runbook/scripts cho DNS/SES/secrets/seed. Tuy nhiên **4 user-executed prep steps** thiếu runbook dedicated:

1. **AWS account creation** — tạo từ root signup → MFA → billing alarm → 1st IAM admin user
2. **Domain registrar** — chọn + đăng ký domain (`kitehub.vn`, `kiteclass.vn`) với KYC + transfer-lock
3. **Password manager policy** — vault structure cho ~30 credentials Phase 1
4. **KiteHub superadmin first-login** — post-seed: invite email → first-login → MFA enroll → ops-admin invite chain

Solo-dev mode + first-deploy = high risk forget-step (vd: quên billing alarm → AWS bill shock; quên domain transfer-lock → domain có thể bị chuyển).

---

## 2. Sequence (T-7 → T-0)

```
T-7 ngày  ┌─ 01-aws-account-creation.md           (1.5h, blocks SES + Secrets Manager)
          ├─ 02-domain-registrar.md               (1h, blocks DNS cutover)
T-5 ngày  └─ 03-password-manager.md               (1.5h, blocks credential storage cho 4 ops trên)
          ┊
          ┊  [Wave 33+38 deploy artifacts ship code/runbooks consuming các account trên]
          ┊
T-0 deploy   04-kitehub-superadmin-first-login.md (2h, runs AFTER seed + email/SES live)
```

**Critical path:** 01 + 02 chạy song song (independent); 03 chạy sau (cần ≥1 credential từ 01/02 để populate vault); 04 chạy sau khi infra live + seed run.

---

## 3. Files

| # | File | Estimated time | Blocks |
|---|------|----------------|--------|
| 1 | [`01-aws-account-creation.md`](01-aws-account-creation.md) | ~1.5h | `secrets-management-runbook.md`, `email-ses-setup-runbook.md`, ECR push, CloudWatch |
| 2 | [`02-domain-registrar.md`](02-domain-registrar.md) | ~1h | `dns-setup-runbook.md`, Cloudflare nameserver migrate |
| 3 | [`03-password-manager.md`](03-password-manager.md) | ~1.5h | Lưu trữ ~30 credentials Phase 1 |
| 4 | [`04-kitehub-superadmin-first-login.md`](04-kitehub-superadmin-first-login.md) | ~2h | First admin có thể vào dashboard production |

**Total:** ~6h docs work.

---

## 4. Standards & cross-links

- AWS Well-Architected (Operational Excellence + Security pillars)
- VN PDPL 2023 + Decree 13/2023 — KYC documents (CMND/CCCD/passport) acceptance policy
- VN Luật An ninh mạng 2018 + Decree 53/2022 — data localization (chọn AWS Singapore `ap-southeast-1`)
- `.claude/rules/release-deploy-standard.md` §3.4 (MAJOR + first PRODUCTION checklist)
- `.claude/rules/logs-format-standard.md` (no credentials trong logs/screenshots)
- `documents/02-architecture/adr/ADR-025-aws-singapore-free-tier.md` (Phase 1 BETA cloud platform = AWS Singapore)

---

## 5. Out-of-scope

- AWS Organizations + multi-account → Phase 2
- HashiCorp Vault self-hosted → Phase 3
- Status page (GAP-373) + CDN (GAP-371) account prep — covered by their own runbooks (`incident-comms-runbook.md` + `cloudflare-setup.md`)
- Hardware security keys (YubiKey) → optional, mentioned trong `03-password-manager.md` as recommended-but-not-required
