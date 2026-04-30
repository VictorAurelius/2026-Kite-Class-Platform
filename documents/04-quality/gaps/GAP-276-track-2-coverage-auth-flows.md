# GAP-276: Track 2 Coverage — Auth flows kit (KC + KH)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (UX growth — All personas, entry-point UI)
**Domain:** Frontend / Design System
**Found:** 2026-04-29 via audit §2.3
**Affects:** `kiteclass-frontend/src/app/(auth)/**` + `kitehub-frontend/src/app/(auth)/**` + auth form components

## Problem

R2/R3 HTML kits each have a login screen but DO NOT cover full auth lifecycle. 6 production pages (4 KC + 2 KH) + 5+ form components ❌ NO kit coverage.

## Current State

**KC (4 pages + 5 forms):**
| Path | Status |
|------|:------:|
| `(auth)/register/page.tsx` | exists, R1 |
| `(auth)/register/student/page.tsx` | exists (student-specific signup) |
| `(auth)/forgot-password/page.tsx` | exists |
| `(auth)/reset-password/page.tsx` | exists |
| `(auth)/parent-invite/[token]/page.tsx` | partial G7 implicit cover |

**KH (2 pages):**
| Path | Status |
|------|:------:|
| `(auth)/register/page.tsx` | exists |
| `(auth)/verify-email/page.tsx` | exists |

## Proposed Fix

Create `ui_kits/auth-flows/` HTML kit covering full auth lifecycle:

**Screens (~12):**
- Login (KC + KH variants — different brand)
- Register (KC tenant + KC student + KH tenant)
- Forgot password (request reset)
- Reset password (token-link landing + success)
- Verify email (waiting + success + token-expired)
- Parent invite redemption (token validation + child link)
- Auth layout (centered card + brand mark)
- Empty/error states (token-expired / already-used / rate-limit-hit)
- Future: MFA setup + social login expansion (Zalo + Google)

## Acceptance Criteria

- [ ] HTML kit `ui_kits/auth-flows/` ≥105/128 across all screens
- [ ] Both KC + KH brand variants present
- [ ] Token flow E2E mocked (forgot → email link → reset → success)
- [ ] Parent invite flow uses G7 component (post-GAP-273 lands)
- [ ] Vietnamese-only with realistic VN names + phones
- [ ] WCAG AA + keyboard navigation flawless
- [ ] Production ported all 6 pages ≥105/128

## Related

- Audit evidence: §2.3
- Sister: GAP-273 (G7 parent-invite component must port first)
- Existing kits: every kit has 1 login screen but not full lifecycle

## Effort estimate

~1-2 weeks. Wave-pack candidate sliced into KC-side / KH-side / cross-cutting layouts.

## Log

- **2026-04-29:** Filed from audit synthesis.
