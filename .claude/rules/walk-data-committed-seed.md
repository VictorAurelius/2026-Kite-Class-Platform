---
paths:
  - "documents/03-planning/waves/wave-*-flow-*.md"
  - "documents/03-planning/roadmap/flow-verification-campaign.md"
  - "documents/05-guides/operations/*-g2-recipe-*.md"
  - "kitehub/scripts/seed-*.sh"
  - ".claude/rules/walk-data-committed-seed.md"
---

# Walk-Data Committed Seed — walk data từ committed idempotent seed, không ad-hoc

**Priority:** 🟠 MANDATORY — walk-data reproducibility + consistency governance
**Version:** 1.0.0
**Created:** 2026-06-16
**Last-Reviewed:** 2026-06-16
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + memory auto-load + worked self-test on 2026-06-16 g2walk ad-hoc seed incident) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "walk data ad-hoc → drift giữa env + agent-G1/human-G2"; META P1 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi walk (G1 browser / G2 human / G3 parity / RST) cần seed data (tenant / course / class / student / parent / payment) trên local Docker stack. Out-of-scope: walk dùng data tự-tạo-trong-walk (journey CRUD walk, owner tạo data qua UI), unit/integration test fixtures (`src/test/**` — khác layer).

---

## 1. The Rule

> **Walk data PHẢI đến từ MỘT committed idempotent seed script (single source of truth), KHÔNG seed ad-hoc/scratch.** Seed re-runnable sau mọi WSL restart / fresh stack → baseline Y HỆT → agent-G1 (Claude) và human-G2 (dev) walk cùng một data. Tenant + credential walk cố định, document trong G2 recipe.

Ad-hoc seed (script trong `.claude/g3-walk-scratch/`, lệnh curl rời, tenant provision tay) sống trong Docker volume nhưng KHÔNG reproducible: volume wipe / fresh stack / máy khác / human muốn re-create → mất, hoặc tái tạo ra data KHÁC → **drift 3 chiều: env (WSL restart) ↔ agent-G1 ↔ human-G2**. Drift = agent walk data A, human walk data B → kết luận walk không so sánh được, gap khó tái hiện.

Force-multiplier: 1 committed idempotent seed → mọi walk subsequent (22-flow campaign) dùng chung baseline → eliminate walk-data-drift class.

---

## 2. Trigger pattern — khi nào rule fires

| Pattern | Ví dụ |
|---|---|
| Walk cần tenant + journey data (course/class/student) | KC-3..7 walk cần course/class/student seeded |
| Walk cần persona account (parent/teacher/staff) + link | KC-8 parent walk cần parent + child link |
| Provision tenant cho walk | `POST /api/auth/register` tạo tenant walk |
| Seed payment/invoice/attendance data cho walk | KC-7 invoice, KC-5 attendance pre-data |
| Re-walk sau WSL restart / fresh stack | Cần re-establish cùng baseline |

Rule **KHÔNG** fires khi:
- Walk theo journey-order tự tạo data qua UI (owner builds up empty tenant — data là output của chính walk, không cần pre-seed)
- Unit/IT test fixtures (`src/test/**`, Testcontainers) — khác layer governance
- One-off probe không cần persisted data (curl smoke 1 endpoint)

---

## 3. Required — committed idempotent seed

### 3.1 Seed script location + tính chất

| Yêu cầu | Chi tiết |
|---|---|
| **Committed** | Dưới `kitehub/scripts/seed-*.sh` (NOT `.claude/g3-walk-scratch/` gitignored). Canonical: `kitehub/scripts/seed-walk-tenant.sh` (Flow Verification Campaign) HOẶC `seed-demo-independent-teachers.sh` (thesis demo) |
| **Idempotent** | Re-run an toàn: register 409/400 → tiếp; entity tồn tại → fetch-by-key (email/code/name), KHÔNG tạo duplicate; child rows 409 → skip |
| **Production API path** | Seed qua gateway `:9000` + owner JWT (production-accurate). Direct SQL chỉ khi API không cover (vd parent-child link chưa có endpoint) — document rõ |
| **Fixed creds** | Tenant subdomain + owner email/password CỐ ĐỊNH, hardcode trong script (vd `g2walk` / `g2walk@kite.local` / `G2walk@2026`) |
| **Self-reporting** | In ra baseline cuối (tenant + ids + access URL) để verify + reference |

### 3.2 Reference trong G2 recipe

G2 handoff recipe (per `g2-handoff-md-mandate.md`) PHẢI ghi rõ:
- Lệnh seed: `bash kitehub/scripts/seed-walk-tenant.sh`
- Tenant + credential walk (khớp seed output)
- Access URL production-accurate (per `g1-browser-walk-before-flip.md` §3.3 — nip.io subdomain cho KC)

→ Human chạy đúng 1 lệnh → data y hệt agent đã walk.

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Seed walk data bằng script trong `.claude/g3-walk-scratch/` rồi walk | Promote → committed `kitehub/scripts/seed-*.sh` idempotent |
| Provision tenant ad-hoc qua curl register, không committed | Tenant provision NẰM TRONG committed seed (idempotent) |
| Walk trên tenant data từ session trước (volume) không có seed tái tạo | Seed phải re-runnable → human/restart re-create đúng baseline |
| Seed non-idempotent (re-run tạo duplicate course/student) | Fetch-by-key idempotent (409 → reuse id) |
| Agent-G1 walk data A, hand human-G2 không nói data từ đâu | Recipe cite seed command → human walk cùng baseline |
| Hardcode random/ad-hoc creds mỗi lần seed | Fixed creds trong committed script |
| "Data còn trong volume nên khỏi commit seed" | Volume wipe/fresh stack/máy khác → mất; commit seed = bảo hiểm |

---

## 5. Override mechanism

Genuine exception (vd walk one-off cần data đặc thù không tái dùng, hoặc journey-walk tự tạo data):

```
git commit -m "...
WALK_SEED_ADHOC: <flow> — <reason — e.g. 'journey-order walk owner tạo data qua UI, không cần pre-seed'>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely nhiều walk seed ad-hoc → cần consolidate vào committed seed).

---

## 6. Worked self-test — g2walk ad-hoc seed incident (2026-06-16, originating)

**Scenario:** Agent-G1 walk KC-1→KC-8 trên tenant `g2walk` (provision ad-hoc qua `curl register`) + seed journey data qua `.claude/g3-walk-scratch/seed-g2walk.sh` (gitignored scratch, non-idempotent). User flag: "seed data phải thống nhất giữa các WSL và với G2 chứ?".

**Apply rule retroactively:**

| Dimension | Ad-hoc (thực tế) | Committed idempotent (rule) |
|---|---|---|
| Persist qua WSL restart | ✅ (Docker volume) | ✅ |
| Reproducible nếu volume wipe / fresh stack | ❌ scratch gitignored, mất | ✅ re-run script |
| Re-run an toàn (no duplicate) | ❌ non-idempotent → duplicate course/student | ✅ fetch-by-key skip |
| Agent-G1 ↔ human-G2 cùng data | ❌ human không có script tái tạo | ✅ recipe cite `bash seed-walk-tenant.sh` |
| Tenant provision reproducible | ❌ curl register tay | ✅ trong seed (409 OK) |

**Counterfactual với rule active từ đầu:** agent promote seed → committed `kitehub/scripts/seed-walk-tenant.sh` idempotent NGAY → human G2 chạy đúng 1 lệnh → walk data y hệt → 0 drift. Self-test PASS ✅ — rule fires đúng trên chính incident sinh ra nó. (Fix shipped same PR: `seed-walk-tenant.sh` idempotent, re-run verified 0 duplicate.)

---

## 7. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 7.1 Reviewer-checklist (active now)

Pre-merge review cho PR/walk seed data:
- [ ] Seed script committed dưới `kitehub/scripts/` (KHÔNG `.claude/g3-walk-scratch/`)?
- [ ] Idempotent (re-run no duplicate — fetch-by-key)?
- [ ] Tenant + creds cố định hardcode?
- [ ] G2 recipe cite seed command + access URL production-accurate?
- [ ] Nếu ad-hoc → override trailer `WALK_SEED_ADHOC:` + lý do valid?

### 7.2 Self-detection (in-turn)

Trước khi seed walk data bằng curl rời / script scratch, mentally check: "Data này human-G2 + WSL-restart re-create được không?" Nếu KHÔNG → promote committed idempotent trước khi walk.

### 7.3 Memory auto-load (paired same-PR)

Memory `feedback_walk_data_committed_seed.md` reminds tại session start trước khi seed walk data.

### 7.4 Detector (HONEST DEFER per `incident-to-rule-pipeline.md` §3.1)

- **Complexity:** detect "walk references scratch-seed vs committed seed" cần parse walk artifact + cross-check script path — moderate, NLP-ish.
- **Recurrence:** 1 (2026-06-16 g2walk).
- **Decision:** reviewer-checklist §7.1 + memory §7.3 + worked self-test §6 sufficient cho v1.0.0; revisit khi recurrence ≥2.

---

## 8. Atomic-unique-bar check (per `rule-change-process.md` §5.1)

- ✅ **Atomic:** single concept = walk data từ committed idempotent seed
- ✅ **Unique:** `g1-browser-walk-before-flip.md` covers HOW walk (browser real); `pre-walk-persona-simulation-mandate.md` covers PRE-walk sim; `g2-handoff-md-mandate.md` covers recipe format — KHÔNG cover walk-DATA source/reproducibility. Different axis.
- ✅ **Widely applicable:** mọi data-dependent walk × 22-flow campaign
- ✅ **Body discipline:** §1 The Rule ≤2 "and"/"và" conjunction

---

## 9. Relationship to other rules

- **`g1-browser-walk-before-flip.md`** v1.3.0 §3.3 — canonical KC access recipe (nip.io); rule này cung cấp DATA cho access đó. Compose: seed data + production-accurate access = full walk setup.
- **`g2-handoff-md-mandate.md`** v1.0.2 — recipe format; rule này mandate recipe cite seed command.
- **`pre-walk-persona-simulation-mandate.md`** v1.0.0 — PRE-walk sim; orthogonal (sim trước, data trước).
- **`small-gap-inline-fix.md`** + **`discovery-to-gap-inline-filing.md`** — walk-found gap handling; rule này = walk-data setup upstream.
- **`worktree-only-branch-work.md`** — meta-PR này dùng worktree.
- **`incident-to-rule-pipeline.md`** v1.1 — rule này = direct output 2026-06-16 user-flagged "seed data phải thống nhất" qua 5-stage.
- **`rule-change-process.md`** §6.5 Enforcement Parity — rule + reviewer-checklist + memory + worked self-test §6 + rules-index.csv row + output-review-mandate §3 row all paired same PR.
- **`meta-gap-priority.md`** §3 — META P1 force-multiplier.
- **`context-budget-mandate.md`** §3.2 — path-scoped (walk artifacts + seed scripts) — 0 base-context cost.
- **`feedback_walk_data_committed_seed.md`** (memory, paired same-PR).

---

## 10. Auto-load justification (per `context-budget-mandate.md` §3.2)

Dùng `paths:` frontmatter (path-scoped, KHÔNG always-load). Fire khi touch walk artifacts (`wave-*-flow-*.md`, `flow-verification-campaign.md`, `*-g2-recipe-*.md`) hoặc seed scripts (`kitehub/scripts/seed-*.sh`) — đúng surface session walk/seed. Path-scope đủ; token tiết kiệm khi session không walk. Priority 🟠 MANDATORY; §5 override cho phép ad-hoc journey-walk.

---

## 11. Log

- **2026-06-16 (v1.0.0):** Rule created in response to user-flagged 2026-06-16 "seed data phải thống nhất giữa các WSL và với G2 chứ?" — sau khi agent-G1 walk KC-1→KC-8 trên tenant g2walk provision ad-hoc + seed qua `.claude/g3-walk-scratch/seed-g2walk.sh` (gitignored scratch, non-idempotent) → data persist trong Docker volume NHƯNG không reproducible (volume wipe/fresh stack/human re-create → drift agent-G1 ↔ human-G2 ↔ WSL restart). Per `incident-to-rule-pipeline.md` 5-stage: Detect ✓ (user-flagged) → Classify ✓ (no existing rule covers walk-DATA source/reproducibility; `g1-browser-walk` covers access path, `g2-handoff-md-mandate` covers recipe format, `pre-walk-persona-simulation` covers pre-walk sim — none cover data single-source-of-truth) → Rule+Enforce ✓ (this file + committed idempotent `kitehub/scripts/seed-walk-tenant.sh` + reviewer-checklist §7.1 + memory `feedback_walk_data_committed_seed.md` paired + worked self-test §6 on g2walk incident + rules-index.csv row + output-review-mandate §3 row per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 — rule fires correctly + seed-walk-tenant.sh idempotent re-run verified 0 duplicate + counterfactual 0 drift) → Retro Log ✓ (this entry). META P1 force-multiplier per `meta-gap-priority.md` §3 — 1 committed idempotent seed → mọi walk subsequent baseline thống nhất prospectively. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered walk-data-reproducibility class; no constraint loosening; existing ad-hoc seeds grandfathered (promote khi re-walk); rule applies prospectively từ this PR forward 2026-06-16). Atomic-unique-bar §8 passed. Detector (§7.4) HONEST-deferred per `incident-to-rule-pipeline.md` §3.1 (recurrence 1, NLP complexity); reviewer-checklist + memory + worked self-test sufficient cho v1.0.0. Path-scoped per `context-budget-mandate.md` §3.2.
