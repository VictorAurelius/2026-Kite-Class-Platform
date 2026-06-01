# GAP-609 — FE thiếu UI nhập claim code; chỉ accept `?token=UUID` deep-link từ email

**Status:** 🔵 OPEN
**Priority:** 🟠 P1
**Domain:** Frontend
**Found:** 2026-05-17 (Wave 90 walkthrough — user "làm gì có trang nào có chỗ nhập mã nhỉ?")
**Affects:** Beta cohort onboarding alternate path; user nhận claim code qua Zalo/WhatsApp/in-person nhưng email không tới (cộng hưởng với email infra bugs GAP-605/606/608)

## Problem

BE đã có 2 paths để hoàn tất beta signup:
1. **Token deep-link** — `GET /api/v1/auth/beta-signup/validate?token=<UUID>` (FE `/beta-signup?token=<UUID>` page) — user click link trong email
2. **Claim code exchange** — `POST /api/v1/auth/beta-signup/exchange-claim-code` body `{claimCode}` → returns token + pre-fill data — user nhập 6-digit code thủ công

FE chỉ implement path 1 (`/beta-signup?token=<UUID>` page tại `kitehub-frontend/src/app/(auth)/beta-signup/page.tsx`). KHÔNG có UI page cho claim code entry.

Wave 90 walkthrough user expected: mở landing → "Tôi có mã invite" → nhập 190563 → tiếp tục. Thực tế: không có entry point. User phải biết URL `?token=` exact format + tự ghép UUID — UX không khả thi cho real cohort.

## Production impact

🟠 (current) Email broken (GAP-605/606/608) → user không có token → KHÔNG có alternate path → beta onboarding hoàn toàn dead-end.

🟢 (post-email-fix) Email-driven path works; claim code chỉ là backup mechanism cho support handoff (vd user mất email original, support gửi claim code qua chat).

## Proposed Fix

### Phase 1 (FE only, ~2h)
1. NEW route `kitehub-frontend/src/app/(auth)/beta-signup/code/page.tsx`
2. Component `BetaClaimCodeForm`:
   - Input 6-digit code (numeric only, validate length)
   - Submit → POST `/api/v1/auth/beta-signup/exchange-claim-code`
   - On success → redirect to `/beta-signup?token=<returned-token>` (reuses existing form)
   - On error → display Vietnamese error message map (CODE_NOT_FOUND / CODE_EXPIRED / ALREADY_USED)
3. Landing page: add "Tôi đã có mã invite" link near "Đăng ký Beta" CTA
4. Per `dev-readable-doc-language.md` Vietnamese narrative
5. Per `user-manual-content-standard.md` annotated screenshots cho user manual

### Phase 2 (UX polish)
- Auto-focus first digit input
- Paste-from-clipboard split into 6 boxes (like OTP UX)
- Resend code link (calls a new endpoint to re-email — depends on email infra fix)

## Acceptance Criteria

- [ ] `/beta-signup/code` page renders + accepts 6-digit code
- [ ] Valid code → redirect `/beta-signup?token=<UUID>` with pre-fill
- [ ] Invalid code → Vietnamese error toast
- [ ] Landing page link visible
- [ ] Component test cover 4 cases (valid / not-found / expired / already-used)

## Related

- BE `exchangeClaimCode` already implemented `BetaAccessController` `@PostMapping("/api/v1/auth/beta-signup/exchange-claim-code")`
- GAP-605/606/608 (sister email infra — if those fix, this becomes backup path; if those don't fix, this becomes primary path)
- `user-manual-content-standard.md` §2 row 6 — annotated screenshots requirement
- Wave 90 walkthrough: user dùng claim code 190563 nhưng không có UI nhập

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough. User explicit: "làm gì có trang nào có chỗ nhập mã nhỉ?". FE-only fix; BE endpoint ready since Wave 36 GAP-388.
