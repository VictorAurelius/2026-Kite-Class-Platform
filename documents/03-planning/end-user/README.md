# 03-planning/end-user — End-user (Beta Tenant) Plans

**Rules:** [`.claude/rules/planning-docs-structure.md`](../../../.claude/rules/planning-docs-structure.md) + [`.claude/rules/docs-folder-structure.md`](../../../.claude/rules/docs-folder-structure.md)

**Last Updated:** 2026-05-13

Plans liên quan đến **end-user (beta tenant)** lifecycle Phase 1 BETA: từ self-test verification, chọn cohort, gửi invite, onboard, monitor đến feedback loop. Khác với `roadmap/` (cross-wave strategic) và `waves/` (per-wave execution) — folder này gom các kế hoạch hướng tới người dùng cuối thay vì hạ tầng/governance.

---

## Directory Map

| Path | Purpose | Typical files |
|------|---------|---------------|
| `README.md` | This index | 1 |
| `plan-N-{slug}.md` | Per-plan execution doc với frontmatter `status:` | 1 plan = 1 deliverable hướng end-user |

---

## File Placement Rules

- ✅ **Belongs here:**
  - Self-test E2E flow verification cho admin trước khi mời người dùng thật
  - Cohort selection + outreach scripts (LinkedIn, network, communities)
  - Invite email content + delivery path decision (SES sandbox vs alternatives)
  - Onboarding checklist + first-tenant support procedures
  - Beta period monitoring + feedback collection loop
  - Post-beta retention + transition tới Phase 1.5 PAID

- ❌ **Does NOT belong here:**
  - Per-wave execution doc → [`waves/`](../waves/)
  - Cross-wave strategic roadmap → [`roadmap/`](../roadmap/)
  - Infrastructure deploy plans → [`infrastructure/`](../infrastructure/)
  - Generic feature plans non-end-user → [`plans/`](../plans/)
  - Business logic + use cases → [`documents/01-business/`](../../01-business/)

- Naming: `plan-{N}-{kebab-case-slug}.md` — N tăng dần theo thứ tự ship; slug mô tả ngắn (vd `plan-1-self-test-e2e.md`)

---

## Frontmatter Required (per `planning-docs-structure.md` §6)

```yaml
---
title: Plan N — <Title>
status: draft | active | complete | superseded
created: YYYY-MM-DD
updated: YYYY-MM-DD
wave: NN  # wave number executing plan, or empty if standalone
gaps: [GAP-XXX, GAP-YYY]
prs: [NNNN]
---
```

---

## Archive Policy

Move plan to [`documents/07-archived/end-user-YYYY/`](../../07-archived/) khi:
- Plan status = `complete` AND không có wave/PR reference trong 90 ngày
- Plan superseded — add `superseded_by: ../path` trong plan mới rồi archive plan cũ
- End-user phase chuyển sang Phase 1.5 PAID (toàn bộ Phase 1 BETA plans archive vào `end-user-2026-phase-1-beta/`)

---

## Key Plans

- [Plan 1 — Self-Test E2E Flow (Wave 69 execution scope)](plan-1-self-test-e2e.md) — Verify đầy đủ end-user flow trước khi mời người dùng thật

---

## Relationship to Other Folders

| Folder | Relationship |
|--------|--------------|
| [`../waves/`](../waves/) | Wave plan execute plan-N. Vd Wave 69 executes plan-1. |
| [`../roadmap/release-1-deploy-plan.md`](../roadmap/release-1-deploy-plan.md) | §2 Phase 1 BETA references end-user activities; folder này là chi tiết thực thi. |
| [`../../04-quality/gaps/`](../../04-quality/gaps/) | Gaps liên quan end-user (GAP-370 SES, GAP-372 invite mechanism, GAP-480 invite flow undefined). |
| [`../../01-business/kiteclass/onboarding/`](../../01-business/kiteclass/) | Use cases + acceptance criteria cho tenant onboarding (business layer). |
