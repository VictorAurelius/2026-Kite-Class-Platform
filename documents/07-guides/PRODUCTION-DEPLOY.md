# Production Deployment Checklist

## Prerequisites

### AWS Account
- [ ] AWS Account with admin access
- [ ] AWS CLI installed and configured: `aws configure`
- [ ] kubectl installed
- [ ] Helm 3 installed
- [ ] Terraform installed (>= 1.5)

### Domain & DNS
- [ ] Domain registered: `kiteclass.com` (or your domain)
- [ ] DNS managed by Route53 (or external DNS)
- [ ] Wildcard SSL: `*.kiteclass.com`

---

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
# Generate secrets
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_KEY=$(openssl rand -base64 32)
DB_PASSWORD=$(aws rds describe-db-instances --query ... )  # From Terraform output

# Create in AWS Secrets Manager
aws secretsmanager create-secret --name kitehub/production/jwt \
  --secret-string "$JWT_SECRET"

aws secretsmanager create-secret --name kitehub/production/encryption \
  --secret-string "$ENCRYPTION_KEY"

# Or create Kubernetes secrets directly
kubectl create namespace kitehub
kubectl -n kitehub create secret generic kitehub-secrets \
  --from-literal=jwt-secret="$JWT_SECRET" \
  --from-literal=encryption-key="$ENCRYPTION_KEY" \
  --from-literal=db-password="$DB_PASSWORD"
```

## Step 4: Deploy KiteHub Platform

```bash
helm install kitehub ./helm/kitehub \
  --namespace kitehub \
  --values helm/kitehub/values-prod.yaml \
  --set global.image.registry=<ECR_REGISTRY> \
  --set global.database.host=<RDS_ENDPOINT> \
  --set global.redis.host=<ELASTICACHE_ENDPOINT>
```

## Step 5: Verify

```bash
# Check pods
kubectl -n kitehub get pods

# Check services
kubectl -n kitehub get svc

# Health check
curl https://api.kiteclass.com/actuator/health
```

## Step 6: SSL/TLS (cert-manager)

```bash
# Install cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set installCRDs=true

# Create ClusterIssuer for Let's Encrypt
kubectl apply -f k8s/cert-manager/cluster-issuer.yaml
```

---

## Environment Variables (ALL Required)

| Variable | Description | Example |
|----------|-------------|---------|
| `JWT_SECRET` | JWT signing key (min 64 chars) | `openssl rand -base64 64` |
| `ENCRYPTION_MASTER_KEY` | AES-256 key (32 bytes base64) | `openssl rand -base64 32` |
| `SPRING_DATASOURCE_URL` | RDS JDBC URL | `jdbc:postgresql://rds-endpoint:5432/kitehub` |
| `SPRING_DATASOURCE_PASSWORD` | RDS password | From Terraform output |
| `OPENAI_API_KEY` | OpenAI API key | `sk-...` |
| `VIETQR_API_KEY` | VietQR Premium key | From VietQR dashboard |
| `WEBHOOK_PAYMENT_SECRET` | HMAC secret for webhooks | `openssl rand -base64 32` |

---

## Rollback

```bash
# List releases
helm history kitehub -n kitehub

# Rollback to previous version
helm rollback kitehub <REVISION> -n kitehub

# Emergency: scale down
kubectl -n kitehub scale deployment --all --replicas=0
```

---

## Monitoring

```bash
# Install Prometheus + Grafana
helm install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace

# Access Grafana
kubectl -n monitoring port-forward svc/monitoring-grafana 3000:80
# Default: admin / prom-operator
```

---

## Cost Estimate (Monthly)

| Resource | Spec | Cost |
|----------|------|------|
| EKS Cluster | Control plane | ~$73 |
| EC2 (3x t3.medium) | Worker nodes | ~$90 |
| RDS (db.t3.medium) | Multi-AZ PostgreSQL | ~$100 |
| ElastiCache (cache.t3.micro) | Redis | ~$15 |
| Amazon MQ (mq.t3.micro) | RabbitMQ | ~$30 |
| S3 | Storage | ~$5 |
| ALB | Load balancer | ~$25 |
| **Total** | | **~$338/month** |
