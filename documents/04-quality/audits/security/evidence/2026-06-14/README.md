# Evidence Index — Security Full Audit 2026-06-14

Audit: `AUDIT-2026-06-14-security-full` (post wave-p0-closeout-1, commit `cd44e035f`).
Report: `../../2026-06-14-security-full-audit.md`.

Per `audit-report-template-v2.md` — each artifact holds full Command run + raw Output; report cites artifact ID only.

| Artifact ID | Category | Subject |
|---|---|---|
| EVIDENCE-2026-06-14-AUTH-001 | Cat 4 / A07+A01 | Gateway default-filters strip list (X-User-Roles / X-User-Email NOT stripped) — F-001 P0 |
| EVIDENCE-2026-06-14-OWASP-A01-002 | Cat 3 / A01 | StorageController confirm/delete missing ownership authz — F-002 P1 |
| EVIDENCE-2026-06-14-INFRA-006 | Cat 5 / A01 | uploaded_files not in DB RLS sweep (Hibernate @Filter only) — F-004 P2 |
| EVIDENCE-2026-06-14-SEC-001 | Cat 2 | Hardcoded-secret grep (broad scope) — 0 real leaks |
| EVIDENCE-2026-06-14-OWASP-A03-001 | Cat 3 / A03 | SQL injection scan — parameterized JPQL only |
| EVIDENCE-2026-06-14-AUTH-005 | Cat 4 | Gateway JWT validation (HS512 ≥64 bytes, strip+re-inject) |
| EVIDENCE-2026-06-14-DEPS-001 | Cat 1 | FE pnpm audit (both apps) — 0 high/critical |
