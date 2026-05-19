# GAP-677: Auto-gen Database Architecture Map from Flyway parser (Backstage pattern)

**Status:** 🟡 OPEN
**Priority:** 🟡 P2
**Domain:** Meta / Tooling
**Phase:** phase-1-beta
**Wave:** Wave 100+ candidate
**Created:** 2026-05-19
**Sister gaps:** GAP-672 (parent — hand-written v1 baseline shipped Wave 99B B3)

---

## Problem

`documents/02-architecture/database-architecture-map.md` shipped Wave 99B Bucket B3 (GAP-672) là hand-written v1 baseline. 3 sections rủi ro drift sau ~3-6 months:

- §1 Entity Catalog (91 tables) — mỗi new V*.sql migration add table → catalog stale
- §2 FK Graph (Mermaid erDiagram) — mỗi new FK relationship → graph stale
- §3 Migration History Index (114 V-files) — mỗi new V*.sql → count + latest stale
- §6 Postgres-Specific Type Inventory — mỗi new `columnDefinition` → inventory stale

Drift trigger frequency: kh-subscription ships ~2-3 V-files/month (Wave 60 → V54 = 7 months avg ~7 V/month); kc-core similar. Mỗi quarter expect ~20-30 schema changes → manual refresh cost grows linearly.

## Root Cause

No automation tooling exists in project to auto-derive DB architecture from Flyway sources of truth. Hand-write pattern phổ biến industry NHƯNG có proven alternative: Spotify Backstage `catalog-info.yaml` + SQL AST parsers (sqlparse Python, JSqlParser Java) emit table/FK/type inventory programmatically.

Per Wave 99B outside-in audit (External Benchmark agent §1 Brainstorm Q3) — Backstage adoption recommended cho FK graph specifically. Cost-benefit deferred Wave 100+ because:
- Phase 1 BETA scale (5-10 tenants) drift risk LOW
- Wave 99B scope focused ship hand-written baseline để unblock 3 personas immediately
- Backstage adoption decision itself = larger meta question (full vs partial tooling)

## Proposed Fix

**Tooling design (3 phases):**

### Phase 1 — Parser script (lightweight, no Backstage)

- `scripts/gen-database-architecture-map.sh` HOẶC `scripts/gen-database-architecture-map.py`
- Input: `kitehub/kitehub-subscription/src/main/resources/db/migration/V*.sql` + `kiteclass/kiteclass-core/src/main/resources/db/migration/V*.sql`
- Parse via:
  - `sqlparse` (Python) HOẶC `JSqlParser` (Java) — both mature + handle ALTER TABLE + CREATE POLICY
  - Extract: CREATE TABLE → table list + columns; ALTER TABLE ENABLE ROW LEVEL SECURITY → RLS map; FOREIGN KEY → FK edges
- Output: Generate sections 1, 2, 3, 6 of `database-architecture-map.md`
- Preserve hand-written sections (4 Tenant_id propagation narrative + 5 Sizing baseline + 7 Auto-gen proposal + 8 Related + 9 Log)

### Phase 2 — Pre-commit hook trigger

- `.husky/pre-commit` detect `db/migration/V*.sql` change → run regen → fail if file diff detected (user must commit regenerated map)
- HOẶC GitHub Action `db-architecture-map-drift` check (lighter — runs on PR not pre-commit)

### Phase 3 — Backstage integration (deferred Wave 110+ if adopted)

- `catalog-info.yaml` template generated from same parser
- Backstage Software Catalog surfaces entities + dependencies UI
- Decision: full Backstage adoption costly (Helm + Postgres + auth) — defer until ≥3 teams need shared service catalog

## Acceptance Criteria

- [ ] Phase 1 parser script shipped + passes self-test on current 114 V-files
- [ ] Output matches hand-written §1 + §2 + §3 + §6 verbatim (verify drift = 0 at landing)
- [ ] Hand-written sections (4/5/7/8/9) preserved via section-anchor template
- [ ] Pre-commit hook OR CI check detects schema drift if regen not run
- [ ] Documentation `documents/05-guides/dev/database-architecture-map-regen.md` covers regen workflow
- [ ] Cross-link from `database-architecture-map.md` §7 updated to point at this GAP-677 resolution

## Out of scope

- Backstage full adoption (Phase 3) — separate gap if decision made
- Auto-generation of §4 Tenant_id propagation narrative (RLS strategy is design intent, not derivable from schema alone)
- Auto-generation of §5 Sizing baseline (requires production data + projection model, separate scope)
- Cross-service FK detection (kh-sub ↔ kc-core via shared `instance_id` UUID semantic — no FK constraint to parse)

## Follow-up triggers (re-evaluate priority)

| Trigger | Action |
|---|---|
| 3rd manual refresh in 90 days (drift cost > automation cost) | Bump priority P2 → P1; ship Phase 1 |
| >5 services have own DB migrations | Auto-gen mandatory (manual scales O(n) services) |
| Backstage adoption decision made for service catalog scope | Pair Phase 3 with Backstage rollout wave |
| Auto-gen FK graph requested by ≥2 sessions consecutive | User signal — bump priority |

## Log

- **2026-05-19 (created):** Filed per `database-architecture-map.md` §7 follow-up. Wave 99B B3 closure (GAP-672) recommended this as Wave 100+ candidate per outside-in Benchmark agent. Hand-written v1 baseline acceptable Phase 1 BETA scale (5-10 tenants); auto-gen becomes mandatory at Phase 2 scale (50-200 tenants) OR when 3rd manual refresh trigger fires. Defer per `incident-to-rule-pipeline.md` premature-rule guard ≥3 manual refresh recurrences. Reviewer: @nguyenvankiet (Wave 99B B3 agent worktree isolation).
