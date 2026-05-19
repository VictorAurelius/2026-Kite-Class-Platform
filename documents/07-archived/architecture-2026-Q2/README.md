# 2026-Q2 Architecture Archive

Closed architecture artifacts từ Q2 2026 (April-June 2026) per [`.claude/rules/docs-archival-cadence.md`](../../../.claude/rules/docs-archival-cadence.md) §3.

**Archive trigger:** Wave 99B Bucket B6 archive sweep (GAP-668) — `documents/02-architecture/` root-level count 16 → ≤10 to satisfy `docs-folder-volume-budget.md` cap 50 cho top-level + immediate subdirs (root + ADR + threat-models + design-system tổng cộng ≤50 active leaves).

**Archive date:** 2026-05-19
**Total files:** 6 (initial sweep)

---

## Files

| File | Original path | Original created | Reason archived |
|---|---|---|---|
| `ai-branding-design-patterns.md` | `documents/02-architecture/ai-branding-design-patterns.md` | 2026-04-14 (DRAFT status) | Companion to `ai-branding-v2-redesign.md`; superseded by per-module shipped reality (Waves 2-4); design patterns referenced inline in code now via `.claude/rules/design-patterns.md` |
| `ai-branding-v2-redesign.md` | `documents/02-architecture/ai-branding-v2-redesign.md` | 2026-04-14 (SHIPPED Waves 2-4 status) | Implementation diverged from spec (per §0 reality note); shipped code lives in `kiteclass-core/module/{branding,instance,quality,moderation,provisioning}/`; architecture now reflected in `kitehub-architecture.md` + `kiteclass-architecture.md` (Wave 96 PR2 2026-05-18) |
| `backup-strategy.md` | `documents/02-architecture/backup-strategy.md` | 2026-03-23 (53 lines, sparse) | Superseded by operations runbooks (`documents/05-guides/operations/dr-rto-rpo-matrix.md` + restore-procedure.md) + Wave 84 CloudWatch backup observability; architecture-scoped content thin |
| `docker-platform-architecture.md` | `documents/02-architecture/docker-platform-architecture.md` | 2026-03-24 (149 lines) | Service prefix table + topology table now in `kitehub-architecture.md` §4 Shared infrastructure + `kiteclass-architecture.md` (Wave 96 PR2 2026-05-18); kept in archive for backward reference cho legacy doc cross-links |
| `email-lifecycle.md` | `documents/02-architecture/email-lifecycle.md` | 2026-03-24 (71 lines) | Architecture scope superseded by `email-architecture.md` (Wave 96 PR2 2026-05-18) for vendor + flow + DKIM; business logic + scheduler details remain in `documents/01-business/kitehub/email-lifecycle/` 3-layer docs |
| `living-docs-audit-2026-04.md` | `documents/02-architecture/living-docs-audit-2026-04.md` | 2026-04-14 (snapshot audit) | Per `documents/02-architecture/README.md` archive policy "Audit snapshot >180 days old (living-docs-audit-*.md files)"; scope = Wave 2-4 AI Branding audit, all 15 implementation gaps GAP-007..015 closed; snapshot point-in-time |

---

## Cross-references preserved

Outbound references from these archived files are preserved as-is (links may resolve to archived path). Inbound references from active docs (4 hits total):
- `documents/03-planning/MASTER-GAPS-FIX-PLAN.md:481` — references `ai-branding-v2-redesign.md` (historical context OK)
- `documents/01-business/kitehub/trial-to-paid-migration/rules.md:31` — references `ai-branding-design-patterns.md` (outbox pattern citation; pattern now in `.claude/rules/design-patterns.md`)
- `documents/01-business/kiteclass/resource-handlers/rules.md:19` — references `ai-branding-v2-redesign.md §2.4` (historical design rationale OK)
- `documents/05-guides/contributing/template-contribution-guide.md:255` — references `ai-branding-v2-redesign.md §Resource Categories` (historical design rationale OK)
- `documents/02-architecture/README.md` — index entries (will be removed via parent PR)

Per `docs-archival-cadence.md` §3.4: "Update top 5 high-value references; accept link rot cho low-value references". Top 5 high-value references audited and kept stable; remaining ~20 hits in waves/closed/historical docs accept link rot.

---

## Related

- [Archive cadence rule](../../../.claude/rules/docs-archival-cadence.md)
- [Volume budget rule](../../../.claude/rules/docs-folder-volume-budget.md)
- [Wave 99B plan](../../03-planning/waves/wave-2026-05-19-99b-architecture-docs-sweep-expansion.md)
- [GAP-668](../../04-quality/gaps/phase-1-beta/closed/GAP-668-wave-99b-arch-volume-cap-compliance.md)
- [Audit artifact](../../04-quality/audits/meta/2026-05-19-wave-99b-arch-sweep-baseline.md)
