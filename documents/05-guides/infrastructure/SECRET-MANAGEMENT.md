# Secret Management Guide

> Last updated: 2026-03-24 | Owner: DevOps/Security

## Overview

All secrets **MUST** be injected via environment variables. No secret defaults are allowed in production configuration files.

---

## Required Secrets

### KiteClass Gateway (`kiteclass-gateway`)

| Variable | Description | Generation |
|----------|-------------|------------|
| `JWT_SECRET` | HS512 signing key (min 512-bit) | `openssl rand -base64 64` |
| `INTERNAL_API_SECRET` | Internal API auth between gateway↔core | `openssl rand -hex 32` |
| `DB_PASSWORD` | PostgreSQL password for kiteclass_dev | Strong random password |
| `MAIL_PASSWORD` | SMTP app password | From email provider |

### KiteHub Services

| Variable | Description | Generation |
|----------|-------------|------------|
| `JWT_SECRET` | HS512 signing key — shared across kitehub services | `openssl rand -base64 64` |
| `DB_PASSWORD` | PostgreSQL password | Strong random password |
| `REDIS_PASSWORD` | Redis auth password | `openssl rand -hex 32` |
| `MAIL_PASSWORD` | SMTP app password | From email provider |
| `CLAUDE_API_KEY` | Anthropic API key for AI branding | From Anthropic console |
| `STRIPE_SECRET_KEY` | Stripe secret key for billing | From Stripe dashboard |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | From Stripe dashboard |

---

## Generation Commands

```bash
# JWT Secret (512-bit minimum for HS512)
openssl rand -base64 64

# General secrets / API tokens
openssl rand -hex 32

# Verify JWT secret length (must be >= 64 bytes)
echo -n "$JWT_SECRET" | wc -c
```

---

## Environment Setup

### Local Development

Use `.env.local` (gitignored):

```bash
# kiteclass/.env.local
JWT_SECRET=<generated-secret>
INTERNAL_API_SECRET=<generated-secret>
DB_PASSWORD=kiteclass123   # local dev only — never in production
```

**Never commit `.env` or `.env.local` files.**

### Production (Docker Compose)

Use Docker secrets or environment injection from CI/CD:

```yaml
# docker-compose.prod.yml
services:
  kiteclass-gateway:
    environment:
      JWT_SECRET: ${JWT_SECRET}          # from .env file or CI
      INTERNAL_API_SECRET: ${INTERNAL_API_SECRET}
```

### CI/CD (GitHub Actions)

Store all secrets in **GitHub Repository Secrets** (Settings → Secrets and variables → Actions):

| Secret Name | Used by Workflow |
|-------------|-----------------|
| `JWT_SECRET` | `deploy-staging.yml`, `deploy-prod.yml` |
| `DB_PASSWORD` | All deploy workflows |
| `CLAUDE_API_KEY` | `kitehub-ci.yml` (branding service tests) |
| `STRIPE_SECRET_KEY` | `kitehub-ci.yml` (subscription tests) |

---

## Security Rules

1. **No default values** for secrets in `application.yml` — use `${VAR_NAME}` without fallback.
2. **No secrets in git history** — use `git-secrets` or `gitleaks` pre-commit hook.
3. **Rotate secrets every 90 days** in production.
4. **Different secrets per environment** — dev ≠ staging ≠ production.
5. **Minimum key lengths:**
   - JWT: 512-bit (64 bytes) for HS512
   - General tokens: 256-bit (32 bytes)

---

## Secret Scanning

Run before each PR:

```bash
# Check for hardcoded secrets
grep -rn "password\s*=\s*['\"]" --include="*.yml" --include="*.yaml" \
  kiteclass kitehub | grep -v "your-" | grep -v ".env"

# Or use gitleaks (if installed)
gitleaks detect --source=. --verbose
```

---

## Incident Response

If a secret is accidentally committed:

1. **Immediately rotate** the compromised secret
2. Force-push to remove from history (coordinate with team)
3. Use `git filter-repo` to purge from all branches
4. Notify team via internal channel
5. Document incident in `documents/04-quality/`
