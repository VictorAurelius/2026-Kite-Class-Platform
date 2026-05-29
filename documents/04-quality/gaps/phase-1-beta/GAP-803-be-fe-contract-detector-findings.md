---
audience: dev
---

# GAP-803 — Findings từ GAP-802 BE↔FE contract-drift detectors

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Mixed (Backend link-builder + FE route + env config)
**Found:** 2026-05-28 (GAP-802 detectors #2 + #5 chạy lần đầu trên main `a0cb5b47`)
**Phase:** phase-1-beta
**Affects:** Password-reset email flow (KiteHub), local-dev email testing (3 flows)

## Problem

GAP-802 detector chạy thật trên main + sweep path-scope đã bắt được findings (BE↔FE contract drift mà không test nào assert):

### Finding 0 — Detector path-scope bug (RESOLVED trong GAP-802 PR #1958)
Detector #2 + rule #4 ban đầu trỏ `kitehub/kiteclass-frontend` (KHÔNG tồn tại — KiteClass FE thật ở `kiteclass/kiteclass-frontend`) → #2 không scan KiteClass FE. Hệ quả `/reset-password` báo FALSE POSITIVE (route có thật ở `kiteclass/kiteclass-frontend/(auth)/reset-password/page.tsx`). **Đã fix trong #1958** (sửa path ở `check-be-fe-url-contract.sh` + `fe-build-local-verify.md` + `rules-index.csv` + `output-review-mandate.md`); #2 giờ exit 0 clean, CI flipped WARN→HARD STOP.

**Còn lại:** `vn-localization-audit-checklist.md` §8 + `rules-index.csv:85` cùng dùng path sai `kitehub/kiteclass-frontend` (agent copy pattern từ đây) → cần fix cùng class (PATCH bump rule đó).

### Finding 2 — 3 env var prod-domain default thiếu local override (cơ chế #5)
`audit-env-coverage.sh` CHECK B (WARN) báo 3 var default về prod domain, không có override trong `kitehub/docker-compose.kitehub.yml` → email gửi ở local nhúng link prod → dead-link khi test local (đúng class GAP-801 part 3):

| Var | Default | File |
|---|---|---|
| `KITEHUB_STAFF_INVITATION_BASE_URL` | `https://kitehub.me` | `StaffInvitationController.java:102` |
| `RESEND_FROM_EMAIL` | `no-reply@kitehub.me` | `ResendEmailService.java:67` |
| `PARENT_PORTAL_REDEEM_BASE_URL` | `https://app.kiteclass.vn/parent-invite/` | `kiteclass-core application.yml:292` |

→ Thêm local override vào `docker-compose.kitehub.yml` env block (vd `http://localhost:3000`), HOẶC thêm vào `ACCEPTABLE_PROD_DOMAINS` nếu cố ý (vd `RESEND_FROM_EMAIL` có thể luôn dùng prod sender — cần rationale + `production-env-config-registry.md` row).

## Acceptance Criteria

- [x] ~~`/reset-password`~~ — FALSE POSITIVE (detector path-scope bug), resolved trong #1958; CI flipped HARD STOP
- [ ] Fix `vn-localization-audit-checklist.md` §8 + `rules-index.csv:85` path `kitehub/kiteclass-frontend` → `kiteclass/kiteclass-frontend` (same path-bug class, PATCH bump)
- [ ] 3 env var: thêm local override HOẶC `ACCEPTABLE_PROD_DOMAINS` + rationale (registry row)
- [ ] (Optional) E2E Flow-1 spec (GAP-802 #3 deferred) khi FE E2E infra lands

## Related

- **GAP-802** — detector wave sinh ra finding này (parent)
- **GAP-801** — bug class gốc (BE→FE URL drift)
- `e2e-rst-test-layer-boundary.md` §3 — RST→E2E promotion (cho #3 deferred)
- `production-env-config-registry.md` §5 — acceptable-default exception cho env var prod-domain
- `local-fix-production-parity-check.md` — sister check env parity

## Log

- **2026-05-28:** Filed từ GAP-802 detector first-run trên main `a0cb5b47`. 2 finding thật (reset-password route + 3 deadlink var) — đúng mục đích detector (validate qua việc bắt drift thật). Per `audit-to-gap-pipeline.md` findings → gap, không fix inline trong PR detector.
