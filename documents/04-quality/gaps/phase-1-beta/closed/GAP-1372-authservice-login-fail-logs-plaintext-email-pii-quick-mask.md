# GAP-1372: AuthService login-fail log plaintext email (PII) — quick-mask trước khi GAP-116 scrubber active

**Status:** 🔵 OPEN
**Priority:** 🟡 P2
**Domain:** Backend
**Found:** 2026-06-14 (ops-readiness full audit post wave-p0-closeout-1 — §2.3 PII)
**Affects:** `kiteclass-core` `AuthService.java:48` (KC-native login, Wave auth-1)

## Problem

`AuthService.login()` khi login fail ghi `log.info("Login failed for email={} (uniform 401)", request.email())` (`AuthService.java:48`) — email là PII per `logs-format-standard.md` §2.4 + §3.1 (cần mask `a***@domain.com`). PII scrubber nền tảng (GAP-116) bị defer Wave 7 → email ghi raw vào log.

Đây là site PII-log MỚI do bề mặt auth-1 (KC-native login) thêm vào — đã được auth-1 ops audit (2026-06-06) flag P2-1 nhưng chưa file gap riêng (chỉ reference GAP-116 umbrella). Vì GAP-116 (full scrubber) còn xa, fix nhanh tại site này (bỏ email khỏi message hoặc mask thủ công) là quick win độc lập, không phải chờ scrubber.

Mặt tích cực (auth-1 audit verified): password + JWT KHÔNG bao giờ bị log; login OK chỉ log role/referenceId/tenantId (không PII). Chỉ login-fail message lộ email.

## Proposed Fix

Bỏ email khỏi log message của login-fail HOẶC mask thủ công (`a***@domain.com`) tại `AuthService.java:48` cho tới khi GAP-116 scrubber active. Sweep sister auth/login sites cùng class (per `cross-flow-bug-class-sweep.md`).

## Acceptance Criteria

- [ ] `AuthService.java:48` không log email plaintext (bỏ hẳn hoặc mask).
- [ ] Sweep các login/auth log site khác không lộ email/phone plaintext.
- [ ] Regression: login-fail vẫn giữ uniform 401 + log đủ context (không PII) để triage.

## Related

- Discovered in: ops-readiness full audit 2026-06-14 (OPS-006); auth-1 ops audit 2026-06-06 P2-1.
- GAP-116 (PII scrubber platform umbrella — Wave 7 deferred; gap này là quick-mask độc lập), `logs-format-standard.md` §2.4/§3.1.
- `cross-flow-bug-class-sweep.md` §3 — sweep sister log sites.
