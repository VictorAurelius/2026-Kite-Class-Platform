---
title: Wave flow-kc1 — Tenant provisioning + settings post-onboarding
status: draft
created: 2026-06-04
updated: 2026-06-04
waves: [flow-kc1]
tag_primary: flow
tags_secondary: [kc1, tenant-provisioning, kiteclass, settings, campaign]
counter: 1
gaps: []
campaign: flow-verification-campaign
---

# Wave flow-kc1 — Tenant provisioning + settings post-onboarding

**Goal:** Walk end-to-end flow KC-1 (Owner mở `/settings` của tenant trên KiteClass FE → xác minh state mặc định sau onboarding wizard → sửa được tối thiểu một trường) trên stack production-equivalent, đạt **G1 PASS**. Verify tenant đã ra lò sẵn sàng để Owner bắt đầu mời nhân viên (KC-2) + setup khóa học (KC-3).

**Trigger:** Flow KC-1 đứng giữa KH-2c (✅ THÔNG) và toàn bộ chuỗi KC-* nghiệp vụ trường (mỗi flow sau đều giả định tenant đã configured đúng). Nếu KC-1 chưa thông → mọi KC-2..KC-9 sẽ tự fail vì thiếu context nghiệp vụ (lịch tuần, niên khóa, branding, locale).

**Estimated wall-clock:** Loop đầu ~45-75 phút (cần verify cross-service KiteClass + KiteHub data sync); subsequent loop ~15-25 phút/cycle.

---

## 1. Brainstorm

**Q1 (alignment):** Persona `Owner Tuấn` (g2test-an-8, đã login qua KH-2c + hoàn thành wizard) + domain `tenant-provisioning` + `kiteclass-core` (chứ không phải kitehub-* — đây là chuyển từ KiteHub SaaS sang KiteClass tenant). Domain downstream cho **mọi** KC-* (staff invite, course, enrollment, attendance, grade, invoice).

**Q2 (trade-offs):** Cân nhắc 3 path verify state mặc định:
- **A** State-check DB sau wizard (Postgres query trên tenant schema riêng) — xác minh row tồn tại + giá trị hợp lý. Đơn giản nhưng không cover UX render.
- **B** Walk qua FE settings page — verify Owner thấy mỗi trường đúng. Cover UX nhưng chậm.
- **C** Cả 2 — DB + FE. Đắt nhưng đầy đủ cho 3-gate (G1 BE-direct + G2 human FE).

→ Chọn C: DB query làm baseline G1; FE walk làm baseline G2. G3 production-parity verify tenant schema migration ổn trên RDS thật.

**Q3 (risks):**
- **Tenant schema isolation**: Phase 1 BETA dùng shared-schema multi-tenant (per ADR-001) — tenant settings nằm trong bảng `instances.settings` hoặc `tenant_settings` riêng? Cần state-check trước
- **Default seed at provisioning time**: wizard 5 bước có set tất cả setting cần thiết, hay còn trường `null` đợi Owner fill sau? Nếu sau wizard còn null fields → KC-2..KC-9 phải tự fallback
- **Cross-service sync**: tenant tạo ở kitehub-platform (Instance entity) nhưng nghiệp vụ ở kiteclass-core. Settings nằm phía nào? Có sync event không? Outbox?
- **Niên khóa Việt**: per `vn-localization-audit-checklist.md` — niên khóa school năm 2025-2026 hay 2026-2027? Tuần Mon-Sat (KHÔNG Mon-Fri)? Cần verify locale config khớp
- **Branding default**: Owner thấy `g2-test-center` slug hay tên đầy đủ "G2 Test Center"? Color scheme default? Logo placeholder?
- **Wizard 5 step coverage**: KH-2c wizard có yêu cầu nhập org-name, subdomain, address, contact phone, school-type. KC-1 verify từng trường này thành tenant settings + có sửa được không?

**Inside-out completeness check (per `inside-out-completeness-trigger.md`):** Deferred to session start — pull ROADMAP §🚀 + `inside-out-queue.md` + CSV `tenant`/`settings`/`onboarding` domain gaps trước khi lock §3 Scope.

**Outside-in audit (per `outside-in-coverage-trigger.md`):** Skip wave-plan thời điểm (per §4 row "wave 100% internal scope = N/A outside-in"). Sẽ pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` lúc bắt đầu walk thật, focus Owner psychology "tôi vừa qua wizard, giờ tôi thấy gì trong settings?".

---

## 2. Task Breakdown

| Bucket | Scope | Owner | Effort | Disjoint? |
|--------|-------|-------|--------|-----------|
| 0 (Pre-walk) | Spawn Opus pre-walk persona simulation per `pre-walk-persona-simulation-mandate.md` §1 — `Owner mở /settings sau onboarding wizard → expects tenant info đầy đủ → sửa được tên/branding/giờ làm việc`; return ≥5 failure modes | Coordinator | ~5-10 min agent + ~30 min batch-fix | n/a — single agent |
| A | Loop walk + catalog (DB query tenant settings → FE `/settings` page render → sửa 1 trường) | claude (session-pick) | 30-60 phút | n/a — state-continuous |
| B | Batch-fix blocker (DB seed thiếu / API contract drift / FE render gap) | claude | 10-30 phút/cycle | n/a |
| C | Re-walk + G1 verdict + G2 handoff MD per `g2-handoff-md-mandate.md` | claude | 15-30 phút | n/a |

Single-agent campaign-loop per Wave flow-kh1/kh2/kh3 protocol. Bucket-level expansion deferred — sẽ scope tại session start khi state-check reveals concrete scope items.

---

## 3. Scope

**TBD at session start.** Required reading before lock scope:

- `documents/01-business/kiteclass/tenant/{rules,use-cases,api-contract}.md` (kiểm tra tồn tại — nếu không có thì cần tạo 3-layer per `business-docs-3-layer.md`)
- `documents/01-business/kitehub/instance-provisioning/*` (tham chiếu chéo — vì KH wizard ghi data, KC đọc)
- `documents/01-business/kitehub/onboarding/*` (wizard 5 bước spec)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/{controller,service}/Tenant*` (entity + setting endpoints)
- `kiteclass/kiteclass-frontend/src/app/(authenticated)/settings/**` (FE settings page)
- Recent commit `c9ba7ed6 feat(wave-14-bcde): entity sync + audit-UUID sweep + DB CI gates` (Wave 14 đã thông schema drift — cần verify tenant cluster OK)
- `documents/03-planning/inside-out-queue.md` — user-flagged tenant scope items
- `flow-verification-campaign.md` §3 KC-1 row chú thích "auto from KH-2b" — clarify ý nghĩa "auto" (DB row tự tạo) vs "configured" (Owner setting đầy đủ)

**Dependency check:** Owner tenant từ KH-1+KH-2c chain ✅ (g2test-an-8 exists per Wave flow-kh1 closure + Wave flow-kh3 walk state — đã tạo subscription PENDING + Payment).

**Cross-layer check (per `contract-first-for-cross-layer.md` §3.2):** wave này có cross-layer scope (kiteclass FE + kiteclass-core BE) → cần Bucket 0 Foundation api-contract.md TRƯỚC khi spawn agents. Lazy decide khi state-check confirm cross-layer thật sự.

**Wave scope completeness reconciliation table** (per `wave-closure-scope-completeness.md` §3): defer to closure PR.

---

## 4. State-Check Evidence

**TBD at session start.** Required state-check rows per `audit-to-gap-pipeline.md` §2.6 wave-plan pre-flight + `contract-first-for-cross-layer.md` §3.2 api-contract row (likely cross-layer).

Minimum row checklist:
- `documents/01-business/kiteclass/tenant/api-contract.md` (api-contract for cross-layer scope — có thể chưa tồn tại, sẽ thành 🆕 to-be-created)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/entity/Tenant.java` (entity)
- `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/controller/TenantSettingsController.java` (BE endpoint)
- `kiteclass/kiteclass-frontend/src/app/(authenticated)/settings/page.tsx` (FE entry point)
- `kitehub/kitehub-platform/src/main/java/com/kitehub/platform/.../Instance.java` (cross-service reference — wizard ghi vào đây)
- DB query `SELECT * FROM kiteclass_g2_test_an_8.tenant_settings LIMIT 1` (tenant-scoped schema verify; thay schema name nếu khác)

---

## 5. Verification Gates

| Gate | Owner | Criteria | Status |
|---|---|---|---|
| G1 — agent runtime walk | Claude | (a) DB tenant settings row tồn tại với mọi trường mặc định hợp lý (org-name, subdomain, niên khóa 2025-2026, tuần Mon-Sat, locale vi-VN, currency VND, branding default); (b) GET `/api/v1/tenant/settings` qua gateway trả 200 + JSON đủ field; (c) FE `/settings` page render đúng trên KiteClass FE (port 3001 hay tenant-subdomain); (d) PATCH 1 trường (vd org-name "G2 Test Center" → "G2 Test Education") trả 200 + DB persist | ✅ **PASS (re-scoped)** — xem verdict §5.1 |
| G2 — human local test | User | Login Owner (g2test-an-8@example.com / WalkKh3@2026) → tới `/settings` KiteClass FE → check mỗi tab/section setting → sửa 1 trường + reload thấy giá trị mới | ⬜ |
| G3 — production parity | Claude + User | Production: tenant-scoped DB schema apply migrate sạch (Flyway trên RDS) + FE serve qua custom domain mapping (nếu Phase 1 BETA có custom-domain) hoặc subdomain wildcard + JWT tenantId claim resolve đúng tenant | ⬜ |

### 5.1 G1 verdict (2026-06-05, coordinator walk — re-scoped)

**Re-scope (user-approved AskUserQuestion 2026-06-05):** plan premise sai — KiteClass KHÔNG có "tenant settings thống nhất". Pre-walk persona sim + DB state-check xác nhận chỉ có `branding` + `user-preferences` (business docs đồng ý). academic-year/locale/currency/tuần Mon-Sat/org-name KHÔNG nằm trong settings → re-scope G1 về **branding + preferences**; tenant-config thiếu → GAP-980 (defer, không block).

**Walk target:** tenant `sky-education` (instance 0edaee10, TRIAL) — `g2test-an-8` plan giả định KHÔNG tồn tại trong DB hiện tại (đã reset). Stack production-equivalent: image fresh V86 (rebuild — GAP-978: build-all.sh bỏ sót KiteClass → stale-image suýt cho false-PASS), Flyway V86.

| Surface | Walk result |
|---|---|
| Branding GET qua gateway (`X-Instance-Subdomain`) | ✅ HTTP 200 + JSON đầy đủ persisted |
| Branding PUT đổi displayName | ✅ HTTP 200 + DB persist |
| FE `/settings` serve | ✅ HTTP 200 (full render = G2) |
| Sad: no-tenant GET | ✅ 400 graceful (no leak) |
| Sad: invalid color PUT | ✅ 400 VALIDATION_ERROR + fieldErrors |
| Preferences "Tùy chọn" tab | ❌→🟡 Bug GAP-979 (Owner 403, no numeric ref-id) → **fix shipped** (ẩn tab cho OWNER, `next build` PASS) |

**Verdict: G1 ✅ PASS** (branding surface production-equivalent + preferences-owner bug fixed). 3 gaps filed: GAP-978 (P1 devops), GAP-979 (P2 fix shipped, G2-visual pending), GAP-980 (P3 defer).

G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 — ship same PR as G1 PASS flip.

---

## 6. Agent Spawn Pattern

**Single-agent campaign-loop** per Wave flow-kh1/kh2/kh3 proven pattern:
- KHÔNG parallel-spawn cho walk (state-continuous flow)
- Pre-walk Opus agent BACKGROUND per `agent-background-spawn-default.md` §1 + `agent-model-opus-default.md` §1 — coordinator prep walk runbook parallel
- Walk + batch-fix + re-walk execute sequential trong session
- Nếu phát hiện cross-layer cần Bucket 0 Foundation (api-contract.md) — spawn 1 Opus agent ship foundation TRƯỚC khi walk thật

---

## 7. Closure Protocol

Per `feedback_post_merge_doc_sync.md` 4-target sync + `post-wave-cleanup.md` + `g2-handoff-md-mandate.md`:

1. Flip `gap-status.csv` rows DONE cho gaps closed wave này (if any) + git mv → `phase-1-beta/closed/`
2. ROADMAP §🎯 Current Status Snapshot — thêm Wave flow-kc1 closure entry
3. `wave-history.jsonl` append entry per `wave-tag-numbering-convention.md` §2.5 new schema (tag_primary=flow, secondary tags chứa kc1)
4. Wave plan frontmatter `status: draft → complete` flip + Scope-Completeness Reconciliation table per `wave-closure-scope-completeness.md` §3
5. Campaign §4 row flip → `🔄 walk-pass-pending-human` (G1 ✅) + ship G2 handoff MD recipe per `g2-handoff-md-mandate.md` §3 (`documents/05-guides/operations/YYYY-MM-DD-g2-recipe-kc1-tenant-settings.md`)
6. Session-handoff note `documents/03-planning/session-handoffs/YYYY-MM-DD-flow-kc1-closure.md`
7. `bash scripts/prune-merged-worktrees.sh --yes` per `post-wave-cleanup.md` §2

---

## 8. Log

- **2026-06-04 (plan stub ship):** Filed sau khi Wave flow-kh3 đi vào batch-fix UC-SUB-01 + GAP-938 + GAP-939 (3 PRs đang chờ CI). KC-1 = next-in-chain per campaign §3 dependency graph (KH-2c → KC-1 → KC-2..9). Owner tenant đã sẵn từ KH-1+KH-2c chain (g2test-an-8). Plan stub thỏa `check-wave-plan-completeness.sh` structural mandate (8 sections + 4 frontmatter fields). Full §3 Scope + §4 State-Check + bucket expansion happens tại session start khi pick wave này — `/start-session` → state-check `kiteclass/kiteclass-core/.../Tenant*` BE + `kiteclass-frontend/.../settings/` FE + docs `01-business/kiteclass/tenant/` → inside-out + outside-in audit per triggers → lock scope → walk G1.
