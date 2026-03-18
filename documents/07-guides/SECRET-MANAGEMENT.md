# Secret Management Guide

## Secrets Inventory

| Secret | Service | Required | How to Generate |
|--------|---------|----------|-----------------|
| `JWT_SECRET` | Subscription | **YES** (all envs) | `openssl rand -base64 64` |
| `ENCRYPTION_MASTER_KEY` | Subscription | **YES** (prod) | `openssl rand -base64 32` |
| `POSTGRES_PASSWORD` | All | **YES** | `openssl rand -base64 24` |
| `RABBITMQ_PASSWORD` | All | **YES** | `openssl rand -base64 24` |
| `MINIO_ROOT_PASSWORD` | Branding | **YES** | `openssl rand -base64 24` |
| `OPENAI_API_KEY` | Branding | No (mock ok) | From OpenAI dashboard |
| `VIETQR_API_KEY` | Subscription | No (mock ok) | From VietQR dashboard |
| `WEBHOOK_PAYMENT_SECRET` | Subscription | No | `openssl rand -base64 32` |
| `INTERNAL_API_SECRET` | KiteClass Core | **YES** | `openssl rand -base64 32` |

---

## Local Development

Secrets stored in `.env` file (gitignored):
```bash
# Auto-generate with setup script
./scripts/setup.sh  # Creates .env with random values
```

---

## Production (AWS)

### Option A: AWS Secrets Manager (Recommended)

```bash
# Create secrets
aws secretsmanager create-secret \
  --name kitehub/prod/database \
  --secret-string '{"password":"<generated>","username":"kitehub"}'

aws secretsmanager create-secret \
  --name kitehub/prod/jwt \
  --secret-string '<generated-jwt-secret>'

aws secretsmanager create-secret \
  --name kitehub/prod/encryption \
  --secret-string '<generated-encryption-key>'
```

### Option B: Kubernetes Secrets

```bash
kubectl -n kitehub create secret generic kitehub-secrets \
  --from-literal=jwt-secret="$(openssl rand -base64 64)" \
  --from-literal=encryption-key="$(openssl rand -base64 32)" \
  --from-literal=db-password="<rds-password>"
```

---

## Rotation Policy

| Secret | Rotation | Method |
|--------|----------|--------|
| JWT Secret | 90 days | Generate new → rolling deploy |
| Encryption Key | 1 year | Requires re-encryption of all data |
| DB Password | 90 days | RDS password rotation |
| API Keys | As needed | Update in Secrets Manager |

### Rotation Steps
1. Generate new secret value
2. Update in AWS Secrets Manager / K8s Secret
3. Rolling restart affected pods: `kubectl rollout restart deployment/<name>`
4. Verify health: `kubectl get pods` + health check URLs
5. Old secret becomes invalid after all pods restarted

---

## Security Rules

1. **NEVER** commit secrets to git (enforced by .gitignore)
2. **NEVER** log secret values (enforced by code review)
3. **NEVER** use default secrets in production (enforced by fail-fast)
4. **ALWAYS** use different secrets per environment
5. **ALWAYS** generate secrets with cryptographically secure randomness
