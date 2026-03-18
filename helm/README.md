# Helm Charts for KiteHub Platform

## Charts

### kitehub/ - Platform Services
Deploys all KiteHub platform services: gateway, subscription, branding, admin, email, frontend.

```bash
# Deploy platform
helm install kitehub ./helm/kitehub \
  --namespace kitehub --create-namespace \
  --set global.image.registry=<ECR_REGISTRY> \
  --set global.database.host=<RDS_ENDPOINT> \
  --set global.redis.host=<REDIS_ENDPOINT>

# Upgrade
helm upgrade kitehub ./helm/kitehub \
  --set global.image.tag=v1.2.0

# Rollback
helm rollback kitehub 1
```

### kiteclass-instance/ - Per-Tenant Instance
Deploys a KiteClass instance for a specific customer.

```bash
# Deploy instance
helm install customer1 ./helm/kiteclass-instance \
  --namespace kiteclass-instances --create-namespace \
  --set instanceId=abc12345-uuid \
  --set subdomain=customer1 \
  --set tier=BASIC \
  --set database.url=jdbc:postgresql://rds:5432/kiteclass_abc12345 \
  --set database.username=kiteclass_abc12345_user \
  --set image.registry=<ECR_REGISTRY>

# Scale up tier
helm upgrade customer1 ./helm/kiteclass-instance \
  --set tier=PREMIUM
```

## Resource Quotas by Tier

| Tier | Replicas | CPU | Memory | Rate Limit |
|------|----------|-----|--------|------------|
| FREE | 1 | 250-500m | 512Mi-1Gi | 100 req/min |
| BASIC | 2 | 500m-1 | 1-2Gi | 500 req/min |
| PREMIUM | 2 | 1-2 | 2-4Gi | 2000 req/min |
| ENTERPRISE | 3 | 2-4 | 4-8Gi | 10000 req/min |
