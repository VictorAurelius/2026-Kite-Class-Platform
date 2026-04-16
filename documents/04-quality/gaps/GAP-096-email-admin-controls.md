# GAP-096: Email Admin Controls & Monitoring Dashboard

**Status:** 🔵 OPEN
**Priority:** 🔴 P0 (trước production)
**Domain:** KiteHub / Email / Admin
**Found:** 2026-04-16 (SaaS business logic deep audit)
**Affects:** Admin operations, email system visibility

## Problem

Admin KHÔNG CÓ cách nào quản lý email notifications:
- Không pause/resume email types khi cần (ví dụ: template lỗi, muốn tạm dừng)
- Không xem email history per instance (user report "tôi không nhận email")
- Không manual trigger scheduler (testing, emergency)
- Không dashboard delivery status
- Config chỉ thay đổi bằng redeploy

## Proposed Fix

### Phase 1: Admin API
```java
@RestController @RequestMapping("/api/admin/emails")
class AdminEmailController {
    GET  /history?instanceId=X          // Email history per instance
    GET  /stats                          // Send/fail rates, daily counts
    POST /trigger/{schedulerName}        // Manual trigger scheduler
    PUT  /config/{emailType}/enabled     // Toggle email type on/off
    GET  /config                         // View current email config
}
```

### Phase 2: Admin Dashboard UI
- Email delivery table: type, recipient, status, timestamp
- Filter by instance, date range, email type
- Charts: daily send volume, failure rate
- Config panel: toggle email types, adjust timing

### Phase 3: Monitoring
- Prometheus metrics: `email_sends_total`, `email_failures_total`, `email_send_duration`
- Grafana dashboard for email system
- Alert: failure rate >5%, email service unreachable >5min

## Acceptance Criteria

- [ ] Admin can view email history per instance
- [ ] Admin can toggle email types on/off without redeploy
- [ ] Admin can manually trigger scheduler jobs
- [ ] Email failure rate visible in monitoring
