# ADR-036: Multi-branch defer Phase 2 — Phase 1 BETA filter invite P2 1-branch only

**Status:** ACCEPTED
**Date:** 2026-05-26
**Deciders:** @nguyenvankiet
**Reviewers:** (none — solo-dev mode per CLAUDE.md "decision context locked 2026-05-06")
**Related Gap(s):** GAP-754 (Multi-branch foundation Phase 2)
**Related Wave:** Wave beta-prep-1 Bucket H (coordinator inline spike per plan §3, Decision D3 locked)

## Context

Outside-in audit Wave beta-prep-1 (3-agent consensus 2026-05-26 — persona simulation + VN edu SaaS benchmark + failure-mode matrix) surfaced finding **P0 Multi-branch missing** trong Persona path: P2 Center Owner cohort (anh Tâm Sky Education + chị Hằng Quang Minh chains) cần quản lý 2-5 chi nhánh từ ngày đầu, blocked signup vì hệ thống mặc định 1-tenant = 1-branch.

Current state (verify-at-spawn 2026-05-26):
- Database schema: `tenants` table chứa 1 implicit "main branch" — không có `branches` table riêng.
- Domain logic: mọi service (enrollment / attendance / invoice / class) scope theo `tenant_id` đơn lẻ — không có `branch_id` discriminator.
- FAQ user manual `documents/05-guides/user-manual/anonymous/faq.md` q1.4 (Wave 79 Bucket F1 shipped 2026-05-14) cũ phát biểu sai: "Gói PRO 1 chi nhánh / PREMIUM 3 / ENTERPRISE không giới hạn" — claim chưa có code backing.

PDPL hard deadline 2026-07-01 (~5 tuần countdown) ưu tiên 5 PDPL compliance-min items + Security baseline + Beta invite mechanism. Adding `branches` schema + cross-branch dashboard + branch-scoped RLS = ~3-4 tuần engineering effort (per outside-in finding C-4). Không khả thi trong Phase 1 BETA scope.

## Decision

**Multi-branch foundation defer Phase 2 (post Phase 1 BETA gate ≥80 + 5 tenants live + 0 P0 incidents 2 tuần).**

Phase 1 BETA execution:
1. **Signup form filter** (Bucket F Agent 6 implement): field `Số chi nhánh` (number input) — if `> 1` → redirect `/waitlist?reason=multi-branch` với FAQ link explain Phase 2 roadmap; if `= 1` → continue normal signup flow.
2. **Beta cohort invite filter** (Bucket F Agent 6 implement): admin shortlist tool filters out multi-branch tenants; chỉ invite P2 1-branch cohort.
3. **FAQ q1.4 update** (Bucket H this ADR ships): replace existing misleading answer với honest "đang phát triển, sẽ ship Q3 2026" + waitlist link.
4. **Honest expectation-setting** (Bucket L coordinator inline): landing page + pricing page note multi-branch roadmap Q3 2026.
5. **Follow-up gap** (Bucket H this ADR ships): GAP-754 P1 track full multi-branch foundation scope cho Wave multi-branch-1 trong Phase 2.

Phase 2 trigger (when to revisit): per CLAUDE.md "Phase 2 trigger = Phase 1 gate met + counsel engaged + 4 sub-conditions" hoặc earlier nếu user pivot cohort priority.

## Consequences

### Positive

- **Phase 1 BETA scope tight + shippable trong ~3-4 tuần parallel** — không bị multi-branch effort blast radius (~3-4 tuần thêm)
- **Honest expectation-setting** — user thấy ngay tại signup form rằng multi-branch chưa support → tránh churn sau khi onboard
- **5 P2 1-branch cohort focus** — hand-holding mạnh hơn (per Bucket F2 5-tenant onboarding playbook)
- **PDPL deadline 2026-07-01 không bị threat** — multi-branch không phải PDPL prerequisite
- **Future Phase 2 foundation cleaner** — học từ Phase 1 BETA actual usage trước khi design `branches` schema (avoid over-engineering)

### Negative

- **Rejected ~50% P2 signups Phase 1 BETA** — anh Tâm Sky Education (3 branches) + chị Hằng Quang Minh (2 branches) PHẢI wait OR accept 1-branch limit. Per failure-mode matrix C-4 finding, ~50% P2 cohort have ≥2 branches.
- **Waitlist UX risk** — nếu user thấy `Number of branches > 1` rồi redirect waitlist mà không explain rõ → bounce cao. Mitigation: clear FAQ Q&A + email template "We'll notify when multi-branch ships Q3 2026".
- **Marketing copy risk** — competitor (Misa / Easy Edu) advertise multi-branch as standard feature. KiteHub Phase 1 BETA pricing/landing PHẢI làm rõ "đa chi nhánh ship Q3 2026" để tránh false advertising.
- **Tech debt accumulated** — eventually phải refactor tenant→branch hierarchy. Sớm-or-muộn cost similar; defer to Phase 2 = appropriate cost-benefit tradeoff.

### Neutral

- Filter logic ở signup form là FE-side validation + BE-side reject (defense-in-depth)
- Waitlist email collection có thể reuse existing beta-request mechanism (`beta_request` table + admin `/admin/beta-requests` UI per Wave 91-92 era)
- ADR-036 supersedes nothing — ADR-002 (Academic year structure) + ADR-003 (Role hierarchy) đều assume 1-tenant scope; multi-branch sẽ extend chứ không thay

## Alternatives Considered

### Alternative A: Ship full multi-branch trong Phase 1 BETA

**Pros:**
- P2 cohort không reject; ~100% addressable market trong Phase 1
- Competitor parity (Misa / Easy Edu multi-branch ngày đầu)
- Tránh phải refactor sau

**Cons:**
- Effort ~3-4 tuần engineering thêm → vượt PDPL deadline 2026-07-01 (~5 tuần buffer eaten up)
- Schema design `branches` table cần consult business analyst (chi nhánh có dùng chung GV / lớp / học sinh không? Per branch quota? Cross-branch transfer logic?) — risk over-engineer trước khi học từ Phase 1 actual usage
- 5 P2 1-branch hand-holding scope dilute thành 10-15 P2 1-2-3-branch cohort scope → quality giảm
- Block Phase 1 BETA gate (≥80 quality + 5 tenants live) → Phase 2 trigger không fire trong target window 2026-Q3

**Rejected because:** PDPL deadline 2026-07-01 không lùi được + Phase 1 BETA gate cần ship trước Phase 2 unlock. Multi-branch không phải PDPL prerequisite.

### Alternative B: Accept all signups, multi-branch "best effort" trong Phase 1

**Pros:**
- Không reject signup; UX friendly
- Marketing không phải làm rõ defer

**Cons:**
- Misleading: hệ thống KHÔNG multi-branch support nhưng accept multi-branch signup → user vào thấy chỉ có 1-branch → bounce cao + bad review
- Tech debt accumulated silently — Phase 2 refactor sẽ phải migrate "fake multi-branch" data (mỗi P2 owner phải tự maintain Excel ngoài system)
- Compliance risk: invoice / audit log scope theo tenant_id mà không có branch_id → user complaint khi reconcile cross-branch

**Rejected because:** Misleading UX > tránh churn-tại-signup; honest expectation-setting tốt hơn.

### Alternative C: Defer to Phase 1.5 (post-beta paid tier)

**Pros:**
- Compromise giữa A và B
- Multi-branch available trước Phase 2 (sớm hơn ~2-3 tuần)

**Cons:**
- Phase 1.5 chỉ ~2-3 tuần (per `release-1-plan-2026.md`); không đủ để design + ship multi-branch foundation cleanly
- Risk: Phase 1.5 trở thành "Phase 1 patch dump" thay vì paid tier prep
- Per CLAUDE.md Phase progression locked Phase 1 (P1+P2) → Phase 2 (+P3) → Phase 3 (+K-12); không nên fragment scope

**Rejected because:** Phase 1.5 scope đã ưu tiên paid tier + Premium plan; multi-branch không fit window này.

## Implementation Notes

### Bucket H artifacts (this ADR + paired ship Wave beta-prep-1)

1. **ADR-036** (this file) — decision record
2. **FAQ q1.4 update** — `documents/05-guides/user-manual/anonymous/faq.md` swap misleading "PRO 1 / PREMIUM 3 / ENTERPRISE unlimited" → honest "Phase 1 BETA chỉ hỗ trợ trung tâm 1 chi nhánh; tính năng đa chi nhánh sẽ ship Q3 2026 trong Phase 2. Đăng ký nhận thông báo qua [waitlist]"
3. **GAP-754** P1 — Multi-branch foundation Phase 2 follow-up gap với scope acceptance criteria
4. **adrs-index.csv row** — ADR-036 entry (paired same PR; also resolves pre-existing merge conflict markers lines 38-46 từ Wave beta-readiness-4 + Wave 88 merge)

### Bucket F agent owns (signup form filter)

Per Wave beta-prep-1 plan §3 Bucket F.7 + Bucket H step 3 — Agent 6 (worktree-f-g-invite-support) implements:
- Signup form field `Số chi nhánh` (required number input, min=1)
- FE-side validation: nếu `> 1` → block submit + redirect `/waitlist?reason=multi-branch`
- BE-side validation: API endpoint reject HTTP 400 với error code `MULTI_BRANCH_DEFER_PHASE_2` (defense-in-depth)
- Waitlist mechanism: `waitlist_requests` table (reuse `beta_requests` schema OR new V62 migration nếu cần distinct cleanup lifecycle)

### Bucket L coordinator inline owns (landing + pricing copy)

Per Wave beta-prep-1 plan §3 Bucket L — coordinator inline ships:
- Landing page hero copy: target audience "trung tâm 1 chi nhánh" explicit Phase 1 BETA scope
- Pricing page: defer "Đa chi nhánh" feature row tới Phase 2 column với "Q3 2026" badge
- FAQ Zalo expectation-setting + multi-branch waitlist FAQ entry

### Rollback plan

Nếu Phase 1 BETA cohort feedback overwhelmingly demand multi-branch trong < 2 tuần ship:
- Re-evaluate ADR via new ADR (e.g., ADR-040 supersede ADR-036)
- Spin Wave multi-branch-1 thẳng vào Phase 1.5 scope (override Phase progression)
- Trade-off: Phase 1.5 paid tier scope shrink; user-pivot decision required

### Success criteria (Phase 2 trigger evaluation)

ADR-036 deemed correct decision khi:
- Phase 1 BETA gate met trong target window (≥80 quality + 5 P2 1-branch tenants live + 0 P0 incidents 2 tuần)
- Waitlist signup count ≥ 10 (signal multi-branch demand real cho Phase 2 prioritization)
- Phase 1 actual usage data informs `branches` table schema design (avoid premature optimization)

## References

- **Wave plan:** `documents/03-planning/waves/wave-2026-05-26-beta-prep-1-mega.md` §3 Bucket H + Decision D3
- **Outside-in audit:** 3-agent consensus 2026-05-26 (persona simulation + VN edu SaaS benchmark + failure-mode matrix) — C-4 P0 Multi-branch missing
- **Sister ADRs:**
  - ADR-002 — Academic year structure (single tenant scope; multi-branch sẽ extend)
  - ADR-003 — Role hierarchy (P1/P2/P3 personas; multi-branch sẽ thêm `BRANCH_MANAGER` role Phase 2)
  - ADR-025 — AWS-only deploy Phase 1 BETA (cost constraint = defer scope expansion)
- **Related gap:** GAP-754 — Multi-branch foundation Phase 2 (paired same PR)
- **Related rules:**
  - `meta-gap-priority.md` §3 — Phase progression discipline
  - `outside-in-coverage-trigger.md` v1.1.0 — 3-agent audit triggered ADR scope
  - `vn-localization-audit-checklist.md` v1.0.0 — VN cultural awareness applied to waitlist email tone
  - `wave-closure-scope-completeness.md` v1.0.0 — ADR ship paired GAP-754 follow-up cho deferred scope
- **CLAUDE.md** §"CURRENT PHASE" — Phase progression locked 2026-05-06

## Log

- **2026-05-26** — Initial proposal + ACCEPTED same-day (solo-dev mode per CLAUDE.md; outside-in audit 3-agent consensus C-4 P0 finding triggered ADR scope; Decision D3 locked trong wave plan PR #1870 merged 2026-05-26)
