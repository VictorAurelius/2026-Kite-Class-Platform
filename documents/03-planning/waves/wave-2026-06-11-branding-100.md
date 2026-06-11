---
title: Wave branding-100 — wizard AI Branding "thông chuỗi thật + UX chuẩn ngành 100%"
status: draft
created: 2026-06-11
updated: 2026-06-11
waves: [branding-100]
tag_primary: branding-100
gaps: [GAP-1021, GAP-1108, GAP-1134, GAP-1135, GAP-1147, GAP-1160, GAP-1212, GAP-1213, GAP-1214, GAP-1215, GAP-1216, GAP-1217, GAP-1218, GAP-1219]
references:
  - documents/04-quality/audits/persona-review/2026-06-11-branding-100-persona-simulation.md
  - documents/04-quality/audits/persona-review/2026-06-11-branding-100-benchmark.md
  - documents/04-quality/audits/persona-review/2026-06-11-branding-100-failure-mode-matrix.md
  - documents/02-architecture/adr/ADR-037 (banner template HTML+Gemini + full-AI image)
---

# Wave branding-100 — wizard AI Branding thông chuỗi thật + UX chuẩn ngành

## 1. Brainstorm

**Q1 (outside-in — 3 audit 2026-06-11, per `outside-in-coverage-trigger` chạy TRƯỚC khi lock plan):**
- **Failure-mode (23/42 ô vỡ):** deploy = MOCK toàn bộ — không propagate KH-branding → KC-core ⇒ landing thật không bao giờ đổi từ wizard (GAP-1213, P0). Quality gate ≥70 không chạy (GAP-1217). WYSIWYG vỡ: preview-source ≠ deploy-source (GAP-1215). FAILED dead-end (GAP-1216).
- **Persona (12 findings):** 2 wizard lệch nhau — KH 7-bước ADR-037 canonical vs KC 6-bước orphan, KC preview `about:blank` (GAP-1214). Mode selector phải lên ĐẦU.
- **Benchmark (7 sản phẩm):** norm = **output-first** (generate sớm từ tên+ngành, refine sau), ≤5 bước, logo không bao giờ bắt buộc, FULL_AI prompt-path là entry default, preview nhiều biến thể, advanced fields progressive-disclose.

**Q2 (trade-off):** GAP-1213 có 2 đường — (a) propagation thật (outbox `branding.deployed` → KC-core consumer, per design-patterns §3.5) = giá trị thật; (b) interim relabel "mô phỏng" = rẻ nhưng wave tên "-100" thì (a) là bắt buộc, (b) chỉ là chặng disclaimer trong lúc làm. **Chốt: (a).**

**Q3 (user direction 2026-06-11):** "các bước AI branding chưa hợp lý 100%, ui kits cần design lại, thêm bước hoặc sửa layout tùy theo audit" → kit redesign (GAP-1212) là bucket đầu, theo bộ bước hợi tụ từ 3 audit.

## 2. Task Breakdown

| # | Task | Bucket | Est |
|---|---|---|---|
| 1 | Kit v3 screens theo §2.5 bộ bước (states GENERATING/FAILED/quota/approve) | A | 0.5-1d |
| 2 | Chốt canonical + KC route embed/redirect + retire orphan FSM | B | 0.5d |
| 3 | Outbox `branding.deployed` + KC-core consumer áp theme + evict cache | C | 1d |
| 4 | Quality gate ≥70 trong pipeline trước DEPLOYED + per-resource approve | C | 0.5d |
| 5 | Persist active theme + SSE auth (GAP-1021) + post-deploy summary/link (GAP-1108) | C | 0.5d |
| 6 | Preview = landing render path (?tenant= preview) + multi-variant | D | 1d |
| 7 | Reorder output-first: mode đầu + gộp audience/tone + generate sớm | E | 1d |
| 8 | FAILED retry/back + portrait step + banner generate wire (1134/1135/1160) | E | 1d |
| 9 | FULL_AI quota/label + regenerate no-op + copy + escape-ramp | F | 0.5d |
| 10 | Vòng fix-found-gaps: pre-walk sim + G1 browser walk + batch-fix + re-walk | gate | ≥30% |

## 2.5 Bộ bước wizard target (hội tụ 3 audit)

`Welcome+Mode (escape-ramp)` → `Brand personality (gộp Audience+Tone)` → `Assets (Logo/Portrait — optional, branch theo mode; FULL_AI bỏ qua)` → `Generate & Live Preview THẬT (multi-variant + quality gate /100 + per-resource approve)` → `Deploy (SSE + FAILED recovery)` → `Hoàn tất + link landing`. ≤5 bước nhập liệu; generate xảy ra NGAY sau bước 2 với defaults (output-first), các bước sau là refine.

## 3. Scope (buckets)

| Bucket | Việc | Gaps | Layer |
|---|---|---|---|
| **A. Kit redesign** | `ui_kits/ai-branding-wizard-v3` (hoặc v2 annotate superseded + v3) theo §2 bộ bước — screens đủ states (GENERATING/FAILED/quota/approve), ≥105/128 | GAP-1212 | Design |
| **B. Unify wizard** | Chốt canonical KH 7-bước; KC route embed/redirect; retire FSM orphan + preview about:blank | GAP-1214 | FE |
| **C. Chuỗi deploy thật (P0)** | Outbox `branding.deployed` → KC-core consumer áp theme/assets vào Branding/LandingPage + evict cache; persist active theme + SSE auth; quality gate ≥70 trước DEPLOYED; post-deploy summary + link landing | GAP-1213, GAP-1021, GAP-1217, GAP-1108 | BE+Mixed |
| **D. WYSIWYG preview** | Preview dùng chính landing render path (`?tenant=` preview mode) thay `buildLandingPreviewHtml`; multi-variant pick | GAP-1215 | FE+BE |
| **E. UX reorder (output-first)** | Mode lên đầu + gộp audience/tone + generate sớm + FAILED retry/back + portrait step + banner generate wire | GAP-1216, GAP-1134, GAP-1135, GAP-1147, GAP-1160 | FE+BE |
| **F. Trust/copy** | FULL_AI không trừ quota khi chưa có output thật + label đúng; regenerate no-op; logo over-promise; escape-ramp | GAP-1218, GAP-1219 | FE |

## 4. State-Check Evidence

3 audit artifacts 2026-06-11 (refs frontmatter) — mọi finding cite file:line; GAP-826/1204/1210 (landing render + heroImages + presigned regenerate) vừa ship wave landing-100 = nền cho bucket C/D. GAP-1147 tier-gate server-side + GAP-1160 portrait inline + GAP-1105 SSE reconnect đã ship trước (không lặp).

## 5. Verification Gates

| Gate | Cách |
|---|---|
| **Quality-target closure gate (per wave-closure-scope-completeness v1.2.0 §2.5 — creation-time mandate)** | **Metric:** (1) chuỗi end-to-end browser-verified: wizard → deploy → **landing per-tenant THẬT đổi theme/banner** (nip.io, per g1-browser-walk §3.1); (2) wizard UX rubric ≥90/100 (kit-based re-score). **Cam kết:** mọi gap surfaced trong wave (walk/review/audit) thuộc closure scope — fix trong wave, không kể phase, trước flip complete; defer duy nhất = PENDING external-blocked liệt kê blocker. **Budget vòng fix-found-gaps:** ≥30% effort wave (walk → catalog → batch-fix → re-walk per feature-ship §3.4). |
| G1 (Claude) | Per-bucket build+tests + browser-real walk wizard end-to-end (happy + FAILED + quota-hết + FULL_AI) |
| G2★ (human) | Recipe per `g2-handoff-md-mandate`: user chạy wizard từ settings → thấy landing đổi thật |
| Pre-walk | Persona-simulation agent per `pre-walk-persona-simulation-mandate` trước walk |

## 6. Agent Spawn Pattern

Đợt 1 (≤2 Opus + inline per `agent-concurrency-budget-inline-hybrid`): Agent A (kit v3 — design, disjoint docs) + Agent C (outbox propagation BE — kitehub-branding + kiteclass-core consumer); coordinator inline bucket F (copy/UX nhỏ). Đợt 2: Agent B+E (FE wizard reorder — chung vùng kitehub-frontend wizard, 1 agent) + Agent D (preview path). Worktree sibling per `worktree-only-branch-work`.

## 7. Closure Protocol

- Flip GAP list frontmatter DONE sau G1+G2★; reconciliation table per v1.2.0 §2.5 (mọi gap không kể phase; pending chỉ external-blocked).
- Re-score wizard rubric ≥90 + cập nhật GAP-1212 kit refs.
- Sync gap-status.csv + ROADMAP + wave-history + campaign row KH-6/KC-10.

## 8. Log

- **2026-06-11 (draft):** Plan tạo từ user directive "nên tạo wave 100 cho ai branding" + 3 outside-in audit (persona 12 findings / benchmark 7 sản phẩm / failure-mode 23/42 vỡ) chạy TRƯỚC khi lock scope per `outside-in-coverage-trigger` (user pick "cả 3 song song" AskUserQuestion). 7 gap mới filed từ findings (GAP-1213..1219). Quality-target gate khai báo tại creation per wave-closure v1.2.0 §2.5 (lần đầu áp creation-time mandate). Execute sau khi landing-100 đóng (human G2★).
