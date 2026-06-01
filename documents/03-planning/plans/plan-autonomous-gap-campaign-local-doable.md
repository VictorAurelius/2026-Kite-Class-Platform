---
title: Autonomous Gap Campaign — local-doable phase-1 + phase-1.5 (no AWS / no external env)
status: active
created: 2026-06-01
updated: 2026-06-01
gaps: [phase-1-beta, phase-1.5-paid]
---

# Goal Spec — Autonomous Local-Doable Gap Campaign

> **Mục tiêu:** fix dần tất cả gap **local-doable** trong phase-1-beta + phase-1.5-paid (code + docs, verify bằng LOCAL Docker stack) — KHÔNG cần start stack AWS, KHÔNG cần cấu hình env ngoài. SKIP + mark blocked mọi gap cần AWS-deploy/DNS/SES/vendor/live-verify.

**Baseline 2026-06-01:** ~207 phase-1-beta active + 41 phase-1.5 = ~248 gap. KHÔNG fit 1 session → campaign nhiều session qua `/loop`.

---

## 1. Scope filter — local-doable vs blocked

Mỗi candidate gap, đọc Problem + Acceptance Criteria, phân loại RUNTIME:

### ✅ LOCAL-DOABLE (fix trong campaign)
- Code change verify bằng: unit/IT test (`./mvnw test`), local Docker stack (`kitehub/scripts/up.sh` — kite-postgres/redis/rabbitmq/minio), browser/Playwright, MailHog (email local)
- Docs / rules / skills / gap-file / ADR
- Meta (skill/rule/workflow)
- FE build local (`pnpm build`) + RST walk local

### ❌ BLOCKED (SKIP + mark, KHÔNG fix) — keyword AC chứa:
- **AWS deploy:** `terraform`, `AWS`, `EC2`, `RDS`, `ALB`, `ACM`, `ECR`, `SSM`, `CloudWatch`, `Secrets Manager`, `deploy-production`, `workflow_dispatch apply`
- **DNS/domain:** `DNS`, `Cloudflare`, `apex`, `TXT record`, `domain cutover`
- **Email live:** `SES`, `Resend` (live send), `deliverability` (MailHog local = OK)
- **Vendor/external:** `GPT`/`OpenAI` (live), `MoMo`/`VNPay`/`Zalo OA approval`, `Stripe`, `vendor approval`, `AWS Activate`
- **Live/prod verify:** `production smoke`, `live verify`, `beta tenant live`, `prod-equivalent on AWS`
- **External secret/env:** `secrets seeding` (prod), `kitehub/production/`, prod env-reference values

Khi BLOCKED → ghi 1-line trong gap notes "blocked: needs <reason> — campaign-skip" + giữ status hiện tại. KHÔNG flip DONE.

---

## 2. Priority order (per `meta-gap-priority.md`)

```
Meta (28) → P0 local-doable (31) → P1 (115) → P2 (55) → P3 (6)
```
Trong mỗi tier: dùng `wave-pack-planner` cluster ≥3 disjoint gap → spawn 4-5 **Opus** agent song song (per `agent-model-opus-default`).

---

## 3. Per-gap workflow (BẮT BUỘC — chống false-DONE)

1. **State-check** (per `audit-to-gap-pipeline.md` §2.8): `query-gaps.sh` + verify symptom còn tồn tại; nếu self-corrected → DONE với evidence.
2. **Superpowers:** brainstorm → task-breakdown → **TDD test-first**.
3. **Implement** + commit thường xuyên (feature branch, never main).
4. **Verify (gate DONE):**
   - Docs/meta gap → reviewer-checklist + self-test
   - Code gap user-facing → **RST walk LOCAL Docker stack** (per `feature-ship-runtime-walk-mandate`) + interactive affordance **runtime click-verify** (per `design-source-implementation-parity` §3.2)
   - **KHÔNG verify local được → giữ PARTIAL** (per `gap-done-discipline` §3) + file note. KHÔNG flip DONE mù.
5. **PR + CI** → docs-only auto-merge; code PR wait CI green (no `--admin` post-rebase per `admin-merge-discipline`).
6. **Sync 4 target** (per `post-merge-sync-completeness`): gap-status.csv + ROADMAP + wave-history + MEMORY.

---

## 4. Safety guardrails

- **Opus agents only** (per `agent-model-opus-default` — Sonnet thrash).
- **No false-DONE:** code gap không verify local → PARTIAL, không DONE (đây là rủi ro #1 của autonomous mass-fix).
- **Docs/meta = autonomous-safe; code = self-verify mandatory.**
- **Cross-flow sweep** sau mỗi bug-class fix (per `cross-flow-bug-class-sweep`).
- **Local Docker stack** required cho code verify — KHÔNG dùng AWS. Start: `bash kitehub/scripts/up.sh`.

---

## 5. Stop conditions

- Local-doable backlog cạn (mọi gap còn lại = blocked), HOẶC
- Token budget hết (per `/loop` budget), HOẶC
- ≥2 consecutive gap fail verify → pause + report (tránh thrash).

---

## 6. Session-start checklist (mỗi /loop session)

1. `/start-session` (context)
2. `bash kitehub/scripts/up.sh` — **local Docker stack** (NOT AWS) cho code verify
3. `bash scripts/query-gaps.sh "" "" phase-1-beta` — refresh backlog (CSV canonical)
4. Pick tier theo §2; cluster wave-pack; spawn Opus agents
5. PC no-sleep đã set 2026-06-01 (powercfg standby=Never) — campaign chạy liên tục khi cắm điện

---

## 7. Invocation

Session sau:
```
/loop fix local-doable phase-1/1.5 gaps per documents/03-planning/plans/plan-autonomous-gap-campaign-local-doable.md
```
`/loop` self-paced; mỗi vòng = 1 wave-pack tier. Dừng theo §5.

## 8. Out-of-scope (defer)
- Mọi BLOCKED gap §1 (AWS/DNS/SES/vendor/live) — chờ session có stack + env
- GAP-826 carousel production / GAP-815 editor UI nếu cần live verify
- Real-user actions (beta recruit, signed reviews) — không phải gap
