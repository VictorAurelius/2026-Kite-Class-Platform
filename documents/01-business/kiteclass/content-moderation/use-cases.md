# Content Moderation — Use Cases

### UC-MOD-01: Auto-approve clean AI output (happy path)
- **Actor:** AI branding pipeline (system — `actorUserId = null`)
- **Service:** `ContentModerationService.check(targetType, targetId, text, imageUrl)`
- **Steps:**
  1. Stage 1 keyword/NSFW scan runs
  2. Score < `nsfw-threshold − 0.2` → APPROVED
  3. AuditLog row written (`moderation.approved`)
  4. No `moderation_queue` row persisted (optimisation: only non-approvals are stored)
- **Postcondition:** Caller proceeds to DEPLOY the resource

### UC-MOD-02: Auto-reject unsafe AI output
- **Actor:** AI branding pipeline (system)
- **Precondition:** Stage 1 finds ≥ `nsfw-threshold` score (e.g. "nsfw violence" keyword hits)
- **Steps:**
  1. Stage 1 flags banned keywords
  2. Status REJECTED persisted to `moderation_queue` (for admin dashboard / audit UI)
  3. AuditLog row `moderation.rejected` written with `reason = banned-keyword: ...`
  4. Service returns REJECTED result
- **Caller (`PublishPackageStep`):**
  - If `moderation.stage2.auto-fallback-to-template = true` → re-run pipeline with TEMPLATE category (no AI call)
  - Else → mark instance FAILED
- **Postcondition:** Unsafe content never reaches DEPLOYED state

### UC-MOD-03: Borderline content needs human review
- **Actor:** AI branding pipeline (system) → admin (follow-up)
- **Precondition:** Stage 1 score lands in `[nsfw-threshold − 0.2, nsfw-threshold)` band
- **Steps:**
  1. Row persisted with status NEEDS_HUMAN_REVIEW
  2. AuditLog row `moderation.needs_human_review` written
  3. Instance stays in pre-DEPLOY state
  4. Admin adjudicates via admin UI (follow-up wave) → transitions row to APPROVED or REJECTED
  5. New AuditLog row written per decision
- **Postcondition:** No content deployed without an explicit decision

### UC-MOD-04: Moderation disabled (dev / CI)
- **Actor:** System during local / CI runs
- **Trigger:** `moderation.stage1.enabled = false`
- **Result:** Every call immediately returns APPROVED with reason `stage1.disabled`; no queue row; one audit row per call (preserves traceability)
- **Use:** Integration tests that don't want to assert moderation side-effects

### UC-MOD-05: Stage 2 template-only retry (caller-owned)
- **Actor:** `PublishPackageStep` after UC-MOD-02 REJECTED
- **Steps:**
  1. Discard AI output
  2. Re-invoke branding pipeline with resource-classification forced to TEMPLATE
  3. Run moderation on the new output (normally passes — templates are pre-curated)
  4. If still REJECTED → mark instance FAILED (no infinite loop)
- **Note:** The moderation service itself is stateless across runs; each `check()` is an independent evaluation

## Error / frontend behaviour

| Scenario | Backend result | Caller reaction | User-visible |
|----------|----------------|-----------------|--------------|
| Clean output | APPROVED | Continue → DEPLOY | Instance deploys normally |
| Unsafe output | REJECTED | Template fallback OR FAILED | Wizard shows "we adjusted your content to match safety rules" (fallback) or "we couldn't generate — try different inputs" (failed) |
| Borderline | NEEDS_HUMAN_REVIEW | Wait for admin | Wizard shows "your content is under review, check back in a few hours" |
| Stage 1 disabled | APPROVED | Continue | No visible difference |

## Log
- 2026-04-15 — Initial use cases (Wave 4 Sub-PR 4.1)
