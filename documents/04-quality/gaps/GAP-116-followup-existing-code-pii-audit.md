# GAP-116-followup: Existing-code PII log-leak audit + remediation

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend / Security / Compliance
**Found:** 2026-05-06 (Wave 25 Bucket B closure of GAP-114 + GAP-116)
**Affects:** All Java services (KiteHub 5 + KiteClass 2) — auditing existing `log.*` callsites for plaintext PII

## Problem

Wave 25 Bucket B shipped the PII scrubbing infrastructure (`PIIScrubber`, `@Redact`, logback `pii` converter) but did NOT audit existing code for leaks. GAP-116 AC #4 ("Existing code audit → fix 100% PII log leaks") was scoped out of Bucket B because it requires a multi-service grep + per-callsite review that does not fit alongside the new shared classes + per-service config.

The infrastructure now exists; it can mask known PII patterns in `message`. But callers that interpolate user objects (`log.info("user " + user)` where `user.toString()` includes phone/email) bypass scrubbing's intent: the field arrives as a free-form string with no `@Redact` opportunity.

## Root Cause

Pre-Wave-25 culture had no PII scrubbing standard. Devs logged whole DTOs / entities for debugging. Now that the `@Redact` annotation exists, those callsites need to be revisited.

## Proposed Fix

1. Audit each service:
   ```bash
   grep -rn "log\.\(info\|debug\|warn\|error\).*\(user\|student\|parent\|teacher\|email\|phone\|password\)" \
     */src/main/java --include="*.java"
   ```
2. For each hit, classify:
   - Already covered by `PIIScrubber` regex (no action needed)
   - Whole-object log → mark DTO fields with `@Redact`
   - String interpolation of PII → rewrite to structured arg + `kv()`
3. Add an ArchUnit (or Checkstyle / PMD) rule:
   - Banned: `System.out.println` / `printStackTrace` in `src/main/java`
   - Discouraged: `log.*` calls whose argument list contains a `User` / `Student` / `Parent` etc. param without `@Redact` on its fields

## Acceptance Criteria

- [ ] Audit report saved to `documents/04-quality/audits/security/<date>-pii-log-audit.md`
- [ ] Every flagged callsite either reframed (structured args) or covered by `@Redact`
- [ ] ArchUnit rule active in `kitehub-platform` test source-set
- [ ] Re-run audit after fix → 0 flagged callsites

## Related

- Parent: GAP-116 (PII scrubbing infrastructure, shipped 2026-05-06 PARTIAL)
- Spec: `.claude/rules/logs-format-standard.md` §3
- Skill once landed: `.claude/skills/quality/security-audit/`

## Log

- 2026-05-06 — Filed during Wave 25 Bucket B closure to capture deferred AC #4 of GAP-116.
