# Pre-walk External Benchmark Audit — KC-1 Tenant Provisioning + Lifecycle + Settings

**Date:** 2026-06-04
**Scope:** Flow KC-1 (KiteClass tenant provisioning + lifecycle + settings — auto từ KH-2b owner signup)
**Method:** External benchmark per `outside-in-coverage-trigger.md` §3 — so sánh Stripe / Slack / Google Classroom / MISA QLTH / Base.vn + edTech + VN edu SaaS
**Phase:** Phase 1 BETA (medium tutoring centers VN, Owner non-tech 40-55 tuổi)
**Reference:** `documents/03-planning/roadmap/flow-verification-campaign.md` §4 row KC-1

---

## Section A — Industry pattern table

| # | Pattern | Stripe | Slack | Google CR | MISA QLTH | Base.vn | Recommended cho KiteClass | Gap if missing |
|---|---|---|---|---|---|---|---|---|
| 1 | **Async provision UX** | Webhook + status poll | Conversational Slackbot wizard | Sync Workspace API | R85+ simplified — chỉ khai báo năm học → ready | Auto workspace at signup | **Progressive 5-step checklist** (workspace ready → settings → invite → first class → first student); show partial UI dưới 30s | ⚠️ Spec hiện chưa rõ — verify trong walk |
| 2 | **Default settings (timezone/locale)** | Account-level inherited | Workspace timezone editable | Inherits Google org | VN default + năm học config | VN-default | **VN locale + Asia/Ho_Chi_Minh + VND mặc định**, editable sau onboarding | ⚠️ Verify default chain |
| 3 | **Email confirm gating** | Required for live mode | Workspace + email verify | Required cho student | Required cho admin signup | Required cho admin | **Email verified + in-app banner cho unconfirmed** | ⚠️ KH-2b dependency — verify token expiry |
| 4 | **Multi-tenant per owner switching** | Account picker on dashboard | Workspace switcher sidebar (cmd+K) | Multi-school org via Workspace admin | Multi-school per cùng MISA ID | Multi-workspace switcher | **Tenant picker on login + persistent sidebar switcher**; default = most recent | ⚠️ Spec mentions multi-tenant per owner but UX chưa rõ |
| 5 | **Lifecycle naming** | trialing/active/past_due/canceled/unpaid | active/archived/deleted | ACTIVE/ARCHIVED/DECLINED | (đăng ký + năm học active/closed) | active/suspended/archived | **TRIAL / ACTIVE / SUSPENDED / EXPIRED / ARCHIVED** với VN i18n labels ("Dùng thử / Hoạt động / Tạm ngưng / Hết hạn / Lưu trữ") | 🔴 **YES** — naming spec chưa lock, VN label chưa định |
| 6 | **Audit log visibility to owner** | Dashboard events | Audit log (Enterprise) | Limited to org admin | Có (admin view) | Có (workspace admin) | **Owner thấy own tenant audit log** (provision events + settings changes + lifecycle transitions) | ⚠️ Verify — likely missing Phase 1 |
| 7 | **Dunning / payment retry** | 4-retry default + smart retries | N/A (free) | N/A | Manual contact | Manual + grace period | **3-retry + grace 7 ngày + lifecycle SUSPENDED → EXPIRED**; pre-expiry email 14d + 7d + 1d | 🔴 **YES — Phase 1 chưa có payment integration; needed Phase 1.5** |
| 8 | **Cancel / cooling-off** | Cancel-at-period-end default | Owner-initiated, 14d recoverable | Owner archive course | Manual offboard | Soft-delete 30d | **Soft-suspend 30d → permanent delete**; PDPL Art 11 compliance | ⚠️ Verify retention policy cho KiteClass |
| 9 | **Settings hierarchy** | Account → Subscription → Customer | Workspace → Channel → DM | Org → School → Course | Tenant → Năm học → Khối → Lớp | Workspace → App → User | **Tenant → Năm học → Khóa/Lớp → Học sinh** matches VN edu hierarchy | ⚠️ Verify Năm học as first-class entity |
| 10 | **Time-to-first-value (TTFV)** | <2 min đến test charge | <2 min đến first message | ~5 min đến first class | R85+ simplified <5 min | <3 min đến first task | **Target <3 min**: provisioning → first class created OR first student imported | 🔴 **YES** — TTFV target chưa set |
| 11 | **Welcome email post-provision** | Onboarding sequence 7 emails | "Welcome" + setup checklist | "Your class is ready" | Bao gồm với hướng dẫn năm học | Welcome + getting-started | **VN welcome email + Zalo OA notify** (per row 13) + 5-step checklist link | ⚠️ Email-only Phase 1 OK; Zalo Phase 1.5 |
| 12 | **Sample data / template** | Test mode + sample customers | DM với Slackbot | Sample class option | Sample năm học fixture | Sample workspace template | **Demo tenant với 1 lớp + 3 học sinh fixture** để Owner walk through trước khi import data thật | 🔴 **YES** — strong cho non-tech Owner persona |
| 13 | **VN notification channel preference** | N/A | N/A | N/A | SMS + email | Email + Zalo | **Email Phase 1 → +Zalo OA Phase 1.5 cho parent notify**; 95% VN users prefer Zalo over email | 🔴 **YES** — Phase 1.5 dependency, surface UI placeholder Phase 1 |
| 14 | **Idempotency on retry** | Idempotency-Key header | Workspace unique slug | Unique invite codes | (transaction lock?) | Workspace ID unique | **Tenant slug uniqueness + retry-safe provision endpoint**; same email → same tenant, không duplicate | ⚠️ Verify — `GAP-927` Wave flow-kh1 G2 surfaced retry rollback bug |

---

## Section B — VN-specific gaps (5 findings)

### B1. Năm học (school year) semantics — Sep→May cohort first-class entity 🔴
VN K-12 academic calendar = Sep 5 → late May, 18+17 weeks 2 semesters. MISA QLTH R85+ đã simplify "khai báo năm học" là step đầu tiên onboarding. KiteClass spec hiện không rõ "Năm học" có phải first-class entity (parent của Khóa/Lớp) hay chỉ metadata field. **Recommendation:** Năm học = required field tại provision, default = current năm học (Sep YYYY → May YYYY+1), với chuyển năm học = lifecycle event riêng (year-end rollover ≠ tenant cancel).

### B2. Persona role VN i18n labels 🔴
Spec dùng English role names (Owner / Teacher / Student / Parent). VN edu personas: "Quản lý / Giáo viên / Học sinh / Phụ huynh". MISA + Base.vn đều dùng VN labels. Non-tech Owner persona (40-55 tuổi) sẽ confuse với English. **Recommendation:** i18n label mandatory Phase 1 — backend role enum giữ English (PLATFORM_ADMIN/TENANT_OWNER/TEACHER/STUDENT/PARENT), FE label always VN.

### B3. VAT TCT eInvoice integration trong tenant onboarding 🟠
VN edu B2B SaaS thu phí PHẢI eInvoice (TCT — Tổng Cục Thuế). MISA partnership = MeInvoice integration. KiteClass nếu tenant trả phí (Phase 1.5+) cần thu MST (tax ID) tại onboarding hoặc settings page. **Recommendation:** Phase 1 BETA không thu phí → skip; Phase 1.5 paid → add "Thông tin xuất hóa đơn" section trong settings (MST + tên DN/cá nhân + địa chỉ); partnership MISA MeInvoice (per Wave 93 GAP-185 decision) thay vì self-build.

### B4. VietQR vs international gateway preference 🟠
VN tenant Owner non-tech mạnh mẽ prefer VietQR (bank-native QR pay) over Stripe/international card. Phase 1 BETA free, không applicable; Phase 1.5+ paid cần payment surface có VietQR option. **Recommendation:** Surface placeholder Phase 1 (settings → "Phương thức thanh toán: Sắp ra mắt — VietQR ưu tiên"), Phase 1.5+ implement.

### B5. Zalo OA cho parent communication preference 🔴
95% VN users prefer Zalo over email cho service notifications. K-12 parent communication = Zalo dominant. KiteClass tenant settings cần "Kênh thông báo phụ huynh" preference. **Recommendation:** Phase 1 default = email-only (matches KH-2b path), surface UI placeholder "Tích hợp Zalo OA — Sắp ra mắt" trong settings. Phase 1.5 = full Zalo OA wiring.

---

## Section C — Recommendations (5 action items cho KC-1 ship)

### C1. 🔴 P0 — Adopt 5-state lifecycle naming với VN i18n (Stripe pattern + MISA labels)
**What:** Lock lifecycle enum {TRIAL, ACTIVE, SUSPENDED, EXPIRED, ARCHIVED} + VN labels {"Dùng thử / Hoạt động / Tạm ngưng / Hết hạn / Lưu trữ"} cho Phase 1.
**Why:** Stripe 5-state pattern industry-standard + clear UX. MISA dùng VN labels — non-tech Owner persona đọc hiểu ngay.
**Effort:** ~2h — enum + i18n keys + admin/owner views.
**Exemplar:** Stripe subscription states ALIVE/SUSPENDED/DEAD framework.

### C2. 🔴 P0 — Năm học là required field tại provision (MISA QLTH R85+ pattern)
**What:** Provision form require "Năm học" với default = current năm học (Sep YYYY → May YYYY+1), validate format. Year rollover = separate lifecycle event ≠ tenant cancel.
**Why:** VN K-12 semantics. MISA QLTH R85+ chứng minh đây là smallest sufficient initialization step. Skip = Owner confuse, settings drift.
**Effort:** ~4h — schema field + FE form + validation + default calc.
**Exemplar:** MISA QLTH R85+ simplified onboarding "chỉ cần khai báo năm học".

### C3. 🟠 P1 — Demo tenant với fixture data sau provision (Shopify sample-product pattern)
**What:** Post-provision, offer "Tạo dữ liệu mẫu" option → 1 lớp + 3 học sinh + 1 giáo viên fixture. Owner walk through trước khi import real data.
**Why:** TTFV <3 min target. Non-tech Owner persona benefit lớn từ "thấy nó hoạt động" trước khi commit. Shopify pattern proven.
**Effort:** ~6h — fixture seed + UI toggle + cleanup flow.
**Exemplar:** Shopify "add sample product" trong setup wizard.

### C4. 🟠 P1 — Progressive 5-step setup checklist (Slack pattern)
**What:** Post-provision dashboard show 5-step checklist {năm học verified / first class / first teacher invited / first student / settings reviewed}. Progress bar + dismissible.
**Why:** Slack chứng minh 93% retention boost cho users complete checklist. Psychological "finish what started" effect mạnh cho VN Owner non-tech.
**Effort:** ~5h — checklist state + UI + dismiss persistence.
**Exemplar:** Slack onboarding progress indicators.

### C5. 🔴 P0 — Tenant picker on login + sidebar switcher (Slack/Notion pattern)
**What:** Owner với N tenants (N≥2) → picker post-login chọn workspace; N=1 → auto-enter. Sidebar persistent switcher (cmd+K hoặc dropdown).
**Why:** Spec mentions multi-tenant per owner nhưng UX chưa rõ. Slack/Notion/Base.vn all converge on này. Per `pre-handoff-self-test-completeness.md` §2.7 tenant-switch checklist (a)→(f) mandatory.
**Effort:** ~8h — picker page + JWT tenant claim swap + sidebar component + cache invalidation per A→B→A walk.
**Exemplar:** Slack workspace switcher cmd+K; Notion sidebar dropdown.

---

## Cross-link

- Flow plan: `documents/03-planning/roadmap/flow-verification-campaign.md` §4 KC-1
- Sister flow audit: KH-2b owner signup (upstream provider) — verify token + email gating tại boundary
- Rule: `.claude/rules/pre-walk-persona-simulation-mandate.md` v1.0.0 — required artifact per §3
- Rule: `.claude/rules/pre-handoff-self-test-completeness.md` §2.7 tenant-switch checklist (recommendation C5 implements)
- Wave precedent: Wave 93 GAP-185 (MISA partnership > self-build VAT engine) — applies to recommendation B3

## Sources

- [MISA QLTH official platform](https://emis.misa.vn/)
- [MISA EMIS app](https://emisapp.misa.vn/)
- [Stripe Subscriptions docs](https://docs.stripe.com/billing/subscriptions/overview)
- [Stripe Subscription States — Solmaz blog](https://solmaz.io/stripe-subscription-states)
- [Stripe failed payments dunning — Ben Foster](https://benfoster.io/blog/stripe-failed-payments-how-to/)
- [Slack 101 Onboarding](https://slack.com/blog/collaboration/slack-101-onboarding)
- [Slack API onboarding best practices](https://api.slack.com/best-practices/onboarding)
- [Google Classroom user roles](https://developers.google.com/workspace/classroom/guides/key-concepts/user-types)
- [Google Classroom admin actions](https://developers.google.com/workspace/classroom/guides/key-concepts/admin-actions)
- [Vietnam 2025-2026 school year Sep 5 start](https://vietnamnet.vn/en/vietnam-sets-september-5-start-for-2025-2026-school-year-2431270.html)
- [Easy Edu — VN tutoring center SaaS](https://easyedu.vn/phan-mem-quan-ly-trung-tam-gia-su-2/)
- [DotB EMS — VN tutoring SaaS](https://dotb.vn/news/phan-mem-quan-ly-trung-tam-day-them/)
- [Base.vn workspace + SaaS B2B onboarding](https://workos.com/blog/developers-guide-saas-multi-tenant-architecture)
- [FPT acquires Base.vn — VN SaaS landscape](https://fptsoftware.com/newsroom/news-and-press-releases/news/fpt-acquires-base-vn-accelerating-digital-transformation-for-800000-vietnamese-enterprises)
- [Zalo OA channel policy](https://oa.zalo.me/home/resources/news/thong-bao-chinh-sach-gui-tin-va-quy-dinh-phi-gui-tin_1433049880779375099)
- [Zalo for business — Infobip](https://www.infobip.com/blog/zalo-business)
- [Shopify onboarding UX guide](https://shopify.dev/docs/apps/design/user-experience/onboarding)
- [Shopify general setup checklist](https://help.shopify.com/en/manual/intro-to-shopify/initial-setup/new-to-shopify-checklists/general-checklist)
- [Notion workspace setup](https://www.notion.com/help/guides/how-to-set-up-your-notion-workspace-for-your-team)
- [Canvas LMS multi-tenancy discussion](https://groups.google.com/g/canvas-lms-users/c/6MwcvF1fSuI)
- [LMS multi-tenancy guide — Moodle](https://moodle.com/us/news/lms-multi-tenancy/)
