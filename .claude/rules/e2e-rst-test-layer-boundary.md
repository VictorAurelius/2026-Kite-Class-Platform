---
paths:
  - "kitehub/kitehub-frontend/e2e/**"
  - "kiteclass/kiteclass-frontend/e2e/**"
  - "kitehub/**/playwright.config.ts"
  - "kiteclass/**/playwright.config.ts"
  - "documents/05-guides/operations/acceptance-tests/**"
  - "documents/04-quality/audits/persona-review/**"
  - "documents/03-planning/waves/wave-*rst*.md"
---

# E2E vs RST Test Layer Boundary — automated regression vs manual exploratory acceptance

**Priority:** 🟠 MANDATORY — test layer scope governance
**Version:** 1.0.0
**Created:** 2026-05-25
**Last-Reviewed:** 2026-05-25
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (boundary matrix + RST→E2E spec promotion mandate + worked self-test trên Đợt 105 5-bug recurrence) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-emergent 2-layer pattern Đợt 105 surfaced; META P0 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn boundary → mọi test addition + mọi RST cycle subsequent auto-comply)
**Applies to:** Mọi test addition (Playwright spec dưới `**/e2e/**`, acceptance test CSV dưới `documents/05-guides/operations/acceptance-tests/**`) + mọi RST (Release Self-Test) cycle (wave plans `wave-*rst*.md`, persona-review audit reports). Out-of-scope: unit tests, integration tests trong `src/test/**` Java/TS (different layer concerns).

---

## 1. The Rule

> **E2E tests (Playwright automated) và RST (Release Self-Test manual walkthrough) là HAI lớp bù trừ — KHÔNG phải thay thế nhau, KHÔNG phải duplicate. Mỗi finding trong RST cycle PHẢI được chuyển thành E2E spec regression-guard trong cùng PR fix (RST→E2E promotion mandate).**

E2E catches **regression** (kịch bản đã spec bị vỡ). RST catches **discovery** (gap UX/flow/copy/locale mà spec chưa cover). Đợt 105 (2026-05-23) chứng minh: E2E PASS clean nhưng RST chuỗi đăng nhập KiteClass bắt 5 lỗi mà gap list không có → E2E mù cho class bugs đó.

Force-multiplier: mỗi RST finding → E2E spec mới → RST surface co lại theo thời gian → Phase 1.5 + Phase 2 RST cycle gọn hơn ~30% per audit cycle.

---

## 2. Boundary matrix — scope ownership rõ ràng

| Aspect | E2E tests (Playwright) | RST (manual walkthrough) |
|---|---|---|
| **Trigger** | Tự động mỗi PR/CI commit | Thủ công per-release (pre-Phase-gate) |
| **Cadence** | Mỗi merge | 1 cycle per major release (Phase 1 → 1.5 → 2 → 3) |
| **Coverage** | Spec-bound (defined `expect()` assertions) | Persona-driven full journey (4-6 vai trò × 20-25 luồng) |
| **Catches** | Regression — kịch bản đã spec vỡ | Discovery — gap chưa spec |
| **Authority** | Code-level invariant (functional) | Product-level acceptance (UX + cultural awareness) |
| **Cost** | ~ms-sec per spec | ~3-5h human per cycle |
| **VN cultural feedback** | Whatever specs codify (limited) | High — VND format / Vietnamese label / Zalo culture / persona expectation per `vn-localization-audit-checklist.md` |
| **Owns these bug classes** | Form validation, auth flow, CRUD, role-guard, API contract, regression invariant | UX feel, copy quality, cross-flow integration with real timing, persona discovery, locale awareness |

### 2.1 Decision tree cho test addition mới

```
1. Tính chất bug: functional regression OR discovery/UX?
   - Functional (form validation broke, API contract mismatch, auth redirect wrong) → E2E spec
   - Discovery (copy English-in-VN-context, persona expectation gap, cross-flow timing) → RST cycle finding
2. Reproducible deterministic (input X → output Y)?
   - YES → E2E spec (automate guard)
   - NO (subjective UX assessment) → RST persona-review audit
3. Cost-benefit: spec writing time vs catch rate?
   - High catch rate + reasonable spec time → E2E
   - Long-tail edge case rarely-hit + expensive spec → RST sampling acceptable
```

### 2.2 Owns table — bug class → test layer mapping

| Bug class | Owner | Rationale |
|---|---|---|
| Auth redirect wrong URL | E2E | Deterministic input→output; `expect(page).toHaveURL(...)` |
| Form validation message text | E2E | Deterministic; `expect(locator).toContainText(...)` |
| Cross-tenant data leak | E2E (Testcontainers IT preferred) | Security regression; deterministic |
| API response shape | E2E (contract test) | Schema validation deterministic |
| Role-guard 403 enforcement | E2E | Deterministic role × endpoint matrix |
| Copy says "Click here" instead of "Bấm vào đây" | RST (cultural feedback) | Subjective UX cultural assessment; persona-driven |
| Form layout breaks on 360px mobile | RST + visual regression | Pixel-blind E2E miss; human or Percy/Chromatic |
| Onboarding wizard feel awkward step 3 | RST (persona walkthrough) | Subjective UX feedback only humans assess |
| Cross-bucket flow gãy chỗ chưa ai assert (Đợt 105 5-bug class) | RST (discovery) | E2E specs scope-bound; RST catches unspec'd integration |
| Wizard skip button hidden on tablet | RST + visual regression | Layout discovery |
| VN cultural mismatch (calendar year vs niên khóa) | RST | Cultural awareness — humans see |

---

## 3. RST→E2E promotion mandate (the force-multiplier)

> **Mọi RST cycle finding (5 lỗi Đợt 105 / N lỗi Đợt 106+) PHẢI được chuyển thành E2E spec regression-guard TRONG CÙNG PR fix bug đó.**

Pattern bắt buộc:
1. RST cycle phát hiện bug X (vd "đăng nhập KiteClass redirect sai URL post-success")
2. Fix PR cho bug X PHẢI include:
   - Code fix (bug X resolved)
   - **NEW E2E spec** trong `kitehub-frontend/e2e/` hoặc `kiteclass-frontend/e2e/` assert đúng behavior X
   - PR body section `## RST→E2E promotion` ghi rõ: "RST Đợt N tìm bug → E2E spec `{file}.spec.ts` thêm để prevent recurrence"
3. Sau merge → RST cycle sau bug X không cần catch lại (E2E làm)

Counter-pattern (banned): ship bug fix only, không thêm spec → bug X có thể recur → RST tốn thêm 1 cycle để re-discover.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| RST cycle fix bug → ship code only, không thêm E2E spec | Pair fix với new E2E spec same PR per §3 promotion mandate |
| E2E spec assert subjective UX ("page should feel responsive") | E2E owns deterministic only; subjective UX = RST persona review |
| Skip RST "vì E2E coverage đủ rồi" | Đợt 105 chứng minh — E2E PASS không nghĩa RST không tìm bug; vẫn cần manual cycle pre-release |
| Skip E2E spec "vì RST sẽ catch" | RST đắt (~3-5h human); E2E rẻ (~ms-sec) — regression layer E2E owns |
| Treat RST + E2E là duplicate, cắt 1 lớp để tiết kiệm | Cả 2 cần thiết, khác bug class — Stripe/Linear/Atlassian cùng pattern |
| RST cycle ship findings không trigger E2E spec PRs | RST closure protocol PHẢI include E2E spec count added (vd "23 luồng walk → 15 findings → 12 new E2E specs + 3 cultural feedback only") |
| Visual regression (Percy/Chromatic) coi là E2E | Visual regression là bridge layer riêng — pixel-blind E2E vs full RST; defer Phase 1.5+ per `release-deploy-standard.md` §3.4 |

---

## 5. Enforcement (per `rule-change-process.md` §6.5)

### 5.1 Reviewer-checklist (active now — primary enforcement)

Pre-merge review cho PR fix bug từ RST cycle:
- [ ] PR title/body reference RST cycle (Đợt N walkthrough hoặc persona-review audit)?
- [ ] Code fix paired với new E2E spec trong `**/e2e/**` directory?
- [ ] E2E spec assert đúng behavior expected (locator + expect verified)?
- [ ] PR body có `## RST→E2E promotion` section?
- [ ] Acceptable exception (cultural feedback only — không có deterministic behavior to assert) → document inline

Pre-merge review cho RST cycle closure PR:
- [ ] Closure protocol report E2E spec count added per finding count?
- [ ] Findings non-deterministic (cultural/UX subjective) clearly marked → documented không cần E2E spec

### 5.2 Override mechanism

Genuine exception (RST finding không deterministic, không có E2E spec equivalent):

```
git commit -m "...
RST_E2E_PROMOTION_EXEMPT: <finding-id> — <reason — e.g., 'cultural feedback only: copy revision không có testable invariant'>"
```

Trailer logged. Pattern frequency >40%/RST-cycle triggers meta-review (likely RST scope leaking into pure-cultural-review territory, OR E2E spec authoring discipline cần upgrade).

### 5.3 CI grep detector (deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** PR body scan cho RST cycle reference + cross-check E2E spec count added — moderate scope
- **Recurrence count:** 0 post-merge (rule shipped 2026-05-25)
- **FP risk:** Moderate — some RST findings legitimately cultural-only (acceptable exempt)
- **Decision:** Reviewer-checklist §5.1 + worked self-test §6 sufficient cho v1.0.0; revisit detector khi recurrence-count ≥2 OR Đợt 106/107 generate large enough finding count to need automation

Future heuristic regex (when implemented, WARN-mode):

```bash
# Scan recent PR bodies for RST reference without E2E spec count
gh pr list --state merged --search "RST OR đợt OR walkthrough" --limit 20 \
  --json number,body,files \
  | jq -r '.[] | select(.body | test("RST|walkthrough|Đợt"; "i")) | select((.files | map(.path | test("e2e/.*\\.spec\\.")) | any) | not) | "WARN: PR #\(.number) references RST but no E2E spec added"'
```

WARN-only. Track follow-up gap khi rule stabilize.

### 5.4 Memory auto-load (optional, deferred)

Memory entry `feedback_e2e_rst_promotion.md` could remind tại session start trước RST cycle execution. Defer per premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test §6 đủ cho v1.0.0.

---

## 6. Worked self-test — Đợt 105 RST 5-bug recurrence (retroactive)

Đợt 105 RST (2026-05-23, persona-review audit `documents/04-quality/audits/persona-review/2026-05-23-wave-107-rst-a-b-onboard.md`) walk chuỗi đăng nhập KiteClass → bắt 5 bugs mà E2E suite (18 kiteclass-frontend specs) PASS clean không catch.

**Apply §3 RST→E2E promotion mandate retroactively:**

| Bug Đợt 105 | Bug class per §2.2 | E2E spec should have added | Status |
|---|---|---|---|
| 1 (KiteClass login redirect wrong) | Functional regression — E2E owns | `kiteclass-frontend/e2e/auth-login-redirect.spec.ts` assert post-success URL | ❌ Not added; bug fix Đợt 107 ship without spec |
| 2-5 (similar pattern) | Functional — E2E owns | 4 more specs | ❌ Not added |

**Counterfactual với rule applied at Đợt 105 closure:**
- 5 bugs fix → 5 new E2E specs added → next RST cycle (Đợt 106+) không cần re-walk chuỗi đăng nhập KiteClass (E2E catches recurrence)
- Đợt 106 (23 luồng × 4 vai trò ~3-5h) → finding budget free cho NEW discovery (cross-flow gãy mới surface), không lãng phí re-confirm Đợt 105 fixed
- Theo thời gian Phase 1.5 / Phase 2 RST cycle → ~30% surface shrink per cycle (mỗi cycle promote 10-15 specs)

→ Rule fires correctly on Đợt 105 originating recurrence. Self-test PASS ✅

**Cost-save projection:** ~30% RST cycle time savings post Đợt 106+107 (estimate based on E2E spec coverage growth). At ~3-5h per cycle × N cycles per quarter, ~1h/cycle savings + improved Phase-gate confidence.

---

## 7. Relationship to other rules

- **`testing-standards.md`** (skill, KHÔNG rule) — covers test types + framework conventions; rule này adds boundary discipline ON TOP (which test type owns which bug class)
- **`pre-handoff-self-test-completeness.md`** — §2.x flow class checklists (7 classes) cho per-flow verification at handoff time; rule này covers test layer scope (E2E vs RST), complementary scope
- **`postgres-specific-type-testcontainers.md`** v1.0.0 — Testcontainers mandate cho Postgres-specific entity binding; orthogonal — different test layer (Java IT vs Playwright E2E)
- **`vn-localization-audit-checklist.md`** v1.0.0 — cross-bucket VN-context checklist; RST cycle persona-review enforces this; E2E specs cố gắng codify (limited)
- **`outside-in-coverage-trigger.md`** v1.1.0 — outside-in audit trigger; RST cycle IS the outside-in user-walkthrough form
- **`gap-done-discipline.md`** §2 — DONE flip AC verified; rule này extends: AC verified through RIGHT test layer (E2E for functional / RST for UX)
- **`release-fix-retry-budget.md`** §3.5 — investigation phase mandate; applies trước khi propose test fix
- **`output-review-mandate.md`** §3 — paired same-PR row "E2E vs RST test layer" tracking review standard
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test §6 + rules-index.csv row all ship same PR
- **`incident-to-rule-pipeline.md`** — applied 5-stage: Detect ✓ (user 2026-05-25 "tại sao vẫn cần RST khi có E2E") → Classify ✓ (no existing rule formalizes boundary; Đợt 105 concrete recurrence) → Rule+Enforce ✓ (this file + paired same-PR) → Self-Test ✓ (§6 Đợt 105 retroactive) → Retro Log ✓ (§8)
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (fix boundary 1 lần → mọi test addition + RST cycle subsequent auto-comply prospectively)
- **`context-budget-mandate.md`** §3.2 — path-scoped frontmatter (e2e/test artifacts + RST wave plans + persona-review audits only) — không global auto-load, save context budget per `context-budget-mandate.md` §3.1

---

## 8. Log

- **2026-05-25 (v1.0.0):** Rule created in response to user-flagged 2026-05-25 question "tại sao vẫn cần RST khi đã có E2E, 2 cái là 1 hay khác nhau". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged + Đợt 105 recurrence concrete evidence — 5 bugs RST catch không có trong E2E coverage) → Classify ✓ (no existing rule formalizes E2E vs RST boundary; `testing-standards.md` skill covers test framework conventions không cover layer ownership; `pre-handoff-self-test-completeness.md` covers per-flow handoff verification different scope) → Rule+Enforce ✓ (this file + §2.2 owns table + §3 RST→E2E promotion mandate + reviewer-checklist §5.1 + worked self-test §6 trên Đợt 105 recurrence + rules-index.csv row + output-review-mandate §3 row paired same PR per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example trên Đợt 105 originating recurrence — rule fires correctly + counterfactual ~30% RST surface shrink per cycle projection) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — fix boundary 1 lần → mọi test addition + RST cycle subsequent auto-comply prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-emergent 2-layer pattern Đợt 105 surfaced; no constraint loosening; existing E2E specs + RST cycles grandfathered until next refresh; rule applies prospectively từ Đợt 106 forward). Atomic-unique-bar §5.1 check passed: ✅ atomic (boundary E2E vs RST + RST→E2E promotion) ✅ unique (no overlap với existing test rules) ✅ widely applicable (every test addition + RST cycle) ✅ body discipline §1 ≤2 "and" conjunctions. CI grep detector (§5.3) + memory auto-load (§5.4) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (recurrence count 0 + reviewer-checklist sufficient + revisit when Đợt 106/107 generate large enough finding count); enforcement = reviewer-checklist + worked self-test §6 đủ cho v1.0.0.
