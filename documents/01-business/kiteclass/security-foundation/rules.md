# Security Foundation — Business Rules

**Domain:** security-foundation
**Source:** Wave 4 Sub-PR 4.0, ADR-010 + ADR-011 + ADR-012 + ADR-013

## Rules

### Audit trail
| ID | Rule |
|----|------|
| BR-AUDIT-001 | Every security-sensitive action MUST write an AuditLog row in the same transaction as the domain change |
| BR-AUDIT-002 | AuditLog is append-only — NEVER UPDATE or DELETE rows in normal operation |
| BR-AUDIT-003 | Callers MUST use AuditLogWriter (Propagation.MANDATORY); direct repository access discouraged |
| BR-AUDIT-004 | Payload truncated to 8 000 chars; reason to 500 chars (prevents DB bloat) |
| BR-AUDIT-005 | actionType format: `{domain}.{verb_past}` (e.g. `rebrand.rejected`, `dmca.takedown.valid`) |

### Security SPI (interfaces in foundation)
| ID | Rule |
|----|------|
| BR-SEC-001 | SvgSanitizer MUST strip script/on*/off-origin xlink-href; preserve path/rect/text/etc. |
| BR-SEC-002 | UrlAllowlistValidator blocks private ranges + unknown tenant allowlists by default |
| BR-SEC-003 | CsrfTokenProvider uses double-submit cookie (token + cookie must match) |
| BR-SEC-004 | Concrete impls live in Sub-PR 4.2; consumers in 4.1/4.3/4.4 code against the interface |

### Migration version reservation (coordination rule)
| Version | Owner Sub-PR | Purpose |
|---------|:------------:|---------|
| V35 | 4.0 (this) | audit_log |
| V36 | 4.1 | moderation_queue |
| V37 | 4.3 | dmca_takedown_requests |
| V38 | 4.4 | deletion_requests + retention_policy |
| V39 | 4.5 | quality_reports |

Agents working on parallel sub-PRs MUST use their reserved version — pre-assigned to prevent conflicts.

## Config keys (deferred to sub-PRs)

Foundation does NOT introduce runtime config; see respective sub-PR docs for:
- `moderation.*` (4.1)
- `security.svg.*`, `security.url.allowlist.*`, `security.csrf.*` (4.2)
- `legal.dmca.*` (4.3)
- `retention.*` (4.4)
- `quality-gate.*` (4.5)

## Five-attribute review per `business-logic-review.md`

Per-rule attributes (Source / Rationale / Reviewer / Compliance check / Review cadence) backfilled at file-level placeholder per Phase 1 of GAP-433. Per-rule granularity tracked via GAP-156 Phase 2 stakeholder sign-offs.

- **Source:** Existing rules in this file derive from a mix of: feature gaps cited inline (where present), ADRs, persona reviews, and informed-gut estimates from Wave 1-30 work. Rules without inline citation default to `informed gut` per `business-logic-review.md` §2.1 and inherit quarterly re-review obligation below.
- **Rationale:** Rule values reflect product judgment + (where applicable) competitor benchmarks + VN regulatory minimums. Detailed per-rule rationale to be backfilled during GAP-156 Phase 2 stakeholder review; until then, treat values as `informed gut` subject to next quarterly review.
- **Reviewer:** @nguyenvankiet (acting Product Owner, solo-dev, 2026-05-08). Formal stakeholder + legal counsel sign-off queued via GAP-156. Solo-dev exemption per `business-logic-review.md` §2.3 — the Reviewer line documents which hat is being worn AND obligation is attached for team-growth or pre-launch trigger.
- **Compliance check:** **Compliant** — Luật An ninh mạng 2018 Art 26; OWASP Top 10 2021; PDPL 2023 Art 27 (security obligations).
- **Review cadence:** Quarterly (default per `business-logic-review.md` §2.5). **Next review:** 2026-08-08. Event triggers: Cybersecurity law amendment, OWASP Top 10 revision, security-incident pattern.

## Log
- 2026-04-14 — Initial rules (Wave 4 Sub-PR 4.0)
