# UI Review — Full Audit /128 (post wave-p0-closeout-1)

**Audit ID:** AUDIT-2026-06-14-ui-review-full
**Ngày:** 2026-06-14
**Phương pháp:** per-screen `/128` rubric per `.claude/skills/quality/ui-review/SKILL.md` §4 (Technical /20 · Design Heuristics /40 · Visual Aesthetics /28 · User Friendliness /20 · WCAG AA /20)
**Phạm vi:** representative 7-screen sample (KH 4 + KC 3) — KHÔNG enumerate toàn bộ 49 screens (out of scope per Wave 98 precedent)
**Base SHA:** `a219c7454` (worktree `chore/audit-ui-2026-06-14`)
**Baseline:** Wave 98 5-screen sample = **110.6/128 A** (`ui/2026-05-19-wave-98-cluster-b-sample.md`)

---

## ⚠️ Methodology caveat — Visual axis UNCHECKED

Worktree này KHÔNG có headless browser (Playwright chưa cài; `scripts/capture-ui-all.sh` không chạy được trong env này). Cả 2 frontend ĐANG chạy local (KH `http://localhost:3001` + KC `http://localhost:3000`) nên audit dùng **hybrid**:

- ✅ **CHECKED qua rendered HTML (curl) + đọc component source**: cấu trúc semantic (heading hierarchy, landmark), label/input association, ARIA, `lang` attr, state handling (loading/error/empty), responsive Tailwind classes, dark-mode token usage, i18n.
- ❓ **UNCHECKED (visual-only, cần screenshot)**: rendered color/contrast ratio thực tế, spacing/polish, typography rendering, hover/focus visual, animation. **Visual Aesthetics /28 + contrast sub-check (WCAG) được chấm THẬN TRỌNG, KHÔNG assume PASS** — đánh dấu ❓ trong bảng.

**Hệ quả:** điểm Visual /28 chấm conservative (~75% = ~21/28) dựa trên consistency của design-token system (shadcn/Tailwind), KHÔNG phải rendered polish. Một in-browser audit có thể +3-5/screen ở trục Visual nếu polish được confirm. Tooling gap tham chiếu GAP-405 (visual-regression baseline) + GAP-537c-followup (live screenshot capture) — KHÔNG file gap mới (tránh duplicate).

---

## 1. Per-screen /128

| # | Screen | App | Source | Tech /20 | Design /40 | Visual /28 ❓ | UX /20 | WCAG /20 | **Total /128** |
|---|--------|-----|--------|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | Landing | KH | `(public)/page.tsx` → `LandingShell` | 15 | 31 | 21 | 16 | 13 | **96** |
| 2 | Login | KH | `(auth)/login/page.tsx` | 16 | 33 | 21 | 16 | 11 | **97** |
| 3 | Admin dashboard | KH | `(admin)/admin/page.tsx` | 16 | 32 | 22 | 15 | 13 | **98** |
| 4 | Request-beta-access | KH | `(auth)/request-beta-access/page.tsx` | 16 | 33 | 22 | 17 | 15 | **103** |
| 5 | Login | KC | `(auth)/login/page.tsx` + `login-form.tsx` | 15 | 32 | 21 | 16 | 14 | **98** |
| 6 | Overview (dashboard home) | KC | `(dashboard)/overview/page.tsx` | 16 | 34 | 22 | 15 | 15 | **102** |
| 7 | Reports | KC | `(dashboard)/reports/page.tsx` | 17 | 35 | 22 | 17 | 14 | **105** |

**Aggregate (mean):** 699 / 7 = **99.9 / 128 (B · ~78%)**
**Lowest screen (quality bar thực sự per SKILL.md):** KH Login **97/128** — bị kéo bởi WCAG **11/20** (label không associated — xem GAP-1374).
**Highest:** KC Reports **105/128** (full state-handling + role-guard + VN format).

---

## 2. Delta vs baseline

| | Wave 98 (2026-05-19) | This audit (2026-06-14) | Delta |
|---|---|---|---|
| Sample | 5 small components (BetaDisclaimerBanner, beta-status, SupportMenu, FeedbackForm, legal/terms) | 7 full screens (landing, 2× login, beta-form, admin dashboard, overview, reports) | — |
| Aggregate | 110.6/128 A | 99.9/128 B | **-10.7** |
| Visual axis | code-level (AWS suspended) | UNCHECKED-conservative | — |

**KHÔNG phải regression.** Delta -10.7 do 3 nguyên nhân, không phải UI xuống cấp:
1. **Sample khó hơn** — full screens (landing + dashboards) gánh nhiều nghĩa vụ a11y/layout hơn 5 small components của Wave 98 (banner/menu/legal page polish cao tự nhiên).
2. **Visual /28 UNCHECKED-conservative** — chấm ~21-22/28 thay vì verify rendered excellence (env không có browser capture).
3. **Findings systemic mới surface** — skip-link thiếu toàn KH + KH login label-assoc (Wave 98 sample không chạm 2 surface này).

Hai sample không trực tiếp comparable; band tổng thể vẫn **A-/B+ về chất lượng cấu trúc**, tụt xuống **B** vì trục Visual chấm thận trọng + a11y findings.

---

## 3. Bug list (primacy — trước per-screen scores per SKILL.md §4)

Audit-level verdict: **PARTIAL PASS** — 0 P0 sub-check FAIL trên sampled screens (không auto-FAIL); 1 P1 + 3 P2 + 3 P3. PASS Phase 1 BETA UI bar.

| Gap | P | Screen(s) | Dimension | Finding |
|---|---|---|---|---|
| GAP-1374 | P1 | KH login | WCAG 1.3.1/3.3.2/1.3.5/4.1.2 | `<label>` raw không `htmlFor`/`id`; input không `id`; 0 `autoComplete`; error `<p>` không link `aria-describedby` + input không `aria-invalid`. KH login là outlier — `BetaRequestForm`/`FeedbackForm`/`BetaSignupForm` của KH ĐÃ dùng `htmlFor`. Sweep: register + 2fa pages. |
| GAP-1373 | P2 | KH (toàn bộ) + KC dashboard/auth/teacher | WCAG 2.4.1 Bypass Blocks | Skip-to-content link CHỈ tồn tại ở `kiteclass-frontend/(public)/layout.tsx`. KH 0 skip-link; KC dashboard/auth/teacher 0 skip-link. Keyboard user phải tab qua toàn nav mỗi page. |
| GAP-1375 | P2 | KH admin dashboard | UX (empty state) | `if (!stats) return null` → màn hình trắng khi không có data (không phải loading, không phải error). Thiếu empty-state UI. |
| GAP-1378 | P2 | KC reports | WCAG 1.1.1 non-text content | `MonthlyBarChart` có `role="img"` + generic `aria-label="Biểu đồ cột theo tháng"` nhưng per-bar `<title>` bị mask bởi role=img → giá trị data KHÔNG đọc được bằng screen reader; thiếu sr-only data-table fallback. |
| GAP-1376 | P3 | Cả 2 app | Technical (dark mode) | `darkMode:['class']` + `dark:` variants dày đặc nhưng 0 user-facing toggle (`setTheme('dark')`/`toggleTheme` = 0 call-site, trừ test). Dark mode chỉ reachable qua OS-preference (nếu provider defaultTheme=system) hoặc tenant-inject (KC) → dead/unverified styling. |
| GAP-1377 | P3 | KC login | WCAG 4.1.3 + no-JS | `next/dynamic({ ssr:false })` skeleton không có `aria-busy`/`aria-live` → screen reader không announce loading; no-JS user chỉ thấy skeleton. |
| GAP-1379 | P3 | KC overview | UX (first impression) | Dashboard home: 4/6 KPI là placeholder `—` + section "Hoạt động gần đây" chỉ là text giải thích → cảm giác trống/chưa-hoàn-thiện cho tenant mới. Honest (anti-fake-data, chờ `/dashboard/stats` endpoint) nhưng first-impression yếu. |

### Điểm tốt (positives — không phải finding)
- `lang="vi"` trên cả 5 SSR screens kiểm tra → i18n base đúng.
- KC login/reports/overview: full state-handling (loading skeleton + error + empty hint) + role-guard friendly message ("Không có quyền truy cập").
- KC overview: honest placeholder `—` thay vì fake numbers (GAP-805 fix giữ vững); `aria-label` trên `<section>`.
- VND/percent format VN chuẩn (`Intl.NumberFormat('vi-VN')` + decimal comma `92,5%`) ở admin dashboard + reports.
- KH login: rate-limit UX cao cấp (parse `Retry-After`, countdown 423/429, `role="alert"`, `aria-hidden` icons).
- Responsive grid (`sm/md/lg/xl`) + dark-token (`bg-card`, `text-muted-foreground`, `dark:bg-green-950/30`) nhất quán qua design system.
- KC FormInput dùng `useId`/`htmlFor` (label association đúng) — chuẩn nên propagate sang KH login.

### Ghi chú nhỏ (fold vào report, KHÔNG file gap — tránh gold-plating)
- KH admin dashboard h1 = "Dashboard" (chưa dịch "Bảng điều khiển") — i18n inconsistency nhỏ giữa biển Việt còn lại.
- `MonthlyBarChart` dùng `preserveAspectRatio="none"` → bars + `rx="0.5"` corner méo khi resize (visual nit).
- KH login thiếu show/hide password toggle (UX nice-to-have).

---

## 4. Per-dimension notes (sampled)

- **Technical /20** — responsive + dark-token CHECKED via classes (PASS). Console-error/anti-pattern ❓ UNCHECKED (cần browser devtools). KH landing + KC login `ssr:false` → initial-paint partial (-).
- **Design Heuristics /40** — Nielsen heuristics chấm qua structure + state handling. Visibility-of-status (loading/error) mạnh ở KC; error-prevention/recovery mạnh ở KH login (lockout). Empty-state yếu ở KH admin (GAP-1375).
- **Visual Aesthetics /28** — ❓ **UNCHECKED rendered**; chấm conservative qua design-token consistency. KHÔNG assume PASS.
- **User Friendliness /20** — nav/CTA/first-impression CHECKED via source. KC overview first-impression yếu (GAP-1379).
- **WCAG AA /20** — heading/landmark/label/aria/lang CHECKED via HTML+source. Contrast ratio + touch-target size ❓ UNCHECKED (cần rendered measurement). Skip-link (GAP-1373) + KH login label (GAP-1374) + chart (GAP-1378) là sub-check FAIL.

---

## 5. Findings → gaps filed

7 gap mới, reserved block GAP-1373..1388 (disjoint per `multi-session-concurrency-coordination.md`):

| Gap | P | Domain | Title |
|---|---|---|---|
| GAP-1373 | P2 | Frontend | Skip-to-content link thiếu systemic (KH toàn bộ + KC dashboard/auth/teacher) — WCAG 2.4.1 |
| GAP-1374 | P1 | Frontend | KH login form label không associated + thiếu autocomplete + error aria — WCAG 1.3.1/3.3.2/1.3.5/4.1.2 |
| GAP-1375 | P2 | Frontend | KH admin dashboard null-render khi no-data → blank screen, thiếu empty state |
| GAP-1376 | P3 | Frontend | Không có user-facing light/dark theme toggle dù darkMode:['class'] + dark: variants |
| GAP-1377 | P3 | Frontend | KC login ssr:false skeleton thiếu aria-busy/aria-live + no-JS chỉ thấy skeleton |
| GAP-1378 | P2 | Frontend | KC reports MonthlyBarChart data-value không screen-reader-accessible — WCAG 1.1.1 |
| GAP-1379 | P3 | Frontend | KC overview dashboard sparse (4/6 KPI placeholder + recent-activity placeholder) — first impression |

Tham chiếu (KHÔNG duplicate): GAP-405 (visual-regression Playwright baseline P2) + GAP-537c-followup (live screenshot capture P1) cho Visual-axis tooling gap.

---

## 6. Path-to-A

- Đóng GAP-1374 (KH login label-assoc) → KH login WCAG 11→15, screen 97→101.
- Đóng GAP-1373 (skip-link propagate từ KC public pattern → KH + KC dashboard) → +2 WCAG mỗi screen → aggregate +~2.
- Chạy in-browser audit (resolve tooling GAP-405/537c) → confirm Visual /28 → có thể +3-5/screen → aggregate về A- band (~108-112).

**Verdict:** **99.9/128 B (PARTIAL PASS)** — PASS Phase 1 BETA UI bar; below Wave 98 A baseline do methodology (Visual UNCHECKED) + sample khó hơn + 1 P1 a11y finding. 0 P0.
