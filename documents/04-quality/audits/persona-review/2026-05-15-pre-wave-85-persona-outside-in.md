---
title: Pre-Wave 85 Persona Outside-In Audit — Multi-tenant Security + Performance + Tier 2 Config
status: complete
created: 2026-05-15
phase: phase-1-beta
wave: 85
gaps: [GAP-466, GAP-469, GAP-432, GAP-503, GAP-506, GAP-475]
---

# Pre-Wave 85 Persona Outside-In Audit

## 1. Scope

Audit outside-in per `outside-in-coverage-trigger.md` §3 Bước 2 (Bucket A của Wave 85 plan). Wave 85 = 8 buckets:
- A Outside-in audit (artifact này)
- B GAP-466 RLS policies V50-V52
- C GAP-469 RLS performance baseline EXPLAIN ANALYZE
- D GAP-432 bound 3 findAll() Pageable
- E GAP-503 Tier 2 config JVM/Tomcat/HikariCP/healthcheck
- F GAP-506 deploy-prod.sh tech debt
- G GAP-475 smoke test extensions
- H Performance + Security audit refresh

Audit scope = 4 personas × 5 question dimensions = 20 cells, focus security + performance + multi-tenant scope. Mục đích surface gaps mà inside-out wave plan miss.

## 2. Methodology

Skill: `.claude/skills/quality/persona-based-business-review/SKILL.md` (role-play 4 persona types → find gaps).

**Personas:**
1. **P1 Solo Teacher** — giáo viên độc lập (vd. cô Mai), không quản trung tâm, dùng KiteHub tự quản lớp nhỏ <30 học sinh
2. **P2 Center Owner** — chị Hằng, chủ trung tâm Anh ngữ Sky Education (~200 học sinh, 5 staff)
3. **P3 Center Manager** — anh Tâm, quản lý vận hành 1 trung tâm cho P2 (không own data, có quyền edit)
4. **Platform Admin** — Mai, internal KiteHub admin (audit + investigation + tenant support)

**5 question dimensions** (focus Wave 85 scope security + perf + multi-tenant):
1. **Data isolation expectation** — Persona có tin/cần biết data của mình không leak cross-tenant không? Có test scenario explicit?
2. **Performance expectation** — Chấp nhận latency bao nhiêu? Khi nào cảm thấy slow?
3. **Security feeling** — Expect 2FA? Audit trail visible? Suspicious login alert?
4. **Recovery expectation** — Nếu data lost (delete nhầm), kỳ vọng recover trong bao lâu?
5. **Audit trail expectation** — Muốn xem ai đã access/edit data của mình không?

Per `dev-readable-doc-language.md` narrative Vietnamese; technical token English.

---

## 3. Matrix 4×5

### 3.1 P1 Solo Teacher (cô Mai)

| # | Dimension | (a) Mong gì | (b) Wave 85 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 1.1 | Data isolation | Mong data học sinh + điểm số của mình KHÔNG ai khác (kể cả P2/P3 khác trung tâm) thấy. Trust qua "tự nhiên không thấy thì OK"; không test cross-tenant explicit. | **Match** — Bucket B RLS V50-V52 cover `students`, `grades`, `classes` | ✅ Bucket B đủ |
| 1.2 | Performance | Chấp nhận ≤3s page load; slow khi >5s. Mobile 3G context (giáo viên di chuyển). | **Partial** — Bucket D paginate findAll() giúp; nhưng KHÔNG có target latency P95 explicit cho mobile 3G | ⚠️ Bucket D add AC: P95 mobile-3G < 5s; **OR** add NEW sub-target trong Bucket H performance audit |
| 1.3 | Security | Expect login/password đủ; KHÔNG quen 2FA (sẽ bỏ nếu bắt buộc). Suspicious login alert qua email = "nice-to-have", không expect. | **Miss** — Wave 85 không có 2FA optional + login alert email cho P1 | ⚠️ Defer Wave 86+ — **NEW gap proposal:** GAP-XXX P2 "Optional 2FA opt-in + login-from-new-device email alert" |
| 1.4 | Recovery | Nếu xóa nhầm 1 lớp/1 cột điểm, mong revert trong <1h qua admin support. Không expect self-service undo. | **Miss** — Wave 85 không cover soft-delete / restore path / admin recovery tooling | 🚨 **NEW gap proposal:** GAP-XXX P1 "Soft-delete + 7-day restore window for students/classes/grades" (cần trước GA Phase 2) |
| 1.5 | Audit trail | "Tôi tự edit nên không cần xem"; nhưng nếu nghi ngờ data lạ, mong support trace được "ai đã làm gì lúc nào". | **Partial** — Bucket B V52 RLS audit_logs table tồn tại, nhưng KHÔNG có FE view cho P1 self-service audit | ⚠️ Defer — `audit_logs` BE-side đủ Wave 85; FE self-service audit defer Wave 86 |

### 3.2 P2 Center Owner (chị Hằng)

| # | Dimension | (a) Mong gì | (b) Wave 85 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 2.1 | Data isolation | **Critical** — kinh doanh + tài chính + danh sách học sinh là tài sản cạnh tranh. Sẽ test cross-tenant: tạo 2 tài khoản, thử thấy data của nhau. Expect 0 leak tuyệt đối. | **Match** — Bucket B RLS V51 cover `payments`, `invoices`, `subscriptions` | ✅ Bucket B đủ; **AC enhancement:** Bucket B AC bổ sung "automated cross-tenant penetration test script (script tạo 2 tenant, query cross → expect 0 rows) — ship trong Bucket G smoke" |
| 2.2 | Performance | Dashboard analytics chấp nhận ≤5s; revenue report ≤10s. Slow = "không dùng được giờ cao điểm" (đầu tháng học phí). | **Partial** — Bucket D paginate Analytics + Payment findAll() giúp; nhưng KHÔNG có concurrent-user load test cho dashboard giờ cao điểm | ⚠️ Bucket G smoke AC bổ sung: k6 50 concurrent P2 owners × 5min hit `/api/analytics/dashboard` — P95 ≤5s |
| 2.3 | Security | Expect 2FA cho owner role (đụng tiền + danh sách HS = high-value). Expect login alert email khi từ thiết bị mới. Expect password complexity rule. | **Miss** — Wave 85 không có 2FA mandatory cho owner role | 🚨 **NEW gap proposal:** GAP-XXX P0 "Mandatory 2FA for P2 owner role + new-device email alert" — chặn GA Phase 2 (high-value role) |
| 2.4 | Recovery | Nếu staff xóa nhầm hóa đơn / học sinh hàng loạt, mong recover trong <4h. RDS backup snapshot daily là baseline; expect documented restore procedure. | **Partial** — Bucket B RLS có `audit_logs`; nhưng KHÔNG có restore drill / runbook đo TTR | 🚨 GAP-257 (existing P0 carry-forward, Wave 84 audit flagged blocking BETA gate) — confirm trong Bucket H audit; **add bucket reference cross-link Wave 85 plan §6** |
| 2.5 | Audit trail | **Critical** — muốn xem "ai (staff/manager) đã edit điểm/học phí lúc nào". FE dashboard self-service. Expect retention ≥1 năm. | **Miss** — Bucket B V52 enable RLS trên `audit_logs` table tồn tại; KHÔNG có FE view cho P2 dashboard | 🚨 **NEW gap proposal:** GAP-XXX P1 "P2 owner audit-log FE dashboard (filter by staff/date/action)" — defer Wave 86 nhưng track |

### 3.3 P3 Center Manager (anh Tâm)

| # | Dimension | (a) Mong gì | (b) Wave 85 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 3.1 | Data isolation | Chỉ thấy data trung tâm mình quản (sub-tenant scope nếu owner có nhiều CN). Không thấy data trung tâm khác cùng chuỗi. | **Partial** — Bucket B RLS scope = `tenant_id`, chưa có sub-tenant scope (branch-level isolation). Phase 1 BETA P2 mostly 1 trung tâm → acceptable. | ⚠️ Defer — Phase 2 multi-branch enterprise scope; document trong Bucket B AC "Phase 1 BETA: tenant_id = trung tâm-level; multi-branch defer Phase 2" |
| 3.2 | Performance | Tương tự P2 nhưng heavier write (attendance daily + grade weekly). Slow = bottleneck operational. | **Match** — Bucket D paginate + Bucket E HikariCP pool tune cover | ✅ Bucket D+E đủ |
| 3.3 | Security | Expect 2FA optional (không bắt buộc như P2 owner). Expect role-scoped permission: KHÔNG được delete financial records. | **Partial** — Wave 80 RBAC FE/BE shipped (PARTIAL); Wave 85 không enhance permission scope. Cross-link với existing GAP. | ⚠️ Cross-link Wave 80 RBAC PARTIAL → Wave 85 plan §6 (existing gap, không file mới) |
| 3.4 | Recovery | Tương tự P2 nhưng manager không own → escalate qua owner. Mong "tôi báo, owner approve, admin restore" — clear escalation path. | **Miss** — KHÔNG có recovery escalation runbook | ⚠️ **NEW gap proposal:** GAP-XXX P2 "Tenant data recovery escalation runbook (Manager→Owner→Admin)" — defer Wave 86 |
| 3.5 | Audit trail | Personal accountability — manager muốn xem chính mình đã làm gì để defend khi owner thắc mắc. | **Partial** — `audit_logs` BE đủ; FE self-view defer cùng 2.5 | ⚠️ Same as 2.5 NEW gap |

### 3.4 Platform Admin (Mai)

| # | Dimension | (a) Mong gì | (b) Wave 85 address? | (c) Gap → bucket / NEW |
|---|---|---|---|---|
| 4.1 | Data isolation | Admin bypass RLS để support / investigate. Expect explicit `SET LOCAL bypass_rls = on` qua role admin trên Postgres (KHÔNG implicit). Expect audit log mỗi lần admin bypass. | **Miss** — Bucket B plan KHÔNG mention admin-bypass mechanism + bypass audit log | 🚨 Bucket B AC bổ sung: "Define `kitehub_admin` Postgres role với `BYPASSRLS`; admin queries logged to `admin_audit_logs` table" — **add row vào V52 migration** |
| 4.2 | Performance | RLS overhead Postgres acceptable <10% (per Postgres docs). Admin support queries cross-tenant occasional → expect bypass path KHÔNG chậm. | **Match** — Bucket C EXPLAIN ANALYZE baseline cover; **AC enhance:** measure cả bypass-path latency để đảm bảo admin support không bị degraded | ⚠️ Bucket C AC bổ sung: "EXPLAIN ANALYZE cả admin-bypass path baseline" |
| 4.3 | Security | **Critical** — admin role = highest-privilege. Expect MFA mandatory + IP allowlist + session timeout ngắn (≤30min) + admin-action audit log immutable. | **Miss** — Wave 85 không có admin MFA / IP allowlist / session timeout hardening | 🚨 **NEW gap proposal:** GAP-XXX P0 "Platform admin hardening — MFA + IP allowlist + 30min session + immutable admin audit" — chặn GA Phase 2 |
| 4.4 | Recovery | Admin owns disaster recovery process. Expect runbook tested quarterly (per `release-deploy-standard.md` §4.3). Expect TTR <4h cho P0 incident. | **Match** — GAP-257 carry-forward P0 (Wave 84 flagged); Wave 85 Bucket H audit refresh confirm status. | ✅ Cross-reference đủ; **Wave 85 plan §6 cross-link bổ sung** |
| 4.5 | Audit trail | Audit EVERY admin action (read + write cross-tenant). Retention ≥3 năm cho compliance PDPL Art 11. Immutable (append-only, no admin can delete admin's own log). | **Miss** — Wave 85 plan không cover admin audit log immutability + retention policy | 🚨 Cross với 4.1 — Bucket B add `admin_audit_logs` table với immutability constraint (no UPDATE/DELETE policy); **OR** NEW gap "Admin audit log immutability + 3y retention" |

---

## 4. Findings summary — Top 10 missed/partial cells priority-ranked

| Rank | Cell | Persona × Dim | Severity | Reason |
|---|---|---|---|---|
| 1 | 4.3 | Admin × Security | 🚨 **P0** | Admin role highest-privilege; missing MFA + IP allowlist = catastrophic blast radius. Chặn GA Phase 2. |
| 2 | 2.3 | P2 Owner × Security | 🚨 **P0** | Owner đụng tiền + danh sách HS; missing 2FA mandatory = trust gap with paying tenants. |
| 3 | 4.1 + 4.5 | Admin × Isolation + Audit | 🚨 **P0** | Admin bypass mechanism undefined → silent cross-tenant access risk. Immutable admin audit log = compliance PDPL Art 11. |
| 4 | 1.4 | P1 Teacher × Recovery | 🚨 **P1** | Soft-delete + restore window critical — current hard-delete = data loss permanent. |
| 5 | 2.5 | P2 Owner × Audit | 🚨 **P1** | P2 owner self-service audit FE dashboard — trust building. Defer Wave 86 OK nhưng track. |
| 6 | 2.1 (enhance) | P2 Owner × Isolation | ⚠️ **AC enhance** | Bucket B AC thêm automated cross-tenant pentest script (ship trong Bucket G smoke). |
| 7 | 2.2 | P2 Owner × Performance | ⚠️ **AC enhance** | Bucket G smoke k6 concurrent test 50 P2 owners × dashboard endpoint. |
| 8 | 1.2 | P1 Teacher × Performance | ⚠️ **AC enhance** | Bucket D mobile-3G P95 < 5s target explicit. |
| 9 | 1.3 | P1 Teacher × Security | ⚠️ **P2** | Optional 2FA opt-in + login-from-new-device alert. Defer Wave 86. |
| 10 | 3.4 | P3 Manager × Recovery | ⚠️ **P2** | Escalation runbook Manager→Owner→Admin. Defer Wave 86. |

---

## 5. AC additions suggested per bucket B-H

### Bucket B (GAP-466 RLS policies)
- **AC bổ sung B1:** Define Postgres role `kitehub_admin` với `BYPASSRLS` privilege; mọi query qua role này được log vào `admin_audit_logs` table.
- **AC bổ sung B2:** V52 migration thêm `admin_audit_logs` table với constraint immutability (RLS policy chặn UPDATE + DELETE cho mọi role kể cả admin).
- **AC bổ sung B3:** Document trong AC "Phase 1 BETA: `tenant_id` = trung tâm-level isolation; multi-branch sub-tenant scope defer Phase 2".

### Bucket C (GAP-469 RLS performance baseline)
- **AC bổ sung C1:** EXPLAIN ANALYZE bao gồm cả admin-bypass path baseline (đảm bảo admin support không bị degraded).

### Bucket D (GAP-432 findAll bounded)
- **AC bổ sung D1:** Performance target explicit P95 mobile-3G < 5s cho student/class/attendance pagination endpoints.

### Bucket G (GAP-475 smoke test extensions)
- **AC bổ sung G1:** Smoke script `smoke-cross-tenant-isolation.sh` — tạo 2 tenant, login P2 owner mỗi tenant, query cross-tenant data → expect 0 rows. Chạy như Bucket B verification.
- **AC bổ sung G2:** Smoke script `smoke-perf-concurrent-dashboard.sh` — k6 50 concurrent P2 owners × 5min hit `/api/analytics/dashboard` — assert P95 ≤5s.

### Bucket H (Performance + Security audit refresh)
- **AC bổ sung H1:** Security /100 v2 audit phải include 3 NEW evidence blocks: admin MFA, admin IP allowlist, admin session timeout — Wave 85 path không ship sẽ surface as P0 finding cho Wave 86.
- **AC bổ sung H2:** Cross-link existing P0 GAP-257 (restore drill) trong audit narrative.

---

## 6. NEW gap proposals

| Sketch ID | Title | P-level | Bucket-affinity / Wave |
|---|---|---|---|
| NEW-A | Mandatory 2FA cho P2 owner role + new-device email alert | **P0** | Wave 86 (chặn GA Phase 2) |
| NEW-B | Platform admin hardening — MFA mandatory + IP allowlist + 30min session + immutable admin audit | **P0** | Wave 86 (chặn GA Phase 2) |
| NEW-C | Soft-delete + 7-day restore window cho students/classes/grades | **P1** | Wave 86 (trước GA Phase 2) |
| NEW-D | P2 owner audit-log FE self-service dashboard | **P1** | Wave 86 |
| NEW-E | Optional 2FA opt-in cho P1 + login-from-new-device email alert | **P2** | Wave 87+ |
| NEW-F | Tenant data recovery escalation runbook (Manager→Owner→Admin) | **P2** | Wave 87+ |

---

## 7. Verdict — Wave 85 scope completeness

**Completeness score: 72%** — Wave 85 cover well multi-tenant data isolation (Bucket B), performance bounding (Bucket D), Tier 2 config (Bucket E), smoke testing (Bucket G). Gaps lớn ở:

- **Security hardening cho high-privilege roles** (P2 owner + Platform admin) — 3 P0 missing (NEW-A, NEW-B, partial cell 4.1+4.5)
- **Recovery / soft-delete** — P1 missing (NEW-C), cross-link GAP-257 carry-forward
- **Admin bypass + immutable admin audit** — P0 missing (cells 4.1, 4.5)
- **Self-service audit trail FE** — P1 deferable (NEW-D)

**Critical adds needed cho Wave 85 (ship same wave):**
1. Bucket B AC bổ sung B1+B2+B3 (admin bypass + admin audit immutability + Phase 1 scope doc)
2. Bucket C AC C1 (bypass path baseline)
3. Bucket D AC D1 (mobile-3G P95 target)
4. Bucket G AC G1+G2 (cross-tenant pentest + concurrent dashboard load)
5. Bucket H AC H1+H2 (admin hardening surface + GAP-257 cross-link)

**Critical defer Wave 86 (file gap, không block Wave 85):**
- NEW-A P0 (P2 owner 2FA mandatory)
- NEW-B P0 (admin MFA + IP allowlist)
- NEW-C P1 (soft-delete + restore)

**Verdict:** Wave 85 ship với 5 AC enhancements (Bucket B/C/D/G/H) đạt completeness ~88%. 3 P0 NEW gaps Wave 86 chặn GA Phase 2. Wave 85 KHÔNG nên expand thêm bucket NEW (scope creep), nhưng các AC enhancement above PHẢI ship để wave plan thực sự cover defense-in-depth.

---

## 8. References

- `documents/03-planning/waves/wave-2026-05-15-85-multi-tenant-security-perf.md` (Wave 85 plan, status: draft)
- `.claude/rules/outside-in-coverage-trigger.md` §3 Bước 2 (audit method)
- `.claude/skills/quality/persona-based-business-review/SKILL.md` (skill reference)
- GAP-257 (existing P0 carry-forward — restore drill, Wave 84 flagged blocking BETA gate 80)
- Wave 80 RBAC FE/BE PARTIAL (related to cell 3.3)
- `documents/04-quality/audits/persona-review/2026-05-14-pre-wave-79-outside-in.md` (prior outside-in pattern reference)
