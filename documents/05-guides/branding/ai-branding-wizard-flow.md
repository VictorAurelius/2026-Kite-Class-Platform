# AI Branding Wizard Flow

**Audience:** Tenant onboarding, customer support, product managers
**Last verified:** 2026-04-26 (GAP-229 Phase 2.2)
**Related:** [`ai-branding-guidelines.md`](../../.claude/rules/ai-branding-guidelines.md), [`api-contract.md UC-AIB-07`](../01-business/kitehub/ai-branding/use-cases.md), `TenantProvisioningSaga`

---

## What the wizard does

When a tenant signs up for KiteClass, they go through a **6-step wizard** that collects branding inputs. The wizard's output (`audience` + `tone` + uploaded logo) becomes a `TenantCreatedEvent`, which `TenantProvisioningSaga` consumes to provision a fully-branded instance end-to-end.

End state: tenant lands on a `DEPLOYED` instance with logo, colors, hero banner, and theme variables already applied — no manual branding step required after signup.

---

## The 6 steps

```
┌─────────┐    ┌──────────┐    ┌──────────┐    ┌─────┐    ┌──────────┐    ┌──────────┐
│Welcome  │ -> │ Logo     │ -> │ Audience │ -> │Tone │ -> │ Template │ -> │ Preview  │
│   1     │    │ upload   │    │   3      │    │  4  │    │   5      │    │ approve  │
│         │    │   2      │    │          │    │     │    │ (6 prev) │    │   6      │
└─────────┘    └──────────┘    └──────────┘    └─────┘    └──────────┘    └──────────┘
                  optional                                                       │
                                                                                 ↓
                                                              POST TenantCreatedEvent
                                                                       (audience + tone)
                                                                                 │
                                                                                 ↓
                                                              TenantProvisioningSaga
                                                              (NOT_STARTED → … → DEPLOYED)
```

### Step 1 — Welcome + info

What user sees: brief intro to AI Branding ("we'll generate your school's brand identity in ~2 minutes"), data-use notice, link to wizard FAQ.
Inputs collected: none (informational).
Backend impact: none yet.

### Step 2 — Upload logo (optional)

User uploads PNG/JPG/SVG up to 2 MB.

If skipped: system uses generated logo from template choice in Step 5.
If uploaded: file analyzed locally (Ollama vision) to extract palette + style signals; stored in MinIO; URL persisted with the eventual `Branding` record.

**Privacy note:** Per ai-branding-guidelines §9, logo analysis runs against local Ollama. Logos are NOT sent to external AI APIs unless tenant has explicit ENTERPRISE consent for OpenAI fallback.

### Step 3 — Choose audience

Dropdown values: `K-12`, `center` (private learning center), `university`.

Why constrained: per `ai-branding-guidelines.md §2.1`, free-form prompts are BANNED for FREE/PRO/PREMIUM. The wizard collects categorical inputs that the backend feeds into a fixed prompt template — tenants don't write AI prompts themselves.

Code mapping: `AnalysisRequest.audience` → consumed by `AnalyzerService.analyze()` to bias palette/composition decisions.

### Step 4 — Choose tone

Dropdown values: `friendly`, `professional`, `energetic`.

Tone shapes:
- Color saturation (`energetic` → higher saturation, `professional` → muted)
- Typography (`friendly` → rounded, `professional` → serif)
- Hero banner composition (template `PickTemplateStep` factors tone into selection)

Code mapping: `AnalysisRequest.tone` → `PlannerService.plan()`.

### Step 5 — Choose template (6 previews)

System pre-renders 6 template options based on (`audience`, `tone`) selections. User picks one.

**Why 6 previews not free-form:** ai-branding-guidelines.md §2.2 — visual choice instead of text descriptions. Tenants have no AI literacy expectations.

Each preview shows:
- Hero banner mock (real composition with placeholder text)
- Color swatches (3 colors)
- Sample headline rendered

Picking auto-advances to Step 6.

### Step 6 — Preview + approve per resource

Final preview screen shows ALL generated assets:

- Logo (uploaded or generated)
- Hero banner
- Color theme (5 swatches)
- Typography sample
- Sample dashboard screenshot with theme applied

User can:
- **Approve all** → proceed to provisioning
- **Regenerate one** → marks that resource for regeneration (counts against tier counter, see "Tier-specific behavior")
- **Go back** → step back to change inputs

Per ai-branding-guidelines §4.2, **per-resource approve is MANDATORY** — never auto-deploy. User explicitly approves the bundle before commit.

---

## Backend handoff

When user clicks "Approve all" in Step 6, the FE sends a `TenantCreatedEvent` to KiteHub (via signup endpoint). Event shape:

```json
{
  "tenantId": "trial-uuid-from-kitehub",
  "slug": "abc-school",
  "audience": "K-12",
  "tone": "friendly",
  "uploadedLogoUrl": "minio://branding/abc/logo.svg",
  "selectedTemplateId": "edu-modern-3"
}
```

KiteHub publishes this onto RabbitMQ (`provisioning.tenant.created`); kiteclass-core's `TenantProvisioningSaga.provision()` consumes it and runs the lifecycle:

| Step in saga | FrontendInstanceStatus transition | Visible to user |
|--------------|-----------------------------------|-----------------|
| `lifecycle.initiate` | NOT_STARTED → INITIALIZING | "Setting up your instance…" |
| `provisionInfrastructure` | (placeholder) | Same screen, progress dots |
| `lifecycle.markInfrastructureReady` | INITIALIZING → GENERATING | "Generating your branding…" |
| Pipeline: Analyzer → Planner → PlanExecutor (4 Steps) | (still GENERATING) | Same |
| `QualityReviewStep` | (still GENERATING) | "Quality checks passing…" |
| `PublishPackageStep` → `markBrandingCompleted` | GENERATING → DEPLOYED | "All set! Redirecting to your dashboard." |

Total wall-clock: typically 30–90 seconds for FREE/PRO tier (template path), up to 2–3 minutes if FULL_AI fallback engaged.

End state: user lands on their tenant subdomain `https://abc-school.kitehub.me` with full branding applied.

---

## Tier-specific behavior

| Tier | Wizard differences | Regenerate budget |
|------|--------------------|-------------------|
| FREE | Standard wizard; templates limited to "core" set (~30) | 3 regenerations / session |
| PRO | Standard wizard; full template gallery | 10 regenerations / session |
| PREMIUM | Standard + extra template variants + custom palette swatch | 30 regenerations / session |
| ENTERPRISE | Optional Advanced Mode toggle (free-form prompt) — disabled by default; enable via `ai.enterprise.advancedModeEnabled` flag in admin settings | Unlimited |

> **Status of regenerate counter:** scaffold (per `ai-branding-guidelines.md §4.3`). Tier-specific counter enforcement is on the wizard frontend; backend rate-limit (`AIB-01`–`04` daily limit) is the hard ceiling. Regenerate counter UX must remain visible per §4.3.

> **Status of ENTERPRISE Advanced Mode:** flag exists in spec but not yet wired into FE/backend (per `ai-branding-guidelines.md §11.4` migration checklist). Treat as roadmap item.

---

## Quality gate gate-keeping

After Step 6 approval, the pipeline runs `InstanceQualityReviewer.review()` (UC-AIB-08). 5 checks score the generated branding 0–100. Default pass-threshold: 70.

- **Score ≥ 70** → `markBrandingCompleted` → DEPLOYED.
- **Score < 70** → `markFailed("score X < threshold 70")` → FAILED state. User sees an apology screen + "Try different inputs" CTA. Saga compensation kicks in.

User-visible states map to messages:

| FrontendInstanceStatus | Wizard screen text |
|------------------------|--------------------|
| NOT_STARTED → INITIALIZING | "Setting up your school…" |
| INITIALIZING → GENERATING | "Designing your brand…" |
| GENERATING (during quality check) | "Final touches…" |
| DEPLOYED | "Welcome! Redirecting…" → /dashboard |
| FAILED | "Something went wrong. Our team is looking. Try again with different inputs?" + retry CTA |

---

## Error states + recovery

| Code path | User experience | Support recovery |
|-----------|-----------------|------------------|
| Step 2 logo upload > 2 MB | Inline error: "Logo must be under 2 MB" | None — user retries |
| Step 2 logo MIME unsupported | Inline error with accepted formats | None — user retries |
| Step 5 zero templates match (`audience`+`tone` combo) | Show fallback "Use AI-generated theme" CTA → engages FULL_AI path | Investigate why template gallery missing combo (designer task) |
| Step 6 "Approve all" → backend timeout | Show retry button; idempotency-key on submit prevents double-charge | If timeout persists, check `TenantProvisioningSaga` logs by tenantId |
| Saga `StepException` mid-pipeline | UI lands on FAILED screen with "Retry" button | `POST /api/v1/instances/{id}/retry` (max 3 retries per BR-LIFE-005) |
| Quality gate fails (score < 70) | FAILED screen | Same retry path; may need template fallback |
| `MAX_RETRIES = 3` exhausted | "Manual intervention needed" CTA | Support runs `POST /api/v1/instances/{id}/retry` with admin override OR escalates to engineering |

---

## Customer support runbook

Common questions:

**"My branding looks wrong" (post-signup)**
1. Check `GET /api/v1/instances/{id}` — current status + brandingVersion
2. If DEPLOYED, customer can rebrand via `POST /api/v1/instances/{id}/rebrand` (UC-AIB-09; FREE/PRO/PREMIUM tier)
3. ENTERPRISE rebrand requires 2-person approval (UC-AIB-10) — escalate to admin

**"My signup is stuck on 'Designing your brand…'"**
1. Status will be GENERATING — query `GET /api/v1/instances/{id}`
2. If GENERATING > 5 min: query `kiteclass-core` logs by tenantId, look for StepException
3. If FAILED with `retryCount < 3`: trigger `POST /api/v1/instances/{id}/retry`
4. If `retryCount = 3`: investigate root cause; manual override via DB OR coordinate with engineering

**"Can I redo the wizard?"**
- Pre-DEPLOYED: yes, page back through wizard
- Post-DEPLOYED: use rebrand flow (UC-AIB-09 / UC-AIB-10), NOT re-running wizard

**"Why is my logo different from what I uploaded?"**
- Logo placement check (BR-QUALITY-002 / `LogoPlacementQualityCheck`) may have rejected oversized/cropped uploads. Re-upload with proper dimensions (recommend 512×512 SVG).

---

## Status (per `ai-branding-guidelines.md` §11.4)

The wizard backend (`TenantProvisioningSaga` + Steps + Quality gate) shipped Wave 4. Real WCAG / visual regression / ML scoring deferred to GAP-226/227/228 (Wave 8+). Until those land, quality scoring is scaffold-only — checks return fixed scores per implementation. `ai-branding-quality-gate` skill (`/ai-branding-quality-gate`) is the manual checklist for AI behavior changes.

---

## Related

- Implementation: `kiteclass/kiteclass-core/src/main/java/com/kiteclass/core/module/{provisioning,ai,quality}/`
- Architecture: `documents/02-architecture/ai-branding-v2-redesign.md`
- Migration test checklist: `.claude/rules/ai-branding-guidelines.md` §11.4
- Quality skill: `.claude/skills/quality/ai-branding-quality-gate/SKILL.md`
- API contract: `documents/01-business/kitehub/ai-branding/api-contract.md` (UC-AIB-07..12)
