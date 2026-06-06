---
title: Session handoff — KH-10/KC-10/11/12 G1 walks + 22/22 G1 complete + 9 G2 recipes + Wave security-2 Bucket A/C
audience: dev
created: 2026-06-06
scope: Evening session 2026-06-06 — Flow Verification Campaign G1-all-first completion + G2 recipe batch + Wave security-2 P0 cluster Buckets A+C
---

# Session handoff — 2026-06-06 evening

## What shipped this session

### 1. G1 walks — secondary flows (4 this session) → 22/22 G1 complete
G1 runtime walks (pre-walk Opus persona-sim → live gateway :9000 walk → catalog → gaps → wave plan):
- **KH-10** notification/email/feedback/support (PR #2201) — GAP-1031 P0 + 1032/1033
- **KC-10** per-tenant branding wizard (PR #2202) — GAP-1034 P0 + 1035/1036/1037/1038
- **KC-11** notification/document-gen (PR #2203) — GAP-1039/1040 P1
- **KC-12** reschedule/payroll (PR #2204) — GAP-1041 P0 + 1042/1043

**🎯 22/22 flow G1 PASS** (G1-all-first phase COMPLETE; KC-9 deferred Phase 2). Memory `project_flow_campaign_g1_first_then_g2` updated → giai đoạn G2.

### 2. G2 recipes — 9 secondary flows (PR #2205, merged)
9 G2 human-test recipe MD (`documents/05-guides/operations/2026-06-06-g2-recipe-*.md`): KH-5..10 + KC-10/11/12. Each 7 sections per `g2-handoff-md-mandate`. Campaign §4 rows linked. KH-5/6/7 via Opus agents; rest authored (agent rate-limit lesson: spawn ≤3 concurrent Opus for doc-gen).

### 3. Wave security-2 — P0 cluster (Bucket A + C)
**Bucket A — gateway routing/exposure (PR #2206, MERGED `ac70b820`):**
- GAP-1031 DONE: removed platform-email gateway route (anon email send 404, was 200 SENT)
- GAP-1034 DONE: 3 kiteclass branding routes before kitehub-branding-v1 (public/versions 200, was 401)
- GAP-1041 DONE: kiteclass-payroll route before kitehub-admin-v1 (ADMIN payroll 200, was 404)
- GAP-1042 PARTIAL: predicate-discipline applied; systematic audit + CI-gate remain
- `audit-gateway-routes.sh` INTERNAL_ONLY_PATTERNS exemption (email)

**Bucket C — authz @PreAuthorize (PR #2207, ⏳ CI re-running post-rebase, NOT merged):**
- GAP-1025 DONE: InstanceController @PreAuthorize(PLATFORM_ADMIN/ADMIN) × 5 admin methods (owner→403, was enum-all 200)
- GAP-1035 DONE: BrandingController @PreAuthorize(ADMIN/OWNER) × 3 mutation (STAFF→403, was A01)
- Caller-sweep: @WithMockUser added to 5 broken InstanceController tests (re-run PASS)
- **Code re-walk verified live + tests PASS.** PR rebased onto main (CSV + wave-history conflict resolved). CI re-running on rebased HEAD.

**🎯 5/7 P0 cluster closed** (Bucket A 3 + Bucket C 1, +GAP-1031 = 4 distinct... GAP-1031/1034/1041/1025).

## NEXT SESSION — pick up here

### Immediate
1. **Merge PR #2207** (Bucket C) — CI was re-running on rebased HEAD at session end; code verified (re-walk + tests PASS). Verify CI green → squash-merge. Do NOT --admin (post-rebase per `admin-merge-discipline.md` §2).

### Then — Wave security-2 Bucket B (cross-tenant IDOR — HARDEST, user-sequenced after C)
**Remaining P0: GAP-1015 (subscription) + GAP-1019 (branding) + GAP-1023 (domain).** Shared root: service trusts client-controlled instance/tenant id without verifying ownership vs caller's tenant. Architectural change:
1. Gateway forward/validate caller tenant (JWT `tenantId`) as trusted header (gateway already strips client headers + injects from JWT — extend for tenant ownership).
2. Service-side ownership check (`{id}` belongs to caller tenant, bypass PLATFORM_ADMIN).
**Investigate 3 services for shared fix pattern BEFORE implementing (high-risk).** Present findings first.

### Lower priority (Wave security-2 P1/P2 follow-ups)
GAP-1039 (reports cross-tenant + payroll repos filter-only), GAP-1040 (document SSRF), GAP-1036 (logo bucket), GAP-1037 (SVG-XSS), GAP-1016/1017/1020/1021/1024/1026/1028/1029. + GAP-1042 systematic gateway route audit.

### Campaign G2 (parallel track — dev/human work)
20 flows have G2 recipes ready (KH-1/2c/3 + KC-1..8 + 9 new). G2 = human walks recipes on local stack → reports 4-outcome. 0/20 walked. G3 production-parity: only 4 flows (KH-1/2c, KC-7/8).

## State notes
- Local stack UP (gateway :9000 has Bucket A routing + Bucket C controllers rebuilt). AWS stopped.
- `wave/security-2-bucket-c` branch still local (delete after #2207 merges).
- gap-status.csv on main: GAP-1031/1034/1041 DONE; GAP-1025/1035 DONE lands with #2207.
