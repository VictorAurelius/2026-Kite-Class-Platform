# Production Deployment Guide

## Strategy: Dual-Cloud

| | Primary | Backup |
|--|---------|--------|
| **KiteHub Platform** | Oracle Cloud Always Free ($0) | AWS ($338/tháng) |
| **KiteClass Instances** | AWS | - |

**Chi tiết architecture**: [kitehub-oracle-cloud-deployment.md](../03-planning/infrastructure/kitehub-oracle-cloud-deployment.md)

---

# Option A: Oracle Cloud Always Free (PRIMARY - $0/tháng)

## Prerequisites

- [ ] Oracle Cloud account: https://www.oracle.com/cloud/free/
- [ ] **Upgrade sang PAYG** (vẫn free, tránh idle reclamation)
- [ ] SSH key pair generated
- [ ] Domain registered + Cloudflare DNS (free)
- [ ] Docker + Docker Compose knowledge

## Step 1: Create VMs

```bash
# Qua Oracle Cloud Console (cloud.oracle.com):
# 1. Create VCN (Virtual Cloud Network)
#    - CIDR: 10.0.0.0/16
#    - Public subnet: 10.0.1.0/24
#    - Security List: allow 22, 80, 443, 9000

# 2. Create VM 1 - Backend
#    Shape: VM.Standard.A1.Flex
#    OCPU: 2, Memory: 12GB
#    Boot volume: 100GB
#    Image: Oracle Linux 9 (ARM)

# 3. Create VM 2 - Frontend + AI
#    Shape: VM.Standard.A1.Flex
#    OCPU: 2, Memory: 12GB
#    Boot volume: 100GB
#    Image: Oracle Linux 9 (ARM)
```

> ⚠️ Nếu gặp "Out of Capacity" → thử region khác hoặc retry sau vài giờ.

## Step 2: Setup Docker (cả 2 VMs)

```bash
ssh opc@<VM_IP>

# Install Docker
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker opc

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-aarch64" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify
docker --version
docker-compose --version
```

## Step 3: Deploy Backend (VM 1)

```bash
# Clone repo (hoặc copy docker-compose + .env)
git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform/kitehub

# Setup environment
cp .env.example .env
# Edit .env: set JWT_SECRET, ENCRYPTION_MASTER_KEY, passwords
# Set AI_PROVIDER=ollama (hoặc openai nếu dùng AWS backup)

# Start backend services only
docker compose -f docker-compose.prod-backend.yml up -d

# Verify
docker compose ps
curl http://localhost:9000/actuator/health
```

## Step 4: Deploy Frontend + AI (VM 2)

```bash
# On VM 2
cd 2026-Kite-Class-Platform/kitehub

# Pull Ollama model
docker run -d --name ollama -v ollama-data:/root/.ollama -p 11434:11434 ollama/ollama
docker exec ollama ollama pull llama3.1:8b
# ~4.7GB download, takes 5-10 minutes

# Start frontend + nginx
docker compose -f docker-compose.prod-frontend.yml up -d

# Verify
curl http://localhost:3001
curl http://localhost:11434/api/tags  # Ollama health
```

## Step 5: SSL + Domain

```bash
# On VM 2 (Nginx)
sudo apt install certbot python3-certbot-nginx  # hoặc dnf

# Get SSL cert
sudo certbot --nginx -d kitehub.me -d api.kitehub.me

# Auto-renew
sudo crontab -e
# 0 3 * * * certbot renew --quiet
```

**Cloudflare DNS** (alternative - recommended):
- Add A record: `kitehub.me` → VM 2 public IP
- Add A record: `api.kitehub.me` → VM 2 public IP
- Enable Cloudflare Proxy (orange cloud) → free SSL + CDN

## Step 6: Verify E2E

```bash
# From your laptop
curl https://kitehub.me                    # Frontend
curl https://api.kitehub.me/actuator/health # Gateway
curl -X POST https://api.kitehub.me/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'
```

## Step 7: Backup Setup

```bash
# Daily PostgreSQL backup → Oracle Object Storage
# On VM 1, create backup script:
cat > /home/opc/backup-db.sh << 'SCRIPT'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec kitehub-postgres pg_dumpall -U kitehub > /tmp/kitehub_${DATE}.sql
gzip /tmp/kitehub_${DATE}.sql
# Upload to Oracle Object Storage (oci cli)
oci os object put --bucket-name kitehub-backups \
  --file /tmp/kitehub_${DATE}.sql.gz \
  --name "db/kitehub_${DATE}.sql.gz"
rm /tmp/kitehub_${DATE}.sql.gz
# Keep only last 7 days locally
find /tmp -name "kitehub_*.sql.gz" -mtime +7 -delete
SCRIPT
chmod +x /home/opc/backup-db.sh

# Add to cron (daily at 3 AM)
echo "0 3 * * * /home/opc/backup-db.sh" | crontab -
```

---

# Option B: AWS (BACKUP - ~$338/tháng)

## Prerequisites

### AWS Account
- [ ] AWS Account with admin access
- [ ] AWS CLI installed and configured: `aws configure`
- [ ] kubectl installed
- [ ] Helm 3 installed
- [ ] Terraform installed (>= 1.5)

### Domain & DNS
- [ ] Domain registered: `kitehub.me` (or your domain)
- [ ] DNS managed by Route53 (or external DNS)
- [ ] Wildcard SSL: `*.kitehub.me`

## Step 1: Infrastructure (Terraform)

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values

terraform init
terraform plan
terraform apply
```

**Creates:**
- EKS Cluster (2-3 nodes, t3.medium)
- RDS PostgreSQL (db.t3.medium, Multi-AZ)
- ElastiCache Redis (cache.t3.micro)
- Amazon MQ RabbitMQ (mq.t3.micro)
- S3 Bucket (kitehub-assets)
- ECR Repositories (3)
- VPC, Subnets, Security Groups, IAM Roles

**Output:** EKS endpoint, RDS endpoint, ECR URLs

## Step 2: Configure kubectl

```bash
aws eks update-kubeconfig --name kitehub-cluster --region ap-southeast-1
kubectl get nodes  # Verify connection
```

## Step 3: Secrets

```bash
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_KEY=$(openssl rand -base64 32)

kubectl create namespace kitehub
kubectl -n kitehub create secret generic kitehub-secrets \
  --from-literal=jwt-secret="$JWT_SECRET" \
  --from-literal=encryption-key="$ENCRYPTION_KEY" \
  --from-literal=db-password="$DB_PASSWORD"
```

## Step 4: Deploy KiteHub Platform

```bash
helm install kitehub ./infrastructure/helm/kitehub \
  --namespace kitehub \
  --values infrastructure/helm/kitehub/values-prod.yaml \
  --set global.image.registry=<ECR_REGISTRY> \
  --set global.database.host=<RDS_ENDPOINT> \
  --set global.redis.host=<ELASTICACHE_ENDPOINT>
```

## Step 5: Verify

```bash
kubectl -n kitehub get pods
kubectl -n kitehub get svc
curl https://api.kitehub.me/actuator/health
```

---

# Switching Between Oracle ↔ AWS

## Oracle → AWS (khi Oracle fail)

```bash
# 1. Terraform apply (nếu chưa có infra)
cd terraform && terraform apply

# 2. Deploy
helm install kitehub ./infrastructure/helm/kitehub -n kitehub

# 3. DNS: update kitehub.me → AWS ALB
# Cloudflare: change A record to ALB DNS name

# 4. AI: set ai.provider=openai (hoặc deploy Ollama trên EC2)
```

**Thời gian**: ~2-3 giờ (nếu Terraform đã apply trước)

## AWS → Oracle (khi Oracle available lại)

```bash
# 1. Verify Oracle VMs running
ssh opc@<VM_IP> docker compose ps

# 2. Restore DB from backup
docker exec -i kitehub-postgres psql -U kitehub < backup.sql

# 3. DNS: update kitehub.me → Oracle LB IP
# 4. AI: set ai.provider=ollama
```

---

# Environment Variables (ALL Required)

| Variable | Description | Oracle | AWS |
|----------|-------------|--------|-----|
| `JWT_SECRET` | JWT signing key (min 64 chars) | .env on VM | Secrets Manager |
| `ENCRYPTION_MASTER_KEY` | AES-256 key | .env on VM | Secrets Manager |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | localhost:5432 | RDS endpoint |
| `SPRING_DATASOURCE_PASSWORD` | DB password | .env on VM | RDS managed |
| `AI_PROVIDER` | AI provider | `ollama` | `openai` |
| `OPENAI_API_KEY` | OpenAI key (AWS only) | Not needed | `sk-...` |
| `VIETQR_API_KEY` | VietQR key | Same | Same |

---

# Rollback

## Oracle
```bash
# SSH to VM
docker compose down
docker compose -f docker-compose.prod-backend.yml up -d  # with previous image tags
```

## AWS
```bash
helm history kitehub -n kitehub
helm rollback kitehub <REVISION> -n kitehub
```

---

# Cost Summary

| | Oracle (Primary) | AWS (Backup) |
|--|-----------------|--------------|
| Monthly | **$0** | ~$338 |
| Annual | **$0** | ~$4,056 |
| AI | $0 (Ollama) | ~$180/year (OpenAI) |
| **Savings** | **~$4,000/year** | Baseline |
