# UI Kit — kiteclass-public (4 trang public per-tenant KiteClass · GV ĐỘC LẬP)

Kit design cho **4 trang public còn lại** của một trang per-tenant KiteClass do **giáo viên độc lập** sở hữu (persona **P1 Solo Teacher**). Trang landing `/` đã có ở kit **[`landing-personal/`](../landing-personal/README.md)** (113/128 — design source) — kit này nối tiếp, đồng bộ token + theme switcher + nav/footer pattern.

Mở [`index.html`](index.html) để vào hub 4 screens. Demo tenant: **Lớp Toán cô Nguyễn Thị Hà** (xanh dương `#2563EB`, dữ liệu thật từ demo-trio).

## Screens — 4 trang public (`screens/`)

| Route | File | Mô tả |
|-------|------|-------|
| `/catalog` | [`screens/catalog.html`](screens/catalog.html) | Danh sách khóa: search + filter (cấp lớp/trình độ) + sort + course cards (badge "Đang tuyển sinh"/"Đã đầy", giá VND, mã khóa) + **gợi ý theo persona** + empty-state + CTA "Không tìm thấy khóa phù hợp?" |
| `/catalog/[id]` | [`screens/catalog-detail.html`](screens/catalog-detail.html) | Hero nhỏ (giá + CTA học thử) + mục tiêu + syllabus theo tuần + GV phụ trách + lịch lớp đang mở + FAQ + **sticky CTA mobile** |
| `/about` | [`screens/about.html`](screens/about.html) | Câu chuyện GV (voice "tôi/cô") + đội ngũ (teacher card) + **số liệu thật anti-fabrication** + chứng chỉ |
| `/contact` | [`screens/contact.html`](screens/contact.html) | Form VN-realistic (họ tên / SĐT 10 số / email optional / lời nhắn) + **nút Zalo** (khi tenant có zaloUrl) + bản đồ + giờ làm việc |

## Persona & audience
- **Chủ tenant:** P1 Solo Teacher (gia sư tự do, 5–50 học viên, 0 nhân viên) — `dossier/01-personas.md` §P1.
- **Audience đọc trang:** **phụ huynh** đang tìm lớp cho con (visitor chính) + học viên.
- **Voice (GAP-1208):** xưng "tôi/cô", giọng cá nhân ấm áp — nhãn GV độc lập: **"Về giáo viên"** (không "Giới thiệu trung tâm"), **"Giáo viên đồng hành"** (không "Đội ngũ giáo viên").

## Theme switcher (GAP-274) — runtime
3 GV demo trên mỗi screen, đổi màu **toàn trang thật** (set class `.theme-*` trên `<body>` + cập nhật brand identity + GV info): **Cô Hà** xanh dương `#2563EB` · **Thầy Nhì** xanh lá `#16A34A` · **Cô Khánh** cam `#EA580C`. Parity token với `landing-personal/` + `marketing-site/`. Per `design-source-implementation-parity.md` §3.2 — affordance click có hiệu ứng runtime thật (không inert).

## Persona-based recommendations (GAP-274 AC) — catalog
Block **"Gợi ý cho con anh/chị"**: phụ huynh chọn tình huống của con (Con mất gốc / Học theo kịp lớp / Chuẩn bị thi vào 6) → JS gợi ý khóa phù hợp theo độ tuổi + mục tiêu, hiển thị runtime. Production: map theo `student.gradeLevel` + `goal` từ tenant catalog data.

## Contact form validation spec
| Trường | Bắt buộc | Quy tắc |
|--------|:--------:|---------|
| Họ tên | ✅ | `≥2` ký tự sau trim |
| SĐT | ✅ | regex VN `^0\d{9}$` (bắt đầu `0`, đúng 10 chữ số); input lọc non-digit + `maxlength=10` |
| Email | ❌ optional | nếu nhập phải khớp `^[^@\s]+@[^@\s]+\.[^@\s]+$` |
| Lời nhắn | ✅ | `≥10` ký tự sau trim |

Hành vi: inline error dưới field + `aria-invalid` + `aria-describedby` + `role="alert"` + focus field lỗi đầu tiên; xóa lỗi khi user sửa; submit → success panel (không reload). States demo: default / error-validation / success. Nút **Zalo** toggle theo `tenant.zaloUrl` (demo-toggle ở demo-strip).

## Anti-fabrication (about stats)
Số liệu KHÔNG bịa — dùng demo-trio **THẬT**: **2 lớp đang mở · 12 học viên hiện tại · 85% lên lớp giỏi** + chú thích "* Số liệu lấy trực tiếp từ dữ liệu tenant trên KiteClass, cập nhật tự động — khi chưa đủ dữ liệu hệ thống ẩn chỉ số thay vì hiển thị ước lượng". Production: stats fetch real, không hardcode.

## VN localization
100% tiếng Việt; VND `800.000 đ` / `900.000 đ` / `1.500.000 đ`; SĐT `0912 345 678`; sample `Nguyễn Thị Hà`, `Trần Thị Hồng`, `Trần Thị Mai`; địa chỉ Đống Đa Hà Nội; Zalo kênh liên hệ chính; "gia sư"/"học thử"/"phụ huynh"/"vào lớp 6"; ĐH Sư phạm Hà Nội. Không `John Doe`/`$`/`lorem`.

## WCAG AA self-measurement (đo trong comment đầu mỗi screen)
Cặp text-bearing chính ≥4.5:1: body slate-800 `~14:1`; muted slate-500 `~4.7:1`; CTA trắng/`--cta-strong` orange-700 `~4.8:1`; hero trắng/gradient `~6.1–8.6:1`; badge tuyển sinh emerald-800/emerald-50 `~7.6:1`; error red-600 `~4.8:1`; filter chip active trắng/blue-600 `~4.6:1`. CTA text dùng `--cta-strong`; `--cta` (`#F97316`) chỉ cho fill trang trí. Skip-link + landmarks + `aria-*` mọi screen.

## Self-score per screen (8 dimension × /16 = /128 · target ≥105)

| Screen | D1 Hierarchy | D2 Persona | D3 Interaction | D4 Theme | D5 VN content | D6 Responsive | D7 A11y | D8 Completeness | **Tổng** |
|--------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **catalog** | 14 | 15 | 15 | 14 | 15 | 14 | 14 | 14 | **115/128** |
| **catalog-detail** | 15 | 14 | 13 | 13 | 15 | 15 | 13 | 14 | **112/128** |
| **about** | 14 | 15 | 12 | 14 | 15 | 14 | 13 | 13 | **110/128** |
| **contact** | 14 | 14 | 15 | 13 | 15 | 14 | 15 | 14 | **114/128** |

Ghi chú điểm trừ: D3 about thấp (12) — ít affordance tương tác (chủ yếu đọc); D7 detail/about (13) — FAQ `<details>` + carousel-less, chưa có `aria-live` cho schedule seats; D4 (13–14) — runtime swap thật nhưng production cần token đầy đủ hơn (dark surface, spacing scale). Mọi screen ≥105 ✓. Hard gates: ✓ VN-only data · ✓ WCAG AA đo · ✓ persona khai báo (HTML comment) · ✓ no hardcoded hex ngoài token block · ✓ form validation hoạt động · ✓ empty/error/success states.

## 4-layer design coverage (per `.claude/rules/design-layer-coverage.md` §2.2)
| Layer | Artifact pointer |
|-------|------------------|
| **要件定義** (requirements / persona / use-case) | `dossier/01-personas.md` §P1 Solo Teacher · `dossier/05-business-flows.md` (prospect → catalog → đăng ký học thử) · GAP-274 AC (per-tenant theme + persona recommendations) |
| **基本設計** (basic / screen design) | **kit này** — 4 screen HTML + section map ở trên (catalog / detail / about / contact) |
| **詳細設計** (detail / state / spec) | Contact form validation spec §"Contact form validation" · catalog filter/sort/empty states · detail sticky-mobile-CTA · about anti-fabrication data-driven note · GAP-274 / GAP-1208 |
| **コンポーネント設計** (component design) | ThemeSwitcher (parity `marketing-site/` + `landing-personal/`) · course card · form field + inline error · `_shared/colors_and_type.css` theme vars · teacher card (tái dùng từ `landing-personal/`) |

## Cross-link
- GAP-274 (per-tenant theme + persona recommendations — kit này) · GAP-1208 (voice GV độc lập) · sibling kit `landing-personal/` (landing `/`).
- Production mapping (đợt 2 port): `kiteclass/kiteclass-frontend/src/app/(public)/catalog/` + `catalog/[id]/` + `about/` + `contact/` · `src/lib/template/configs.ts` (per-tenant theme inject) · `src/components/sections/`.
- Dossier: `dossier/01-personas.md` · `dossier/02-vietnamese-ux-musts.md` · `dossier/06-quality-bar.md`.

## Notes cho production-port agent (đợt 2)
1. **Theme:** thay 3 preset demo bằng CSS vars `--theme-*` do branding inject runtime (GAP-274) — giữ nguyên contrast pairing (`--cta-strong` cho text-bearing CTA). Token đã RGB-space-separated sẵn cho `rgb(var(--x)/.alpha)`.
2. **Catalog:** filter/sort/search demo bằng vanilla JS trên DOM tĩnh → port sang server-side query (Spring `kiteclass-core` course list endpoint) + client filter chips; persona reco map `student.gradeLevel`+`goal`.
3. **Detail:** schedule seats + badge "Đã đầy" wire từ `class.capacity` thật; sticky mobile CTA dùng `position:fixed` — kiểm tra với bottom-nav nếu có.
4. **About stats:** BẮT BUỘC fetch real từ tenant data, ẩn chỉ số khi `null` (anti-fabrication) — KHÔNG hardcode 2/12/85%.
5. **Contact:** validation regex là contract — replicate server-side; SĐT `^0\d{9}$`; nút Zalo chỉ render khi `tenant.zaloUrl != null`; consent text PDPL giữ nguyên; POST → `kiteclass-core` lead/contact endpoint.
6. Re-run Lighthouse/axe + `pnpm build` trước merge per `output-review-mandate.md` §3 row "HTML/JSX prototypes" + `fe-build-local-verify.md`.

> Design artifact (self-contained HTML+CSS+vanilla JS — không phụ thuộc `_shared/` hay CDN). Production port = Next.js + `next/image` + `next/font`.
