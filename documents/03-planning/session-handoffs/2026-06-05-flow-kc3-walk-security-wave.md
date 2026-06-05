---
title: Session handoff 2026-06-05 — KC-3 G1 walk + P0 cross-tenant leak + security-1 wave plan
audience: dev
created: 2026-06-05
scope: Bàn giao phiên KC-3 walk (G1 functional PASS + P0 GAP-983 discovery) + dedicated security wave plan
---

# Session handoff 2026-06-05 — KC-3 walk + GAP-983 P0 + security-1 plan

## Đã ship (PR #2175 merged → main a4d14a42)

| Artifact | Nội dung |
|---|---|
| KC-3 G1 walk findings | course → class → schedule → sessions **functional PASS** (27 sessions auto-gen MON+WED) |
| Pre-walk persona sim | `audits/persona-review/2026-06-05-pre-walk-kc3-course-class-schedule.md` (10 failure modes) + audits-index row |
| **GAP-982** P1 | academic-year module orphan (service full logic, no controller/caller) |
| **GAP-983** P0 | LIVE cross-tenant by-id read leak + root-cause + 4-layer fix plan |
| **GAP-984** P2 | per-tenant DB provisioned nhưng core dùng shared DB (isolation model mismatch) |
| **Wave security-1 plan** | `waves/wave-2026-06-05-security-1-tenant-isolation-byid.md` (GAP-983 fix) |

PR merged qua `--admin` (docs-only, 20/20 GitHub-hosted checks green; 8 self-hosted queued vì 2 runner offline). `ADMIN_MERGE_OVERRIDE` trailer trong PR body.

## P0 GAP-983 — root cause đã investigated (quan trọng cho next session)

Tenant A đọc được data tenant B qua **GET-by-id** (course/class/session/teacher) — confirmed LIVE. LIST path an toàn (Specification predicate); by-id leak.

**Root cause:** `spring.jpa.open-in-view: false` (OSIV off) + method `@Transactional` (vd `ClassServiceImpl.getClass`) mở Hibernate session riêng mà `TenantFilterInterceptor.enableFilter` (set trên OSIV/default session) KHÔNG reach. Method KHÔNG `@Transactional` (course/teacher/student getById) được filter áp dụng.

**Fix attempt v1 reverted:** thêm `@Filter` 4 entity → PARTIAL (teacher blocked-but-500; getClass vẫn leak). `@Filter` cần nhưng KHÔNG đủ. Stack đã rebuild về committed state (no @Filter).

**Blast radius:** 58 entity extends BaseEntity thiếu `@Filter` (chỉ 3 marketing entity có). Platform-wide.

## Pending (cho next session)

1. **Wave security-1 EXECUTE** — gated **self-hosted runner ONLINE** (cần full kiteclass-core IT suite validate). Bật runner: `! sudo systemctl restart actions.runner.VictorAurelius-2026-Kite-Class-Platform.kite-dev-wsl-runner.service` trên máy `nguyenvankiet`. Plan §1 có 4-layer fix (filter-enablement trên txn session = core + 58-entity sweep + exception→404 + RLS defense). Ties GAP-746/749/362.
2. **KC-3 campaign §4** vẫn 🔄 G1 functional — NOT THÔNG (blocked GAP-983). G2 recipe + flip pending-human chờ GAP-983 fix + re-walk.
3. **GAP-982** academic-year controller — gộp với GAP-960 thành wave riêng.

## Môi trường

- Stack UP (kitehub + kiteclass production-equivalent, kiteclass-core rebuilt clean 2026-06-05).
- Self-hosted CI runner **OFFLINE** (cả 2 runner) → self-hosted Quality jobs queue. GitHub-hosted checks vẫn chạy.
- Walk data test (course id=10, class id=14, teacher id=10, 27 sessions, tenant sky-education `0edaee10`) còn trong `kiteclass_shared` DB — dev seed, không cần clean.

## Lưu ý kiến trúc (phát hiện session này)

- kiteclass-core dùng **single shared DB** `kiteclass_shared` + cột `instance_id` isolate (NOT per-tenant DB dù `instances.database_url` khai báo `kiteclass_0edaee10`). Per-tenant DB provisioned nhưng 0 rows (GAP-984).
- `TenantIsolationIT.shouldIsolateCourseDataBetweenTenants` chỉ test LIST endpoint, KHÔNG test by-id → đó là coverage gap để lọt GAP-983 (security-1 wave Bucket D mở rộng test này).
