---
paths:
  - "kitehub/kitehub-frontend/**"
  - "kitehub/kiteclass-frontend/**"
---

# FE Production-Build Local-Verify — đụng frontend → chạy `pnpm build` local TRƯỚC push, không chỉ lint/tsc

**Priority:** 🟠 MANDATORY — frontend-change completeness governance
**Version:** 1.0.0
**Created:** 2026-05-28
**Last-Reviewed:** 2026-05-28
**Reviewer-Approver:** @nguyenvankiet (solo-dev — MINOR self-approve per `rule-change-process.md` §5; new rule với built-in enforcement (reviewer-checklist + override trailer + worked self-test on GAP-801 Suspense bailout 2026-05-28) per §6.5 Enforcement Parity Mandate; no constraint loosening — codifies previously-uncovered class "FE PR pass lint/tsc nhưng fail production build"; sister của `api-contract-change-caller-sweep.md` ("run tests/build not just compile") ở axis FE-build cụ thể; META P0 force-multiplier per `meta-gap-priority.md` §3)
**Applies to:** Mọi PR đụng frontend source dưới `kitehub/kitehub-frontend/**` hoặc `kitehub/kiteclass-frontend/**` (`*.tsx`/`*.ts` trong `src/app/**`, `src/components/**`, `src/lib/**`, hoặc bất kỳ React/Next.js source). Out-of-scope: docs-only change trong FE folder (`*.md`), asset-only change (PNG/SVG), config-comment-only edit.

---

## 1. The Rule

> **Khi PR đụng frontend source, PHẢI chạy `pnpm --filter <fe-package> build` (Next.js production build) ở local TRƯỚC khi push. Nếu build FAIL → fix trước push.** `eslint` PASS + `tsc --noEmit` PASS KHÔNG đủ — production build chạy prerender + static optimization mà lint/type-check không cover.

`eslint` verify code-style; `tsc` verify type-correctness. Production build (`next build`) thêm một tầng: prerender bailout check, Suspense boundary requirement, static-vs-dynamic boundary, server-vs-client component validation. Bug class này chỉ lộ khi RUN production build — KHÔNG lộ khi dev chạy `pnpm dev` (dev server bỏ qua prerender) hoặc `eslint`/`tsc`.

Sister mandate `api-contract-change-caller-sweep.md` §1: "run tests/build not just compile/lint" cho BE method changes. Rule này extend tinh thần đó sang FE production build cụ thể.

---

## 2. Trigger pattern — khi nào fire

Rule fires khi PR diff đụng FE source, đặc biệt khi thêm/sửa các construct sau (cao khả năng prerender bailout / boundary violation):

| Pattern | Vì sao production build catch mà lint/tsc không | Ví dụ |
|---|---|---|
| **`useSearchParams()` không bọc `<Suspense>`** | `next build` prerender bailout: "useSearchParams() should be wrapped in a suspense boundary" | GAP-801: `BetaClaimCodeForm` dùng `useSearchParams` raw → build FAIL |
| **`usePathname()` / `useRouter()` trong static-prerendered page** | Dynamic hook trong static context → prerender error | `app/page.tsx` dùng `usePathname` không có dynamic boundary |
| **Dynamic import (`next/dynamic`) với `ssr: false` mismatch** | Build resolve module graph khác dev server | `dynamic(() => import(...), { ssr: false })` ở Server Component |
| **Server-vs-client boundary** (`"use client"` thiếu / thừa) | Build enforce RSC boundary, dev server lenient hơn | Client hook (`useState`) trong Server Component thiếu `"use client"` |
| **`generateStaticParams` / `generateMetadata` lỗi runtime** | Chỉ chạy lúc build prerender | Async metadata throw khi build |
| **Env var dùng lúc build (`process.env.NEXT_PUBLIC_*`)** | Build inline env; missing → build behavior khác dev | Reference env var chưa set lúc build |
| **Import path case-mismatch** | Linux production build case-sensitive; macOS/Windows dev lenient | `import X from './Foo'` khi file là `foo.tsx` |

Rule **KHÔNG** fire khi:
- PR docs-only trong FE folder (`*.md`)
- Asset-only change (image/font/svg, không đụng source)
- Comment-only / formatting-only edit không đổi component logic

---

## 3. Required action (same PR, before push)

### 3.1 Chạy production build local cho FE package bị đụng

Package name từ `package.json` `name` field — dùng làm `--filter` value:

| FE app | Package name | Lệnh build local |
|---|---|---|
| KiteHub frontend | `kitehub-frontend` | `pnpm --filter kitehub-frontend build` |
| KiteClass frontend | `kiteclass-frontend` (verify `name` field) | `pnpm --filter kiteclass-frontend build` |

```bash
# Từ repo root (pnpm workspace)
pnpm --filter kitehub-frontend build
# Expect: "✓ Compiled successfully" + "✓ Generating static pages" + exit 0
# KHÔNG chỉ: pnpm --filter kitehub-frontend lint / type-check
```

Nếu chỉ đụng KiteClass FE → chạy filter cho `kiteclass-frontend`. Nếu đụng cả hai → chạy cả hai.

### 3.2 Nếu build FAIL → fix trước push

Build error (vd Suspense bailout, prerender error, RSC boundary) PHẢI fix trong cùng PR trước khi push. KHÔNG push với build broken trông chờ CI bắt.

### 3.3 Document build evidence trong PR body

Add section `## FE build local-verify (per fe-build-local-verify.md §3)`:

```markdown
## FE build local-verify

`pnpm --filter kitehub-frontend build` PASS local trước push:
- ✓ Compiled successfully
- ✓ Generating static pages (N/N)
- exit 0
```

---

## 4. Banned shortcuts

| ❌ Don't | ✅ Do |
|---|---|
| Push FE change sau khi chỉ `eslint` + `tsc --noEmit` PASS | `pnpm --filter <pkg> build` PASS local TRƯỚC push — prerender/Suspense chỉ lộ ở production build |
| "CI Docker build sẽ bắt lỗi này" (đúng nhưng tốn round-trip) | CI = canonical, nhưng local build = pre-push filter; bắt sớm hơn ~5-10 phút |
| Trust `pnpm dev` chạy OK = production build OK | Dev server bỏ qua prerender + static optimization; build behavior khác hẳn |
| Thêm `useSearchParams` không build local "vì component nhỏ" | Component nhỏ vẫn trigger prerender bailout; build mọi FE change |
| Skip build "vì chỉ sửa 1 dòng tsx" | 1 dòng (vd thêm hook) đủ phá prerender boundary; build vẫn cần |
| Document build "đã chạy" trong chat, không trong PR body | PR body section `## FE build local-verify` mandatory cho transparency + reviewer trail |
| Chạy build cho package KHÔNG bị đụng | Chỉ filter package(s) trong diff scope — tiết kiệm thời gian |

---

## 5. Enforcement (per `rule-change-process.md` §6.5 Enforcement Parity Mandate)

### 5.1 Reviewer-checklist (active now)

Pre-merge review cho PR đụng `kitehub/kitehub-frontend/**` hoặc `kitehub/kiteclass-frontend/**` source:

- [ ] PR diff đụng FE source (`*.tsx`/`*.ts` không phải docs/asset)?
- [ ] Nếu CÓ — PR body có section `## FE build local-verify` với `pnpm --filter <pkg> build` PASS evidence?
- [ ] Build evidence cho ĐÚNG package(s) trong diff scope (kitehub-frontend / kiteclass-frontend / cả hai)?
- [ ] Special-attention check: PR thêm `useSearchParams`/`usePathname`/`useRouter`/`next/dynamic`/`"use client"` boundary → build evidence bắt buộc (cao khả năng prerender bailout)?

### 5.2 Override mechanism

Genuine exception (vd local node_modules broken, build env không khả dụng, hotfix urgency):

```
git commit -m "...
FE_BUILD_LOCAL_VERIFY_OVERRIDE: <reason — e.g. 'local pnpm install broken, CI Docker build canonical fallback'>
FE_BUILD_LOCAL_VERIFY_FOLLOWUP: <gap link nếu cần fix local env, hoặc 'CI verified green' note>"
```

Trailer logged. Pattern frequency >10%/quarter triggers meta-review (likely local FE env reliability issue OR rule scope mis-defined).

### 5.3 CI detector (deferred per `incident-to-rule-pipeline.md` §3.1 tightened conditions)

CI Docker build (`docker-build-push.yml`) ĐÃ bắt lỗi này như canonical gate — rule này là **pre-push local filter** để tiết kiệm round-trip, KHÔNG thay thế CI. Quan hệ tương tự `ci-queue-local-runner-threshold.md` (local-CI = filter, CI = canonical).

Detector defer:
- **Detector complexity:** scan PR body cho FE-source diff signal + verify presence of `## FE build local-verify` section + parse build evidence — cần diff-scope parser + heuristic FP handling, NOT trivial bash
- **Recurrence count:** 1 pre-rule (GAP-801 Suspense bailout 2026-05-28); recurrence count starts từ rule landing
- **FP risk:** Moderate — docs-only/asset-only FE PRs không cần build evidence; flexible matcher cần phân biệt
- **Decision:** Reviewer-checklist §5.1 + worked self-test §6 sufficient cho v1.0.0; revisit detector khi recurrence-count ≥2 post-rule

### 5.4 Memory auto-load (optional, deferred)

Memory entry `feedback_fe_build_local_verify.md` could remind tại session start trước khi đụng FE source. Defer per `incident-to-rule-pipeline.md` §3.1 premature-rule guard ≥7 ngày; reviewer-checklist + worked self-test §6 đủ cho v1.0.0.

---

## 6. Worked self-test — GAP-801 Suspense bailout (2026-05-28)

Áp dụng rule retroactively vào GAP-801 FE incident:

**Bug class:** `BetaClaimCodeForm` component dùng `useSearchParams()` không bọc `<Suspense>` boundary.

**Pre-push state (what actually happened):**
- Dev chạy `eslint` local → PASS (lint không catch prerender bailout)
- Dev chạy `tsc --noEmit` (nếu có) → PASS (type-correct)
- Push branch + open PR
- CI Docker build (`next build`) → FAIL: "useSearchParams() should be wrapped in a suspense boundary" prerender bailout
- Round-trip: fix + re-push + wait CI lại (~5-10 phút lãng phí)

**Apply §3 rule retroactively (counterfactual):**
1. PR đụng `kitehub/kitehub-frontend/src/components/.../BetaClaimCodeForm.tsx` → §2 trigger fire (thêm `useSearchParams`)
2. Chạy `pnpm --filter kitehub-frontend build` local TRƯỚC push
3. Build FAIL ngay tại local với cùng prerender bailout error
4. Fix (bọc `<Suspense>`) → re-run build local → PASS
5. Push 1 lần → CI green first try

**Verdict:** Rule fires correctly trên GAP-801 originating incident. Counterfactual: local `next build` bắt Suspense bailout NGAY tại pre-push, eliminate 1 CI Docker build round-trip (~5-10 phút) + reviewer notice friction. Self-test PASS ✅

**Cost-save projection:** mỗi FE prerender/boundary bug bắt local thay vì CI = ~5-10 phút wall-clock saved per occurrence + CI compute. FE PRs đụng dynamic hooks recurring → force-multiplier per `meta-gap-priority.md` §3.

---

## 7. Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Đồng nhất "lint + tsc PASS" = "production build PASS" | 3 tầng riêng: lint (style) + tsc (type) + build (prerender/boundary) — chạy cả 3 |
| Trust dev server `pnpm dev` thay cho `pnpm build` | Dev bỏ qua prerender; chỉ `next build` reproduce production prerender |
| Build toàn workspace `pnpm -r build` khi chỉ đụng 1 app | Filter đúng package(s) trong scope — nhanh hơn |
| Đợi CI báo build fail rồi mới fix | Local build pre-push = filter; fix trước push |
| Bỏ build "vì component thuần presentational" | Presentational component vẫn có thể thêm hook gây bailout; build mọi FE change |

---

## 8. Relationship to other rules

- **`api-contract-change-caller-sweep.md`** v1.0.0 — sister rule cùng tinh thần "run tests/build not just compile/lint". Axis khác: nó cho BE method-contract change (`./mvnw test` không chỉ compile); rule này cho FE production build (`pnpm build` không chỉ lint/tsc).
- **`ci-queue-local-runner-threshold.md`** v1.0.0 §1 — local-CI = filter, CI = canonical. Rule này cùng quan hệ: local `next build` = pre-push filter, CI Docker build = canonical gate. CI vẫn chạy unconditionally.
- **`admin-merge-discipline.md`** v1.0.3 §3 — verify exact merge candidate local (`pnpm -F <pkg> test --run && pnpm -F <pkg> build && pnpm -F <pkg> lint`); rule này sharpens: build cụ thể là mandatory cho FE change, không chỉ lint.
- **`docs-only-pr-no-block-wait.md`** v1.0.0 §5 — FE source PR (out-of-scope của docs-only) cần careful CI gate; rule này thêm pre-push local build cho FE.
- **`meta-gap-priority.md`** §3 — META P0 force-multiplier (1 chuẩn FE build local → mọi FE change subsequent auto-comply prospectively → eliminate CI round-trip class).
- **`incident-to-rule-pipeline.md`** v1.1 — rule này = direct output của GAP-801 Suspense bailout incident applied through 5-stage pipeline.
- **`rule-change-process.md`** §6.5 Enforcement Parity Mandate — rule + reviewer-checklist + override trailer + worked self-test §6 paired same PR.
- **`output-review-mandate.md`** §3 — paired same-PR new matrix row "FE production-build local-verify" tracking review standard (coordinator thêm row để tránh xung đột).
- **`context-budget-mandate.md`** §3.2 — path-scoped `paths: ["kitehub/kitehub-frontend/**", "kitehub/kiteclass-frontend/**"]` — rule load chỉ khi context chạm FE source, không global auto-load.

---

## 9. Log

- **2026-05-28 (v1.0.0):** Rule created in response to GAP-801 FE Suspense bailout incident — `BetaClaimCodeForm` dùng `useSearchParams()` không bọc `<Suspense>` → `eslint` PASS nhưng `next build` (production build trong CI Docker) FAIL với "useSearchParams() should be wrapped in a suspense boundary" prerender bailout. Bug chỉ lộ khi CI chạy production build, KHÔNG lộ khi dev chạy lint/tsc local → 1 CI round-trip lãng phí. Per `incident-to-rule-pipeline.md` 5-stage applied: Detect ✓ (GAP-801 FE part CI fail) → Classify ✓ (no existing rule mandates FE production build local pre-push; `api-contract-change-caller-sweep.md` covers BE method-contract change run-tests-not-compile, KHÔNG cover FE build; `admin-merge-discipline.md` §3 lists `pnpm build` trong verify matrix nhưng generic merge-candidate verify, không mandate FE build specifically as pre-push filter; `ci-queue-local-runner-threshold.md` covers docs-only CI runner choice, không cover FE build) → Rule+Enforce ✓ (this file + reviewer-checklist §5.1 + override trailer §5.2 + worked self-test §6 trên GAP-801 originating incident + paired same-PR rules-index.csv row + output-review-mandate §3 row (coordinator) per `rule-change-process.md` §6.5 Enforcement Parity Mandate) → Self-Test ✓ (§6 worked example — rule fires correctly trên GAP-801 + counterfactual local build bắt Suspense bailout pre-push, eliminate ~5-10 phút CI round-trip) → Retro Log ✓ (this entry). META P0 force-multiplier per `meta-gap-priority.md` §3 — 1 chuẩn FE build local → mọi FE change subsequent auto-comply prospectively → eliminate "lint pass nhưng build fail" CI round-trip class. Reviewer: @nguyenvankiet (solo-dev MINOR self-approve per `rule-change-process.md` §5 — new constraint codifying previously-uncovered class "FE PR pass lint/tsc nhưng fail production build"; no constraint loosening; existing FE PRs grandfathered; rule applies prospectively từ this PR forward 2026-05-28). Atomic-unique-bar §5.1 check passed: ✅ atomic (single concept: FE production build local before push) + ✅ unique (sister `api-contract-change-caller-sweep` covers BE method-caller-sweep+run-tests axis; `ci-queue-local-runner-threshold` covers docs-only CI runner choice; NEITHER mandates FE production build specifically) + ✅ widely applicable (every FE source PR) + ✅ body discipline §1 ≤2 "and" conjunctions. Path-scoped `paths: ["kitehub/kitehub-frontend/**", "kitehub/kiteclass-frontend/**"]` per `context-budget-mandate.md` §3.1 — deferred-load chỉ khi context chạm FE source. CI detector (§5.3) + memory auto-load (§5.4) deferred per `incident-to-rule-pipeline.md` §3.1 tightened legitimate-deferral conditions (recurrence 1 pre-rule; reviewer-checklist + worked self-test sufficient cho v1.0.0; revisit khi recurrence ≥2 post-rule).
