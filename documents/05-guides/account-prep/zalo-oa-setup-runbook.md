# Zalo OA Setup Runbook — Phase 1 BETA Support Channel

**Audience:** Solo dev tạo Zalo Official Account (OA) lần đầu cho Phase 1 BETA — support channel cho VN edu cohort.
**Standards:** `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2 · `deployment-naming-convention.md` §2 (`account-prep/` — one-time per OA account).
**Cross-link upstream:** Wave 73+ user manual landed (`/help/anonymous`); domain `kitehub.me` đã verify (`02-domain-registrar.md` hoặc `02b-github-student-pack-free-domain.md`).
**Cross-link downstream:** Blocks `SupportMenu.tsx` (`kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx`) Zalo OA item active + `Footer.tsx` Zalo OA link + email template footer Zalo OA CTA.
**Estimated time:** ~30-45 phút cho Option A fast-path (founder personal OA); ~2-3 tuần cho Option B (business verification).
**Last-Updated:** 2026-05-18

---

## TL;DR

> Phase 1 BETA cần Zalo OA = first-class support channel theo VN edu cohort norm (KiteViet/Haravan/Misa pattern). Option A: dùng founder personal Zalo OA trong tuần này (no business verification). Option B: register business OA + verify (≥2 tuần). Code path đã có sẵn env-var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` — placeholder default `kitehub`; chỉ cần update khi OA ID thực sự active.

Quick path 5 bước cho Phase 1 BETA (Option A fast-path):

1. Mở app Zalo trên điện thoại → vào "Khám phá" → "Tạo Official Account" (hoặc truy cập [oa.zalo.me](https://oa.zalo.me/) trên desktop)
2. Chọn loại OA: **Cá nhân** (personal) cho fast-path; **Doanh nghiệp** cho long-term (xem §3 Option B)
3. Điền tên hiển thị: `KiteHub - Quản lý trung tâm giáo dục`
4. Upload avatar + cover image (paired GAP-225 AI branding scope) + về chúng tôi text (1-2 đoạn tiếng Việt)
5. Copy OA ID từ profile → update env var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` trong Vercel/EC2 deployment

---

## 1. Trước khi bắt đầu — chuẩn bị

| Item | Yêu cầu |
|------|---------|
| Tài khoản Zalo cá nhân | Đã active + verified với số điện thoại VN; recommend dùng số dành riêng cho business (KHÔNG dùng số cá nhân chính nếu muốn separate scope) |
| Số điện thoại VN | Để OTP verify khi tạo OA (Zalo yêu cầu) |
| Avatar 500×500 px PNG | **Sẵn có:** `assets/zalo-oa/kitehub-oa-avatar.png` (kite mark + chữ KiteHub, nền trắng). Lưu ý Zalo crop tròn → chữ đáy có thể cắt nhẹ; mark giữa luôn đầy đủ. |
| Cover image 1080×400 px | **Sẵn có:** `assets/zalo-oa/kitehub-oa-cover.png` (gradient brand + mark + tagline VN). Nguồn SVG cùng thư mục để chỉnh sau (GAP-225 AI branding có thể refine). |
| Mô tả ngắn (về chúng tôi) | 1-2 đoạn tiếng Việt giới thiệu KiteHub + support hours |
| Business license (Option B only) | Giấy đăng ký kinh doanh (cho business OA verification — không cần cho fast-path Option A) |

---

## 2. Option A — Fast-path (founder personal OA, Phase 1 BETA Wave 98+)

**Khi nào dùng:** Cần Zalo OA active TRONG WAVE 98 cohort polish (không thể chờ business verification 2+ tuần). KiteHub chưa incorporate hoặc đang chờ MST.

**Trade-offs:**
- ✅ Active ngay (15-30 phút setup)
- ✅ Không yêu cầu business license
- ❌ Không có badge "Đã xác minh" → tenant có thể nghi ngờ trust signal
- ❌ Cap follower ~5,000 (personal OA limit)
- ❌ Không dùng ZNS programmatic API (Wave 99+ scope)

### 2.1 Bước setup

1. Mở [oa.zalo.me](https://oa.zalo.me/) trên desktop browser
2. Click "Đăng ký Zalo Official Account" → đăng nhập bằng số điện thoại VN (Zalo personal account)
3. Chọn loại OA: **Cá nhân** (Personal)
4. Điền form:
   - **Tên OA:** `KiteHub - Quản lý trung tâm giáo dục`
   - **Danh mục:** Giáo dục → Công nghệ giáo dục
   - **Mô tả:** "KiteHub là nền tảng SaaS giúp trung tâm giáo dục VN quản lý học viên, lớp học, điểm danh, học phí. Hỗ trợ T2-T6 8h-18h."
5. Upload avatar + cover (xem §1 specs)
6. Click "Tạo OA" → wait Zalo review (~5-15 phút cho personal OA)
7. Sau khi approved → vào OA dashboard → copy OA ID (dạng số 13-16 chữ số, vd `1234567890123456`)

### 2.2 Update env var Phase 1 BETA

Sau khi có OA ID:

```bash
# Vercel (Phase 1 BETA pre-Wave-82 path)
vercel env add NEXT_PUBLIC_KITEHUB_ZALO_OA_ID production
# Paste OA ID khi prompt

# AWS EC2 self-host (Wave 82+ pattern per no-vercel-references.md)
# Update kitehub/kitehub-frontend/.env.production hoặc Helm values
echo "NEXT_PUBLIC_KITEHUB_ZALO_OA_ID=<OA_ID>" >> .env.production
```

Nếu env var chưa set → SupportMenu fallback dùng placeholder `kitehub` (matches Zalo URL `https://zalo.me/kitehub` — sẽ 404 cho tới khi OA active).

### 2.3 Verify

1. Open `https://zalo.me/<OA_ID>` trên browser → expect OA profile page render
2. Open Zalo app mobile → search "KiteHub" → expect OA xuất hiện
3. Click "Quan tâm" / "Follow" → verify follower count tăng

---

## 3. Option B — Business OA + verification (long-term, Wave 99+)

**Khi nào dùng:** KiteHub đã incorporate, có MST, business license; muốn badge "Đã xác minh" trust signal + ZNS programmatic API future scope.

**Trade-offs:**
- ✅ Badge "Đã xác minh" trên OA profile (trust signal cho Tier 1 tenant)
- ✅ Cap follower unlimited
- ✅ ZNS programmatic API (Zalo Notification Service) cho transactional notification Wave 99+
- ❌ Yêu cầu business license + 2-3 tuần Zalo review
- ❌ Phí Zalo OA Business plan (~5tr/năm cho mid-tier)

### 3.1 Prerequisites

- Giấy đăng ký kinh doanh (Business License) PDF
- MST (Tax code)
- Email theo domain `@kitehub.me` (vd `support@kitehub.me`)
- Số điện thoại business

### 3.2 Bước setup

1. Vào [oa.zalo.me](https://oa.zalo.me/) → Đăng ký → chọn **Doanh nghiệp** (Business)
2. Submit form business info + upload business license PDF
3. Wait Zalo review (2-3 tuần — Zalo verify business license + MST)
4. Sau khi approved → setup OA profile (avatar/cover/về chúng tôi) per §2.1 bước 4
5. Apply ZNS template (Wave 99+ scope — không cần Phase 1 BETA)

### 3.3 Migration từ Option A → Option B

Khi business OA approved, migrate followers từ personal OA:
1. Announce trên personal OA: "Chúng tôi đã chuyển sang OA chính thức — click [link] để follow"
2. Update env var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` → business OA ID mới
3. Keep personal OA active trong 30 ngày cho follower migration
4. Sau 30 ngày → archive personal OA (KHÔNG xóa — keep cho audit trail)

---

## 4. Code integration

### 4.1 Env variable convention

| Env | Variable name | Example value | Notes |
|-----|---------------|---------------|-------|
| `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` | OA ID hoặc slug | `kitehub` (placeholder) hoặc `1234567890123456` (real OA ID) | Public env var (NEXT_PUBLIC prefix) — không sensitive |

### 4.2 URL format

| Platform | URL pattern | Example |
|----------|-------------|---------|
| Web (desktop + mobile fallback) | `https://zalo.me/{oa_id}` | `https://zalo.me/kitehub` |
| Mobile deep-link | `zalo://chat?oa_id={oa_id}` | `zalo://chat?oa_id=1234567890123456` (mobile only, fall back to web nếu Zalo app chưa cài) |

### 4.3 Component consumers

- `kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx` — Zalo OA menu item (5th item dropdown)
- `kitehub/kitehub-frontend/src/components/layout/Footer.tsx` — Inline Zalo OA link trong section "Hỗ trợ"
- `kitehub/kitehub-email/src/main/resources/templates/emails/*.html` — Email footer Zalo OA CTA (beta-invite + invite-staff)

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Hardcode OA ID trực tiếp trong source code | Dùng env var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` |
| Skip Option A "vì chờ business verification" | Ship Option A trong Wave 98 — migrate sang Option B sau (§3.3) |
| Đặt OA ID vào commit message hoặc public docs ngoài runbook này | OA ID là semi-public (zalo.me URL anyway) — OK trong docs nhưng KHÔNG đặt vào secrets manager |
| Dùng số điện thoại cá nhân chính cho OA | Dùng số dành riêng cho business (separate scope) |
| Hỗ trợ qua Zalo OA ngoài giờ hành chính mà không thông báo | Cập nhật "về chúng tôi" với support hours T2-T6 8h-18h |
| Migrate personal OA → business OA mà không announce | Bước §3.3 announce + keep personal OA 30 ngày |

---

## 6. Troubleshooting

| Lỗi | Nguyên nhân | Fix |
|---|---|---|
| `https://zalo.me/kitehub` → 404 | OA chưa active hoặc env var dùng placeholder `kitehub` | Update env var với OA ID thực sự sau §2.1 bước 7 |
| Mobile deep-link `zalo://chat?oa_id=...` không mở Zalo app | User chưa cài Zalo app | Fallback tự động về web link (xem SupportMenu onClick handler) |
| OA review pending >24h (Option A) | Zalo backend slow | Check email báo về tài khoản Zalo cá nhân; nếu >48h liên hệ Zalo support |
| OA business verification rejected (Option B) | Business license PDF không rõ hoặc MST không match | Re-upload license rõ hơn; verify MST với cục thuế nếu cần |

---

## 7. Acceptance checklist (cho dev verify after setup)

- [ ] OA active tại `https://zalo.me/<OA_ID>` (HTTP 200)
- [ ] OA profile complete: avatar + cover + tên + về chúng tôi + support hours
- [ ] Env var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` set trên production
- [ ] SupportMenu Zalo OA item hiển thị + click mở Zalo app/web
- [ ] Footer Zalo OA link hiển thị inline với email + status page
- [ ] Email footer (beta-invite + invite-staff) include Zalo OA CTA

---

## 8. Related

- **Gap:** GAP-660 (this gap closure) + GAP-540 (sister gap — support channel discoverability, 100% complete via Wave 78 + Wave 98 B6)
- **Wave plan:** `documents/03-planning/waves/wave-2026-05-18-98-cluster-b-beta-cohort-polish.md` Bucket B6
- **Audit:** `documents/04-quality/audits/persona-review/2026-05-18-wave-98-cluster-b-external-benchmark.md` B-NEW-2 (VN benchmark KiteViet/Haravan/Misa)
- **Sister scope future:** Wave 99+ ZNS programmatic notification (out-of-scope Wave 98)
- **Rules:** `dev-readable-doc-language.md` v1.0.1 (Vietnamese narrative); `deployment-naming-convention.md` v1.0.2 (account-prep folder placement)
- **Cross-link sister account-prep:** `01-aws-account-creation.md` · `05-cloudflare-account-setup.md` · `06-resend-account-setup.md`
