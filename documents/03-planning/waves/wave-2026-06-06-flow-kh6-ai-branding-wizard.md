---
title: Wave flow-kh6 — KH-6 AI Branding wizard G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kh6]
wave: wave-2026-06-06-flow-kh6
tag_primary: flow-kh6
tags_secondary: [branding, ai-wizard, outbox, async, idor]
date: 2026-06-06
flow: KH-6 (AI Branding wizard generate→apply→approval)
gaps: [GAP-1019, GAP-1020, GAP-1021, GAP-1022]
---

# Wave flow-kh6 — KH-6 AI Branding wizard G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KH-6 — Owner dùng AI Branding wizard: generate (theme/text/image) → tạo branding job (async) → assets → apply. Flow secondary thứ 2 trong đợt G1-cho-tất-cả-flow.

## 1. Brainstorm

KH-6 là flow nặng nhất nhóm secondary: AI generation (mock mode local) + async job queue (RabbitMQ + outbox) + asset storage (MinIO) + apply/approval. Pre-walk Opus persona simulation (per `pre-walk-persona-simulation-mandate.md`) trả 12 failure mode; key insight: **AI provider mock-mode mặc định (KHÔNG phải gate)** — blocker thật ở gateway↔branding auth/tenant/outbox layer.

## 2. Task Breakdown

1. Static pre-walk: AI provider env (mock?), gateway X-Instance-Id, branding authority filter, schema.
2. Walk: generate-theme/text → create job → poll async → assets → apply template.
3. Batch-fix blocker-class bug → 1 rebuild → re-walk.
4. Cross-flow sweep bug class → file gap còn lại → flip campaign.

## 3. Scope

Walk-only G1 cho generate + job + apply path qua gateway. Trong scope: fix inline 2 blocker (outbox instance_id NOT NULL; filter async/error dispatch 401). Ngoài scope (→ gap): X-Instance-Id IDOR (gateway change), RLS GUC, tier-trust, job-apply persistence, SSE auth, outbox relay. Walk solo (coordinator); 1 Opus pre-walk agent.

## 4. State-Check Evidence

| Symbol | Verdict | Evidence |
|---|---|---|
| `AIBrandingController.generate-theme/text/image` | ✅ present | controller line 104/140/177 |
| `BrandingJobController.createJob` (X-Instance-Id) | ✅ present | line 74 |
| `BrandingEventEmitter.emit` + `BrandingOutboxEvent` | ✅ present | outbox/ package |
| `branding_outbox.instance_id NOT NULL` (V58 RLS) | ✅ present | V58__rls_sweep_kh.sql:41-69 — **entity drift confirmed** |
| AI provider mock | ✅ confirmed | running container `AI_PROVIDER=openai OPENAI_API_KEY=sk-mock-key` |

## 5. Verification Gates

### Pre-walk
12 failure mode, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kh6-ai-branding-wizard.md`. AI mock confirmed (not gate).

### G1 walk — evidence

Credential `owner.test@test.vn / Test@1234` (tenant/instance `22003e3c…`), gateway :9000, header `X-Instance-Id`.

**Happy paths (PASS, post-fix):**

| Step | Kết quả | Side effect |
|---|---|---|
| `POST /ai/generate-theme` | HTTP 200 | palette JSON (deterministic) |
| `POST /ai/generate-text` | HTTP 200 | VN mock marketing copy |
| `GET /templates` | HTTP 200 | Classic Academy + others |
| `POST /jobs` (create) | HTTP 201 | job QUEUED + outbox rows `instance_id` set |
| async process | QUEUED→COMPLETED ~5s | consumer via fast-path publish |
| `GET /jobs/{id}/assets` | HTTP 200 | full asset map (copy + logos + hero + og + profile) |
| `POST /templates/{id}/apply` | HTTP 200 | `status:applied` + themeConfig |

**Sad path (PASS):** create job thiếu `X-Instance-Id` → HTTP 400 (đúng, sau Bug B fix — trước bị mask 401).

**Bugs surfaced (6+) — 2 fix inline, 4 file gap:**

| FM | Severity | Verdict |
|---|---|---|
| Bug A: `branding_outbox.instance_id` NOT NULL (V58 RLS drift; emitter never set) → ALL job create 500-masked-401 | **P0** | **FIXED inline** |
| Bug B: `XUserRolesHeaderFilter` (OncePerRequestFilter) skips async+error dispatch → 4 Mono AI endpoints 401 + errors masked 401 | **P1** | **FIXED inline** |
| FM-1 X-Instance-Id client-controlled IDOR | P0 | GAP-1019 |
| FM-3 RLS GUC not set + FM-6 tier header client-controlled | P1 | GAP-1020 |
| FM-7 job assets không persist active theme + FM-4 SSE auth | P1 | GAP-1021 |
| FM-5 outbox relay not dispatching | P2 | GAP-1022 |

### Inline fixes (this wave)

1. **Bug A** — `BrandingOutboxEvent` +`instanceId` field (`@Column instance_id NOT NULL`) + `BrandingEventEmitter.emit()` +`instanceId` param + builder + 2 callers (`BrandingJobService` pass instanceId; `InstanceLifecycleService` pass instanceId=aggregateId). Entity-migration drift per `design-patterns.md` §3.12 (V58 added column, entity/emitter never updated).
2. **Bug B** — `XUserRolesHeaderFilter` override `shouldNotFilterAsyncDispatch()` + `shouldNotFilterErrorDispatch()` → `false` (re-auth on servlet async/error re-dispatch that Mono triggers).

Caller sweep (per `api-contract-change-caller-sweep`): 2 prod + 4 test (`BrandingEventEmitterTest` ×4 + `BrandingJobServiceTest` + `InstanceLifecycleServiceTest` verify) migrated. `./mvnw test` 21/21 PASS. Rebuild + re-walk: generate-text 200 ✓, job create 201 + outbox instance_id set ✓, missing-header 400 ✓.

## 6. Agent Spawn Pattern

N/A — walk solo. 1 Opus background agent cho pre-walk persona simulation.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1019: Branding X-Instance-Id client-controlled IDOR (P0, Backend)
- GAP-1020: Branding RLS GUC + tier header trust (P1, Backend)
- GAP-1021: Branding job-apply persistence + SSE auth (P1, Mixed)
- GAP-1022: Branding outbox relay not dispatching (P2, Backend)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

- **Bug A:** V58 added `instance_id NOT NULL` cho 2 tables — `payments` (set by payment code, KH-5 walk confirmed OK) + `branding_outbox` (fixed). Contained.
- **Bug B:** subscription `XUserRolesHeaderFilter` cùng OncePerRequestFilter pattern NHƯNG không có Mono endpoint → không bị async-dispatch 401 (KH-5 500 surfaced đúng). Note: nếu subscription thêm reactive endpoint sau → cần cùng fix.

### Sync targets

Campaign §4 KH-6 → `🔄 walk-pass-pending-human`; wave-history flow-kh6; gap-status.csv 4 rows; audits-index pre-walk row. Test sub `81cf38cd` không đụng (KH-6 dùng instance branding scope).

### Outcome

KH-6 **G1 ✅ PASS** sau 2 inline fix (cả 2 là walk-blocker: outbox chặn toàn bộ job creation; filter chặn 4 AI endpoint). Core wizard flow walk được end-to-end. **Lưu ý review/G2:** GAP-1019 P0 IDOR cùng systemic gateway-tenant-bind với GAP-1015 — nên fix chung; GAP-1021 (job-apply persistence) là gap chức năng thật của wizard "approve" step.

## 8. Log

- **2026-06-06:** Wave flow-kh6 — KH-6 G1 walk complete. 2 inline fixes (Bug A outbox instance_id V58 drift + Bug B filter async/error dispatch 401) + 4 gaps (GAP-1019 P0 IDOR + GAP-1020/1021 P1 + GAP-1022 P2). Campaign row → walk-pass-pending-human.
