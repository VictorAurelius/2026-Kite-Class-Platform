# GAP-611 — `POST /api/v1/auth/beta-signup` returns 404 (route exists nhưng gateway/subscription không serve)

**Status:** 🔵 OPEN
**Priority:** 🔴 P0
**Domain:** Backend
**Found:** 2026-05-17 (Wave 90 walkthrough — FE submit signup form fails)
**Affects:** Beta signup completion step (final step) — invitee cannot create tenant even if validate token works (which it doesn't either per GAP-610)

## Problem

FE BetaSignupForm submits to `POST /api/v1/auth/beta-signup`. Browser console:
```
3858-d0d051bd316bcdde.js:1  POST https://api.kitehub.me/api/v1/auth/beta-signup 404 (Not Found)
```

Curl direct probe (Wave 90 verify):
```bash
$ curl -s -o /tmp/r.txt -w "HTTP %{http_code}\n" -X POST \
    "https://api.kitehub.me/api/v1/auth/beta-signup" \
    -H "Content-Type: application/json" \
    -d '{"token":"98446443-e5cc-43e9-9498-6799d460d2db","ownerPassword":"TestPass1234","subdomain":"dgedu"}'
# HTTP 404 (empty body)
```

Body empty (NOT a JSON error response from BE controller). Suggests gateway 404 OR Spring routing 404 BEFORE reaching controller.

Source verified:
- BE: `BetaAccessController.@PostMapping("/api/v1/auth/beta-signup")` exists ✅
- Gateway route: `id=kitehub-auth-v1, Path=/api/v1/auth/**, uri=http://kitehub-subscription:8080` (catch-all, applies both GET + POST) ✅
- FE: `apiClient.post(endpoints.auth.completeBetaSignup)` → endpoint string `/api/v1/auth/beta-signup` ✅

All 3 layers look correct. Yet 404.

## Root cause hypothesis (need verify — AWS suspended)

### Hypothesis 1: Gateway predicate order — earlier route shadows POST

Gateway `application.yml` line 460 `kitehub-auth-v1` catch-all. Earlier in file (lines 310-370) there are specific routes like `kitehub-auth-v1-request-beta-access` (POST `/api/v1/auth/request-beta-access`) + 2FA endpoints. If Spring Cloud Gateway predicate order treats more-specific routes first AND one of them mistakenly matches `/api/v1/auth/beta-signup` POST, request gets misrouted.

### Hypothesis 2: Spring Cloud Gateway HTTP method filter bug

The catch-all has no `Method=` predicate. But CircuitBreaker filter might wrap POST differently OR Reactor-Netty might reject POST with specific content-type.

### Hypothesis 3: JwtAuthenticationGatewayFilter (new Wave 89) blocks POST silently

The new filter shipped staging.21 has `isPublicPath()` whitelisting `/api/v1/auth/*`. If path matching pattern doesn't handle trailing path correctly (vd matches `/api/v1/auth/` but not `/api/v1/auth/beta-signup`), filter might short-circuit incorrectly.

Verify filter logic for `/api/v1/auth/beta-signup`:
```java
String path = request.getURI().getPath();  // "/api/v1/auth/beta-signup"
if (isPublicPath(path)) return chain.filter(exchange);
// isPublicPath: path.startsWith("/api/v1/auth/") ← MATCHES → bypass ✓
```

Should be fine. But if Wave 89 filter actually has bug (vd typo path), this would 404 silently.

### Hypothesis 4: Spring Security on subscription service blocks POST

Subscription `SecurityConfig` might require auth on `/api/v1/auth/beta-signup` POST. Public endpoint annotation might be `permitAll()` for GET only.

## Verify steps (resume when AWS restored)

```bash
# 1. Direct subscription bypass gateway
docker exec kitehub-subscription curl -X POST http://localhost:8080/api/v1/auth/beta-signup \
  -H "Content-Type: application/json" -d '{}'
# If 400 = controller reached (good — gateway is at fault)
# If 404 = controller NOT reached (subscription routing issue)
# If 403 = Security blocking (security config bug)

# 2. Subscription logs while POST
docker logs kitehub-subscription -f &
curl -X POST https://api.kitehub.me/api/v1/auth/beta-signup -d '{}'

# 3. Gateway logs route resolution
docker logs kitehub-gateway | grep "beta-signup"
```

## Production impact

🔴 100% beta signup completion broken. Even if user has token + can validate, cannot complete signup → tenant never provisioned.

## Proposed Fix

Depends on root cause; if Hypothesis 3 (JwtAuthenticationGatewayFilter):
- Verify Wave 89 filter `isPublicPath` correctly matches `/api/v1/auth/beta-signup`
- Add explicit unit test for this path
- Pair fix with GAP-610 if same root cause class

If Hypothesis 1 (gateway route shadow):
- Reorder gateway routes; explicit route for `/api/v1/auth/beta-signup` before catch-all

## Acceptance Criteria

- [ ] Root cause identified
- [ ] Fix deployed staging.N
- [ ] Curl POST with valid token returns 200 + JSON response per `BetaAccessController.completeBetaSignup`
- [ ] FE form submit → tenant created → owner can login
- [ ] Integration test cover full happy path
- [ ] Pair with GAP-610 if validate also 404'd by same cause

## Related

- GAP-610 (sister — GET validate same 404 class)
- GAP-604 (Wave 89 deployed JwtAuthenticationGatewayFilter — potential regression suspect)
- BetaAccessController.completeBetaSignup
- Wave 90 walkthrough audit (when filed) — browser console evidence

## Log

- **2026-05-17:** Gap filed during Wave 90 walkthrough. AWS account suspended mid-investigation. Pair-investigate với GAP-610 (likely same root cause class — gateway OR security config OR new JWT filter regression).
- **2026-05-18 (Wave 91 Bucket D deep investigation):** Status PARTIAL. PR #1490 đã ship defensive hardening + 2 gateway-filter regression tests. Wait-time deep investigation (`documents/04-quality/audits/aws-verification/2026-05-18-wave-91-bucket-d-deep-investigation.md`) **REJECTED all 4 original hypotheses** với code evidence: (1) gateway predicate order — specific routes phía trên catch-all không match `/api/v1/auth/beta-signup`; (2) HTTP method filter — catch-all không có Method predicate; (3) Wave 89 JWT filter `isPublicPath` matches startsWith("/api/v1/auth/") → bypass đúng; (4) Spring Security `permitAll()` on /api/v1/auth/**. Surfaced 3 NEW hypothesis Bucket D missed: **#7 controller catches IllegalArgumentException → returns empty-body 404 (matches gap evidence exactly) — Medium**, #5 empty-body suggests gateway CircuitBreaker fallback or controller exception path, #6 image promotion drift. **Cross-gap với GAP-610: single root cause likely `findByInviteToken` returns empty Optional** → service throws IllegalArgumentException → controller line 119 catches → 404 empty body matches reported behavior exactly. Coordinator F debug sequence post-AWS-restore: 5 steps (~15 min). Gap stays PARTIAL until live verify.
