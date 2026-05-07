# ADR-026: Defer Ollama / FULL_AI Inference to Phase 2 (Phase 1 BETA = Template-Only)

**Status:** ACCEPTED
**Date:** 2026-05-07
**Deciders:** @nguyenvankiet (solo-dev, acting CTO + acting Product Owner)
**Reviewers:** N/A (solo-dev mode per CLAUDE.md decision context locked 2026-05-06)
**Related Gap(s):** GAP-416 (this ADR); GAP-006 (Gemma 4 9B migration — deferred consequence); GAP-225 (scaffold-as-DONE umbrella — Phase 2-4 future scope confirmed); GAP-228 (ML classifier scoring — deferred)
**Related Rule(s):** `.claude/rules/ai-branding-guidelines.md` §1.1 (STATIC/TEMPLATE/FULL_AI taxonomy)
**Supersedes:** Implicit Phase 1 BETA scope assumed FULL_AI inference per Wave 4 AI Branding scaffold + GAP-006 backlog framing

---

## Context

Phase 1 BETA invite-only soft launch (chốt 2026-05-06, P1+P2 personas) ban đầu giả định AI Branding bao gồm cả **FULL_AI route** (Ollama self-host LLM cho banner/hero/copy generation). Wave 4 đã ship scaffold của AI Branding pipeline (Analyzer → Planner → Executor) + 6 templates SVG (Wave 30/31). GAP-006 (Gemma 4 9B migration) backlog cho Phase 1 ship.

Audit + cost analysis 2026-05-07 cho thấy 3 con đường khả thi cho FULL_AI route Phase 1, mỗi cái đều có tradeoff fail Phase 1 BETA constraints:

| Option | Cost | Coverage | Issue |
|---|---|---|---|
| Local Ollama hybrid (cloud → tunnel → home Ollama) | ~$0 | ~70% (latency + uptime SLO không đạt) | KHÔNG production-ready, tunnel reliability, home power dependency |
| Cloud GPU EC2 g4dn.xlarge (24/7) | ~$379/mo | 100% | Vượt budget Architecture B ($72/mo Yr1 target) ~5×; pre-revenue |
| OpenAI cloud API (gpt-4o-mini) | ~$30-60/mo recurring | 100% | Recurring cost, PDPL data localization concern (data flow ra ngoài VN), vendor dependency |

**Constraints chốt 2026-05-06:**
- Solo-dev pre-revenue mode → cash burn budget ≤ $100/mo Phase 1 BETA
- Architecture B (ADR-025): split EC2 t3.medium + t3.small + RDS db.t3.micro → ~$72/mo Yr1 (Free Tier covers ~70%; AWS Activate $1k credit covers Yr1 effective $0)
- PDPL hard deadline 2026-07-01 → data localization requirement (Luật An ninh mạng 2018 + ND-53/2022/NĐ-CP) khiến cloud API dùng provider Mỹ phải document bổ sung
- 6 templates SVG đã ship Wave 30/31 → đã cover ~80% requests theo `ai-branding-guidelines.md` §1.1 (TEMPLATE-first routing)
- Phase 1 BETA scope: 5-10 invite tenants → đủ thử nghiệm template-only path

**Stakeholders:**
- Beta tenants P1 (small-center owner) + P2 (medium-center owner) — kỳ vọng "smart branding" nhưng có thể accept template variants
- Marketing — phải rebrand "AI Branding" thành "Smart Brand Templates" + "AI generation Phase 2" để tránh false advertising
- Compliance — PDPL audit trail simpler nếu KHÔNG có AI provider data flow Phase 1

---

## Decision

> **We will defer FULL_AI inference (Ollama / Gemma / cloud LLM) to Phase 2. Phase 1 BETA AI Branding scope = STATIC + TEMPLATE routes only, per `ai-branding-guidelines.md` §1.1 ResourceCategory taxonomy.**

Concretely:
1. Phase 1 BETA `ResourceRoutingService.classify()` MAY return STATIC hoặc TEMPLATE; FULL_AI route disabled via config flag `ai.fullai.enabled=false`.
2. Marketing copy thay đổi: "AI Branding" → "Smart Brand Templates"; tagline "AI generation coming Phase 2" trên signup + dashboard banner.
3. GAP-006 (Gemma 4 9B migration) status flip: 🔵 OPEN → 🟡 PARTIAL hoặc explicit DEFERRED tag với reference ADR-026.
4. GAP-225 cluster (scaffold-as-DONE umbrella) Phase 2-4 future scope confirmed; KHÔNG block Release 1 v0.9.0-beta.
5. GAP-228 (ML classifier scoring cho quality gate) → Phase 2.
6. `release-deploy-standard.md` Phase 1 BETA checklist (§3.1) không yêu cầu AI inference smoke test.

**Phase 2 trigger gate:** ≥30 paying tenants AND revenue covers either $379/mo (GPU EC2) hoặc $60/mo (OpenAI cloud API). Choice giữa 2 path defer to Phase 2 plan với fresh PDPL re-audit nếu chọn cloud API.

---

## Consequences

### Positive

- **Effective $0 AI infrastructure cost Phase 1 BETA** — không Ollama compute, không OpenAI API. Reserve budget cho EC2/RDS/Email production.
- **Free Tier compliance** — Architecture B fits AWS Singapore Free Tier per ADR-025; thêm GPU vượt budget hard cap.
- **PDPL data localization simpler** — không có provider AI Mỹ trong data flow Phase 1; chỉ cần document templates + user-uploaded assets.
- **Beta scope realistic** — 5-10 tenants × 6 templates SVG đủ variant; user feedback dữ liệu sẽ inform Phase 2 AI provider choice (Ollama vs OpenAI).
- **Marketing honesty** — "AI generation coming Phase 2" disclaimer tránh consumer protection risk (Luật Bảo vệ Quyền lợi Người tiêu dùng 2023 — "advertised features must be deliverable").
- **Wave 37 scope reducible** — không cần ship GPU autoscale, không cần Ollama Helm chart Phase 1.

### Negative

- **Differentiation gap vs competitor** — Hotmart/Teachable cùng có template path; Kite mất "AI" như USP. Mitigation: 6 templates Wave 30/31 ship "smart" via brand-color matching + tone selection (audience × tone combinatorics ~24 variants per template — đủ "feel intelligent" cho beta).
- **GAP-006 / GAP-225 backlog tăng tuổi** — defer ~6-9 tháng tới Phase 2 trigger. Mitigation: ADR-026 ghi rõ trigger gate; GAP-225 §"Future scope" đã có; quarterly retro re-evaluate.
- **Tech debt** — Phase 1 BETA ship code path FULL_AI scaffolded (Analyzer/Planner/Executor) nhưng disabled. Mitigation: feature flag `ai.fullai.enabled` + dead code annotation `@SuppressWarnings("phase-2-deferred")`; quarterly dead-code review.
- **Beta tenant expectation management** — nếu tenant kỳ vọng AI generation, có thể churn. Mitigation: invite-only beta cho phép screening + onboarding wizard explain template path.

### Neutral

- **`ai-branding-guidelines.md` §1.1 taxonomy unchanged** — STATIC/TEMPLATE/FULL_AI structure đã design cho phased rollout; ADR-026 chỉ confirm Phase 1 = STATIC + TEMPLATE.
- **Quality gate scaffold (GAP-225 cluster)** unchanged — InstanceQualityReviewer + ContentModerationService scaffold ship Wave 4 vẫn callable cho TEMPLATE path; FULL_AI checks (WCAG measurement GAP-226 / visual regression GAP-227 / ML classifier GAP-228) defer Phase 2.
- **`output-review-mandate.md` §3 row "AI-generated assets"** unchanged status (⚠️ PARTIAL); FULL_AI deferral KHÔNG đổi review standard nhưng narrows what's reviewed Phase 1.

---

## Alternatives Considered

### Alternative A: Ship FULL_AI Phase 1 với Local Ollama hybrid (cloud → tunnel → home GPU)

**Pros:**
- Effective $0 AI compute cost (home hardware sunk cost)
- Full FULL_AI feature parity với Phase 2 vision
- Differentiation maintained vs competitor

**Cons:**
- Tunnel reliability ~95% (home internet + power dependency) — fail Phase 1 BETA SLO uptime ≥99% target per `release-deploy-standard.md` §3.1
- Latency p95 5-15s (tunnel hop + Ollama 9B inference WSL2 CPU) — fail UX target <3s per `nfr-catalog.md`
- Single point of failure (home machine) — không có HA Phase 1 budget
- WSL2 CPU inference ~30-60s per generation — không production grade

**Rejected because:** SLO + latency targets không đạt; complexity tunnel infrastructure không phù hợp solo-dev mode pre-revenue.

### Alternative B: Cloud GPU EC2 g4dn.xlarge 24/7

**Pros:**
- Production-grade FULL_AI inference Phase 1
- Single region (ap-southeast-1) — PDPL data localization OK
- Latency consistent <2s

**Cons:**
- $379/mo cost vượt Architecture B target ($72/mo) ~5×
- AWS Activate $1k credit cover ~2.6 tháng — ngắn hơn Phase 1 BETA window 9-12 tuần + buffer
- Pre-revenue → không sustainable

**Rejected because:** Cost vượt budget hard cap; pre-revenue không sustain.

### Alternative C: OpenAI cloud API (gpt-4o-mini) recurring $30-60/mo

**Pros:**
- Production-grade FULL_AI inference
- Cost-controlled (per-request billing)
- Quick implementation (existing OpenAIClient adapter Wave 4)

**Cons:**
- PDPL data localization concern — provider US, data flow ra ngoài VN, cần document additional consent (DPIA update)
- Vendor lock-in early — Phase 2 nếu pivot Ollama self-host phải re-engineer
- Recurring cost ăn vào budget Architecture B; Activate credit không cover OpenAI billing
- Marketing message dilution — "AI" nhưng outsource → khác value proposition self-host

**Rejected because:** PDPL hard deadline 2026-07-01 + complexity DPIA update + vendor lock-in cho early-stage product. Defer Phase 2 cho phép re-evaluate với fresh data.

### Alternative D: Skip AI Branding entirely Phase 1 (templates picker UI without "AI" framing)

**Pros:**
- Simplest scope
- No marketing rebrand needed (just "Templates")

**Cons:**
- Vứt bỏ Wave 4 scaffold investment + 6 templates Wave 30/31 marketing positioning
- Lose "smart" UX (audience × tone combinatorics) — degrade tới generic template picker
- Mất differentiation vs competitor entirely

**Rejected because:** Templates Wave 30/31 đã có brand-color matching + tone selection — đó là "smart" enough; chọn Decision (template-only với AI deferred messaging) preserves investment.

---

## Implementation Notes

### Migration strategy

1. **Same wave (Wave 37):** ADR-026 ship; GAP-006/GAP-225/GAP-228 cross-impact notes updated; feature flag `ai.fullai.enabled=false` confirmed in `application.yml`.
2. **Wave 38+ candidate:** Marketing copy update — "AI Branding" → "Smart Brand Templates" trong:
   - `kitehub-frontend` signup wizard
   - `kitehub-frontend` dashboard banner
   - Beta invite emails
   - README.md (per `readme-content-discipline.md` — link to ROADMAP for AI generation Phase 2)
3. **Phase 2 trigger:** ≥30 paying tenants AND revenue ≥$400/mo → spawn Phase 2 wave plan với fresh ADR (Alternative B vs C re-evaluation).

### Rollback plan

ADR-026 rollback = re-enable `ai.fullai.enabled=true` + provision compute path (B hoặc C). Trigger: post-Phase-1 beta feedback chỉ ra template path không đủ differentiation AND revenue covers budget. Decision rollback = new ADR superseding 026.

### Feature flags

- `ai.fullai.enabled` (default `false` Phase 1) — gates `FullAIRoute.execute()` in `PlanExecutor`
- `ai.provider` — config key chuẩn bị cho Phase 2 ("ollama" | "openai" | "bedrock") — Phase 1 ignored
- `ai.template.tone-variants-enabled` (default `true`) — preserve tone × audience combinatorics smart UX

### Monitoring / success criteria

Phase 1 BETA success cho ADR-026 path:
- ≥80% beta tenants accept generated theme (template + brand colors) trong wizard
- 0 customer complaint "missing AI generation" trong first 30 ngày
- Quality audit /100 ≥80 (per `release-1-plan-2026.md` Phase 1 trigger gate)
- 0 PDPL incident liên quan AI provider data flow (vacuously true cho template-only)

Re-evaluate Phase 2 trigger gate sau 30 tenants paying.

---

## References

- ADR-025 — AWS-only Deploy Phase 1 BETA Free Tier Singapore (parallel infra decision)
- ADR-015 — AWS Agent Plugins Evaluation (DEFER Q3 2026 — same defer-pattern)
- `.claude/rules/ai-branding-guidelines.md` §1.1 — STATIC/TEMPLATE/FULL_AI taxonomy
- `.claude/rules/ai-branding-guidelines.md` §11.4 — Migration test checklist (governs Phase 2 re-enable)
- `.claude/rules/release-deploy-standard.md` §3.1 — Phase 1 BETA artifact checklist (no AI smoke test required)
- `.claude/rules/business-logic-review.md` — PDPL compliance review for AI provider data flow
- GAP-006 — Gemma 4 9B migration (deferred Phase 2)
- GAP-225 — Scaffold-as-DONE governance closure umbrella (Phase 2-4 future scope confirmed)
- GAP-228 — ML classifier scoring (deferred Phase 2)
- GAP-411 — AWS Architecture B sizing matrix (cost context)
- GAP-412 — AWS Activate Founders Pack ($1k credit, Yr1 cover)
- `documents/05-guides/deploy/aws-architecture-sizing-matrix.md` — phase progression matrix
- `documents/03-planning/roadmap/release-1-plan-2026.md` — Phase 1 BETA scope chốt 2026-05-06
- `documents/03-planning/roadmap/phase-2-eks-migration.md` — Phase 2 infra trigger (orthogonal but related)
- `feedback_release_1_first_session_priority.md` — MVP-first philosophy

---

## Log

- **2026-05-07** — ACCEPTED. Initial proposal + acceptance same day per solo-dev mode (CLAUDE.md decision context 2026-05-06). Triggered by Wave 37 Layer 5 cost analysis + user-confirmed defer Ollama scope. Closes GAP-416 acceptance criterion. Cross-impact updates to GAP-006/225/228 tracked separately (each gap file Log entry). Phase 1 BETA AI Branding scope finalized = STATIC + TEMPLATE only.
