---
title: Wave 32 REWORK — AI Branding Wizard v2 (post-audit hardening, Opus model)
status: complete
created: 2026-05-07
updated: 2026-05-07
waves: [32]
gaps: [GAP-272]
supersedes: documents/03-planning/waves/wave-2026-05-06-32-ai-branding-wizard-v2.md
---

<!-- wave-plan-completeness-exempt: Pre-Wave-76 legacy plan — predates current _TEMPLATE.md structure -->

# Wave 32 REWORK — AI Branding Wizard v2 (post-audit hardening)

**Goal:** Re-execute Wave 32 (4 buckets A/B/C/D) sau khi audit 3 PRs (#883/884/885) đã shipped 2026-05-06 phát hiện scaffold-quality ship + AI Branding rules vi phạm + tests không chạy local. Original plan AC + scope vẫn giữ; rework brief này thêm **anti-pattern guards**, **stricter verification gates**, và **Opus model mandate** để tránh recurrence.

**Trigger:** User-flagged miss 2026-05-07 sau verify Wave 32 PRs phát hiện 2 unit tests fail trên Bucket D (TS2307 import depth lệch 6 vs 4) + 3 audit reports xác nhận pattern scaffold-as-DONE qua cả 3 buckets. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ → Classify ✓ (Sonnet crash skip verification gate) → Rule+Enforce ✓ (this rework brief) → Self-Test (Opus rerun verify) → Retro Log (Wave closure).

**Estimated wall-clock:** ~30-40 min/agent parallel với Opus (hơn Sonnet ~10min vì model verify chặt hơn — net wall-clock thấp hơn vì không cần round 2 fix).

---

## 1. Why rework

3 audit reports (commits ad-hoc, không file dưới `documents/04-quality/audits/` vì agents background) tóm tắt:

### Bucket A (#884) — ⚠️ NEEDS-WORK
- AC delivery ~85% nhưng heavy mocks: `MOCK_TAKEN_SLUGS`, `MOCK_BRAND`, `STUB_JOB_ID` không wire API
- AudienceStep + ToneStep stubs (~45 LOC each) hardcode `'mixed'`/`'professional'` defaults — cross-bucket scope leak
- 31 tests (PR claim 33), smoke-only, no integration
- Hardcoded VN strings inline
- Bundle baseline missing

### Bucket C (#885) — ❌ NOT-READY (worst)
- **3/5 required component files MISSING**:
  - `TemplateGrid.tsx` — inlined trong TemplateStep (vi phạm plan §3 Files)
  - `TemplateFullscreen.tsx` — **không tồn tại** (fullscreen modal + WCAG/responsive/text-safety badges)
  - `Step6Preview.tsx` — renamed → ApprovalStep, signature deviates
- **2/3 spec variants missing**: custom-prompt Enterprise-gated variant ❌, iframe live preview ❌
- Template filtering by audience+tone đọc state nhưng unused (vi phạm `ai-branding-guidelines.md` §2.2 spirit)
- ResourceToggle state local `useState` thay vì WizardState reducer (vi phạm plan §4.2 architecture)
- 9 tests không cover missing components

### Bucket D (#883) — ⚠️ NEEDS-WORK + 🔴 2 tests fail trên CI
- 9/14 AC ✅, 5/14 ⚠️ PARTIAL
- 🔴 **§5 QualityGate là MOCK**: `mockChecks()` returns fake pass/fail dựa trên score bucket (ai-branding-guidelines §5 yêu cầu real measurement)
- 🔴 **§6 Lifecycle State Machine VIOLATION**: `LifecycleInline` render-only stateless với `buildMockEvents()` hardcoded — vi phạm `ai-branding-guidelines.md` §6 "State transitions via InstanceLifecycleService ONLY"
- 🔴 Tier detection mocked FREE
- 🔴 Lint fix postmortem: agent skip local `pnpm test` (TS2307 import depth lệch 2 levels — phải fix coordinator-side)
- 2 unit tests fail trên CI rerun: SSE timeout (line 206) + ambiguous query "/Advanced Mode/" matches multiple elements

### Bucket B — VẮNG MẶT entirely
Original plan có 4 buckets A/B/C/D nhưng không có PR mở cho Bucket B (AudienceStep + ToneStep cards với 4 VN cards mỗi). Bucket A đã ship stubs nhận hardcoded defaults — Wave 32 thực tế không complete.

---

## 2. Reuse vs Recreate

**Reuse từ Wave 32 v1 (do NOT discard):**
- Original plan `wave-2026-05-06-32-ai-branding-wizard-v2.md` — toàn bộ AC, scope, state-check evidence, file list, test counts vẫn binding.
- Existing branches (`wave/32-bucket-{A,C,D}-*`) làm **scaffolding reference** — Opus agents có thể READ để hiểu pattern (imports, hook signatures, route structure) nhưng KHÔNG copy verbatim.
- Tests đã viết — Opus agents có thể REUSE good test cases (e.g., Bucket D's 8 functional tests cho RegenerateCounter quota/upsell logic) nhưng MUST run local + verify pass.

**Recreate clean trên branch mới:**
- Mỗi bucket spawn worktree mới (`wave/32-rework-bucket-{A,B,C,D}-*`).
- Original 3 PRs sẽ closed `wontfix-rework` với comment pointer đến rework PRs.
- Branches old preserve 7 ngày sau rework merge → prune.

---

## 3. Anti-patterns BANNED in rework (derived from audit)

Per `gap-done-discipline.md` §2 + audit findings, mỗi PR phải PASS check sau trước khi flip 🟢 DONE:

### 3.1 Mock-as-implementation BANNED

| ❌ Banned pattern | ✅ Required pattern |
|------------------|---------------------|
| `const MOCK_BRAND = { primary: '#2563eb', ... }` | Live data từ wizard state OR explicit `// TODO(GAP-XXX): wire to /api/...` + plan-§exemption ref + follow-up gap filed |
| `const STUB_JOB_ID = 'job-stub-id'` | Real jobId từ generate endpoint OR `// TODO(GAP-XXX)` + gap filed |
| `function mockChecks(score) { return ... }` | Real measurement service call OR component-level scaffold flagged via `// TODO(GAP-226/227/228)` + plan §11.4 reference |
| `useState<string[]>([])` cho state plan §architecture nói thuộc reducer | `useReducer` qua `wizard-shared.tsx` exported reducer; OR add field vào WizardState + dispatch action |
| Hardcoded slug taken list | Real `/api/.../slug-availability` call OR `// TODO(GAP-XXX)` + gap |
| `useBrandingTier()` always returns `FREE` | Real tier từ `useActiveSubscription(instanceId)` OR `// TODO(GAP-XXX)` + plan-§4.3 ref |

**Rule:** Mọi mock/stub data PHẢI có (1) file-level `// TODO(GAP-XXX): ...` comment, (2) follow-up gap filed trong cùng PR, (3) PR body §"Mocks deferred" section liệt kê. Nếu thiếu cả 3 → BLOCK merge.

### 3.2 Cross-bucket scope leak BANNED

- Bucket A KHÔNG tạo stub components cho B/C/D. Nếu B/C/D imports cần placeholder, A export TYPE STUB only (`export type AudienceStepProps = {...}`) — KHÔNG render component.
- Mỗi bucket chỉ touch components trong scope §3 Files của plan.
- Coordinator merge sequential A→B→C→D — nếu B chậm, A không "preempt" bằng cách ship B's stubs.

### 3.3 AI Branding rules strict enforcement

| Rule | Bucket | Enforcement |
|------|--------|-------------|
| §2.1 Free-form prompt BANNED for non-Enterprise | C | TemplateStep custom-prompt UI MUST gate `tier === 'ENTERPRISE'` check via `useBrandingTier()` — test case mandatory |
| §2.2 ≥6 SVG previews filtered by audience+tone | C | TemplateGrid filter `TEMPLATES.filter(t => matchesAudience(t, audience) && matchesTone(t, tone))` — test case mandatory |
| §4.2 Per-resource state in WizardState | C | `approvedResources: string[]` field trong WizardState reducer + actions `APPROVE_RESOURCE`/`UNAPPROVE_RESOURCE` — test reducer transitions |
| §5 QualityGate /100 logic | D | Component-level scaffold OK với explicit `// TODO(GAP-226/227/228)` per check; test cases verify component render với passed-in score, NOT mock-internal |
| §6 Lifecycle via InstanceLifecycleService | D | LifecycleInline accepts `instanceId` prop + uses real lifecycle service hook OR explicit `// TODO(GAP-XXX)` + plan-§6 reference + integration test stub |

### 3.4 Tests MUST run local before push

Per `feedback_agent_local_verify_both_layers.md` v2.0 (this rework strengthens):

**Pre-push verification gate per bucket** — agent MUST execute trong worktree + paste output trong PR body:
```bash
cd kitehub/kitehub-frontend
pnpm install --frozen-lockfile
pnpm type-check        # ALL PASS, no errors
pnpm test --run        # ALL PASS, no skipped (unless plan explicitly defers)
pnpm build             # PASS, no warnings escalated to errors
```

**PR body MUST include §"Local verification" section:**
````markdown
## Local verification (pre-push)

```
$ pnpm type-check
✓ tsc --noEmit clean (0 errors)

$ pnpm test --run
Test Files  N passed (N)
Tests  M passed (M)

$ pnpm build
✓ Compiled successfully
First Load JS:  /branding/wizard  X kB / Y kB  (baseline: Z kB / W kB, delta: -A%)
```
````

**Bundle baseline:** Bucket A pre-rework First Load JS for `/branding/wizard` = **149 kB** (per #884 PR body). All buckets target ≤ 149 kB or document regression reason.

### 3.5 PR body §"AC Coverage" mandatory

Mỗi PR body PHẢI có table:
| AC item (from plan §3) | Status | Evidence |
|------------------------|--------|----------|
| ... | ✅ DELIVERED / ⚠️ PARTIAL / ❌ MISSING | file:line OR test name OR explicit defer + GAP ref |

Coordinator audit-gate sẽ check table presence trước merge.

### 3.6 i18n hardcode

Project state-check (Bucket A agent first task): grep `kitehub-frontend/src` for `useTranslation\|t\(` to confirm i18n system. IF system tồn tại → use translation keys mandatorily. IF không → accept hardcoded VN inline + file follow-up gap GAP-272h "i18n migration cho Wave 32 wizard components".

### 3.7 Test count accuracy

PR body claim test count = actual count. Bucket A v1 claimed 33, actual 31 → counts mismatch là red flag. Auditor sẽ verify `find . -name "*.test.tsx" | xargs grep -c "it(" | awk -F: '{sum+=$2} END {print sum}'`.

---

## 4. Per-bucket rework brief (deltas vs original plan)

### Bucket A — Wizard shell + StepIndicator + Steps 1-2 + wizard-shared

**Original plan §3 Bucket A scope unchanged.** Deltas:

- **Mock data**: `MOCK_TAKEN_SLUGS` MUST be replaced với real `/api/slug-availability` call OR explicit `// TODO(GAP-272i)` + gap filed cho slug-validation backend endpoint
- **Cross-bucket leak**: KHÔNG tạo `AudienceStep.tsx`/`ToneStep.tsx`/`TemplateStep.tsx`/`ApprovalStep.tsx` stubs. Export type stubs only:
  ```typescript
  // wizard-shared.tsx
  export type AudienceStepProps = { wizardState: WizardState; onNext: (audience: string) => void; onBack: () => void };
  export type ToneStepProps = { wizardState: WizardState; onNext: (tone: string) => void; onBack: () => void };
  ```
  `wizard/page.tsx` orchestrator dùng `dynamic()` với explicit fallback `<div>Bucket {B,C,D} chưa shipped</div>` cho phase trước khi B/C/D land.
- **WizardState reducer**: thêm `approvedResources: string[]` field + actions `APPROVE_RESOURCE`/`UNAPPROVE_RESOURCE`/`RESET_APPROVALS` (yêu cầu của Bucket C §4.2 compliance)
- **Tests ≥6**: claim count = actual count; tests verify reducer state transitions including `APPROVE_RESOURCE` action
- **Bundle baseline**: PR body MUST include `pnpm build` output với First Load JS for `/branding/wizard` route + delta vs 149 kB

### Bucket B — Steps 3-4 (Audience + Tone) — NEW BUCKET

**Bucket B chưa ship — scope từ original plan §3 Bucket B vẫn binding.** Cụ thể:

- 4 audience cards: mầm non / THCS / trung tâm tiếng Anh / luyện thi đại học
- 4 tone cards: Chuyên nghiệp / Thân thiện / Năng động / Cao cấp với tiny rendered preview
- AudienceCard + ToneCard components (separate files)
- Backend audience/tone persistence state-check at agent runtime (mock + GAP-272b/c if absent)
- Tests ≥4 per original plan
- AC coverage table mandatory in PR body
- Local verification §3.4 mandatory

### Bucket C — Step 5 + Step 6 main preview

**Original plan §3 Bucket C scope binding. Deltas đặc biệt critical (worst bucket trong v1):**

- **3 missing files PHẢI ship đúng tên + path:**
  - `kitehub-frontend/src/components/branding/wizard/TemplateGrid.tsx` (extracted, NOT inlined trong TemplateStep)
  - `kitehub-frontend/src/components/branding/wizard/TemplateFullscreen.tsx` (fullscreen modal với WCAG + responsive + text-safety badges)
  - `kitehub-frontend/src/components/branding/wizard/Step6Preview.tsx` (NOT renamed `ApprovalStep` — keep `Step6Preview` name as plan §3 spec)
- **Custom-prompt Enterprise gating mandatory** — TemplateStep render `<CustomPromptInput />` IFF `useBrandingTier().canUseCustomPrompt === true`. Test case verify gate.
- **Iframe live preview mandatory** — Step6Preview render `<iframe src={previewUrl} />` với `previewUrl` từ template + brand state. Mock URL `data:text/html,...` if endpoint absent + `// TODO(GAP-272j)` + gap filed.
- **Template filtering mandatory** — `TEMPLATES.filter(t => matchesAudience(t, wizardState.audience) && matchesTone(t, wizardState.tone))`. Test case verify filtering logic.
- **WizardState approvedResources** — `Step6Preview` dispatches `APPROVE_RESOURCE`/`UNAPPROVE_RESOURCE` instead of local `useState`. Test reducer transitions.
- **G11 ThemePreview integration** — wire live brand colors từ wizard state, NOT MOCK_BRAND. If endpoint absent → `// TODO(GAP-272k)` + gap.
- **Tests ≥6** (original plan asks ≥4; rework mandates ≥6 to cover missing components):
  - TemplateGrid filtering by audience+tone props
  - TemplateFullscreen modal opens + 3 badges render
  - TemplateStep custom-prompt visible IFF tier ENTERPRISE
  - Step6Preview iframe renders
  - ResourceToggle dispatches APPROVE_RESOURCE action
  - ResourceToggle reducer state persists across re-render

### Bucket D — Step 6 sub-states + Settings Advanced + lifecycle inline

**Original plan §3 Bucket D scope binding. Deltas:**

- **§5 QualityGate scaffold flagging**: 3 explicit `// TODO(GAP-226)` (WCAG real measurement), `// TODO(GAP-227)` (visual regression), `// TODO(GAP-228)` (ML classifier) AT THE FILE TOP của QualityGateWidget + PR body §"Mocks deferred" lists 3 items
- **§6 LifecycleInline service integration**: component MUST accept `instanceId` prop + use `useInstanceLifecycle(instanceId)` hook (or stub hook returning typed events từ real service). State transitions NOT hardcoded `buildMockEvents`. If real hook absent → file `// TODO(GAP-272l)` + gap stub hook with TODO inside hook implementation, plan-§6-violation-defer flag in PR body
- **Tier detection real**: `useBrandingTier()` MUST call real `useActiveSubscription` OR explicit deferral với plan §4.3 reference + GAP-272m
- **2 broken tests fix mandatory**:
  - Line 206 SSE timeout: use `vi.useFakeTimers()` + `vi.advanceTimersByTimeAsync(N)` correctly; test must pass <2s wall
  - Line 305 "/Advanced Mode/" multiple matches: use `screen.getByRole('heading', { name: /Advanced Mode/ })` or `getAllByText(...)` then `[0]`
- **Local verify gate**: PR body MUST include `pnpm test --run -- src/components/branding/wizard/__tests__/wave32-bucket-d.test.tsx` output showing 0 failures
- **Tests ≥8** per original plan, count accuracy verified

---

## 5. Verification gates (coordinator-enforced)

Mỗi rework PR phải pass **TẤT CẢ** trước khi merge:

| Gate | Method | Owner |
|------|--------|-------|
| 5.1 Local verify output trong PR body | §3.4 mandatory section | Agent self-check |
| 5.2 AC Coverage table trong PR body | §3.5 mandatory section | Agent self-check |
| 5.3 CI green (Lint + Type Check + Unit Tests + Build + Docker) | gh pr checks | Coordinator |
| 5.4 Audit pass: third-party Explore agent re-runs §1 audit | parallel coordinator job | Coordinator |
| 5.5 Anti-pattern grep clean | `grep -rE 'MOCK_|STUB_|hardcoded' kitehub-frontend/src/components/branding/wizard/` — every match has matching `// TODO(GAP-XXX)` line within 3 lines | Coordinator |
| 5.6 Bundle size delta documented | First Load JS in PR body | Agent self-check |
| 5.7 No cross-bucket file edits | git diff stat: only files trong scope §3 | Coordinator |

---

## 6. Agent spawn pattern

- **Model:** Opus 4.7 mandatory (per Wave 32 v1 retro: Sonnet ship-without-verify recurrence)
- **Background + worktree-isolated** per `agent-background-spawn-default.md` + `feedback_parallel_agent_strategy.md`
- **4 agents spawn parallel sau khi rework plan PR merge**:
  - Agent A (Bucket A) — `wave/32-rework-bucket-A-shell`
  - Agent B (Bucket B) — `wave/32-rework-bucket-B-audience-tone` (NEW)
  - Agent C (Bucket C) — `wave/32-rework-bucket-C-template-preview`
  - Agent D (Bucket D) — `wave/32-rework-bucket-D-substates-settings`
- **Coordinator checkout sau notifications** — sequential merge A→B→C→D với each gate §5
- **Token budget**: rework brief sẽ trigger ~4× Opus background agents — coordinator session đã ~150k tokens; spawn LATE risk → recommend `/clear` post-plan-merge

---

## 7. Closure protocol (delta vs original)

Same as original plan §7 + extras:
- Wave 32 status flip 🟡 PARTIAL → 🟢 DONE (after rework, original v1 misses fixed)
- Closure PR §"Audit findings retrospective" comparing v1 vs rework: scaffold count delta, mock count delta, test failure delta
- Memory entry `feedback_wave_32_sonnet_recurrence.md` documenting the verification gate enforcement that worked for Opus
- Original 3 PRs (#883/884/885) close `wontfix-rework` with comment pointer to rework PR numbers

---

## 8. Risks

| Risk | Mitigation |
|------|------------|
| R1 — Opus token budget exceeded mid-wave | spawn LATE in fresh `/clear` session; monitor `/repo-status` token usage |
| R2 — Backend endpoints still absent → mocks proliferate again | rework §3.1 mandates explicit `// TODO(GAP-XXX)` + gap filed; coordinator §5.5 grep gate |
| R3 — Rework branches contaminated với stale Sonnet code | fresh worktree từ origin/main, NOT from old wave/32-* branches |
| R4 — User pause mid-rework | branches preserve; resume per `feedback_session_resume_cross_contamination.md` pattern |
| R5 — Opus also crashes | Opus 4.7 production-stable; if issue → fallback to manual coordinator-driven incremental commits |

---

## 9. Log

- **2026-05-07 (draft):** Plan REWORK created sau audit Wave 32 v1 PRs (#883/884/885) phát hiện scaffold-as-DONE pattern + AI Branding rules vi phạm + tests không chạy local. Per `incident-to-rule-pipeline.md` 5-stage applied. Original plan §1-§7 vẫn binding; rework adds §3 Anti-patterns + §3.4-3.7 verification gates + Opus model mandate + Bucket B explicit re-spawn. Reviewer: @nguyenvankiet (solo-dev — execution incident response, not rule change). Closes Wave 32 v1 audit findings.
- **2026-05-07 (complete):** All 4 buckets shipped Opus rework (#888 Bucket C TemplateStep + #889 Bucket B audience+tone + #890 Bucket D Step 6 sub-states / QualityGate / Lifecycle / Settings + #892 Wave 32 letter collision recovery + GAP-272h tech debt). 4/4 verification gates passed (mock count = 0 hard mocks, 11 documented `// TODO(GAP-272x)` deferrals; 0 cross-bucket scope leak; tests run local before push). Phase B closure PR (this commit) wires orchestrator `(customer)/branding/wizard/page.tsx` — replaces 4 `BucketPlaceholder` blocks với real `dynamic()` imports + adapter callbacks (`handleAudienceNext` / `handleToneNext` dispatch SET_AUDIENCE/SET_TONE then NEXT_STEP because Bucket B's local `onNext(selected: string)` signature drifted from Bucket A's stub `onNext: () => void`). Final test count 67/67 wizard tests + tsc clean + `pnpm build` clean. Worktree prune deferred to post-Phase-A merge per `post-wave-cleanup.md` §2 (Phase A meta-update agent in-flight). GAP-272 stays 🟡 PARTIAL — sub-letters 272c-l (8 P1 + 1 P0 backend endpoints + MSW infra) tracked for Wave 34 backend cluster per locked Post-Wave-32 sequence (project memory). Status flips draft → complete.
