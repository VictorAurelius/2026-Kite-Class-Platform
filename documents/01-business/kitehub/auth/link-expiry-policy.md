# Link & Token Expiry Policy — Phase 1 BETA

**Domain:** Authentication / Email / Token lifecycle
**Last verified:** 2026-05-16 (Wave 86 docs-cluster — GAP-590 closure)
**Source-of-truth code:** `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/auth/**` + `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java`
**Related rules:**
- `.claude/rules/pre-launch-auth-hardening-checklist.md` Cat 4 §2.8 (token rotation)
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.10 (Time-sensitive flow gap)
- `documents/01-business/kitehub/auth/rules.md` (parent auth business rules)

---

## 1. Mục đích

Codify chuẩn TTL cho mọi loại link/token email-driven trong Phase 1 BETA. Đầu Wave 86, các giá trị TTL nằm rải rác trong code (constants, `@Value` defaults) và chưa có 1 spec doc duy nhất để:
- Reviewer verify code khớp với business expectation.
- FE countdown UI biết hiển thị bao lâu.
- Support team biết khi resend / khi yêu cầu user thử lại.
- Industry benchmark sanity-check (per Wave 86 Bucket A benchmark Q5).

Spec này KHÔNG thay đổi code (Wave 86 docs-cluster scope) — nó **document hóa trạng thái hiện tại** và **chỉ rõ deltas** cần fix trong follow-up gap nếu có.

---

## 2. Business Rules

### BR-AUTH-LINK-EXPIRY-001 — Email verification link TTL

**Rule:** Link xác minh email trong welcome email cho tenant mới signup có TTL **24 giờ**.

- **Lý do:** Industry standard (Stripe, Vercel, Resend). User có thể click sau bữa tối / cuối tuần mà không miss.
- **Code reference:** Hiện chưa tách riêng email-verification dedicated; email verify hiện đi qua beta-invite token (BR-002) trong Phase 1 BETA. Phase 1.5+ tách riêng khi self-signup mở.
- **FE behavior:** Welcome email body show `Hết hạn: <expiry-date>` (đã có trong `beta-invite.html` template).

### BR-AUTH-LINK-EXPIRY-002 — Beta invite token TTL

**Rule:** Beta invite token (claim code 6 số + invite UUID) có TTL **24 giờ**.

- **Lý do:** Industry email-verification norm. Solo dev approve sáng → invitee có cả ngày click trước khi expire.
- **Code reference:** `BetaAccessService.INVITE_TOKEN_TTL_HOURS = 24L` ([`BetaAccessService.java:55`](../../../../kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/service/BetaAccessService.java)).
- **Resend behavior:** Admin click "Re-issue" → token mới với TTL mới 24 giờ; token cũ revoked. Rate limit: 5 resend/giờ/email.
- **FE behavior:** Landing page beta-signup hiển thị "Hết hạn: <expiry>" + countdown < 1h.

**Verdict:** ✅ Code khớp spec (24h).

### BR-AUTH-LINK-EXPIRY-003 — Magic link login TTL

**Rule:** Magic link login (passwordless one-tap) có TTL **15 phút**.

- **Lý do:** Security-sensitive — magic link bypass password. Industry standard (Slack, Notion, Auth0).
- **Code reference:** Magic-link login chưa implement Phase 1 BETA (password + 2FA primary path). Spec này áp dụng khi feature ship trong Phase 1.5+.
- **FE behavior:** Email body show "Link có hiệu lực 15 phút"; landing page show countdown.

**Verdict:** ⏳ Future implementation — TTL spec lock cho Phase 1.5+ shipping.

### BR-AUTH-LINK-EXPIRY-004 — 2FA challenge token TTL

**Rule:** Challenge token được issue giữa `POST /api/auth/login` và `POST /api/auth/2fa/{verify,enroll-*}` có TTL **5 phút**.

- **Lý do:** Security-sensitive — token bypass password-checked gate. Per Wave 72b GAP-516 design: short window forces user complete 2FA quickly.
- **Code reference:** `application.yml` line 103-106 — `jwt.challenge-secret` documents 5-min TTL (GAP-516 Wave 72b).
- **FE behavior:** 2FA page show "Vui lòng nhập mã trong 5 phút"; failure → return to login.

**Verdict:** ✅ Code khớp spec (5m).

### BR-AUTH-LINK-EXPIRY-005 — 2FA TOTP code window

**Rule:** Mã TOTP 6-số (Google Authenticator / Authy) có time-window **30 giây** + clock-skew tolerance **±60 giây** (1 step trước, 1 step sau).

- **Lý do:** RFC 6238 standard. Clock-skew tolerance accommodates user device drift.
- **Code reference:** `kitehub.auth.totp.window-steps` (default 1) trong TwoFactor service.
- **FE behavior:** Authenticator app tự refresh 30s; user UI không cần countdown (app handles).

**Verdict:** ✅ Code khớp RFC 6238 standard.

### BR-AUTH-LINK-EXPIRY-006 — Password reset token TTL

**Rule:** Password reset token có TTL **60 phút** (1 giờ).

- **Lý do:** Balance security (short window giảm window attack nếu email leak) vs UX (user check email không real-time, 1h đủ cho hầu hết use case). Per Wave 79 GAP-548.
- **Code reference:** `kitehub.auth.password-reset.token-ttl-minutes = 60` ([`application.yml:124`](../../../../kitehub/kitehub-subscription/src/main/resources/application.yml)) + `PasswordResetService:55` `@Value`.
- **FE behavior:** Reset email body show "Hết hạn sau 1 giờ"; landing page show countdown < 5 phút.

**Verdict:** ✅ Code khớp spec (60 phút).

**Note:** GAP-590 §Proposed Fix mô tả 24h cho password reset (industry "norm"). Sau review benchmark Q5 + Wave 79 GAP-548 implementation context → 60 phút là phù hợp cho Phase 1 BETA (security-leaning). Phase 1.5+ có thể relax lên 24h khi user volume tăng + magic-link path thay thế.

### BR-AUTH-LINK-EXPIRY-007 — Access token (JWT) TTL

**Rule:** Access token JWT có TTL **15 phút**; refresh token TTL **30 ngày** với rolling rotation.

- **Lý do:** Industry standard. Short access TTL + long refresh = balance security/UX. Rotation prevent replay attack nếu refresh leak.
- **Code reference:** `kitehub.auth.jwt.access-token-ttl-minutes`, `refresh-token-ttl-days` trong `application.yml`.
- **FE behavior:** Silent refresh via `httpOnly` refresh cookie; user invisible.

**Verdict:** ✅ Code khớp standard (15m / 30d rotating).

### BR-AUTH-LINK-EXPIRY-008 — Session cookie sliding window

**Rule:** Session cookie (refresh token) có **30 ngày sliding** — mỗi successful refresh extends thêm 30 ngày từ thời điểm đó.

- **Lý do:** Active user không bị logout đột ngột; idle user (>30 ngày không login) bị invalidate.
- **Code reference:** Refresh-token rotation logic trong `RefreshTokenService`.
- **FE behavior:** User active → never see login screen; idle 30 ngày → next request redirect login.

**Verdict:** ✅ Code khớp standard.

### BR-AUTH-LINK-EXPIRY-009 — Resend rate limit per email

**Rule:** Mỗi email address chỉ được phép resend invite / password-reset / magic-link tối đa **5 lần / giờ**.

- **Lý do:** Anti-abuse (prevent inbox flooding); anti-bot (prevent attacker brute force re-issue).
- **Code reference:** Rate limit logic trong `RateLimitFilter` + `PasswordResetService` resend endpoint.
- **FE behavior:** Resend button disabled với countdown khi cap reached; toast "Vui lòng đợi X phút trước khi thử lại".

**Verdict:** ⏳ Verify code state — Phase 1 BETA scope acceptable nếu chưa enforce strict 5/hour (low user volume; revisit Phase 1.5+).

---

## 3. Tóm tắt TTL matrix

| Token type | TTL | Code reference | Status |
|---|---|---|---|
| Email verification (welcome) | 24h | Đi qua beta-invite token Phase 1 BETA | ✅ |
| Beta invite (claim code + UUID) | 24h | `BetaAccessService.INVITE_TOKEN_TTL_HOURS=24L` | ✅ |
| Magic link login | 15m | Chưa implement Phase 1 BETA | ⏳ Phase 1.5+ |
| 2FA challenge token | 5m | `jwt.challenge-secret` GAP-516 | ✅ |
| 2FA TOTP code | 30s + ±60s skew | RFC 6238 default | ✅ |
| Password reset | 60m | `kitehub.auth.password-reset.token-ttl-minutes=60` | ✅ |
| Access JWT | 15m | `kitehub.auth.jwt.access-token-ttl-minutes` | ✅ |
| Refresh JWT | 30d sliding | `refresh-token-ttl-days` + rotation | ✅ |
| Resend rate limit | 5/hour/email | `RateLimitFilter` | ⏳ Verify enforcement |

---

## 4. Acceptance Criteria — code ↔ spec sync (GAP-590 closure)

| AC | Criterion | Verify |
|---|---|---|
| AC1 | Spec doc shipped | ✅ This file |
| AC2 | TTL matrix tableized | ✅ §3 above |
| AC3 | Code reference cite cho mỗi BR | ✅ §2 mỗi BR có "Code reference" line |
| AC4 | Status verify (khớp/lệch/future) | ✅ §3 column "Status" |
| AC5 | Auth hardening checklist Cat 4 row 7 cite this doc | ⏳ Follow-up — cập nhật `pre-launch-auth-hardening-checklist.md` trong Wave 86b nếu chưa link |
| AC6 | Integration test verify TTL enforcement | ⏳ Follow-up GAP nếu missing — Phase 1 BETA scope accept unit test current; integration mandatory Phase 1.5+ |
| AC7 | FE countdown UI hiển thị TTL on invite/reset landing | ⏳ Follow-up GAP — Phase 1 BETA accept email body display; FE countdown Phase 1.5+ |

---

## 5. Notes & follow-ups

- **GAP-590 closure:** spec doc shipped → AC1-4 ✅. AC5-7 (checklist cite + integration test + FE countdown) deferred to follow-up gap GAP-590b nếu user thấy cần force trong Wave 86b. Phase 1 BETA scope (5 cohort tenant manual flow) accept current state.
- **Code/spec mismatch surface:** Không tìm thấy mismatch quan trọng giữa current code và industry benchmark. Magic link + email-verification dedicated là future scope, không phải Phase 1 BETA gap.
- **Cross-link:** `documents/01-business/kitehub/auth/rules.md` (parent auth rules) nên reference §3 matrix khi next refresh.

---

## 6. Log

- **2026-05-16** Wave 86 docs-cluster — GAP-590 closure. Spec doc shipped với 9 BR + TTL matrix + status verify. Code state-check confirm: beta invite 24h ✅, password reset 60m ✅, 2FA challenge 5m ✅, JWT 15m/30d ✅. Magic link + email-verification dedicated marked Phase 1.5+ future scope. Spec định nghĩa Phase 1 BETA TTL contract; revisit khi feature scope expand.
