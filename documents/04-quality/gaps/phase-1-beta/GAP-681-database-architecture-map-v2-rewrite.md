# GAP-681: Database architecture map v2 rewrite — Vietnamese narrative + per-service mapping + design principles + maturity assessment

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Architecture
**Detected:** 2026-05-19
**Related PRs:** GAP-672 (predecessor v1 shipped Wave 99B B3, commit c6c84c70)
**Related Docs:** `documents/02-architecture/database-architecture-map.md` (v1), `.claude/rules/dev-readable-doc-language.md` §2, `documents/03-planning/roadmap/release-1.5-thesis-scope.md` (thesis Ch.2 prep)

## Current State (verified 2026-05-19)

> Step 2.5 state-check per `audit-to-gap-pipeline.md` — grep paths gap sẽ touch.

| Piece | File / Path | Status |
|---|---|---|
| `database-architecture-map.md` v1 | `documents/02-architecture/database-architecture-map.md` (446 LOC, 3612 từ, 9 sections: Entity Catalog / FK Graph / Migration History / tenant_id Propagation / Sizing / Postgres Types / Follow-up / Related / Log) | ✅ shipped Wave 99B B3 (GAP-672) |
| Vietnamese narrative ratio | Measured ~5-8% Vietnamese diacritic chars / 3612 từ = **English-heavy** | ❌ vi phạm `dev-readable-doc-language.md` §2 row Architecture docs (`audience: mixed` = Vietnamese narrative + English identifiers) |
| Per-service table mapping (service X reads/writes tables Y/Z) | KHÔNG có | ❌ missing |
| Service data flow per service (sequence diagrams) | KHÔNG có (FK graph erDiagram có nhưng KHÔNG phải operation flow) | ❌ missing |
| Database design principles + rules section | Partial — §4 RLS coverage + §6 type inventory chỉ là inventory, KHÔNG phải design rationale | ❌ missing dedicated section |
| Maturity assessment + roadmap | §3 migration history shows breaking changes; §7 follow-up partial; KHÔNG có "stable / gaps / Wave 101+ roadmap" verdict | ❌ missing |
| Auto-gen tooling Backstage pattern | Filed separately as GAP-677 (P2 Wave 100+ tooling) | 🟡 PARTIAL (different scope, defer) |

**Grep commands run:**
```bash
wc -l documents/02-architecture/database-architecture-map.md  # 446 lines
grep -nE "^##" documents/02-architecture/database-architecture-map.md  # 9 section headers
grep -oE "[àáâãèéêìíòóôõùúýăđĩũơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]" documents/02-architecture/database-architecture-map.md | wc -l  # 290 Vietnamese diacritic chars
```

Per Step 0 canonical-status lookup: 0 duplicate hits trong gap-status.csv; ROADMAP §Dropped no match; sister gaps GAP-672 (closed v1) + GAP-677 (auto-gen tooling P2) cover different scope.

## Problem

Dev (user) flagged 2026-05-19 trên `database-architecture-map.md` v1 (vừa ship Wave 99B B3 cùng ngày) 4 concerns:

1. **Vi phạm dev-readable-doc-language rule:** narrative phần lớn English thay vì Vietnamese narrative + English technical identifier mixed-language per §4. Architecture docs `audience: mixed` → reader dev VN phải hiểu nhanh, English-heavy gây friction.
2. **Per-service table mapping thiếu:** §1.1 + §1.2 list table theo service owner, NHƯNG KHÔNG có mapping "service X uses (reads/writes) tables Y/Z trong operation context". Backend dev cần biết "kitehub-platform call query nào trên bảng nào trong luồng login?" — v1 không trả lời được.
3. **Service data flow thiếu:** FK graph (§2 Mermaid erDiagram) cho thấy quan hệ entity tĩnh, KHÔNG cho thấy data flow động (request → service → table operations sequence). Cần sequence diagram per service business flow.
4. **Design principles section thiếu:** v1 inventory features (RLS coverage 56% / type usage / migration breaking changes) nhưng KHÔNG explain DESIGN RATIONALE — why RLS NULL force-fail pattern? why JSONB for which columns? why per-service migration ownership? Thesis Chapter 2 cần design philosophy explicit.
5. **Maturity verdict thiếu:** §7 follow-up scope partial, KHÔNG có "database stable areas / known gaps / Wave 101+ roadmap". Dev không biết "database đã ổn định chưa, có upgrade nữa không?".

Thesis Chapter 2 (architecture + design philosophy + main businesses SAAS + B-learning) trực tiếp depend on this doc làm source for narrative + diagrams. Current v1 không đủ chất lượng cho thesis source material.

## Context

`database-architecture-map.md` v1 shipped Wave 99B B3 (2026-05-19 cùng ngày) qua GAP-672 với scope "consolidated DB architecture report". V1 focus inventory + FK graph + RLS coverage — sufficient for "what exists" question. User feedback ngay sau merge surface gap "why + how + where-next" questions chưa cover.

Per `release-1.5-thesis-scope.md` §3 Phase 1 BETA scope: Chapter 2 needs architecture + design rationale + main business flow (SAAS + B-learning). V2 rewrite addresses both Wave 100 thesis prep AND immediate dev-readability concern.

## Evidence

User direction 2026-05-19 (post Wave 100 plan PR pre-commit):
> "documents/02-architecture/database-architecture-map.md => dev chưa hài lòng với các báo cáo architecture: 1. chưa đúng rule ngôn ngữ của dev, tiếng việt chiếm tỷ lệ cao [meant English-heavy per clarify], 2. chưa trình bày rõ về database như dev muốn: từng service sử dụng bảng nào, luồng như nào, thao tác như nào, thiết kế bảng ra sao? => đây là cũng là dữ liệu quan trọng cho thesis, 3. giải thích rõ các quan điểm, rules thiết kế database mà dự án đang sử dụng, 4. database đã ổn định chưa, hay vẫn còn gaps, sẽ được tiếp tục nâng cấp không?"

Sister rule `dev-readable-doc-language.md` v1.0.2 §2 row Architecture docs explicitly mandate Vietnamese narrative + English identifiers — v1 ratio measured 5-8% Vietnamese fails this.

## Proposed Fix

Rewrite `documents/02-architecture/database-architecture-map.md` v1 → v2 với 4 changes major + structural additions:

### Change 1 — Vietnamese narrative rewrite (§1-§9 existing)

- Section headers chuyển sang Vietnamese (vd "Section 1 — Entity Catalog" → "Mục 1 — Catalog thực thể (Entity)")
- Narrative paragraphs Vietnamese; English giữ cho identifier (table name `subscriptions`, column `tenant_id`, enum `STATUS_ACTIVE`, migration version `V61__*`)
- TL;DR + Audience preserve Vietnamese (đã đúng)
- Table descriptions Vietnamese; column headers + values English (per `dev-readable-doc-language.md` §3)

### Change 2 — ADD §10 Per-service Table Mapping

| Service | Tables read | Tables write | Hot operations |
|---|---|---|---|
| `kitehub-platform` | users, sessions, login_attempts | sessions, login_attempts, mfa_devices | login (read users + write session) |
| `kitehub-subscription` | (full 32 tables) | invoices, subscriptions, ... | trial→paid conversion |
| ... | ... | ... | ... |

### Change 3 — ADD §11 Service Data Flow (Mermaid sequence diagrams)

Per service main business flow:
- Login flow (kitehub-platform → users + sessions)
- Trial signup → paid conversion (kitehub-subscription → trial_records → subscriptions → invoices)
- Tenant provision (kiteclass-core → tenants + branding_resources)
- Class enrollment (kiteclass-core → enrollments + classes + students)
- Email send outbox (kitehub-email → email_outbox → outbox_events)

Per `diagram-format-selection.md` Mermaid sequenceDiagram preferred cho time-ordered flow.

### Change 4 — ADD §12 Database Design Principles + Rules

- Multi-tenant isolation pattern (RLS NULL force-fail rationale + tenant_id column convention)
- Type choice rationale (JSONB vs JSONB array vs JSON; INET → VARCHAR migration lesson)
- FK convention (CASCADE policy per relationship class)
- Migration discipline (Flyway V*__ naming + breaking change protocol + data migration pattern)
- Naming convention (snake_case columns + plural table names + audit column standards `created_at` / `updated_at` / `created_by`)

### Change 5 — ADD §13 Maturity Assessment + Roadmap

- Stable areas (RLS pattern locked Wave 85, type inventory consolidated Wave 99B)
- Known gaps (RLS coverage 51/91 = 56% — 40 tables pending; per-tenant DB sequence `invoice_seq_${tenantId}` Wave 100 Bucket A; eInvoice VAT schema Wave 101+)
- Wave 101+ roadmap (GAP-677 auto-gen Backstage tooling, GAP-185 MISA partnership schema, Phase 2 EKS migration impact)
- Verdict per category (stable ✅ / partial 🟡 / planned 📅 / blocked ❌)

## Acceptance Criteria

- [ ] §1-§9 narrative rewritten Vietnamese per `dev-readable-doc-language.md` §2 row Architecture docs; measured ratio ≥40% Vietnamese diacritic words (compared baseline ~5-8%)
- [ ] §10 Per-service Table Mapping table shipped (≥7 services × tables matrix với hot operations column)
- [ ] §11 Service Data Flow ≥5 Mermaid sequenceDiagram (login + trial→paid + tenant provision + class enrollment + email outbox) per `diagram-format-selection.md` §2.2
- [ ] §12 Database Design Principles ≥5 design rationale sections (RLS / type / FK / migration / naming) với historical lesson cite Wave / GAP refs
- [ ] §13 Maturity Assessment table + Wave 101+ roadmap với GAP refs (GAP-677 / GAP-185 / Phase 2 EKS)
- [ ] frontmatter `version: 2` + Log entry "v1 → v2 rewrite — Wave 100 GAP-681"
- [ ] Reviewer-checklist per `dev-readable-doc-language.md` §7.1 + `diagram-format-selection.md` §5.1 PASS pre-merge

## Related

- `documents/02-architecture/database-architecture-map.md` v1 (predecessor) — GAP-672 closed Wave 99B B3
- `documents/04-quality/gaps/phase-1-beta/GAP-677-auto-gen-database-architecture-map.md` — sister tooling gap (P2 Wave 100+ Backstage pattern)
- `documents/04-quality/gaps/phase-1-beta/GAP-678-wave-99b-post-wave-audit-suite.md` — Wave 99B post-audit gate; GAP-681 may surface Cat 9 ADR coverage delta when audit refreshes
- `.claude/rules/dev-readable-doc-language.md` v1.0.2 §2 row Architecture docs (mandate Vietnamese narrative + English identifiers)
- `.claude/rules/diagram-format-selection.md` v1.0.3 §2.2 (Mermaid sequenceDiagram cho time-ordered flow)
- `.claude/rules/docs-folder-structure.md` (Architecture folder governance)
- `documents/03-planning/roadmap/release-1.5-thesis-scope.md` (thesis Chapter 2 architecture content depends on this doc)
- `documents/03-planning/waves/wave-2026-05-19-100-thesis-push.md` (Wave 100 Bucket F = GAP-681)
- `.claude/rules/incident-to-rule-pipeline.md` — 5-stage applied (Detect user-flagged 4 concerns post Wave 99B merge → Classify v1 scope insufficient cho dev + thesis → Rule+Enforce file gap + add Bucket F + Wave 100 plan update → Self-Test post-rewrite measure ratio + section presence → Retro Log)

## Log

- **2026-05-19** — Initial write-up. Step 0 canonical-status lookup completed (GAP-681 next; 0 duplicate hits; sister GAP-672 v1 closed + GAP-677 auto-gen tooling defer different scope). State-check §2.5 completed (file v1 verified 446 LOC, 9 sections, English-heavy ratio). Filed post user feedback 2026-05-19 trên v1 ship Wave 99B B3 cùng ngày. ADD Bucket F vào Wave 100 plan (per user choice α). Defer rule body write (cấu trúc rewrite chi tiết) sang execution wave 100 Bucket F.
