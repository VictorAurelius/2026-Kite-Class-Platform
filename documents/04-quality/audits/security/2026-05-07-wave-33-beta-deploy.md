# Security Audit — Wave 33 BETA Deploy Cluster

**Date:** 2026-05-07
**Auditor:** Background agent a24fe574 (Sonnet, Explore subagent)
**Scope:** GAP-376/370/372/369/379 — BetaAccess token flow + secrets/DNS infra

---

## Score: 72/100 — C+ (delta -13 vs 2026-04-25 baseline 85/100)

| Category | Score | Notes |
|----------|:-----:|-------|
| Dependency vulnerabilities | 18/20 | No critical CVEs; minor: no proactive scan in CI |
| Secrets & credentials | 16/20 | K8s Secrets template OK; rotation policy chưa documented |
| OWASP Top 10 | 14/20 | Auth guard missing (-5), PDPL consent gap (-3), honeypot logging (-2), token URL plaintext (-1) |
| Auth & Access Control | 10/20 | UUID entropy + 24h TTL OK; **admin endpoints unguarded (-8)**; per-email rate-limit missing (-2) |
| Infrastructure security | 14/20 | CORS OK; CSP headers chưa documented (-3); K8s securityContext không visible (-3) |

---

## OWASP Top 10 Traffic Light

| # | Category | Status |
|---|----------|:------:|
| A01 Broken Access Control | 🔴 P0 | Admin beta endpoints thiếu `@PreAuthorize` |
| A02 Cryptographic Failures | 🟢 LOW | Env-var secrets, K8s REPLACE markers OK, TLS configured |
| A03 Injection | 🟡 YELLOW | V28 migration safe, validation present, regex strict |
| A04 Insecure Design | 🟠 MEDIUM | Honeypot validation rejection silent (no metric) |
| A05 Broken Authentication | 🟡 YELLOW | Token UUID + 24h TTL OK; **plaintext token in email href** |
| A06 Sensitive Data Exposure | 🟠 MEDIUM | **PDPL consent gap — beta-signup collect PII without consent flow** |
| A07 CORS/CSRF | 🟡 YELLOW | CORS OK; CSRF token mechanism not explicit (stateless JWT may be acceptable) |
| A08 SSRF | 🟢 LOW | Email Thymeleaf safe |
| A09 CVEs | 🟢 LOW | No critical Maven/npm |
| A10 Logging | 🟡 YELLOW | Approve/reject logged; no rate-limit breach logging |

---

## Top 5 Vulnerabilities

| # | Sev | Issue | File | Fix Effort |
|---|:---:|-------|------|:---:|
| 1 | 🔴 P0 | **Admin beta endpoints unauthenticated** — `/api/v1/admin/beta-requests/{id}/approve\|reject` thiếu `@PreAuthorize`; gateway routing scope mismatch (`/api/v1/admin/**` vs gateway's `/api/platform/admin/**`). Unauth user có thể approve/reject | `BetaAccessController.java:120-148` | 1h |
| 2 | 🔴 P0 | **PDPL 2023 consent gap** — beta-signup form collect email+name+orgName (PII) KHÔNG có consent checkbox/privacy link/ToS. Vi phạm PDPL §3.1 explicit consent | `BetaRequestForm` + `BetaRequestDto` | 3h |
| 3 | 🟠 P1 | Honeypot validation rejection silent — `@Size(max=0)` enforced by Jakarta nhưng controller không log → metric blind | `BetaRequestDto.honeypot` + `BetaAccessController.submitRequest()` | 2h |
| 4 | 🟠 P1 | Token leakage in plaintext email — `<a th:href="${inviteUrl}">` chứa raw UUID token; email TLS assumed nhưng PDPL không guarantee | Beta invite email template (Thymeleaf) | 2h (2FA) / 5h (S/MIME) |
| 5 | 🟠 P1 | Per-email rate limit missing — public `/api/v1/auth/request-beta-access` chỉ rate-limit per IP (gateway). Single attacker IP có thể spam 5 burst + 3/sec → DDoS DB với duplicate PENDINGs | `BetaAccessService.submitRequest()` | 1h |

---

## Gap Recommendations

| Gap | Sev | Effort |
|-----|:---:|:------:|
| Admin auth guard `@PreAuthorize` | 🔴 P0 | 1h |
| PDPL consent flow on beta-signup | 🔴 P0 | 3h |
| Honeypot metric counter | 🟠 P1 | 2h |
| Token email encryption (2FA preferred) | 🟠 P1 | 2h |
| Per-email rate limit | 🟠 P1 | 1h |
| SES TLS DKIM/SPF mandatory in runbook | 🟡 P2 | 30min |
| AWS SM secrets rotation policy doc | 🟡 P2 | 2h |

---

## Delta vs 2026-04-25 Baseline (85/100 B)

| Category | Δ | Reason |
|----------|---|--------|
| OWASP A01 | -5 | New admin surface ships unguarded |
| OWASP A06 | -3 | New PII collection without consent |
| OWASP A04 | -2 | New honeypot validation gap |
| OWASP A05 | -1 | New plaintext token email |
| Subtotal | **-11 → 74** | (rounded to 72 with secondary deductions) |

**Root cause:** Wave 33 introduced new public + admin attack surface BEFORE wiring auth + consent + rate limiting properly. Patterns recurrent với GAP-308 P3 RBAC findings — endpoints shipped before guards.

---

## 1-line summary

Wave 33 BETA introduces token-based invite (good entropy/TTL) but ships **unguarded admin endpoints (P0), missing PDPL consent flow (P0), honeypot logging gap (P1), plaintext token email (P1), per-email rate-limit missing (P1)** — must block production deployment until P0 resolved.
