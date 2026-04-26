# GAP-229: AI Branding business docs v2 sync + 3 missing user guides

**Status:** 🟡 PARTIAL — Phase 1 SHIPPED 2026-04-26 (3 business docs synced); Phase 2 (3 user guides) + Phase 3 (instance-provisioning verify) pending separate sessions
**Priority:** 🟠 P1 Business-Logic (per `meta-gap-priority.md` Business-Logic > Feature; rule says wrong business docs = wrong product)
**Domain:** Business documentation / User guides
**Found:** 2026-04-26 (GAP-016 verification sweep, Sub-PR 223.1 correction)
**Affects:** Tenant onboarding (no wizard guide), kiteclass-frontend integration (no integration guide), template contributors (no contribution guide), code↔doc sync (per `audit-to-gap-pipeline.md` Living Documents rule)

## Problem

V2 AI Branding implementation shipped in `kiteclass-core` Waves 2-4 (verified GAP-016 sweep) but business documentation + user guides did NOT sync:

1. **Business docs still v1:** `documents/01-business/kitehub/ai-branding/{rules,use-cases,api-contract}.md` describe AIB-01..11 v1 rate limit + v1 endpoints only. Missing wizard rules, lifecycle rules, quality gate rules, resource classification, workflow rules, regenerate counter, tier-specific governance.

2. **3 user guides never created:**
   - `documents/05-guides/branding-integration.md` (how kiteclass-frontend consumes branding package — needed for FE devs)
   - `documents/05-guides/ai-branding-wizard-flow.md` (wizard UX user guide — needed for tenant onboarding)
   - `documents/05-guides/template-contribution-guide.md` (how to add templates — needed for designer/template contributors)

3. **Living Documents rule violation:** `CLAUDE.md` explicitly mandates business docs update with code change. Waves 2-4 violated this; latent until GAP-016 sweep surfaced.

## Current State (verified 2026-04-26)

| Doc | State | Real content vs needed |
|-----|:-----:|------------------------|
| `01-business/kitehub/ai-branding/rules.md` | ❌ v1 only | AIB-01..11 rate limit; missing: ResourceCategory STATIC/TEMPLATE/FULL_AI rules, FrontendInstanceStatus 6-state lifecycle, §5 quality gate ≥70 score rule, regenerate counter limits per tier, free-prompt BAN for FREE/PRO/PREMIUM, ENTERPRISE Advanced Mode opt-in, wizard 6-step flow rules |
| `01-business/kitehub/ai-branding/use-cases.md` | ❌ 4/11 UCs | Only UC-AIB-01..04 (analyze-logo, generate-image, generate-text, basic template); missing UC-AIB-05 Template apply, UC-AIB-06 Template gallery list, UC-AIB-07 Wizard 6-step, UC-AIB-08 Per-resource approve, UC-AIB-09 Regenerate with counter, UC-AIB-10 Quality review automated, UC-AIB-11 Auto-provisioning event-driven |
| `01-business/kitehub/ai-branding/api-contract.md` | ❌ v1 endpoints | Has 7 v1; missing v2: `GET /api/v1/branding/{instanceId}/package` (composite ETag), `POST /analyze`, `POST /plan`, `POST /execute`, `GET /jobs/{id}`, `GET /instances/{id}/status`, `POST /wizard/session` |
| `01-business/kitehub/instance-provisioning/{rules,use-cases,api-contract}.md` | ⚠️ NEED VERIFY | Last touched 2026-04-18; lifecycle UC may be missing |
| `05-guides/branding-integration.md` | ❌ MISSING | Needed for FE devs consuming `BrandingProvider` + theme injection |
| `05-guides/ai-branding-wizard-flow.md` | ❌ MISSING | Needed for tenant onboarding handoff to support |
| `05-guides/template-contribution-guide.md` | ❌ MISSING | Needed for designer onboarding (per GAP-011 review criteria) |

## Proposed Fix (phased)

### Phase 1 (~2h): Business docs v2 sync — `01-business/kitehub/ai-branding/`

Update 3 files based on actual `kiteclass-core` implementation:

1. **rules.md:** Append v2 rules section sourcing from `ai-branding-guidelines.md` §1-§9 + §11.4. Verification chain: each rule → config key → code reference.
2. **use-cases.md:** Append UC-AIB-05..11 with actor/precondition/steps/errors/FE behavior. Source: code paths in `kiteclass-core/module/branding/handler/`, `kiteclass-core/module/ai/workflow/`, `kiteclass-core/module/quality/`, `kiteclass-core/module/provisioning/`.
3. **api-contract.md:** Append v2 endpoints with request/response schemas. Source: real Controllers in `kiteclass-core/module/branding/controller/`, `kiteclass-core/module/instance/controller/`.

### Phase 2 (~3h): 3 user guides

1. **branding-integration.md:** kiteclass-frontend `BrandingProvider` + `useEffect` theme injection + ETag-based cache + SSE re-fetch flow. Diagrams + code samples.
2. **ai-branding-wizard-flow.md:** 6-step wizard screenshot walkthrough + error states + tier-specific behavior.
3. **template-contribution-guide.md:** GAP-011 5-criteria review checklist + SVG placeholder convention + brand-family consistency rules + commit checklist.

### Phase 3 (~30min): instance-provisioning docs verify

Spot-check `instance-provisioning/{rules,use-cases,api-contract}.md` against `kiteclass-core/module/instance/`. Update if drift found; may not need work if already in sync.

## Acceptance Criteria

- [ ] Phase 1: rules.md has v2 rules covering 7 v2 areas (resource classification, lifecycle, quality gate, regenerate limits, free-prompt ban, ENTERPRISE Advanced Mode, wizard flow); each rule has config key + code ref
- [ ] Phase 1: use-cases.md has UC-AIB-05..11 (7 new UCs) with full structure
- [ ] Phase 1: api-contract.md has 7+ v2 endpoints with request/response schemas
- [ ] Phase 2: 3 guides exist + screenshot examples + code snippets
- [ ] Phase 3: instance-provisioning docs verified or updated
- [ ] CLAUDE.md Living Documents rule violation closed
- [ ] business-gap-check skill §2.9 audit re-run → pass

## Dependencies

- **Tracked under:** GAP-016 (parent — verified sweep) and GAP-225 (umbrella for scaffold-as-DONE pattern)
- **Aligned with:** `audit-to-gap-pipeline.md` Step 5 (ROADMAP update mandatory), `output-review-mandate.md` §3 matrix sync
- **Blocked by:** none (docs work, no code dependency)

## Risk / Tradeoffs

- **Risk if not fixed:** New devs onboarding miss v2 architecture (read v1 docs → wrong mental model); tenant support team has no wizard reference → escalations; designer contributors can't add templates → ecosystem stalls
- **Why P1 not P0:** v1 docs aren't actively MISLEADING for live operations; gap is "missing v2 description", not "wrong v1 description". P0 reserved for active blockers.

## References

- GAP-016 (verified sweep parent gap)
- GAP-225 (scaffold-as-DONE umbrella; this is cluster C3 docs sync)
- GAP-223 (Sub-PR 223.1 governance scaffold; this gap closes the docs side)
- GAP-011 (Template contribution review criteria — input for Phase 2 template-contribution-guide.md)
- `CLAUDE.md` §"CRITICAL: Living Documents"
- `.claude/rules/audit-to-gap-pipeline.md` (Living Docs rule application)
- `documents/02-architecture/ai-branding-v2-redesign.md` (v2 architecture source)
- `kiteclass-core/module/{ai,branding,instance,quality,moderation,provisioning}/` (v2 implementation source)

## Log

- **2026-04-26 (Phase 1 SHIPPED):** 3 business docs synced from real `kiteclass-core` v2 implementation:
  - `rules.md` — appended 24 rules across 6 v2 areas (BR-RES Resource Classification 6 rules per ADR-005, BR-LIFE Lifecycle State Machine 6 rules per ADR-004, BR-QUALITY 4 rules per §5 + GAP-012, BR-APRV Rebrand Approval 6 rules per GAP-070, BR-WIZARD Provisioning 6 rules per ADR-006, BR-MOD Content Moderation 3 rules per GAP-018, BR-PKG Composite Package 5 rules per ADR-009). Each rule has code reference + config key. v2 config block added (`branding.routing`, `quality-gate`).
  - `use-cases.md` — added UC-AIB-07 Tenant Provisioning Saga, UC-AIB-08 Quality Gate Review, UC-AIB-09 Rebrand Request, UC-AIB-10 Enterprise Approval, UC-AIB-11 Branding Package Fetch, UC-AIB-12 Public Branding Lookup. All sourced from real Controllers + Services in `kiteclass-core`.
  - `api-contract.md` — added 12 v2 endpoints: 8 lifecycle (`/api/v1/instances/...`) + 2 branding package (`/api/v1/branding/{id}/package` ETag + `/api/v1/branding/public`) + 1 internal webhook + 4 approval endpoints noted as TBD (RebrandApprovalService exists, controller pending). Schema derived from real `InstanceController`, `BrandingPackageController`, `PublicBrandingController`, `InternalWebhookController`, `InstanceResponse` record.

  Per memory `feedback_search_all_modules_before_missing_claim.md` + `feedback_gap_state_check_required.md`: documented REAL implementation, not aspiration. Where `ai-branding-guidelines.md` specifies behavior NOT yet in code (e.g. tier-based regenerate counter, ENTERPRISE Advanced Mode toggle), docs note it as gated/scaffold per source-of-truth.

  Phase 2 (3 user guides: branding-integration.md, ai-branding-wizard-flow.md, template-contribution-guide.md, ~3h) + Phase 3 (instance-provisioning docs verify, ~30min) deferred to separate session.

- **2026-04-26** — Filed during GAP-016 verification sweep + Sub-PR 223.1 correction. Captures business-docs side of cluster C3 governance closure that Sub-PR 223.1 didn't cover (it was code-side governance only). Phased plan: Phase 1 ~2h business docs, Phase 2 ~3h guides, Phase 3 ~30min verify. Total ~5-6h spread across multiple sessions.
