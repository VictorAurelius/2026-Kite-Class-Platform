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

GAP-802 vừa ship 2 detector và khi chạy thật trên main đã bắt được **2 finding thật cùng class với GAP-801** (BE↔FE contract drift mà không test nào assert):

### Finding 1 — `/reset-password` BE link không có FE route (cơ chế #2)
`PasswordResetService.java:80` dựng link `resetBaseUrl + "/reset-password?token=..."` nhúng vào email reset mật khẩu. `check-be-fe-url-contract.sh` báo **MISSING**: `kitehub-frontend/src/app/**` KHÔNG có route `/reset-password` (chỉ `kiteclass-frontend` có). Nếu `resetBaseUrl` trỏ kitehub-frontend → user click link reset → **404**, không reset được mật khẩu. Đúng class GAP-801 (`/signup/beta` 404).

→ Cần xác định `resetBaseUrl` trỏ FE nào. Nếu kitehub-frontend → thêm route `/reset-password` HOẶC sửa BE path. Nếu cố ý trỏ kiteclass-frontend → tinh chỉnh detector để resolve cross-FE (giảm false-positive).

### Finding 2 — 3 env var prod-domain default thiếu local override (cơ chế #5)
`audit-env-coverage.sh` CHECK B (WARN) báo 3 var default về prod domain, không có override trong `kitehub/docker-compose.kitehub.yml` → email gửi ở local nhúng link prod → dead-link khi test local (đúng class GAP-801 part 3):

| Var | Default | File |
|---|---|---|
| `KITEHUB_STAFF_INVITATION_BASE_URL` | `https://kitehub.me` | `StaffInvitationController.java:102` |
| `RESEND_FROM_EMAIL` | `no-reply@kitehub.me` | `ResendEmailService.java:67` |
| `PARENT_PORTAL_REDEEM_BASE_URL` | `https://app.kiteclass.vn/parent-invite/` | `kiteclass-core application.yml:292` |

→ Thêm local override vào `docker-compose.kitehub.yml` env block (vd `http://localhost:3000`), HOẶC thêm vào `ACCEPTABLE_PROD_DOMAINS` nếu cố ý (vd `RESEND_FROM_EMAIL` có thể luôn dùng prod sender — cần rationale + `production-env-config-registry.md` row).

## Acceptance Criteria

- [ ] Xác định `resetBaseUrl` trỏ FE nào; fix (thêm route HOẶC sửa BE path HOẶC tinh chỉnh detector cross-FE)
- [ ] 3 env var: thêm local override HOẶC `ACCEPTABLE_PROD_DOMAINS` + rationale (registry row)
- [ ] Sau khi `/reset-password` resolved → flip `be-fe-url-contract` CI job WARN → HARD STOP (sửa `quality-code.yml`)
- [ ] (Optional) E2E Flow-1 spec (GAP-802 #3 deferred) khi FE E2E infra lands

## Related

- **GAP-802** — detector wave sinh ra finding này (parent)
- **GAP-801** — bug class gốc (BE→FE URL drift)
- `e2e-rst-test-layer-boundary.md` §3 — RST→E2E promotion (cho #3 deferred)
- `production-env-config-registry.md` §5 — acceptable-default exception cho env var prod-domain
- `local-fix-production-parity-check.md` — sister check env parity

## Log

- **2026-05-28:** Filed từ GAP-802 detector first-run trên main `a0cb5b47`. 2 finding thật (reset-password route + 3 deadlink var) — đúng mục đích detector (validate qua việc bắt drift thật). Per `audit-to-gap-pipeline.md` findings → gap, không fix inline trong PR detector.
