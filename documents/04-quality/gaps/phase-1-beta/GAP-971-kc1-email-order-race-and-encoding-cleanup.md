# GAP-971: Email order race + VN encoding overflow + first-login redirect ambiguous (hygiene cluster)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Mixed
**Found:** 2026-06-04 (Wave flow-kh3 KC-1 pre-walk audit — 3-agent outside-in consensus)
**Affects:** KC-1 (Hygiene cluster — UX polish)
**Defer-to:** After Wave flow-kh3 finish

## Problem

Cluster 3 hygiene findings:

1. **Email order race** (matrix A4×E6×EC1): Owner signup completes → tenant-ready email queued → owner immediately invites staff → invite-staff email queued. Outbox FIFO not guaranteed across topics → tenant-ready có thể arrive AFTER invite-staff (confusing).
2. **VN encoding overflow risk** (matrix A6×E2×EC7): `@Size(min=2, max=200)` operates on `String.length()` (char count). VARCHAR(200) DB column. If VN all-diacritic name → char count OK but worth verifying VARCHAR semantic (character vs byte).
3. **First-login redirect ambiguous** (matrix A5×E1×EC5): `OWNER` role redirect default not consistent; depending on FE buildtime, owner may land `/admin` (no permission) instead of `/dashboard`. Wave flow-kh2 G2 walk surfaced this.

Surfaced: matrix A4×E6×EC1 + A6×E2×EC7 + A5×E1×EC5.

## Proposed Fix

(1) Sequence tenant-ready BEFORE invite-staff via topic-level FIFO key (tenantId) or explicit ordering. (2) Verify `instances` table column type ≥ VARCHAR(512) — extend if needed. (3) Owner first-login redirect explicitly `/dashboard` (not `/admin`) — FE redirect logic + Playwright spec lock behavior.

## Acceptance Criteria

- [ ] Walk owner signup → invite-staff: MailHog UI shows tenant-ready arrived first
- [ ] `psql -c "\\d instances"` shows `database_url` + `organization_name` column types fit VN 200-char names
- [ ] Walk owner first-login → URL = `/dashboard`, not `/admin` or `/login`

## Related

- Discovered in: 3-agent outside-in audit 2026-06-04
- Audit artifact: persona-review/2026-06-04-pre-walk-kc1-failure-mode-matrix.md (multiple cells)
- Sister: Wave flow-kh2 G2 walk
- Flow Verification Campaign §4 row KC-1
