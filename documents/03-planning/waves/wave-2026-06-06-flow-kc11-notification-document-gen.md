---
title: Wave flow-kc11 — KC-11 Notification + document generation G1 walk
status: complete
created: 2026-06-06
updated: 2026-06-06
waves: [flow-kc11]
wave: wave-2026-06-06-flow-kc11
tag_primary: flow-kc11
tags_secondary: [notification, document-gen, pdf, reports, campaign-g1]
date: 2026-06-06
flow: KC-11 (Notification Zalo OA + email + document gen PDF)
gaps: [GAP-1039, GAP-1040]
---

# Wave flow-kc11 — KC-11 Notification + document generation G1 walk

**Mục tiêu:** G1 (agent runtime walk) cho flow KC-11 — document generation (PDF/XLSX/DOCX) + reports (revenue/attendance) + notification (Zalo OA stub + parent facet). Flow secondary thứ 8. Sau wave này chỉ còn KC-12 để hoàn tất G1-all-first.

## 1. Brainstorm

KC-11 = kiteclass-core document/report/notification. Document gen real (DocumentGenerationController + pdf/xlsx/docx generators); reports ADMIN-only; Zalo OA = stub (GAP-721); parent notifications đã walk KC-8. Risk class: routing collision (KC-10 recurrence?), document-data IDOR, report tenant-scoping, format injection, SSRF/template injection, XLSX formula injection.

## 2. Task Breakdown

1. Pre-walk Opus persona-sim (≥5 FM) → artifact.
2. MUST-run (routing / role-bridge / reports tenant-scope / SSRF / NPE).
3. Walk document gen (pdf/xlsx/docx happy + injection) + reports + security spot-checks.
4. Catalog → file gaps → wave plan + sync.

## 3. Scope

- `kiteclass-core`: `DocumentGenerationController` (`/api/v1/documents/{format}/{preview,download}`, ADMIN/OWNER/TEACHER) + pdf/xlsx/docx generators + `DocumentBrandingAssembler`; `ReportController` (`/api/v1/reports/{revenue,attendance}`, ADMIN); `ParentNotificationsFacetController` (walked KC-8); `ZaloOaNotificationServiceImpl` (stub GAP-721).

## 4. State-Check Evidence

- Stack up healthy. Auth: OWNER (kitehub login) + minted ADMIN/TEACHER HS512 tokens (gateway JWT_SECRET) same tenant aaaabbbb-…-0001.
- Templates: PDF `invoice`, XLSX `attendance`, DOCX `teacher-contract`/`thesis-report`.
- DocumentGen returns `ResponseEntity<byte[]>` stream (KHÔNG lưu MinIO → tránh GAP-1036 bucket-500 class).

## 5. Verification Gates

### Pre-walk

Opus persona-sim, 10 FM, artifact `documents/04-quality/audits/persona-review/2026-06-06-pre-walk-kc11-notification-document-gen.md` (🟠3 🟡3 🟢4). 2 headline flip worst-case: (i) **NO routing collision** (documents/reports fall through catch-all → kiteclass-core đúng, khác KC-10); (ii) **NO document-data IDOR** (DTO chỉ templateId+data map, service không fetch entity by id — caller POST toàn bộ content).

### G1 walk — evidence (live)

**Happy paths (PASS):**
- Document gen: `POST /documents/pdf/preview` invoice → **200** PDF 422KB (valid, 1 page); `/xlsx/download` attendance → 200 3987B; `/docx` teacher-contract reachable. Format injection `/documents/exe/download` → **400** (allowlist). Missing body → **400** (không 500).
- Reports: ADMIN `GET /reports/revenue` → **200** (2M scoped); `/attendance` reachable.
- Role bridge: TEACHER → documents 200 ✅; TEACHER → reports **403** (ADMIN-only) ✅; OWNER → reports 403 (đúng, không phải ADMIN).

**Security spot-checks:**
- **FM-1 P1 CONFIRMED — reports cross-tenant leak:** direct :8080 revenue NO X-Tenant-Id → 200 totalRevenue **3.5M (all-tenant SUM)** vs WITH header → 2M scoped. Repos lack instance_id predicate; TenantFilterInterceptor absent-header → log-not-reject. → GAP-1039.
- **FM-2 P1 CONFIRMED — SSRF:** `data.logoUrl=http://169.254.169.254/…` accepted → PDF generated, OpenHTMLtoPDF server-fetch th:src. Caller branding override (DocumentBrandingAssembler putAll). No host allowlist. → GAP-1040.
- **FM-3 REFUTED:** documents no-tenant-context → 400 (không NPE 500 như predicted).
- **NO doc-data IDOR** ✅ (DTO no entity-id); **routing OK** ✅; **XLSX formula injection NEGATIVE** (caller rows không map cells, data từ DB); document stream no-MinIO ✅.
- **Zalo OA stub:** graceful no-op (`log.info("would send Zalo OA…")`, REQUIRES_NEW); hardcodes nil-UUID tenant (`ZaloOaNotificationServiceImpl:137-141`, "Wave 106 reconcile") — **known stub, tracked GAP-721/Wave 106, không file duplicate**.
- KC-11 notifications all stub/outbox (no email/MailHog/RabbitMQ dispatch; parent facet returns empty page — by-design, không phải bug).

**Bug surfaced (2 P1 — all filed, no inline fix):**
- 🟠 **GAP-1039 P1** (FM-1): reports cross-tenant aggregate leak.
- 🟠 **GAP-1040 P1** (FM-2): document gen SSRF via logoUrl override.

**No inline fix** — cả 2 là security (multi-tenant isolation + SSRF) cần careful review → batch Wave security-1 cùng GAP-983/1035/etc. Per `release-fix-retry-budget` §3.5 investigation-first done.

## 6. Agent Spawn Pattern

1 Opus pre-walk persona-sim (background, model opus). Walk solo coordinator.

## 7. Closure Protocol

### Discoveries filed (per `discovery-to-gap-inline-filing.md` §3)

- GAP-1039 P1 — reports cross-tenant aggregate leak (Backend/security)
- GAP-1040 P1 — document gen SSRF logoUrl override (Backend/security)

### Cross-flow sweep (per `cross-flow-bug-class-sweep.md`)

**GAP-1039 class (repo relies on Hibernate filter, no explicit tenant predicate):** sweep other report/aggregate repos in kiteclass-core for missing instance_id WHERE — defer to security-1 tenant-isolation audit (sister GAP-983). Signals systemic "filter-only tenant isolation" review needed.

**GAP-1040 class (caller data override server-controlled fetch-able field):** check other document templates (docx teacher-contract/thesis-report, xlsx) for same caller-URL injection — documented in GAP-1040 (invoice th:src is the confirmed vector; other templates use th:text).

### Sync targets

- gap-status.csv: 2 rows ✅
- campaign §4 table: KC-11 → 🔄 walk-pass-pending-human ✅
- wave-history.jsonl: flow-kc11 entry ✅
- audits-index.csv: pre-walk row ✅

### Outcome

KC-11 G1 **PASS** — document gen (pdf/xlsx/docx) + reports + role bridge all functional; routing OK (no collision); no doc IDOR; Zalo stub graceful. 2 P1 security (reports cross-tenant + SSRF) filed cho Wave security-1. Cleaner than KC-10 (no P0, no walk-blocker). Campaign KC-11 → `🔄 walk-pass-pending-human`. Docs-only PR. Remaining G1: **KC-12** (cuối cùng).

## 8. Log

- **2026-06-06:** G1 walk. Pre-walk Opus 10 FM (2 headline flip: no routing collision + no doc-data IDOR). Walk: PDF/XLSX/DOCX gen 200 + format-injection 400 + missing-body 400; reports ADMIN 200 / TEACHER 403; role bridge OK. Security: FM-1 reports cross-tenant leak CONFIRMED (3.5M all-tenant vs 2M scoped, GAP-1039); FM-2 SSRF logoUrl override CONFIRMED (GAP-1040); FM-3 NPE REFUTED (400); XLSX formula NEGATIVE; Zalo stub graceful nil-UUID (GAP-721/Wave 106 known). No inline fix (security-1 batch). Campaign → walk-pass-pending-human.
