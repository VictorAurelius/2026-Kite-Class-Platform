# 02 — Domain Registrar Runbook (`.vn` + fallback `.com`)

**Đối tượng:** Solo dev đăng ký 2 domain `kitehub.vn` + `kitehub.me` lần đầu.
**Tiêu chuẩn:** VN Luật Giao dịch điện tử 2023 · VNNIC `.vn` policy · Cloudflare onboarding.
**Cross-link:** Blocks `dns-setup-runbook.md` §2.1 (domain registration) → §2.2 (Cloudflare nameserver migrate) → SSL Let's Encrypt (`02-architecture/adr/`).
**Estimated time:** ~1h registration + 24-48h DNS propagation.

> ⚡ **Free alternative cho Release 1:** Nếu chỉ cần 1 domain cho Phase 1 BETA + Phase 1.5 PAID (~6 tháng), dùng GitHub Student Pack — claim **free `.me` 1 năm** qua Namecheap. Xem [`02b-github-student-pack-free-domain.md`](02b-github-student-pack-free-domain.md) (GAP-458). Decision lock 2026-05-09: front-door domain = `kitehub.me`. Sau Year 1 quyết định renew (~$10-20/year) hoặc switch sang `.vn` paid (xem runbook này).

---

## 1. Decision matrix — registrar nào?

Phase 1 BETA dùng `beta.kitehub.vn` + `beta.kitehub.me` (sub-domain), nhưng vẫn cần đăng ký apex domain `kitehub.vn` + `kitehub.me` trước.

### 1.1 So sánh 3 registrar phổ biến VN + fallback `.com`

| Tiêu chí | Nhân Hòa (`.vn`) | Mắt Bão (`.vn`) | Cloudflare Registrar (`.com`) | Namecheap (`.com`) |
|----------|------------------|-----------------|-------------------------------|--------------------|
| TLD support | `.vn` `.com.vn` `.com` | `.vn` `.com.vn` `.com` | `.com` `.net` `.org` (NO `.vn`) | `.com` `.vn`* (resell) |
| Renewal price (USD/year) | ~$30 (`.vn`) | ~$32 (`.vn`) | ~$10 (`.com` at-cost) | ~$13 (`.com`), ~$70 (`.vn` resell) |
| DNS console UX | Cơ bản, VN | Khá ổn, VN UI | Excellent (cùng Cloudflare DNS) | Tốt, EN UI |
| Transfer-lock default | ON | ON | ON | ON |
| WHOIS privacy | Có (free) | Có (free `.vn`) | Free | Free |
| KYC docs cần (`.vn`) | CMND/CCCD + đăng ký chủ thể VNNIC | Tương tự | N/A (`.com`) | N/A (`.com`) hoặc CMND nếu `.vn` resell |
| API for automation | Có (limited) | Có (limited) | Có (full) | Có (full) |
| 2FA on account | Có (SMS) | Có (SMS) | Có (TOTP) | Có (TOTP) |
| Customer support | VN tiếng Việt, slow ~24h | VN tiếng Việt, fast | EN only, fast | EN only, fast |
| Recommended for | `.vn` apex (brand VN) | `.vn` apex (alternative) | `.com` operations subdomain | Mixed `.com`/`.vn` |

\* Namecheap là reseller `.vn` qua VNNIC partner — giá cao hơn registrar VN trực tiếp.

### 1.2 Khuyến nghị

**Phase 1 BETA chọn:**

| Domain | Registrar | Reason |
|--------|-----------|--------|
| `kitehub.vn` | **Nhân Hòa** (preferred) hoặc **Mắt Bão** | `.vn` brand VN; KYC qua chủ thể cá nhân OK |
| `kitehub.me` | Same registrar as `kitehub.vn` | Single bill, easier renewal tracking |
| `kitehub.com` (optional fallback) | **Cloudflare Registrar** | At-cost pricing + tích hợp Cloudflare DNS |

⚠️ KHÔNG dùng Cloudflare Registrar cho `.vn` — không hỗ trợ. Phải qua VN registrar.

---

## 2. Step-by-step — `.vn` qua Nhân Hòa (preferred)

### 2.1 Account creation (~10 min)

1. [nhanhoa.com](https://nhanhoa.com) → Đăng ký tài khoản → email + số điện thoại VN.
2. Verify email + SMS OTP.
3. Login → Hồ sơ → upload CMND/CCCD (2 mặt) + chủ thể cá nhân thông tin.
4. Enable 2FA SMS (Nhân Hòa chưa hỗ trợ TOTP — chấp nhận SMS Phase 1).
5. **Lưu email + password vào vault** `Kite-Personal` (`03-password-manager.md`).

### 2.2 Domain search + registration (~15 min mỗi domain)

1. Trang chủ → "Tên miền" → search `kitehub.vn`.
2. Verify available → Add to cart.
3. Period: **2 năm** (ưu đãi giá thường rẻ hơn 1 năm/năm; expiry buffer cao hơn).
4. Auto-renewal: ✅ ON (tránh quên renewal → domain mất).
5. WHOIS privacy: ✅ ON (Nhân Hòa free `.vn`).
6. Lặp `kitehub.me`.
7. Checkout → thẻ Visa/MasterCard hoặc chuyển khoản ngân hàng VN.
8. Sau payment, domain hoạt động trong 1-15 phút.

### 2.3 Transfer-lock (BẮT BUỘC, ~2 min)

1. Dashboard → Quản lý tên miền → `kitehub.vn` → Tab "Bảo mật" hoặc "Khoá".
2. Verify "Transfer Lock" / "Khoá chuyển nhượng" = ON.
3. Một số registrar có 60-ngày lock sau registration mặc định (ICANN policy) — verify status.
4. Lặp `kitehub.me`.

⚠️ **Critical:** transfer-lock unlock chỉ khi PHẢI move registrar. Mặc định ON tránh attacker steal domain qua social engineering Nhân Hòa support.

### 2.4 DNS to Cloudflare migrate (~20 min)

Nameserver migrate là cách tốt nhất để Cloudflare manage DNS (proxy + DDoS).

1. Đăng ký Cloudflare account [cloudflare.com](https://cloudflare.com) (Free tier).
2. Cloudflare Dashboard → Add Site → nhập `kitehub.vn` → Free plan.
3. Cloudflare scan existing DNS records (nếu Nhân Hòa có default A records).
4. Cloudflare cấp 2 nameservers (vd: `aria.ns.cloudflare.com`, `bob.ns.cloudflare.com`). **Ghi xuống**.
5. Quay về Nhân Hòa dashboard → `kitehub.vn` → Tab "Nameserver" hoặc "DNS Manager" → đổi sang 2 NS Cloudflare.
6. Save. Đợi propagation 5-30 phút (đôi khi tới 24h cho `.vn`).
7. Verify: `dig NS kitehub.vn @8.8.8.8` → expect `*.ns.cloudflare.com`.
8. Cloudflare dashboard show "Active" trong "Overview" tab.
9. Lặp `kitehub.me`.

### 2.5 DNS records cho Phase 1 BETA

Sau khi nameserver active trên Cloudflare, follow `dns-setup-runbook.md` §2.3 add records cho `beta.kitehub.vn` + `beta.kitehub.me` + Cloudflare proxy orange-cloud.

---

## 3. Alternative — `.com` qua Cloudflare Registrar (fallback)

Nếu `.vn` KYC quá lâu hoặc reject, fallback `.com`:

1. Cloudflare Dashboard → Domain Registration → Search `kitehub.com`.
2. Cart → Register → at-cost price ~$10/year.
3. Account đã có 2FA TOTP (Cloudflare excellent default).
4. DNS auto-managed trong cùng Cloudflare account.
5. Email forwarding (free) → catch-all `*@kitehub.com` → user inbox.

⚠️ `.com` không có brand VN signal nhưng có thể dùng Phase 1 BETA fallback. Plan Phase 1.5 PAID switch sang `.vn` khi KYC clear.

---

## 4. Verification checklist

Sau khi xong:

- [ ] `kitehub.vn` registered + transfer-lock ON + auto-renewal ON
- [ ] `kitehub.me` registered + transfer-lock ON + auto-renewal ON
- [ ] Both domains migrated nameservers → Cloudflare (2 NS visible trong `dig NS`)
- [ ] Cloudflare dashboard "Active" status both sites
- [ ] WHOIS privacy enabled (verify: `whois kitehub.vn | grep -i "registrant\|whois.*privacy"`)
- [ ] Registrar account 2FA enabled (SMS minimum, TOTP nếu support)
- [ ] Renewal calendar reminder set 60 ngày trước expiry (calendar + password manager note)
- [ ] Credentials saved vào `Kite-Personal` vault (per `03-password-manager.md`)

---

## 5. What can go wrong

| Symptom | Root cause | Fix |
|---------|-----------|-----|
| `.vn` registration reject "Chủ thể chưa hợp lệ" | KYC docs blurry hoặc CMND expired | Re-upload với CMND/CCCD rõ + bill điện gần nhất |
| Domain "registered" nhưng không resolve sau 24h | Nameserver mismatch | Verify Cloudflare NS đúng + Nhân Hòa NS đã save → redo §2.4 |
| Cloudflare "Pending Nameserver Update" >48h | TLD slow propagation | Liên hệ Nhân Hòa support yêu cầu force NS push |
| Registrar charge thẻ fail | VN bank decline international USD | Đổi sang chuyển khoản VND nội địa |
| Lost access to registrar account | Email/phone changed mà không update | Mở support ticket với CMND scan + ownership proof; có thể lose domain |
| WHOIS public expose info cá nhân | Privacy không enable | Re-enable trong dashboard → public WHOIS update 24h |
| `dig NS kitehub.vn @8.8.8.8` trả về `dns-vn.com` thay vì cloudflare | Nameserver migrate chưa active | Wait + verify save trên registrar dashboard |

---

## 6. Out-of-scope

- Email hosting (Google Workspace / Zoho) — Phase 2 nếu cần `@kitehub.vn` mailbox
- DNSSEC enable — Phase 2 (Cloudflare hỗ trợ free nhưng cần coordinate với registrar)
- Domain trademark protection (VN Cục SHTT) — Phase 3 khi business stable
- Multi-domain bundle (e.g., `.com.vn` + `.org.vn`) — defer

---

## 7. Cross-link

- `documents/05-guides/deploy/dns-setup-runbook.md` §2.1 — domain step assumed done
- `documents/05-guides/vietnamese/cloudflare-setup.md` — Cloudflare full walkthrough (496 dòng)
- `01-aws-account-creation.md` — domain → SES domain identity (verify ownership qua TXT record)
- `03-password-manager.md` — vault entries cho registrar + Cloudflare credentials
- VNNIC policy: [vnnic.vn](https://vnnic.vn) — `.vn` chủ thể cá nhân yêu cầu hợp lệ
- ICANN transfer policy: 60-day registrar lock post-registration

---

## 8. Log

- **2026-05-07** — Runbook created. Phase 1 GAP-394 sub-runbook 2/4. Recommend Nhân Hòa cho `.vn` (brand VN + KYC qua chủ thể cá nhân OK + WHOIS privacy free). Cloudflare Registrar fallback cho `.com`.
