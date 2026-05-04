# branding — AI Branding Wizard & Integration

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Operator-facing docs cho AI Branding feature: wizard flow, FE consumption, support runbook. Audience: onboarding team, support, FE engineers.

---

## Directory Map

| File | Purpose |
|------|---------|
| `ai-branding-wizard-flow.md` | 6-step wizard + saga handoff + tier behavior + support runbook (GAP-229 Phase 2.2) |
| `branding-integration.md` | FE consumption của `/api/v1/branding/{public,/{id}/package}` — CSS-vars, ETag flow, BrandingProvider (GAP-229 Phase 2.1) |

---

## File Placement Rules

- ✅ **Belongs here:** operator-facing branding docs (how wizard works, how FE consumes, how to debug branding issues)
- ❌ **Does NOT belong here:**
  - Architecture decisions: [`../../02-architecture/ai-branding-v2-redesign.md`](../../02-architecture/ai-branding-v2-redesign.md) + [`../../02-architecture/ai-branding-design-patterns.md`](../../02-architecture/ai-branding-design-patterns.md)
  - Business rules: [`../../01-business/kitehub/branding/rules.md`](../../01-business/kitehub/branding/rules.md)
  - Template contribution: [`../contributing/template-contribution-guide.md`](../contributing/template-contribution-guide.md)
  - Branding governance rules: `.claude/rules/ai-branding-guidelines.md`

---

## Related

- Tenant onboarding (branding wizard fits in here): [`../tenant-lifecycle/tenant-onboarding-checklist.md`](../tenant-lifecycle/tenant-onboarding-checklist.md)
- Branding service code: `kitehub/kitehub-branding/`
- Quality gate: `.claude/skills/quality/ai-branding-quality-gate/SKILL.md`

---

## Archive Policy

Move sang `documents/07-archived/branding-YYYY/` khi AI provider stack thay đổi major (vd Ollama → Bedrock complete swap).
