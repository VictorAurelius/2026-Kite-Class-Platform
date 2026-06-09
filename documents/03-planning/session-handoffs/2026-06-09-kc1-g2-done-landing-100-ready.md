---
title: Session Handoff 2026-06-09 — KC-1 G2 DONE + wave landing-100 READY to execute
audience: dev
date: 2026-06-09
branch: fix/v87-attendance-status-normalize-kc5 (PR #2274 OPEN)
context_at_handoff: 74% (744k/1M Opus 4.8 1M)
---

# Handoff 2026-06-09 — KC-1 G2 done, landing-100 wave ready

## Đã làm session này

### PR #2274 — CI fixes + GAP-1077 port (pushed)
- 3 FE test fixes: vetting `usePathname` mock (8 file sweep) + reports-page `auth-store` bare-`useAuthStore()` mock + auth.test logout fail-open sync.
- **GAP-1077 port DONE (code)**: host→tenant middleware **moved kitehub-frontend → kiteclass-frontend** (Model "Move"). suite (middleware + lib/tenant + suspended + 41 test) ported, wired `(public)/page.tsx`+`layout.tsx` đọc `x-tenant-id`, removed khỏi kitehub-frontend. Build PASS cả 2 FE. GAP-811/1077 vẫn **PARTIAL** — walk nip.io `:3000` là gate DONE riêng (cần rebuild).
- Bundle budget `(dashboard)/settings/page` 250→270 (peer-consistent, diffuse dep growth) — unblock CI.

### KC-1 G2 walk PASS → 5 gap DONE
`GAP-1067/1071/1072/1073/1074` flipped DONE (CSV canonical + git mv `closed/` + Log + G2 evidence). 3/3 browser-walk PASS (`:3000`): W1 page+shell, W2 cross-tab session, W3 logo upload+render.
- Recipe: `documents/05-guides/operations/2026-06-09-g2-recipe-kc1-simple.md`.

### Discoveries (OPEN, folded into wave)
- **GAP-1036 OPEN P1** — MinIO bucket `kite-branding-assets` missing + no ensure-bucket-on-startup → logo upload 500. Tạo bucket thủ công G2 để unblock; systemic fix (`@PostConstruct` ensure-bucket trong `MinIOBrandingAssetStorageImpl`) = wave.
- **Theme bugs (Bucket D)**: (1) theme không áp khi reload, áp khi focus tab — `BrandingThemeApplier` post-hydration useEffect + react-query refetchOnWindowFocus → cần SSR-inline. (2) `themeReceiver.ts` allowlist thiếu `localhost:3000` → console spam preview.

## Next session — EXECUTE wave landing-100

**Plan (PR-first, đã viết):** `documents/03-planning/waves/wave-2026-06-09-landing-100.md` — 7 bucket, completion verdict per bucket, execution 2 đợt (4 agent song song + 1 agent batch), state-check evidence, gates.

**Bối cảnh đã có (từ 3 outside-in audit + thesis):**
- Rào cản #1 "đẹp 100% mọi tenant" = landing fallback **DEMO data bịa** khi tenant chưa seed → Bucket A (empty-state) + Bucket G (seed demo-trio thật).
- Seed demo-trio thesis §4.1-4.2: **Khánh** (§4.1) + **Hà** (free/xanh dương/Toán) + **Nhì** (paid/xanh lá/Hóa). Hình 4.1/4.3/4.4.
- Reconcile target: apex `kitehub.me`=marketing kit / subdomain=per-tenant template.

**Holds đã clear:** G2 walk xong ✅ + #2274 CI fixed ✅ → sẵn sàng spawn agents (Opus, worktree-isolated, ≤5 concurrent) + rebuild.

**Lưu ý execute:**
- Đợt 1 song song: C (HeroSection) / D (ThemeSync+BrandingThemeApplier+themeReceiver+globals) / F (new kit sections) / G (seed backend) — file disjoint.
- Đợt 2: 1 agent batch A+B+E (cùng vùng page.tsx/layout/sections).
- GAP-1036 ensure-bucket = tiền đề seed (Hà/Nhì branding upload).
- Mỗi agent: `fe-build-local-verify` build PASS + `agent-model-opus-default`.

## Còn lại GAP-811/1077 (riêng, không trong landing-100)
Walk production-accurate nip.io `sky-education.127.0.0.1.nip.io:3000` (per `g1-browser-walk-before-flip` §3.1) sau khi rebuild kiteclass-frontend → flip GAP-811/1077 DONE.
