---
audience: dev
created: 2026-06-09
---

# Session Handoff — 2026-06-09 — Tier-enforcement + AI branding template-first clarification

**Branch:** `feature/tier-ui-fix-g2-browser-2026-06-09` (3 commit, **CHƯA push**, working tree clean). KHÔNG ở `main`.

---

## 1. Đã làm xong (3 commit trên branch)

### `23199384` — tier-UI display + G2-browser meta-governance
- **GAP-1090 DONE** (Backend): `SubscriptionService.applyPendingUpgrade` thiếu `instance.setTier()` → 3 path fixed + `V68` backfill migration + SUB-21 invariant. Verified Playwright 7/7 + curl.
- **GAP-1091 DONE** (Frontend): branding hub `MOCK_QUOTA` hardcode + advanced wrong-instanceId → fixed; dashboard `buildHealthSnapshot` gate trial trên `isOnTrial`; `use-subscriptions`/`use-payments` invalidate `['instances']`.
- Meta: g2-handoff-md-mandate **v1.0.2** (browser-walk mandate FE-wired G2) + flow-verification-campaign §1 + output-review §3. KH-5/KH-6 recipe browser-ified + 22 recipe audited + 12 product headers.

### `75532497` — tier-enforcement wave (GAP-1020)
- **ADR-039** cross-service tier propagation (Option A JWT claim). subscription `AuthService` + `TokenService` embed `tier` claim từ `instances.tier`; gateway `JwtAuthenticationGatewayFilter` inject trusted `X-Subscription-Tier` (mirror X-User-Roles); `application.yml` strip client-sent (anti-spoof). Branding consume 0-code.
- **Verified live:** test-8 re-login → `/regenerate-quota` = **PREMIUM/30** (hết FREE/3) + anti-spoof (ENTERPRISE header strip→PREMIUM). Tests: gateway 37/0 + subscription 7/0.
- **SUB-22 entitlement matrix canonical** (rules.md) + tier-name drift sweep 2 file (`PRO→BASIC`).
- AC2 preview: Step6Preview render srcDoc (brand colours + org + logo) thay vì blank `src` — **NHƯNG generic, chưa reflect template** (xem §3).

### `c99f1dba` — follow-up gaps
GAP-1092 (G2 curl loophole, DONE) + GAP-1093 (renew no-FE) + GAP-1094 (docs 404) + GAP-1095 (trial-to-paid tier sync) + GAP-1096 (activateSubscription dead-code) + GAP-1097 (TwoFactor token tier/tenant parity) + GAP-1098 (tier-name drift sweep ~8 file).

---

## 2. AI branding — model làm rõ (QUAN TRỌNG, user đã confused)

**2 TRỤC độc lập (KHÔNG phải "3 cấp AI"):**
- **Generation MODE** (per-resource, `ai-branding-guidelines.md:34-36`): `STATIC` (upload/default, no compute) / `TEMPLATE` (SVG template + brand params, default ~80%) / `FULL_AI` (AI sinh, heavy/đắt, chỉ khi template không khớp HOẶC Enterprise custom).
- **Subscription TIER** (per tenant): FREE/BASIC/PREMIUM/ENTERPRISE → quota + model + có FULL_AI không.
- **Phase 1 = TEMPLATE-first by design** (ADR-037 + ADR-026 defer FULL_AI/Ollama). KHÔNG phải hạn chế — là chủ đích.

**Code thực tế HIỆN TẠI = MOCK/scaffold:** `AIBrandingProcessor` copy `logoUrl` + marketing copy hardcode English; chưa TEMPLATE-compose thật, chưa FULL_AI; `OPENAI_API_KEY=sk-mock`.

**User decision flow (2026-06-09):**
1. Ban đầu chọn "pull FULL_AI lên (OpenAI key)" → tôi spawn ADR-040 + branding real-gen.
2. Sau khi tôi clarify model → **user ĐẢO sang "TEMPLATE-real + preview-reflect-template trước (no key); FULL_AI defer ngoại lệ".**
3. → Tôi **discard detour FULL_AI** (ADR-040 reverted, ADR-026/037 deferral GIỮ NGUYÊN — đúng).

---

## 3. PICKUP — việc dở dang (resume next session)

### 3a. 🔄 TEMPLATE-real + preview-reflect-template (Q3 — đang làm dở, agent stopped khi investigate)
**Bug:** Step6Preview srcDoc generic (chỉ màu+tên+logo), template Step5 chỉ là **label** → preview KHÔNG phản ánh template đã chọn. User: "preview phải theo lựa chọn template trước đó".
**Fix (FE-only, no key, design-first):** `Step6Preview.buildPreviewHtml` render **layout/style của template ĐÃ CHỌN** (`selectedTemplateId`) + brand params → chọn template khác = preview khác.
- Đọc TRƯỚC: `ui_kits/ai-branding-wizard-v2/screens/**` (mỗi template trông sao) + `TemplateStep.tsx` template registry (id/name/layout/colors) + `Step6Preview.tsx` lấy selectedTemplate.
- Verify: rebuild frontend + headless drive wizard (drive script pattern đã dùng — login test-8 → Step1 name+slug(async available) → Step2 logo-skip → Step3-5 pick → Step6) → chọn template khác → preview khác.

### 3b. Còn nợ file (discovery-to-gap-inline-filing)
- **/admin/staff UX gap (P2 Frontend):** verdict = OWNER vào `/admin/staff` **đúng authz** (ADR-003 OWNER=TENANT_OWNER; guard `hasAdminLayoutAccess` cho OWNER chỉ `/admin/staff*`), KHÔNG leak. NHƯNG UX smell: `/admin/staff` ở route group `(admin)` → render `AdminLayout` chrome platform-admin (Sidebar Beta/Instances/Payments/Revenue + badge Admin + dead nav-links bounce). **Fix:** relocate `/admin/staff*` sang `(customer)` group. Low-sev. → **CHƯA file gap, file next session.**

### 3c. By-design / deferred (KHÔNG fix)
- **Q2 portrait upload:** wizard Step2 design chỉ logo (upload OR AI-tạo) — KHÔNG có portrait. Portrait giáo viên ở **landing banner** (GAP-810 DONE, surface khác). Muốn portrait trong wizard = cần **ADR + design mới** (Phase 1.5+).
- **FULL_AI (Q3 "AI thật"):** deferred ngoại lệ per ADR-026/037 (deferral intact sau khi discard ADR-040). Khi cần: OpenAI key vào `kitehub/.env` (gitignored) + AIBrandingProcessor mock→real + GAP-1078 tier→model routing (unblocked bởi gateway tier ADR-039) + GAP-003 image-gen + GAP-1021 persist.

---

## 4. Push/PR pending
~54 file, 3 commit, chưa push. Đề xuất tách: **PR code** (BE+FE+V68, chạy CI build/test) / **PR docs** (meta-governance + recipes + GAP-1092..1098, auto-merge). User chưa cho push.

## 5. Goal hook
Session goal "invest và fix đến khi ai branding hoàn thiện (lấy được tier, preview được)" còn ACTIVE. Literal ACs (tier ✅ + preview-renders ✅) đạt; preview-reflect-template (Q3 deeper) dở dang per §3a. User end session sớm → cần `/goal clear` để end nếu hook block.
