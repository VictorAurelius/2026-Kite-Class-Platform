# Account Prep Runbooks — Phase 1 BETA

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) · [`.claude/rules/deployment-naming-convention.md`](../../../.claude/rules/deployment-naming-convention.md) §2
**Audience:** Solo dev / first-time deploy operator chuẩn bị các tài khoản upstream trước khi chạy first-deploy.
**Closes:** GAP-394 — 7 runbooks shipped cover Phase 1 BETA account-prep matrix (Wave 84 Bucket C 2026-05-15 ship 3 còn thiếu: Cloudflare / Resend / Vercel).
**Cross-link:** Các runbook này là **prerequisite upstream** cho `documents/05-guides/deploy/{secrets-seeding,dns-setup,email-ses-setup,cloudflare-setup,resend-provisioning,vercel-production-setup}-runbook.md`.

---

## 1. Mục đích

Phase 1 BETA deploy cluster (Wave 33 SHIPPED 2026-05-07 + Wave 84 SHIPPED 2026-05-15) ship code/runbook/scripts cho production deploy. Trước khi có thể chạy bất kỳ deploy script nào, solo dev cần các **tài khoản upstream** active:

1. **AWS account** — cloud platform (EC2, RDS, S3, Secrets Manager, ALB)
2. **Domain registrar** — sở hữu `kitehub.me`
3. **Password manager** — vault structure cho ~30 credentials
4. **KiteHub superadmin first-login** — post-seed admin invite chain
5. **Cloudflare account** (NEW Wave 84) — DNS authoritative + CDN proxy + DDoS protection
6. **Resend account** (NEW Wave 84) — transactional email delivery (HTTP API path per ADR-025 Stream A)
7. **Vercel account** (NEW Wave 84) — FE production hosting + preview deploys per PR

Solo-dev mode + first-deploy = high risk forget-step (vd: quên billing alarm AWS → bill shock; quên SSL Full strict Cloudflare → middle-mile insecure; quên DKIM Resend → email vào spam).

---

## 2. Sequence (T-7 → T-0)

```
T-7 ngày  ┌─ 01-aws-account-creation.md                (1.5h, blocks SES + Secrets Manager + ECR)
          ├─ 02-domain-registrar.md                    (1h, blocks DNS cutover)
          │  HOẶC 02b-github-student-pack-free-domain.md (Free path Porkbun)
T-5 ngày  └─ 03-password-manager.md                    (1.5h, blocks credential storage)
          ┊
T-4 ngày  ┌─ 05-cloudflare-account-setup.md            (1h, blocks dns-setup-runbook + cdn rules)
          ├─ 06-resend-account-setup.md                (45 min, blocks email-deliverability + resend-provisioning)
T-3 ngày  └─ 07-vercel-account-setup.md                (45 min, blocks FE production + preview deploys)
          ┊
          ┊  [Wave 33+38+84 deploy artifacts ship code/runbooks consuming các account trên]
          ┊
T-0 deploy   04-kitehub-superadmin-first-login.md      (2h, runs AFTER seed + email/Resend live)
```

**Critical path:**
- 01 + 02 chạy song song (independent)
- 03 chạy sau (cần ≥1 credential từ 01/02 để populate vault)
- 05 chạy sau 02 (cần ownership domain)
- 06 chạy sau 05 (cần DNS records cho DKIM)
- 07 chạy parallel 05/06 (cần 01 cho Secrets Manager + 05 cho DNS)
- 04 chạy sau khi infra live + seed run

---

## 3. Files

| # | File | Estimated time | Blocks | Owner | Reviewer |
|---|------|----------------|--------|-------|----------|
| 1 | [`01-aws-account-creation.md`](01-aws-account-creation.md) | ~1.5h | `deploy/secrets-seeding-runbook.md`, `deploy/email-ses-setup-runbook.md`, ECR push, CloudWatch | Dev | Tech Lead |
| 2 | [`02-domain-registrar.md`](02-domain-registrar.md) | ~1h | `deploy/dns-setup-runbook.md`, Cloudflare nameserver migrate | Dev | Tech Lead |
| 2b | [`02b-github-student-pack-free-domain.md`](02b-github-student-pack-free-domain.md) | ~30 min | Alternative Free path (Porkbun + Student Pack) cho 02 | Dev | Tech Lead |
| 3 | [`03-password-manager.md`](03-password-manager.md) | ~1.5h | Lưu trữ credentials Phase 1 | Dev | Security Lead |
| 4 | [`04-kitehub-superadmin-first-login.md`](04-kitehub-superadmin-first-login.md) | ~2h | First admin có thể vào dashboard production | Dev | Tech Lead |
| 5 | [`05-cloudflare-account-setup.md`](05-cloudflare-account-setup.md) | ~1h | `deploy/cloudflare-setup.md`, `deploy/dns-setup-runbook.md`, `operations/email-deliverability-runbook.md` | Dev | Tech Lead |
| 6 | [`06-resend-account-setup.md`](06-resend-account-setup.md) | ~45 min | `deploy/resend-provisioning-runbook.md`, `operations/email-deliverability-runbook.md` | Dev | Tech Lead |
| 7 | [`07-vercel-account-setup.md`](07-vercel-account-setup.md) | ~45 min | `deploy/vercel-production-setup.md`, `deploy/cloudflare-setup.md` (CNAME `app`) | Dev | Tech Lead |

**Total:** ~9h docs work + DNS propagation wait (parallelizable).

---

## 4. File Placement Rules

Per [`docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) §2 + [`deployment-naming-convention.md`](../../../.claude/rules/deployment-naming-convention.md) §2:

- ✅ **Belongs here:** One-time per cloud account / domain / vendor signup runbooks (pre-environment phase).
- ❌ **Does NOT belong here:**
  - Production seed runbooks → [`documents/05-guides/deploy/`](../deploy/)
  - Recurring rotation / incident response → [`documents/05-guides/operations/`](../operations/)
  - Infrastructure code → [`infrastructure/`](../../../infrastructure/)
- Naming: `NN-<vendor>-<action>.md` numbered by sequence (01, 02, 02b, 03, ...).

---

## 5. Standards & cross-links

- AWS Well-Architected (Operational Excellence + Security + Reliability pillars)
- VN PDPL 2023 + Decree 13/2023 — KYC documents (CMND/CCCD/passport) acceptance policy
- VN Luật An ninh mạng 2018 + Decree 53/2022 — data localization (chọn AWS Singapore `ap-southeast-1`)
- [`.claude/rules/release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) §3.4 (MAJOR + first PRODUCTION checklist)
- [`.claude/rules/logs-format-standard.md`](../../../.claude/rules/logs-format-standard.md) (no credentials trong logs/screenshots)
- [`.claude/rules/dev-readable-doc-language.md`](../../../.claude/rules/dev-readable-doc-language.md) §2 (Vietnamese narrative)
- [`documents/02-architecture/adr/ADR-025-aws-singapore-free-tier.md`](../../02-architecture/adr/ADR-025-aws-singapore-free-tier.md) (Phase 1 BETA cloud platform = AWS Singapore)
- [Cloudflare API Tokens — least privilege](https://developers.cloudflare.com/api/tokens/)
- [Resend Docs](https://resend.com/docs/introduction)
- [Vercel Docs](https://vercel.com/docs)

---

## 6. Out-of-scope

- AWS Organizations + multi-account → Phase 2
- HashiCorp Vault self-hosted → Phase 3
- Status page + AWS CloudFront / Cloudflare Pages account prep — covered by their own runbooks (`incident-comms-runbook.md` + `cloudflare-setup.md`)
- Hardware security keys (YubiKey) → optional, mentioned trong `03-password-manager.md` as recommended-but-not-required
- Stripe / payment gateway account-prep → Phase 1.5+ khi Premium plan launch
- Sentry / observability vendor account-prep → defer Phase 1.5+ post-MVP

---

## 7. Archive Policy

Move to `documents/07-archived/account-prep-YYYY/` khi:
- Vendor được thay (vd switch Cloudflare → Fastly) — archive old vendor runbook
- Runbook superseded bởi automation (vd Terraform provisioning replaces manual signup)
- Doc >180 ngày old AND no recent reference

Per [`docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md) §archive policy.

---

## 8. Log

- **2026-05-15:** Wave 84 Bucket C ship 3 new runbooks (05-cloudflare, 06-resend, 07-vercel) closing GAP-394. README index updated với ownership matrix + file placement rules + cross-link to `deployment-naming-convention.md`. Total 7 runbooks cover Phase 1 BETA account-prep matrix.
- **2026-05-07:** Original index created (Wave 33). 4 runbooks shipped (01-aws, 02-domain, 03-password, 04-superadmin).
