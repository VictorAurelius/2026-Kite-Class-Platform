# GAP-926: BetaSignupForm catch block maps every error to "Token expired / already used"

**Status:** 🔵 OPEN
**Priority:** 🟠 P1 (user-facing — wrong error message blocks invitee diagnosis on retryable failures like subdomain conflict)
**Domain:** Frontend
**Found:** 2026-06-04 (Wave flow-kh1 G2 walk — user submitted signup form, BE returned 409 Conflict (subdomain `g2test-an` taken), FE rendered "Hoàn tất đăng ký thất bại. Token có thể đã hết hạn hoặc đã được sử dụng." — wrong reason)
**Affects:** `kitehub/kitehub-frontend/src/components/auth/BetaSignupForm.tsx` line 103

## Problem

`BetaSignupForm.tsx` lines 86-108 handle the POST `/api/v1/auth/beta-signup` submit:

```tsx
try {
  await apiClient.post(endpoints.auth.completeBetaSignup, { token, ownerPassword, subdomain, consent });
  setSubmitted(true);
} catch {
  setError('Hoàn tất đăng ký thất bại. Token có thể đã hết hạn hoặc đã được sử dụng.');
} finally {
  setSubmitting(false);
}
```

The catch block is **status-blind** — every backend failure renders the same Vietnamese token-expired message. BE returns several distinct error codes per `BetaSignupErrorResponse`:

| BE response | Real cause | FE message shown today |
|---|---|---|
| 404 + `INVALID_TOKEN` | Token not in DB (already redeemed + nulled out, or never issued) | "Token đã hết hạn / đã sử dụng" ✅ correct by accident |
| 409 + `TOKEN_EXPIRED` | Token row found but expired | "Token đã hết hạn / đã sử dụng" ✅ correct by accident |
| 409 + `WRONG_STATE` | Token row found but request status ≠ APPROVED (PENDING / REJECTED / ABORTED / SIGNED_UP) | "Token đã hết hạn..." ❌ wrong |
| **409 (no body)** | Tenant provisioning conflict — subdomain or email already taken (controller line 146-149) | "Token đã hết hạn..." ❌ **wrong** (real cause: pick different subdomain) |
| 500 | Tenant provisioning runtime failure | "Token đã hết hạn..." ❌ wrong (retry would help) |
| Network error | apiClient cannot reach gateway | "Token đã hết hạn..." ❌ wrong (network problem) |

Empirical 2026-06-04 04:39 UTC, Wave flow-kh1 G2 walk: invitee g2test-an-4 submitted with subdomain `g2test-an` (already taken by g2test-an-1 earlier in the day) → 409 with empty body from `BetaAccessController.java:149` → FE catch block fired generic message → invitee believed their token had expired and stopped, when the real fix was "pick a different subdomain."

Sister bug class: GAP-924 (FE 2FA verify silent UI) — both forms have insufficient per-status error mapping. Same fix shape: read `error.response.status` + `error.response.data.errorCode` and render specific Vietnamese guidance.

## Root Cause

The catch block was written when the BE returned only token-validity errors (404 + 410). After GAP-372 (Wave 45 Bucket A) added the subdomain conflict path (`BetaAccessController.java:139-149`) and GAP-611 (Wave beta-readiness-5 Bucket D) added the `BetaSignupErrorResponse` shape with `errorCode`, the FE never picked up the richer contract — it kept the generic catch.

## Proposed Fix

Replace the bare `catch {}` with per-status + per-errorCode mapping:

```tsx
} catch (err) {
  const status = (err as { response?: { status?: number } })?.response?.status;
  const errCode = (err as { response?: { data?: { errorCode?: string } } })?.response?.data?.errorCode;

  if (status === 404 && errCode === 'INVALID_TOKEN') {
    setError('Liên kết không hợp lệ hoặc đã được sử dụng. Hãy yêu cầu invite mới.');
  } else if (status === 409 && errCode === 'TOKEN_EXPIRED') {
    setError('Liên kết kích hoạt đã hết hạn. Hãy yêu cầu invite mới.');
  } else if (status === 409 && errCode === 'WRONG_STATE') {
    setError('Yêu cầu beta không ở trạng thái có thể đăng ký. Vui lòng liên hệ đội ngũ KiteClass.');
  } else if (status === 409) {
    // Tenant provisioning conflict — subdomain or email taken (BE returns 409 empty body)
    setError('Subdomain đã được sử dụng. Vui lòng chọn tên khác và thử lại.');
  } else if (status === 500) {
    setError('Hệ thống đang gặp sự cố khi tạo tenant. Vui lòng thử lại sau vài phút.');
  } else if (status == null) {
    setError('Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.');
  } else {
    setError('Hoàn tất đăng ký thất bại. Vui lòng thử lại.');
  }
}
```

Bonus (low cost, high signal): when the 409 has empty body, also clear the `subdomain` field so the invitee notices it needs a new value.

## Acceptance Criteria

- [ ] BetaSignupForm catch block inspects `error.response.status` + `error.response.data.errorCode` and renders per-case Vietnamese message
- [ ] All five distinct cases above + network-error case have a dedicated message
- [ ] 409 empty-body (subdomain conflict) explicitly maps to "Subdomain đã được sử dụng"
- [ ] Cross-flow sweep: confirm no other public FE form in `app/(auth)/**` uses the same generic catch pattern (per `cross-flow-bug-class-sweep.md` §3); GAP-924 already shipped the equivalent for 2FA verify
- [ ] Empirical re-walk: submit with taken subdomain → see correct error → fix subdomain → success

## Related

- Discovered in: Wave flow-kh1 G2 walk session 2026-06-04 (subdomain `g2test-an` taken by g2test-an-1 → invitee saw wrong error)
- Sister: GAP-924 (FE 2FA verify silent UI — same class, different page)
- Sister: GAP-927 (BE rollback token rotation chain — both surfaced same flow, both block invitee retry but for different reasons)
- BE contract source: `kitehub/kitehub-subscription/src/main/java/com/kitehub/subscription/beta/dto/BetaSignupErrorResponse.java`
- BE controller dispatch: `BetaAccessController.java:118-156` (404 / 409 with body / 409 empty / 500 paths)
- Per `pre-handoff-self-test-completeness.md` §2.2 anonymous/public flow (b)+(c) — confirmation surface must reflect actual state, not fall back to generic message
