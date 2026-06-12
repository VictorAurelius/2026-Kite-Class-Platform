# Environment Configuration Matrix

## Environments

| Aspect | Local (dev) | Staging | Production (Primary) | Production (Backup) |
|--------|-------------|---------|---------------------|---------------------|
| **Profile** | `dev` | `staging` | `prod` | `prod` |
| **Provider** | Docker Compose | Oracle Cloud | **Oracle Cloud Free** | AWS |
| **Domain** | localhost:3001 | staging.kitehub.me | kitehub.me | kitehub.me |
| **API URL** | localhost:9000 | api-staging.kitehub.me | api.kitehub.me | api.kitehub.me |
| **Cost** | $0 | $0 | **$0/tháng** | ~$338/tháng |

---

## Infrastructure

| Component | Local | Production (Oracle Free) | Production Backup (AWS) |
|-----------|-------|-------------------------|------------------------|
| **Compute** | Docker Compose | 2x ARM VMs (4 OCPU, 24GB total) | EKS (3+ nodes, t3.medium) |
| **PostgreSQL** | Docker (postgres:15) | Docker on VM (self-hosted) | RDS (db.t3.medium, Multi-AZ) |
| **Redis** | Docker (redis:7) | Docker on VM (self-hosted) | ElastiCache (t3.small) |
| **RabbitMQ** | Docker (rabbitmq:3) | Docker on VM (self-hosted) | Amazon MQ (t3.small) |
| **S3/Storage** | MinIO (Docker) | Oracle Object Storage (20GB) | S3 Bucket + CloudFront |
| **AI** | Ollama (Docker, 8b) | Ollama on ARM (8b) | OpenAI API (fallback) |
| **SSL** | None (HTTP) | Let's Encrypt (certbot) | ACM (AWS Certificate Manager) |
| **DNS** | /etc/hosts | Cloudflare (free) | Route53 |
| **CDN** | None | Cloudflare (free) | CloudFront |
| **LB** | None | Oracle LB (10 Mbps, free) | ALB |

---

## Security

| Setting | Local | Production (Oracle) | Production (AWS) |
|---------|-------|--------------------|--------------------|
| **JWT Secret** | Random (from .env) | .env on VM (encrypted disk) | AWS Secrets Manager |
| **Encryption Key** | Random (from .env) | .env on VM | AWS Secrets Manager |
| **DB Password** | Random (from .env) | .env on VM | RDS managed |
| **CORS** | localhost:* | production domain only | production domain only |
| **HTTPS** | No | Yes (Let's Encrypt) | Yes (ACM enforced) |
| **Rate Limiting** | Relaxed | Strict (by tier) | Strict (by tier) |
| **Health Details** | always | when_authorized | when_authorized |
| **SQL Logging** | DEBUG | OFF | OFF |
| **Log Level** | DEBUG | WARN | WARN |

---

## Services

| Service | Local | Production (Oracle) | Production (AWS) |
|---------|-------|--------------------|--------------------|
| **AI Provider** | Ollama (mock/8b) | **Ollama (8b on ARM)** | OpenAI API |
| **VietQR** | Mock (placeholder) | Production API | Production API |
| **Email** | Mock (log only) | SES or SMTP | SES Production |
| **Storage** | MinIO mock | Oracle Object Storage | S3 + CDN |

---

## Data

| Data Type | Local | Production |
|-----------|-------|------------|
| **Demo Users** | seed-data.sh | NONE |
| **KiteTeam** | seed-data.sh | Real accounts |
| **Test Instances** | Auto-created | Customer-created |
| **DB Provisioning** | Real (shared PostgreSQL) | Real (per-tenant) |
| **Backup** | None | pg_dump → Object Storage (Oracle) / RDS snapshots (AWS) |

---

## Deployment

| Aspect | Local | Production (Oracle) | Production (AWS) |
|--------|-------|--------------------|--------------------|
| **Deploy Method** | `./scripts/up.sh` | `docker compose up -d` (SSH) | `helm upgrade` |
| **Image Source** | Local Docker build | Docker Hub / GHCR | ECR (tagged release) |
| **Rollback** | `./scripts/down.sh` | `docker compose down` + previous images | `helm rollback` |
| **Replicas** | 1 | 1 (single VM) | 2-3+ |
| **Auto-scaling** | No | No | Yes (2-20) |
| **Zero-downtime** | No | No (accept brief downtime) | Canary → Rolling |

---

## Cost Comparison

| | Oracle Free (Primary) | AWS (Backup) |
|--|----------------------|--------------|
| Compute | $0 | ~$163 |
| Database | $0 | ~$100 |
| Cache + Queue | $0 | ~$45 |
| Storage + LB | $0 | ~$30 |
| AI | $0 (Ollama) | ~$15 (OpenAI) |
| **Total** | **$0/tháng** | **~$338/tháng** |

**KiteClass Instances**: AWS (không đổi, per-tenant cost riêng)
