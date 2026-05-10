# deploy — Hướng Dẫn Phase 1 BETA Pre-Launch + Release Lifecycle

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Procedures cho release lifecycle: pre-deploy gate, CI/CD pipeline, rollback khi deploy fail, restore từ backup. Plus 13 user-executable runbooks cho Phase 1 BETA pre-launch (mua domain → Cloudflare → SSL → Vercel → AWS resume → smoke test). Audience: solo dev / release manager / on-call.

---

## 🚀 Phase 1 BETA Pre-Launch — Workflow theo thứ tự

Theo `release-1-deploy-plan.md` §2.1 pre-deploy checklist. **Phụ thuộc giữa các bước phải tuân thủ thứ tự** — không thể skip.

### Tier 0 — Account prep (one-time, làm sớm — vendor lead time dài)

| # | Action | Guide | Thời lượng | Cost |
|---|---|---|---|---|
| 0a | AWS account creation | [`account-prep/01-aws-account-creation.md`](../account-prep/01-aws-account-creation.md) | 30 phút | $0 |
| 0b | AWS Activate Founders Pack apply | [`aws-activate-credit-policy.md`](aws-activate-credit-policy.md) | 30 phút apply, **1-2 tuần approval** | $0 (offset $1k credit) |
| 0c | Password manager setup | [`account-prep/03-password-manager.md`](../account-prep/03-password-manager.md) | 15 phút | $0-3/month |
| 0d | KiteHub superadmin first login | [`account-prep/04-kitehub-superadmin-first-login.md`](../account-prep/04-kitehub-superadmin-first-login.md) | 10 phút | $0 |

### Tier 1 — Domain + DNS (vendor lead time ~1-3 ngày)

| # | Action | Guide | Thời lượng | Cost |
|---|---|---|---|---|
| 1 | **Domain procurement** — chọn 1 path: <br>· **Free path (Recommended Release 1):** `kitehub.me` qua GitHub Student Pack — [`account-prep/02b-github-student-pack-free-domain.md`](../account-prep/02b-github-student-pack-free-domain.md) <br>· **Paid path:** `.vn` qua VN registrar — [`account-prep/02-domain-registrar.md`](../account-prep/02-domain-registrar.md) | (xem cột guide) | 30 phút + propagation 1-3 ngày | **$0** (Student Pack) hoặc **~$60/year** (2 `.vn` domains) |
| 2 | Cloudflare account + nameservers active | [`cloudflare-setup.md`](cloudflare-setup.md) §1-2 | 20 phút + Cloudflare verification | $0 (Free tier) |
| 3 | Cloudflare DNS records → AWS ALB | [`cloudflare-setup.md`](cloudflare-setup.md) §3 + [`dns-setup-runbook.md`](dns-setup-runbook.md) §2.3 | 15 phút | $0 |
| 4 | SSL Full(strict) + ACM cert | [`dns-setup-runbook.md`](dns-setup-runbook.md) §2.4 | 15 phút (auto cert issue 1-3 phút) | $0 |

### Tier 2 — Infrastructure provisioning (terraform apply, 1-time bootstrap)

| # | Action | Guide | Thời lượng | Cost |
|---|---|---|---|---|
| 2a | Terraform bootstrap state backend | [`terraform-apply-bootstrap-runbook.md`](terraform-apply-bootstrap-runbook.md) | 15 phút | $0 |
| 2b | Production Architecture B apply | [`staging-activation-runbook.md`](staging-activation-runbook.md) (precedent) + [`aws-architecture-sizing-matrix.md`](aws-architecture-sizing-matrix.md) | 30-60 phút apply | EC2/RDS/ALB monthly |
| 2c | Secrets Manager populate `kite/prod/*` | [`secrets-populate-phase-2-4.md`](secrets-populate-phase-2-4.md) | 30 phút | <$1/month |
| 2d | Image push to ECR | [`phase-3-image-push.md`](phase-3-image-push.md) | auto via CI on tag | $0 (10 GB free tier) |

### Tier 3 — Backend services (compute resume khi sẵn sàng test)

| # | Action | Guide | Thời lượng | Cost |
|---|---|---|---|---|
| 7 | Resume EC2 + RDS từ cost-save | [`aws-cost-scheduling.md`](aws-cost-scheduling.md) §4 Manual override | 5 phút | **~$85-100/month** running |
| 7b | Right-size verify (nếu downsize) | [`right-size-stress-test.md`](right-size-stress-test.md) | 30 phút stress test | $0 |
| 7c | SES sandbox→production approval | [`email-ses-setup-runbook.md`](email-ses-setup-runbook.md) | 30 phút apply, **2-7 ngày AWS approval** | $0.10/1k emails |

### Tier 4 — Frontend wire (Vercel cài đặt cuối cùng — phụ thuộc tier 1+3)

| # | Action | Guide | Thời lượng | Cost |
|---|---|---|---|---|
| 5 | Vercel `NEXT_PUBLIC_API_URL` env vars | [`vercel-production-setup.md`](vercel-production-setup.md) §2 | 10 phút | $0 (Hobby Free) / **$20/mo Pro** (Phase 1.5 PAID commercial) |
| 6 | Vercel custom domain bindings | [`vercel-production-setup.md`](vercel-production-setup.md) §3 | 15 phút × 2 projects | (cùng plan §5) |

### Tier 5 — Verification + go-live

| # | Action | Guide | Thời lượng | Cost |
|---|---|---|---|---|
| 8 | ALB health check + smoke test | [`vercel-production-setup.md`](vercel-production-setup.md) §4 + smoke script | 15 phút | $0 |
| 8b | Pre-deploy backup snapshot | [`backup-runbook.md`](backup-runbook.md) | 10 phút | $0 (RDS automated backup) |
| 8c | Go/No-Go checklist final review | [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md) | 15 phút | $0 |

---

## 💰 Cost summary 3 paths

| Path | Mô tả | Spend (sau Activate credit) | Spend (no Activate) |
|---|---|---|---|
| **A — Min cost** (dev/staging only, dùng `*.vercel.app`) | Skip §1, §2, §6 — không phù hợp Phase 1 BETA invite | **~$2/month** + $0 one-time | ~$85-100/month |
| **B — Phase 1 BETA recommended** | Tất cả 8 actions; Vercel Hobby Free | **~$2/month** + $60 first year | ~$85-100/month + $60 first year |
| **C — Phase 1.5 PAID public launch** | Path B + Vercel Pro $20/month | **~$22/month** + $60/year | ~$105-225/month + $60/year |

Detail: xem [`aws-architecture-sizing-matrix.md`](aws-architecture-sizing-matrix.md) + [`aws-cost-monitoring.md`](aws-cost-monitoring.md).

---

## 📋 Release lifecycle (CI/CD)

| File | Mục đích |
|---|---|
| [`cicd-release-procedure.md`](cicd-release-procedure.md) | PR merge → prod deploy procedure đầy đủ |
| [`deploy-go-nogo-checklist.md`](deploy-go-nogo-checklist.md) | Pre-deploy gate checklist (GAP-087) |
| [`rollback-procedure.md`](rollback-procedure.md) | Per-service rollback steps (GAP-088) |
| [`restore-procedure.md`](restore-procedure.md) | Restore từ backup khi data loss (DR companion) |
| [`backup-runbook.md`](backup-runbook.md) | Pre-deploy backup snapshot |

---

## 📁 File Placement Rules

- ✅ **Thuộc về đây:** release lifecycle (pre-deploy, deploy, rollback, restore) + Phase 1 BETA pre-launch user-action runbooks
- ❌ **KHÔNG thuộc về đây:**
  - Infrastructure code: [`../../../infrastructure/`](../../../infrastructure/)
  - Incident response: [`../operations/`](../operations/)
  - DR strategy: [`../operations/disaster-recovery-plan.md`](../operations/disaster-recovery-plan.md)
  - Account prep (one-time per cloud account): [`../account-prep/`](../account-prep/)
- Naming: action-oriented (`*-runbook`, `*-procedure`, `*-checklist`, `*-setup`)

---

## 🔗 Liên quan

- **Plan documents:** [`../../03-planning/roadmap/release-1-plan-2026.md`](../../03-planning/roadmap/release-1-plan-2026.md) · [`../../03-planning/roadmap/release-1-deploy-plan.md`](../../03-planning/roadmap/release-1-deploy-plan.md)
- **DR plan + RTO/RPO matrix:** [`../operations/`](../operations/)
- **Incident runbooks:** [`../operations/runbooks/`](../operations/runbooks/)
- **Architecture:** [`../../02-architecture/adr/ADR-025-aws-singapore-architecture-b.md`](../../02-architecture/adr/ADR-025-aws-singapore-architecture-b.md)
- **Helm charts:** [`../../../infrastructure/helm/`](../../../infrastructure/helm/)
- **Rules:** [`.claude/rules/release-deploy-standard.md`](../../../.claude/rules/release-deploy-standard.md) · [`.claude/rules/agent-aws-access.md`](../../../.claude/rules/agent-aws-access.md)

---

## 🗂️ Archive Policy

Move sang `documents/07-archived/deploy-YYYY/` khi:
- Pipeline thay đổi major (vd switch GitHub Actions → GitLab CI)
- Architecture migration (vd Architecture B → C)
- Phase progression render runbook obsolete (vd Phase 3 K-12 launch → archive Phase 1 BETA-specific runbooks không còn áp dụng)

---

## 🧭 Quick start cho người mới

**Lần đầu cài đặt Phase 1 BETA from scratch?** Theo thứ tự:

1. **Tuần 1:** Đăng ký AWS + apply Activate Founders Pack → đợi approval (1-2 tuần)
2. **Song song Tuần 1:** Mua 2 domain `.vn` → wait propagation
3. **Tuần 2:** Cloudflare account + nameservers + SSL Full(strict)
4. **Tuần 2-3:** Terraform bootstrap → production apply
5. **Tuần 3:** Secrets Manager populate + ECR image push (auto via CI tag)
6. **Tuần 3-4:** Resume EC2 + RDS + SES sandbox→production approval (đợi 2-7 ngày AWS)
7. **Tuần 4:** Vercel env vars + custom domain bindings
8. **Tuần 4 cuối:** Smoke test + Go/No-Go checklist + tag `v0.9.0-beta` → invite 10-20 beta tenants

Tổng thời gian: ~4 tuần (vendor approval lead time chiếm phần lớn). Code + infra work là vài giờ phân tán.

---

## Last Updated

2026-05-09 — GAP-457 ship (Vercel guide mới + 3 Mixed guides VN sync + workflow index)
