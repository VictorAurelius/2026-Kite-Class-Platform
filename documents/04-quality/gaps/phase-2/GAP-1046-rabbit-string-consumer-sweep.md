# GAP-1046: Sweep 4 `@RabbitListener(String)` consumer còn lại — Jackson-converter mismatch (DEFER từ GAP-1045)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-07 (cross-flow sweep từ GAP-1045)
**Affects:** KC-12 reschedule flow + email service consumers

## Problem

GAP-1045 fix lớp wire-format mismatch (`@RabbitListener(String)` + manual parse dưới `Jackson2JsonMessageConverter` factory + producer raw application/json bytes → "Fatal message conversion error" → message drop) cho 2 consumer KC-1 critical path. Cross-flow sweep (per `cross-flow-bug-class-sweep.md`) lộ 4 consumer khác cùng signature class:

| Site | Flow |
|---|---|
| `kiteclass ClassRescheduledNoOpConsumer:39 handle(String)` | KC-12 reschedule |
| `kiteclass ClassRescheduledEmailConsumer:65 handle(String)` | KC-12 reschedule → email |
| `kitehub-email ClassRescheduledEmailService:69 handle(String)` | email service |
| `kitehub-email EmailEventListener:97 onEmailEvent(String)` | email service |

DEFER (không phải KC-1 path). Email flows được cho là đã chạy trong prior walks → có thể dùng converter/content-type khác (vd SimpleMessageConverter ở kitehub-email factory, hoặc producer gửi text/plain) → KHÔNG đổi mù tránh phá code đang chạy.

## Proposed Fix

Per-site: (1) đọc listener container factory converter của service đó; (2) đọc producer gửi với content-type gì + convertAndSend vs raw Message; (3) nếu cùng broken class (Jackson factory + raw application/json bytes + String param) → convert sang Message-param decode như GAP-1045; (4) nếu đã đúng (typed param / SimpleMessageConverter / text-plain) → mark EXEMPT + document.

## Acceptance Criteria

- [ ] 4 site verify producer content-type + factory converter
- [ ] Site cùng broken class → Message-param fix + test
- [ ] Site đúng → EXEMPT documented inline

## Related

- Parent: GAP-1045 (KC-1 saga fix)
- Sister: GAP-925 (wire-format origin)
