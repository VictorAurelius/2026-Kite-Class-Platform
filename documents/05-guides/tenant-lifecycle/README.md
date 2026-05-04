# tenant-lifecycle — School Onboarding & Off-boarding

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

Procedures cho tenant lifecycle: onboarding (3-day target), off-boarding (data export + retention + deletion). Audience: customer success, support, ops.

---

## Directory Map

| File | Purpose |
|------|---------|
| `tenant-onboarding-checklist.md` | End-to-end school onboarding (3-day target) (GAP-102) |
| `tenant-off-boarding-runbook.md` | Off-boarding workflow (export → retention → deletion per PDPL) |

---

## File Placement Rules

- ✅ **Belongs here:** customer-facing tenant lifecycle (onboard, scale, churn, off-board)
- ❌ **Does NOT belong here:** branding setup (xem [`../branding/`](../branding/)), provisioning infrastructure (xem KiteHub `kitehub-provisioning` service docs)

---

## Related

- AI branding wizard (part of onboarding): [`../branding/ai-branding-wizard-flow.md`](../branding/ai-branding-wizard-flow.md)
- Subscription state machine: KiteHub `kitehub-subscription` source
- Data retention compliance: [`../infrastructure/SECRET-MANAGEMENT.md`](../infrastructure/SECRET-MANAGEMENT.md) + PDPL rules trong `documents/01-business/kitehub/data-privacy/rules.md`

---

## Archive Policy

Move sang `documents/07-archived/tenant-lifecycle-YYYY/` khi target onboarding time thay đổi major (vd 3 ngày → 1 ngày tự động).
