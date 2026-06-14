# GAP-219: Wave 5 audit follow-ups — P1/P2 backlog

**Status:** 🔵 OPEN (umbrella)
**Priority:** 🟠 P1 (per `audit-to-gap-pipeline.md` §6 — P1s queue, do not block 5.6b per wave plan §4)
**Domain:** Cross-cutting (Backend / Testing / Performance / Security / Ops)
**Found:** 2026-04-25 (Wave 5 audit suite — aggregate of P1/P2 findings)
**Affects:** Wave 5 polish + future maintenance windows

## Problem

The Wave 5 audit suite (api-contract / security / performance / ops / quality) surfaced **~13 findings at P1/P2 severity** that don't individually block Sub-PR 5.6b but represent real maintenance debt. Per `audit-to-gap-pipeline.md` Step 3 anti-pattern "1 gap = 1 issue", filing 13 separate gaps would explode ROADMAP noise. This umbrella tracks them as a single backlog with sub-bullets, to be split into individual gap files **only when prioritized for a specific PR**.

Per `audit-to-gap-pipeline.md` §3 the umbrella pattern is acceptable for a "tracking-only" gap — the real issue here is "Wave 5 audit found P1/P2 backlog of 13 items"; sub-bullets are evidence, not separate gaps to fix piecemeal.

## Findings backlog

### P1 (target: address in next maintenance window after Wave 5 closes)

1. **Auth-rejection IT for `DocumentGenerationController`** — add `@WithMockUser`/`@WithAnonymousUser` cases verifying 401/403 enforcement of `@PreAuthorize hasAnyRole(ADMIN,OWNER,TEACHER)`. (api-contract audit finding 1)
2. **Async / timeout config on document endpoints** — `spring.mvc.async.request-timeout` + `@Async` or `CompletableFuture` wrapper + circuit-breaker. Cross-ref `GAP-210` (async queue, deferred). (performance audit finding 3, ops audit finding 7)
3. **`TemplateEngine` singleton + DejaVuSans font cache** — make `InvoiceRenderer.templateEngine` a Spring-managed singleton or static; pre-cache font byte arrays so `useFont()` callback doesn't re-stream from classpath every render. (performance audit findings 3 & 4)
4. **Spring Cache Micrometer metrics + alerts** — enable `spring.cache.gets/puts/evictions` metrics; required by GAP-217's `DocumentBrandingCacheMissStorm` rule. (ops audit finding 6)
5. **Global `@ControllerAdvice` for `DocumentGenerationController`** — RFC 9457 `application/problem+json` response shape with structured context (tenantId, format, templateId, durationMs). Replaces raw 500 + stack trace. (ops audit finding 10)

### P2 (next quarterly polish or opportunistic fix)

6. **`UnsupportedOperationException` mapped in `GlobalExceptionHandler`** — return 501 with detail message instead of generic 500. Future-proofing for new format wiring. (api-contract audit finding 2)
7. **Blank/null format error split in `api-contract.md`** — separate from "unknown format" entry in error matrix. (api-contract audit finding 3)
8. **Caller-precedence example in `api-contract.md`** — concrete request body showing `data:{"branding.primaryColor":"#FF0000",...}` override per `BR-DOC-015`. (api-contract audit finding 4)
9. **Defense-in-depth `HexColorUtil.stripHash` on Thymeleaf model values** — second validation gate at template-context-population time for `branding.*Color` keys. (security audit finding 1)
10. **Whitelist allowed keys in `DocumentGenerationRequestDto.data`** — reject `branding.*` from request body; only server-resolved keys allowed under that prefix. (security audit finding 2)
11. **Async `TenantContext` binding pattern documentation** — capture in skill / memory for future scheduled / queue-consumer document generation. (security audit finding 3)
12. **POI XLSX row-limit documentation** — document upper bound for attendance reports + Streaming API trigger in `BR-DOC-XLSX-*`. (performance audit finding 5)
13. **GAP-210 reference link from `api-contract.md`** — rate-limit deferral note hyperlinks to the gap. (api-contract audit finding 5)

### P3 / informational (defer indefinitely or merge into related gaps)

- Font integrity verification (SHA-256 manifest for bundled TTFs) — only relevant when fonts go to external CDN. Document, don't fix.
- Thymeleaf 3.2.x / OGNL 3.4.x upgrade tracking — already captured in `feedback_thymeleaf_ognl_pin.md` memory; no separate gap needed.

## Proposed Fix

When a maintenance PR is opened that touches one of these areas, split the relevant sub-bullet into its own gap file (`GAP-XXX-<concise-title>.md`) and address it. This umbrella gap closes when **all 13 sub-bullets** are either:
- Fixed (commit reference)
- Split into a separate gap file (and that gap is OPEN/PARTIAL/DONE)
- Explicitly marked WONT_FIX with rationale

## Acceptance Criteria

- [ ] Each sub-bullet either resolved (commit) or migrated to a dedicated gap file
- [ ] When fewer than 3 unresolved sub-bullets remain, split survivors into individual gaps and close this umbrella
- [ ] No sub-bullet sits unaddressed past Wave 7 (90 days from filing)

## Related

- Parent audit reports:
  - `documents/04-quality/audits/api/api-contract-audit-2026-04-25-wave5.md`
  - `documents/04-quality/audits/security/security-audit-2026-04-25-wave5.md`
  - `documents/04-quality/audits/performance/performance-audit-2026-04-25-wave5.md`
  - `documents/04-quality/audits/ops/ops-readiness-audit-2026-04-25-wave5.md`
  - `documents/04-quality/audits/quality/quality-audit-2026-04-25-wave5.md`
- GAP-214: parent audit suite gap (this is one of the 5 closure outputs)
- GAP-210: async document queue — sub-bullet 2 cross-references
- GAP-120 / GAP-114 / GAP-111: pre-existing baseline ops gaps that interact with the P1 list

## Log


- 2026-06-14: phase re-triage — n/a→phase-1-beta (Wave 5 audit follow-up umbrella backlog; meta).
- **2026-04-25:** Filed as umbrella from Wave 5 audit suite. 5 P1 + 8 P2/P3 sub-bullets. Per `audit-to-gap-pipeline.md` exception for tracking-only gaps; sub-bullets split out only when scheduled into a PR.
