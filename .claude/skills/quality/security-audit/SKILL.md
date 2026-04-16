---
name: security-audit
description: "Dùng khi user nói 'security audit', 'pentest', 'kiểm tra bảo mật', 'dependency scan', hoặc trước production deploy. Deep security check /100 — vượt xa quality-audit §2."
user-invocable: true
---

# /security-audit — Deep Security Assessment

Score /100. Goes deeper than quality-audit's 10-point security category. Covers dependencies, secrets, OWASP, auth, and infra.

## Process

### 1. Run Automated Scans

```bash
# Dependency vulnerabilities
cd kitehub/kitehub-frontend && npm audit --json 2>/dev/null | head -50
cd kiteclass/kiteclass-frontend && npm audit --json 2>/dev/null | head -50

# Secret patterns (grep is OK for scanning — not executing)
grep -rn "password\s*=\|secret\s*=\|api_key\|Bearer " --include="*.java" --include="*.ts" --include="*.yml" kiteclass/ kitehub/ | grep -v node_modules | grep -v test | grep -v ".example"

# Hardcoded IPs/URLs
grep -rn "localhost\|127\.0\.0\.1\|0\.0\.0\.0" --include="*.java" --include="*.yml" kiteclass/kiteclass-core/src/main/ | head -20
```

### 2. Score 5 Categories

| # | Category (20pts) | Key Checks |
|---|-----------------|------------|
| 1 | **Dependency Vulnerabilities** | npm audit critical/high count, Maven dep versions |
| 2 | **Secrets & Credentials** | No hardcoded secrets, .env gitignored, rotation policy |
| 3 | **OWASP Top 10** | XSS/SQLi/CSRF/SSRF guards per Wave 4 defense-in-depth |
| 4 | **Auth & Access Control** | JWT validation, role checks, rate limiting, session mgmt |
| 5 | **Infrastructure Security** | TLS config, CORS, CSP, Docker non-root, k8s security context |

Scoring details: `reference/scoring-guide.md`

### 3. Output

Save to `documents/04-quality/audits/security/security-audit-[date].md`

## Context Management

Token budget ~25-40K. Kiểm soát bằng:

1. **npm audit output** — `npm audit --json 2>/dev/null | head -50`. KHÔNG đọc full JSON (có thể 10K+ lines). Chỉ cần summary + critical/high count.
2. **Secret scan** — LUÔN `| grep -v node_modules | grep -v test | head -30`. Không scan toàn bộ repo.
3. **Staged execution** — Phase 1: automated scans (categories 1-2). Phase 2: manual code review (categories 3-5). Nếu context low sau phase 1, delegate phase 2 cho subagent.
4. **Config files** — Đọc CHỈ security-related sections của `application.yml`, không đọc toàn bộ file. Dùng `grep -A5 'security\|jwt\|cors\|csrf'`.

## Gotchas

- Wave 4 added SVG sanitizer, URL allowlist, CSRF provider — verify they're ACTIVE not just coded
- `application.yml` security keys: check both main AND test profiles
- Gateway CORS config is the real enforcement point — not individual service configs
- JSoup 1.18.1 was added for SVG sanitization — check for CVEs on that version
- Rate limiting config is in gateway `application.yml`, not core
- npm audit JSON output có thể rất lớn — LUÔN limit output

## Skill Contents

- `reference/scoring-guide.md` — Detailed rubric per category
