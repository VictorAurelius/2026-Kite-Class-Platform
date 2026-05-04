---
title: P1 Solo Teacher — Persona Review Round 1
status: draft
persona_id: P1
persona_name: Solo Teacher (Gia sư tự do)
tier: 1 Primary
review_date: 2026-05-04
reviewer: Wave 17 Bucket A Agent (autonomous)
ac_source: documents/00-brd/persona-criteria/P1-solo-teacher.md
gap_range_reserved: GAP-286..295
---

# P1 Solo Teacher — Persona Review Round 1 (2026-05-04)

**Status:** 🟡 DRAFT — closure PR (Wave 17 closure step) sẽ flip sang `approved`
**Wave:** 17 — Persona Review Round 1 (GAP-152)
**Bucket:** A (P1)
**Methodology:** [`persona-based-business-review.md`](../../../.claude/skills/quality/persona-based-business-review.md)
**Source AC:** [`P1-solo-teacher.md`](../persona-criteria/P1-solo-teacher.md) — 29 ACs across 6 categories
**Filing pipeline:** [`audit-to-gap-pipeline.md`](../../../.claude/rules/audit-to-gap-pipeline.md)

---

## §0 Executive Summary

| Metric | Value |
|---|---|
| Total ACs reviewed | 29 |
| PASS | TBD (filled in §3) |
| PARTIAL | TBD |
| FAIL | TBD |
| Coverage score | TBD/100 |
| Verdict | TBD |
| New gaps filed | TBD (range GAP-286..295 reserved) |

**Top 3 critical findings:** TBD (filled in §4)

**Priority-reordering recommendation:** TBD (filled in §5)

---

## §1 Scenario at scale

Theo `personas-catalog.md` §P1 + AC §0:

- **Actor:** 1 gia sư tiếng Anh part-time tại TPHCM, 30 học sinh, 3 courses (English-Beginner / English-Intermediate / English-IELTS)
- **Hierarchy:** Flat — 1 teacher = owner = operator = billing person. Không có admin staff. Phụ huynh KHÔNG đăng nhập (chỉ là contact qua Zalo/SMS).
- **Device profile:** iPhone (Safari) làm primary; Android (Chrome) làm backup. Không có laptop riêng cho công việc dạy thêm.
- **Usage pattern:** Mobile-first 70%+ thời gian. Peak hours weekday 17:00-21:00 + cuối tuần. Lessons 1-on-1 hoặc nhóm ≤5 students.
- **Tier:** FREE (5-15 students) → PRO (15-50 students). Sub-200K VND/month preferred. Không bao giờ chạm PREMIUM/ENTERPRISE.
- **Communication channel:** Zalo + SMS primary; email secondary; phụ huynh nhận receipt PDF qua Zalo link.
- **Billing:** Cash (60%) + bank transfer (40%). Không cần e-invoice (mã số thuế). Per-session pricing (200K/buổi × 8 buổi = 1.6M/tháng).

**Critical concerns** (từ AC §0):
1. Setup ≤30 phút onboarding
2. Mobile-friendly toàn diện
3. Sub-200K VND/month FREE tier useable
4. Simple invoicing (PDF receipt qua Zalo, không e-invoice)
5. Zalo/SMS-first (không email-first)

---

## §2 Journey walk-through

End-to-end role-play: discovery → signup → provisioning → daily ops → financial → communication → edge case → termination.

### 2.1 Discovery

Solo teacher tìm thấy KiteClass qua:
- Google search "phần mềm quản lý lớp học gia sư"
- Facebook group giáo viên dạy thêm TPHCM
- Word-of-mouth từ giáo viên khác

→ Landing page (KiteHub marketing site) cần (a) explain "miễn phí cho ≤15 students", (b) demo screenshots mobile-first, (c) pricing minh bạch trước khi signup. Nếu landing toàn enterprise/multi-tenant copy → bounce ngay.

### 2.2 Signup + provisioning

(AC-ONBOARD-001..004) — Mở landing trên iPhone Safari → "Sign up" → email + phone + tên → role "Solo Teacher" → OTP qua Zalo/SMS → confirm → wizard branding (skip được) → dashboard ready.

Target: ≤10 phút từ click "Sign up" tới dashboard. KHÔNG force AI branding flow.

### 2.3 Daily ops

(AC-OPS-001..008) — Schedule lesson, recurring class (Tuesday-Thursday 19:00-20:30), mark attendance trên phone (≤2 phút cho 10 students), nhập grades, track student progress, reschedule, cancel, quick-add student mid-course.

Tất cả thao tác phải work hoàn toàn trên mobile. Tap-target ≥44pt. Offline-capable cho mark attendance.

### 2.4 Financial

(AC-FIN-001..005) — Per-session pricing (200K/buổi). PDF receipt gửi qua Zalo link. Monthly income summary (thu / outstanding / chi). Reminder cho học sinh chưa đóng. KHÔNG hiển thị payroll/teacher commission menu (irrelevant).

### 2.5 Communication

(AC-COMM-001..004) — Zalo/SMS template predefined. Auto-reminder 1h trước class. Cancel/reschedule notify ngay. KHÔNG có parent portal (parent là contact, không phải user).

### 2.6 Edge cases

(AC-EDGE-001..005) — No-show vs excused-absent differentiation. Late-cancel <2h policy (charge full/partial/waive). Payment dispute audit log. Offline attendance sync. Account survive mobile-uninstall.

### 2.7 Exit / termination

(AC-EXIT-001..003) — Student progress export PDF. Account pause/resume (3-state lifecycle: Active → Paused 30d free → Archived). Self-service export toàn bộ data (xlsx + PDF zip).

---

## §3 AC Scoring (29 ACs)

Format: AC-ID | Status (PASS/PARTIAL/FAIL) | Evidence | Linked gap

### 3.1 Onboarding (4 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-ONBOARD-001 | TBD | TBD | — |
| AC-ONBOARD-002 | TBD | TBD | — |
| AC-ONBOARD-003 | TBD | TBD | — |
| AC-ONBOARD-004 | TBD | TBD | GAP-053 (related) |

### 3.2 Daily Operations (8 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-OPS-001 | TBD | TBD | — |
| AC-OPS-002 | TBD | TBD | — |
| AC-OPS-003 | TBD | TBD | — |
| AC-OPS-004 | TBD | TBD | GAP-055 (out-of-scope) |
| AC-OPS-005 | TBD | TBD | — |
| AC-OPS-006 | TBD | TBD | GAP-063 |
| AC-OPS-007 | TBD | TBD | GAP-063 |
| AC-OPS-008 | TBD | TBD | GAP-051 (related) |

### 3.3 Financial / Admin (5 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-FIN-001 | TBD | TBD | GAP-185 (related) |
| AC-FIN-002 | TBD | TBD | GAP-185 (related) |
| AC-FIN-003 | TBD | TBD | — |
| AC-FIN-004 | TBD | TBD | GAP-063 |
| AC-FIN-005 | TBD | TBD | GAP-057 (out-of-scope) |

### 3.4 Communication (4 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-COMM-001 | TBD | TBD | GAP-063 |
| AC-COMM-002 | TBD | TBD | GAP-063 |
| AC-COMM-003 | TBD | TBD | GAP-063 |
| AC-COMM-004 | TBD | TBD | GAP-052 (out-of-scope) |

### 3.5 Edge Cases (5 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-EDGE-001 | TBD | TBD | — |
| AC-EDGE-002 | TBD | TBD | — |
| AC-EDGE-003 | TBD | TBD | — |
| AC-EDGE-004 | TBD | TBD | — |
| AC-EDGE-005 | TBD | TBD | — |

### 3.6 Exit / Termination (3 ACs)

| AC | Status | Evidence | Gap |
|---|:---:|---|---|
| AC-EXIT-001 | TBD | TBD | — |
| AC-EXIT-002 | TBD | TBD | — |
| AC-EXIT-003 | TBD | TBD | GAP-051 (related) |

---

## §4 Top Critical Findings

TBD — filled after §3 scoring.

---

## §5 Priority-Reordering Recommendation

TBD — filled after §3 scoring.

---

## §6 New Gaps Filed (range GAP-286..295)

TBD — list of new gap IDs filed during this review.

---

## §7 Coverage Calculation

```
Coverage % = (PASS_count + 0.5 × PARTIAL_count) / total × 100
```

| Tier | Cutoff | Verdict |
|---|---|---|
| ≥85% | ✅ Production-ready for this persona |
| 60-84% | ⚠️ Partially supported (defer GA) |
| 30-59% | 🔴 Major gaps (not production-ready) |
| <30% | ❌ Persona NOT viable |

**Result:** TBD/100 → TBD verdict.

---

## §8 Methodology Notes

- State-check approach: cho mỗi AC, grep code paths trong `kiteclass-frontend/`, `kiteclass-core/`, `kitehub-*` để xác nhận PASS/PARTIAL/FAIL. KHÔNG self-score from imagination.
- Mobile-first emphasis: AC nói "trên mobile" → check responsive CSS + viewport meta + tap-target sizes.
- Out-of-scope ACs (AC-COMM-004 parent portal, AC-FIN-005 payroll, AC-OPS-004 MOET report card) scored as PASS-by-design IF system correctly hides feature theo tier/role gate; FAIL nếu force surface vào solo UX.
- Banned phrases (per `gap-done-discipline.md`): KHÔNG dùng "deferred", "manual run", "out-of-scope" trong bất kỳ Status DONE flip — review report là draft, không flip GAP-152.

---

## §9 Log

- **2026-05-04** (skeleton): Wave 17 Bucket A Agent created. Filling §3 + §4 + §5 + §6 incrementally with commit-frequently mandate (3/4 prior agents in this session were killed silently mid-flight).
