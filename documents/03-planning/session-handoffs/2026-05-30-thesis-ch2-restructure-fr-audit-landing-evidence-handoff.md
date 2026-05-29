---
title: Session Handoff — Thesis Ch.2 restructure + FR audit + Landing evidence (2026-05-30)
audience: dev
last-updated: 2026-05-30
status: complete
branch: wave/thesis-3-content-fixes
---

# Session Handoff — 2026-05-30 (wave/thesis-3-content-fixes)

## Scope shipped session này (commits, chưa push — 26 ahead origin)

Branch `wave/thesis-3-content-fixes`. Tất cả docx rebake sau mỗi fix (per `feedback_thesis_rebake_after_fix`).

1. **§1.2.6 Bảng 1.1** — thêm caption + "Phiên bản sau"→"Phát triển sau"; Zalo/payment status lock ("Có"); narrative §1.3.2 sync
2. **ch1-ai-techniques** — sweep 11 "giai đoạn thử nghiệm/mở rộng/GA" → "hiện tại"/"phát triển sau" (binary-time §3)
3. **Mở đầu §4** — cắt bullet "audit bảy chiều /100 /128" (orphan claim); rút gọn QDD (bỏ Lean + incident-to-rule); "Quality-Driven Development"→"phương pháp luận hướng chất lượng"
4. **Mở đầu §5** — bỏ "Tóm tắt nội dung" (trùng §6 Cấu trúc); fix mô tả Chương 2 (bỏ "use case" không tồn tại + thêm §2.3 Class/ERD/Sequence/State/DB/SaaS)
5. **Hình 2.4a/b/d + 2.7a/b + 4.2a/b** — wrap:true + compact margins + font 36/34/34 (font to hơn ~44%); strip 11 banned `<br/>` (detector 0 hazard)
6. **§2.2.3** — nén 2 đoạn so sánh verbose (6-bullet tiêu chí + 6-bullet diễn giải P4 → 2 đoạn), giữ 2 bảng
7. **§2.1.1 FR** — MISA MeInvoice → "lộ trình phát triển sau"; thêm FR section "Email giao dịch (kitehub-email)"
8. **§2.3 RESTRUCTURE LỚN** — Class/ERD tách 2 cụm (Hình 2.5a/b KiteHub control-plane + KiteClass domain; 2.6a/b); DB design move lên §2.3.3 (data cluster Class→ERD→DB); +bảng subscriptions; renumber sections + bảng (instances 2.7/subscriptions 2.8/students 2.9/service 2.10/SaaS 2.11); fix ch4 cross-ref §2.2.3/§2.2.4
9. **Hình 2.8** — ACTIVE self-loop → note (bỏ vòng gấp nhọn)
10. **Terraform "1.x"→"1.5+"** (state-check required_version >= 1.5)
11. **Seed:** `seed-demo-sky-education.sh` (78 students/5 courses/5 classes/4 teachers/12 enrollments + branding cam #EA580C) — VERIFIED. ⚠️ core unhealthy fix = thiếu RabbitMQ queue `class.rescheduled.queue` + `.email.queue` (runtime-declared, chưa IaC)
12. **3 portrait + 3 banner** — `compose-teacher-banners.mjs` render HTML→PNG cho 3 giảng viên độc lập

## 🔴 PRIORITY session sau — FR fix OPTION C (TOÀN BỘ) — user chốt 2026-05-30

Áp dụng **toàn bộ** fix FR §2.1.1 từ audit 4-agent (MUST-FIX overclaim + ADD T1-T6 + tất cả). Bảng gap tổng:

### A. OVERCLAIM cần SỬA (defense-critical)
| # | FR nói | Code thực | Hành động |
|---|---|---|---|
| O1 | AI "MiniMax" (§2.1.1) + "MiniMax" (§2.2 A10) + "SDXL/Replicate" (Ch.1 §1.3.2) | **OpenAI GPT-4 Vision + DALL-E 3 + Ollama** (verified 0 grep minimax/replicate; `AIProviderConfig.java:54`, `application.yml:88 provider:openai`, `:148 dalle:dall-e-3`) | Thống nhất 3 chỗ → OpenAI+Ollama. **Ch.1 §1.3.2 cost-analysis SDXL $0.0012 cũng phải sửa** |
| O2 | AI Quality Gate NSFW classifier (Ch.1 §1.3.3 chi tiết + cost $0.001/img như đã chạy) | `QualityScoreAggregator` "deterministic placeholders" defer GAP-226/228 | Ch.1 reframe "thiết kế/lộ trình" |
| O3 | "thang điểm 10" | thực thang 0-100 + letter A+/A + GPA 4.0 (`GradingScale.java`) | sửa FR khớp HOẶC sửa code về thang 10 (tốt cho VN-localization) |
| O4 | "báo cáo cuối kỳ HK1/HK2/HK_Hè" | `ReportCardService` orphan, không endpoint | hạ claim "lộ trình expose API" hoặc wire controller |
| O5 | DKIM per-tenant gói PRO (như đã có) | chưa impl (comment platform 1 domain) | → defer |
| O6 | "Resend tự động chuyển đổi khi lỗi" | static config, default `mock`, no runtime failover (`EmailProviderRouter.java:70`) | sửa mềm "lộ trình failover sau" |
| O7 | bảng `tenant_quota` | thực `branding_regenerate_usage` | sửa tên |
| O8 | `email_logs` thuộc kitehub-email | thực kitehub-subscription (`V5__create_email_logs_table.sql`) | sửa vị trí |
| O9 | path `/admin/v1/revenue`, `/api/impersonate/start` | thực `/api/v1/admin/revenue`, `/api/v1/admin/impersonate/{slug}` | **bỏ path khỏi FR** (đưa Ch.3 API contract) |
| O10 | "nhập CSV/Excel" | chỉ .xlsx (Apache POI, no CSV) | bỏ "CSV" |
| O11 | `class_schedule_slots` | thực ClassSession + RFC 5545 recurrence | sửa cơ chế |
| O12 | "VietQR thủ công; MoMo/VNPay lộ trình sau" | **VNPay impl thật 166 dòng + webhook**; MoMo/ZaloPay stub | reconcile "khung VNPay sẵn sàng + webhook; VietQR chính hiện tại; MoMo/ZaloPay stub" |
| O13 | Attendance "Có/Vắng/Nghỉ phép" (3) | 5 (PRESENT/ABSENT/LATE/EXCUSED/MAKEUP) | FR under-claim → nâng 5 trạng thái |

### B. THIẾU FR — feature implement đáng kể, ADD (ưu tiên T1-T6)
| # | Feature | Bằng chứng | Ưu tiên |
|---|---|---|---|
| T1 | **Cổng phụ huynh** (invitation + 5 facet điểm/điểm danh/học phí/hạnh kiểm/học bạ + complaint + read-audit) | 11 controllers, `ParentInvitationServiceImpl` 335 dòng | 🔴 gap lớn nhất |
| T2 | **LMS** (CourseModule/Lesson/LearningResource/LessonProgress) | `LmsController` 24 endpoints | 🔴 |
| T3 | **Assignment/Submission** (giao→nộp→chấm→trả) | `AssignmentController` 16 endpoints | 🔴 |
| T4 | **Course** (tầng cha của Class) | `CourseController` 10 endpoints | 🟠 |
| T5 | **Transcript** (học bạ + GPA + PH xem) | `GradeController /transcripts/*` | 🟠 |
| T6 | **Tài chính:** trả góp (InstallmentPlan) + hoàn tiền (RefundRequest) + lương GV (Payroll) | 3 controller impl thật | 🟠 |
| T7 | 2FA TOTP Owner (RFC 6238 + recovery codes) — `TwoFactorController` 5 endpoint | 🟡 |
| T8 | StaffInvitation (mời nhân viên) + OnboardingProgress wizard | 🟡 |
| T9 | Teacher management + phân công GV (`TeacherController`) | 🟡 |
| T10 | CRM Lead + Vetting/Incident (K-12) + Storage quota | 🟡 |

### C. Nội bộ/orphan — KHÔNG đưa FR
LoginAuditLog/Outbox/IdempotencyKey/BackupRecord (infra) · **ContentModeration/ModerationQueue (dead-code orphan — cân nhắc xóa)** · RBAC Role/Permission (service no controller) · StudentPoint (side-effect) · Curriculum (entity-only)

**Lưu ý page count:** thêm nhiều FR → Ch.2 dài (cap 60-80 trang). Thêm gọn 1-2 bullet/feature.

## 🎯 UI STANDARD CHỐT (landing giảng viên độc lập — từ 2-agent audit)

**Approach chốt (user 2026-05-30):** DÙNG + NÂNG landing thực Next.js (KHÔNG mockup HTML from-scratch). Landing thực đã có engine: `TemplateRenderer` + 8 section thật (`HeroSection`/`FeaturesSection`/`PricingSection`/`CTASection`/`TestimonialsSection`/`TeachersSection`/`AboutSection`/`ContactSection`) + **đổi theme per-tenant qua query param `?primary=&secondary=&accent=`** (không cần backend, `page.tsx:69` + `ThemeSync.tsx:56`).

**Font:** user chốt **chuyển web sang Be Vietnam Pro** (hiện Inter `layout.tsx:3` — lệch banner Be Vietnam Pro).

**10 section chuẩn (benchmark) vs component thực:**
- ✅ Có: Hero, Giới thiệu(About), Môn/Lớp(Features), Học phí(Pricing VND), Testimonial, Đội ngũ(Teachers), CTA, Contact
- ❌ Thiếu (cần thêm): **stat counters** (năm KN/số HS/% đỗ) · **timeline lộ trình** · **urgency badge khai giảng** · **FAQ thật** (đang PlaceholderSection rỗng) · **floating Zalo** · **hero trust row**
- ⚠️ Ẩn 4 placeholder rỗng (gallery/news/faq/parents) cho Personal template

**11 banner/visual:** hero full-width · section dividers · stat cards · teacher badge · course cards · timeline graphic · pricing cards · testimonial carousel · urgency banner · CTA banner · floating Zalo.

**VN-localization:** VND `1.500.000đ` · Zalo CTA · 100% tiếng Việt · cấp Tiểu/THCS/THPT · cam kết kết quả · testimonial PH+HV tên+lớp+điểm · mobile-first carousel · form 3-4 trường · 1 CTA "học thử miễn phí".

**Brand tokens thực:** primary #3B82F6 default, radius 8px, `--theme-primary/secondary/accent` RGB vars + `bg-theme-primary` class. Banner compose dùng `--theme-secondary` cho hero gradient.

**8 reference URL:** nguyenvietanh.net, vinalink, ladipage, hoola, simplepage (VN) + Preply, iTalki, Teachable (global).

## Pivot quan trọng: 3 GIẢNG VIÊN ĐỘC LẬP (không phải trung tâm)

- **Cô Đỗ Lan Khánh** — THPT Pháp luật & Đời sống (Sky Education ch3 ĐỔI theo cô Khánh) — banner navy+gold ✓ (portrait hơi lệch, cần chỉnh object-position)
- **Cô Nguyễn Thị Hà** — Tiểu học Toán (dạy Tin ở trường, dạy thêm Toán) — banner blue ✓ — §4.2 FREE
- **Thầy Nguyễn Đình Nhì** — THCS Hóa — banner green ✓ — §4.2 PREMIUM
- Portrait: `documents/08-thesis/portrait/` (committed); banner: `portrait/banners/*.png` (committed)

## Outstanding (session sau)

1. **FR fix OPTION C toàn bộ** (bảng trên) — sửa §2.1.1 + §2.2 A10 + Ch.1 §1.3.2/§1.3.3 (cross-chapter AI provider + Quality Gate) + ADD T1-T6 + reconcile O12 payment
2. **Seed rename:** Sky Education trung tâm → 3 tenant giảng viên độc lập theo môn từng người; sửa narrative Ch.3 (bỏ "Trung tâm Anh ngữ Sky Education/tiếng Anh/trung tâm" → giảng viên độc lập cô Khánh Pháp luật THPT)
3. **Landing nâng cấp:** chuyển font Be Vietnam Pro + thêm stat-counter/timeline/urgency/Zalo/hero-trust-row + ẩn placeholder; truyền query-param theme per GV + banner hero → screenshot evidence Ch.3
4. **§4.2 reframe:** 2 GV (Hà FREE + Nhì PREMIUM) — seed + landing + capture; nhấn free vs premium diff; feedback fake
5. **Capture:** 5 ảnh Ch.3 (`evidence/demo-trio/`: homepage/login/dashboard/branding/students) + ảnh §4.2
6. **RabbitMQ queue → IaC:** `class.rescheduled.queue` + `.email.queue` runtime-declared, thêm vào RabbitConfig
7. **ContentModeration dead-code:** cân nhắc xóa (orphan)

## Rules áp dụng session này
`thesis-binary-time-concept` · `diagram-format-selection` §4 (no `<br/>` sequence) · `feedback_thesis_rebake_after_fix` (NEW) · `feedback_thesis_banner_html_compose` (NEW) · `cross-flow-bug-class-sweep` · `agent-model-opus-default` (6 Opus agents: 1 seed + 4 FR audit + 2 UI audit) · `feedback_no_push_without_explicit_ask`
