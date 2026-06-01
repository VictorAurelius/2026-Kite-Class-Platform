# GAP-222: Outbox Bypass Policy + Migrate 5 Direct-Publish Services

**Status:** 🟡 PARTIAL — Phase 1 (policy) + Phase 3 (detector calibration) shipped Sub-PR 6.4; Phase 2 migration broken into GAP-222a (shared lib), GAP-222b (ParentInvitation), GAP-222c (4 kitehub services)
**Priority:** 🟠 P1 (reliability — events at-most-once instead of at-least-once for 5 services)
**Domain:** Backend / Architecture
**Found:** 2026-04-26 (`design-pattern-audit` baseline run, Sub-PR 6.1)
**Affects:** 5 services in kiteclass-core + kitehub-branding + kitehub-subscription that publish RabbitMQ events directly via `rabbitTemplate.convertAndSend(...)` instead of through `OutboxEventWriter`

## Problem

`design-patterns.md` §3.5 BANS direct event publishing — "If broker down, event lost but DB updated" → cross-service inconsistency. Outbox infra ships in `kiteclass-core/common/outbox/` (7 classes) but adoption is partial.

Audit baseline (2026-04-26) found 5 services bypassing Outbox without documented policy:

| Service | Path | Use case |
|---------|------|----------|
| `ParentInvitationServiceImpl` | `kiteclass-core/.../module/parent/service/impl/` | Parent invitation events |
| `BrandingJobService` | `kitehub-branding/.../service/` | AI generation job dispatch |
| `AIQueueDispatcher` | `kitehub-branding/.../queue/` | AI tier queue routing |
| `EmailServiceClient` | `kitehub-subscription/.../client/` | Email send events |
| `InstancePurgeService` | `kitehub-subscription/.../service/` | Tenant purge cleanup events |

**Excluded as documented exception:**
- `BrandingEventPublisher` — has inline comment `// Best-effort fast-path for cache eviction; outbox is the reliability net.` Justified bypass with backup path.

## Root Cause

No team-wide policy on when direct-publish is acceptable. `design-patterns.md` §3.5 says "use Outbox" but doesn't define the exception envelope. Result: ad-hoc bypass.

## Proposed Fix

### Phase 1 — Policy (S, ≤30 min)
Update `.claude/rules/design-patterns.md` §3.5 (or new §3.5.1) with **Outbox Bypass Policy**:
- Default: ALL cross-service events go through `OutboxEventWriter`
- Exception A — fast-path with backup: bypass allowed IF reliable backup path exists (e.g., outbox covers retry, direct publish optimizes latency)
- Exception B — config bootstrap: bean wiring code in `*Config.java` is exempt
- Exception C — documented test fixtures
- Anti-pattern: silent bypass without one of the above

### Phase 2 — Migration (M, 1-2h per service)
For each of 5 services:
1. Add `OutboxEventWriter` dependency
2. Replace `rabbitTemplate.convertAndSend(exchange, routingKey, payload)` with `outboxEventWriter.write(OutboxEvent.builder()...)` inside the same `@Transactional` method
3. Add integration test verifying outbox row created on commit
4. Verify async outbox publisher drains the row → message reaches consumer (existing integration test pattern)

### Phase 3 — Detector update (S, ≤15 min)
Update `quality/design-pattern-audit/reference/anti-pattern-detectors.md` Cat 5 to:
- Skip javadoc comments (`-v '\\*\\s'`)
- Recognize "fast-path" comment annotation as documented exception
- Cross-reference `design-patterns.md` §3.5 policy

## Acceptance Criteria

- [ ] `design-patterns.md` §3.5 Outbox Bypass Policy documented (Phase 1)
- [ ] All 5 services migrated to OutboxEventWriter with green integration tests (Phase 2)
- [ ] Detector updated to skip documented exceptions (Phase 3)
- [ ] Re-run `design-pattern-audit` skill → Cat 5 score ≥ 16/20 (1-2 sites only, all documented)
- [ ] No new direct-publish sites introduced (PR review checklist)

## Dependencies

- Outbox infra already shipped — `kiteclass-core/common/outbox/` (no new infra needed)
- GAP-046 (parent design-pattern gap) — this is one piece of remaining anti-pattern work
- Integration test container (TestContainers RabbitMQ) — already in CI

## Risk / Tradeoffs

- **Latency increase**: outbox adds 1 DB row + async dispatch latency. For non-fast-path services, acceptable tradeoff for reliability.
- **Migration ordering**: ParentInvitation + InstancePurge are tenant-critical paths; migrate behind feature flag if change is risk-sensitive.
- **EmailServiceClient**: name suggests it's a Feign-style HTTP client, not RabbitMQ — verify scope before migration starts.

## Related

- Audit report: `documents/04-quality/audits/design-patterns/audit-2026-04-26.md`
- Parent gap: GAP-046 (Apply Design Patterns Systematically — 🟡 PARTIAL)
- Rule: `.claude/rules/design-patterns.md` §3.5 (Direct Event Publish BANNED)
- Infrastructure: `kiteclass-core/src/main/java/com/kiteclass/core/common/outbox/` (7-class infra)
- Wave: Wave 6 plan §2.1 (this gap = candidate for Sub-PR 6.4 if Phase 2 fits effort budget; otherwise post-Wave 6)

## Log

- 2026-06-01 — **Wave meta-8 Bucket B SCOPE-REVISE:** SCOPE-REVISE — AC checkboxes outdated vs Status text claim (Phase 1+3 shipped). Recommend sub-AC restructure to reflect actual shipping per Sub-PR 6.4 + 222a/b sub-gaps CSV completion_pct adjusted to unchanged; gap body Status/AC reflect documented scope BEFORE Wave meta-7 audit — re-read audit artifact for current empirical reality. Source: `documents/04-quality/audits/meta/2026-06-01-wave-meta-7-bucket-d-p1-partial.md`.

- 2026-04-26 — **Sub-PR 6.4** shipped Phase 1 + Phase 3 (no migration code). `design-patterns.md` v1.0.0 → **v1.1.0**: added §3.5.1 Outbox Bypass Policy (Exception A fast-path + B bean-wiring + C test fixtures + anti-pattern envelope) + frontmatter backfill per `rule-change-process.md` §3. Detector reference doc updated for all 4 calibration items from baseline audit (Cat 2 HTTP-status filter, Cat 3 boundary-DTO note, Cat 4 `/client/` accept, Cat 5 javadoc + fast-path-marker skip). Phase 2 migration scope-checked + split into 3 sub-gaps: **GAP-222a** (extract Outbox to shared lib — blocker for 222c), **GAP-222b** (ParentInvitationServiceImpl — kiteclass-core internal, not blocked), **GAP-222c** (4 kitehub services — blocked on 222a). All 3 sub-gaps include Current State tables per `audit-to-gap-pipeline.md` Step 2.5. Status: 🔵 OPEN → 🟡 PARTIAL.
- 2026-04-26 — Gap created from `design-pattern-audit` baseline run (Sub-PR 6.1). 5 real hotspots out of 7 raw grep hits; 2 false-positives identified (RabbitConfig javadoc comment + BrandingEventPublisher documented fast-path). Score Cat 5 = 10/20.
