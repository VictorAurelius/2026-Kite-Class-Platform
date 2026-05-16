---
paths: ["**/*Service.java", "**/*Service*.java"]
---

# Audit Service Isolation — `Propagation.REQUIRES_NEW` for every audit/log/notification path

**Priority:** 🟠 MANDATORY — protects auth/business txns from audit-layer failures
**Version:** 1.0.0
**Created:** 2026-05-16
**Last-Reviewed:** 2026-05-16
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule with built-in enforcement per §6.5 Enforcement Parity Mandate; paired same-PR with hotfix `LoginAuditService` REQUIRES_NEW patch + audit RCA + ArchUnit detector + worked self-test on 2026-05-16 admin-login 500 incident; no constraint loosening; existing audit services grandfathered until next refresh)
**Applies to:** Every Spring `@Service` method named `*record*` / `*log*` / `*audit*` / `*notify*` / `*track*` whose responsibility is to persist a side-effect (audit row, log entry, notification dispatch, metric write) that the caller's success path should NOT depend on. Scope explicitly excludes the primary business transaction itself.

---

## 1. The Rule

> **Every audit / log / notification side-effect method that participates in a parent `@Transactional` flow MUST annotate `@Transactional(propagation = Propagation.REQUIRES_NEW)`.** A local `try/catch` around the side-effect is necessary but NOT sufficient — it swallows the exception but does NOT clear Spring's rollback-only flag on the parent transaction, which still throws `UnexpectedRollbackException` at commit.

Without this rule, a failed audit insert (any cause: SQL type mismatch, constraint violation, lock timeout, RLS rejection, NPE) turns a successful business operation into HTTP 500 — even when the side-effect is meant to be best-effort per the method's own javadoc.

---

## 2. Why this matters — 2026-05-16 admin login 500 incident

`LoginAuditService.recordLogin` declared `@Transactional` (default propagation = REQUIRED → joins parent txn from `AuthService.login`). It also wrapped its body in `try { ... } catch (Exception ex) { log.warn(...); }` and the javadoc claimed *"Audit failure must NEVER block login."*

Bug 1 caused every audit INSERT to fail with `SQLState 42804` (Postgres INET vs Java String binding). The local `catch` swallowed the exception → control returned to `AuthService.login` → AuthService completed and returned the JWT response object → Spring's `TransactionInterceptor` then hit the commit phase and saw `rollback-only = true` (set by the failed INSERT BEFORE the catch fired) → threw `UnexpectedRollbackException` → 500 to client.

Cost to confirm root cause: ~2 hours session, 4 wasted login attempts in production, user-flagged P0 incident.

With `Propagation.REQUIRES_NEW`, the audit insert runs in a **separate physical transaction**. Its failure can never set rollback-only on the parent. `try/catch` then actually means what it says.

---

## 3. What counts as audit/log/notification scope (rule applies)

| Pattern | Examples in this repo |
|---------|----------------------|
| `*AuditService.recordX` | `LoginAuditService`, `AdminAuditLog` writer, `ChildProtectionAuditService`, `ParentReadAuditLogService`, `StaffInvitationAuditLog` |
| `*LogService.write*` | `EmailLogService`, `OutboxEventWriter` (debatable — see §4) |
| `*NotificationService.dispatch` | `AdminLoginAlertEventListener` (via async path is fine), direct dispatchers |
| Metric / telemetry write that hits DB | Future scope when added |

| Out of scope (rule does NOT apply) | Why |
|-----------|-----|
| The primary business write itself (saving the User, Instance, Payment, etc.) | That IS the transaction; rolling it back is correct on failure |
| Read-only methods | No write to protect against |
| Methods already invoked from a non-transactional context (e.g. `@Async @TransactionalEventListener(phase=AFTER_COMMIT)`) | Already isolated via event boundary |
| Outbox writers that MUST share parent txn (per `design-patterns.md` §3.5.1 Outbox pattern) | Outbox reliability REQUIRES same-txn write; not an "audit" failure mode |

---

## 4. Decision flow before adding `@Transactional` to a side-effect method

1. **Does this method exist purely to record a side-effect (audit / log / notification)?** If no → standard `@Transactional` semantics apply, rule N/A.
2. **Is the side-effect supposed to be best-effort (caller's success NOT dependent)?** If no → it's not "audit" scope; it's part of the business transaction. Rule N/A.
3. **Does this method participate in a parent `@Transactional` flow (called from another service that is itself transactional)?** If no → standalone txn, default propagation OK.
4. **All three YES → mandatory `@Transactional(propagation = Propagation.REQUIRES_NEW)`.**

Companion requirement: still wrap the body in `try/catch` and log the failure. REQUIRES_NEW isolates the failure; the catch keeps the caller from seeing it as a checked exception. Both layers are needed.

---

## 5. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| `@Transactional` + try/catch + javadoc claim "never blocks caller" | `@Transactional(propagation = REQUIRES_NEW)` + try/catch + javadoc reference to this rule |
| Catch SQLException specifically and assume it isolates | Spring marks rollback-only when ANY `DataAccessException` propagates through `TransactionInterceptor`, including catches that re-throw or trigger nested calls |
| Omit `@Transactional` entirely to "avoid the problem" | Side-effect needs a transaction for atomicity (audit row + alert flag flip in same insert). REQUIRES_NEW gives you that without parent contamination. |
| Use `@Async` to side-step the issue | `@Async` is correct for *truly* async needs but adds thread pool + retry complexity. REQUIRES_NEW is simpler when the caller doesn't need fire-and-forget semantics. |
| Pass parent transaction's connection explicitly | Defeats Spring's transaction abstraction; fragile across upgrades |

---

## 6. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 6.1 ArchUnit test (active now — landed same PR)

Add to `kitehub-subscription/src/test/java/com/kitehub/subscription/architecture/AuditServiceIsolationArchTest.java` (and parallel modules):

```java
@AnalyzeClasses(packages = "com.kitehub")
class AuditServiceIsolationArchTest {
    @ArchTest
    static final ArchRule audit_services_use_requires_new =
        methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("AuditService")
            .or().areDeclaredInClassesThat().haveSimpleNameEndingWith("LogService")
            .and().areAnnotatedWith(Transactional.class)
            .should().beAnnotatedWith(transactional ->
                ((Transactional) transactional).propagation() == Propagation.REQUIRES_NEW)
            .because("audit/log services must isolate from parent transaction — see " +
                    ".claude/rules/audit-service-isolation.md");
}
```

(Skill `quality/design-pattern-audit/SKILL.md` is updated to flag manual review of methods matching the pattern when ArchUnit unavailable.)

### 6.2 Reviewer-checklist (manual)

When reviewing a PR that adds or modifies a `*AuditService` / `*LogService` / `*NotificationService` class:
- [ ] Every `@Transactional` method has `propagation = REQUIRES_NEW`
- [ ] Body is wrapped in try/catch logging the failure
- [ ] Javadoc cites this rule

### 6.3 Override mechanism

```
git commit -m "...
AUDIT_ISOLATION_OVERRIDE: <reason — why this service truly belongs in parent txn>"
```

Quarterly retro reviews overrides. Pattern frequency >5% triggers meta-review.

### 6.4 Detector (deferred per `incident-to-rule-pipeline.md` premature-rule guard)

Future: `audit-gate.py` AUDIT_RULES entry scanning Java diffs for `@Transactional` on `*AuditService` without `REQUIRES_NEW`. Defer until 2nd recurrence; ArchUnit + reviewer + memory sufficient v1.0.0.

---

## 7. Self-test (worked example — 2026-05-16 admin login 500)

Apply rule §4 decision flow retroactively at `LoginAuditService.recordLogin` design moment:

1. Method purely records side-effect (audit row + alert flag)? **YES** — exists for GAP-517 audit + new-fingerprint detection only.
2. Best-effort (caller's success NOT dependent)? **YES** — javadoc explicitly claims so.
3. Participates in parent `@Transactional`? **YES** — called from `AuthService.login` which is `@Transactional`.

→ All three YES. Rule §1 mandates `Propagation.REQUIRES_NEW`. Original ship missed this; hotfix PR adds it. Rule fires correctly on the originating incident. ✅

Counterfactual: if rule existed at GAP-517 design time, reviewer-checklist (§6.2) catches missing REQUIRES_NEW → fix during code review → 0 production incidents.

---

## 8. Relationship to other rules

- **`design-patterns.md`** §3.11 (new sister-row added same PR) — anti-pattern table now lists this gotcha; rule §1 is the positive form
- **`pre-handoff-self-test-completeness.md`** §2.1 auth-gated user-flow — required check (g) "Target action succeeds" would have caught 500 if E2E test had run against Postgres; this rule prevents the bug class earlier
- **`postgres-specific-type-testcontainers.md`** (new sister-rule same PR) — covers the OTHER half of the same incident (Bug 1, INET binding)
- **`release-deploy-standard.md`** §3 smoke-admin-login extension (same PR) — post-deploy net even when both rules above miss
- **`incident-to-rule-pipeline.md`** — this rule = direct output of 2026-05-16 admin login 500 via 5-stage pipeline
- **`rule-change-process.md`** §5.1 atomic-unique bar — atomic (single concept), unique (no existing rule covers REQUIRES_NEW for audit), widely applicable (≥15 audit/log services repo-wide), body ≤2 "and"

---

## 9. Log

- **2026-05-16 (v1.0.0):** Rule created. Triggered by 2026-05-16 production admin login 500 incident (`LoginAuditService.recordLogin` failed at DB binding + default propagation = REQUIRED → poisoned parent txn → `UnexpectedRollbackException`). Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (user-flagged P0) → Classify ✓ (no existing rule mandates REQUIRES_NEW for audit/log services; `design-patterns.md` §3 lacked entry) → Rule+Enforce ✓ (this file + ArchUnit test + reviewer-checklist + paired same-PR with `postgres-specific-type-testcontainers.md` + `design-patterns.md` §3.11 + `release-deploy-standard.md` §3 smoke-admin-login extension per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§7 worked example on the originating incident — rule fires) → Retro Log ✓ (this entry). Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint, no constraint loosening; existing audit services grandfathered, rule applies prospectively from this PR). ArchUnit test deferred to follow-up if scope blows up; v1.0.0 reviewer-checklist enforcement sufficient.
