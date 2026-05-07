# 03 — Password Manager Runbook (1Password / Bitwarden)

**Audience:** Solo dev setup vault structure trước khi populate ~30 credentials Phase 1 BETA.
**Standards:** OWASP ASVS V2 (credential storage) · NIST SP 800-63B (memorized secrets) · `secrets-management-runbook.md` (architectural — này là user-level companion).
**Cross-link:** Blocks lưu trữ credentials từ `01-aws-account-creation.md` + `02-domain-registrar.md` + Cloudflare + GitHub + Oracle Cloud + SES + DB breakglass.
**Estimated time:** ~1.5h (account + vault structure + credential inventory).

---

## 1. Decision — 1Password vs Bitwarden

| Tiêu chí | 1Password | Bitwarden |
|----------|-----------|-----------|
| Pricing solo | $3/month Personal | **$0 Free tier** đủ Phase 1 |
| Self-host option | KHÔNG | Có (Vaultwarden Docker) |
| TOTP built-in | Có | Có (Premium $10/year) |
| Sharing solo→team | Family $5/month (5 users) | Free 2-user; Teams $4/user |
| Mobile + browser | Excellent | Good |
| Audit log | Có | Premium only |
| Recovery | Secret Key + Master Password | Master Password only (recovery code optional) |
| **Recommended Phase 1** | ✅ Solo dev with budget — better UX | ✅ Solo dev frugal — Free tier đủ |

**Khuyến nghị:**
- **Default:** Bitwarden Free + Premium TOTP ($10/year) → ~$1/month total. Đủ tính năng Phase 1 BETA.
- **Upgrade:** 1Password Personal ($3/month) khi team >1 person hoặc cần shared vaults nhiều levels.

Runbook này dùng **Bitwarden** làm reference; 1Password tương đương ~95% (folder = vault, items mostly same).

---

## 2. Vault structure design

3 vaults (folders) phân theo blast radius:

```
Kite-Personal/                    (solo dev daily — chỉ user này access)
├── AWS/
│   ├── aws-root@kitehub.vn       (Email + password + MFA recovery codes)
│   ├── solo-dev-admin user       (IAM user console password + access key)
│   └── ci-deploy access key      (programmatic key — cũng ở Kite-Production sharing)
├── Cloud/
│   ├── Cloudflare account        (TOTP + recovery codes)
│   ├── Oracle Cloud root         (Phase 0 dev stack)
│   └── GitHub personal           (TOTP + recovery + PAT)
├── Domain/
│   ├── Nhân Hòa account          (registrar credentials)
│   └── Mắt Bão account (backup)
├── Password manager itself/
│   ├── Bitwarden master pw       (lưu offline + qua secret-share friend)
│   └── Recovery codes printed    (reference: két sắt / nhà mẹ)
└── Personal-misc/                (banking/email — out-of-scope but co-located OK)

Kite-Production/                  (shared sau khi team >1; lúc đầu solo nhưng tách sẵn)
├── Database/
│   ├── postgres app user         (rotated 90 ngày)
│   ├── postgres breakglass admin (rotation locked; emergency only)
│   └── DB backup encryption key
├── Secrets-Manager/
│   ├── JWT_SECRET                (mirror AWS Secrets Manager value)
│   ├── ENCRYPTION_MASTER_KEY     (mirror)
│   ├── INTERNAL_API_SECRET       (mirror)
│   └── (~10 more app secrets)
├── 3rd-party/
│   ├── SES SMTP credentials
│   ├── OpenAI API key            (Phase 2 — null Phase 1)
│   ├── Cloudflare API token      (DNS automation)
│   ├── Statuspage / Instatus API
│   └── Sentry DSN
├── Service accounts/
│   ├── superadmin@kitehub.vn     (KiteHub first-login per `04-...md`)
│   └── ops-admin@kitehub.vn
└── CI/
    └── GitHub Actions secrets    (mirror — actual storage trong GH Settings)

Kite-Staging/                     (Phase 2 — separate AWS account)
└── (TBD Phase 2)
```

⚠️ **Critical:** vault `Kite-Personal` = single-user. Vault `Kite-Production` ban đầu solo nhưng STRUCTURE ready để invite ops-admin sau. Vault `Kite-Staging` chưa setup Phase 1.

---

## 3. Step-by-step setup

### 3.1 Bitwarden account (~5 min)

1. [bitwarden.com](https://bitwarden.com) → Get Started → Free Personal.
2. Email: dùng email cá nhân (KHÔNG dùng `aws-root@kitehub.vn` — Bitwarden là defense-in-depth, không nên cùng email với asset bảo vệ).
3. **Master password: ≥20 chars passphrase** (vd: `correct-horse-battery-staple-9-purple-coffee-mountain-2026`). Dùng diceware nếu cần (`https://world.std.com/~reinhold/dicewarewordlist.txt`).
4. Hint: KHÔNG để hint dễ đoán.
5. Create account → verify email.
6. Settings → Security → enable **Two-step Login** với Authenticator app (TOTP). NEVER chỉ dùng email 2FA.
7. Generate **Recovery Code** (Settings → Security → Two-step Login → View Recovery Code) → in giấy + lưu offline két sắt.
8. (Premium $10/year) Enable cho TOTP storage capability.

### 3.2 Browser + mobile setup (~10 min)

1. Browser extension: Chrome/Firefox/Safari → install Bitwarden ext → login + unlock.
2. Mobile: install Bitwarden app → enable biometric unlock (FaceID/fingerprint).
3. Verify: tạo test login item → close ext + retest auto-fill works.

### 3.3 Create 3 vaults / folders (~5 min)

Bitwarden Free dùng "Folders" (1Password dùng "Vaults" cho permission isolation; Bitwarden Free chỉ Personal vault — folders cho organization).

1. Bitwarden web → Folders → New Folder: `Kite-Personal`.
2. Sub-folders Bitwarden không support nested; dùng tag/notes hoặc upgrade Organizations (free 2-user) cho multi-vault.
3. **Recommended:** Upgrade Bitwarden Free Organization (free 2 users) → tạo Organization "Kite" → 3 collections: `Personal`, `Production`, `Staging`.

⚠️ Free Organization 2-user limit. Phase 2 team >2 → upgrade Teams ($4/user/month).

### 3.4 Populate Phase 1 credential inventory (~30-45 min)

Tạo items theo thứ tự (xem §2 vault structure). Mỗi item:
- **Name:** rõ ràng (vd: "AWS root - aws-root@kitehub.vn")
- **Username:** email/handle
- **Password:** generate trong Bitwarden (length 20+, all char types)
- **TOTP key:** scan QR khi enable MFA (Premium feature)
- **URLs:** login URL chính xác (auto-fill match)
- **Notes:** account ID, recovery info, MFA backup codes

Ví dụ entry AWS root:

```
Name: AWS root - aws-root@kitehub.vn
Folder: Kite-Personal / AWS
Username: aws-root@kitehub.vn
Password: [generated 20-char]
URL: https://[12-digit-account-id].signin.aws.amazon.com/console
TOTP: [scanned từ AWS MFA setup]
Notes:
  Account ID: 123456789012
  MFA Recovery codes:
    code1
    code2
    ...
  Phone: +84xxx (số đã verify)
  Created: 2026-05-07
  Last reviewed: 2026-05-07
```

⚠️ MFA recovery codes cũng lưu **offline** (giấy in két sắt) — phòng case Bitwarden compromise.

### 3.5 Sharing setup (Phase 2) — KHÔNG cần Phase 1 solo

Khi invite ops-admin:
1. Bitwarden Organization → Invite member với email ops-admin.
2. Grant access cho collection `Production` only (NOT `Personal`).
3. Member tự tạo master password riêng.
4. Audit logs (Premium feature) track ai access cái gì.

---

## 4. Rotation policy

| Credential type | Rotation interval | Trigger |
|----------------|-------------------|---------|
| AWS access keys | 90 ngày | Cron reminder + `secrets-management-runbook.md` |
| AWS console passwords | 180 ngày | Manual |
| App secrets (JWT, ENCRYPTION) | 180 ngày OR upon suspected leak | `secrets-management-runbook.md` rotation procedure |
| DB passwords | 365 ngày OR upon team departure | Coordinated downtime window |
| Domain registrar password | 180 ngày | Manual |
| Bitwarden master password | 365 ngày | Calendar reminder |
| TOTP recovery codes | Re-generate khi suspected leak | Rare |
| GitHub PAT | 90 ngày | Auto-expiry GH default |

Calendar reminder: Google Calendar / Apple Calendar event recurring với title "Rotate AWS keys per `03-password-manager.md`".

---

## 5. Backup + Recovery

### 5.1 Vault export backup (mỗi 30 ngày)

1. Bitwarden web → Tools → Export Vault.
2. Format: **Encrypted (Account-restricted)** — chỉ decrypt trong cùng account.
3. Download `bitwarden_export_*.json`.
4. Lưu ở:
   - Encrypted USB drive (LUKS / VeraCrypt) trong két sắt
   - Encrypted cloud (Mega.nz / Tresorit) với passphrase khác master
5. **DO NOT** lưu plaintext export. **DO NOT** Google Drive plaintext.

### 5.2 What to do if Bitwarden compromised

1. Sign out all sessions từ Bitwarden web Settings.
2. Đổi master password.
3. Re-generate recovery code.
4. **Audit gần đây:** rotate TOÀN BỘ credentials trong vault (assume bị copy).
5. Notify AWS billing alarm + monitor unusual activity 30 ngày.

### 5.3 What to do if master password forgotten

1. Bitwarden không recover master password được.
2. Login với recovery code (Settings → Two-step Login → ... ).
3. Reset master password.
4. Re-enable 2FA + regenerate recovery code.
5. Encrypted exports backup là last resort nếu cả master + recovery code mất.

---

## 6. Hardware security key (optional, recommended Phase 2)

YubiKey 5 NFC (~$50) added security cho high-value accounts:

- AWS root (USB-C / NFC tap to authenticate)
- GitHub (FIDO2 instead of TOTP)
- Bitwarden master (Premium feature)

Phase 1 BETA solo dev: **optional**. TOTP TOTP đủ baseline. Recommend procure YubiKey trước Phase 2 PAID launch.

---

## 7. Verification checklist

- [ ] Bitwarden Free + Premium TOTP active
- [ ] Master password ≥20 chars passphrase
- [ ] 2FA TOTP enabled + recovery code in giấy lưu offline
- [ ] 3 collections (Organization Free): Personal / Production / Staging
- [ ] Browser ext + mobile app installed + auto-fill works
- [ ] AWS root entry với MFA TOTP + recovery codes
- [ ] AWS solo-dev-admin entry
- [ ] AWS ci-deploy access key entry
- [ ] Cloudflare entry với TOTP
- [ ] Registrar (Nhân Hòa hoặc Mắt Bão) entry
- [ ] GitHub entry với PAT note
- [ ] Calendar reminders set cho rotation policy §4
- [ ] First encrypted export backup tạo + lưu offline

---

## 8. What can go wrong

| Symptom | Root cause | Fix |
|---------|-----------|-----|
| Master password forgot | Solo dev typed nhanh không kiểm tra | Recovery code → reset; encrypted export → import vault mới |
| TOTP secret lost (phone reset) | Không backup recovery codes | Login với recovery code → re-enroll TOTP |
| Auto-fill không trigger | Browser ext outdated hoặc URL match strict | Update ext; verify URL exact match (https vs http, www vs apex) |
| Bitwarden Premium charge fail | VN debit card | Switch sang Visa credit hoặc PayPal |
| Vault export decrypt fail | Account-restricted format dùng tài khoản khác | Phải import lại đúng account |
| Multi-device sync delay | Bitwarden Free server lag (rare) | Force sync trong app settings |

---

## 9. Out-of-scope

- Self-hosted Vaultwarden (Docker self-host) — Phase 3 nếu compliance cần
- Hardware security keys (YubiKey) — optional Phase 2
- 1Password migration — chỉ migrate nếu team scale Phase 2 cần better UX
- Biometric unlock policy enforcement — Phase 2 enterprise feature
- Audit log retention 7-year — Phase 3 compliance

---

## 10. Cross-link

- `01-aws-account-creation.md` — credentials populated từ §2.5/2.6/2.7
- `02-domain-registrar.md` — registrar credentials populated từ §2.1
- `04-kitehub-superadmin-first-login.md` — superadmin@kitehub.vn entry tạo trước first-login
- `documents/05-guides/operations/secrets-management-runbook.md` — architectural side; rotation procedure
- OWASP ASVS V2 — credential storage standard
- NIST SP 800-63B — passphrase length recommendation

---

## 11. Log

- **2026-05-07** — Runbook created. Phase 1 GAP-394 sub-runbook 3/4. Recommend Bitwarden Free + Premium TOTP $10/year cho solo dev frugal mode. 1Password Personal $3/month cho user budget. 3 collections / vaults: Personal / Production / Staging với blast-radius isolation. Out-of-scope: self-host Vaultwarden + YubiKey + 1Password migrate → Phase 2-3.
