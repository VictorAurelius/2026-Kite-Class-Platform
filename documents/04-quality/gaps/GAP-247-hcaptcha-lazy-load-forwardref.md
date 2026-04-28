# GAP-247: HCaptcha not lazy-loaded on KH `/register` (~80 KB First Load JS win blocked by ref-forwarding)

**Status:** 🔵 OPEN
**Priority:** 🟡 P2 (perf — fits within 250KB budget today, ~80 KB potential headroom)
**Domain:** Frontend (KiteHub) / Performance
**Detected:** 2026-04-28 (Wave GAP-236 Agent D return finding)
**Affects:** `kitehub-frontend` `/register` route First Load JS budget headroom

## Problem

`kitehub/kitehub-frontend/src/app/(auth)/register/page.tsx:8` imports `@hcaptcha/react-hcaptcha` statically. The library ships ~80 KB gzipped of widget host code, but at runtime HCaptcha already lazy-loads its own iframe — so the page-level lazy boundary would shave the host JS from First Load JS without affecting UX.

Agent D attempted `next/dynamic` lazy-load during Wave GAP-236 but reverted: the `captchaRef.current?.resetCaptcha()` call at line 21 needs a forwarded ref, and Next 15 / React 19 `next/dynamic` doesn't natively forward refs.

Current state-check 2026-04-28: `register/page.tsx` still has `import HCaptcha from '@hcaptcha/react-hcaptcha'` + `useRef<HCaptcha>(null)` — Agent D's revert is in place.

## Root Cause

Next.js `next/dynamic` returns a wrapper component that doesn't pass refs through. To lazy-load while preserving `resetCaptcha()` access, a manual wrapper using `React.forwardRef` + `useImperativeHandle` is required.

## Proposed Fix

1. Create `kitehub-frontend/src/components/auth/dynamic-hcaptcha.tsx`:
   - Inner component (`HCaptchaInner`) that consumes `react-hcaptcha`, exposes `resetCaptcha` + `execute` via `useImperativeHandle`
   - Outer component using `next/dynamic({ ssr: false })` with `React.forwardRef` + ref proxy
2. Replace `import HCaptcha` in `register/page.tsx` with the new wrapper; preserve `captchaRef` API
3. Verify with `pnpm build` + bundle budget — expect `/register` First Load JS to drop ~60-80 KB

Reference: [vercel/next.js#42501](https://github.com/vercel/next.js/issues/42501) for the canonical forwardRef pattern.

## Acceptance Criteria

- [ ] `dynamic-hcaptcha.tsx` wrapper created with forwardRef + useImperativeHandle
- [ ] `register/page.tsx` consumes wrapper; `resetCaptcha()` still works post-submit error
- [ ] `/register` First Load JS reduced (measure via `pnpm check:budget` before/after)
- [ ] No regression in register flow (Playwright smoke test if available)
- [ ] Bundle budget green (52 routes ≤250 KB)

## Related

- Parent wave: `documents/03-planning/waves/wave-gap-236-fe-code-split.md`
- PR #603 (Agent D revert noted in summary)

## Log

- **2026-04-28** — Filed during Wave GAP-236 consolidation. Agent D's attempt reverted because of ref-forwarding gap; documented pattern for follow-up wrapper.
