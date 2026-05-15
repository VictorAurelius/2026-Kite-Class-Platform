---
persona: platform-admin
topic: monitoring
last-updated: 2026-05-15
version: v0.9.0-beta
effort_minutes: 4
---

# Theo dõi sức khoẻ hệ thống — Monitoring dashboard

> 📅 Cập nhật lần cuối: **2026-05-15** · Phiên bản KiteHub: **v0.9.0-beta** · Đọc khoảng **4 phút**

## TL;DR

Mỗi sáng em Mai check dashboard `/admin/monitoring` trong 5 phút để verify hệ thống khoẻ. Có 4 metric quan trọng và 3 ngưỡng cảnh báo.

- 📊 **4 metric chính:** Error rate · P95 latency · Queue depth · Uptime
- 🚨 **3 ngưỡng:** Healthy < Warning < Critical
- 📨 **Alert:** Email tới `admin@kitehub.me` + Slack khi vượt Critical
- 🎯 **SLO target:** Uptime ≥99.5% / P95 ≤500ms / Error rate ≤0.5%

---

## 1. Mở dashboard

<!-- Screenshot placeholder: capture monitoring-step-1.png — 1440×900 vi-VN — show /admin/monitoring với 4 metric cards (Error Rate 0.12%, P95 287ms, Queue Depth 3, Uptime 99.87%) + 4 chart 24h trend + mũi tên đỏ chỉ vào ngưỡng warning -->

URL: `/admin/monitoring`

Layout 4 cards trên cùng + 4 line chart 24h dưới:

| Card | Hiện tại | Trend 24h | SLO target |
|---|---|---|---|
| **Error rate (5xx)** | 0.12% | ↘ (giảm) | ≤0.5% |
| **P95 latency** | 287ms | ↔ (ổn định) | ≤500ms |
| **Queue depth (RabbitMQ)** | 3 jobs | ↗ tăng nhẹ | ≤50 |
| **Uptime (30d)** | 99.87% | — | ≥99.5% |

---

## 2. Hiểu các metric

### 2.1 Error rate (5xx)

Tỷ lệ request trả về HTTP 5xx so với tổng request 1 giờ qua.

- 🟢 < 0.1% → khoẻ
- 🟡 0.1-0.5% → warning, kiểm tra log
- 🔴 > 0.5% → critical, file P0 gap

Cách debug: mở `/admin/logs?level=ERROR&time=1h` → grep theo `service` (kitehub-subscription, kitehub-branding, kiteclass-core) → tìm stack trace phổ biến.

### 2.2 P95 latency

Phản hồi API tại percentile 95 (95% request trả về trong khoảng thời gian này).

- 🟢 < 300ms → khoẻ
- 🟡 300-500ms → warning, có thể DB slow query
- 🔴 > 500ms → critical, alert SLO breach

Cách debug: mở `/admin/slow-queries?threshold=500ms` → liệt kê top SQL gây chậm → optimize index hoặc query.

### 2.3 Queue depth (RabbitMQ)

Số job đang chờ xử lý trong queue (AI Branding, Email transactional, Migration import).

- 🟢 < 10 jobs → worker keep up
- 🟡 10-50 jobs → worker chậm, kiểm tra throughput
- 🔴 > 50 jobs → critical, scale worker

Cách debug: SSH vào EC2 worker, `docker logs kitehub-ai-worker` xem có exception nào không. Restart worker nếu cần.

### 2.4 Uptime

Phần trăm thời gian endpoint health-check `/actuator/health` trả 200 trong 30 ngày qua.

- 🟢 ≥99.5% → đạt SLO
- 🟡 99.0-99.5% → warning, gần breach
- 🔴 <99.0% → SLO breach, communicate với beta tenant

---

## 3. Alert routing

Khi metric vượt Critical, hệ thống tự động:

1. Send email tới `admin@kitehub.me` với subject `[KiteHub Alert P0] {metric} = {value}`
2. Send Slack message vào channel `#kitehub-ops` (nếu Slack integration active)
3. Write alert row vào `/admin/alerts` với link tới dashboard

<!-- Screenshot placeholder: capture monitoring-step-2.png — 1440×900 vi-VN — show /admin/alerts list với 2 alert rows (Error rate 0.7% P95 latency 612ms) status=FIRING + nút Acknowledge + Resolve -->

Em Mai click **Acknowledge** trong /admin/alerts → ghi nhận đã thấy → ngừng spam Slack.

Sau khi fix → click **Resolve** + nhập root cause + link gap (nếu có) → đóng alert.

---

## 4. Daily routine 5 phút

```
08:30 — Mở /admin/monitoring
08:31 — Verify 4 metric đều 🟢
08:33 — Mở /admin/alerts xem có FIRING không
08:34 — Mở /admin/email-events kiểm tra Resend delivery rate >95%
08:35 — Done
```

Nếu phát hiện 🟡 Warning → file gap P2 cho ngày sau debug.

Nếu phát hiện 🔴 Critical → trigger incident response (xem [index §4](index.md)).

---

## 5. SLO targets per release

| Phase | Uptime | P95 latency | Error rate |
|---|---|---|---|
| Phase 1 BETA (Q2-Q3 2026) | ≥99.5% | ≤500ms | ≤0.5% |
| Phase 2 P3 medium (Q4 2026) | ≥99.8% | ≤300ms | ≤0.3% |
| Phase 3 P5 K-12 (Q1 2027+) | ≥99.9% | ≤200ms | ≤0.1% |

Tracking SLO breach trong `documents/04-quality/audits/ops-readiness/`.

---

## 6. Liên kết

- [Tổng quan Platform Admin](index.md)
- [Beta Approval](beta-approval.md)
- Ops runbook: [`documents/05-guides/operations/`](../../operations/)
- Performance audit: [`documents/04-quality/audits/performance-audit/`](../../../04-quality/audits/performance-audit/)

---

## 🆘 Cần hỗ trợ?

- 📧 Email nội bộ: [admin-support@kitehub.me](mailto:admin-support@kitehub.me)
- 🚨 Incident response: [`documents/05-guides/operations/incident-response-runbook.md`](../../operations/)
- 📊 Trạng thái beta: [/beta-status](/beta-status)
