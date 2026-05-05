# GAP-363: kiteclass-student kit polish — `payments.html` persona-AC violation + 4 partials

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (BLOCKING Track 2 port — child-protection AC-FIN-001 violation per `secondary/student-in-P2.md`)
**Domain:** Frontend / Design System / Persona compliance
**Found:** 2026-05-05 (Wave 20 Bucket A external review)
**Affects:** `documents/02-architecture/design-system/ui_kits/kiteclass-student/screens/payments.html` + 4 partial screens; downstream blocks **GAP-269** (Track 2 port)

## Problem

External /128 review (Wave 20 Bucket A — `audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md`) scored kit avg **100.4/128** (delta -15.6 vs self-report 116, calibration band ✓). Verdict: **APPROVE WITH POLISH** — 1 P0 finding + 4 partials block Track 2 production port.

### P0 finding (blocking)

`payments.html` (score 92/128, below kit floor 95) has **child-protection persona violation**: hero "2.400.000đ" + button "Đóng học phí ngay" + aria-label "Đóng học phí 2 triệu 400 nghìn đồng" — accessible to S. Student persona via bottom-nav. Per `documents/00-brd/persona-criteria/secondary/student-in-P2.md` AC-FIN-001 (lines 117-122):

> Student xem fee status read-only ... KHÔNG có button 'Pay' cho student ... vi phạm tuổi pháp lý ký

K-12 students (10-15 tuổi per AC-ONBOARD-003) cannot legally execute payment commitments. Disclaimer in HTML comment L9 ("older student persona — vocational / university tutoring contexts") is invisible to user; segmentation logic missing.

### 4 Partials (Track 2 port spec ambiguities)

- `assignment-detail.html` — saved-draft scope unclear (per-assignment vs global)
- `login.html` — AC-EDGE-001 "Student quên password — parent reset" workflow MISSING; "Quên mật khẩu?" link L62 loops to login.html
- `notifications.html` — parent-kép visualization absent (when both parents in account)
- `grade-detail.html` — K-12 scope vs `secondary/student-in-P2.md` Tier-2 doc scope mismatch

## Current State (verified 2026-05-05 via Bucket A report)

| Screen | Score /128 | Status | Action |
|---|---:|---|---|
| `payments.html` | 92 | ⭐ rebuild | P0 FIX (3 alternatives below) |
| `today.html` | 101 | ⭐⭐⭐ good | OK |
| `my-classes.html` | 99 | ⭐⭐ borderline | Polish chip-parity ("Yêu thích" needs count) |
| `class-detail.html` | 100 | ⭐⭐⭐ good | OK |
| `assignments.html` | 100 | ⭐⭐⭐ good | Tab counts (4/8/24) ≠ subtitle (12/8/24) — fix |
| `assignment-detail.html` | 102 | ⭐⭐⭐ good | Saved-draft scope clarification → port spec |
| `grades.html` | 103 | ⭐⭐⭐ good | OK |
| `grade-detail.html` | 100 | ⭐⭐⭐ good | TT 22/2021 weighting more discoverable |
| `attendance.html` | 102 | ⭐⭐⭐ good | OK |
| `notifications.html` | 102 | ⭐⭐⭐ good | Parent-kép visualization → port spec |
| `profile.html` | 100 | ⭐⭐⭐ good | "Học lực Giỏi" pill linkable to grades.html |
| `login.html` | 100 | ⭐⭐⭐ good | AC-EDGE-001 parent-reset workflow MISSING |
| `empty-states.html` | 104 | ⭐⭐⭐ good | OK |

## Proposed Fix

### P0 — `payments.html` (3 alternatives, pick ONE)

| Option | Description | Effort |
|---|---|---|
| **A** | Hide "Đóng học phí" button when persona segment K-12 (`age < 18`) | ~2h |
| **B** | Move screen to `vocational/` namespace + remove from S. Student bottom-nav for K-12 | ~3h |
| **C** | Replace button with "Yêu cầu ba/mẹ đóng" parent-trigger workflow per AC-FIN-001 (preferred long-term) | ~6h |

Recommend C — aligned with parent-kép visualization (notifications partial) + complete child-protection compliance.

### Polish (parallel-safe with P0)

- `my-classes.html`: chip "Yêu thích" add count parens (5 min)
- `assignments.html`: reconcile tab counts vs subtitle (1h)
- `grade-detail.html`: TT 22/2021 weighting info-icon (2h)
- `profile.html`: link "Học lực Giỏi" pill → grades.html (1h)

### Track 2 port spec clarifications (defer to GAP-269 spec phase, NOT this gap)

- assignment-detail saved-draft scope (per-assignment vs global)
- login parent-reset workflow (AC-EDGE-001)
- notifications parent-kép visualization

## Acceptance Criteria

- [ ] `payments.html` Option A/B/C implemented + new score ≥95
- [ ] `my-classes.html` chip-parity fixed
- [ ] `assignments.html` tab counts reconciled
- [ ] `grade-detail.html` TT 22/2021 info discoverable
- [ ] `profile.html` Học lực pill linkable
- [ ] Re-score affected screens via `quality/ui-review-prototype` skill — kit avg ≥105 (was 100.4)
- [ ] GAP-269 unblocked for Track 2 production port
- [ ] Cross-link added in `ui_kits/kiteclass-student/README.md` to this polish gap

## Related

- Review report: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md`
- Parent gap: GAP-348 (Wave 20 Round 3 review) — flips PARTIAL on this filing
- Persona AC source: `documents/00-brd/persona-criteria/secondary/student-in-P2.md` AC-FIN-001 + AC-ONBOARD-003
- Tier-1 doc absence: GAP-365 (file `S-student.md` Tier-1 AC doc — surfaced by Bucket A)
- Track 2 port (BLOCKED): GAP-269

## Effort estimate

~12-14h total (3-6h P0 + ~6h polish + verification). Single agent bucket; can pair-wave with GAP-364 (kitehub-admin polish) as 2-bucket marketing-storytelling-style wave-pack.

## Log

- **2026-05-05:** Filed by Wave 20 Bucket C closure (this PR) per `audit-to-gap-pipeline.md` + Bucket A external review findings. P0 child-protection persona violation makes this **BLOCKING** for Track 2 production port (GAP-269) per gap §AC last bullet.
