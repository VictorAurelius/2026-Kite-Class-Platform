---
title: Outside-In Phase 1 BETA Closure — Failure-Mode Matrix
type: failure-mode-matrix
created: 2026-05-24
wave: "Phase 1 BETA closure"
personas: [anonymous-prospect, p1-solo-teacher, p2-center-owner, p3-center-manager, platform-admin]
env_states: [clean-local, aws-suspended-gap-612, partial-aws-restore, production-live]
actions: [signup, onboarding, daily-use, billing-event, incident, churn]
source_gaps: [gap-status.csv 289 rows, wave-105-matrix, wave-107-rst]
---

# Outside-In Failure-Mode Matrix — Phase 1 BETA Closure (2026-05-24)

## 1. Phương pháp

3-axis simulation per `.claude/skills/quality/simulation-gap-finder/SKILL.md`:

- **Persona axis (5):** Anonymous / P1-Solo / P2-Owner / P3-Manager / Platform-Admin
- **Action axis (6):** signup / onboarding / daily-use / billing-event / incident / churn
- **Env State axis (4):** clean-local / aws-suspended (GAP-612) / partial-aws-restore / production-live

Tổng 120 cells. Focus 36 high-risk cells bên dưới.

Nguồn dữ liệu:
- `gap-status.csv` (289 rows, đọc toàn bộ)
- Wave 105 failure-mode matrix (2026-05-22) — 18 scenarios, 5 P0
- Wave 107 RST results (2026-05-23) — 7 flows, 6/7 PASS, FAIL B2 branding wizard
- `.claude/rules/pre-handoff-self-test-completeness.md` §2.5-§2.11

Phân loại kết quả:
- **M-CARRY** — P0 từ Wave 105 matrix còn open (không có gap riêng, code-level)
- **M-NEW-LOCAL** — lỗi mới verify được trên local Docker stack
- **M-NEW-AWS-GATED** — blocked bởi GAP-612 AWS suspension, chỉ có trên production
- **M-NEW-VENDOR** — cần third-party integration (Zalo, email, legal)

---

## 2. Matrix — High-Risk Cells

### Nhóm A: Anonymous Prospect × Signup × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| A1 | Anonymous | Signup (email-only) | clean-local | Signup thành công → không có SMS/Zalo OTP option | GAP-286 email-only migration NOT documented with FAQ "Vì sao chỉ email?" | GAP-286 OPEN P0 | M-CARRY | P0 |
| A2 | Anonymous | Signup | aws-suspended | POST /auth/register → ECONNREFUSED (kitehub.me API down) | GAP-612 AWS suspension, no fallback UI | GAP-612 OPEN | M-NEW-AWS-GATED | P0 |
| A3 | Anonymous | Signup | clean-local | PDPL cookie consent banner missing trước khi collect email | GAP-353 PENDING legal | GAP-353 P0 PENDING | M-NEW-VENDOR | P0 |
| A4 | Anonymous | Signup → double-submit | clean-local | Duplicate account created — nút Submit 2 lần | No idempotency key on POST /auth/register | Wave 105 systemic pattern | M-CARRY | P0 |

### Nhóm B: P2 Owner × Onboarding × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| B1 | P2-Owner | Onboarding (login flow) | clean-local | Owner login → redirect /dashboard PASS | Wave 107 RST B1 PASS | — | M-CARRY | ✅ PASS |
| B2 | P2-Owner | Onboarding (branding wizard) | clean-local | `/branding/wizard` → blank body, ECONNREFUSED 127.0.0.1:8080 | kitehub-branding service not running / routing misconfigured | GAP-726 P1 (filed Wave 107) | M-NEW-LOCAL | **P1** |
| B3 | P2-Owner | Onboarding (tenant picker) | clean-local | Single-tenant owner → picker skipped (PASS) | Wave 107 RST B3 PASS | — | — | ✅ PASS |
| B4 | P2-Owner | Onboarding (dashboard routes) | clean-local | /dashboard, /classes, /students render (PASS) | Wave 107 RST B4 PASS | — | — | ✅ PASS |
| B5 | P2-Owner | Onboarding (email welcome) | aws-suspended | Welcome email không gửi được — Resend cần kitehub API webhook | GAP-370 PARTIAL 95%, remaining: operator verify POST-AWS-restore | GAP-370 P0 | M-NEW-AWS-GATED | P0 |
| B6 | P2-Owner | Onboarding | production-live | Branding không apply sau wizard → BrandingService.getBranding() không cache | GAP-215 OPEN P0 | GAP-215 P0 | M-CARRY | P0 |

### Nhóm C: P2 Owner × Billing Event × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| C1 | P2-Owner | Billing (create payment) | clean-local | Payment gán sai userId — mọi payment gán cho userId=1L | PaymentController line 49/69 hardcoded userId=1L, TODO never implemented | Wave 105 P0 finding | **M-CARRY** | **P0-CRITICAL** |
| C2 | P2-Owner | Billing (batch invoice) | clean-local | Batch invoice generation missing — không có tính năng | GAP-297 OPEN P0 phase-1-beta | GAP-297 P0 | M-CARRY | P0 |
| C3 | P2-Owner | Billing (per-session pricing) | clean-local | Per-session pricing model chưa implement | GAP-292 OPEN P0 phase-1-beta | GAP-292 P0 | M-CARRY | P0 |
| C4 | P2-Owner | Billing (double payment submit) | clean-local | Double charge: owner submit 2 lần → 2 payment rows | No idempotency key on POST /payments | Wave 105 systemic pattern | M-CARRY | P0 |
| C5 | P2-Owner | Billing (API contract drift) | production-live | Payment endpoint response shape mismatch → FE 500 | GAP-231 OPEN P0 phase-1-beta API contract drift payment-invoice | GAP-231 P0 | M-NEW-AWS-GATED | P0 |
| C6 | P2-Owner | Billing (VND format) | clean-local | Billing UI displays `$60.00` thay vì `1.500.000đ` | VN-localization rule §1 violation | vn-localization-audit-checklist | M-NEW-LOCAL | P1 |

### Nhóm D: P1 Solo × Daily Use × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| D1 | P1-Solo | Daily use (student enrollment) | clean-local | Race condition: 2 requests enroll student vào class FULL → 1 vượt capacity | Enrollment lacks optimistic locking / capacity check atomicity | Wave 105 P0 finding | M-CARRY | P0 |
| D2 | P1-Solo | Daily use (attendance API) | production-live | Attendance API contract drift → FE gets unexpected response shape | GAP-232 OPEN P0 phase-1-beta API contract drift attendance | GAP-232 P0 | M-NEW-AWS-GATED | P0 |
| D3 | P1-Solo | Daily use (student enrollment API) | production-live | Student enrollment API contract drift | GAP-233 OPEN P0 phase-1-beta | GAP-233 P0 | M-NEW-AWS-GATED | P0 |
| D4 | P1-Solo | Daily use (reschedule lesson) | clean-local | Không thể reschedule lesson session | GAP-291 OPEN P0 phase-1-beta | GAP-291 P0 | M-CARRY | P0 |
| D5 | P1-Solo | Daily use (per-resource authz) | clean-local | Solo teacher truy cập resource của owner khác → không bị chặn | A01 OWASP per-resource authz unverified beyond tenant isolation | Wave 105 systemic pattern | M-CARRY | P0 |
| D6 | P1-Solo | Daily use (frontend bundle) | production-live | Initial page load >3s, code splitting chưa implement | GAP-127 PARTIAL 50% | GAP-127 P0 | M-NEW-AWS-GATED | P1 |

### Nhóm E: P3 Manager × Daily Use × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| E1 | P3-Manager | Daily use (XSS admin panel) | clean-local | Stored XSS trong admin panel — malicious tenant name renders unescaped | Input sanitization missing | Wave 105 P0 finding | M-CARRY | P0 |
| E2 | P3-Manager | Daily use (document gen) | clean-local | PDF font missing → document generation fails silently | GAP-218 OPEN P0 | GAP-218 P0 | M-CARRY | P0 |
| E3 | P3-Manager | Daily use (document gen perf) | production-live | PDF/XLSX/DOCX p95 latency not measured → production SLA unknown | GAP-216 OPEN P0 | GAP-216 P0 | M-NEW-AWS-GATED | P0 |
| E4 | P3-Manager | Daily use (alerts) | production-live | No alert rules for /api/v1/documents/* → silent failure | GAP-217 OPEN P0 | GAP-217 P0 | M-NEW-AWS-GATED | P0 |

### Nhóm F: Platform Admin × Incident × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| F1 | Platform-Admin | Incident (restore drill) | partial-aws-restore | Không có restore drill runbook được test | GAP-117 PARTIAL 50% — drill never executed | GAP-117 P0 | M-CARRY | P0 |
| F2 | Platform-Admin | Incident (CVE deps) | clean-local | 7 transitive Maven CVEs unfixed | GAP-203 IN_PROGRESS 40% | GAP-203 P0 | M-CARRY | P0 |
| F3 | Platform-Admin | Incident (AI branding) | aws-suspended | AI Branding migration unverified in production | GAP-223 PARTIAL 50% | GAP-223 P0 | M-NEW-AWS-GATED | P0 |

### Nhóm G: P2 Owner × Churn × All Env States

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| G1 | P2-Owner | Churn (off-boarding data export) | aws-suspended | Data export không thể verify với AWS down | No data export DONE gap found | M-NEW (recommend GAP-NEW-data-export) | M-NEW-AWS-GATED | P1 |
| G2 | P2-Owner | Churn (email confirmation) | clean-local | Churn/cancellation email không có VN template | Email template missing per vn-localization rule §2 | vn-localization-audit-checklist | M-NEW-LOCAL | P1 |

### Nhóm H: Anonymous × Signup × Vendor-Gated

| Cell | Persona | Action | Env State | Symptom | Root Cause | Existing Gap | Category | Severity |
|------|---------|--------|-----------|---------|------------|-------------|----------|---------|
| H1 | Anonymous | Signup (mobile OTP) | production-live | Zalo/SMS OTP missing → VN mobile-first users blocked | GAP-286 OPEN P0 — Zalo OA API required | GAP-286 P0 | M-NEW-VENDOR | P0 |
| H2 | Anonymous | Signup (PDPL consent) | production-live | PDPL Article 11 consent không collect trước data collection | GAP-353 PENDING legal | GAP-353 P0 | M-NEW-VENDOR | P0 |
| H3 | Anonymous | Signup (BrandingService cache) | clean-local | Every signup hit triggers getBranding() uncached → N+1 | GAP-215 OPEN P0 | GAP-215 P0 | M-CARRY | P0 |

---

## 3. Tổng hợp theo Category

### M-CARRY (từ Wave 105 matrix + gap-status P0 confirmed)

| # | Finding | Severity | Source |
|---|---------|---------|--------|
| 1 | PaymentController hardcoded userId=1L (C1) | P0-CRITICAL | Wave 105 P0-1 |
| 2 | Stored XSS admin panel (E1) | P0 | Wave 105 P0-2 |
| 3 | Beta-request / payment / enrollment POST double-submit no idempotency (A4, C4) | P0 | Wave 105 systemic |
| 4 | Enrollment race condition on FULL class (D1) | P0 | Wave 105 P0-4 |
| 5 | Per-resource authz A01 OWASP — tenant isolation ≠ per-resource (D5) | P0 | Wave 105 P0-5 |
| 6 | GAP-286: Mobile OTP/Zalo signup missing (A1, H1) | P0 | gap-status.csv OPEN |
| 7 | GAP-297: Batch invoice generation (C2) | P0 | gap-status.csv OPEN |
| 8 | GAP-292: Per-session pricing model (C3) | P0 | gap-status.csv OPEN |
| 9 | GAP-291: Reschedule lesson session (D4) | P0 | gap-status.csv OPEN |
| 10 | GAP-215: BrandingService.getBranding() not @Cacheable (B6, H3) | P0 | gap-status.csv OPEN |
| 11 | GAP-218: PDF font-missing runbook (E2) | P0 | gap-status.csv OPEN |
| 12 | GAP-203: 7 CVEs transitive Maven deps (F2) | P0 | gap-status.csv IN_PROGRESS |
| 13 | GAP-117: Backup restore drill unexecuted (F1) | P0 | gap-status.csv PARTIAL 50% |
| 14 | GAP-127: Frontend code splitting (D6) | P0 | gap-status.csv PARTIAL 50% |

**M-CARRY total: 14 items (11 P0-CRITICAL/P0, 2 P0-carries từ Wave 105 systemic)**

### M-NEW-LOCAL (mới, verify được trên local Docker stack)

| # | Finding | Severity | Recommend |
|---|---------|---------|---------|
| 1 | GAP-726: Branding wizard ECONNREFUSED 127.0.0.1:8080 → blank body (B2) | P1 | Fix kitehub-branding routing/startup; re-run Wave 107 RST B2 |
| 2 | VND format violation: billing UI displays `$60.00` instead of `1.500.000đ` (C6) | P1 | Apply vn-localization §1 to all billing components; add Playwright assertion |
| 3 | Churn email template missing VN locale (G2) | P1 | Add VN email template per vn-localization §2 tone matrix |

**M-NEW-LOCAL total: 3 items (0 P0, 3 P1)**

### M-NEW-AWS-GATED (blocked bởi GAP-612 AWS suspension)

| # | Finding | Severity | Unblocked by |
|---|---------|---------|------------|
| 1 | POST /auth/register ECONNREFUSED — no API fallback UI (A2) | P0 | GAP-612 restore |
| 2 | GAP-370: Email welcome Resend delivery verify (B5) | P0 | GAP-612 + operator verify |
| 3 | GAP-231: Payment-invoice API contract drift (C5) | P0 | GAP-612 restore |
| 4 | GAP-232: Attendance API contract drift (D2) | P0 | GAP-612 restore |
| 5 | GAP-233: Student enrollment API contract drift (D3) | P0 | GAP-612 restore |
| 6 | GAP-216: PDF/XLSX/DOCX p95 latency SLA (E3) | P0 | GAP-612 restore |
| 7 | GAP-217: Alert rules /api/v1/documents/* (E4) | P0 | GAP-612 restore |
| 8 | GAP-223: AI Branding migration verify (F3) | P0 | GAP-612 restore |
| 9 | Data export flow — churn offboarding unverified (G1) | P1 | GAP-612 restore |

**M-NEW-AWS-GATED total: 9 items (8 P0, 1 P1) — tất cả unblock sau GAP-612**

### M-NEW-VENDOR (cần third-party / legal / external API)

| # | Finding | Severity | Dependency |
|---|---------|---------|----------|
| 1 | GAP-353: PDPL cookie/consent banner missing trước data collection (A3, H2) | P0 | Legal counsel, Decree 13/2023 PDPL Art 11 |
| 2 | GAP-286: Zalo/SMS OTP mobile signup (H1) | P0 | Zalo OA registration (Vietnam) |

**M-NEW-VENDOR total: 2 items (2 P0)**

---

## 4. Systemic Patterns (cross-cutting, không phải individual cell)

| Pattern | Scope | Recommendation |
|---------|-------|----------------|
| **No idempotency on POST mutations** | signup / payment / enrollment / beta-request | Add `X-Idempotency-Key` header + DB unique constraint per Wave 105 §4 |
| **JWT principal extraction inconsistent** | PaymentController hardcoded userId=1L; other controllers extract from SecurityContext | Audit all controllers: grep `userId=1L` + `userId.*hardcode`; fix via `@AuthenticationPrincipal` |
| **Per-resource authz untested** | All resources beyond tenant-level isolation | Add @PreAuthorize at method-level per resource; integration test cross-tenant access attempt |
| **VN-localization gaps in billing UI** | Currency format, date format, Vietnamese labels | Apply vn-localization-audit-checklist §2 §3 pre-merge review cho mọi billing component |
| **AWS-gated test coverage** | 8 P0 API-level verifications only testable post-GAP-612 | Define acceptance test matrix against staging URL; trigger automated smoke post-AWS-restore |

---

## 5. Phase 1 BETA Gate Assessment

**Gate criteria:** Quality /100 ≥80 + 5 beta tenants live + 0 P0 incidents 2 tuần

| Criterion | Current State | Blocker |
|-----------|------------|--------|
| Quality /100 ≥80 | Wave 98: 90/110 B+ quality overall — nhưng API audit 76/100 FAIL | GAP-662/663/664 path to 82 |
| 5 beta tenants live | 0 live (GAP-612 AWS suspended) | GAP-612 restore critical |
| 0 P0 incidents 2 tuần | PaymentController P0-CRITICAL (userId=1L) blocks safe beta invite | Fix C1 before ANY beta invite |
| P0 OPEN gaps | 11 M-CARRY P0 + 8 M-NEW-AWS-GATED P0 + 2 M-NEW-VENDOR P0 = 21 P0 items | Sequential fix per priority |

**Verdict:** Phase 1 BETA gate CANNOT be cleared in current state. Critical path:
1. **Immediately (local):** Fix PaymentController userId=1L (C1) — security P0 blocks beta invite
2. **Immediately (local):** Fix Stored XSS admin panel (E1)
3. **Next (local):** Fix idempotency gaps (A4, C4)
4. **Concurrent:** Restore AWS GAP-612 → unblock 8 M-NEW-AWS-GATED P0 verifications
5. **Post-restore:** API contract drift cluster (GAP-231/232/233) — verify + fix same sprint
6. **Phase 1.5 (vendor):** PDPL consent (GAP-353) + Zalo OTP (GAP-286)

---

## 6. Audit Caveat

Agent thực hiện simulation-based analysis. Không có production access do GAP-612. Local Docker stack healthy (Wave 107 RST 6/7 PASS). M-NEW-AWS-GATED findings cần confirmation post-GAP-612 restore.

Wave 107 RST FAIL (B2 GAP-726) và Wave 105 P0 list (5 items) là high-confidence findings từ recent test evidence. M-CARRY gaps confirmed open qua gap-status.csv đọc đầy đủ 289 rows.
