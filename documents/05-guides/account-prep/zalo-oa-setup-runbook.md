# Zalo OA Setup Runbook — Phase 1 BETA Support Channel

**Audience:** Solo dev tạo Zalo Official Account (OA) lần đầu cho Phase 1 BETA — support channel cho VN edu cohort.
**Standards:** `release-deploy-standard.md` §3.4 · `dev-readable-doc-language.md` §2 · `deployment-naming-convention.md` §2 (`account-prep/` — one-time per OA account).
**Cross-link upstream:** Wave 73+ user manual landed (`/help/anonymous`); domain `kitehub.me` đã verify (`02-domain-registrar.md` hoặc `02b-github-student-pack-free-domain.md`).
**Cross-link downstream:** Blocks `SupportMenu.tsx` (`kitehub/kitehub-frontend/src/components/support/SupportMenu.tsx`) Zalo OA item active + `Footer.tsx` Zalo OA link + email template footer Zalo OA CTA.
**Estimated time:** ~30-45 phút cho Option A fast-path (OA Doanh nghiệp, skip verification → chưa-verified); ~2-3 tuần cho Option B (hoàn tất verification lấy badge).
**Last-Updated:** 2026-06-08

---

## TL;DR

> Phase 1 BETA cần Zalo OA = first-class support channel theo VN edu cohort norm (KiteViet/Haravan/Misa pattern). **⚠️ Cập nhật 2026-06-08:** Zalo KHÔNG còn loại "Cá nhân" — chỉ 3 loại: **Doanh nghiệp** / **Nội dung** / **Cơ quan nhà nước** (xem §0). Solo dev dùng **Doanh nghiệp** (chính sách Zalo: "phù hợp doanh nghiệp, tổ chức, **và cá nhân muốn xây thương hiệu**"). Option A = tạo Doanh nghiệp + **skip verification** ("Thoát xác thực OA") → OA chưa-verified, đủ cho passive CTA. Option B = hoàn tất verification (cần giấy phép KD) lấy badge. Code path đã có env-var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` — placeholder default `kitehub`; update khi OA ID active.

Quick path 5 bước cho Phase 1 BETA (Option A fast-path):

1. Truy cập [oa.zalo.me](https://oa.zalo.me/) → "Tạo Official Account" (desktop khuyến nghị)
2. Chọn loại OA: **Doanh nghiệp** (KHÔNG có "Cá nhân"; Doanh nghiệp bao gồm cá nhân xây thương hiệu). Khi gặp màn "Xác thực OA" → bấm **"Thoát xác thực OA"** để skip (OA chưa-verified, vẫn dùng được passive CTA)
3. Điền tên hiển thị: `KiteHub - Quản lý trung tâm giáo dục`
4. Upload avatar + cover image (dùng `assets/zalo-oa/*.png` §1) + về chúng tôi text (1-2 đoạn tiếng Việt)
5. Copy OA ID từ profile → update env var `NEXT_PUBLIC_KITEHUB_ZALO_OA_ID` trong EC2/Helm deployment

---

## 0. Loại OA Zalo 2026 (KHÔNG còn "Cá nhân")

Zalo 2026 chỉ có **3 loại** OA khi đăng ký (loại "Cá nhân/Personal" cũ đã bỏ):

| Loại | Cho ai | Giấy phép KD lúc tạo? | Broadcast |
|---|---|---|---|
| **Doanh nghiệp** ⭐ | Doanh nghiệp, tổ chức, **và cá nhân muốn xây thương hiệu** | Không bắt buộc (verify để sau lấy badge) | 4 tin/tháng/follower |
| **Nội dung** | Creator / cộng đồng / báo chí | Không cần | 1 tin/ngày/follower |
| **Cơ quan nhà nước** | Cơ quan hành chính | N/A (xác thực cơ quan) | 4 tin/tháng |

→ **Solo dev KiteHub chọn "Doanh nghiệp"** (chính sách Zalo bao gồm cá nhân xây thương hiệu). Đây KHÔNG phải bắt buộc có pháp nhân — chỉ là loại OA; verification (badge) mới cần giấy phép, và có thể **skip** lúc tạo. Fallback nếu Doanh nghiệp kẹt duyệt: loại **Nội dung** (creator, không cần giấy phép).

---

## 1. Trước khi bắt đầu — chuẩn bị

| Item | Yêu cầu |
|------|---------|
| Tài khoản Zalo cá nhân | Đã active + verified với số điện thoại VN; recommend dùng số dành riêng cho business (KHÔNG dùng số cá nhân chính nếu muốn separate scope) |
| Số điện thoại VN | Để OTP verify khi tạo OA (Zalo yêu cầu) |
| Avatar 500×500 px PNG | **Sẵn có:** `assets/zalo-oa/kitehub-oa-avatar.png` (kite mark + chữ KiteHub, nền trắng). Lưu ý Zalo crop tròn → chữ đáy có thể cắt nhẹ; mark giữa luôn đầy đủ. |
| Cover image 16:9 (≥1280×720 px) | **Sẵn có:** `assets/zalo-oa/kitehub-oa-cover.png` (1280×720 full-bleed gradient + mark + tagline VN). Zalo cover crop tỉ lệ 16:9 (min 320×180) — full-bleed lấp kín khung crop. Nguồn SVG cùng thư mục để chỉnh sau (GAP-225 AI branding có thể refine). |
| Mô tả ngắn (về chúng tôi) | 1-2 đoạn tiếng Việt giới thiệu KiteHub + support hours |
| Business license (Option B only) | Giấy đăng ký kinh doanh (cho business OA verification — không cần cho fast-path Option A) |

---

## 2. Option A — Fast-path (OA Doanh nghiệp, skip verification → chưa-verified)

**Khi nào dùng:** Cần Zalo OA active nhanh cho passive CTA support channel. Solo dev chưa có pháp nhân / chưa muốn verify ngay.

**Trade-offs:**
- ✅ Active nhanh (skip verification → Zalo chỉ review nội dung cơ bản, thường vài phút–vài giờ)
- ✅ Không yêu cầu giấy phép KD (skip verification)
- ❌ Không có badge "Đã xác minh" → tenant có thể nghi ngờ trust signal
- ❌ Chưa dùng ZNS programmatic API (cần OA verified — Option B, cho GAP-819 push phụ huynh)
- ⚠️ Loại Doanh nghiệp đôi khi review lâu hơn; nếu kẹt >24-48h → fallback loại "Nội dung" (§0)

### 2.1 Bước setup

1. Mở [oa.zalo.me](https://oa.zalo.me/) trên desktop browser
2. Click "Tạo Official Account" → đăng nhập bằng số điện thoại VN
3. Chọn loại OA: **Doanh nghiệp** (KHÔNG có "Cá nhân" — xem §0; Doanh nghiệp bao gồm cá nhân xây thương hiệu)
4. Điền form:
   - **Tên OA:** `KiteHub - Quản lý trung tâm giáo dục`
   - **Danh mục:** Giáo dục → Công nghệ giáo dục
   - **Mô tả:** "KiteHub là nền tảng SaaS giúp trung tâm giáo dục VN quản lý học viên, lớp học, điểm danh, học phí. Hỗ trợ T2-T6 8h-18h."
5. Upload avatar + cover (dùng `assets/zalo-oa/*.png` — xem §1)
6. **Khi gặp màn "Xác thực OA"** → bấm **"Thoát xác thực OA"** để skip (OA chưa-verified vẫn hoạt động cho passive CTA; verify để sau = Option B)
7. Click "Tạo OA" → Zalo review nội dung cơ bản (~vài phút–vài giờ; KHÔNG cần giấy phép vì đã skip verification). Status hiện "Đang chờ duyệt" cho tới khi xong
8. Sau khi approved → vào OA dashboard → copy **OA ID** (dạng số, vd `1851148412966286224`)

### 2.2 Update env var Phase 1 BETA

Sau khi có OA ID:

`NEXT_PUBLIC_*` được Next.js inline lúc **build** → set ở build-time:

```bash
# Local dev: kitehub/kitehub-frontend/.env.local (gitignored)
echo "NEXT_PUBLIC_KITEHUB_ZALO_OA_ID=<OA_ID>" >> kitehub/kitehub-frontend/.env.local

# Production (AWS EC2 self-host, Wave 82+): cần wiring 3 chỗ vì NEXT_PUBLIC = build-arg
#   1. kitehub/kitehub-frontend/Dockerfile: ARG + ENV NEXT_PUBLIC_KITEHUB_ZALO_OA_ID
#   2. kitehub/docker-compose.kitehub.yml build.args: NEXT_PUBLIC_KITEHUB_ZALO_OA_ID
#   3. .github/workflows/docker-build-push.yml: pass --build-arg
```

Nếu env var chưa set → SupportMenu/Footer fallback placeholder `kitehub` (`https://zalo.me/kitehub` — 404 cho tới khi OA active + env set).

### 2.3 Verify

1. Open `https://zalo.me/<OA_ID>` trên browser → expect OA profile page render
2. Open Zalo app mobile → search "KiteHub" → expect OA xuất hiện
3. Click "Quan tâm" / "Follow" → verify follower count tăng

---

## 3. Option B — Hoàn tất verification cho OA Doanh nghiệp (lấy badge + ZNS)

**Khi nào dùng:** KiteHub đã có pháp nhân (hộ KD / công ty), MST; muốn badge "Đã xác minh" + ZNS programmatic API (bắt buộc cho GAP-819 push phụ huynh).

> **Lưu ý:** KHÔNG tạo OA mới — đây là **cùng OA Doanh nghiệp** ở Option A, chỉ hoàn tất bước verification đã skip (Cách 1: xác thực theo tên doanh nghiệp). OA ID giữ nguyên → env var không đổi.

**Trade-offs:**
- ✅ Badge "Đã xác minh" trên OA profile (trust signal cho Tier 1 tenant)
- ✅ ZNS programmatic API (Zalo Notification Service) — unblock GAP-819 active push
- ❌ Yêu cầu giấy phép KD + 2-3 tuần Zalo review
- ❌ Phí Zalo OA Business plan tùy gói

### 3.1 Prerequisites

- Giấy đăng ký kinh doanh (hộ KD đủ — không bắt buộc công ty) PDF
- MST (Tax code)
- Email theo domain `@kitehub.me` (vd `support@kitehub.me`)
- Số điện thoại business

### 3.2 Bước verification (trên OA đã tạo)

1. Vào OA dashboard → Cài đặt → **Xác thực OA** → Cách 1 (tên doanh nghiệp)
2. Submit form business info + upload giấy phép KD PDF
3. Wait Zalo review (2-3 tuần — verify giấy phép + MST)
4. Sau khi verified → badge xuất hiện; OA ID + profile giữ nguyên (không đổi env var)
5. Apply ZNS template (cho GAP-819 push phụ huynh)

### 3.3 Không cần migration

Vì Option A → Option B là **cùng một OA** (chỉ thêm verification), KHÔNG có migration followers / đổi OA ID / đổi env var. Followers tích lũy từ Option A giữ nguyên.

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
| Skip Option A "vì chờ business verification" | Ship Option A (skip verification) ngay — hoàn tất verification sau = Option B (§3, cùng OA) |
| Đặt OA ID vào commit message hoặc public docs ngoài runbook này | OA ID là semi-public (zalo.me URL anyway) — OK trong docs nhưng KHÔNG đặt vào secrets manager |
| Dùng số điện thoại cá nhân chính cho OA | Dùng số dành riêng cho business (separate scope) |
| Hỗ trợ qua Zalo OA ngoài giờ hành chính mà không thông báo | Cập nhật "về chúng tôi" với support hours T2-T6 8h-18h |
| Tạo OA mới khi muốn verify (tưởng phải đổi sang "business OA") | Verify ngay trên OA Doanh nghiệp đã có (§3) — cùng OA, giữ OA ID + followers |

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
