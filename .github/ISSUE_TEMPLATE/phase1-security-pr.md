---
name: Phase 1 - Security PR
about: Template cho Security Review PRs (Phase 1)
title: '[SECURITY] PR 1.X: <Title>'
labels: security, phase-1, critical
assignees: ''
---

## PR Information

**PR Number:** PR 1.X (từ code-review-pr-plan.md)
**Phase:** Phase 1 - Critical Security Review
**Priority:** 🔴 P0 - CRITICAL
**Estimated Effort:** X days
**Assignee:** @username

---

## Security Issue

### Vulnerability Description

(Mô tả lỗ hổng bảo mật cần fix)

### Impact

- **Severity:** HIGH / CRITICAL
- **Affected Components:** (list files/modules)
- **Risk:** (data leakage / payment fraud / tier bypass / etc.)

### Reference

- Code Quality Audit Report: `documents/reports/code-quality-audit-report.md`
- Security Standards: `.claude/skills/security-testing-standards.md`

---

## Implementation Checklist

### Code Changes

- [ ] Identify all affected files
- [ ] Implement security fix
- [ ] Remove hardcoded values (if applicable)
- [ ] Add proper validation
- [ ] Add error handling

### Testing Requirements

- [ ] Write security tests (minimum 5 tests per vulnerability)
- [ ] Test happy path
- [ ] Test attack scenarios
- [ ] Test cross-tenant access (if applicable)
- [ ] Test payment fraud scenarios (if applicable)
- [ ] All security tests passing

### Documentation

- [ ] Document security fix approach
- [ ] Update security testing standards (if needed)
- [ ] Add comments explaining security measures

---

## Security Test Checklist

### Multi-Tenant Security (if applicable)

```java
@Test
void shouldPreventCrossTenantAccess() {
    UUID tenant1 = UUID.randomUUID();
    UUID tenant2 = UUID.randomUUID();

    setTenantContext(tenant1);
    Entity entity1 = repository.save(createEntity());

    setTenantContext(tenant2);
    Optional<Entity> result = repository.findById(entity1.getId());

    assertThat(result).isEmpty(); // Cross-tenant blocked
}
```

### Payment Security (if applicable)

```java
@Test
void shouldPreventDoublePayment() {
    PaymentOrder order = createPaidOrder();

    assertThatThrownBy(() -> paymentService.verifyPayment(order.getOrderId()))
        .isInstanceOf(PaymentAlreadyPaidException.class);
}
```

### JWT Security (if applicable)

```java
@Test
void shouldRejectCrossTenantToken() {
    UUID tenant1 = UUID.randomUUID();
    String token = jwtService.generateToken(1L, "user@t1.com", tenant1, roles);

    setTenantContext(UUID.randomUUID()); // Different tenant

    assertThatThrownBy(() -> jwtService.validateToken(token))
        .isInstanceOf(InvalidTokenException.class);
}
```

---

## Review Criteria

### Security Review

- [ ] Security fix addresses root cause
- [ ] No new vulnerabilities introduced
- [ ] All attack vectors covered
- [ ] Security tests comprehensive
- [ ] Code reviewed by security team

### Code Quality

- [ ] Code follows security best practices
- [ ] No hardcoded secrets
- [ ] No sensitive data in logs
- [ ] Proper error handling
- [ ] Input validation comprehensive

---

## Merge Criteria

- [ ] All security tests passing (100%)
- [ ] Security team approval
- [ ] Code review approved (≥2 reviewers)
- [ ] No regressions in existing tests
- [ ] Documentation updated

---

## Post-Merge Actions

- [ ] Update security audit status
- [ ] Monitor for security alerts
- [ ] Notify team of security fix

---

**Reference Documents:**
- `.claude/skills/security-testing-standards.md`
- `documents/reports/code-quality-audit-report.md`
- `documents/plans/code-review-pr-plan.md` (Phase 1)
