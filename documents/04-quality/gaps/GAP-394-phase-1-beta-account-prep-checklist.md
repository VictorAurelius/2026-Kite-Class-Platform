# GAP-394: Phase 1 BETA Account Prep Checklist (4 missing runbooks)

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 STRONGLY recommend (Phase 1 BETA — không block code ship nhưng block first-deploy execution)
**Domain:** Operations / Documentation
**Found:** 2026-05-07 (coverage audit — Wave 35 prep session)
**Affects:** First-deploy execution — Oracle Cloud + AWS + domain registrar + password vault + KiteHub superadmin first-login

---

## Problem

Phase 1 BETA deploy artifact cluster (Wave 33 SHIPPED 2026-05-07) đã ship code/runbook/scripts cho DNS/SES/secrets/seed. Tuy nhiên 4 user-executed prep steps thiếu runbook dedicated, blocking first-deploy actual execution:

1. **AWS account creation walkthrough** — `secrets-management-runbook.md` giả định AWS account đã sẵn sàng (root MFA, billing alarm, 1st IAM admin user). Solo-dev chưa từng tạo, KYC + billing setup không trivial.
2. **Domain registrar account** — `huong-dan-deploy-oracle-cloud.md` Bước 5 chỉ nói Cloudflare DNS, KHÔNG nói đăng ký domain ở registrar nào (Namecheap / Mắt Bão / Cloudflare Registrar) — KYC, thẻ tín dụng, transfer-lock policy.
3. **Password manager policy** — `infrastructure/SECRET-MANAGEMENT.md` (3.6K) là architectural, KHÔNG phải user-level. Solo-dev cần vault structure cho ~20-30 credentials Phase 1 (Oracle root, AWS root, AWS IAM, domain registrar, Cloudflare, GitHub, status page, SES SMTP, DB password breakglass, OpenAI key, etc.).
4. **KiteHub superadmin first-login** — `GAP-376 production-data-seed` ship seed script nhưng chưa rõ runbook end-to-end: chạy seed → email superadmin invite → first-login → MFA enroll → invite next admin.

State-check (per `audit-to-gap-pipeline.md` §2.5):
- `find documents/05-guides -iname "*aws*account*"` → 0
- `find documents/05-guides -iname "*registrar*" -o -iname "*namecheap*"` → 0
- `find documents/05-guides -iname "*password*manager*" -o -iname "*1password*" -o -iname "*bitwarden*"` → 0
- `grep -rn "superadmin.*first.login\|first.login.*MFA" documents/05-guides/` → 0
- Tất cả 4 missing → 🆕 to-be-created

## Root Cause

Wave 33 deploy cluster scope = code/runbook for *services* (DNS, SES, secrets infra). Account prep = *prerequisite* upstream của services, được giả định "user sẽ tự làm" — không có owner trong wave plans. Solo-dev mode + first-deploy = high risk forget-step (vd: quên billing alarm → AWS bill shock; quên domain transfer-lock → registrar có thể chuyển domain qua attacker).

## Proposed Fix

File 4 runbook docs dưới `documents/05-guides/account-prep/`:

```
documents/05-guides/account-prep/
├── README.md                        — index + ordering (sequence T-7 → T-0)
├── 01-aws-account-creation.md       — Root signup, MFA, billing alarm, 1st IAM admin user, CLI config
├── 02-domain-registrar.md           — Compare Namecheap/Cloudflare Registrar/Mắt Bão (VN), KYC, transfer-lock, DNS migrate to Cloudflare
├── 03-password-manager.md           — 1Password/Bitwarden vault structure, ~30 credentials inventory cho Phase 1, sharing policy (solo→team transition), rotation reminders
└── 04-kitehub-superadmin-first-login.md  — Post-seed: invite email check → first login → MFA enroll → password rotate → invite ops admin
```

Mỗi runbook:
- Tiếng Việt theo CLAUDE.md communication rule (technical English terms OK)
- Step-by-step screenshots placeholder (capture sau khi user execute thực tế)
- KYC + billing pitfalls section (timezone, document acceptance rate VN passport vs CCCD, common reject reasons)
- "What can go wrong" section
- Cross-link với existing runbooks (secrets-management, dns-setup, email-ses-setup)

## Acceptance Criteria

- [ ] `documents/05-guides/account-prep/README.md` — index + sequence diagram T-7 → T-0
- [ ] `01-aws-account-creation.md` — covers root signup → MFA → billing alarm $5/$50/$200 thresholds → 1st IAM admin user (no console access for service users) → AWS CLI profile config
- [ ] `02-domain-registrar.md` — comparison table 3 registrars (Namecheap / Cloudflare Registrar / Mắt Bão) — KYC docs needed, payment methods accepting VN cards, transfer-lock default state, WHOIS privacy, expiry auto-renewal
- [ ] `03-password-manager.md` — vault structure (Personal / Kite-Production / Kite-Staging vaults), credential inventory list (~30 items Phase 1), 1Password/Bitwarden setup walkthrough, recovery code policy
- [ ] `04-kitehub-superadmin-first-login.md` — runbook from seed execution → email delivery verify → first-login → MFA TOTP enroll → password complexity validation → ops-admin invite chain
- [ ] All 4 docs cross-link với secrets-management-runbook.md + dns-setup-runbook.md + email-ses-setup-runbook.md
- [ ] `output-review-mandate.md` §3 matrix row "Account prep guides" added (if applicable) — atau tracked under "Logs format" / "Runbooks" existing rows
- [ ] ROADMAP §🚀 Next Action updated với this gap link

## Out-of-scope (track separately)

- AWS Organizations + multi-account strategy → defer Phase 2 (currently 1 account đủ)
- HashiCorp Vault self-hosted → defer Phase 3 (AWS Secrets Manager đủ Phase 1)
- Hardware security keys (YubiKey) → optional, document trong password-manager.md as recommended-but-not-required Phase 1
- Status page (GAP-373) + CDN (GAP-371) account prep — covered by their own gap files

## Related

- `documents/05-guides/operations/secrets-management-runbook.md` — assumes AWS account ready
- `documents/05-guides/deploy/email-ses-setup-runbook.md` — assumes AWS account ready
- `documents/05-guides/vietnamese/huong-dan-deploy-oracle-cloud.md` Bước 1 — Oracle account already covered
- GAP-369 DNS setup — domain registrar gap fills upstream prerequisite
- GAP-376 production-data-seed — superadmin first-login runbook fills downstream consumer
- GAP-379 secrets-management-rotation — password manager policy is upstream prerequisite

## Estimated effort

- 01-aws-account-creation.md — 1.5h (covers 1st-time AWS user with VN bank account specifics)
- 02-domain-registrar.md — 1h (comparison + walkthrough preferred path)
- 03-password-manager.md — 1.5h (vault structure design + credential inventory)
- 04-kitehub-superadmin-first-login.md — 2h (cross-reference seed script + email template + actual first-login flow)
- Cross-link updates + ROADMAP — 30min

**Total:** ~6h docs work, no code, no agent spawn needed (single solo session OR 1 background agent)

## Log

- **2026-05-07** Filed during Wave 35 prep session. Coverage audit identified 4 missing runbooks blocking actual first-deploy execution post-Wave-33 ship. State-check confirmed all 4 absent. Pairs với deploy artifact cluster Phase 1 BETA P0 BLOCKING (already PARTIAL per ROADMAP).
