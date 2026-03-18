# Environment Configuration Matrix

## Environments

| Aspect | Local (dev) | Staging | Production |
|--------|-------------|---------|------------|
| **Profile** | `dev` | `staging` | `prod` |
| **Domain** | localhost:3001 | staging.kiteclass.com | kiteclass.com |
| **API URL** | localhost:9000 | api-staging.kiteclass.com | api.kiteclass.com |

---

## Infrastructure

| Component | Local | Staging | Production |
|-----------|-------|---------|------------|
| **Kubernetes** | Docker Compose | EKS (1 node) | EKS (3+ nodes) |
| **PostgreSQL** | Docker (postgres:15) | RDS (db.t3.micro) | RDS (db.t3.medium, Multi-AZ) |
| **Redis** | Docker (redis:7) | ElastiCache (t3.micro) | ElastiCache (t3.small) |
| **RabbitMQ** | Docker (rabbitmq:3) | Amazon MQ (t3.micro) | Amazon MQ (t3.small) |
| **S3/Storage** | MinIO (Docker) | S3 Bucket | S3 Bucket + CloudFront |
| **SSL** | None (HTTP) | Let's Encrypt | ACM (AWS Certificate Manager) |
| **DNS** | /etc/hosts | Route53 | Route53 |
| **CDN** | None | None | CloudFront |

---

## Security

| Setting | Local | Staging | Production |
|---------|-------|---------|------------|
| **JWT Secret** | Random (from .env) | AWS Secrets Manager | AWS Secrets Manager |
| **Encryption Key** | Random (from .env) | AWS Secrets Manager | AWS Secrets Manager |
| **DB Password** | Random (from .env) | RDS managed | RDS managed |
| **CORS** | localhost:* | staging domain | production domain only |
| **HTTPS** | No | Yes | Yes (enforced) |
| **Rate Limiting** | Relaxed | Standard | Strict (by tier) |
| **Health Details** | always | when_authorized | when_authorized |
| **SQL Logging** | DEBUG | OFF | OFF |
| **Log Level** | DEBUG | INFO | WARN |

---

## Services

| Service | Local | Staging | Production |
|---------|-------|---------|------------|
| **OpenAI** | Mock (sk-mock) | Real API | Real API |
| **VietQR** | Mock (placeholder) | Sandbox | Production API |
| **Email (SES)** | Mock (log only) | SES Sandbox | SES Production |
| **S3 Storage** | MinIO mock | Real S3 | Real S3 + CDN |

---

## Data

| Data Type | Local | Staging | Production |
|-----------|-------|---------|------------|
| **Demo Users** | seed-data.sh | seed-data.sh | NONE |
| **KiteTeam** | seed-data.sh | seed-data.sh | Real accounts |
| **Test Instances** | Auto-created | Manual | Customer-created |
| **DB Provisioning** | Real (shared PostgreSQL) | Real (shared RDS) | Real (per-tenant RDS) |

---

## Deployment

| Aspect | Local | Staging | Production |
|--------|-------|---------|------------|
| **Deploy Method** | `./scripts/up.sh` | `helm upgrade` (auto on develop) | `helm upgrade` (manual approve) |
| **Image Source** | Local Docker build | ECR (develop branch) | ECR (tagged release) |
| **Rollback** | `./scripts/down.sh` + `up.sh` | `helm rollback` | `helm rollback` |
| **Replicas** | 1 | 1-2 | 2-3+ |
| **Auto-scaling** | No | Yes (2-5) | Yes (2-20) |
| **Zero-downtime** | No | Rolling update | Canary → Rolling |
