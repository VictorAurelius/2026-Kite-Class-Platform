# 02-architecture/threat-models — STRIDE Threat Modeling

**Rules:** [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)
**Last Updated:** 2026-05-16

Threat models per feature/flow using STRIDE methodology — Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege. Mỗi flow user-facing có security implication PHẢI có 1 threat model document trước khi launch.

**Audience:** Architects, Security reviewers, Tech Leads.

Closes Wave 86 Cat 3 OWASP REST sweep P1 — A04 Insecure Design no documented threat modeling.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `_TEMPLATE.md` | STRIDE template | 1 |
| `YYYY-MM-DD-<topic>.md` | Per-flow threat model | N |

---

## File Placement Rules

- ✅ **Belongs here:**
  - STRIDE analysis per user-facing flow (auth, payment, upload, admin actions)
  - Threat mitigation mapping → code/config controls
  - Trust boundary diagrams (PlantUML or markdown)

- ❌ **Does NOT belong here:**
  - Implementation details (belongs to feature folder under business/architecture)
  - General security policy (belongs to `documents/01-business/.../rules.md`)
  - Pentest reports (belongs to `documents/04-quality/audits/security/`)

---

## STRIDE Reference

| Letter | Threat | Example | Mitigation Pattern |
|--------|--------|---------|--------------------|
| **S**poofing | Identity forgery | Stolen magic-link token | Token TTL + single-use + HMAC sign |
| **T**ampering | Data modification | CSV injection on import | Server-side validation + size limit |
| **R**epudiation | Deny action taken | "Tôi không xoá" | Immutable audit log (PDPL Art 11) |
| **I**nformation Disclosure | Data leak | Cross-tenant query leak | RLS + tenant_id filter + force-fail |
| **D**enial of Service | Service unavailable | ZIP bomb upload | Size cap + decompress limit + rate limit |
| **E**levation of Privilege | Gain unauthorized access | Role escalation via JWT tamper | JWT sign + role-guard server + RLS |

---

## Cadence

- **Pre-launch:** Every new user-facing flow gets threat model before merge to main
- **Quarterly review:** Re-audit existing models; refresh mitigation status
- **Incident-driven:** Post-mortem incidents → update relevant threat model

---

## Index

| Date | Topic | Status | Linked gaps |
|------|-------|--------|-------------|
| 2026-05-16 | [Auth flow — magic-link login](2026-05-16-auth-flow-magic-link.md) | complete | GAP-584 / GAP-582 |
| 2026-05-16 | [Bulk import — CSV upload](2026-05-16-bulk-import-csv.md) | complete | Wave 84 P2 import |
| 2026-05-16 | [Tenant isolation — RLS multi-tenant](2026-05-16-tenant-isolation-rls.md) | complete | Wave 85 Bucket B |

---

## Related

- [`../adr/`](../adr/) — Architectural decisions (some threat-model mitigations are ADRs)
- [`../../04-quality/audits/security/`](../../04-quality/audits/security/) — Pentest + security audits
- [`../../01-business/kitehub/auth/rules.md`](../../01-business/kitehub/auth/rules.md) — Auth business rules
- [`.claude/rules/pre-launch-auth-hardening-checklist.md`](../../../.claude/rules/pre-launch-auth-hardening-checklist.md) — Auth hardening per OWASP A07
