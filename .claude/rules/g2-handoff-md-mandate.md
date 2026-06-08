---
paths:
  - documents/03-planning/waves/wave-*-flow-*.md
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/05-guides/operations/*-g2-recipe-*.md
---

# G2 Handoff MD Mandate — stepped recipe file khi G1 PASS

**Priority:** 🟠 MANDATORY — flow verification campaign G2 handoff discipline
**Version:** 1.0.1
**Created:** 2026-06-04
**Last-Reviewed:** 2026-06-08
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + worked self-test on Wave flow-kh1 G2 handoff 2026-06-04) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies G2 handoff format mandate previously implicit/missing; META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi G1 PASS flow transition `🔄 walk-pass-pending-human` trong `flow-verification-campaign.md` §4 → G1-passer (Claude) MUST create dedicated stepped MD recipe cho user G2 test cùng PR

---

## 1. The Rule

> **Khi flow campaign G1 ✅ PASS (flip campaign §4 row → `🔄 walk-pass-pending-human`), G1-passer PHẢI ship dedicated G2 handoff MD recipe file trong cùng PR với G1 PASS evidence.** File MUST cover §3 required sections, follow §4 filename convention + §5 language convention, give user clear stepped instructions để self-test trên local stack.

Inline G2 instructions trong chat hay buried trong wave plan section = friction high + risk skip G2 + lose stepped clarity. Dedicated MD file = single artifact user reference + execute → báo kết quả → Claude flip campaign 🔄 → ✅ THÔNG (sau G3).

Force-multiplier: 1 chuẩn G2 MD format → mọi flow subsequent (22 flows × ~3 G2 handoff per loop) auto-comply → eliminate user push-back round-trip class "Hướng dẫn G2 luôn, bằng file md các bước rõ ràng".

---

## 2. Trigger pattern — khi nào rule fires

Rule fires khi:

| Pattern | Ví dụ |
|---|---|
| Wave plan PR flips campaign §4 row status → `🔄 walk-pass-pending-human` | Wave flow-kh1 PR #2147 flips KH-1 + KH-2c rows |
| Wave plan §5 Verification Gates table flips Gate `G1 ⬜ → ✅ PASS` | Wave flow-kh1 §5 |
| Closure PR commit mentions "G1 ✅" / "G1 PASS" cho flow scope | feat(wave-flow-X): G1 PASS commit |

Rule **KHÔNG** fires khi:
- G1 ⚠️ PARTIAL (chưa flip pending-human, vẫn iterate fix loop) — KH-2 wave flow-kh2 ban đầu PARTIAL stage
- Wave non-flow scope (vd Wave 14 anomaly fix, không campaign loop)
- Hotfix PR không touch campaign §4 rows
- KH-4 already-verified flow (campaign §4 status đã ✅ THÔNG, no new G2 needed)

---

## 3. Required content — 7 sections mandatory

G2 MD file MUST include 7 sections (per Wave flow-kh1 G2 recipe shipped 2026-06-04 self-test):

### 3.1 Frontmatter
```yaml
---
title: G2 Human Test Recipe — <flow-id> <scope>
audience: dev
created: YYYY-MM-DD
scope: Flow Verification Campaign G2 handoff for <flow> chain
references:
  - documents/03-planning/roadmap/flow-verification-campaign.md
  - documents/03-planning/waves/wave-YYYY-MM-DD-flow-<id>.md
---
```

### 3.2 Goal + prereq + duration estimate
- Mục tiêu rõ ràng (user test gì, kết quả mong đợi)
- Prereq state (stack UP, test users, GAP fixes shipped)
- Thời lượng ước tính (10-15 phút typical)

### 3.3 Setup section
- Browser + DevTools (Network tab filter)
- Auxiliary tools (MailHog UI, terminal DB query)
- Initial state verify command

### 3.4 Stepped instructions
Mỗi step PHẢI có 4 sub-sections:

1. **Hành động** — UI clicks / form input cụ thể, KHÔNG vague
2. **✅ Kỳ vọng (PASS)** — HTTP code expected + FE behavior expected + UX render expected
3. **⚠️ Sad path** — common error scenarios + quick fix
4. **🔍 Verify** (optional) — DB query / log check để confirm side effects

Steps sequenced theo user journey (anonymous → register → login → action → state).

### 3.5 Sad path quick checks (separate section)
Tổng hợp common edge cases not covered in main steps:
- Wrong credentials
- Re-use single-use resources (invite tokens)
- Phase-gated routes (vd `/register` 307 redirect)

### 3.6 Báo kết quả section (4-outcome matrix)
```markdown
**Khi G2 xong, báo lại 1 trong 4:**
- ✅ FULL PASS → Claude flip campaign rows → ✅ G1+G2 chờ G3
- ⚠️ MOSTLY PASS với cosmetic → catalog gap cho polish
- 🔴 BLOCKING ISSUE → catalog blocker + fix loop tiếp + re-walk
- ❓ UNCLEAR → ping với screenshot/error
```

### 3.7 Troubleshooting + G3 preview
- Troubleshooting quick table (symptom → quick fix)
- Production parity G3 preview (cho user awareness về Phase tiếp theo)

---

## 4. Filename convention

Per `docs-filename-prefix-convention.md` Tier 2 time-bound:

```
documents/05-guides/operations/YYYY-MM-DD-g2-recipe-<flow-id>[-<scope>].md
```

**Examples:**
- ✅ `documents/05-guides/operations/2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md` (Wave flow-kh1 + chain với KH-2c)
- ✅ `documents/05-guides/operations/2026-06-15-g2-recipe-kh3-subscription.md` (future KH-3 G2)
- ❌ `documents/05-guides/operations/g2-recipe-kh1.md` (missing date prefix)
- ❌ `documents/03-planning/waves/g2-kh1.md` (wrong folder — recipe ops content, not planning)

---

## 5. Language convention

Per `dev-readable-doc-language.md` §2:
- **Vietnamese narrative** cho instructions, expectations, troubleshooting prose
- **English technical identifiers** giữ nguyên: HTTP codes, endpoint paths (`POST /api/v1/...`), CLI commands (`docker exec`, `curl`), DB queries, JSON keys, field names

Example acceptable mix:
- ✅ "Click CTA 'Dùng thử miễn phí 14 ngày' → redirect tới `/request-beta-access`"
- ✅ "HTTP 201 cho `POST /api/v1/auth/request-beta-access` + FE render success screen"
- ❌ "Tag PASS verification" (English narrative thuần khi không cần)
- ❌ "Click nút Đăng ký miễn phí" (force translate technical UI label khi label production tiếng Anh)

---

## 6. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Inline G2 instructions trong chat khi flip G1 PASS | Ship dedicated MD file cùng PR |
| Bury G2 recipe trong wave plan §7 closure section | Tách MD file riêng — user reference dễ |
| Skip Verify DB queries "vì user tin Claude" | Empirical verify commands hỗ trợ user catch silent fail |
| Skip Sad path "vì happy path đủ" | Sad path catch real-world FE behavior + spec drift (vd GAP-917 sad path 400 vs 401) |
| Skip Báo kết quả section | 4-outcome matrix giúp user know exactly what feedback Claude needs |
| Multiple G2 recipes cho cùng 1 wave G1 PASS | 1 wave G1 PASS = 1 dedicated MD; chain với downstream flow (vd KH-2c chain KH-1) folded vào cùng MD |
| Filename collision với hyperlink-conflict format | Per `docs-filename-prefix-convention` Tier 2 — `YYYY-MM-DD-g2-recipe-<id>.md` |

---

## 7. Worked self-test — Wave flow-kh1 2026-06-04 (this rule's originating incident)

**Scenario:** Wave flow-kh1 G1 PASS shipped PR #2147 closure 2026-06-04. Initial handoff = inline chat message với G2 stepped recipe. User push back: "Hướng dẫn G2 luôn, bằng file md các bước rõ ràng => có meta chưa".

**Apply rule retroactively at G1 PASS flip moment:**

1. §2 Trigger fires: Wave PR flips campaign §4 row → 🔄 walk-pass-pending-human ✓
2. §3 Required content mandate: 7 sections shipped trong MD ✓
3. §4 Filename: `documents/05-guides/operations/2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md` ✓
4. §5 Language: Vietnamese narrative + English HTTP/CLI/DB identifiers ✓

→ **Rule fires correctly trên originating incident.** Counterfactual với rule active from start: G2 MD shipped same PR as G1 PASS → 0 user push-back round-trip. Save 1 round-trip × N flows over 22-flow campaign = 22 user interventions eliminated.

**Verdict:** Self-test PASS ✅. File `2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md` đã ship same PR → demonstrates pattern + future flows auto-comply.

---

## 8. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 8.1 Reviewer-checklist (active now)

Pre-merge review cho wave plan closure PR flipping campaign §4 row → `🔄 walk-pass-pending-human`:

- [ ] PR diff include G2 MD file `documents/05-guides/operations/YYYY-MM-DD-g2-recipe-<flow>.md`?
- [ ] File contain 7 sections per §3?
- [ ] Filename match Tier 2 time-bound convention §4?
- [ ] Language Vietnamese narrative + English identifiers per §5?
- [ ] Báo kết quả 4-outcome matrix present §3.6?
- [ ] FE port khớp flow per `kitehub-kiteclass-boundary.md` §2 (KH-* = `:3001` kitehub-frontend, KC-* = `:3000` kiteclass-frontend; platform-side KC exception §4.1 state-check)?

### 8.2 Cross-link wave plan §7 Closure Protocol

Wave plan §7 Closure Protocol có thêm sub-section trỏ tới G2 MD:

```markdown
### 7.4 G2 handoff recipe (per `g2-handoff-md-mandate.md`)

G2 stepped recipe shipped: [`documents/05-guides/operations/YYYY-MM-DD-g2-recipe-<flow>.md`](...)
```

### 8.3 Override mechanism

Genuine exception (trivial scope, no UI interaction, vd internal cron G1 verification):

```
git commit -m "...
G2_HANDOFF_MD_OVERRIDE: <flow-id> — <reason e.g. 'internal cron flow, no UI G2 applicable'>"
```

Trailer logged. Pattern frequency >10%/quarter → meta-review.

### 8.4 Detector (DEFER per `incident-to-rule-pipeline.md` §3.1)

- **Detector complexity:** moderate — detect campaign §4 row status change + verify matching G2 MD file in same PR
- **Recurrence count:** 1 today (this incident)
- **FP risk:** moderate — G1 PARTIAL vs PASS state machine + chain flows folded cùng MD
- **Decision:** Reviewer-checklist §8.1 + worked self-test §7 sufficient cho v1.0.0; revisit detector when recurrence-count ≥2 OR proven campaign-state parser available

---

## 9. Relationship to other rules

- **`feature-ship-runtime-walk-mandate.md`** v1.1.0 §3.4 — covers G1 agent walk catalog-then-batch + walk evidence. This rule extends to G2 handoff format mandate
- **`pre-handoff-self-test-completeness.md`** v1.2.0 — covers agent pre-handoff self-test completeness. This rule covers AFTER G1 PASS — handoff content format
- **`flow-verification-campaign.md`** §2 step 7 — "Hand cho human (G2)" mandate this rule fills format requirement
- **`docs-filename-prefix-convention.md`** Tier 2 time-bound — `YYYY-MM-DD-g2-recipe-<flow>.md` follow Tier 2 convention
- **`dev-readable-doc-language.md`** §2 — Vietnamese narrative + English identifiers; this rule mandates same convention
- **`output-review-mandate.md`** §3 — paired same-PR matrix row "G2 handoff recipe MD"
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + worked self-test + matrix row + rules-index.csv row paired same PR
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn → mọi G1 PASS subsequent auto-comply prospectively)
- **`incident-to-rule-pipeline.md`** — rule này direct output 2026-06-04 user-flagged "có meta chưa"
- **GAP-921** filed cùng PR documenting incident → rule pipeline trace

---

## 10. Log

- **2026-06-09 (v1.0.1):** PATCH — added §8.1 reviewer-checklist row "FE port khớp flow per `kitehub-kiteclass-boundary.md` §2 (KH-* `:3001`, KC-* `:3000`)". Cross-ref paired same-PR với new rule `kitehub-kiteclass-boundary.md` v1.0.0 (per its §6.2 Enforcement Parity) sau 2026-06-09 KH-3/KC-8 recipe FE port drift incident. Additive checklist row, no constraint loosening. Reviewer: @nguyenvankiet (solo-dev PATCH self-approve per `rule-change-process.md` §5).
- **2026-06-04 (v1.0.0):** Rule created in response to user-flagged 2026-06-04 Wave flow-kh1 G2 handoff: "Hướng dẫn G2 luôn, bằng file md các bước rõ ràng => có meta chưa" → "fix luôn" (after state-check confirmed coverage gap). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged) → Classify ✓ (no existing rule mandates G2 handoff MD format — `feature-ship-runtime-walk-mandate` covers G1 only; `pre-handoff-self-test-completeness` covers agent self-test before handoff; `flow-verification-campaign` §2 step 7 mentions handoff but no format spec) → Rule+Enforce ✓ (this file + reviewer-checklist §8.1 + worked self-test §7 + paired same-PR `documents/05-guides/operations/2026-06-04-g2-recipe-kh1-kh2c-beta-funnel.md` + GAP-921 update DONE + output-review-mandate.md §3 row + rules-index.csv row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example — rule fires correctly on Wave flow-kh1 G1 PASS handoff) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn → mọi G1 PASS flow subsequent (KH-3, KC-1..12, KH-5..10) auto-comply prospectively, eliminate user push-back round-trip class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered G2 handoff format; no constraint loosening; existing waves (KH-4 ✅ pre-rule) grandfathered; rule applies prospectively từ KH-3 wave forward). Atomic-unique-bar §5.1 check: ✅ atomic (single concept: G2 handoff MD recipe) + ✅ unique (sister rules cover G1 / pre-handoff, not post-G1 handoff format) + ✅ widely applicable (every G1 PASS flow × 22 campaign flows) + ✅ body discipline §1 ≤2 "and" conjunctions. Detector (§8.4) deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions (recurrence 1, reviewer-checklist + worked self-test sufficient cho v1.0.0); memory auto-load + cross-link extensions (`feature-ship-runtime-walk-mandate` §3.5 + `flow-verification-campaign` §2 step 7) deferred ≥7 days per premature-rule guard.
