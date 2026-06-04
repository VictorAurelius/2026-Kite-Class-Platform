---
paths:
  - "documents/03-planning/waves/**/*.md"
  - "documents/03-planning/roadmap/flow-verification-campaign.md"
  - "documents/05-guides/operations/*-g2-recipe-*.md"
  - ".claude/skills/quality/persona-based-business-review.md"
  - ".claude/skills/quality/simulation-gap-finder.md"
  - ".claude/skills/quality/wave-pack-planner/**"
---

# Pre-Walk Persona Simulation Mandate — surface failure modes BEFORE walk

**Priority:** 🟠 MANDATORY — flow-walk efficiency governance
**Version:** 1.0.0
**Created:** 2026-06-04
**Last-Reviewed:** 2026-06-04
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + memory auto-load + worked self-test on Wave flow-kh1 G2 walk session 2026-06-04 — 5/6 bugs would have surfaced in pre-walk simulation) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "spawn persona simulation BEFORE flow walk on local Docker stack")
**Applies to:** Mọi Wave / PR ship user-facing flow (signup, login, onboarding, payment, beta-invite, claim-code, password-reset, 2FA, invite-acceptance, tenant-switch, file-upload, subscription, refund, etc.) AND involves a user (OR coordinator) walking the flow end-to-end trên local Docker stack. Out-of-scope: pure refactor không user-facing surface, internal infra changes, docs-only PR (per `docs-only-pr-auto-merge.md` §2).

---

## 1. The Rule

> **Trước khi user / coordinator chạy manual walk end-to-end cho user-facing flow trên local Docker stack, PHẢI spawn 1 Opus background agent simulate walk qua `persona-based-business-review` Pre-Walk Mode + `simulation-gap-finder` failure-mode matrix + external benchmark / failure-mode reflection.** Agent return 5-10 likely failure modes (FE / BE / gateway / consumer / side-effect). Coordinator + user batch-fix high-confidence findings TRƯỚC walk → live walk catches only residual bugs.

Sister rule to `feature-ship-runtime-walk-mandate.md` v1.1.0 §3.4 (catalog-then-batch DURING walk). Rule này extends to **PRE-walk** — same evidence philosophy, different trigger moment.

Wave flow-kh1 G2 walk session 2026-06-04 surfaced 6 unexpected bugs trong ~3h. Per §5 worked self-test, 5 of 6 (GAP-924 silent 2FA verify UI / GAP-926 generic catch / GAP-927 rollback rotate without resend / GAP-928 false 503 / GAP-930 path mismatch) would have surfaced trong a 5-minute pre-walk persona simulation asking: "Tôi là invitee mới — gặp lỗi gì? gửi lại email được không? mạng chậm thì sao? subdomain trùng thì sao? token hết hạn thì sao?". GAP-925 BE String double-encode = "true gem" (BE-internal class, persona không simulate được) — residual class rule cho phép.

Force-multiplier: 1 chuẩn pre-walk simulation → mọi user-facing flow walk subsequent eliminate 80%+ surprise bugs at walk time → save ~1-2h round-trip per walk session.

---

## 2. Trigger pattern — flow classes mandating pre-walk simulation

Rule fires khi Wave / PR scope includes ANY of:

| Pattern | Example |
|---|---|
| **Signup / onboarding flow** | Beta signup, tenant provisioning, instance bootstrap, profile setup wizard |
| **Auth flow** | Login, OAuth, MFA/2FA, password reset, magic link, session refresh |
| **Invite / claim flow** | Beta invite acceptance, staff invitation, parent-child link, claim-code redemption |
| **Subscription / payment flow** | Pricing → checkout → webhook → invoice → renewal → refund |
| **Tenant-switch / multi-tenant flow** | Workspace picker, tenant claim, cross-tenant data isolation walk |
| **File-upload flow** | Avatar upload, document attach, CSV import, ZIP bundle |
| **Email-driven flow** | Token-link click → state transition (verify-email, accept-invite, reset-password) |
| **Async / background flow** | Long-running job, queue processing, retry / DLQ, scheduled task trigger |
| **Persona-attributed AC** | Gap AC mentions "Owner / Teacher / Parent / Student / Admin can do X" |
| **Multi-service workflow ≥3 services** | invite → email → token → accept → user-provision → login |

Rule **KHÔNG** fire khi:
- Pure refactor (no behavior change, no user surface)
- Internal infra / Helm / terraform / secrets rotation
- Docs-only PR per `docs-only-pr-auto-merge.md` §2
- Dev-tool / CI workflow changes
- Bug-fix re-walk (covered by `pre-handoff-self-test-completeness.md` §3 post-fix re-walk)
- Recent pre-walk simulation ≤30 ngày cho same flow scope (refresh không cần)

---

## 3. Required agent output

Pre-walk Opus agent (spawn per `agent-model-opus-default.md` v1.0.0 + `agent-background-spawn-default.md` v1.0.1) PHẢI return numbered list 5-10 specific failure modes. Mỗi failure mode có 3 fields:

| Field | Description |
|---|---|
| **(a) Where it fires** | FE component path / BE service path / gateway route / consumer queue / side-effect (email / Zalo / payment redirect) |
| **(b) Symptom user sees** | Browser behavior (silent fail, 503, redirect loop, blank page), email observed (or not), DB state |
| **(c) Recommended pre-walk check** | grep command / Read file:line / curl probe / DB query / log inspection — concrete + executable |

### 3.1 Format template (agent returns this)

```markdown
## Pre-walk persona simulation — <flow name>

**Persona walked:** <persona name, vd "Invitee mới (chưa có account)" hoặc "Owner đăng nhập lần đầu">

**Failure modes (5-10):**

1. **<1-line title>**
   - (a) Where: <FE file:line / BE endpoint / gateway route>
   - (b) Symptom: <what user sees / experiences>
   - (c) Pre-walk check: <grep / Read / curl / psql command>

2. **<1-line title>**
   - ...

...

## Recommended pre-walk batch fix

Sort by confidence × impact:
- HIGH confidence + HIGH impact: fix trước walk (items #1, #3, #5)
- MEDIUM: spot-check Read + grep trước walk (items #2, #4)
- LOW: defer to walk catch (items #6+)
```

### 3.2 Agent prompt skeleton

```
Spawn Opus background agent với prompt:

"Wave <X> sắp ship flow <Y> (vd Owner mời staff). User sẽ walk end-to-end local Docker
stack persona <Z>. Trước walk, simulate persona psychology:

- Tôi là <persona> — kỳ vọng gì khi click 'Mời nhân viên'?
- Tôi gặp lỗi gì? — subdomain trùng / email sai format / network slow / token hết hạn?
- Tôi retry như nào? — F5 refresh / click lại / mở tab mới?
- Tôi không thấy email — sao biết phải làm gì?

Per persona-based-business-review.md Pre-Walk Mode + simulation-gap-finder.md
failure-mode matrix + benchmark sister product (vd Notion invite, Linear invite).

Return 5-10 failure modes per §3 format. Cite concrete file:line / endpoint /
grep command cho mỗi pre-walk check."
```

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Skip pre-walk simulation "vì audit suite + tests pass" | Audit + tests cover code correctness, NOT persona psychology — pre-walk simulation fires anyway |
| Spawn pre-walk agent foreground (block coordinator ~5-10 min) | Background per `agent-background-spawn-default.md` v1.0.1 — coordinator prep walk runbook parallel |
| Use Sonnet for pre-walk agent | Opus 4.7 mandatory per `agent-model-opus-default.md` v1.0.0 — persona reasoning needs depth |
| Return 1-2 failure modes "vì flow simple" | Min 5 failure modes; sweep persona psychology (auth state / sad path / retry / network / locale / device) |
| Treat agent output as suggestion list (skip pre-walk fixes) | High-confidence findings PHẢI batch-fix TRƯỚC walk per §3.1 recommendation |
| Defer pre-walk "vì walk time gấp" | Pre-walk simulation = ~5-10 min agent + ~30 min batch fix; saves ~1-2h walk round-trips (net positive) |
| Spawn multiple sequential pre-walk agents (different perspectives) | Single Opus agent run 3 skills in parallel (`persona-based-business-review` + `simulation-gap-finder` + benchmark) trong 1 spawn |
| Skip pre-walk khi walk persona ≠ "real user" (vd Claude self-walk) | Self-walk still benefits — same psychology lens surfaces same bugs |
| Document pre-walk findings trong chat only | Save artifact `documents/04-quality/audits/persona-review/YYYY-MM-DD-pre-walk-<flow>.md` per `output-review-mandate.md` §3 |

---

## 5. Worked self-test — Wave flow-kh1 G2 walk session (2026-06-04, originating incident)

**Scenario:** Wave flow-kh1 G2 walk shipped 6 unexpected bugs trong ~3h walk session. Apply rule retroactively to PR creation moment (BEFORE walk).

### 5.1 What rule would have required

Pre-walk simulation agent spawn TRƯỚC user walk session. Persona = "Invitee click email link → tạo account → set password → login first time → join workspace". Agent reflects: "Tôi sẽ làm gì? Tôi expect gì? Gặp lỗi gì?"

### 5.2 Counterfactual — 6 bugs vs 5-10 expected failure modes

| Bug | Description | Pre-walk persona reasoning that WOULD HAVE surfaced it | Verdict |
|---|---|---|---|
| **GAP-924** | FE 2FA verify silent 401 — user gets no feedback | "Tôi gõ sai mã 2FA — UI báo gì? Tôi biết phải retry không?" → grep `2fa-verify` FE catch block | ✅ HIGH confidence surface |
| **GAP-925** | Subscription EmailEvent String double-encode | "Email body của tôi có ký tự lạ?" — needs BE log inspection, NOT user-observable | ❌ TRUE GEM (residual) |
| **GAP-926** | FE BetaSignupForm generic `catch (e)` swallows backend reason | "Subdomain trùng / email format sai — tôi thấy error gì?" → grep `BetaSignupForm` catch block | ✅ HIGH confidence surface |
| **GAP-927** | BE rollback rotates token without resend → user lockout | "Tôi click link xong rollback — link còn xài được không?" → trace token rotation flow | ✅ HIGH confidence surface |
| **GAP-928 P1+P2** | Gateway false 503 — shared breaker; carved write route | "Tôi retry submit — gateway throttle tôi với user khác?" → benchmark gateway breaker pattern + grep route config | ✅ HIGH confidence surface (P1 via psychology, P2 via benchmark) |
| **GAP-930** | admin-new-login-alert path mismatch | "Admin notif của tôi có arrive không khi user login lần đầu?" → grep `admin-new-login-alert` BE→FE route | ✅ HIGH confidence surface |

### 5.3 Verdict

5 of 6 (83%) bugs would have surfaced trong 5-10 min pre-walk persona simulation. Only GAP-925 (BE-internal String encoding) is residual class (not user-observable, persona psychology không reach it).

**Cost-save:**
- Without rule (actual): ~3h walk session + 6 bugs surface mid-walk + per-bug rebuild cycles + user round-trips
- With rule: ~5-10 min agent spawn + ~30-45 min batch-fix 5 surfaced bugs PRE-walk + ~1h walk catching ONLY residual GAP-925 = ~1.5h total
- **Net savings: ~1.5h wall-clock per walk session + restored user trust trong walk efficiency**

### 5.4 Self-test PASS ✅

Rule fires correctly trên originating Wave flow-kh1 G2 walk session. Prospective application to Flow Verification Campaign §4 22-flow queue eliminates same class permanently.

---

## 6. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 6.1 Reviewer-checklist (active now)

Pre-merge review cho Wave plan PR / Bucket PR shipping user-facing flow per §2 trigger:

- [ ] Wave scope matches §2 trigger pattern (signup / auth / invite / payment / tenant-switch / upload / email-driven / async / persona-AC)?
- [ ] Pre-walk simulation agent spawned BEFORE walk (per §1 mandate)?
- [ ] Agent output saved artifact `documents/04-quality/audits/persona-review/YYYY-MM-DD-pre-walk-<flow>.md`?
- [ ] Min 5 failure modes returned với (a) where + (b) symptom + (c) pre-walk check?
- [ ] HIGH-confidence findings batch-fixed PRE walk (per §3.1 recommendation)?
- [ ] If override trailer present (per §6.4), reason valid?

### 6.2 Wave plan §3 Scope section extension

Wave plan files trong `documents/03-planning/waves/` PHẢI include "Pre-walk persona simulation" row trong §3 Scope khi wave matches §2 trigger:

```markdown
| Bucket | Scope | Owner | Walk class |
|---|---|---|---|
| 0 (Pre-walk) | Spawn Opus agent simulate <persona> walk <flow>, return ≥5 failure modes per pre-walk-persona-simulation-mandate.md §3 | Coordinator | n/a |
| A | ... | <agent> | user-facing flow ✅ pre-walk required |
| B | ... | <agent> | internal refactor — pre-walk N/A |
```

### 6.3 Memory auto-load (paired same-PR)

Memory entry `feedback_pre_walk_persona_simulation.md` loads at session start, reminds 4-bullet checklist:
1. Wave / PR ship user-facing flow per §2?
2. Pre-walk persona simulation agent spawned per §1?
3. Min 5 failure modes returned + saved artifact?
4. HIGH-confidence findings batch-fixed PRE walk?

### 6.4 Override mechanism

Genuine exception (vd vendor sandbox down blocks walk, walk persona unavailable, hotfix urgency):

```
git commit -m "...
PRE_WALK_PERSONA_DEFER: <wave/PR ID> — <reason — e.g. 'hotfix P0, walk skipped per release-fix-retry-budget.md §5'>
PRE_WALK_PERSONA_FOLLOWUP: <gap link OR wave plan link scheduling pre-walk within Ndays>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely §2 trigger scope mis-defined OR rule overhead too high).

### 6.5 CI grep detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

- **Detector complexity:** Scan wave plan PR body / commit body for §2 trigger keywords (signup/auth/invite/payment/tenant-switch/upload) + verify presence of pre-walk artifact reference — moderate scope, NLP classification
- **Recurrence count:** 1 today (Wave flow-kh1 G2 walk)
- **FP risk:** Moderate — many legit wave scopes touch flow keywords without needing full pre-walk simulation (vd minor copy edits on login page)
- **Decision:** Reviewer-checklist §6.1 + memory auto-load §6.3 + Wave plan §3 row §6.2 + worked self-test §5 sufficient cho v1.0.0; revisit detector khi recurrence-count ≥2 post-rule (Flow Verification Campaign §4 22-flow queue subsequent walks)

---

## 7. Relationship to other rules

- **`feature-ship-runtime-walk-mandate.md`** v1.1.0 §3.4 — sister rule covers DURING-walk catalog-then-batch protocol; this rule extends to PRE-walk persona simulation. Both compose: pre-walk surfaces 80%+ → walk catches residual → catalog-then-batch fixes batch.
- **`pre-handoff-self-test-completeness.md`** v1.2.0 §3 — sister rule covers POST-FIX re-walk mandate; this rule covers PRE-original-walk. Three rules form full walk lifecycle: PRE (this) → DURING (`feature-ship-runtime-walk-mandate` §3.4) → POST-FIX (`pre-handoff-self-test-completeness` §3).
- **`outside-in-coverage-trigger.md`** v1.1.0 — sister rule covers inside-out → outside-in audit at wave/scope brainstorm time; this rule covers persona simulation at flow-walk time. Different trigger boundary, complementary scope.
- **`agent-model-opus-default.md`** v1.0.0 §1 — pre-walk agent MUST use Opus 4.7 (reasoning depth for persona simulation).
- **`agent-background-spawn-default.md`** v1.0.1 §1 — pre-walk agent MUST background-spawn (coordinator prep walk runbook parallel).
- **`docs-only-pr-no-block-wait.md`** v1.1.0 §5.5 — pre-walk agent spawn = heavy local op (Opus reasoning), continue work parallel.
- **`audit-to-gap-pipeline.md`** §2.5-§2.8 state-check ladder — pre-walk simulation finds candidate gaps; pipeline filters duplicates + handles existing gaps.
- **`persona-based-business-review.md`** skill — extended với "Pre-Walk Mode" section paired same-PR cho concrete invocation pattern.
- **`simulation-gap-finder.md`** skill — used in pre-walk agent prompt cho failure-mode matrix.
- **`wave-pack-planner` SKILL.md** — extended với "Pre-walk persona simulation" step paired same-PR cho wave plan integration.
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-06-04 Wave flow-kh1 G2 walk surface 6 bugs (83% pre-walk-detectable) applied through 5-stage pipeline.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + skill updates + memory + worked self-test §5 + rules-index.csv row + output-review-mandate.md §3 row all paired same PR.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier (1 chuẩn pre-walk → mọi Flow Verification Campaign §4 22-flow queue subsequent walks auto-comply prospectively → eliminate 80%+ surprise-bug class permanently).
- **`output-review-mandate.md`** §3 — paired same-PR row "User-facing flow walk readiness" tracking pre-walk artifact standard.
- **`feedback_pre_walk_persona_simulation.md`** (memory, paired same-PR per Enforcement Parity).

---

## 8. Log

- **2026-06-04 (v1.0.0):** Rule created in response to Wave flow-kh1 G2 walk session 2026-06-04 surfacing 6 unexpected bugs trong ~3h (GAP-924 silent 2FA verify UI / GAP-925 BE String double-encode / GAP-926 FE generic catch / GAP-927 rollback rotate without resend / GAP-928 Phase 1+2 false 503 + write route / GAP-930 admin-new-login-alert path mismatch). Per §5 worked self-test, 5 of 6 (83%) would have surfaced trong 5-min pre-walk Opus persona simulation asking "Tôi là invitee — gặp lỗi gì? retry sao? token expired sao?". Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (walk session evidence + retro lesson "persona simulation BEFORE walk would have caught these") → Classify ✓ (no existing rule mandates pre-walk persona simulation; `feature-ship-runtime-walk-mandate.md` §3.4 covers catalog DURING walk; `pre-handoff-self-test-completeness.md` §3 covers POST-FIX re-walk; `outside-in-coverage-trigger.md` covers wave-scope brainstorm not flow-walk boundary; gap previously uncovered) → Rule+Enforce ✓ (this file + memory `feedback_pre_walk_persona_simulation.md` paired same-PR + `persona-based-business-review.md` skill extension "Pre-Walk Mode" + `wave-pack-planner` SKILL.md step + reviewer-checklist + rules-index.csv row + output-review-mandate.md §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§5 worked example trên Wave flow-kh1 originating incident — rule fires correctly + counterfactual ~1.5h wall-clock saved per walk session) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — fix 1 chuẩn pre-walk → Flow Verification Campaign §4 22-flow queue subsequent walks auto-comply prospectively → eliminate 80%+ surprise-bug class at original-ship moment. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered "pre-walk persona simulation" gate; no constraint loosening for prior walks; existing walks grandfathered; rule applies prospectively từ next user-facing flow walk forward 2026-06-04). Atomic-unique-bar check passed: ✅ atomic (single concept: pre-walk persona simulation BEFORE local Docker stack walk) + ✅ unique (sister rules cover DURING + POST-FIX boundaries) + ✅ widely applicable (every user-facing flow walk subsequent) + ✅ body discipline §1 ≤2 "and" conjunctions. CI grep detector (§6.5) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions; reviewer-checklist + wave plan §3 row + memory + worked self-test §5 sufficient cho v1.0.0.
