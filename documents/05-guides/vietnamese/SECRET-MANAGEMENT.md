# Secret Management Guide

## Secrets Inventory

| Secret | Service | Required | How to Generate |
|--------|---------|----------|-----------------|
| `JWT_SECRET` | Subscription | **YES** (all envs) | `openssl rand -base64 64` |
| `ENCRYPTION_MASTER_KEY` | Subscription | **YES** (prod) | `openssl rand -base64 32` |
| `POSTGRES_PASSWORD` | All | **YES** | `openssl rand -base64 24` |
| `RABBITMQ_PASSWORD` | All | **YES** | `openssl rand -base64 24` |
| `MINIO_ROOT_PASSWORD` | Branding | **YES** (local only) | `openssl rand -base64 24` |
| `AI_PROVIDER` | Branding | No (default: openai) | `ollama` or `openai` |
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

## Production: Oracle Cloud (Primary - $0/tháng)

Secrets stored in `.env` file on VM (encrypted disk, SSH-only access):

```bash
# On VM 1 (backend)
ssh opc@<VM_IP>
cd ~/kitehub

# Generate .env
cat > .env << 'EOF'
POSTGRES_PASSWORD=$(openssl rand -base64 24)
RABBITMQ_PASSWORD=$(openssl rand -base64 24)
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_MASTER_KEY=$(openssl rand -base64 32)
INTERNAL_API_SECRET=$(openssl rand -base64 32)
AI_PROVIDER=ollama
EOF

# Restrict permissions
chmod 600 .env
```

**Security notes (Oracle):**
- `.env` file chỉ accessible qua SSH
- Oracle VM boot volume encrypted by default
- Không có Secrets Manager miễn phí → `.env` trên VM là đủ cho giai đoạn đầu
- Backup `.env` ra local machine (encrypted)

---

## Production: AWS (Backup - ~$338/tháng)

### Option A: AWS Secrets Manager (Recommended)

```bash
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

| Secret | Rotation | Oracle | AWS |
|--------|----------|--------|-----|
| JWT Secret | 90 days | Update .env → restart containers | Secrets Manager → rolling deploy |
| Encryption Key | 1 year | Requires re-encryption | Same |
| DB Password | 90 days | Update .env + psql ALTER | RDS password rotation |
| API Keys | As needed | Update .env | Update Secrets Manager |

### Rotation Steps (Oracle)
1. Generate new secret value
2. Update `.env` on VM
3. Restart affected containers: `docker compose restart <service>`
4. Verify health: `docker compose ps` + health check URLs

### Rotation Steps (AWS)
1. Generate new secret value
2. Update in AWS Secrets Manager / K8s Secret
3. Rolling restart: `kubectl rollout restart deployment/<name>`
4. Verify: `kubectl get pods` + health check URLs

---

## Security Rules

1. **NEVER** commit secrets to git (enforced by .gitignore)
2. **NEVER** log secret values (enforced by code review)
3. **NEVER** use default secrets in production (enforced by fail-fast)
4. **ALWAYS** use different secrets per environment
5. **ALWAYS** generate secrets with cryptographically secure randomness
