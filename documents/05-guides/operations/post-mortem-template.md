# Post-Mortem Template — Incident RCA

**Usage:** Copy this file to `documents/04-quality/incidents/INC-YYYY-MM-DD-{shortname}.md` after incident resolution. Fill within 48h per [`incident-comms-runbook.md`](incident-comms-runbook.md) §6.

---

## Header

| Field | Value |
|---|---|
| **Incident ID** | INC-YYYY-MM-DD-{shortname} |
| **Title** | [1-line incident summary] |
| **Severity** | 🔴 Sev1 / 🟠 Sev2 / 🟡 Sev3 / 🔵 Maintenance |
| **Date** | YYYY-MM-DD |
| **Duration** | [HH:MM] - [HH:MM] ICT (~[X] phút) |
| **Status** | Resolved |
| **Incident Commander** | @handle |
| **Affected components** | [list per Instatus components] |
| **Affected tenants** | [count + scope] |
| **SLA breach?** | Yes / No (if yes, count toward 30-day rolling) |

---

## 1. Tóm tắt (Summary)

[1 đoạn ngắn — non-technical, user-facing language. Mô tả what happened + impact + resolution at high level. Tối đa 5 câu.]

Ví dụ:
> Vào lúc 14:23 ICT ngày 2026-05-15, tính năng tạo branding AI ngừng hoạt động đối với toàn bộ tenant trong 47 phút do timeout kết nối tới Ollama service. Đội ngũ đã restart Ollama container và confirm khắc phục lúc 15:10. Tổng cộng ~12 generation requests bị fail; tenants đã được retry tự động qua Outbox queue. Không có data loss.

---

## 2. Timeline

| Thời điểm | Sự kiện | Người |
|---|---|---|
| HH:MM | [Trigger event — alert fired / user reported / smoke test fail] | [handle/system] |
| HH:MM | IC acknowledged | @handle |
| HH:MM | Severity assessed: SevX | @handle |
| HH:MM | Initial incident posted to Instatus | @handle |
| HH:MM | [Diagnosis step 1] | @handle |
| HH:MM | Root cause identified: [1 sentence] | @handle |
| HH:MM | Fix deployed: [action] | @handle |
| HH:MM | Smoke test pass | @handle |
| HH:MM | Incident marked Resolved | @handle |
| HH:MM | Post-mortem draft started | @handle |

---

## 3. Phân tích nguyên nhân (Root Cause Analysis)

### 5-Whys

1. **Why did the incident occur?** [Surface symptom]
2. **Why did [step 1 cause] happen?** [Deeper layer]
3. **Why was that the case?** [Even deeper]
4. **Why?** [Architectural/process layer]
5. **Why?** [Root cause — ideally fixable]

### Root cause statement

[1-2 sentences. Be specific about technical layer + process gap.]

Ví dụ:
> Ollama container OOMKilled vì JVM heap cap GAP-408 không áp dụng cho ai-local profile (chỉ áp cho Java services). Process gap: Wave 37 Bucket D không include Ollama trong heap-cap scope vì assumed Ollama tự manage memory; production realities khác.

### Contributing factors

- [Factor 1 — secondary cause that amplified impact]
- [Factor 2]
- [...]

---

## 4. Tác động (Impact)

| Dimension | Impact |
|---|---|
| **Tenants affected** | [count + scope — all/subset/named] |
| **Users affected** | [estimated count] |
| **Requests affected** | [count fail / count total = X% error rate] |
| **Data loss?** | None / [describe] |
| **Revenue impact** | [if applicable Phase 1.5 PAID] |
| **SLA impact** | Within / breaches by [X]% |
| **External-facing severity** | Public / Internal-only |

---

## 5. What Went Well

- [Item 1 — process/tool/decision that helped resolve]
- [Item 2]
- [...]

Ví dụ:
- Instatus subscribers nhận update kịp thời mỗi 15 min trong suốt 47 phút sự cố — không có tenant complaint trực tiếp
- Outbox retry queue đảm bảo 12 failed requests đều được retry tự động sau khi Ollama restart — không cần manual replay
- Smoke test phát hiện vấn đề trong 3 phút sau deploy

## 6. What Went Poorly

- [Item 1 — gap/delay/process miss]
- [Item 2]
- [...]

Ví dụ:
- Detection delay 8 phút vì Ollama không có Grafana alert (only Java services covered Wave 37)
- IC mất 12 phút để xác định root cause — runbook thiếu Ollama-specific diagnostic steps
- Post-mortem template chưa có Ollama-specific failure scenarios

---

## 7. Action Items

| # | Action | Owner | Priority | Due | Tracking |
|---|---|---|:---:|---|---|
| 1 | [Concrete fix — code/infra/runbook change] | @handle | P0/P1/P2 | YYYY-MM-DD | GAP-XXX (file as gap if not exists) |
| 2 | [Process improvement] | @handle | P1/P2 | YYYY-MM-DD | Memory/skill/rule update |
| 3 | [Monitoring/alerting addition] | @handle | P0/P1 | YYYY-MM-DD | GAP-XXX |
| 4 | [Documentation update] | @handle | P2 | YYYY-MM-DD | This runbook §X |

**Quy tắc:** mọi action item PHẢI có owner + due date + tracking link. "TBD" = action sẽ bị quên.

---

## 8. Lessons Learned

[2-3 paragraph reflection. What patterns emerge across incidents? What architectural/process change would prevent this class of incident, not just this specific instance?]

Ví dụ:
> Class of incident: external dependency (Ollama, OpenAI API, payment processor) failure mode chưa được covered by Phase 1 BETA monitoring. Wave 37 GAP-115 monitoring scope ưu tiên Java services + database; non-Java external services bị skip. Pattern này có thể tái lặp với SES email + Cloudflare CDN + AWS S3.
>
> Root architectural fix: extend Wave 37 GAP-115 monitoring scope đến mọi external dependency với health-check endpoint + Grafana dashboard tile per dependency. Track follow-up gap.
>
> Process fix: post-mortem template thêm "Class of incident" reflection prompt để force pattern detection across incidents (vs treating each as isolated).

---

## 9. References

- Initial incident posting: [Instatus permalink]
- Status page history: https://status.kitehub.vn (per ADR-027)
- Related logs: [Grafana dashboard link / Loki query]
- Related deploys: [git commits in incident window]
- Sister runbook: [`incident-comms-runbook.md`](incident-comms-runbook.md)
- Related gaps: GAP-XXX, GAP-YYY

---

## 10. Distribution

- [ ] Posted to Instatus public history
- [ ] Emailed to subscribers (within 48h post-resolution)
- [ ] Filed in `documents/04-quality/incidents/`
- [ ] Action items filed as gaps trong `documents/04-quality/gaps/`
- [ ] Lessons learned cross-referenced trong relevant skills/rules/memory entries

---

## Log

- **2026-05-07:** Template created Wave 38 Bucket C. Pattern: Header + Summary + Timeline + RCA (5-Whys) + Impact + What Went Well/Poorly + Action Items + Lessons Learned + References + Distribution checklist. Vietnamese-first body với English structural keys.
