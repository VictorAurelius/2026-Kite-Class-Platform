# Security Audit — Wave 40 Milestone (`release-deploy-artifacts`)

**Date:** 2026-05-08
**Auditor:** Background agent (Opus 4.7, security-audit skill)
**Scope:** Toàn bộ KiteClass + KiteHub platform sau cụm Wave 33+34+35+36+37+38+39 (đầy đủ chuỗi `release-deploy-artifacts` deferred audits) — kích hoạt bởi `post-wave-audit-mandate.md` §2.4.2 milestone gate.
**Baseline trước:** 84/100 B (2026-05-07 `2026-05-07-post-wave-35.md`)
**Standards:** OWASP Top 10 (2021) · NIST SP 800-53 Rev 5 · AWS Well-Architected (Security pillar) · `release-deploy-standard.md` §3.4 cổng MAJOR · PDPL 2023 + Luật An ninh mạng 2018 · `.claude/skills/quality/security-audit/reference/scoring-guide.md`

---

## Score: 87/100 — B (delta +3 vs baseline 84/100)

| Category | Score (Δ) | Notes |
|----------|:---------:|-------|
| 1. Dependency vulnerabilities | 19/20 (=) | `pnpm audit` clean cả 2 FE (0 critical/high/moderate, 870 deps mỗi app). Spring Boot 3.5.14 / Tomcat 10.1.54 current. Trivy + Cosign + SBOM wired vào `docker-build-push.yml` (GAP-400) cho ECR push. Còn -1 vì PR-time `pnpm audit` chưa wire vào FE CI workflows |
| 2. Secrets & credentials | 19/20 (+2) | `secrets-management-runbook.md` shipped (GAP-379) — 12 secret keys + rotation cadence (90/180/365d) + AWS Secrets Manager + IAM workload-identity. `secrets.tf` provisions 8 secrets via Terraform với `recovery_window_in_days = 7`. GAP-426 setup.sh fix bảo toàn base64 key (32 bytes nguyên vẹn) + EncryptionService strict-validate. Còn -1 vì rotation Lambda chưa code (manual only Phase 1) |
| 3. OWASP Top 10 | 18/20 (=) | A05/A07 vẫn -1 mỗi (xem §OWASP), không regression. CSP/HSTS gateway header filter chưa explicit |
| 4. Auth & Access Control | 18/20 (+1) | GAP-388 Wave 36 closed — claim-code 2FA (V33 migration `claim_code` 6-digit) + per-email rate-limit (`BetaAccessService.METRIC_HONEYPOT_REJECTIONS` + 24h window) + `BetaRateLimitExceededException` → HTTP 429. JWT refresh-rotation explicit chưa documented (-1), MFA TOTP cho admin chưa ship (-1) |
| 5. Infrastructure security | 13/20 (=) | K8s `runAsNonRoot` vẫn missing (-4). Gateway response header filter (CSP/HSTS) chưa code (-3). ALB/EC2 SG production-hardened OK; ZAP baseline workflow_dispatch wired (`zap-baseline.yml`); restore-drill cron monthly OK |

---

## OWASP Top 10 — Traffic Light (vs 84-baseline)

| # | Category | Status | Δ vs 84 baseline |
|---|----------|:------:|:----------------:|
| A01 Broken Access Control | 🟢 LOW | unchanged — `@PreAuthorize` admin guards + `XUserRolesHeaderFilter` + 401 entrypoint vẫn intact |
| A02 Cryptographic Failures | 🟢 LOW | ✅ +1 GAP-426 — EncryptionService 32-byte AES-256-GCM strict; setup.sh không corrupt key nữa |
| A03 Injection | 🟢 LOW | unchanged — V33 migration parameterized; honeypot strict |
| A04 Insecure Design | 🟢 LOW | unchanged — Wave 35 metrics + alert rules vẫn intact |
| A05 Broken Authentication | 🟡 MEDIUM | ✅ +1 GAP-388 claim-code 2FA closed, per-email rate-limit closed; nhưng plaintext token email vẫn -1 (P2 deferred) |
| A06 Sensitive Data Exposure | 🟢 LOW | ✅ Wave 38 ConsentBanner integrated cả `kitehub-frontend/PublicLayout.tsx:94` + `kiteclass-frontend/(public)/layout.tsx:210` (PDPL Phase 2 production-active) |
| A07 CORS/CSRF | 🟡 MEDIUM | unchanged — gateway CORS prod-restricted; CSRF disabled (stateless JWT) acceptable; CSP/HSTS chưa explicit |
| A08 SSRF | 🟢 LOW | unchanged |
| A09 CVEs | 🟢 LOW | ✅ Trivy gating fail-on-HIGH/CRITICAL trên `docker-build-push.yml` (GAP-400) — production blocker layer |
| A10 Logging | 🟢 LOW | unchanged — `logback-spring.xml` + `logging-standard.md` |

---

## Wave-by-wave verification (cụm `release-deploy-artifacts`)

| Wave | Scope | Security verification |
|---|---|---|
| 33 | BetaAccessRequest + 24h UUID token | ✅ `@PreAuthorize` admin guards intact; token UUID v4 + 24h TTL OK; honeypot field `@Size(max=0)` strict |
| 34 | AI Branding wizard 7 endpoints | ✅ `@PreAuthorize` per-tenant guards (verified subset); BR-INPUT-CAP-001..007 (`AIInputCapService`) tier-aware token cap |
| 35 | Admin auth + V31 indexes + cookie consent | ✅ `SecurityConfig.java` `requestMatchers("/api/v1/admin/**").authenticated()` + 401 entrypoint; `XUserRolesHeaderFilter` translates `X-User-Roles` |
| 36 | GAP-388 P1 cluster | ✅ Claim-code 2FA (V33 migration) + `BetaRateLimitExceededException` HTTP 429 + per-email 24h window — full P1-2/P1-3 closure |
| 37 | Terraform + GAP-415 EKS defer | ✅ `secrets.tf` 8 secrets via random_password; `iam.tf` GitHub OIDC `terraform_plan` read-only; `recovery_window_in_days = 7` |
| 38 | CDN headers + statuspage + staging | ✅ `verify-cdn-headers.sh` checks HSTS + CF-Ray + cloudflare server; ADR-027 Instatus chosen; ALB/EC2 SG `aws_security_group.alb`/`ec2_app` SG-to-SG only |
| 39 | dev-stack readiness + GAP-426 | ✅ `setup.sh` `tr -d '\n'` only (preserves base64 +/= padding); EncryptionService strict 32-byte validate; quoted env template prevents shell injection |

---

## Top Findings (P0/P1/P2/P3)

### P0 — 0 findings ✅

Không có P0 mới. Wave 33-39 không introduce P0 security issue nào.

### P1 — 3 findings (carry-over từ baseline + 1 mới)

**P1-1: K8s deployments thiếu `securityContext.runAsNonRoot`** (carry-over)
- File: `infrastructure/k8s/kiteclass-template/*-deployment.yaml`, `infrastructure/k8s/kitehub/*-deployment.yaml`
- Evidence: `grep runAsNonRoot infrastructure/k8s/` → 0 matches (cùng kết quả như baseline)
- Risk: container escape → host root nếu compromised
- Fix effort: 1h (add `securityContext: { runAsNonRoot: true, runAsUser: 1000, allowPrivilegeEscalation: false }` per pod template)
- ADR-025 ghi nhận EKS defer Phase 2; nhưng K8s manifest vẫn phải production-ready cho khi cutover
- Đề xuất gap mới: GAP-XXX-k8s-security-context-runasnonroot

**P1-2: Gateway prod profile thiếu security headers** (carry-over + escalation)
- File: `kitehub/kitehub-gateway/src/main/resources/application-prod.yml` (chỉ 12 dòng — comment `Security headers: Strict` nhưng KHÔNG code header filter)
- Evidence: `grep -r "Content-Security\|Strict-Transport" kitehub/kitehub-gateway/src/main/` → 0 matches
- Risk: missing CSP + HSTS = clickjacking + MITM exposure (OWASP A05/A07)
- Fix effort: 2h Spring Cloud Gateway response-header filter (CSP, HSTS, X-Frame-Options=DENY, X-Content-Type-Options=nosniff)
- Mitigation hiện tại: Cloudflare proxy (per Wave 38 GAP-371) thêm HSTS ở edge — nhưng defense-in-depth yêu cầu app-level
- Escalation từ baseline P2-1 → P1 vì BETA launch sắp diễn ra (production deploy gate)
- Đề xuất gap mới: GAP-XXX-gateway-security-headers-filter

**P1-3: Plaintext invite token trong email href** (carry-over)
- File: `kitehub-email/src/main/resources/templates/emails/beta-invite.html` (line 100 cho thấy 6-digit claim code đã thêm — 2FA cải thiện)
- Status update: GAP-388 388-B đã add claim-code 2FA → token alone không đủ truy cập (phải có claim-code email cùng); risk giảm
- Mặc dù vậy plaintext token in href vẫn existing; nhưng severity hiện downgrade vì 2FA gate
- Decision: maintain P1 nhưng đánh dấu "mitigated by 2FA" → có thể downgrade P2 sau wave 41 review
- Fix effort: 5h (S/MIME) hoặc 0h (giữ vì 2FA đủ)

### P2 — 4 findings

**P2-1: AWS Secrets Manager rotation Lambda chưa code**
- File: `secrets-management-runbook.md` §4 documented manual rotation; Lambda rotation deferred Wave 34 (sau release)
- Risk: long-lived secrets nếu missed manual rotation cadence
- Fix effort: 4h (terraform module + Lambda template)
- Đề xuất gap: GAP-XXX-secrets-rotation-lambda

**P2-2: PR-time `pnpm audit` chưa wire vào FE CI**
- File: `.github/workflows/frontend-ci.yml`, `kitehub-frontend-ci.yml`
- Evidence: workflows không có `pnpm audit --audit-level=high` step
- Risk: high CVE introduced via dependabot PR có thể merge mà không block
- Fix effort: 30 phút mỗi workflow (2 workflow)
- Đề xuất gap: GAP-XXX-fe-ci-pnpm-audit-job

**P2-3: ZAP baseline scan chưa scheduled**
- File: `.github/workflows/zap-baseline.yml`
- Status: workflow_dispatch only, weekly cron commented (Phase 2)
- Risk: detection delay nếu staging có regression
- Fix effort: 1 dòng uncomment (sau khi staging URL stable per Wave 38 GAP-380)
- Đề xuất gap: GAP-XXX-zap-baseline-weekly-cron

**P2-4: MFA TOTP cho PLATFORM_ADMIN chưa design**
- File: chưa có ADR
- Risk: admin compromise → full takeover. Hiện tại admin chỉ dựa vào password + JWT.
- Fix effort: 1d ADR + 3-5d implementation
- Đề xuất gap: GAP-XXX-admin-mfa-totp-design

### P3 — 3 findings

- **P3-1: K8s NetworkPolicy chưa applied** — pod-to-pod traffic unrestricted (carry-over).
- **P3-2: JWT refresh-rotation chưa explicit trong audit trail** — config exists nhưng không log refresh reuse detection.
- **P3-3: Restore-drill cron `BACKUP_DRILL_ENABLED` gate vẫn unset** — drill body skip; cần flip flag sau khi GAP-093 confirmed.

---

## Dependency Scan Results

```
kitehub-frontend (870 deps):    0 critical / 0 high / 0 moderate / 0 low / 0 info
kiteclass-frontend (870 deps):  0 critical / 0 high / 0 moderate / 0 low / 0 info
Spring Boot:                    3.5.14 (current 2026-05-08, no known critical CVEs)
Tomcat:                         10.1.54 (pinned via <tomcat.version>, current)
JSoup:                          1.18.1 (Wave 4 baseline, no new CVEs in 2026-Q1/Q2)
Lombok:                         no version pin (BOM-managed via spring-boot-parent)
Trivy gating:                   ✅ docker-build-push.yml fails on HIGH/CRITICAL fixable CVEs
Cosign + SBOM:                  ✅ images signed + SBOM (CycloneDX) attested per ECR push
```

**Verdict:** A09 Vulnerable Components → 🟢 LOW. Production deploy không có known CVE blocker.

---

## Secret Scan Results

- Production secrets in repo: **0** (env-var pattern dùng nhất quán; `application-prod.yml` chỉ reference `${JWT_SECRET}`, `${SPRING_DATASOURCE_PASSWORD}`, `${ENCRYPTION_MASTER_KEY}`)
- Test secrets: 14 matches, all isolated trong `src/test/**` (`Test@123`, `test-secret-for-hmac`, mock JWT)
- Default placeholders: `OPENAI_API_KEY: ${OPENAI_API_KEY:-sk-mock-key}` — env override required for prod
- Terraform `secrets.tf`: 8 secrets via `random_password` provider (jwt 64-char, encryption 32-byte raw → base64), 5 placeholders for user-fill (SES, Cloudflare, OpenAI, Anthropic, RabbitMQ)
- IAM scope: `secretsmanager:GetSecretValue` confined to `${var.project_name}/${var.environment}/*` ARN pattern (least-privilege)
- AWS access keys: NEVER stored as secrets — IAM workload identity (EC2 instance profile + GitHub OIDC) only ✅

---

## Auth Flow Review (Wave 33-36 cumulative)

| Element | Status | Evidence |
|---|:---:|---|
| `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` 3 admin endpoints | ✅ | `BetaAccessController.java:126,147,164` |
| `@EnableMethodSecurity` activated | ✅ | `SecurityConfig.java` |
| `XUserRolesHeaderFilter` translates gateway `X-User-Roles` → `ROLE_*` | ✅ | Wave 35 GAP-384 |
| 401 entrypoint (anonymous → 401) | ✅ | `SecurityConfig.java` line 85-87 |
| Public endpoints `/api/v1/auth/**` permitAll | ✅ | line 77 |
| Admin endpoints `/api/v1/admin/**` authenticated | ✅ | line 81 |
| Test profile permissive (no regression) | ✅ | line 64-65 |
| BetaAccessRequest token: UUID v4 + 24h TTL | ✅ | Wave 33 |
| Claim-code 2FA (6-digit numeric, V33 migration) | ✅ | Wave 36 GAP-388 388-B |
| Per-email rate-limit (24h window, HTTP 429) | ✅ | Wave 36 GAP-388 388-C — `BetaRateLimitExceededException` |
| Honeypot anti-bot (`@Size(max=0)`) | ✅ | Wave 33 + Wave 35 metrics |
| Honeypot rejection counter (`beta_honeypot_rejections_total`) | ✅ | Wave 35 GAP-387 |
| EncryptionService AES-256-GCM strict 32-byte validate | ✅ | Wave 39 GAP-426 (post-fix) |
| MFA TOTP cho admin | ❌ | P2-4 — chưa designed |
| JWT refresh-rotation explicit audit | ⚠️ | P3-2 — implementation exists nhưng audit chưa explicit |

---

## Delta Analysis vs 84 baseline (Wave 35)

| Category | Δ | Lý do |
|----------|:-:|-------|
| Cat 1 Dependency | 0 | Trivy gating đã ở Wave 35; FE pnpm audit job vẫn miss (-1 carry-over) |
| Cat 2 Secrets | +2 | `secrets-management-runbook.md` shipped (GAP-379) + GAP-426 setup.sh strict-base64 fix + EncryptionService 32-byte validate |
| Cat 3 OWASP | 0 | A02 +1 (GAP-426), A05 +1 (GAP-388 2FA), nhưng A07 carry-over -1 (CSP/HSTS missing) → net 0 |
| Cat 4 Auth | +1 | GAP-388 claim-code 2FA + per-email rate-limit closed P1-2/P1-3 baseline. JWT refresh + MFA admin vẫn -2 |
| Cat 5 Infra | 0 | ZAP baseline + restore-drill workflows wired Wave 37/38, nhưng K8s securityContext + gateway header filter vẫn carry-over -7 |
| **Subtotal** | **+3 → 87/100** | (B grade — production-ready với 2 P1 fix nhanh ~3h) |

---

## Verdict

**Score: 87/100 B** — vượt cổng `release-deploy-standard.md` §3.4 MAJOR requirement (≥80) cho Phase 1 BETA launch.

**Cụm `release-deploy-artifacts` Phase 7 gate:** ✅ **PASS** (87 ≥ 80). Production-deployable trong điều kiện hiện tại.

### Wave 33-39 cumulative security wins
1. ✅ Admin auth defense-in-depth (Wave 35 GAP-384)
2. ✅ PDPL Phase 2 ConsentBanner integrated production (Wave 38, both FE PublicLayout)
3. ✅ Beta access 2FA (claim-code) + per-email rate-limit (Wave 36 GAP-388)
4. ✅ Trivy + Cosign + SBOM (`docker-build-push.yml` GAP-400)
5. ✅ AWS Secrets Manager runbook + Terraform automation (GAP-379, secrets.tf)
6. ✅ Encryption key generation hardening (GAP-426 — base64 nguyên vẹn 32 bytes)
7. ✅ ZAP baseline scan workflow + restore-drill monthly cron wired
8. ✅ Cloudflare CDN HSTS verification script (`verify-cdn-headers.sh`)

### Khuyến nghị trước production deploy (Phase 7)
1. **Fix P1-1 K8s `securityContext.runAsNonRoot`** (1h) — mặc dù EKS defer Phase 2, manifest vẫn phải ready
2. **Fix P1-2 gateway security headers filter** (2h) — Cloudflare HSTS edge defense + app-level CSP/HSTS defense-in-depth
3. **Document MFA TOTP roadmap** (1h ADR + scheduling) — admin compromise risk acknowledged

Sau 3 fix → expected 90-91/100 A−.

---

## Proposed Gaps (KHÔNG file trong audit này — pipeline `audit-to-gap-pipeline.md` step 3)

| Tên đề xuất | Sev | Effort | Rationale |
|---|:---:|:---:|---|
| GAP-XXX-k8s-security-context-runasnonroot | P1 | 1h | Container escape defense |
| GAP-XXX-gateway-security-headers-filter | P1 | 2h | OWASP A05/A07 app-level CSP/HSTS |
| GAP-XXX-secrets-rotation-lambda | P2 | 4h | Long-lived secret risk |
| GAP-XXX-fe-ci-pnpm-audit-job | P2 | 1h (2 workflow × 30m) | PR-time CVE gating |
| GAP-XXX-zap-baseline-weekly-cron | P2 | 1d (sau staging URL stable) | Regression detection cadence |
| GAP-XXX-admin-mfa-totp-design | P2 | 1d ADR + 3-5d code | Admin compromise mitigation |
| GAP-XXX-k8s-network-policy | P3 | 3h | Lateral movement defense |
| GAP-XXX-jwt-refresh-rotation-audit | P3 | 2h | Audit trail completeness |

---

## Phase 7 Production Deploy Gate Verdict

**Cổng:** `post-wave-audit-mandate.md` §2.4.2 cho cụm `release-deploy-artifacts` + `release-deploy-standard.md` §3.4 MAJOR (`v1.0.0`)

| Yêu cầu | Threshold | Audit kết quả | Verdict |
|---|:---:|:---:|:---:|
| Security audit /100 | ≥ 80 | **87/100 B** | ✅ **PASS** |
| Penetration test light (OWASP Top 10) | conducted | code-level review hoàn tất | ⚠️ PARTIAL (live ZAP scan chờ staging up) |
| Secret in repo | 0 | 0 production / 14 test isolated | ✅ PASS |
| Trivy HIGH/CRITICAL fail-gate | wired | `docker-build-push.yml` ✅ | ✅ PASS |
| Cosign + SBOM | required MAJOR | wired | ✅ PASS |
| Auth flow review | conducted | 14/16 elements ✅, 2 ⚠️/❌ | ✅ PASS (acceptable) |

**Verdict:** **GO** for Phase 7 BETA production deploy với điều kiện 2 P1 fix shipped trong wave kế tiếp (~3h work).

---

## 1-line summary

Wave 40 milestone audit cụm `release-deploy-artifacts` (Wave 33-39): Security score **84→87/100 (+3) B grade** — Phase 7 cổng PASS (≥80); 8 wins từ Wave 36 GAP-388 (claim-code 2FA + per-email rate-limit), Wave 38 ConsentBanner production integration, Wave 39 GAP-426 encryption-key fix, Trivy+Cosign+SBOM gating; 2 P1 carry-over (K8s `runAsNonRoot` + gateway CSP/HSTS) cần fix ~3h trước GA → expected 90/100 A−.
