# GAP-1098: Tier-name drift còn sót ngoài 2 file đã sweep — PRO/STARTER/PROFESSIONAL stale vs canonical FREE/BASIC/PREMIUM/ENTERPRISE

**Status:** 🟢 DONE 2026-06-11 — Wave ui-kits-100 Bucket E sweep 23 file (8 listed + 15 sister-site); code-contract docs EXEMPT → GAP-1228 (code drift mới phát hiện)
**Priority:** 🟢 P3
**Domain:** Mixed
**Found:** 2026-06-09 (tier-enforcement wave — cross-flow sweep DEFER)
**Affects:** ~8 file docs + skills + BRD dùng tên tier stale (PRO / STARTER / PROFESSIONAL / PRO_PLUS) khác canonical `PricingTier.java` (FREE / BASIC / PREMIUM / ENTERPRISE)

## Problem

Canonical tier names trong code = `PricingTier.java` (`kitehub-platform/.../domain/enums/PricingTier.java:16-31`): `FREE` / `BASIC` / `PREMIUM` / `ENTERPRISE`. Wave tier-enforcement đã sweep 2 file đổi tên tier stale (PRO/STARTER → BASIC/PREMIUM khớp canonical): `multi-tenant-architecture.md` + `ai-branding-guidelines.md`.

Nhưng grep broader codebase còn ~8 file dùng tên tier stale (PRO / STARTER / PROFESSIONAL / PRO_PLUS) → drift class chưa đóng hết:

- `.claude/skills/quality/ai-branding-quality-gate/SKILL.md` (line ~81/88/91/139 — tier names + regen quota)
- `.claude/skills/quality/cross-app-consistency.md` (line ~31)
- `.claude/skills/quality/security-audit/reference/scoring-guide.md` (line ~84)
- `.claude/skills/backend/backend-standards.md` (line ~283 — `TierLevel` STARTER/PROFESSIONAL)
- `kitehub/kitehub-frontend/.../frontend-standards.md` (line ~258) — hoặc `.claude/skills/frontend/frontend-standards.md` (verify path khi sweep)
- `documents/00-brd/billing-terms.md` (line ~141)
- `documents/02-architecture/zalo-integration-design.md` (line ~98)

Hệ quả: doc/skill reference tier sai tên → reader (dev + Claude) hiểu nhầm tier scheme; risk hardcode tên sai khi code mới; audit skill chấm sai khi reference tier stale. Low severity (docs/skills, không phải runtime), nhưng để lâu = drift decay.

## Root Cause

Tier scheme đổi tên trong code (`PricingTier` canonical FREE/BASIC/PREMIUM/ENTERPRISE) nhưng docs + skills + BRD viết trước đó dùng tên cũ (PRO/STARTER/PROFESSIONAL) chưa được sweep đồng bộ. Wave tier-enforcement mới sweep 2 file → cross-flow sweep chưa hoàn tất per `cross-flow-bug-class-sweep.md`.

## Proposed Fix

1. Sweep ~8 file đổi tên tier stale → canonical, verify TỪNG chỗ value-preserving (vd `PRO` regen=10/day → map đúng tier canonical tương ứng, không đổi giá trị số).
2. Phân biệt **tier name** (sweep) vs **proper noun** (giữ): folder UI kit `ui_kits/kitehub-pro-v2/` là tên kit, KHÔNG phải tier → EXEMPT.

### EXEMPT (KHÔNG sweep)

- `documents/08-thesis/**` — intentional thesis naming (per `thesis-content-standard.md`)
- `.claude/rules/business-logic-review.md:58` — illustrative fictional example (cố ý)
- `ui_kits/kitehub-pro-v2/**` — proper noun (tên UI kit, không phải tier enum)

## Acceptance Criteria

- [x] 23 file sweep (8 listed + 15 sister-site phát hiện qua sweep) → canonical FREE/BASIC/PREMIUM/ENTERPRISE
- [x] Value-preserving verified (quota 3/10/30, GB caps, SLA % giữ nguyên — chỉ rename label)
- [x] grep = 0 ngoài EXEMPT mở rộng: thesis + illustrative + proper-noun (kitehub-pro-v2/kiteclass-pro-v2/iubenda Pro) + historical (ADR-036, persona-reviews dated) + brand_personality PROFESSIONAL (không phải tier) + CODE-CONTRACT docs (api-contract/branding-wizard rules/ai-agent-workflow rules/03-branding.md — phản ánh code thật, đổi cùng GAP-1228) + wizard kit README (Bucket D)
- [x] Skills audit reference đúng tier canonical (ai-branding-quality-gate + cross-app-consistency + security scoring-guide + backend-standards PricingTier + frontend-standards)

## Related

- Discovered in: tier-enforcement wave 2026-06-09 (cross-flow sweep DEFER — broader grep ngoài 2 file đã làm)
- Drift sweep 2 file đã làm: GAP-1020 (branding RLS GUC + tier header trust) cluster
- ADR-039 SUB-22 — tier propagation matrix (canonical tier names)
- Sweep methodology: `cross-flow-bug-class-sweep.md` §1

## Log

- **2026-06-11 (DONE — Wave ui-kits-100 Bucket E):** Sweep 23 file: 8 listed (trừ frontend-standards fix tại đây luôn) + 15 sister-site phát hiện khi verify (pricing-model resolve naming TODO, personas-catalog, nfr-catalog, terms-of-service, P1/P2 persona-criteria + _TEMPLATE, dossier 01/05/08/14/prompts, compliance-control-map, storage/report-card/rebrand-approval/ai-branding rules.md, ai-external-observability-plan, story-v2 README). Mapping value-preserving: PRO→BASIC (doc 4-tier FREE/PRO/...) hoặc PRO→PREMIUM (doc 5-name mess có cả BASIC lẫn PRO). Cross-check code phát hiện drift CODE-layer (RegenerateQuotaService case "PRO" vs JWT claim "BASIC" → BASIC tenant quota sai) → file GAP-1228 P1; code-contract docs EXEMPT chuyển sang đó per Living Docs same-PR mandate.
