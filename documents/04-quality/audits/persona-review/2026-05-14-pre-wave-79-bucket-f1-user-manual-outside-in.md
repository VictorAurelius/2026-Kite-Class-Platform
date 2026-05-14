---
title: Persona Outside-In Audit — Wave 79 Bucket F1 User Manual scope (format/media/discoverability)
status: complete
created: 2026-05-14
audit_type: outside-in
wave: 79
bucket: F1
scope: user-manual
personas: [P2_center_owner, P3_center_manager, anonymous_prospect, platform_admin]
related_inside_out_gaps: [GAP-537, GAP-559]
related_outside_in_audits: [2026-05-14-pre-wave-79-outside-in.md]
gap_targets: [GAP-563]
saas_benchmarks_cited: [Intercom Articles, Stripe Docs, Linear Method, Notion Help, Loom]
---

# Persona Outside-In Audit — Wave 79 Bucket F1 User Manual (Format / Media / Discoverability)

## Scope

Audit này **KHÔNG** lặp lại nội dung gap audit gốc (`2026-05-14-pre-wave-79-outside-in.md`) — gap đó đã cover **content gaps** (cookie consent, RBAC, invite-staff, onboarding nav, disclaimer specificity). Audit này tập trung **3 trục khác**:

1. **FORMAT** — Web-page với TOC sidebar / PDF tải về / Video / Screenshot tutorial step-by-step?
2. **MEDIA** — Image, video, GIF, screenshot annotated, code block, table?
3. **DISCOVERABILITY** — User Hằng/Tâm/Vy/Mai bị stuck → tìm help ở ĐÂU? Click ?, search, footer, Zalo, Google?
4. **META-STANDARD** — GAP-563 review standard cho user manual review process

Goal: surface gap NGOÀI GAP-537 inside-out scope (mà chỉ liệt kê "user manual screenshots per persona, 5-10 mỗi persona"), để **GAP-563 (meta standard)** + Wave 79 Bucket F1 (sample subset) có scope chính xác hơn từ đầu.

## Phương pháp

Walk through 4 personas × 5 outside-in questions (Discovery / Format / Cognitive load / VN edu context / Trust gates). Mỗi câu hỏi 2-3 phút role-play. Cross-reference với SaaS benchmark public (Intercom Articles, Stripe Docs, Linear Method, Notion Help, Loom) cho format conventions; benchmark với VN edu (Misa, KiteOS competitor, Smile, KidsPay) cho VN-specific patterns. Surface gap mà cả inside-out queue + GAP-537 hiện tại chưa cover.

Đọc kỹ:
- GAP-537 scope hiện tại: "5-10 screenshots per persona × 4 personas" — vẫn quá vague
- Wave 79 plan §3 row 6: Bucket F1 sample = anonymous-prospect persona prototype
- CSV self-test 126 rows: nhiều flow_id reference UI screens cần manual document

---

## Persona 1 — P2 Center Owner (Chị Hằng, 38, Hải Phòng, 120 hs)

**Context:** vừa nhận beta-invite, mở dashboard lần đầu, stuck onboarding step 2 (Bật dữ liệu mẫu confirm dialog). Câu hỏi đầu tiên: **"Tôi tìm hướng dẫn ở đâu?"**

### Q1: Discovery — tìm help ở đâu?

Hành vi expected của chị Hằng:
1. **Phản xạ đầu tiên:** mắt scan **góc phải header** tìm icon "?" hoặc chữ "Help". KiteHub hiện tại: KHÔNG có Help button trong topbar (verify: AdminLayout chỉ có user-email + Đăng xuất).
2. **Reflex thứ 2:** scroll xuống cuối page tìm **footer link "Hướng dẫn"**. KiteHub có Footer (Wave 78 GAP-540) với `support@kitehub.me` + Zalo, nhưng KHÔNG có link "Hướng dẫn sử dụng" / "User Manual" / "Help Center".
3. **Reflex thứ 3:** Google "kitehub hướng dẫn sử dụng" → nếu không có site-search hoặc user manual public-indexable → 0 results → bounce sang đối thủ.
4. **Reflex cuối:** mở Zalo, search "KiteHub" tìm group support → KHÔNG có Zalo OA chính thức (verify với Wave 76 GAP-440 footer scope).

**Gap surface (F1-DISC-1):** KHÔNG có entry point user manual từ dashboard. Hằng phải tự đoán URL `/help` hoặc `/docs` hoặc Google. ETA-to-help = ∞ nếu không tìm được. **P0 cho discoverability.**

### Q2: Format expectation

Chị Hằng đã quen Misa (PDF tải về) + Google Workspace (Help Center web-based với search). Kỳ vọng:

| Format | Mức ưu tiên | Lý do |
|---|---|---|
| **Web page với TOC sidebar** (Intercom style) | 🥇 P1 | Hằng đang ở browser, click nhanh, có search inline |
| **PDF tải về** | 🥈 P2 | Print share cho thư ký + giáo viên không tự tìm được, đọc offline trên xe |
| **Video walkthrough YouTube** | 🥉 P3 | Hằng dành 5 phút coi tutorial Misa khi setup; OK nếu video <5 phút |
| Live chat | ❌ | Chỉ trả lời trong giờ HC, beta volume thấp không justify |
| Screenshot tutorial step-by-step | 🥇 P1 | Screenshot CÓ ANNOTATION (mũi tên đỏ + chú thích) hiệu quả nhất cho non-tech user |

**Gap surface (F1-FMT-1):** GAP-537 scope hiện tại nói "5-10 screenshots per persona" → vague + thiếu deliverable format. Hằng cần: (a) web page Hằng đọc được trên mobile khi stuck, (b) PDF Hằng share Zalo cho thư ký, (c) screenshot có annotation (mũi tên + chú thích đỏ) thay vì screenshot trần. → cần meta-standard rõ deliverable format.

### Q3: Cognitive load

Hằng đọc manual khi đang stuck → cần **TL;DR top of page** + **5 bước numbered list** + thời gian đọc ≤ 2 phút.

Format KHÔNG chấp nhận được:
- Wall-of-text 3 paragraph mở đầu giải thích "what is KiteHub" — Hằng đã ở trong sản phẩm rồi
- Markdown table 8 columns thuật ngữ
- Code block với JSON request body — Hằng không phải dev

**Gap surface (F1-COG-1):** mỗi trang user manual cần **TL;DR box** (1 câu summary + 3-5 step bullet) ngay đầu trang, full detail bên dưới scroll-able. Chuẩn Intercom Articles + Stripe Docs.

### Q4: VN edu context

Manual SaaS quốc tế (Intercom/Stripe) tone "you" + "click button" formal English. VN edu cần:

| Yếu tố | VN edu expected |
|---|---|
| Tone | Anh/chị formal cho P2 Owner, bạn friendly cho P1 Solo Teacher (xem persona-catalog) |
| Sample data | "Lớp Anh ngữ Sky Education 5A1", "GV Trần Thị Hồng" — KHÔNG dùng "Class A1", "John Doe" |
| Date format | "Thứ Hai, 14/05/2026" thay vì "Mon May 14, 2026"; tùy chọn lịch âm bổ sung |
| Currency | "1.500.000đ" thay vì "$60.00" |
| Share via Zalo OA / Zalo group | Manual có link "Chia sẻ Zalo" button + QR Zalo OA |
| Print | A4 portrait, in được trên máy in văn phòng, font Times New Roman 12pt |

**Gap surface (F1-VN-1):** GAP-537 scope hiện tại KHÔNG cite Vietnamese narrative mandate per `.claude/rules/dev-readable-doc-language.md` § scope "End-user docs / customer-facing". Cần explicit: narrative tiếng Việt + sample data VN-friendly + Zalo share + A4 print PDF.

### Q5: Trust gates

Hằng decide trust manual qua:
- ✅ Screenshot có **timestamp / version badge** ("Cập nhật 14/05/2026, KiteHub v0.9.0-beta") — show fresh
- ✅ Mỗi trang có **"Có vấn đề? Liên hệ support@kitehub.me / Zalo 0xxx / chat trong app"** footer — support channel rõ ràng
- ✅ Sample data VN tone ("Trần Thị Hồng", "Lớp 5A1") không Lorem Ipsum placeholder
- ❌ Trust break nếu thấy: "Coming soon" placeholder, "Page not found" trên link nội bộ, screenshot blur/low-res, English UI text trong VN narrative

**Gap surface (F1-TRUST-1):** Manual cần frontmatter `last-updated` + app version + support channel ngay top of page. Chuẩn Linear Method (luôn show "Last edited X days ago" + author).

---

## Persona 2 — P3 Center Manager (Anh Tâm, 32, được Owner invite)

**Context:** Email "Chị Hằng mời bạn vào KiteHub" → click link → setup password → đăng nhập lần đầu. Câu hỏi: "Tôi phải làm gì? Đây là sản phẩm gì? Vai trò tôi là gì?"

### Q1: Discovery

Tâm tech-savvy hơn Hằng — biết SaaS, Notion, Asana. Reflex:
1. **Onboarding tour overlay** lần đầu login — KiteHub có `/onboarding` checklist (Wave 78 GAP-538) NHƯNG đó là Owner-scope; Manager invite-link onboarding flow CHƯA TỒN TẠI (xem GAP-561 → P3 Manager flow blocker).
2. Search "kitehub manager guide" trong Google → KHÔNG indexable (Phase 1 BETA private).
3. Reflex: gõ `/help` URL trực tiếp → 404 nếu chưa có route.

**Gap surface (F1-DISC-2):** Manual phải có **persona-specific landing page**: `/user-manual/manager` với câu mở đầu "Bạn được Chủ trung tâm mời tham gia KiteHub. Vai trò của bạn là quản lý lịch học + báo cáo + giám sát chấm công. 3 việc đầu cần làm: ..." — KHÔNG dùng cùng manual cho Owner + Manager.

### Q2: Format expectation

Tâm dùng Notion, biết format "Getting Started" guide chuẩn. Mong:
- **Web page với breadcrumb persona** (`Trang chủ / Hướng dẫn / Manager / Tổng quan`)
- **Search inline** trong manual (Algolia DocSearch style) — Stripe Docs benchmark
- **Code block** không cần (Tâm không phải dev)
- **Diagram flow chart** "Đăng nhập → Dashboard → Lịch học → Báo cáo" — Tâm parse diagram nhanh hơn text

### Q3: Cognitive load

Tâm chấp nhận đọc 5-10 phút first day setup, sau đó skim reference khi cần. → Manual chia 2 layer:
- **Quick Start** (5 phút, 5-7 trang) — "First day with KiteHub"
- **Reference** (full feature, on-demand) — "Lịch học", "Chấm công", "Báo cáo"

### Q4 + Q5

Trust gates tương tự Hằng. Bonus: Tâm sẽ check **changelog page** ("New in this version" Linear style) để biết feature mới khi version update.

**Gap surface (F1-FMT-2):** Manual cần 2-tier structure (Quick Start + Reference) + persona-specific landing + changelog page. → impacts meta-standard.

---

## Persona 3 — Anonymous Prospect (Em Vy, 24, intern marketing)

**Context:** Research pitch sếp. Tìm "demo / hướng dẫn sử dụng" để show sếp.

### Q1: Discovery — vào public surface

Vy chưa có account. Reflex:
1. Landing page → tìm **"Demo"** / **"Xem trước"** / **"Hướng dẫn"** trên top nav. KiteHub hiện tại landing top nav: chỉ có Pricing + Yêu cầu Beta (verify Wave 78 surface).
2. Footer → tìm "Help Center" / "Documentation" link. KiteHub Footer: support email + Zalo, KHÔNG có link manual public.
3. Google "kitehub demo video" → expected: video YouTube 2-3 phút screen-record của Owner journey.

**Gap surface (F1-DISC-3):** **Public-indexable user manual phải có** từ landing nav OR footer. Sếp Vy không signup beta → cần public preview content. Chuẩn Stripe (full docs public) + Notion Help (public + search).

### Q2 + Q3: Format expectation cho prospect

Vy collect info → pitch sếp Excel:
- **Video walkthrough 2-3 phút** show Owner journey "từ signup tới tạo lớp đầu tiên" — Vy embed link vào Slack/Zalo gửi sếp
- **PDF brochure 4-6 trang** "What is KiteHub" + 5 use case + pricing — Vy print attach email pitch
- **Comparison page** "KiteHub vs Misa vs KidsPay" — table feature × competitor — Vy reference khi sếp hỏi

**Gap surface (F1-FMT-3):** Beyond per-persona screenshots, cần **marketing-tier collateral**:
- 1 video YouTube demo 2-3 phút (KHÔNG voiceover phức tạp, music + caption đủ)
- 1 PDF brochure "Pitch deck" cho prospect
- 1 comparison page

GAP-537 scope KHÔNG cover marketing-tier. → file riêng OR expand GAP-537 scope OR defer Wave 80.

### Q4: VN edu context

Sếp Vy là chủ trung tâm tiếng Anh ~200 hs, U50, ít tech. Cần:
- Brochure A4 print (2 trang in 2 mặt)
- Sample data VN ("Trung tâm Anh ngữ Phương Đông, 200 hs, 8 chi nhánh")
- Pricing trong VND không USD

### Q5: Trust gates cho prospect

- ✅ Testimonial từ trung tâm thực + logo + tên người + chức danh
- ✅ Screenshot product có timestamp current
- ✅ "Đã hoạt động ổn định N tháng, phục vụ N trung tâm" — proof point
- ❌ Trust break: "Coming soon banner" trên landing manual (Phase 1 BETA chỉ có 0 testimonial → cần workaround: dùng "Đang chạy beta cùng N trung tâm tiên phong" thay testimonial cuối)

---

## Persona 4 — Platform Admin (Bạn Mai, 27, internal new hire)

**Context:** New ops/dev join team, cần self-onboard.

### Q1: Discovery — runbook nội bộ vs user docs

Mai phân biệt 2 nguồn:
- **User-facing docs** (`documents/05-guides/user-manual/`) — cho tenant (Hằng/Tâm/Vy)
- **Internal runbook** (`documents/05-guides/operations/` per `.claude/rules/deployment-naming-convention.md`) — cho ops daily

Mai vào repo Git, navigate → KHÔNG cần discoverability trên UI (Mai có access GitHub).

### Q2: Format expectation

Mai prefer **markdown trong Git** + **mermaid diagram cho flow** + **table reference**. Đã có chuẩn từ `dev-readable-doc-language.md` + `docs-folder-structure.md`. → User manual scope KHÔNG cần serve Mai persona.

### Q3-5: Cognitive load + trust + VN

Mai parse markdown nhanh, không cần TL;DR. Trust qua git log + last-updated frontmatter.

**Gap surface (F1-ADMIN-1):** Manual scope cho admin **không nên** ở `documents/05-guides/user-manual/` — đó là tenant-facing. Admin runbook đã có `documents/05-guides/operations/` + `documents/05-guides/remote-access/` etc. → GAP-537 scope "platform-admin persona" có thể overlap với existing internal runbook. Clarify scope: user-manual = tenant-facing (Hằng/Tâm/Vy), admin = nội bộ Git.

---

## Format + Media Recommendations

Tổng hợp từ 4 persona × 5 questions:

### Top 3 deliverable formats (must-have cho Bucket F1 sample)

1. **Web page với TOC sidebar** (per persona section)
   - Path: `kitehub-frontend/src/app/(public)/help/*` (Next.js MDX hoặc markdown render)
   - URL pattern: `/help/{persona-slug}/{topic-slug}`
   - Top nav entry: "Hướng dẫn" (Help)
   - Sidebar: TOC scrollable + persona switcher dropdown
   - Search: client-side (Fuse.js) cho Phase 1, Algolia DocSearch Phase 1.5+

2. **PDF tải về** (auto-generated từ web markdown)
   - Render via `mdpdf` hoặc `weasyprint` (đã có precedent trong `scripts/render-acceptance-test-xlsx.sh` pattern — XLSX render script reusable cho PDF)
   - Path: `documents/05-guides/user-manual/{persona}/*.md` (source canonical) → CI build `*.pdf` artifact
   - PDF gitignored, regen on-demand per `test-artifact-format-standard.md` §4.2 pattern

3. **Screenshot có annotation** (mũi tên đỏ + chú thích viền vàng)
   - Tool: GIMP / Figma export PNG + post-process; hoặc Playwright + Sharp programmatic
   - Resolution: 1440×900 desktop + 375×812 mobile
   - Locale: vi-VN browser locale → text Vietnamese
   - Naming: `screenshots/{persona}/{topic}-{step-num}.png`

### Defer Phase 1.5+ (NOT block Wave 79 Bucket F1)

- Video YouTube walkthrough (2-3 phút) — needs voiceover review + brand polish
- PDF brochure marketing cho anonymous prospect — overlap với landing page GAP-541
- Comparison page KiteHub vs Misa vs KidsPay — needs legal/competitive review

---

## Meta-Standard Recommendations cho GAP-563

GAP-563 (META — user manual review standard) phải define:

### Checklist mandatory per user manual page

- [ ] **Frontmatter:** `persona`, `topic`, `last-updated`, `version` (app version), `effort_minutes` (estimated read time)
- [ ] **TL;DR box** đầu trang (1 câu + 3-5 bullet step) — Intercom/Stripe convention
- [ ] **Persona-specific landing page** (`/help/p2-owner`, `/help/p3-manager`, `/help/anonymous`) — không cùng manual cho mọi role
- [ ] **Screenshot có annotation** (mũi tên đỏ / viền vàng / số bước) — không screenshot trần
- [ ] **Sample data VN-friendly** ("Trung tâm Anh ngữ Sky Education", "Trần Thị Hồng") — không Lorem Ipsum / English placeholder
- [ ] **Currency VND** ("1.500.000đ") không USD
- [ ] **Date format VN** ("Thứ Hai, 14/05/2026") không "Mon May 14"
- [ ] **Vietnamese narrative** per `.claude/rules/dev-readable-doc-language.md` — code-shaped token English giữ nguyên (HTTP, URL, JWT...)
- [ ] **Support footer** mỗi trang: `support@kitehub.me` + Zalo + "Báo lỗi trang này" link (mailto subject pre-fill page URL)
- [ ] **Last-updated badge** visible + auto-update từ git log
- [ ] **Print-friendly CSS** (`@media print { ... }`) — A4 portrait, font Times New Roman, no nav/sidebar
- [ ] **Mobile responsive** — tested ≥360px viewport
- [ ] **Accessibility WCAG AA** — heading hierarchy, alt text screenshot, contrast ratio ≥4.5:1
- [ ] **Discoverability entry points** ≥3:
  - Header nav button "Hướng dẫn" / "?"
  - Footer link "Hướng dẫn sử dụng"
  - Onboarding step "Xem hướng dẫn" CTA
- [ ] **Search functional** (Fuse.js v1 hoặc Algolia v1.5+) — index page title + headings + first 200 chars

### Discoverability matrix (must-have per persona)

| Persona | Entry point 1 | Entry point 2 | Entry point 3 |
|---|---|---|---|
| P2 Owner (Hằng) | Header "?" button | Footer "Hướng dẫn" | Onboarding step CTA |
| P3 Manager (Tâm) | First-login overlay tour | Header "?" button | Persona landing page direct URL trong invite email |
| Anonymous (Vy) | Landing top nav "Hướng dẫn" | Google indexable | Footer "Documentation" |
| Platform Admin (Mai) | repo `documents/05-guides/operations/` | GitHub navigation | N/A (internal scope) |

### Review process cho user manual

Per `.claude/rules/output-review-mandate.md` §3, GAP-563 PHẢI add row matrix:

| Output Type | Review Standard | Process | Reviewer |
|---|---|---|---|
| **User manual pages** | GAP-563 checklist (15 items trên) + WCAG AA + VN narrative | Per-page pre-merge | Author + UI reviewer + 1 native VN reader |

---

## Wave 79 Bucket F1 Sample Scope — Refinement Recommendation

Wave 79 plan hiện tại §3 row 6 (Bucket F) ship full GAP-537 (4 personas × 5-10 screenshots). Recommendation:

### Bucket F1 (sample subset cho Wave 79)

Ship **anonymous prospect persona only** với 5 pages prototype:
1. `/help/anonymous` — Welcome + 4 use-case cards (Owner / Solo Teacher / Manager / Parent)
2. `/help/anonymous/pricing-faq` — common questions sếp Vy hỏi
3. `/help/anonymous/getting-started` — "Cách bắt đầu dùng thử KiteHub" 5 bước
4. `/help/anonymous/comparison` — placeholder "Comparison đang được hoàn thiện" + cite competitors (Misa/KidsPay/Smile) — Wave 80 fill
5. Header nav + Footer link entry points wired

Tại sao **anonymous-only** cho F1:
- Validate template/format **TRƯỚC** khi invest cho P2/P3/Admin (avoid rework)
- Anonymous personalization risk thấp nhất (public surface, no auth)
- Quickly verify discoverability gates (header nav + footer + Google indexable)
- Sample 5 pages đủ cover format checklist GAP-563 above (Bucket F1 hands-on test of GAP-563 standard)

### Defer to Wave 80+ (NOT block Wave 79 launch)

- P2 Owner persona full manual (10 pages — onboarding, class mgmt, payment, settings)
- P3 Manager persona (5-7 pages — schedule, attendance, reporting)
- Video walkthroughs (3 personas × 1 video each)
- Marketing PDF brochure
- Comparison KiteHub vs Misa vs KidsPay (legal review needed)
- Algolia DocSearch v1.5+

---

## SaaS Benchmark Comparison

| Benchmark | What KiteHub user manual should adopt |
|---|---|
| **Intercom Articles** | TL;DR box top + screenshot annotated + "Was this helpful?" feedback widget |
| **Stripe Docs** | Public + indexable + persona-tabbed examples + version badge |
| **Linear Method** | Last-updated badge + changelog page + opinionated minimal navigation |
| **Notion Help** | TOC sidebar + inline search + nested category structure |
| **Loom** | Video embed inline + 2-3 phút walkthrough + caption auto-generated |
| **Misa (VN)** | PDF tải về A4 + Zalo support group link + lịch âm tooltip |
| **KiteOS (VN edu competitor)** | Persona-specific landing + screenshot annotated tiếng Việt + tone formal |

KiteHub user manual = Intercom format + Stripe public surface + Misa PDF availability + KiteOS VN tone.

---

## Gap Filing Recommendation

### GAP-563 (META — user manual review standard)

**Title:** GAP-563: Meta standard for user manual review (checklist + persona matrix + discoverability gates)

**Priority:** P1 (block Wave 79 Bucket F1 launch — F1 sample must apply standard, otherwise F1 ship lệch và rework Wave 80)

**Status:** 🔵 OPEN (META P0 force-multiplier per `.claude/rules/meta-gap-priority.md` §3 — meta gap precedes feature gap GAP-537)

**Scope:**
- Define 15-item checklist (frontmatter + TL;DR + persona landing + screenshot annotated + VN narrative + support footer + last-updated + print CSS + mobile + WCAG + discoverability ≥3 + search)
- Define discoverability matrix (4 persona × 3 entry points)
- Add row to `output-review-mandate.md` §3 "User manual pages"
- Reference benchmark: Intercom + Stripe + Linear + Notion + Loom + Misa + KiteOS
- Ship as `.claude/rules/user-manual-content-standard.md` (new rule) OR `documents/05-guides/user-manual/README.md` (standard doc) — recommend rule file for governance enforcement parity per `rule-change-process.md` §6.5

**Acceptance Criteria:**
- [ ] Rule/standard file ship same Wave 79 PR with Bucket F1 sample
- [ ] Bucket F1 anonymous-prospect 5-page prototype demonstrably pass 15-item checklist
- [ ] `output-review-mandate.md` §3 add "User manual pages" row
- [ ] GAP-537 scope refined to reference GAP-563 standard (no longer vague "5-10 screenshots")

### Refinement to GAP-537 (NO new gap needed; update existing)

Update GAP-537 §Proposed Fix: "Follow GAP-563 standard. Bucket F1 ship anonymous-prospect 5-page prototype Wave 79. Bucket F2/F3 ship P2/P3 manuals Wave 80+. Defer videos + marketing brochure Wave 81+."

### Discoverability gap (consolidate into GAP-563)

F1-DISC-1 + F1-DISC-2 + F1-DISC-3 (no header "?" button, no footer link, not public-indexable) consolidate vào GAP-563 §discoverability matrix. KHÔNG file riêng.

---

## Inside-out vs Outside-in (this audit) Coverage Map

| Inside-out gap | This audit cover? | Surfaced delta? |
|---|---|---|
| GAP-537 (user manual screenshots per persona) | ✅ — covered with format/media/discoverability extension | YES — scope was vague, this audit refines into 15-item checklist + persona matrix + discoverability ≥3 + 2-tier structure (Quick Start + Reference) |
| GAP-559 (dashboard CTA + nav cho /onboarding) | ⚠️ partial — F1-DISC-1 ("?" button) overlaps but scope different (manual entry point vs onboarding entry point) | NO new gap; clarify GAP-559 covers onboarding nav, GAP-563 covers manual nav |

---

## Recommendations cho Wave 79 plan §1 Brainstorm Q1

### Must-add P1 (block Bucket F1 launch quality):

1. **GAP-563** META user manual review standard (15-item checklist + discoverability matrix) — ship same PR with Bucket F1 sample

### Refinement (existing gap, no new filing):

2. **GAP-537** scope refined: Bucket F1 = anonymous-prospect 5-page sample Wave 79; P2/P3 manuals defer Wave 80+

### Defer Wave 80+:

3. Video walkthrough (3 personas)
4. PDF brochure marketing prospect
5. Comparison KiteHub vs competitors (legal review)
6. Algolia DocSearch upgrade

---

## Meta Observation — outside-in caught format/media/discoverability blind spot

Pattern lặp lại: dev brainstorm gap "user manual screenshots per persona" → focus **deliverable count** (5-10 screenshots × 4 personas = 20-40 screenshots). Outside-in audit caught **deliverable QUALITY + STRUCTURE + ENTRY POINTS** không có trong inside-out scope:

- **Format diversity** — không chỉ screenshot; web page + PDF + annotated screenshot + (defer) video
- **Persona-specific landing** — không cùng manual cho mọi role
- **Discoverability ≥3 entry points** — Hằng/Tâm/Vy stuck → 90% time bottleneck là "không tìm được manual" không phải "manual sai"
- **Cognitive load TL;DR top + 5-bullet pattern** — Hằng đọc manual khi stuck, không phải khi rảnh
- **VN edu context** — sample data + currency + date + Zalo share + A4 print
- **Trust gates** — last-updated badge + support footer + version sync

→ Outside-in audit là **force multiplier** cho gap quality. Per `.claude/rules/outside-in-coverage-trigger.md` Wave 79 plan §1 Brainstorm Q1 should integrate this audit finding TRƯỚC khi F1 spawn.

---

## References

- Sister audit: `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (parent audit, content gaps)
- Sister audit: `documents/04-quality/audits/persona-review/2026-05-14-phase-1-beta-persona-walkthrough.md` (pre-Wave-73 persona walkthrough)
- Rule: `.claude/rules/outside-in-coverage-trigger.md` (mandates this audit BEFORE plan lock)
- Rule: `.claude/rules/dev-readable-doc-language.md` §2 row "End-user docs / customer-facing" (Vietnamese narrative)
- Rule: `.claude/rules/output-review-mandate.md` §3 (review standard matrix — GAP-563 adds row)
- Rule: `.claude/rules/meta-gap-priority.md` §3 (META P0 force multiplier — GAP-563 precedes GAP-537)
- Rule: `.claude/rules/rule-change-process.md` §6.5 Enforcement Parity Mandate
- Personas catalog: `documents/00-brd/personas-catalog.md`
- Wave 79 scope: `documents/03-planning/waves/wave-2026-05-14-79-beta-invite-close-out.md` §3 row 6 (Bucket F)
- SaaS benchmarks (public refs): Intercom Articles, Stripe Docs, Linear Method, Notion Help, Loom
- VN edu benchmarks (competitor refs): Misa, KiteOS, Smile, KidsPay
- CSV self-test: `documents/05-guides/operations/acceptance-tests/phase-1-beta-acceptance-self-test.csv` (126 rows, many reference UI screens to document)
