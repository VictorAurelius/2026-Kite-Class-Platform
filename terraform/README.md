# KiteHub Terraform Infrastructure

Creates full AWS infrastructure for KiteHub production deployment.

## Prerequisites

- [Terraform >= 1.5](https://developer.hashicorp.com/terraform/install)
- [AWS CLI](https://aws.amazon.com/cli/) configured with admin access
- AWS account with billing enabled

## Quick Start

```bash
# 1. Configure
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values

# 2. Initialize
terraform init

# 3. Preview changes
terraform plan

# 4. Deploy (~15-20 minutes)
terraform apply

# 5. Configure kubectl
$(terraform output -raw configure_kubectl)

# 6. Verify
kubectl get nodes
```

## Resources Created

| Resource | Type | Cost/month |
|----------|------|------------|
| VPC | Network (2 public + 2 private subnets) | Free |
| EKS Cluster | Kubernetes control plane | ~$73 |
| EC2 Nodes | 2x t3.medium (EKS workers) | ~$60 |
| RDS PostgreSQL | db.t3.medium (Multi-AZ) | ~$100 |
| ElastiCache | cache.t3.micro (Redis) | ~$15 |
| NAT Gateway | Outbound internet for private subnets | ~$32 |
| S3 | Asset storage | ~$5 |
| ECR | Docker image registry | ~$1 |
| **Total** | | **~$286/month** |

## Outputs

After `terraform apply`, use these values for Helm deployment:

```bash
terraform output eks_cluster_name      # EKS cluster name
terraform output rds_endpoint          # RDS connection endpoint
terraform output redis_endpoint        # ElastiCache endpoint
terraform output s3_bucket_name        # S3 bucket name
terraform output ecr_registry          # ECR registry URL
terraform output -raw rds_password     # RDS password (sensitive)
```

## Destroy

```bash
terraform destroy  # WARNING: Destroys ALL resources
```

## Cost Optimization Tips

- **Dev/Staging**: Use `t3.small` nodes, single-AZ RDS, no NAT Gateway
- **Scale down**: `eks_node_desired_size = 1` when not in use
- **Spot instances**: Add spot node group for non-critical workloads
