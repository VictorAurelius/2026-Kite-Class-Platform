# GAP-363: kiteclass-student kit polish — `payments.html` persona-AC violation + 4 partials

**Status:** 🟡 PARTIAL 2026-05-06 — Wave 22 Bucket A polish shipped (Option C parent-trigger + 4 polish items + kit floor restored 100); kit avg ≥105 AC NOT met (self-rescore ~102.5 < 105 threshold). External re-audit + delta-to-target tracked in **GAP-363b**. Per `gap-done-discipline.md` §3 PARTIAL exit ramp — coordinator downgrade from agent-claimed DONE because §2 AC threshold genuinely unmet.
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

- [x] `payments.html` Option A/B/C implemented + new score ≥95 — **Option C parent-trigger workflow shipped** (self-rescore ~108, lifted from 92)
- [x] `my-classes.html` chip-parity fixed — chip "Yêu thích (5)" now matches sibling chips parens convention
- [x] `assignments.html` tab counts reconciled — tab "Chờ nộp (12)" now matches subtitle "12 chờ nộp"
- [x] `grade-detail.html` TT 22/2021 info discoverable — clickable info-icon tooltip in section heading; expanded tooltip discloses 3-tier weighting + formula
- [x] `profile.html` Học lực pill linkable — wrapped in `<a href="grades.html">` with chevron + aria-label
- [ ] Re-score affected screens — kit avg ≥105 AC genuinely unmet. Self-rescore ~102.5 (was 100.4, +2.1) below threshold. Per `feedback_audit_calibration.md` self-rescore overstates 15-20 pts vs specialist; external would likely be ~85-90. Kit floor ≥95 restored (lowest screen 100), but avg AC explicitly ≥105 → tracked in **GAP-363b** for external re-audit + further polish if delta needed
- [x] GAP-269 unblocked for Track 2 production port — child-protection persona violation cleared (P0 portion); payments.html now AC-FIN-001-compliant. Track 2 port remains technically clear to start (no LEGAL block), but kit avg <105 means port screens may need polish parallel with port work — tracked in GAP-363b
- [x] Cross-link added in `ui_kits/kiteclass-student/README.md` to this polish gap — "Polish history" section appended with full before/after rescore table + 4-layer cross-references

## Related

- Review report: `documents/04-quality/audits/ui-review/2026-05-05-round-3-kiteclass-student-review.md`
- Parent gap: GAP-348 (Wave 20 Round 3 review) — flips PARTIAL on this filing
- Persona AC source: `documents/00-brd/persona-criteria/secondary/student-in-P2.md` AC-FIN-001 + AC-ONBOARD-003
- Tier-1 doc absence: GAP-365 (file `S-student.md` Tier-1 AC doc — surfaced by Bucket A)
- Track 2 port (BLOCKED): GAP-269

## Effort estimate

~12-14h total (3-6h P0 + ~6h polish + verification). Single agent bucket; can pair-wave with GAP-364 (kitehub-admin polish) as 2-bucket marketing-storytelling-style wave-pack.

## Log

- **2026-05-06 (closure correction):** Coordinator downgrade Status 🟢 DONE → 🟡 PARTIAL per `gap-done-discipline.md` §3 PARTIAL exit ramp. Bucket A agent ticked AC #6 "kit avg ≥105" with self-rescore ~102.5 + rationale "calibration band" — this is exactly the GAP-235 silent-deferral anti-pattern §2 was designed to prevent. AC threshold was explicit (≥105) and genuinely unmet. Kit floor restored, P0 child-protection violation cleared, polish items real — but avg target needs external re-audit + possibly further polish. Filed **GAP-363b** for that follow-up. PR #811 work itself stands; only Status field corrected.
- **2026-05-06:** Wave 22 Bucket A polish PR shipped. Option C parent-trigger workflow applied to `payments.html` per AC-FIN-001 (child-protection): primary CTA flipped "Đóng học phí ngay" → "Yêu cầu ba/mẹ đóng" with full screen rebuild — visible amber disclaimer block citing AC-FIN-001 (replaces invisible HTML-comment-only L9), state-machine sketch in HTML header (DRAFT → REQUEST_SENT → PAID per `parent-portal/rules.md` BR-PARENT-PORTAL-* read-mode scope guard), mock state chip "Đã gửi yêu cầu — chờ ba/mẹ xác nhận", history reframed "Ba/mẹ đã đóng" (read-only past payments), "Cách thức thanh toán" 3-step explainer replaces "Phương thức nhanh" picker. WCAG self-measured: disclaimer block 7.8:1 AAA, body 16.5:1 AAA. 4 polish items shipped: my-classes "Yêu thích (5)" chip parens; assignments tab counts (12/8/24) reconciled to subtitle; grade-detail TT 22/2021 weighting moved to clickable info-icon tooltip with `aria-expanded`/`aria-controls` toggle; profile "Học lực Giỏi" pill linked to `grades.html` with chevron + descriptive aria-label. README "Polish history" section appended with before/after rescore table — kit avg estimated ~102.5/128 (best-effort self-rescore acknowledging calibration band). Kit floor ≥95 fully restored (lowest 100). Verification artifact: README "Polish history" §"Estimated new kit avg" + "Acceptance gate restoration" table. GAP-269 unblocked for Track 2 production port.
- **2026-05-05:** Filed by Wave 20 Bucket C closure (this PR) per `audit-to-gap-pipeline.md` + Bucket A external review findings. P0 child-protection persona violation makes this **BLOCKING** for Track 2 production port (GAP-269) per gap §AC last bullet.
