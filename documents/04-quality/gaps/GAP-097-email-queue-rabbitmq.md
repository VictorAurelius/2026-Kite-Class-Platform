# GAP-097: Email Queue via RabbitMQ (Replace Direct HTTP)

**Status:** 🟢 DONE
**PR:** #312
**Priority:** 🔴 P0 (trước production)
**Domain:** KiteHub / Email / Architecture
**Found:** 2026-04-16 (SaaS business logic deep audit)
**Affects:** Email reliability, scalability

## Problem

Hiện tại email gửi bằng **direct synchronous HTTP** từ scheduler → email service:

```
@Scheduled cron → findInstances() → for each → HTTP POST /send → done
```

Vấn đề:
1. **Email service down → emails mất**: Không retry, không queue, log + swallow exception
2. **Không scale**: 10K instances × 1 HTTP call each = timeout trước khi xong
3. **Không rate limit**: SES/SMTP có sending limits, current code không respect
4. **Blocking**: Scheduler thread blocked cho mỗi HTTP call (800ms average × 1000 = 800s)
5. **RabbitMQ đã có sẵn** nhưng chỉ dùng cho AI Branding, KHÔNG dùng cho email

## Proposed Fix

### Architecture Change

```
BEFORE:
@Scheduled → HTTP POST /send (sync, no retry)

AFTER:
@Scheduled → RabbitMQ queue "email.send" (async, durable)
    ↓
Email Consumer → HTTP POST /send (with retry)
    ↓ (fail)
DLQ "email.dlq" → Admin notification
```

### Implementation

1. **Producer** (trong schedulers):
```java
// Thay thế HTTP call trực tiếp
rabbitTemplate.convertAndSend("email.send", emailRequest);
```

2. **Consumer** (mới, trong kitehub-email hoặc kitehub-subscription):
```java
@RabbitListener(queues = "email.send")
void handleEmail(EmailRequest request) {
    try {
        emailService.send(request);
    } catch (Exception e) {
        // Retry 3 lần với backoff, sau đó → DLQ
        throw new AmqpRejectAndDontRequeueException(e);
    }
}
```

3. **DLQ handling**:
```java
@RabbitListener(queues = "email.dlq")
void handleFailedEmail(EmailRequest request) {
    // Log to admin dashboard
    // Alert admin
    emailSentLogService.markFailed(request);
}
```

4. **Rate limiting**: Consumer prefetch = 5 (max 5 concurrent sends)

### Config
```yaml
spring.rabbitmq:
  listener.simple:
    retry:
      enabled: true
      max-attempts: 3
      initial-interval: 60000    # 1 min
      multiplier: 5              # 1min, 5min, 25min
```

## Acceptance Criteria

- [ ] Emails published to RabbitMQ queue (not direct HTTP)
- [ ] Consumer processes with retry (3 attempts, exponential backoff)
- [ ] DLQ captures permanently failed emails
- [ ] Admin alerted on DLQ accumulation
- [ ] Rate limiting: max 5 concurrent sends
- [ ] Email service restart → queued emails NOT lost (durable queue)
