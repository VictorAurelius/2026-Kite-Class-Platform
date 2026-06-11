# 08 — Direction Decisions

4 hard decisions made in session 2026-04-29 — non-negotiable for Round 2. Claude Design must respect these.

**Use this when:** scoping Round 2 work. If Claude Design proposes something that contradicts a decision, push back — don't silently expand scope.

---

## Decision 1 — Direction B (kiteclass-pro) is the priority

**Decision:** Direction B (Linear/Vercel/Stripe-vibe owner dashboard) is **highest priority** for Round 2 product UI work.

**Rationale:**
- P2 Center Owner is the **paying customer persona** — improving their daily experience compounds retention.
- KC `/dashboard` (363 LOC, score 84) is mid-tier; can lift to >110 with command palette + sparklines + skeleton + dark-mode polish.
- Bundle Round 1 already shipped `kiteclass-pro` skeleton — extending it is cheaper than starting fresh.

**Round 2 scope for B:**
- ⌘K command palette with 20+ commands, grouped (Search / Action / Navigation)
- Sparkline mini-charts in stat cards (recharts already in KH; reuse)
- Skeleton loaders for every data zone
- Dark mode polished (sun→moon morph animation)
- Drag-drop widget grid with persistence (state shape: `user-dashboard-prefs` API)
- Toast confetti on success milestones (mark all attendance, finalize grades)

**Out of scope for B:**
- Backend changes (drag-drop persistence backend can land later)
- Analytics integration (no Mixpanel/Amplitude yet)

---

## Decision 2 — Direction D = web responsive + PWA-grade (NOT native app)

**Decision:** Direction D pivots from "mobile native app" to **"web responsive + PWA-grade enhancements"** for parent + student screens.

**Rationale:**

| Tiêu chí | Native app | Web responsive + PWA |
|----------|-----------|----------------------|
| Dev cost (solo dev) | 2–3× (multi-stack: RN/Flutter) | 1× (Next.js đã có) |
| Time-to-store | 2–4 tuần (Apple/Google review) | 0 (deploy là dùng) |
| Code sharing với web | low–medium (RN good, Flutter no) | 100% (same codebase) |
| Push notification VN | FCM/APNs native | **Zalo OA primary** (~95% reach VN parents) + Web Push fallback (Safari iOS 16.4+, Chrome OK) |
| Payment integration | Native MoMo/VNPay SDK | Web flow (đủ tốt VN — gateway redirect works) |
| QR điểm danh (camera) | Native | `getUserMedia` web (đủ với Chrome/Safari mobile) |
| Offline support | Native cache | Service Worker + IndexedDB |
| Install presence | App Store trust | "Add to home screen" — yếu hơn nhưng có |
| Maintenance | 2 codebases | 1 codebase |
| **Verdict cho dự án solo dev** | Defer post-PMF | ✅ **Pick** |

**Round 2 scope for D (renamed `kiteclass-parent` + `kiteclass-student`):**
- Mobile-first screens (320–414px primary, 768px+ secondary)
- Touch-friendly tap targets ≥ 44×44
- PWA manifest + Service Worker spec
- Web Push notification design (with Zalo OA card as primary fallback)
- Bottom tab navigation (3-4 tabs max for parent / student)
- Pull-to-refresh on data screens (lists, dashboards)

**Out of scope for D:**
- React Native / Flutter — defer until subscriber count > X (TBD post-PMF)
- Native iOS/Android shells (no separate codebases)
- App Store / Play Store presence (defer)

**Decision artifact:** ADR-024 will be filed in Track 2 documenting this pick (when first parent kit ports to production).

---

## Decision 3 — Direction A (marketing storytelling) stays included

**Decision:** Direction A `kitehub-story` (marketing landing with kite character + scroll storytelling + before/after slider) is **kept in Round 2** scope.

**Rationale (from user 2026-04-29):** "rất đẹp mà nhỉ" — visual quality is high, would polish KH marketing.

**Round 2 scope for A:**
- Refine existing `kitehub-story` kit — current state is `546 LOC JSX` from Round 1
- Add scroll-driven storytelling sections (parallax, sticky headers, before/after slider)
- Add "một ngày của chủ trung tâm" section (Sao Demo storytelling)
- Mock dashboard animation (chart tăng dần, notification pop-in)
- Marketing-only — does NOT touch product UI (`/dashboard`, `/billing`, etc.)

**Out of scope for A:**
- Product UI redesign (covered by Direction B/D)
- A/B test infrastructure (separate concern)
- Investor pitch deck (different document type, not web design)

**Note:** Direction A is **lower priority** than B/D. Schedule it last in Round 2 batch.

---

## Decision 4 — Direction C integrates into existing wizard (NOT separate playground)

**Decision:** Direction C AI Branding playground (Round 1 standalone) **refactors into the existing 6-step provisioning wizard** — does NOT stay as separate playground.

**Rationale:**
- `documents/02-architecture/ai-branding-v2-redesign.md` defines lifecycle: NOT_STARTED → INITIALIZING → GENERATING → DEPLOYED → REGENERATING.
- `.claude/rules/ai-branding-guidelines.md` §4.1 mandates wizard pattern — playground UX violates this rule (free-form prompt would be P§2.1 violation).
- Round 1 playground was prototype — never meant for production.

**Round 2 scope for C (renamed `ai-branding-wizard-v2`):**
- 6-step wizard refactored:
  1. Welcome + tenant info
  2. Logo upload (optional)
  3. Audience picker (Trường mầm non / THCS / Trung tâm tiếng Anh / Lớp luyện thi)
  4. Tone picker (Chuyên nghiệp / Thân thiện / Năng động / Sang trọng)
  5. Template picker (6 preview cards)
  6. Preview + per-resource approve (logo/colors/banner/hero — NOT all-or-nothing)
- Quality gate /100 widget visible on step 6 (5 checks: WCAG / vars / 404 / regression / logo placement)
- Regenerate counter visible per tier:
  - FREE: 3/session
  - BASIC: 10/session
  - PREMIUM: 30/session
  - ENTERPRISE: unlimited
- Lifecycle progress visualization (use Component G9 from `04-component-gaps.md`)

**Out of scope for C:**
- Free-form prompt entry (BANNED per `ai-branding-guidelines.md` §2.1, except Enterprise opt-in)
- Direct AI API calls in UI (must go through `AnalyzerService → PlannerService → PlanExecutor`)
- Side-by-side comparison playground (cute but redundant with per-resource approve)

---

## Decision summary table

| # | Direction | Priority | Status | Key constraint |
|:-:|-----------|:--------:|--------|----------------|
| B | kiteclass-pro (owner dashboard) | **HIGHEST** | Extend Round 1 skeleton | ⌘K + sparkline + drag-drop + dark mode polish |
| D | kiteclass-parent + kiteclass-student | HIGH | Pivot to web responsive (NOT native) | Mobile-first 320-414px, PWA-grade, Zalo OA primary |
| C | ai-branding-wizard-v2 | MEDIUM | Refactor playground → 6-step wizard | Per-resource approve, quality gate /100, no free-form prompt |
| A | kitehub-story (marketing) | LOWER | Polish Round 1 kit | Marketing only, no product UI changes |

---

## Things explicitly NOT decided (escalate to user)

| Open question | When it matters |
|---------------|----------------|
| Whether Direction B "drag-drop widgets" persists per-user or per-tenant | Round 2 spec for state API |
| Whether Direction D student kit ships in Round 2 or Round 3 | Scope budget — parent is priority over student |
| Whether Direction C wizard supports ENTERPRISE Advanced Mode (free-form prompt) toggle | Compliance with `ai-branding-guidelines.md` §2.4 |
| Whether Direction A includes investor pitch deck variant | Separate doc type — likely no for Round 2 |
| Mobile native app re-evaluation trigger (X subscribers? Y revenue? Z parent NPS?) | ADR-024 acceptance criteria |

When Claude Design hits one of these, it should ASK rather than assume.
