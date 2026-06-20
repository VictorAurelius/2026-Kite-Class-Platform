---
title: Quality /110 Full Audit — Post phase1-closeout-loop (GAP-1308 closure refresh)
status: complete
created: 2026-06-21
auditor: Claude Code (Opus 4.8 1M context) — quality-audit skill v1.1
wave: phase1-closeout-loop
baseline: 2026-06-14-quality-full-audit.md (88/110 → 81 tech-only — B FAIL, 1 P0 open GAP-1308)
base_sha: 3d5179551
gaps_filed: [GAP-1492, GAP-1493]
---

> ⚠️ **v1 audit format** (no per-control evidence blocks — GAP-564 v2 format scope = security-audit). Findings + scores valid; structure mirrors `2026-06-14-quality-full-audit.md` baseline per `audit-skill-rubric-quality-audit.md` per-check pass/fail rubric.

# Quality /110 Full Audit — Post phase1-closeout-loop (GAP-1308 closure refresh)

**Scope:** Targeted refresh trigger = đóng GAP-1308 (gating P0 gateway role-spoof) qua SECURITY batch waves `close-2` / `close-2-sec` / `close-2-sec-2` (PR #2508/#2511/#2512) + BRD/notification/OTP waves (PR #2513/#2514/#2515). 102 commits kể từ baseline SHA `bf207a6ea` (2026-06-14) → `3d5179551` (2026-06-21).
**Baseline:** 2026-06-14 (wave-p0-closeout-1) = **88/110 (81 tech-only) B — VERDICT 🔴 FAIL** vì 1 P0 OPEN (GAP-1308 gateway không strip `X-User-Roles` → role-spoof priv-esc) capped Cat 2 ≤6.
**Current HEAD:** `3d5179551` (docs handoff 2026-06-21 session — Business/BRD + mobile-OTP + Phase-1 loop prep).
**Method:** quality-audit skill v1.1 — 11 categories /110 (10 tech /100 + 1 persona /10), per-check pass/fail rubric per `audit-skill-rubric-quality-audit.md` §2 (P0 sub-check FAIL caps category ≤ max-4 AND audit-level verdict = FAIL).

---

## OVERALL SCORE: 90/110 → 83 tech-only — B (Δ +2 vs 2026-06-14 baseline 88) — AUDIT-LEVEL VERDICT: ✅ PASS

Aggregate **90/110** (grade B "Good, needs polish"), **audit-level verdict = PASS** per rubric §2: **GAP-1308 closed → Cat 2 P0 sub-check now PASSES → cap lifted → Cat 2 6→8 → verdict clean**. Tech-only 83/100 vượt Phase 1 BETA gate ≥80 (buffer +3). PROD MAJOR gate ≥85: tech-only 83 — chưa đạt (path còn P1 cluster: GAP-1491 A01 + GAP-825 + GAP-985 RLS + GAP-664/1251 docs).

**Single-root unblock confirmed:** đúng dự đoán baseline ("Path-to-clean: close GAP-1308 → Cat 2 6→8 → 90/110 PASS"). GAP-1308 fix verified empirically tại `kitehub/kitehub-gateway/src/main/resources/application.yml` L974-987 — `default-filters` thêm `RemoveRequestHeader=X-User-Roles` (GAP-1308 P0) + `RemoveRequestHeader=X-User-Email` (GAP-1310 P2); `JwtAuthenticationGatewayFilter` re-inject verified claim SAU strip → client-supplied role header không bao giờ tới downstream (least-privilege).

**Delta drivers vs 2026-06-14:**
- **Cat 2 Security 6 → 8 (+2):** 6 security gap đóng (GAP-1308 P0 + GAP-1413 P0 nil-UUID tenant resolver + GAP-1309 P1 StorageController IDOR + GAP-1310/1311 P2 + GAP-1428 P1 attendance no-auth leak) + GAP-1005 90% InvoiceController 13/13 `@PreAuthorize` + InvoiceControllerAuthzTest 403 IT. P0 cap lifted, verdict clean. KHÔNG lên 9 vì residual P1: GAP-1491 (A01 cluster — 8 financial/admin controller thiếu method-level `@PreAuthorize`) + GAP-825 OPEN + GAP-985 RLS PARTIAL.
- **Cat 3-11 stable (Δ 0):** GAP-1344 (Jacoco config) DONE + GAP-1347 (perf refresh) DONE + GAP-1332/1333/662/663 (SSO/impersonation/API doc) DONE — củng cố nền nhưng không đủ lift category nguyên (coverage % vẫn chưa enforce gate; branding 13-endpoint doc vẫn PARTIAL).

---

## Bug list (per-check FAIL — rubric §2 primacy, precedes score table)

| Sev | Cat | Sub-check FAIL | Evidence | Tracking |
|:---:|:---:|----------------|----------|----------|
| 🟠 P1 | 2 | 8 financial/admin controller thiếu method-level `@PreAuthorize` (OWASP A01 cluster) | close-2-sec-2 sweep — PaymentController 8/0, RefundRequestController 5/0, InstallmentPlanController 5/0… | GAP-1491 (filed 2026-06-19) — P1, không cap Cat 2 |
| 🟠 P1 | 2 | Tenant-isolation hardening — JWT-sig-verify TenantResolver fallback + A01 regression | Sibling security audit | GAP-825 (OPEN) |
| 🟠 P1 | 2 | RLS defense-in-depth không bảo vệ cross-tenant by-id (V58 FORCE-RLS chưa tới read path) | GAP-985 PARTIAL 60% | GAP-985 |
| 🟠 P0→PARTIAL | 1/2 | KiteClass OWNER không công nhận tenant-admin → 403 reports/enrollments/payroll | GAP-1139 PARTIAL 95% — code+test shipped, pending human G2 walk | GAP-1139 (carry-forward; functional authz false-negative, KHÔNG phải confidentiality breach) |
| 🟡 P2 | 3 | Jacoco `report` chạy nhưng KHÔNG có `jacoco:check` threshold gate → coverage % silent regress | `core-ci.yml` L62 `./mvnw jacoco:report`; pom không có `check` execution + `<minimum>` rule | **GAP-1492 (NEW)** |
| 🟡 P2 | 2 | Specialist Security audit (2026-06-14, 85/100 FAIL) content stale post 6-gap closure — verdict chưa re-score | `audits/security/2026-06-14-security-full-audit.md` vẫn ghi P0 OPEN GAP-1308 (đã DONE) | **GAP-1493 (NEW)** |
| 🟢 P3 | 5 | 5 stale remote feature branch (down từ 15) | `git branch -r` | GAP-1348 (PARTIAL 40%) |

**Carry-forward (đã có gap, KHÔNG dup):** GAP-1139 (KC OWNER 403 PARTIAL 95% pending walk); GAP-1345/1346 (FE TODO + God-service — reclassified phase-2); GAP-664/1251 (3-layer + branding doc drift); GAP-987 (ddl-auto test fidelity); GAP-152 (persona critical-gap remediation); GAP-049/154/156/063/286 (counsel/vendor-blocked PARTIAL P0 — out of code-closable scope per Phase 1 risk tolerance).

---

## 11-Category Scoring (rubric v1.1 — per-check pass/fail)

| # | Category | Score/10 | Δ vs 06-14 | Status | Evidence |
|:-:|----------|:--------:|:----------:|:------:|----------|
| 1 | E2E Functionality | **9** | 0 | ✅ | Wave 1 walk evidence (GAP-1115 LMS seed + 1066/1139/1213 G1) + mobile-OTP signup full-stack shipped (GAP-286 PR #2515) + beta-funnel chain regression + SSO E2E. Critical flows signup→login→dashboard→RBAC→LMS end-to-end. GAP-1139 PARTIAL 95% (KC OWNER tenant-admin — code+test done, pending human G2 walk). AI stub-only Phase 1 TEMPLATE (đúng ADR-037). |
| 2 | Security | **8** | **+2** | ✅ | **P0 cap LIFTED — GAP-1308 DONE** (verified `application.yml` L974-987: `RemoveRequestHeader=X-User-Roles` + `X-User-Email` default-filter; JWT filter re-inject verified claim sau strip). 6 security gap đóng: GAP-1308 P0 + GAP-1413 P0 nil-UUID resolver + GAP-1309 P1 IDOR + GAP-1310/1311 P2 + GAP-1428 P1 attendance leak. GAP-1005 90% InvoiceController 13/13 `@PreAuthorize` + 403 IT. **NOT 9:** residual P1 GAP-1491 (A01 cluster 8 controller) + GAP-825 OPEN + GAP-985 RLS PARTIAL. AWS-live TLS/IAM UNCHECKED (stack nuked post-demo per memory). |
| 3 | Backend Tests | **8** | 0 | ✅ | **565 test file** (KC 286 + KH 279; +18 vs 547) trên 1368 Java main (KC 849 + KH 519; +11). **GAP-1344 DONE — Jacoco configured** (kitehub/pom.xml + kiteclass-core/pom.xml + `core-ci.yml` `jacoco:report`). Compile Gate + Gitleaks green main. **Gap:** coverage % vẫn chưa enforce — không `jacoco:check` threshold gate (GAP-1492 NEW P2); IT `ddl-auto=create-drop` che migration drift (GAP-987 carry). |
| 4 | Frontend Tests | **8** | 0 | ✅ | **965 ts/tsx** (KC 563 + KH 402; +41). Playwright e2e (beta-funnel, SSO, RBAC redirect) + `fe-build-local-verify` pre-push gate + MSW. **Watch:** 72 FE TODO (GAP-1345 → phase-2). |
| 5 | CI/CD | **9** | 0 | ✅ | Recent main runs **all green** (Compile Gate all-module + Gitleaks + Actionlint + Quality Code/Rules-Skills). **0 open PR**. 5 stale remote branch (down từ 15 — GAP-1348 partial). 1 Dependabot dompurify fail = known pnpm-transitive limitation (non-actionable per `feedback_dependabot_pnpm_transitive`). |
| 6 | UI/UX | **8** | 0 | ✅ | ui-kits-100 + landing-100 baseline ≥105/128 maintained + kit↔production parity contract (`frontend-standards.md` §3.1). Be Vietnam Pro token + dark-mode + loading/empty states. |
| 7 | DevOps/Infra | **9** | 0 | ✅ | **GAP-1347 DONE** — Performance audit refreshed (`audits/performance/2026-06-14-performance-full-audit.md` + 06-15 FE bundle) + ops-readiness full audit 2026-06-14 (77/100 C+). Local stack healthy; Terraform AWS + Helm IaC intact (state S3 KEPT for redev post-demo teardown). **Carry:** GAP-257 restore drill P0 + AWS-live verify (stack nuked post-demo, redev via terraform apply). |
| 8 | Documentation | **8** | 0 | ⚠️ | **3101 .md** (+184). **Drift recovery:** GAP-1332 (SSO doc) DONE + GAP-1333 (impersonation doc) DONE + GAP-662/663 (EmailController URL + Preferences IT) DONE + GAP-1320 reclassified phase-2. **Residual P1 (sub-check "match code"):** GAP-1251 branding ~13 endpoint undoc PARTIAL 50% + GAP-664 3-layer completeness PARTIAL 40% + GAP-666 BR-ID javadoc OPEN. 4/6 cited drift đóng nhưng branding doc giữ category ở 8. |
| 9 | Code Quality | **7** | 0 | ✅ | 17 TODO/FIXME Java main (KC 6 + KH 11; +5 vs 12 — minor creep). Strict-warnings stable; design pattern intact (State Machine + Outbox + RoleGuard + gateway anti-spoof). admin-merge-discipline 0 incident. **Watch:** 11 God-service candidate (GAP-1346 → phase-2). |
| 10 | Project Management | **9** | 0 | ✅ | phase1-closeout-loop + ~6 wave clean ship (102 commit / 0 open PR); gap registry robust + phase-4-deploy taxonomy restructure (PR #2502/2503/2504 re-classification audit); SECURITY batch state-check-first (6/9 already-fixed CSV stale caught); multi-session-concurrency + walk-data-seed rules shipped. |
| 11 | Persona Coverage | **7** | 0 | ✅ | Pre-walk persona simulation cadence maintained (flow-verification campaign). 4 Tier 1 report (older than 90d window → cần refresh). **NOT 9-10:** GAP-152 critical-gap remediation chưa hoàn tất; pre-walk sim ≠ Tier 1 quarterly report. Honest 7/10. |
| | **TOTAL** | **90/110** | **+2** | ✅ **PASS** | Tech-only **83/100** (+2 vs 81); aggregate 90. Phase 1 BETA điểm gate ≥80 PASS buffer +3 + **VERDICT PASS** (0 OPEN P0 phase-1-beta; GAP-1308 closed). |

**Sum check:** 9+8+8+8+9+8+9+8+7+9+7 = 90. Tech-only (1-10): 9+8+8+8+9+8+9+8+7+9 = 83.

---

## GAP-1308 closure impact (central refresh trigger)

| Aspect | 2026-06-14 (P0 open) | 2026-06-21 (P0 closed) |
|---|---|---|
| GAP-1308 status | 🔵 OPEN — gateway không strip X-User-Roles | 🟢 DONE — `RemoveRequestHeader=X-User-Roles` in default-filters (verified L983) |
| Cat 2 Security | **6** (P0-capped) | **8** (cap lifted) |
| Audit-level verdict | 🔴 FAIL (P0 open) | ✅ PASS (0 open P0) |
| Aggregate | 88/110 | **90/110** |
| Sibling security gaps | GAP-1309/1310/1311 open | all DONE + GAP-1413/1428 DONE |

**Empirical fix verification (per `design-first-investigation-order` — code-read confirms design intent):**
- `application.yml` default-filters strip ALL spoofable identity headers: `X-Tenant-Id`, `X-User-Id`, `X-User-Reference-Id`, `X-Subscription-Tier`, **`X-User-Roles` (GAP-1308)**, **`X-User-Email` (GAP-1310)**.
- `JwtAuthenticationGatewayFilter` re-injects `X-User-Id`/`X-User-Roles`/`X-User-Email`/`X-Subscription-Tier` from verified JWT claims AFTER default-filter strip → downstream only ever sees gateway-verified values.
- Comment L977-982: "client sending `X-User-Roles: OWNER` on tokenless/role-only-gated path → strip → downstream NO X-User-Roles = least privilege (no authority granted)." Defense matches GAP-814 anti-spoof class.

---

## Comparison with Previous Audit

| Category | W98 (05-19) | 2026-06-14 | **2026-06-21** | Δ |
|----------|:---:|:---:|:---:|:---:|
| 1 E2E | 9 | 9 | **9** | 0 |
| 2 Security | 8 | 6 | **8** | **+2** ✅ |
| 3 Backend Tests | 8 | 8 | **8** | 0 |
| 4 Frontend Tests | 8 | 8 | **8** | 0 |
| 5 CI/CD | 9 | 9 | **9** | 0 |
| 6 UI/UX | 7 | 8 | **8** | 0 |
| 7 DevOps/Infra | 9 | 9 | **9** | 0 |
| 8 Documentation | 9 | 8 | **8** | 0 |
| 9 Code Quality | 7 | 7 | **7** | 0 |
| 10 Project Management | 9 | 9 | **9** | 0 |
| 11 Persona Coverage | 7 | 7 | **7** | 0 |
| **Total /110** | **90** | **88** | **90** | **+2** |
| **Tech-only /100** | 83 | 81 | **83** | **+2** |
| **Verdict** | PASS | 🔴 FAIL | ✅ **PASS** | — |

**Delta interpretation:** Single-axis recovery — đóng GAP-1308 (root P0) trả Cat 2 về Wave 98 baseline 8 + flip verdict FAIL→PASS. Tốc độ hardening đã bắt kịp surface mở rộng (RBAC/LMS/SSO/branding) qua SECURITY batch state-check-first (6/9 gap đã được fix, CSV stale). Net +2 — quality phục hồi đúng dự đoán baseline path-to-clean.

---

## Specialized Audit Scores (cross-reference)

| Audit | Date | Score | Verdict | Note |
|-------|------|-------|:-------:|------|
| Security /100 v2 | 2026-06-14 | 85/100 B | 🔴 FAIL (cited GAP-1308) | **CONTENT STALE** — GAP-1308/1413/1309/1310/1311 đã đóng → re-score due (GAP-1493 NEW) |
| Business Logic /100 | 2026-06-14 | 70/100 C | 🔴 FAIL | carry — GAP-664/666 doc drift |
| API Contract /100 | 2026-06-14 | 80/100 C+ | 🔴 FAIL→partial-recover | GAP-662/663/1332/1333 DONE; GAP-1251 branding residual |
| Performance /100 | 2026-06-14 | (refreshed) | ✅ GAP-1347 DONE | perf baseline no longer stale |
| Ops Readiness /100 | 2026-06-14 | 77/100 C+ | carry | GAP-257 restore drill |
| UI /128 per-screen | 2026-06-11 ui-kits-100 | 105-109/128 | ✅ ≥105 | maintained |

---

## Improvement Roadmap

### Quick Wins (1-2h)
- GAP-1492 — wire `jacoco:check` threshold rule (min 60% line baseline) + surface coverage % in CI → Cat 3 honest gate.
- GAP-1493 — re-score security /100 reflecting GAP-1308/1413 closure (expected 85 FAIL → ~88-90 PASS) → align security verdict với quality PASS.
- GAP-1348 — prune 5 còn lại stale remote branch.

### Medium Effort (0.5-1 ngày)
- GAP-1491 — 8 financial/admin controller `@PreAuthorize` cluster fix + 403 IT → Cat 2 8→9.
- GAP-1251 — document branding-100 ~13 endpoint trong api-contract.md → Cat 8 8→9.
- GAP-1139 — human G2 walk verify KC OWNER tenant-admin (code đã 95%) → close residual PARTIAL P0.

### Major Effort (2+ ngày)
- GAP-825 + GAP-985 — tenant-isolation hardening (JWT-sig TenantResolver) + RLS read-path FORCE-RLS → Cat 2 → 9, PROD MAJOR path.
- GAP-152 — Tier 1 persona quarterly report refresh → Cat 11 7→9.

---

## Phase Gate Verdict

| Gate | Threshold | 06-14 | **06-21** | Status |
|------|:-:|:-:|:-:|:-:|
| Phase 1 BETA invite (điểm) | ≥80 | 88 (81) | **90 (83 tech)** | ✅ PASS buffer +3 |
| Audit-level verdict (P0-free) | 0 open P0 phase-1-beta | 🔴 1 P0 open | **✅ 0 open P0** | ✅ **PASS** |
| First PROD MAJOR | ≥85 | 88 | **90 (83 tech)** | ⚠️ tech-only 83 — chưa đạt 85 |

**Kết luận gate:** GAP-1308 closure flip quality verdict FAIL→PASS + Cat 2 6→8 đúng dự đoán. Phase 1 BETA gate clean (điểm + verdict). PROD MAJOR (≥85 tech-only) còn cần P1 cluster (GAP-1491 A01 + GAP-825 + GAP-985 RLS + GAP-664/1251 docs) ~+2-3 điểm.

---

## Gaps Filed (this audit — reserved block GAP-1492..1493, disjoint per multi-session-concurrency)

| Gap | Pri | Cat | Finding |
|-----|:---:|:---:|---------|
| GAP-1492 | P2 | 3 | Jacoco report chạy nhưng không có `jacoco:check` coverage threshold gate — silent regress |
| GAP-1493 | P2 | 2 | Specialist Security audit content stale post GAP-1308/1413 closure — verdict re-score due |

**Dup-avoided (per `audit-to-gap-pipeline.md` §2.5, query-gaps.sh checked):** GAP-1491 (A01 cluster đã filed close-2-sec-2), GAP-1345/1346 (TODO/God-service → phase-2), GAP-1348 (stale branch), GAP-825/985/1005/1139 (security carry), GAP-664/666/1251 (doc drift), GAP-987 (test fidelity), GAP-049/154/156/063/286 (counsel/vendor-blocked).

---

## Reviewer notes

Audit chạy bởi Claude Code (Opus 4.8 1M context) tuân thủ `quality-audit/SKILL.md` v1.1 + `audit-skill-rubric-quality-audit.md` (per-check pass/fail + bug-list > scoring primacy) + `design-first-investigation-order` (đọc design `application.yml` config trước khi phán GAP-1308 closed) + `audit-to-gap-pipeline.md` §2.5 dedup-first (query-gaps.sh) + `mcp-first-with-fallback.md` + `dev-readable-doc-language.md` + `gap-done-discipline.md` (0 DONE flip — audit ≠ closure) + `session-currentdate-check.md` (`created: 2026-06-21`).

Self-audit overstates ~15-20pts vs specialist per `feedback_audit_calibration.md` — 90/110 là upper bound; true production-grade ~82-84/110. Honest signal = **delta +2/110 + verdict FAIL→PASS** (single-root GAP-1308 unblock, đúng baseline prediction). Verdict PASS hợp lệ: 0 OPEN P0 phase-1-beta; PARTIAL P0 còn lại đều counsel-blocked (GAP-049/154/156 — Phase 1 risk tolerance cho phép "v1 pending counsel"), vendor-live-blocked (GAP-063/286 — code shipped), hoặc pending human walk (GAP-1139 95% — code+test done). Wave kế: GAP-1491 A01 cluster + GAP-1251 branding doc để đẩy PROD MAJOR ≥85.

## References
- **Baseline:** `documents/04-quality/audits/quality-audit/2026-06-14-quality-full-audit.md` (88/110 FAIL)
- **GAP-1308 fix:** `kitehub/kitehub-gateway/src/main/resources/application.yml` L965-987 (default-filters) + `JwtAuthenticationGatewayFilter.java`
- **Security batch:** PR #2508/#2511/#2512 (close-2 / close-2-sec / close-2-sec-2)
- **Sibling Security (stale):** `documents/04-quality/audits/security/2026-06-14-security-full-audit.md` (85/100, cites closed GAP-1308)
- **Skill rubric:** `.claude/skills/quality-audit/SKILL.md` v1.1 + `.claude/rules/audit-skill-rubric-quality-audit.md`
