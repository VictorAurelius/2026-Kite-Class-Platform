# Session handoff — 2026-06-07 (P0 drive-down: 3 waves shipped)

**Scope:** Tiếp tục P0-over-G2 drive-down ([[project-p0-priority-over-g2]]). Session này ship **3 wave** + fix CI 2 PR kẹt. **Phase 1 BETA P0: 28 → 20.**

## Shipped (all merged to main)

| Wave | PR | Scope | Kết quả |
|------|----|-------|---------|
| **g2-blockers-1** | #2244-2248 | Clear 10 OPEN P1 flow-blockers (KH-9/6/5 + KC-7/6) | 5 PR merged; fix CI #2245 (NPE) + #2246 (9 stale-fixture tests) |
| **p0-prov-1** | #2249 | KC-1 provisioning + KH-3 subscription P0 closure | 7 gap DONE + GAP-1055 filed; P0 28→23 |
| **p0-ux-1** | #2250 | Local-verifiable UX (batch invoice + onboarding sample-data + mobile admin) | 3 P0 DONE; P0 23→20 |

## Gap outcomes

**p0-prov-1 (walk-verified live gateway :9000):**
- **GAP-942 DONE** — POST /api/platform/subscriptions BASIC → HTTP 201 + PENDING + null started_at/expires_at (was 409); regression IT 2/2.
- **GAP-945 DONE** — saga wired + `markProvisioned` PENDING→TRIAL on tenant.deployed.
- **GAP-946 DONE** (Phase 1 BETA scope) — defensive done + 0/9 half-provisioned; real `provisionInfrastructure` → **GAP-1055** Phase 1.5.
- **GAP-948 DONE** — tenant-ready email renders + delivers MailHog (after GAP-1053 template fix).
- **GAP-949 DONE** — 3 `TENANT_PROVISIONED` audit rows (FK-timing bug did not materialize).
- **GAP-1053 DONE** (P1) — tenant-ready Thymeleaf template + graceful `renderHtmlWithFallback`.
- **GAP-1054 DONE** (P2) — `SubscriptionPendingNullableColumnsIT` FK-parent-instance seed.
- **GAP-1055 OPEN** (P2, phase-1.5-paid) — `TenantProvisioningSaga.provisionInfrastructure` real DB-schema/MinIO/DNS (scope-split từ GAP-946).

**p0-ux-1 (walk-verified live gateway :9000):**
- **GAP-297 DONE** — batch monthly invoice BE (V93 + idempotency + pro-rata + IT 4/4) + FE (preview drawer). Live: prorated 1.3M (26/30d), confirm idempotent. NOTE: `InvoiceCreatedEvent` = Spring ApplicationEvent (mirrors single-invoice); email channel live (Wave 18a); Zalo/SMS dispatch defer Phase 1.5.
- **GAP-950 DONE** — sample-data import `POST /api/v1/onboarding/sample-data` (1 GV + 1 lớp + 3 HS). **Scope-revise:** wizard pre-existing (AC#1 ✅); AC#2 localStorage giữ (one-time onboarding, đủ Phase 1 BETA).
- **GAP-951 DONE** — mobile-first admin (44px targets + 7 page header responsive + Playwright Mobile SE 5/5). **Scope-revise:** sidebar collapsible pre-existing.

## Key lessons
- **State-check cứu rebuild thừa 2 lần** (GAP-950 wizard + GAP-951 sidebar đã tồn tại một phần — gap premise stale). Always state-check trước greenfield assumption.
- **Walk-convergent structure** cho cluster hội tụ 1 flow/module (p0-prov-1): KHÔNG fan-out, prep parallel → coordinator sequential walks.
- **Vendor-gating tách rõ** (`feedback_real_user_action_not_a_gap.md`): GAP-063/286 (Zalo/SMS), GAP-975/976 (SePay) cần user đăng ký vendor account = real-user action, không phải gap.

## NEXT SESSION — drive P0 tiếp (P0=20)

**Local-verifiable cluster đã cạn** (UX + provisioning đóng hết). Còn lại chủ yếu gated:
1. **SePay** (GAP-975/976, 85%) — cần one-time setup (S1-S3 recipe `2026-06-04-g2-recipe-kh3-subscription.md`) + Zalo OA... no, SePay key + tunnel. **Real-user action** trước, rồi walk verify.
2. **Vendor UX** (GAP-063 Zalo/SMS root → 286/297-dispatch) — cần Zalo OA Business account + SMS provider. Phase 1.5.
3. **AWS-gated cluster** (GAP-793/502/608/533/567/566/572/117/756/952) — cần `bash scripts/aws/start-stack.sh`.
4. **Net-new OPEN** (GAP-877 UUID sweep, GAP-885 RLS coverage, GAP-950... done) — GAP-885 RLS là real local kiteclass-core work.
5. **Outside-in/feature build** — onboarding wizard DB-progress (deferred), mobile OTP (GAP-286 vendor-gated).

**Gợi ý:** GAP-885 RLS coverage (local-buildable, real migration work) hoặc đợi user setup vendor (SePay/Zalo) để unblock 975/976 + 063/286.

## Stack state
Local prod-parity UP (kiteclass-core rebuilt V93 + onboarding + batch-invoice this session; kitehub-email rebuilt tenant-ready template). AWS STOPPED. `SEPAY_API_KEY` empty local.
