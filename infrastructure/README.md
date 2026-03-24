# Infrastructure

Deployment, orchestration, and provisioning configurations for the Kite Platform.

## Structure

```
infrastructure/
├── helm/                   # Helm charts for Kubernetes
│   ├── kitehub/            # KiteHub platform chart
│   └── kiteclass-instance/ # Per-tenant KiteClass chart
├── k8s/                    # Raw Kubernetes manifests
│   ├── kitehub/            # KiteHub deployments, services, secrets
│   └── kiteclass-template/ # KiteClass instance template
├── terraform-aws/          # AWS infrastructure (VPC, EKS, RDS, S3, ECR)
├── terraform-oracle/       # Oracle Cloud infrastructure (compute, network)
└── logs/                   # CI/CD operation logs (mostly gitignored)
```

## Usage

### Helm (Production/Staging)
```bash
helm upgrade --install kitehub ./infrastructure/helm/kitehub \
  --namespace kite-platform \
  --set global.image.tag=latest
```

### Terraform — AWS
```bash
cd infrastructure/terraform-aws
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply
```

### Terraform — Oracle Cloud
```bash
cd infrastructure/terraform-oracle
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply
```

### Kubernetes (Manual)
```bash
kubectl apply -f infrastructure/k8s/kitehub/
kubectl apply -f infrastructure/k8s/kiteclass-template/
```

## Docker

Docker Compose files live in service directories, not here:
- **Full stack:** `kitehub/docker-compose.kitehub.yml` (canonical)
- **KiteClass standalone:** `kiteclass/docker-compose.dev.yml`

See `documents/02-architecture/docker-platform-architecture.md` for details.
