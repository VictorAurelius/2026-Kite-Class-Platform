# Prompt Library — for Claude Design (claude.ai/design)

Round 2+ prompts to feed Claude Design after uploading the 10 dossier files. Paste in order — opener first, then per-direction deep-dives as user picks them.

**Workflow:**
1. Open new chat in Claude Design
2. Upload all 10 `.md` dossier files + this `prompts.md` (single batch)
3. Paste **§1 Round 2 opener** → wait for Claude Design to confirm context loaded
4. Paste **§2 Round 2 main directive**
5. Review output → if quality gate met, port to production via Track 2
6. If not met, paste targeted **§3 / §4 / §5 / §6** prompts for that specific direction

---

## §1 — Round 2 Opener (paste FIRST, after upload)

```
Tôi đã upload 11 file dossier mô tả thực trạng dự án Kite Platform (KiteHub +
KiteClass). Trước khi response bất cứ điều gì, đọc HẾT theo thứ tự:

1. README.md — entry point + glossary
2. 01-personas.md — 5 BRD personas (Solo Teacher / Center Owner / Medium Center
   Admin / K-12 Principal / Student / Parent)
3. 02-vietnamese-ux-musts.md — VN UX patterns (currency, date, phone, payment
   gateways, identity docs, MoET requirements)
4. 03-screen-inventory.md — 63 routes với UI score /128 hiện tại
5. 04-component-gaps.md — 12 components THIẾU bundle Round 1
6. 05-business-flows.md — 10 user flows critical
7. 06-quality-bar.md — UI audit /128 rubric + WCAG AA + perf budget
8. 07-existing-pain-points.md — 14 lowest-scoring screens (33-78/128)
9. 08-direction-decisions.md — 4 quyết định cứng cho Round 2
10. 09-tech-constraints.md — Stack lock (Next.js 15, React 19, Tailwind 3.4,
    shadcn, Radix, lucide, etc.)
11. 10-acceptance-criteria.md — Checklist 100 items cho mỗi deliverable

Confirm context loaded bằng cách trả lời 5 câu hỏi này:
1. Direction nào là HIGHEST priority?
2. Direction D pivot thành cái gì? (mobile native? hay khác?)
3. AI Branding Round 2 phải tích hợp vào đâu?
4. Quality target /128 cho Round 2 là bao nhiêu?
5. Mock data: tên người mock như thế nào? (VN cụ thể, không John Doe)

Nếu trả lời sai bất kỳ câu nào, RE-READ dossier rồi answer lại. Đừng bắt đầu
design cho đến khi 5/5 đúng.
```

---

## §2 — Round 2 Main Directive (paste AFTER §1 confirmed)

```
ROUND 2 deliverables — 5 items. KHÔNG redo foundation (giữ tokens trong
colors_and_type.css). Nếu thấy item nào ambiguous, HỎI TRƯỚC, đừng đoán.

DELIVERABLE 1 — kiteclass-pro v2 (Direction B HIGHEST PRIORITY)
- Persona: P2 Center Owner (xem 01-personas.md)
- Screens: KC /dashboard (363 LOC, score 84) + sub-screens
- Tính năng must-have:
  - ⌘K command palette (20+ commands, group Search/Action/Navigation)
  - Sparkline mini-chart trong stat cards
  - Skeleton loaders khắp nơi
  - Dark mode polished (sun→moon morph animation)
  - Drag-drop widget grid với state shape spec
  - Toast confetti on success milestones
- Output: ui_kits/kiteclass-pro-v2/{README.md, styles.css, index.html, app.jsx,
  screens/{default,loading,empty,error,success,dark}.html}

DELIVERABLE 2 — kiteclass-teacher (NEW)
- Persona: Homeroom teacher (GVCN) + subject teacher
- Screens: KC /classes/[id]/attendance (Flow #3), /classes/[id]/grades (NEW),
  /classes/[id]/schedule (NEW), /attendance/reports
- Components: G2 Attendance Roster + G3 Gradebook + G4 Schedule Manager + G8
  Attendance Calendar (xem 04-component-gaps.md)
- Density: dense (P3 admin level)
- Touch UX: tablet-friendly (44×44 tap targets) — teachers dùng tablet ở lớp

DELIVERABLE 3 — kiteclass-parent (Direction D PIVOT — web responsive)
- Persona: Pa. Parent (mobile primary, low-medium tech literacy)
- KHÔNG phải native mobile app. PHẢI là web responsive + PWA-grade.
- Screens: KC /parent (Wave 2 placeholder hiện tại) + redesign từ đầu
- Mobile-first 320-414px primary, tablet 768px secondary, desktop 1440px tolerable
- Tính năng:
  - Bottom tab navigation (3-4 tabs: Trang chủ / Học bạ con / Học phí / Cài đặt)
  - Pull-to-refresh
  - PWA manifest + Service Worker spec
  - Web Push notification card (with Zalo OA fallback design)
  - One screen = one task (parent có ít thời gian, low cognitive load)
- Vocabulary: rất đơn giản, không jargon
- Mock: theo Flow #6 trong 05-business-flows.md

DELIVERABLE 4 — ai-branding-wizard-v2 (Direction C INTEGRATED)
- Persona: P2 Center Owner first-time setup, P3 Admin rebrand
- KHÔNG phải playground tách rời. PHẢI là 6-step wizard tích hợp.
- Refactor playground Round 1 → 6 steps:
  1. Welcome + tenant info
  2. Logo upload (optional)
  3. Audience picker (4 options trong 05-business-flows.md Flow #8)
  4. Tone picker (4 options)
  5. Template picker (6 preview cards REAL, không placeholder)
  6. Preview + per-resource approve (logo/colors/banner/hero — KHÔNG all-or-nothing)
- Quality gate /100 widget visible trên step 6 (5 checks WCAG/vars/404/regression/logo-place)
- Regenerate counter tier-aware (FREE 3 / BASIC 10 / PREMIUM 30 / ENTERPRISE unlimited)
- Lifecycle progress UI (G9): NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING → FAILED
- KHÔNG free-form prompt (BANNED per ai-branding-guidelines.md §2.1, except Enterprise opt-in)

DELIVERABLE 5 — Component spec set (8 components THIẾU)
- 1 HTML demo + 1 spec.md cho mỗi component:
  - G1 Bulk Import (kéo-thả xlsx + preview validation + commit progress)
  - G5 Payment Method Selector (VNPay/MoMo/ZaloPay/Bank/Cash + QR)
  - G6 Invoice Detail (VN format, line items, VAT, total)
  - G7 Parent Invite Card (sender + redemption flows)
  - G9 Instance Lifecycle Status (6 states + retry)
  - G10 Payment Status Timeline
  - G11 Theme Live Preview (color picker + preview iframe)
  - G12 Bulk Actions Bar (sticky on selection)
- Mỗi demo có 4 states: default / loading / empty/error / success

YÊU CẦU CHẤT LƯỢNG (xem 06-quality-bar.md + 10-acceptance-criteria.md):
- Mỗi screen WCAG AA contrast 4.5:1 — DOCUMENT measured ratios trong HTML comment
- Mobile 320 / Tablet 768 / Desktop 1440 cả 3 viewports
- Dark mode parity bắt buộc
- Loading / empty / error / success states mỗi screen
- Mock data VN: tên Việt, đ currency, dd/MM/yyyy, 0901... phone
- KHÔNG Lorem ipsum, KHÔNG John Doe, KHÔNG $99.00
- Score self-estimate /128 trong README cuối kit
- Quality gate self-report format: "Y/10 passed. Failed: [list]. Fix or escalate?"

KHÔNG làm Direction A storytelling Round 2 (sẽ làm sau, xem §6 prompt riêng)

Khi hoàn thành, output structured matrix:

| Kit | Screens | Avg /128 | Min /128 | States | Mock VN | Self-pass |
|-----|---------|----------|----------|--------|---------|-----------|
| kiteclass-pro-v2 | X | YYY | YYY | Y/N | Y/N | Y/N |
| ...

VÀ list những điều cần user clarify (nếu có).
```

---

## §3 — Direction B Deep-dive (paste if Round 2 deliverable 1 fails AC)

```
Direction B kiteclass-pro v2 chưa đạt AC. Đào sâu:

1. ⌘K command palette taxonomy:
   - Group 1: Search (find student/class/invoice/teacher) — 6 commands
   - Group 2: Action (mark all attendance / finalize grades / send invoice /
     export Excel / regenerate branding) — 8 commands
   - Group 3: Navigation (Trang chủ / Học sinh / Lớp / Khóa / Điểm danh /
     Báo cáo / Cài đặt) — 7 commands
   - Mỗi command: icon (lucide) + name + shortcut hint (e.g., "G then S" cho
     Go to Students)
   - Recent + Pinned sections trên đầu

2. Sparkline data shape spec (cho FE port sau):
   - DashboardController.java mock response (đọc trong dossier 09)
   - Format: { metric: "students", values: [120, 124, 128, 132, ...30 days],
     delta: +12, deltaPct: +10.5 }
   - Render với recharts <SparklineLine>

3. Drag-drop widget grid persistence:
   - State shape: user-dashboard-prefs API
   - Field: { layout: [{id, x, y, w, h}], hidden: [id, id, id] }
   - Default layout for P2 Center Owner persona
   - Reset to default button

4. Dark mode toggle animation:
   - Sun → moon morph (Linear-style)
   - Use Framer Motion (KH already has it)
   - 300ms ease-out
   - Persistent in localStorage via next-themes

5. Toast confetti success milestones:
   - "Đã chốt điểm 25 học sinh" → confetti
   - "Đã điểm danh 30/30" → confetti
   - "Tenant đã DEPLOYED" → confetti + "Tải logo xong" notification

Output: ui_kits/kiteclass-pro-v2/screens/{dashboard, command-palette,
widget-grid-edit, dark-mode-toggle, success-confetti}.html với 4 states mỗi cái.
```

---

## §4 — Direction C Deep-dive (paste if Round 2 deliverable 4 fails AC)

```
ai-branding-wizard-v2 chưa đạt. Đào sâu 6 steps + quality gate:

STEP 1 Welcome:
- Tenant name + slug input
- "Bạn sẽ tạo trang web cho [business name]" greeting
- "Mất khoảng 5 phút" expectation setter
- Estimated finish time: 5 min

STEP 2 Logo:
- Drag-drop upload (max 2MB, PNG/SVG/JPG)
- "Bạn có logo chưa?" Yes/No fork
- If No: AI auto-generate from business name (template-only mode)
- Skip option: "Bỏ qua, dùng template logo có sẵn"

STEP 3 Audience picker:
- 4 options as cards:
  - 🏫 Trường mầm non / mẫu giáo
  - 📚 Trường THCS / THPT
  - 🌐 Trung tâm tiếng Anh / ngoại ngữ
  - 🎓 Lớp luyện thi / gia sư
- Mỗi card có illustration phù hợp + 1-line description
- Selected = primary border + checkmark

STEP 4 Tone picker:
- 4 options as cards:
  - 💼 Chuyên nghiệp (corporate)
  - 😊 Thân thiện (warm)
  - ⚡ Năng động (energetic)
  - ✨ Sang trọng (luxury)
- Each card shows TINY preview với tone đó áp dụng (button + heading sample)

STEP 5 Template picker:
- 6 REAL preview cards (KHÔNG placeholder)
- Mỗi template = 1 visual language family
- Filter by audience+tone (ẩn template không match)
- Hover preview: phóng to 1.05× + shadow lift
- Click: full-screen preview với "Chọn template này" + "Quay lại"

STEP 6 Preview + per-resource approve:
- Live iframe preview (KC tenant view)
- Right panel: 4 toggle/approve switches:
  - ☐ Logo (preview circle)
  - ☐ Bảng màu (color swatches)
  - ☐ Banner trang chủ
  - ☐ Hero / homepage
- Quality gate /100 visible:
  - WCAG contrast: ✓ 4.7:1 (passing 4.5:1)
  - Theme vars applied: ✓ 24/24
  - No 404 assets: ✓
  - Visual regression: ✓ (12% diff vs baseline, <20%)
  - Logo placement: ✓
  - Total: 95/100 ✓ PASS
- Regenerate button (with counter): "Tạo lại (còn 2/3)"
- Approve button: "Đồng ý, triển khai" (CTA primary)
- Cancel: "Quay lại sửa"

Lifecycle progress strip dưới mỗi step:
NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED
   [done]      [done]         [active]      [pending]

Output: ui_kits/ai-branding-wizard-v2/screens/{step-1, step-2, step-3, step-4,
step-5, step-6, quality-gate-fail, regenerate-quota-empty, lifecycle-FAILED,
lifecycle-DEPLOYED}.html
```

---

## §5 — Direction D Deep-dive (paste if Round 2 deliverable 3 fails AC)

```
kiteclass-parent chưa đạt. Đào sâu:

PERSONA REMINDER: Pa. Parent
- Mobile primary (~95% sessions)
- Low-medium tech literacy
- Evening/weekend usage (7pm-10pm after work)
- Comfortable: Zalo, banking apps, Facebook
- Wary: "yet another app"
- Top job: monitor child's attendance + grades + pay fees

NAVIGATION:
- Bottom tab bar (4 tabs max):
  - 🏠 Trang chủ — overview hôm nay
  - 📊 Học bạ — child's grades + attendance history
  - 💳 Học phí — invoices + pay
  - ⚙️ Cài đặt — Zalo OA toggle, language, account

SCREENS:
1. /parent (home)
   - Greeting: "Chào buổi tối, anh/chị [Parent Name]"
   - Hero metric: child's name + class + attendance % this week
   - Latest grade card (most recent assignment)
   - Today's class card (if any)
   - Fee status pill (Đã đóng / Còn nợ X.XXXđ)
   - Recent activity timeline (3-5 items, "Con đã đến lớp 14:00")

2. /parent/grades (học bạ con)
   - Tabs: Học kỳ I / Học kỳ II / Cả năm
   - Subject list with grade pills (Toán 8.5, Văn 7.5, ...)
   - Average + honor classification (Khá / Giỏi / Xuất sắc)
   - Tap subject → grade detail (assignment list + scores)
   - Download official report card PDF (Thông tư 22 format)

3. /parent/attendance (điểm danh)
   - Calendar month view (color-coded: green=present, red=absent, amber=late)
   - Day tap → session detail
   - Stats: 92% present / 5% late / 3% absent / 0% excused
   - Note section: GVCN comments

4. /parent/billing (học phí)
   - Outstanding invoice card (urgent if overdue)
   - Pay button → payment method selector (G5)
   - Invoice history list

5. /parent/settings
   - Zalo OA notification toggle (default ON)
   - Web Push toggle (fallback)
   - Email digest frequency (daily/weekly/never)
   - Logout

PWA SPEC:
- manifest.json: name "KiteClass Phụ huynh", short_name "KC PH",
  icons 192/512, theme_color from tenant
- Service Worker: cache static assets, offline message
- Add-to-home-screen prompt after 3 visits
- Push notification format: Zalo OA card style fallback to native

TOUCH UX:
- All buttons ≥44×44
- Pull-to-refresh on list screens
- Swipe-to-archive on notifications
- Floating action button (FAB) on home: "Liên hệ giáo viên" (link to Zalo)

PUSH NOTIFICATION CARD DESIGN:
- 320×100 card
- Tenant logo + initial of teacher (avatar 40×40)
- Headline: "Con An có điểm Toán mới"
- Body: "Con vừa được 8.5 điểm bài kiểm tra giữa kỳ. Xem chi tiết →"
- CTA button: "Xem"
- Timestamp: "5 phút trước"

Output: ui_kits/kiteclass-parent/screens/{home, grades-overview, grades-subject-detail,
attendance-calendar, attendance-day, billing-list, billing-pay, settings,
push-notification-card, pwa-install-prompt, dark-mode-home}.html
```

---

## §6 — Direction A Marketing (paste WHEN ready for Round 2 finale)

```
Direction A kitehub-story polish — marketing landing storytelling.

STATUS: Round 1 đã ship 546 LOC JSX. Polish thêm để đạt "wow" cho investor pitch
hoặc khách hàng mới đánh giá.

ADD:
1. Scroll-driven storytelling sections:
   - Hero: kite character bay lên, kéo theo các metric cards (parallax)
   - Section 2: "Một ngày của chủ trung tâm" — sticky scroll, mỗi step phóng
     to 1 hành động (mở dashboard sáng → thêm học sinh → điểm danh →
     gửi học phí → xem báo cáo cuối ngày)
   - Section 3: "Trước KiteClass vs Sau KiteClass" before/after slider
     - Trước: messy spreadsheet, sticky notes, calculator
     - Sau: clean dashboard, sparkline, single-tap actions
   - Section 4: Customer testimonial cards với VN classroom photography
     (real names: Cô Hương — Trung tâm Anh ngữ Việt Anh, Q.3 TP.HCM)
   - Section 5: Pricing với "Phổ biến nhất" ribbon (đã có Round 1 — chỉ polish)
   - Section 6: FAQ accordion với câu hỏi empathetic (đã có)
   - Section 7: CTA cuối với 3 trust markers + countdown timer (urgency)

2. Animations:
   - Hero kite SVG: bay nhẹ y: [0, -8, 0] 4s loop
   - Metric cards: stagger fadeInUp 0.1s on scroll-in
   - Charts: animate height 0% → final % với 50ms stagger per bar
   - Floating badges trên hero: y: [0, -8, 0] / [0, 8, 0] 3-4s loop
   - Easing: ease-out only, KHÔNG linear

3. Dashboard mock animation:
   - Stat cards count up từ 0 → final value
   - Chart fill in left-to-right
   - Notification toast pop in mid-scroll: "🎉 Trung tâm Eduplus đã thêm 50
     học sinh mới!"

4. Vietnamese copy polish (per 02-vietnamese-ux-musts.md):
   - Headline: "Giúp bạn bay cao cùng học viên"
   - Subhead: "KiteClass — phần mềm quản lý trung tâm giáo dục đơn giản, hiệu quả"
   - 3 trust markers: "✓ Không cần thẻ tín dụng  ✓ Hủy bất kỳ lúc nào  ✓ Hỗ trợ tiếng Việt"
   - CTA: "Dùng thử miễn phí 14 ngày"
   - Empathetic FAQ ("Tôi không rành công nghệ lắm, có dùng được không?")

5. Investor-pitch variant (bonus, optional):
   - Add data section: "1.2K trung tâm tin dùng — giảm 60% thời gian admin"
   - Logo wall (5-10 customer logos placeholder)
   - "As featured in" media mentions placeholder

Output: ui_kits/kitehub-story-v2/{README.md, styles.css, index.html, screens/{
hero, section-day-of-owner, before-after-slider, testimonials, pricing,
faq, cta-final, dashboard-mock, dark-mode}.html, app.jsx}

QUALITY: avg ≥110/128, polished animations, real VN photography placeholders.
```

---

## §7 — Acceptance check prompt (paste AFTER each deliverable)

```
Self-review deliverable [tên kit] vs 10-acceptance-criteria.md.

Output đúng format này:

| Section | Score /10 | Failed items |
|---------|:---------:|--------------|
| 1 Visual fidelity | X/10 | [list nếu có] |
| 2 VN UX | X/10 | [list] |
| 3 Accessibility | X/10 | [list] |
| 4 States | X/10 | [list] |
| 5 Persona | X/10 | [list] |
| 6 Data realism | X/10 | [list] |
| 7 Component reuse | X/10 | [list] |
| 8 Performance | X/10 | [list] |
| 9 i18n | X/10 | [list] |
| 10 Documentation | X/10 | [list] |
| **TOTAL** | **XX/100** | |

Per-screen score /128:
[list từng screen với score]

Self-verdict: SHIP / FIX-BEFORE-SHIP / ESCALATE-TO-USER

Nếu < 80/100 hoặc có screen <95/128 → đề xuất 3 fix hành động cụ thể.
Nếu ≥ 80/100 và mọi screen ≥95/128 → SHIP.
```

---

## §8 — Mobile vs Web Decision Matrix prompt (DEFERRED — không paste Round 2)

```
[GIỮ LẠI — không paste Round 2]

Project chọn web responsive + PWA-grade per 08-direction-decisions.md Decision 2.
Native app deferred until post-PMF. KHÔNG cần Claude Design làm decision matrix
PWA vs RN vs Flutter — đã quyết.

Nếu sau này (Round 4+) cân nhắc lại, paste prompt:
"Output mobile-tech-decision.md so sánh PWA / React Native / Flutter trên 8 trục..."
```

---

## §9 — Closing prompt (paste WHEN tất cả deliverables done)

```
Tất cả Round 2 deliverables đã ship.

Final summary cho user:

1. Deliverable matrix (xem §7 format) — toàn bộ 5 kits
2. Coverage:
   - Personas served: X/6 (target: 5/6 — Solo Teacher có thể defer Round 3)
   - Component gaps closed: X/12
   - Business flows covered: X/10
   - Pain-point screens fixed: X/14
3. Open questions chờ user clarify
4. Round 3 candidate scope:
   - Solo Teacher kit (P1)
   - Internal admin kit (KH /admin/*)
   - Marketing pitch deck variant
   - Mobile native re-evaluation if PMF achieved

Đồng thời output 1 file `documents/02-architecture/design-system/ROUND-2-RECEIPT.md`
ghi: ngày ship, deliverable list, score table, open questions — để user
import vào project repo.
```
