# Phases 4-5-6-7: Documentation, Review, Commit, Merge

> Pointer: read this when entering pre-merge gates — docs sync, code review, commit workflow, merge criteria. Parent skill: `../SKILL.md`.

<!-- TODO: verify against current state — references to `kiteclass-implementation-plan.md`, `QUICK-START.md`, and per-service docs may not match current docs/03-planning/ structure. -->

## 📚 Phase 4: Documentation Updates

### 4.1 Update Implementation Plan

**WHEN:** After completing any PR implementation

**FILE:** `/documents/scripts/kiteclass-implementation-plan.md`

**WHAT TO UPDATE:**

#### Progress Tracking Section

```markdown
## Gateway Service (feature/gateway branch)
- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
...
- ✅ PR 1.X: [Just completed PR] ← UPDATE THIS

**Gateway Status:** X/7 PRs completed (X.X%) ← UPDATE THIS
**Tests:** XX passing (XX unit + XX integration) ← UPDATE THIS
**Last Updated:** 2026-XX-XX (PR 1.X - Description) ← UPDATE THIS
**Current Work:** [Next planned work] ← UPDATE THIS
```

#### PR Status Icon

Change from `⏳` (pending) to `✅` (completed):

```markdown
## ✅ PR 1.5 - Email Service  ← Change icon
```

#### Overall Progress

```markdown
**Overall Progress:** X/27 PRs completed (X.X%) ← UPDATE THIS
**Last Updated:** 2026-XX-XX (PR X.X - Description) ← UPDATE THIS
```

---

### 4.2 Update QUICK-START.md

**WHEN:** After completing PR, before ending session

**FILE:** Service-specific `docs/QUICK-START.md`
- Gateway: `kiteclass-gateway/docs/QUICK-START.md`
- Core: `kiteclass-core/docs/QUICK-START.md`
- Frontend: `kiteclass-web/docs/QUICK-START.md`

**WHAT TO UPDATE:**

#### Current State Section

```markdown
## 🚀 Current State

**Latest Completed:** PR X.X - Feature Name
**Branch:** feature/{service}
**Tests:** XX passing (XX unit + XX integration)
**Docker:** ✅ Ready
**Status:** Ready for PR X.X
```

#### Add New PR Option

```markdown
## 🚀 Option X: PR X.X - Feature Name

**Copy this prompt when context is cleared:**

\```
I'm continuing development on KiteClass {Service} Service.

CURRENT STATE:
- Working directory: /path/to/kiteclass-{service}
- Branch: feature/{service}
- Completed: PR X.X (Feature Name)
- Status: XX tests, Docker setup complete

DOCUMENTATION TO READ:
1. /path/to/docs/README.md
2. /path/to/docs/PR-X.X-SUMMARY.md

MY REQUEST: Implement PR X.X - Next Feature

REQUIREMENTS:
- Requirement 1
- Requirement 2
- Add tests

CONSTRAINTS:
- Follow reactive patterns (Gateway)
- Follow module structure (Core)
- Use existing error handling

USER INFO:
- Name: VictorAurelius
- Email: vankiet14491@gmail.com

**NOTE: Communicate in Vietnamese for easier control.**

Please read the docs first, then help me implement step by step.
\```
```

#### Update Completed Items

Mark completed PRs:
```markdown
### Completed PRs
- ✅ PR 1.1: Project Setup
- ✅ PR 1.2: Common Components
- ✅ PR 1.X: [Just completed] ← ADD THIS
```

---

### 4.3 Update Module Documentation

**WHEN:** After implementing Core Service modules

**FILES:** `kiteclass-core/docs/modules/{module}-module.md`

**WHAT TO UPDATE:**
- Implementation status
- Business rules (if changed)
- API endpoints (if added/modified)
- Error scenarios (if new errors added)
- Caching strategy (if changed)
- Future enhancements (if completed items)

**Template:** See `kiteclass-core/docs/module-business-logic.md`

---

## 📋 Phase 5: Documentation Update Checklist

**Before Committing PR:**

- [ ] Implementation plan status icon updated (⏳ → ✅)
- [ ] Progress tracking statistics updated
- [ ] Test count updated
- [ ] Last Updated date updated
- [ ] Current Work updated
- [ ] Overall progress percentage recalculated
- [ ] QUICK-START.md updated with new PR option
- [ ] QUICK-START.md current state updated
- [ ] Module docs updated (if applicable)

---

## 🔍 Phase 6: Code Review & Commit

### KiteClass-Specific Code Review Checklist

**⚠️ CRITICAL: These checks are MANDATORY for KiteClass Platform**

#### Multi-Tenant Security Checks

- [ ] **Tenant Context Injection**: All repository queries include `instance_id` filter
- [ ] **No Hardcoded Instance IDs**: No `UUID.fromString("...")` in code
- [ ] **TenantContext Usage**: Use `TenantContext.getCurrentInstanceId()` instead of hardcoded IDs
- [ ] **@TenantScoped Annotation**: All repositories have `@TenantScoped` or manual tenant filtering
- [ ] **Cross-Tenant Access Prevention**: Tested that users from Tenant A cannot access Tenant B data
- [ ] **JWT Token Validation**: JWT tokens are validated for correct instanceId
- [ ] **API Response Filtering**: Responses only include data for current tenant
- [ ] **Bulk Operations Safety**: Bulk operations (delete, update) respect tenant boundaries

**Example of CORRECT tenant filtering:**
```java
// ✅ GOOD - Uses tenant context
@Query("SELECT s FROM Student s WHERE s.instanceId = :instanceId AND s.deletedAt IS NULL")
Page<Student> findByInstanceId(@Param("instanceId") UUID instanceId, Pageable pageable);

// ❌ BAD - No tenant filter
@Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL")
Page<Student> findAll(Pageable pageable);
```

#### Feature Detection Checks

- [ ] **Feature Gate Usage**: Premium features wrapped in `@RequireFeature("FEATURE_NAME")`
- [ ] **Tier-Based Access Control**: API returns 403 if feature unavailable for tier
- [ ] **UI Feature Gating**: Frontend components use `<FeatureGate>` for tier-locked features
- [ ] **Upgrade Prompts**: Locked features show upgrade prompt with required tier
- [ ] **Feature Config Caching**: Instance feature config cached with 1-hour TTL
- [ ] **Graceful Degradation**: Missing features degrade gracefully (not crash)

**Example of CORRECT feature gating:**
```java
// ✅ GOOD - Feature gate with annotation
@PostMapping("/engagement/track")
@RequireFeature("ENGAGEMENT")
public ResponseEntity<EngagementResponse> trackEngagement(@Valid @RequestBody TrackEngagementRequest request) {
    return ResponseEntity.ok(engagementService.track(request));
}

// ❌ BAD - No feature gate
@PostMapping("/engagement/track")
public ResponseEntity<EngagementResponse> trackEngagement(@Valid @RequestBody TrackEngagementRequest request) {
    return ResponseEntity.ok(engagementService.track(request));
}
```

#### Payment Security Checks

- [ ] **Amount Validation**: Payment amounts match tier pricing (499k/999k VND)
- [ ] **Double Payment Prevention**: Order status checked before processing
- [ ] **Order Expiry Validation**: QR codes expire after 10 minutes
- [ ] **Transaction Idempotency**: VietQR callbacks are idempotent
- [ ] **Audit Logging**: All payment state changes logged
- [ ] **Error Handling**: Payment failures logged with reason
- [ ] **No Financial Data in Logs**: Never log full credit card/bank details

**Example of CORRECT payment validation:**
```java
// ✅ GOOD - Validates order status
public void verifyPayment(VerifyPaymentRequest request) {
    PaymentOrder order = orderRepo.findByOrderId(request.getOrderId())
        .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

    // Prevent double payment
    if (order.getStatus() == PaymentStatus.PAID) {
        throw new PaymentAlreadyPaidException(order.getOrderId());
    }

    // Verify amount matches
    if (!order.getAmount().equals(request.getAmount())) {
        throw new AmountMismatchException(order.getAmount(), request.getAmount());
    }

    order.setStatus(PaymentStatus.PAID);
    orderRepo.save(order);
}

// ❌ BAD - No validation
public void verifyPayment(VerifyPaymentRequest request) {
    PaymentOrder order = orderRepo.findByOrderId(request.getOrderId()).get();
    order.setStatus(PaymentStatus.PAID);
    orderRepo.save(order);
}
```

#### Trial System Checks

- [ ] **Trial Status Validation**: Trial days remaining calculated correctly
- [ ] **Grace Period Handling**: Grace period (3 days) enforced after trial expires
- [ ] **Suspension Logic**: Instance suspended after grace period expires
- [ ] **Data Retention**: Suspended instances retained for 90 days before deletion
- [ ] **Status Transitions**: Only valid status transitions allowed (TRIAL → GRACE → SUSPENDED → DELETED)
- [ ] **Banner Display**: Correct trial banner shown based on status

#### AI Service Checks (if applicable)

- [ ] **Circuit Breaker**: AI calls wrapped in circuit breaker (Resilience4j)
- [ ] **Timeout Handling**: AI calls timeout after 10 seconds
- [ ] **Fallback Strategy**: Graceful fallback if AI service unavailable
- [ ] **Cost Controls**: Request throttling to prevent runaway costs
- [ ] **Error Logging**: AI failures logged with error details
- [ ] **Retry Logic**: Exponential backoff retry (max 3 attempts)

#### General Security Checks

- [ ] **Input Validation**: All user inputs validated with `@Valid`, `@NotNull`, `@Size`
- [ ] **SQL Injection Prevention**: Using JPA/JPQL (not raw SQL)
- [ ] **XSS Prevention**: Frontend sanitizes user-generated content
- [ ] **CSRF Protection**: CSRF tokens enabled for state-changing operations
- [ ] **Authentication**: All non-public endpoints require JWT token
- [ ] **Authorization**: Role-based access control enforced (OWNER, TEACHER, STUDENT)
- [ ] **Sensitive Data**: No passwords, API keys, secrets in code/logs
- [ ] **Rate Limiting**: API endpoints rate-limited (100 req/min per instance)

---

### Code Review Checklist (Self-Review)

**Before Creating PR:**

- [ ] Code đúng requirement
- [ ] **ALL KiteClass-specific checks passed** (above section)
- [ ] Không có security issues
- [ ] Xử lý error cases đầy đủ
- [ ] Có unit tests (coverage >= 80%)
- [ ] Code clean, readable
- [ ] Không có duplicate code
- [ ] Performance acceptable
- [ ] No compilation warnings
- [ ] Documentation updated

### Warning Policy

| Warning Type | Action |
|--------------|--------|
| Compilation warning | MUST fix before merge |
| Deprecated API | Must have upgrade plan |
| Security warning | CANNOT merge |
| Performance warning | Review and document |

### Commit Workflow

**Step 1: Check Git Status**
```bash
git status
```

**Step 2: Stage Changes**
```bash
git add -A
```

**Step 3: Commit with Detailed Message**

Use HEREDOC for complex commits:

```bash
git commit -m "$(cat <<'EOF'
feat(core): implement PR 2.3 - Student Module

Complete Student module with CRUD operations and business rules.

Features:
- Student entity with soft delete
- CRUD endpoints with pagination
- Business rules validation (email/phone uniqueness)
- Redis caching with 1-hour TTL
- MapStruct for DTO mapping

Changes:
- Created Student entity, DTOs, mapper, service, controller
- Created Flyway migration V2__create_student_tables.sql
- Created 5 test classes (35/41 tests passing)
- Updated module business logic documentation

Tests:
- 10/10 service tests passing ✅
- 3/3 mapper tests passing ✅
- 0/5 controller tests (needs security config)
- 0/6 repository tests (needs Docker)

Files: 15 files changed

Co-Authored-By: VictorAurelius <vankiet14491@gmail.com>
EOF
)"
```

**Step 4: Verify Commit**
```bash
git log -1 --stat
```

### Commit Rules

1. **ALWAYS commit after implementing features**
2. **NEVER skip commit step**
3. **ALWAYS use Co-Authored-By for AI assistance**
4. **ALWAYS include test results**
5. **ALWAYS list major changes**

---

## ✅ Phase 7: Merge Criteria

### PR Merge Checklist

**MUST PASS before merge:**

- [ ] All checklist items completed
- [ ] Tests pass 100% (or documented failures with reason)
- [ ] No compilation warnings
- [ ] Code review approved
- [ ] Documentation updated (plan, quick-start, module docs)
- [ ] No security warnings
- [ ] CI/CD pipeline green

### Cannot Merge If:

- ❌ Any checklist item uncompleted
- ❌ Tests failing (without documented reason)
- ❌ Security warnings present
- ❌ No code review
- ❌ Breaking changes without migration guide
