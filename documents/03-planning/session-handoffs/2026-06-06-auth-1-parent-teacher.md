---
title: Session handoff 2026-06-06 — KC-8 + KC-9 + Wave auth-1 (parent+teacher auth)
audience: dev
created: 2026-06-06
---

# Session handoff 2026-06-06

## Shipped this session

1. **KC-8 Parent portal G1 PASS** — PR #2185 MERGED. Pre-walk sim + GAP-1006 (fees MultipleBagFetch) + 2 @PreAuthorize. Walk W1-W11 PASS.
2. **KC-9 Student portal** — surface = contract-first stub (GAP-269b); deferred Phase 2 (folded into auth path).
3. **G3 discovery** — parent/student production-access was Phase-2-gated (no login). User chose pull-forward Option B.
4. **Wave auth-1 (PR #2186, branch `wave/auth-1-kc-native-login-plan`, 11 commits)** — KC-native login (Option B):
   - Bucket A: `POST /api/v1/tenant-auth/login` (HS512 JWT, role+tenantId+referenceId claims).
   - Bucket C: gateway X-User-Reference-Id inject + anti-spoof + public route.
   - Bucket B: parent provisioning via redeem (`AuthCredentialProvisioningService`).
   - Teacher provisioning: `POST /api/v1/teachers/{id}/credentials` (admin).
   - **G3 full chain verified** (real parent, gateway :9000): login→facet 200 + IDOR 403 + anti-spoof. Teacher login role=TEACHER.
   - ParentInvitationServiceTest 13/13 + TeacherServiceTest 9/9 PASS.

## Remaining (next session)

- **Student provisioning + KC-9 build** (Bucket E) — `StudentPortalServiceImpl` empty + FE mock (GAP-269b). Student account model decision (own login vs inherited).
- **OTP Hướng C** (Zalo/SMS) — real Phase 2, vendor-dependent.
- **Production parity** — kiteclass-core prod JWT_SECRET + PARENT_PORTAL_ENABLED via fetch-secrets.sh + IaC (per `local-fix-production-parity-check.md`).
- **Flip gaps** at auth-1 closure: GAP-725 (parent+teacher DONE, student remains) + GAP-798b (parent producer DONE) → currently PARTIAL.
- **Merge PR #2186** (CI: self-hosted runner slow — Test Core Service gate is the key).
- **G2 human walks** still pending for KC-1..8 (8 flows `walk-pass-pending-human`).

## State

- Stack UP (kiteclass-core + gateway rebuilt with auth-1). Test creds: parent-walk@test.com/Walk@5678, teacher_a@test.com/Teach@1234. kitehub `instances` seeded aaaabbbb-…0001.
- Wave plan: `wave-2026-06-06-auth-1-kc-native-login.md` (full Log).
- Memory `project_parent_student_portal_phase2_gated.md` updated (parent+teacher pulled forward).
