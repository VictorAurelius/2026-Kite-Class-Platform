---
audience: dev
---

# GAP-787 — Staff invite email send never implemented (Bug #14 Wave meta-6 walk shutdown)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-28 (Wave meta-6 Bucket A RST walk shutdown — see `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` §Bug class F #14)
**Phase:** phase-1-beta

## Problem

`StaffInvitationServiceImpl.invite()` ONLY saves DB row. **Zero outbox event / RabbitMQ publish / email service binding / template rendering / SES dispatch logic.**

Feature "Owner mời staff via email" is non-functional in production — staff sẽ không bao giờ nhận được lời mời.

## Reproduction

Wave meta-6 Bucket A walk 2026-05-28:

```bash
# Owner POST invite (after walk-fixes)
curl -X POST "http://localhost:9000/api/v1/staff-invitations" \
  -H "X-User-Roles: OWNER" -H "X-Instance-Subdomain: sky-edu-test" \
  -d '{"email":"staff.test1@test.vn","role":"TEACHER"}'
# → 201 Created (DB row created)

# Check MailHog inbox:
curl -s http://localhost:8025/api/v2/messages?limit=5
# → 2 unrelated emails to race1@example.com; NO email to staff.test1@test.vn

# Check outbox events:
docker exec kite-postgres psql -U kitehub -d kiteclass_shared \
  -c "SELECT * FROM outbox_events WHERE event_type LIKE '%staff%';"
# → 0 rows

# Check kitehub-email service logs:
docker logs kitehub-email --since 5m | grep -i staff
# → empty (no consumer triggered)
```

## Root Cause

`StaffInvitationServiceImpl.invite()` lines 55-76 (Wave meta-6 Bucket A PR #1904 ship):

```java
public StaffInvitationResponse invite(UUID tenantId, String email, String role, Long inviterId) {
    log.info("Issuing staff invitation: ...");
    StaffInvitation invitation = StaffInvitation.builder()
            .email(...)
            .role(role)
            .token(UUID.randomUUID().toString())
            // ...
            .build();
    invitation.setInstanceId(tenantId);
    invitation = invitationRepository.save(invitation);
    log.info("Staff invitation created: ...");
    return toResponse(invitation, /* includeToken */ true);
}
```

Missing:
- Outbox event publish (`OutboxEventWriter` per `design-patterns.md` §3.5)
- RabbitMQ binding for `staff.invitation.created` routing key
- kitehub-email consumer cho new event type
- Email template (Vietnamese, persona-appropriate per `vn-localization-audit-checklist.md` + `user-manual-content-standard.md`)
- Link content với accept URL containing token

## Proposed Fix

**Architecture decision:** outbox + event-driven pattern per `design-patterns.md` §3.5.1.

### Step 1: Outbox event (kiteclass-core)

Inject `OutboxEventWriter` into `StaffInvitationServiceImpl`. Add in `invite()` method (same `@Transactional` as save):

```java
outboxEventWriter.enqueue(
    "staff.invitation.created",         // routingKey
    "STAFF_INVITATION",                  // aggregateType
    invitation.getId().toString(),        // aggregateId
    Map.of(
        "tenantId", tenantId.toString(),
        "email", invitation.getEmail(),
        "role", invitation.getRole(),
        "token", invitation.getToken(),
        "expiresAt", invitation.getExpiresAt().toString()
    )
);
```

### Step 2: kitehub-email consumer

Add `@RabbitListener(queues = "staff.invitation.queue")` consumer in kitehub-email:
- Bind queue to exchange với routing key `staff.invitation.created`
- Render email template với accept URL `https://<subdomain>.kitehub.me/staff/accept-invite?token=<token>`
- Call existing SES/Resend `EmailService` to dispatch
- Audit log per `logs-format-standard.md`

### Step 3: Email template

Create `kitehub-email/.../templates/staff-invitation.html`:
- Vietnamese narrative per `vn-localization-audit-checklist.md` §2
- Persona tone matrix STAFF/TEACHER/MANAGER appropriate
- VN cultural awareness (Zalo culture aware per §4 row 1)
- Includes Owner organization name + invitation expiry + accept CTA

### Step 4: Verify MailHog dev / SES prod

Local: confirm email arrives MailHog. Production: confirm SES sending domain DKIM verified + email arrives recipient inbox.

### Step 5: RabbitMQ queue auto-declare

Avoid Bug #6 recurrence (`class.rescheduled.queue` manual declare). Verify queue/exchange/binding declared via Spring AMQP config OR migration script — không assume admin manual declare.

## Acceptance Criteria

- [ ] Outbox event published synchronously với invitation create (`design-patterns.md` §3.5 outbox pattern)
- [ ] kitehub-email consumer listens for `staff.invitation.created` routing key
- [ ] Email template rendered Vietnamese narrative + persona-appropriate tone
- [ ] Accept URL in email correct: `https://<subdomain>.kitehub.me/staff/accept-invite?token=<token>`
- [ ] MailHog dev: email appears với correct recipient + content
- [ ] Production smoke: email arrives recipient inbox (DKIM PASS, không spam)
- [ ] RabbitMQ queue auto-declared (no manual `rabbitmqadmin` step required)
- [ ] `feature-ship-runtime-walk-mandate.md` v1.0.0 §3 walk evidence: walk full flow Owner invite → MailHog email → recipient click → accept page renders
- [ ] Test: IT covers outbox event publish + consumer end-to-end (Testcontainers RabbitMQ if needed)
- [ ] Sister gap GAP-786 (Bug #17 user provision) coordinated — both ship together OR documented sequencing

## Related

- Walk shutdown findings: `documents/04-quality/audits/rst-html/2026-05-28-wave-meta-6-bucket-a-walk-shutdown-findings.md` Bug class F #14
- Sister gap GAP-786 (Bug #17 user provision on accept — paired feature gap, ship together makes feature usable end-to-end)
- META rule: `.claude/rules/feature-ship-runtime-walk-mandate.md` v1.0.0 — closure MUST satisfy walk evidence
- Architecture pattern: `design-patterns.md` §3.5 Outbox + §3.5.1 outbox bypass policy
- Email standards: `vn-localization-audit-checklist.md` v1.0.0 + `user-manual-content-standard.md` v1.0.0 (Vietnamese narrative + persona tone)
- Recurrence: Wave 6 RST findings Bug #6 (`class.rescheduled.queue` manual declare) — avoid same auto-declare miss
- Wave meta-6 Bucket A: `documents/03-planning/waves/wave-2026-05-27-meta-6-fix-p0-meta-update-rst-html.md` (re-classification candidate)

## Log

- **2026-05-28** — Filed in response to Wave meta-6 Bucket A RST walk shutdown (17 bugs surfaced). P0 because feature non-functional end-to-end — staff không bao giờ nhận lời mời trong production. Estimated 8-12 similar email-path-missing features per audit retro doc Top 3 predicted bug patterns (Bug #14 class recurrence wave-80-plus-retro). Walk-fix not applicable (feature path missing). Phase 2 BETA Wave B scope per audit retro recommendation. Ship together với GAP-786 to make feature actually usable.
