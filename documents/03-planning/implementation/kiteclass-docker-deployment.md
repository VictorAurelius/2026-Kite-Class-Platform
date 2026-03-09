# KiteClass Docker Deployment Strategy

**Version:** 1.0
**Created:** 2026-03-09
**Purpose:** Docker Registry Pattern for KiteHub multi-tenant deployment

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  DEVELOPMENT & CI/CD WORKFLOW                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. GitHub Repository                                           │
│     └── 2026-Kite-Class-Platform/                              │
│         ├── kiteclass/                                          │
│         │   ├── kiteclass-core/        (Spring Boot)           │
│         │   ├── kiteclass-gateway/     (Spring Cloud Gateway)  │
│         │   └── kiteclass-frontend/    (Next.js 14)            │
│         └── docker/kiteclass/                                   │
│             ├── Dockerfile.core                                 │
│             ├── Dockerfile.gateway                              │
│             └── Dockerfile.frontend                             │
│                                                                 │
│  2. GitHub Actions CI/CD                                        │
│     └── On push to main or tag v*.*.* →                        │
│         ├── Build multi-stage Docker images                    │
│         ├── Run security scan (Trivy)                          │
│         └── Push to AWS ECR                                    │
│                                                                 │
│  3. AWS ECR (Docker Registry)                                   │
│     └── Registry: 123456789.dkr.ecr.us-east-1.amazonaws.com   │
│         ├── kiteclass/core:v1.0.0                              │
│         ├── kiteclass/core:v1.1.0                              │
│         ├── kiteclass/core:latest                              │
│         ├── kiteclass/gateway:v1.0.0                           │
│         └── kiteclass/frontend:v1.0.0                          │
│                                                                 │
│  4. KiteHub Provisioning Service                                │
│     └── Pull images from ECR →                                 │
│         Deploy to Kubernetes →                                  │
│         Configure per-tenant settings                           │
│                                                                 │
│  5. Kubernetes Cluster (EKS)                                    │
│     └── Namespace: kiteclass-instances                         │
│         ├── Pod: kiteclass-customer1-core                      │
│         │   Image: ecr.../kiteclass/core:v1.0.0               │
│         │   Env: INSTANCE_ID, DATABASE_URL                     │
│         ├── Pod: kiteclass-customer1-gateway                   │
│         │   Image: ecr.../kiteclass/gateway:v1.0.0            │
│         └── Pod: kiteclass-customer2-core                      │
│             Image: ecr.../kiteclass/core:v1.1.0 (different!)  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Dockerfile Specifications

### Core Service (Dockerfile.core)

**Build Strategy:** Multi-stage build
- **Stage 1 (Builder):** Maven build with dependencies cached
- **Stage 2 (Runtime):** JRE-only, minimal image size

**Key Features:**
- Base: `eclipse-temurin:17-jre-alpine` (~180MB)
- Non-root user: `spring:spring`
- Health check: `/actuator/health`
- JVM tuning: Container-aware, 75% max RAM
- Build time: ~3-5 minutes

**Image Size:** ~220MB (JAR ~40MB + JRE ~180MB)

### Gateway Service (Dockerfile.gateway)

**Same as Core** - Spring Boot service pattern

**Image Size:** ~200MB (smaller JAR)

### Frontend Service (Dockerfile.frontend)

**Build Strategy:** Next.js standalone output
- **Stage 1 (Deps):** Install dependencies with pnpm
- **Stage 2 (Builder):** Build Next.js with standalone output
- **Stage 3 (Runtime):** Node.js runtime only

**Key Features:**
- Base: `node:20-alpine` (~120MB)
- Non-root user: `nextjs:nodejs`
- Standalone output: Only runtime files, no dev dependencies
- Build time: ~2-4 minutes

**Image Size:** ~150MB (Next.js standalone ~30MB + Node ~120MB)

---

## CI/CD Workflow

### Trigger Events

1. **Push to main** → Build and tag as `latest`
2. **Push tag `v*.*.*`** → Build and tag as semantic version (e.g., `v1.0.0`, `1.0`, `1`)
3. **Pull Request** → Build only (no push)
4. **Manual Dispatch** → Build with custom version tag

### Build Process

```yaml
1. Checkout code
2. Setup Docker Buildx (multi-platform support)
3. Configure AWS credentials (OIDC)
4. Login to Amazon ECR
5. Build Docker image (multi-arch: amd64, arm64)
6. Push to ECR
7. Scan for vulnerabilities (Trivy)
8. Upload scan results to GitHub Security
```

### Tagging Strategy

| Git Action | Docker Tags Generated |
|------------|----------------------|
| Push to `main` | `latest`, `main-abc1234` (SHA) |
| Tag `v1.2.3` | `v1.2.3`, `1.2`, `1`, `latest` |
| PR #123 | `pr-123` (not pushed) |

---

## KiteHub Provisioning Integration

### Instance Deployment Flow

```java
@Service
public class InstanceProvisioningService {

    @Autowired
    private KubernetesService k8sService;

    @Autowired
    private DatabaseProvisioningService dbService;

    @Value("${kiteclass.docker.registry}")
    private String dockerRegistry; // 123456.dkr.ecr.us-east-1.amazonaws.com

    @Value("${kiteclass.default.version}")
    private String defaultVersion; // v1.0.0

    public Instance provisionInstance(CreateInstanceRequest request) {
        // 1. Create instance record
        Instance instance = new Instance();
        instance.setSubdomain(request.getSubdomain());
        instance.setStatus(InstanceStatus.TRIAL);
        instance = instanceRepo.save(instance);

        // 2. Provision database
        Database db = dbService.createDatabase(instance.getId());
        instance.setDatabaseUrl(db.getUrl());
        instance.setDatabaseUsername(db.getUsername());
        instance.setDatabasePassword(encrypt(db.getPassword()));
        instanceRepo.save(instance);

        // 3. Deploy Core Service
        String coreImage = String.format(
            "%s/kiteclass/core:%s",
            dockerRegistry,
            request.getVersion() != null ? request.getVersion() : defaultVersion
        );

        k8sService.deployDeployment(
            "kiteclass-instances",
            "kiteclass-core-" + instance.getId(),
            coreImage,
            Map.of(
                "SPRING_DATASOURCE_URL", db.getUrl(),
                "SPRING_DATASOURCE_USERNAME", db.getUsername(),
                "SPRING_DATASOURCE_PASSWORD", db.getPassword(),
                "INSTANCE_ID", instance.getId().toString(),
                "SPRING_PROFILES_ACTIVE", "production"
            ),
            Map.of(
                "cpu", "1000m",
                "memory", "2Gi"
            )
        );

        // 4. Deploy Gateway Service
        String gatewayImage = String.format(
            "%s/kiteclass/gateway:%s",
            dockerRegistry,
            request.getVersion() != null ? request.getVersion() : defaultVersion
        );

        k8sService.deployDeployment(
            "kiteclass-instances",
            "kiteclass-gateway-" + instance.getId(),
            gatewayImage,
            Map.of(
                "CORE_SERVICE_URL", "http://kiteclass-core-" + instance.getId() + ":8080",
                "INSTANCE_ID", instance.getId().toString(),
                "SPRING_PROFILES_ACTIVE", "production"
            ),
            Map.of(
                "cpu", "500m",
                "memory", "1Gi"
            )
        );

        // 5. Create Kubernetes Service (Load Balancer)
        k8sService.createService(
            "kiteclass-instances",
            instance.getSubdomain() + "-svc",
            Map.of(
                "app", "kiteclass-gateway-" + instance.getId()
            ),
            80,
            8080
        );

        // 6. Run Flyway migrations
        flywayService.migrate(db);

        // 7. Mark instance as ACTIVE
        instance.setStatus(InstanceStatus.ACTIVE);
        instanceRepo.save(instance);

        return instance;
    }
}
```

### Version Management

**Default Behavior:**
- New instances → Use `latest` tag (always newest version)
- Existing instances → Pin to specific version (e.g., `v1.0.0`)

**Upgrade Flow:**
```java
public void upgradeInstance(UUID instanceId, String newVersion) {
    Instance instance = instanceRepo.findById(instanceId)
        .orElseThrow(() -> new NotFoundException("Instance not found"));

    // Update Kubernetes Deployment image tag
    String newCoreImage = dockerRegistry + "/kiteclass/core:" + newVersion;
    String newGatewayImage = dockerRegistry + "/kiteclass/gateway:" + newVersion;

    k8sService.updateDeploymentImage(
        "kiteclass-instances",
        "kiteclass-core-" + instanceId,
        newCoreImage
    );

    k8sService.updateDeploymentImage(
        "kiteclass-instances",
        "kiteclass-gateway-" + instanceId,
        newGatewayImage
    );

    // Kubernetes will perform rolling update automatically
    // Old pods terminated after new pods are ready
}
```

---

## Security Considerations

### Image Scanning

**Trivy Scan (in CI/CD):**
- Scans for CVEs in base images and dependencies
- Blocks deployment if critical vulnerabilities found
- Uploads results to GitHub Security tab

### Secrets Management

**DO NOT include in Docker images:**
- Database passwords
- API keys
- JWT secrets
- AWS credentials

**Use Kubernetes Secrets instead:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: kiteclass-customer1-secrets
type: Opaque
stringData:
  DATABASE_PASSWORD: "encrypted_password_here"
  JWT_SECRET: "random_secret_here"
```

### Network Policies

**Isolate instances:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: kiteclass-customer1-policy
spec:
  podSelector:
    matchLabels:
      instance: customer1
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: kitehub-gateway
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres-customer1
```

---

## Cost Optimization

### Storage Costs (AWS ECR)

| Item | Monthly Cost |
|------|--------------|
| Storage (10 images × 500MB) | ~$0.50 |
| Data transfer (50GB pull) | ~$4.50 |
| **Total** | **~$5/month** |

### Image Size Optimization

**Current:**
- Core: 220MB
- Gateway: 200MB
- Frontend: 150MB

**Optimizations applied:**
- ✅ Multi-stage build (removed build tools from runtime)
- ✅ Alpine Linux (smallest base image)
- ✅ .dockerignore (exclude unnecessary files)
- ✅ Layer caching (dependencies cached separately)

**Potential improvements:**
- Use distroless images (Google) - reduce to ~120MB
- Use GraalVM native image - reduce to ~50MB (requires code changes)

---

## Rollback Strategy

### Rollback to Previous Version

```java
public void rollbackInstance(UUID instanceId) {
    Instance instance = instanceRepo.findById(instanceId)
        .orElseThrow(() -> new NotFoundException("Instance not found"));

    // Get previous version from deployment history
    String previousVersion = k8sService.getDeploymentHistory(
        "kiteclass-instances",
        "kiteclass-core-" + instanceId
    ).get(1).getImageTag(); // Index 0 = current, 1 = previous

    // Rollback
    upgradeInstance(instanceId, previousVersion);
}
```

### Zero-Downtime Deployment

Kubernetes performs **rolling updates** automatically:
1. Create new Pod with new image
2. Wait for health check to pass
3. Route traffic to new Pod
4. Terminate old Pod

**Max unavailable:** 0 (always have at least 1 pod running)
**Max surge:** 1 (max 1 extra pod during update)

---

## Monitoring & Observability

### Image Metrics

**CloudWatch Dashboards:**
- Image pull latency
- Image size trends
- Vulnerability scan results

### Deployment Metrics

**Prometheus:**
- `kiteclass_deployment_version{instance="customer1", service="core"}` → "v1.0.0"
- `kiteclass_deployment_uptime_seconds{instance="customer1"}`
- `kiteclass_deployment_restarts_total{instance="customer1"}`

---

## Checklist Before Production

- [ ] AWS ECR repository created (kiteclass/core, kiteclass/gateway, kiteclass/frontend)
- [ ] AWS IAM role for GitHub Actions OIDC authentication
- [ ] GitHub Secrets configured (AWS_ROLE_ARN)
- [ ] Trivy vulnerability scanning enabled
- [ ] Kubernetes cluster (EKS) provisioned
- [ ] Docker build workflow tested (push tag v0.1.0-beta)
- [ ] KiteHub provisioning service can pull images from ECR
- [ ] Network policies configured for multi-tenancy isolation
- [ ] Monitoring dashboards created (CloudWatch + Prometheus)
- [ ] Rollback procedure tested
- [ ] Cost alerts configured (ECR storage, data transfer)

---

## Reference

**Docker Best Practices:**
- Multi-stage builds
- Minimal base images (Alpine, Distroless)
- Security scanning (Trivy, Snyk)
- Non-root users
- Health checks

**Kubernetes Deployment:**
- Rolling updates
- Resource limits (CPU, memory)
- Readiness/Liveness probes
- Pod disruption budgets

**Multi-tenancy:**
- Namespace isolation
- Network policies
- Resource quotas per tenant

---

**Last Updated:** 2026-03-09
**Next Steps:** Setup AWS ECR, test Docker build workflow, integrate with KiteHub provisioning service
