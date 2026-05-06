---
title: Wave 22 — UI Kits Polish (post-Round-3 follow-ups)
status: complete
created: 2026-05-06
updated: 2026-05-06
waves: [22]
gaps: [GAP-363, GAP-364, GAP-365]
---

# Wave 22 — UI Kits Polish (post-Round-3 follow-ups)

**Goal:** Close 3 disjoint Wave 20 follow-up gaps (kiteclass-student polish + kitehub-admin school-profile rebuild + Tier-1 S-student.md AC doc) so Track 2 ports GAP-269/GAP-271 unblock.
**Trigger:** Wave 20 SHIPPED 🟡 PARTIAL 2026-05-05 with 3 follow-up gaps filed; ROADMAP §🚀 Next Action lists Wave 22 as recommended pick. Track 2 Phase 4 BLOCKED on these.
**Estimated wall-clock:** ~34h agent work raw; longest-bucket ~14h serial → background-parallel wall-clock ~30-45min based on Wave 20-21 prior cadence (similar /128 polish scope).

---

## 1. Brainstorm (5-10 min)

**Q1 (alignment):**
- **S. Student persona** (Tier-1, mobile-PWA primary, K-12 + vocational/university spectrum): Bucket A polishes their kit; Bucket C ships the canonical AC doc that A+B audits cite.
- **P5 K-12 Principal + P2 Center Owner**: Bucket B polishes admin kit they consume (school-profile is provisioning-heavy screen for both).
- **Track 2 production port** (GAP-269 student + GAP-271 admin): both BLOCKED on this wave's outputs.
- **4-layer V-model coverage** (per `design-layer-coverage.md` §2):
  - 要件定義: Bucket C creates Tier-1 persona AC (S-student.md). A+B cite it.
  - 基本設計: A+B touch HTML kit screens (existing).
  - 詳細設計: payments.html Option C parent-trigger workflow = state machine sketch in HTML comments + cross-link to AC-FIN-001.
  - コンポーネント設計: A+B reuse `_shared/components/`; B may extract Zalo OA pattern (deferred to GAP-364b — out of scope here).

**Q2 (trade-offs):**
- **Reject:** ship full GAP-364 in single bucket (~37h serial → bottleneck swallows wave).
- **Reject:** sequential A→C→B (Bucket A needs S-student.md? — no, A can cite `secondary/student-in-P2.md` proxy refs Bucket A external review already used; C finalizes Tier-1 AC, no hard dependency mid-wave).
- **Accept:** GAP-364 ships 🟡 PARTIAL (school-profile rebuild only); cross-screen items (skeletons × 12, empty-states × 12, dark-mode × 12, staff-vetting screen, Zalo OA extract) → file GAP-364b in closure PR. Per `gap-done-discipline.md` §3 PARTIAL exit ramp. Saves ~23h from wave critical path.
- **Reject:** payments.html Option A (hide button) or B (move to vocational namespace) — Option C (parent-trigger workflow per AC-FIN-001) is preferred long-term per gap §"Proposed Fix" recommendation; aligns with parent-kép visualization deferred from Round 3.

**Q3 (risks):**
- **Risk: Bucket A payments.html Option C UX undefined** — mitigation: agent drafts mockup + cross-link to `parent-portal/rules.md` BR-PARENT-PORTAL-* for parent-trigger pattern; if uncertain, agent ships sketch + flags follow-up sub-gap.
- **Risk: Bucket B school-profile rebuild scope creep** — mitigation: explicit scope = single screen rebuild only; cross-screen items belong to GAP-364b filed in closure.
- **Risk: Bucket C S-student.md drift from secondary/* docs** — mitigation: C cross-references all 3 secondary docs explicitly; flips them to "extends Tier-1 S-student.md with tenant-context overrides" framing.
- **Risk: kit re-score < target after polish** — mitigation: each agent runs `quality/ui-review-prototype` skill self-check at end; if delta insufficient, file follow-up sub-gap rather than block bucket.
- **Risk: parallel agent worktree absolute-path contamination** (per `feedback_worktree_absolute_path_contamination.md`) — mitigation: ALL agent prompts use RELATIVE paths only; coordinator verifies branch identity post-completion.

---

## 2. Task Breakdown

| Bucket | Gap(s) | Owner | Effort (raw) | Disjoint? |
|--------|--------|-------|--------------|-----------|
| A | GAP-363 | bg-agent (worktree-isolated) | ~13h | ✅ kit `ui_kits/kiteclass-student/` only |
| B | GAP-364 PARTIAL | bg-agent (worktree-isolated) | ~14h | ✅ kit `ui_kits/kitehub-admin/screens/school-profile.html` + README only |
| C | GAP-365 | bg-agent (worktree-isolated) | ~7h | ✅ `documents/00-brd/persona-criteria/` only |

**Disjoint check:** A touches `ui_kits/kiteclass-student/`, B touches `ui_kits/kitehub-admin/`, C touches `documents/00-brd/persona-criteria/`. Zero file overlap. ROADMAP cross-link in closure PR (post-merge), not parallel.

---

## 3. Scope (per bucket)

### Bucket A — GAP-363 kiteclass-student polish

- **Files (modify):**
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/payments.html` — P0 rebuild Option C (parent-trigger workflow per AC-FIN-001)
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/my-classes.html` — chip "Yêu thích" add count parens
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/assignments.html` — reconcile tab counts vs subtitle
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/grade-detail.html` — TT 22/2021 weighting info-icon
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/profile.html` — link "Học lực Giỏi" pill → grades.html
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/README.md` — cross-link to GAP-363 + new score
- **payments.html Option C details:**
  - Replace primary "Đóng học phí ngay" button with "Yêu cầu ba/mẹ đóng" parent-trigger CTA
  - Update aria-label + screen-reader text accordingly
  - Add visible disclaimer block (replace HTML-comment-only L9): "Học sinh dưới 18 tuổi xem fees read-only. Yêu cầu thanh toán chuyển tới ba/mẹ qua KiteClass Parent Portal." citing AC-FIN-001
  - Cross-link to `documents/01-business/kiteclass/parent-portal/rules.md` BR-PARENT-PORTAL-* (read-mode scope guard)
  - Mock parent-trigger flow: chip showing "Đã gửi yêu cầu — chờ ba/mẹ xác nhận" state
- **Self-verification:** run `quality/ui-review-prototype` skill on 5 affected screens; target kit avg ≥105 (was 100.4); file sub-gap if any individual screen < 95 floor
- **Bucket-level AC** (subset of GAP-363 AC):
  - [ ] payments.html Option C implemented + new score ≥95
  - [ ] my-classes/assignments/grade-detail/profile polish items shipped
  - [ ] kit avg re-score ≥105
  - [ ] README cross-link added
  - [ ] GAP-363 file Log entry + status flip 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2

### Bucket B — GAP-364 PARTIAL kitehub-admin school-profile rebuild

- **Files (modify):**
  - `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/school-profile.html` — P1 rebuild form-only → dashboard-style (target 91 → ≥105)
  - `documents/02-architecture/design-system/ui_kits/kitehub-admin/README.md` — cross-link to GAP-364 + new score + GAP-364b deferral note
- **school-profile.html rebuild details:**
  - Hero KPI block (school stats: 1.247 HS / 62 GV / 25 lớp / NK 2025-2026) using existing `_shared/colors_and_type.css` tokens
  - Tabbed sections: Thông tin cơ bản / Cơ sở vật chất / Đội ngũ / Pháp lý
  - Progressive disclosure: Pháp lý & MoET tab default-collapsed
  - Visual cues: school logo upload preview, organizational chart sparkline, accreditation badge pills
  - WCAG AA contrast ratios self-measured + commented (per Round 3 kit standard)
  - Realistic VN K-12 mock data (matches existing kit voice)
- **OUT OF SCOPE (defer to GAP-364b):**
  - Loading skeletons across 12 screens
  - Per-screen empty states (gallery → in-context migration)
  - Dark-mode parity sweep
  - Staff vetting workflow visualization (AC-ONBOARD-005)
  - Zalo OA pattern extraction → `_shared/components/zalo-oa-card.html`
- **Self-verification:** `quality/ui-review-prototype` skill on school-profile.html; target ≥105
- **Bucket-level AC** (subset of GAP-364 AC):
  - [ ] school-profile.html rebuilt to ≥105 score
  - [ ] kit README cross-link added with GAP-364b deferral note
  - [ ] GAP-364 file Log entry + status flip 🔵 OPEN → 🟡 PARTIAL per `gap-done-discipline.md` §3
  - [ ] GAP-364b filing deferred to coordinator closure PR (not bucket scope)

### Bucket C — GAP-365 Tier-1 S-student.md AC doc

- **Files (create):**
  - `documents/00-brd/persona-criteria/S-student.md` — NEW Tier-1 doc following `_TEMPLATE.md`
- **Files (modify):**
  - `documents/00-brd/persona-criteria/README.md` — index update (add S-student row)
  - `documents/00-brd/personas-catalog.md` — top-level registry update
  - `documents/00-brd/persona-criteria/secondary/student-in-P2.md` — header note "extends Tier-1 [`S-student.md`](../S-student.md) with P2 tenant-context overrides"
  - `documents/00-brd/persona-criteria/secondary/student-in-P3.md` — same extension note
  - `documents/00-brd/persona-criteria/secondary/student-in-P5.md` — same extension note
  - `documents/02-architecture/design-system/ui_kits/kiteclass-student/README.md` — cross-link to new Tier-1 doc
- **S-student.md content scope:**
  - Persona basics (name, age range 6-22, primary device mobile-PWA, session pattern, comm preferences)
  - 8 journeys: Today / My Classes / Assignment workflow / Grades / Attendance / Notifications / Profile / Payment fees (READ-ONLY for K-12)
  - AC-* identifiers (use AC-* prefix per existing pattern):
    - AC-ONBOARD-001..N (registration, parent-paired account creation)
    - AC-FIN-001..N (READ-ONLY fees access; child-protection lock)
    - AC-EDGE-001..N (forgotten password parent-reset workflow)
    - AC-NOTIF-001..N (parent-kép visualization, throttling)
    - AC-CONTENT-001..N (assignment, grade, attendance access patterns)
  - Cross-references: secondary/* docs (preserved as tenant-context-specific extensions), personas-catalog, kit README, parent-portal rules.md
- **Self-verification:** doc passes `_TEMPLATE.md` structure check; AC-FIN-001 wording matches `secondary/student-in-P2.md` line 118 exactly (no drift)
- **Bucket-level AC** (subset of GAP-365 AC):
  - [ ] S-student.md created following _TEMPLATE.md
  - [ ] All 8 journey areas with AC-* identifiers
  - [ ] Child-protection constraints (AC-FIN-001 + AC-EDGE-001) enumerated
  - [ ] secondary/* docs flipped to "extends Tier-1" framing
  - [ ] personas-catalog.md + persona-criteria/README.md + kit README updated
  - [ ] GAP-365 file Log entry + status flip 🔵 OPEN → 🟢 DONE per `gap-done-discipline.md` §2

---

## 4. State-Check Evidence (BẮT BUỘC per `audit-to-gap-pipeline.md` §2.6)

| Symbol | Type | Verification command | Evidence | Verdict |
|--------|------|----------------------|----------|---------|
| `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/payments.html` | HTML kit screen | `Glob ui_kits/kiteclass-student/screens/*.html` | 13 files including payments.html | ✅ exists |
| `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/school-profile.html` | HTML kit screen | `Glob ui_kits/kitehub-admin/screens/school-profile.html` | 1 file | ✅ exists |
| `documents/02-architecture/design-system/ui_kits/_shared/colors_and_type.css` | Shared tokens | `Glob ui_kits/_shared/colors_and_type.css` | 1 file | ✅ exists |
| `documents/00-brd/persona-criteria/_TEMPLATE.md` | Persona doc template | `Glob persona-criteria/_TEMPLATE.md` | 1 file | ✅ exists |
| `documents/00-brd/persona-criteria/secondary/student-in-P2.md` | Proxy AC source | `Grep "AC-FIN-001\|AC-ONBOARD-003\|AC-EDGE-001"` | lines 118 / 76 / 154 | ✅ exists |
| `AC-FIN-001` (student read-only fees) | Persona AC | `Grep "AC-FIN-001" persona-criteria/secondary/student-in-P2.md` | line 118 verbatim | ✅ exists |
| `AC-ONBOARD-003` (≤13 yo wizard) | Persona AC | `Grep "AC-ONBOARD-003"` | line 76 | ✅ exists |
| `AC-EDGE-001` (parent-reset) | Persona AC | `Grep "AC-EDGE-001"` | line 154 | ✅ exists |
| `documents/00-brd/personas-catalog.md` | Top-level registry | `Glob personas-catalog.md` | 1 file | ✅ exists |
| `documents/02-architecture/design-system/dossier/03-screen-inventory.md` | Screen inventory | `Glob dossier/03-screen-inventory.md` | 1 file | ✅ exists |
| `.claude/skills/quality/ui-review-prototype/SKILL.md` | Re-score skill | `Glob quality/ui-review-prototype/SKILL.md` | 1 file | ✅ exists |
| `documents/01-business/kiteclass/parent-portal/rules.md` | Parent BR source | `Grep "BR-PARENT-PORTAL"` exists per Wave 19 Bucket C closure | per ROADMAP entry 2026-05-05 Bucket C | ✅ exists |
| `documents/00-brd/persona-criteria/S-student.md` | Tier-1 persona AC doc | (no file) | not present | 🆕 to-be-created (Bucket C) |
| `_shared/components/zalo-oa-card.html` | Reusable Zalo OA card | (out of scope) | n/a | 🆕 deferred → GAP-364b (NOT this wave) |
| `documents/02-architecture/design-system/ui_kits/kitehub-admin/screens/staff-vetting.html` | Staff vetting screen | (out of scope) | n/a | 🆕 deferred → GAP-364b (NOT this wave) |
| `GAP-364b-kitehub-admin-cross-screen-polish` | Follow-up gap | (filed at closure) | filed in closure PR | 🆕 to-be-created (closure PR) |

---

## 5. Verification Gates (per bucket)

| Bucket | Local verify command | Pre-merge gate |
|--------|---------------------|----------------|
| A | Open kit landing in browser → click each affected screen → run `quality/ui-review-prototype` skill on 5 modified screens | Manual /128 re-score in PR description; landing-parity script if README touched |
| B | Open `school-profile.html` in browser → run `quality/ui-review-prototype` skill | Manual /128 re-score in PR description |
| C | `_TEMPLATE.md` structure compare; cross-reference grep AC-FIN-001/AC-ONBOARD-003/AC-EDGE-001 against secondary/student-in-P2.md for verbatim consistency | Manual review; ensure no AC-* ID collision with existing P1/P2/P3/P5 docs |

No CI gate runs on docs-only kit/HTML/.md changes — manual reviewer checks per `output-review-mandate.md` §3 row "HTML/JSX prototypes".

---

## 6. Agent Spawn Pattern

Per `feedback_parallel_agent_strategy.md` + `agent-background-spawn-default.md` + `feedback_worktree_absolute_path_contamination.md`:

- All 3 buckets spawned with `run_in_background: true`
- `isolation: worktree` for parallel write safety (each bucket gets own worktree)
- **RELATIVE paths in agent prompts** — explicit reminder: agents do NOT `cd` outside worktree, do NOT use absolute paths starting with `/home/`, all `Read`/`Write`/`Edit` use repo-root-relative paths
- Coordinator merges sequentially after all background completions: A → B → C (alphabetical, no functional dependency)
- Per-bucket PR base = `main` (NOT stacked — buckets are file-disjoint)

---

## 7. Closure Protocol

Per `gap-done-discipline.md` + `feedback_post_merge_doc_sync.md` + `feedback_wave_history_append_required.md`:

- Each bucket PR updates affected GAP file Log + status (A: GAP-363 DONE; B: GAP-364 PARTIAL; C: GAP-365 DONE)
- Coordinator closure PR (after 3 bucket PRs merge):
  - File **GAP-364b** (cross-screen polish — skeletons / empty-states / dark-mode / staff-vetting / Zalo OA, ~23h, P2)
  - Update `documents/04-quality/gaps/ROADMAP.md` §🚀 Next Action signpost (Wave 22 SHIPPED + Track 2 unblocked status)
  - Flip wave plan frontmatter `status: draft` → `status: complete`
  - Append `wave-history.jsonl` Rule 15 entry
  - Update `documents/02-architecture/design-system/ui_kits/README.md` if score table changes
  - Verify landing parity: `bash documents/02-architecture/design-system/ui_kits/_shared/scripts/check-ui-kits-landing.sh`
- Sub-gaps for any per-bucket deferral filed inline by bucket PR (NOT closure)
- Track 2 GAP-269 (student port) + GAP-271 (admin port) cross-link updates: GAP-269 unblocked, GAP-271 unblocked-on-floor (cross-screen items still GAP-364b)

---

## 8. Log

- **2026-05-06** (draft): Plan created. Optimized scope: GAP-364 split → ship PARTIAL (school-profile only) + file GAP-364b at closure for ~23h cross-screen items. Saves ~23h from wave critical path. 3-bucket parallel; expected wall-clock ~30-45min based on Wave 20-21 cadence.
- **2026-05-06** (complete): Wave SHIPPED. 3 PRs merged: #811 (Bucket A GAP-363 — coordinator corrected DONE→PARTIAL because AC ≥105 self-rescore 102.5 unmet), #812 (Bucket B GAP-364 PARTIAL — school-profile.html 91→107), #813 (Bucket C GAP-365 DONE — S-student.md Tier-1 431 lines + 21 ACs). 4 follow-up gaps filed at closure: GAP-363b (external re-audit + delta), GAP-364b (cross-screen polish), GAP-366 + GAP-367 (meta — kit-as-source-of-truth standard + parity skill, surfaced by user Q2/Q3 during wave). Wall-clock 35min vs 45min estimated. 56th consecutive 0-clarif streak. Track 2 GAP-269 unblocked on P0; GAP-271 still BLOCKED on GAP-364b.
