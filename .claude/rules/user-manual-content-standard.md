---
paths:
  - "documents/05-guides/user-manual/**"
  - "kitehub/kitehub-frontend/src/app/help/**"
---

# User Manual Content Standard — 15-item checklist + persona discoverability matrix

**Priority:** 🟠 MANDATORY — tenant-facing doc content governance
**Version:** 1.0.1
**Created:** 2026-05-14
**Last-Reviewed:** 2026-05-31
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (15-item checklist + persona discoverability matrix + reviewer-checklist + worked self-test on Wave 79 Bucket F1 anonymous-prospect 5-page prototype) per §6.5 Enforcement Parity Mandate; no constraint loosening — closes coverage gap surfaced by Wave 79 Bucket F1 outside-in audit `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-bucket-f1-user-manual-outside-in.md`; META P0 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Every user manual page under `documents/05-guides/user-manual/**` AND the matching Next.js MDX route `kitehub/kitehub-frontend/src/app/help/**`. Scope = tenant-facing help content (P1 Solo Teacher / P2 Center Owner / P3 Center Manager / Anonymous Prospect personas). Internal runbooks at `documents/05-guides/operations/**` + `documents/05-guides/deploy/**` follow separate `docs-folder-structure.md` rule và NOT covered here.

---

## 1. The Rule

> **Mọi trang user manual (web hoặc PDF source) PHẢI đáp ứng đủ 15-item checklist §2 trước khi merge.** Reviewer enforces per-page tại pre-merge; CI grep detector deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày.

User manual = tenant-facing help content user (chị Hằng / anh Tâm / em Vy) đọc khi cần hỗ trợ. Nếu manual ship vague "5-10 screenshots per persona" mà không có format/discoverability/cognitive-load discipline → tenant stuck → support burden tăng → bounce. Rule này codify outside-in audit findings thành mandatory checklist + persona discoverability matrix.

Force-multiplier: 1 chuẩn chung cho mọi page → mọi page subsequent (Wave 80+ P2/P3/Admin) auto-comply → eliminate retroactive rework cost.

---

## 2. The 15-item checklist (mandatory per page)

Mỗi `.md` page dưới `documents/05-guides/user-manual/**` PHẢI satisfy:

### Foundation (5 items)

1. **Frontmatter mandatory fields:**
   ```yaml
   ---
   persona: anonymous | p1-solo-teacher | p2-center-owner | p3-center-manager | platform-admin
   topic: <short-slug>
   last-updated: YYYY-MM-DD
   version: <app-version-or-doc-version> (e.g., 1.0 hoặc v0.9.0-beta)
   effort_minutes: <estimated read time in minutes>
   ---
   ```
   `last-updated` MUST equal session date per `session-currentdate-check.md` §1 (cannot forward-date).

2. **TL;DR box** đầu page (within first 200 words):
   - 1 câu summary "Trang này giúp bạn ..."
   - 3-5 bullet step ngắn (mỗi bullet ≤ 15 từ)
   - Total TL;DR ≤ 80 từ — đọc trong 30 giây
   - Pattern: Intercom Articles / Stripe Docs convention

3. **Persona-specific landing page** (`/help/anonymous`, `/help/p2-owner`, etc.) — KHÔNG merge mọi persona vào 1 trang. URL pattern `/help/{persona-slug}/{topic-slug}`.

4. **Vietnamese narrative** per `dev-readable-doc-language.md` §2 (end-user docs scope). Technical token (HTTP, URL, JWT, KiteHub brand name) giữ English natural. Code-switching trong câu acceptable.

5. **Support footer** mỗi page (cuối content, before related-links):
   ```markdown
   ---
   ## 🆘 Cần hỗ trợ?

   - 📧 Email: [support@kitehub.me](mailto:support@kitehub.me)
   - 💬 Zalo OA: [zalo.me/kitehub](https://zalo.me/kitehub) (defer Phase 1.5+ if Zalo OA chưa active)
   - 🐛 Báo lỗi trang này: [mailto:support@kitehub.me?subject=Lỗi tại {page-url}](mailto:support@kitehub.me)
   - 📊 Trạng thái beta: [/beta-status](/beta-status)
   ```

### Visual + Annotated Media (3 items)

6. **Annotated screenshots** (NOT raw screenshots). Mỗi screenshot có:
   - Mũi tên đỏ (color `#dc2626` recommended) chỉ vào element user click/nhập
   - Viền vàng (color `#facc15`) khoanh vùng cần chú ý
   - Số bước (1, 2, 3...) đặt trên screenshot tương ứng numbered list trong narrative
   - Resolution: 1440×900 desktop OR 375×812 mobile
   - Browser locale vi-VN → UI text Vietnamese
   - File path: `screenshots/{persona}/{topic}-{step-num}.png`
   - Tooling: GIMP / Figma export PNG + post-process; OR Playwright + Sharp programmatic capture
   - **Wave 79 Bucket F1 allowance:** placeholder markup comment (`<!-- Screenshot placeholder: capture {topic}-step-1.png — 1440×900 vi-VN — show {element} -->`) acceptable when UI under active polish. Actual capture deferred to follow-up task tracked GAP-537.

7. **Sample data VN-friendly**:
   - Tên trung tâm: "Trung tâm Anh ngữ Sky Education", "Trung tâm Toán Quang Minh"
   - Tên người: "Trần Thị Hồng", "Nguyễn Văn An", "Phạm Thị Mai"
   - Tên lớp: "Lớp Anh ngữ 5A1", "Lớp Toán 9B"
   - Địa chỉ: "123 Lê Lợi, Q.1, TP.HCM", "45 Hai Bà Trưng, Hà Nội"
   - ❌ BANNED: Lorem Ipsum / "John Doe" / "Class A1" / "Example Center"

8. **Currency VND + date format VN**:
   - Currency: `1.500.000đ` hoặc `1.500.000 ₫` (NOT `$60.00`, `60 USD`)
   - Date long: `Thứ Hai, 14/05/2026` (NOT `Mon May 14, 2026`)
   - Date short: `14/05/2026` (NOT `2026-05-14` in narrative; ISO format OK trong frontmatter + code)
   - Time: `09:30` hoặc `14:00` (24h preferred), `9 giờ 30 sáng` natural narrative acceptable

### Trust + Discoverability (4 items)

9. **Last-updated badge** visible top of page (after frontmatter, before TL;DR):
   ```markdown
   > 📅 Cập nhật lần cuối: **{YYYY-MM-DD}** · Phiên bản KiteHub: **{app-version}** · Đọc khoảng **{effort_minutes} phút**
   ```
   Linear Method convention — show fresh + version sync + read time upfront.

10. **Discoverability ≥3 entry points** (PER PERSONA, see §3 matrix):
    - User stuck → 90% bottleneck là "không tìm được manual" not "manual sai"
    - Manual chỉ valuable khi tìm được trong < 30 giây từ moment stuck
    - 3 entry points required: 1 trong header/nav + 1 trong footer + 1 trong onboarding/CTA flow

11. **WCAG AA accessibility**:
    - Heading hierarchy đúng: `# H1 (page title)` → `## H2 (section)` → `### H3 (subsection)`; KHÔNG skip levels
    - Alt text mọi screenshot (mô tả tiếng Việt nội dung screenshot)
    - Contrast ratio ≥ 4.5:1 cho text bình thường, ≥ 3:1 cho text lớn ≥ 18pt
    - Keyboard-navigable (Next.js MDX default ok; verify TOC sidebar có focus indicator)
    - Skip-to-content link cho screen reader

12. **Search functional**:
    - Phase 1 (Wave 79): Fuse.js client-side, index page title + headings + first 200 chars body
    - Phase 1.5+ (Wave 80+): Algolia DocSearch upgrade
    - Search box visible header sidebar
    - Min 2 ký tự trigger search; debounce 300ms

### Format Discipline (3 items)

13. **Print-friendly CSS** (`@media print`):
    - A4 portrait orientation
    - Font: Times New Roman 11-12pt body, sans-serif heading OK
    - Hide nav/sidebar/footer in print
    - Page breaks before each `## H2` section
    - URL hyperlinks expanded inline (`<a>...</a>` → "text (full URL)" trong print)
    - Test: browser Ctrl+P → PDF preview clean

14. **Mobile responsive ≥360px viewport**:
    - TOC sidebar collapse → hamburger menu trên mobile
    - Screenshot scale-to-fit width, KHÔNG horizontal scroll
    - Font size ≥ 14px body trên mobile (avoid pinch-zoom)
    - Touch targets ≥ 44×44px (buttons, links)
    - Test browser DevTools responsive mode 360×640 + 375×812

15. **PDF auto-generation từ MDX source**:
    - Script `scripts/render-user-manual-pdf.sh <persona-slug>` convert `documents/05-guides/user-manual/{persona}/*.md` → `<persona>-manual.pdf`
    - PDF gitignored (regen on-demand per `test-artifact-format-standard.md` §4.2 pattern)
    - Engine: Puppeteer headless OR wkhtmltopdf fallback
    - Output: A4 portrait, includes header (logo + persona name) + footer (page N/M + URL)
    - Used cho: tenant share Zalo cho thư ký, offline read trên xe, in giấy backup

---

## 3. Discoverability matrix per persona

Mỗi persona PHẢI có ≥3 entry points cụ thể:

| Persona | Entry point 1 (in-app) | Entry point 2 (in-app) | Entry point 3 (external) |
|---|---|---|---|
| **Anonymous Prospect (Vy)** | Landing top nav "Hướng dẫn" link | Footer "Tài liệu" link | Google indexable (`/help/anonymous/*` public) |
| **P2 Center Owner (Hằng)** | Header `?` button (top right) → dropdown menu | Onboarding step CTA "Xem hướng dẫn cho chủ trung tâm" | Footer "Hướng dẫn sử dụng" link |
| **P3 Center Manager (Tâm)** | First-login overlay tour CTA "Mở hướng dẫn Manager" | Header `?` button → "Tôi là Manager" link | Persona landing URL `/help/p3-manager` trong invite email |
| **P1 Solo Teacher** | Header `?` button → "Tôi là giáo viên" link | Sidebar footer "Hướng dẫn nhanh" | Footer global |
| **Platform Admin (Mai)** | N/A (internal scope; uses repo `documents/05-guides/operations/`) | GitHub navigation | N/A |

Bucket F1 sample (Wave 79) scope: **Anonymous persona only** — minimum 2 in-app entry points (top nav + footer) + Google indexable. P2/P3 personas defer Wave 80+ Bucket F2 sau F1 dev review.

---

## 4. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| English narrative "View pricing page" trong content | "Xem trang bảng giá" Vietnamese narrative |
| Raw screenshot không annotation | Mũi tên đỏ + viền vàng + số bước per §2 row 6 |
| Lorem Ipsum / "John Doe" / "Class A1" sample data | "Trần Thị Hồng" / "Trung tâm Sky Education" / "Lớp 5A1" |
| Currency `$60.00` hoặc `60 USD` | `1.500.000đ` VND format |
| Date `Mon May 14, 2026` | `Thứ Hai, 14/05/2026` |
| Wall-of-text 3 paragraph intro "What is KiteHub" | TL;DR box top + 5-bullet steps |
| 1 manual cho mọi persona | Persona-specific landing `/help/{persona-slug}/*` |
| Manual không có entry point từ dashboard | ≥3 entry points per §3 matrix |
| Manual không có support footer | Standard footer §2 row 5 mỗi page |
| Forward-date `last-updated` | Match session date per `session-currentdate-check.md` |
| Ship 5-10 screenshots không có 15-item checklist verify | Verify per-page §2 checklist trước merge |
| Skip print CSS "vì user đọc web" | Misa benchmark: VN edu user share PDF qua Zalo thường xuyên — print PDF mandatory |
| Trust user "sẽ Google ra manual" | Public surface + ≥3 entry points + Google indexable cho anonymous persona |

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active now)

Pre-merge review cho PR touching `documents/05-guides/user-manual/**` hoặc `kitehub-frontend/src/app/help/**`:

- [ ] §2 Foundation (5 items): frontmatter + TL;DR + persona landing + Vietnamese narrative + support footer
- [ ] §2 Visual (3 items): annotated screenshots (or placeholder comment) + VN sample data + VND currency + date VN
- [ ] §2 Trust (4 items): last-updated badge + ≥3 discoverability entry points + WCAG AA + Fuse.js search
- [ ] §2 Format (3 items): print CSS + mobile responsive + PDF render script
- [ ] §3 persona matrix: entry points wired per persona scope
- [ ] Vietnamese narrative per `dev-readable-doc-language.md`
- [ ] `last-updated` ≤ session date per `session-currentdate-check.md`

### 5.2 Cross-reference `output-review-mandate.md` §3

This rule paired same-PR with new matrix row "User manual pages" — review standard tracking.

### 5.3 Memory auto-load (optional, deferred)

Memory entry `feedback_user_manual_content_standard.md` could remind at session start before user manual editing. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test sufficient cho v1.0.0.

### 5.4 CI grep detector (deferred)

Future enhancement — heuristic regex tìm common anti-patterns trong `documents/05-guides/user-manual/**`:

```bash
# Detect Lorem Ipsum / John Doe / English placeholder
grep -rnE "Lorem [Ii]psum|John Doe|Jane Doe|example\.com|Class [0-9A-Z]+|Example Center" \
  documents/05-guides/user-manual/ 2>/dev/null \
  && { echo "WARN: English/placeholder sample data — use VN-friendly per user-manual-content-standard.md §2"; exit 0; }

# Detect USD currency
grep -rnE "\\\$[0-9]+|\\b[0-9]+ USD\\b" documents/05-guides/user-manual/ 2>/dev/null \
  && { echo "WARN: USD currency — use VND per user-manual-content-standard.md §2"; exit 0; }
```

WARN-only (false positives expected — some `$` legitimate in code blocks). Track follow-up gap when stabilize.

### 5.5 Override mechanism

Genuine exception (e.g., page targeting English-speaking expat tenant cohort, regulator template English-only):

```
git commit -m "...
USER_MANUAL_STANDARD_OVERRIDE: <page-path> — <reason — e.g., expat-targeted i18n English variant per /help/en/p2-owner/*>"
```

Trailer logged. Pattern frequency >5%/quarter triggers meta-review.

---

## 6. Self-test (worked example — Wave 79 Bucket F1 anonymous-prospect 5-page prototype)

Apply 15-item checklist to 5 sample pages shipped same PR (`documents/05-guides/user-manual/anonymous/{index,pricing,beta-access,terms,faq}.md`):

| # | Checklist item | index | pricing | beta-access | terms | faq |
|---|---|:---:|:---:|:---:|:---:|:---:|
| 1 | Frontmatter (persona + topic + last-updated + version + effort_minutes) | ✅ | ✅ | ✅ | ✅ | ✅ |
| 2 | TL;DR box ≤80 từ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 3 | Persona-specific landing (`/help/anonymous/{slug}`) | ✅ | ✅ | ✅ | ✅ | ✅ |
| 4 | Vietnamese narrative | ✅ | ✅ | ✅ | ✅ | ✅ |
| 5 | Support footer mandatory section | ✅ | ✅ | ✅ | ✅ | ✅ |
| 6 | Annotated screenshots (placeholder comment OK Wave 79) | 🟡 placeholder | 🟡 placeholder | 🟡 placeholder | N/A (text-only) | N/A (text-only) |
| 7 | VN sample data ("Sky Education", "Trần Thị Hồng") | ✅ | ✅ | ✅ | N/A | ✅ |
| 8 | Currency VND + date VN | ✅ | ✅ | ✅ | ✅ | ✅ |
| 9 | Last-updated badge | ✅ | ✅ | ✅ | ✅ | ✅ |
| 10 | ≥3 discoverability entry points | ✅ (top nav + footer + Google) | ✅ | ✅ | ✅ | ✅ |
| 11 | WCAG AA (heading hierarchy + alt text) | ✅ | ✅ | ✅ | ✅ | ✅ |
| 12 | Search functional (Fuse.js wired in route) | ✅ (route-level) | ✅ | ✅ | ✅ | ✅ |
| 13 | Print CSS (`@media print` in route layout) | ✅ (route-level) | ✅ | ✅ | ✅ | ✅ |
| 14 | Mobile responsive ≥360px (Next.js route) | ✅ (route-level) | ✅ | ✅ | ✅ | ✅ |
| 15 | PDF auto-generation script ready | ✅ (`scripts/render-user-manual-pdf.sh`) | ✅ | ✅ | ✅ | ✅ |

**Verdict:** all 5 pages satisfy 13/15 items fully + 2 items partial (screenshots placeholder comment, acceptable Wave 79 Bucket F1 with §2 row 6 allowance — actual capture tracked GAP-537 follow-up). Self-test PASS ✅ — rule fires correctly + 5-page prototype demonstrably applies standard.

**Counterfactual without rule:** F1 sample ship vague "5-10 screenshots per persona" theo GAP-537 inside-out scope only → no Vietnamese narrative discipline, no entry-point matrix, no TL;DR pattern → Wave 80+ P2/P3/Admin pages rework retroactively when standard surfaces.

---

## 7. Relationship to other rules

- **`dev-readable-doc-language.md`** §2 row "End-user docs / customer-facing" — this rule extends Vietnamese narrative mandate cho user manual scope specifically
- **`output-review-mandate.md`** §3 — paired same-PR with new matrix row "User manual pages" tracking this rule's review standard
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (this rule precedes feature gap GAP-537)
- **`outside-in-coverage-trigger.md`** — this rule = direct output of Wave 79 Bucket F1 outside-in audit (4 personas × 5 questions) applied through `incident-to-rule-pipeline.md` 5-stage pipeline
- **`session-currentdate-check.md`** §4.2 — `last-updated` field MUST match session date, banned forward-date
- **`docs-folder-structure.md`** §3 — `documents/05-guides/user-manual/` follows folder README template
- **`test-artifact-format-standard.md`** §4.2 — PDF gitignored + regen-on-demand pattern reused here
- **`gap-done-discipline.md`** §3 — user manual page Wave 80+ rest defer = PARTIAL exit ramp (GAP-537 stays PARTIAL ~25% until 4 personas shipped)
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test all paired same PR
- **`incident-to-rule-pipeline.md`** — applied 5-stage; Detect ✓ (outside-in audit caught format/discoverability blind spot) → Classify ✓ (no existing rule mandates user manual content discipline) → Rule+Enforce ✓ (this file + matrix row + rules-index row + 5-page self-test) → Self-Test ✓ (§6 above) → Retro Log ✓ (§8 below)

---

## 8. Log

- **2026-05-31** (v1.0.1): PATCH — added `paths:` frontmatter per `context-budget-mandate.md` §3.2 (rule was always-load, violating §3.2 size-gate ≥1k tokens requires path-scope/justification/hook). Scope matches rule's own **Applies to** — no behavior change (rule still fires when relevant files touched); removes ~18k chars from base session context. Part of Wave meta context-budget rule-scoping batch 2026-05-31. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per §5 — path-scope correction, no constraint loosening).

- **2026-05-14 (v1.0.0):** Rule created in response to Wave 79 Bucket F1 outside-in audit (`documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-bucket-f1-user-manual-outside-in.md`) — 4 personas × 5 questions (Discovery / Format / Cognitive load / VN edu / Trust gates) surfaced format + media + discoverability blind spot mà GAP-537 inside-out scope chỉ liệt kê "5-10 screenshots per persona" thiếu enforcement. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (outside-in audit + user request meta standard) → Classify ✓ (no existing rule codifies user manual content discipline; closest = `dev-readable-doc-language.md` covers narrative-only; `output-review-mandate.md` §3 had no row for user manual pages) → Rule+Enforce ✓ (this file + 15-item checklist + persona discoverability matrix + paired same-PR: `output-review-mandate.md` §3 row "User manual pages" + `rules-index.csv` row + Bucket F1 anonymous-prospect 5-page prototype as worked self-test + GAP-537 scope refinement per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example on 5 sample pages — 13/15 PASS + 2 partial with §2 row 6 placeholder allowance) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix standard 1 lần → force-multiplier mọi page user manual subsequent (Wave 80+ P2/P3/Admin). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per §5 — new constraint codifying previously-uncovered class; no constraint loosening; existing user manual scope = empty folder (verify-at-spawn confirmed); rule applies prospectively from Wave 79 Bucket F1 forward). Detector wiring (§5.4 CI grep + §5.3 memory auto-load) deferred per `incident-to-rule-pipeline.md` premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test sufficient cho v1.0.0.
