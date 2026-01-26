# Skill: Cloud Infrastructure

Kiến trúc Cloud và DevOps cho KiteClass Platform.

## Mô tả

Tài liệu hướng dẫn về:
- Cloud architecture (AWS primary)
- Kubernetes deployment
- CI/CD pipelines (GitHub Actions)
- Environment management (staging, production)
- Monitoring & observability
- Security & secrets management

## Trigger phrases

- "cloud deployment"
- "kubernetes"
- "ci/cd"
- "deploy production"
- "infrastructure"
- "aws config"

---

## Cloud Architecture Overview

### AWS Services Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS Cloud                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐     ┌──────────────────────────────────────────────────┐  │
│  │ Route 53    │────▶│               CloudFront (CDN)                   │  │
│  │ DNS         │     │  - Static assets                                  │  │
│  └─────────────┘     │  - Frontend (Next.js static export)               │  │
│                      └─────────────────────┬────────────────────────────┘  │
│                                            │                                │
│                      ┌─────────────────────▼────────────────────────────┐  │
│                      │          Application Load Balancer               │  │
│                      │  - SSL termination                                │  │
│                      │  - Path-based routing                             │  │
│                      └─────────────────────┬────────────────────────────┘  │
│                                            │                                │
│  ┌─────────────────────────────────────────┴───────────────────────────┐   │
│  │                        Amazon EKS Cluster                            │   │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐         │   │
│  │  │   KiteHub      │  │  KiteClass     │  │  KiteClass     │         │   │
│  │  │   Backend      │  │  Gateway       │  │  Core          │         │   │
│  │  │   (Pod)        │  │  (Pod)         │  │  (Pod)         │         │   │
│  │  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘         │   │
│  │          │                   │                   │                   │   │
│  └──────────┼───────────────────┼───────────────────┼───────────────────┘   │
│             │                   │                   │                       │
│  ┌──────────▼───────────────────▼───────────────────▼───────────────────┐   │
│  │                         Data Layer                                    │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                │   │
│  │  │ RDS          │  │ ElastiCache  │  │ Amazon MQ    │                │   │
│  │  │ PostgreSQL   │  │ Redis        │  │ RabbitMQ     │                │   │
│  │  │ Multi-AZ     │  │ Cluster      │  │              │                │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                │   │
│  │                                                                       │   │
│  │  ┌──────────────┐  ┌──────────────┐                                  │   │
│  │  │ S3           │  │ Secrets      │                                  │   │
│  │  │ File Storage │  │ Manager      │                                  │   │
│  │  └──────────────┘  └──────────────┘                                  │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Service Mapping

| Component | AWS Service | Purpose |
|-----------|-------------|---------|
| Container Orchestration | EKS (Kubernetes) | Run microservices |
| Database | RDS PostgreSQL | Primary database |
| Cache | ElastiCache Redis | Session, caching |
| Message Queue | Amazon MQ | Async messaging |
| File Storage | S3 | Uploads, media |
| CDN | CloudFront | Static assets, frontend |
| DNS | Route 53 | Domain management |
| Load Balancer | ALB | Traffic distribution |
| Secrets | Secrets Manager | Credentials |
| Monitoring | CloudWatch + Grafana | Logs, metrics |
| Container Registry | ECR | Docker images |

---

## Environment Strategy

### Environments

| Environment | Purpose | URL Pattern | Database |
|-------------|---------|-------------|----------|
| **Development** | Local dev | localhost:* | Docker Compose |
| **Staging** | Testing, QA | staging.kiteclass.com | RDS (separate) |
| **Production** | Live users | *.kiteclass.com | RDS (Multi-AZ) |

### Environment Variables by Stage

```yaml
# Base configuration (all environments)
base: &base
  SPRING_PROFILES_ACTIVE: "${ENVIRONMENT}"
  SERVER_PORT: 8080
  LOGGING_LEVEL_ROOT: INFO

# Development
development:
  <<: *base
  SPRING_PROFILES_ACTIVE: local
  SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/kiteclass_dev
  LOGGING_LEVEL_ROOT: DEBUG
  LOGGING_LEVEL_COM_KITECLASS: DEBUG

# Staging
staging:
  <<: *base
  SPRING_PROFILES_ACTIVE: staging
  SPRING_DATASOURCE_URL: ${RDS_STAGING_URL}
  SPRING_REDIS_HOST: ${ELASTICACHE_STAGING_HOST}
  LOGGING_LEVEL_ROOT: INFO

# Production
production:
  <<: *base
  SPRING_PROFILES_ACTIVE: prod
  SPRING_DATASOURCE_URL: ${RDS_PROD_URL}
  SPRING_REDIS_HOST: ${ELASTICACHE_PROD_HOST}
  LOGGING_LEVEL_ROOT: WARN
  LOGGING_LEVEL_COM_KITECLASS: INFO
```

---

## Kubernetes Configuration

### Namespace Structure

```
kiteclass-staging/
├── kitehub-backend
├── kiteclass-gateway
├── kiteclass-core
└── kiteclass-frontend

kiteclass-production/
├── kitehub-backend
├── kiteclass-gateway
├── kiteclass-core
└── kiteclass-frontend
```

### Deployment Manifest

```yaml
# kubernetes/base/kiteclass-core/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kiteclass-core
  labels:
    app: kiteclass-core
    version: v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: kiteclass-core
  template:
    metadata:
      labels:
        app: kiteclass-core
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8081"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: kiteclass-core
      containers:
        - name: kiteclass-core
          image: ${ECR_REGISTRY}/kiteclass-core:${IMAGE_TAG}
          ports:
            - containerPort: 8081
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "${ENVIRONMENT}"
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: kiteclass-db-secret
                  key: url
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: kiteclass-db-secret
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: kiteclass-db-secret
                  key: password
            - name: SPRING_REDIS_HOST
              valueFrom:
                configMapKeyRef:
                  name: kiteclass-config
                  key: redis-host
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: kiteclass-jwt-secret
                  key: secret
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1Gi"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 60
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: kiteclass-core
                topologyKey: kubernetes.io/hostname
```

### Service Manifest

```yaml
# kubernetes/base/kiteclass-core/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: kiteclass-core
  labels:
    app: kiteclass-core
spec:
  type: ClusterIP
  ports:
    - port: 8081
      targetPort: 8081
      protocol: TCP
      name: http
  selector:
    app: kiteclass-core
```

### Ingress Manifest

```yaml
# kubernetes/base/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kiteclass-ingress
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: ${ACM_CERTIFICATE_ARN}
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: "443"
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
spec:
  rules:
    # KiteHub API
    - host: api.kiteclass.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: kitehub-backend
                port:
                  number: 8080

    # KiteClass Instance API (wildcard)
    - host: "*.kiteclass.com"
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: kiteclass-gateway
                port:
                  number: 8080
```

### ConfigMap

```yaml
# kubernetes/overlays/staging/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: kiteclass-config
  namespace: kiteclass-staging
data:
  redis-host: "kiteclass-staging.xxxxx.cache.amazonaws.com"
  rabbitmq-host: "b-xxxxx.mq.ap-southeast-1.amazonaws.com"
  s3-bucket: "kiteclass-staging-uploads"
  cdn-domain: "staging-cdn.kiteclass.com"
```

### HorizontalPodAutoscaler

```yaml
# kubernetes/base/kiteclass-core/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: kiteclass-core-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: kiteclass-core
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
```

---

## CI/CD Pipeline (GitHub Actions)

### Build & Test Pipeline

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '20'

jobs:
  # ==================== BACKEND ====================
  backend-test:
    name: Backend Tests
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: kiteclass_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'maven'

      - name: Run tests
        working-directory: ./kiteclass/kiteclass-core
        run: ./mvnw verify -Dspring.profiles.active=test
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/kiteclass_test
          SPRING_DATASOURCE_USERNAME: test
          SPRING_DATASOURCE_PASSWORD: test

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./kiteclass/kiteclass-core/target/site/jacoco/jacoco.xml

  # ==================== FRONTEND ====================
  frontend-test:
    name: Frontend Tests
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}

      - name: Setup pnpm
        uses: pnpm/action-setup@v2
        with:
          version: 8

      - name: Install dependencies
        working-directory: ./kiteclass/kiteclass-frontend
        run: pnpm install --frozen-lockfile

      - name: Type check
        working-directory: ./kiteclass/kiteclass-frontend
        run: pnpm type-check

      - name: Lint
        working-directory: ./kiteclass/kiteclass-frontend
        run: pnpm lint

      - name: Unit tests
        working-directory: ./kiteclass/kiteclass-frontend
        run: pnpm test:coverage

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./kiteclass/kiteclass-frontend/coverage/lcov.info

  # ==================== BUILD IMAGES ====================
  build:
    name: Build Docker Images
    needs: [backend-test, frontend-test]
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-southeast-1

      - name: Login to ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Set image tag
        id: vars
        run: |
          echo "IMAGE_TAG=${GITHUB_SHA::8}" >> $GITHUB_OUTPUT
          echo "ENVIRONMENT=${{ github.ref == 'refs/heads/main' && 'production' || 'staging' }}" >> $GITHUB_OUTPUT

      - name: Build and push kiteclass-core
        uses: docker/build-push-action@v5
        with:
          context: ./kiteclass/kiteclass-core
          push: true
          tags: |
            ${{ steps.login-ecr.outputs.registry }}/kiteclass-core:${{ steps.vars.outputs.IMAGE_TAG }}
            ${{ steps.login-ecr.outputs.registry }}/kiteclass-core:latest

      - name: Build and push kiteclass-gateway
        uses: docker/build-push-action@v5
        with:
          context: ./kiteclass/kiteclass-gateway
          push: true
          tags: |
            ${{ steps.login-ecr.outputs.registry }}/kiteclass-gateway:${{ steps.vars.outputs.IMAGE_TAG }}
            ${{ steps.login-ecr.outputs.registry }}/kiteclass-gateway:latest

      - name: Build and push frontend
        uses: docker/build-push-action@v5
        with:
          context: ./kiteclass/kiteclass-frontend
          push: true
          tags: |
            ${{ steps.login-ecr.outputs.registry }}/kiteclass-frontend:${{ steps.vars.outputs.IMAGE_TAG }}
            ${{ steps.login-ecr.outputs.registry }}/kiteclass-frontend:latest
          build-args: |
            NEXT_PUBLIC_API_URL=${{ steps.vars.outputs.ENVIRONMENT == 'production' && 'https://api.kiteclass.com' || 'https://staging-api.kiteclass.com' }}

    outputs:
      image_tag: ${{ steps.vars.outputs.IMAGE_TAG }}
      environment: ${{ steps.vars.outputs.ENVIRONMENT }}
```

### Deploy Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy

on:
  workflow_run:
    workflows: ["CI Pipeline"]
    types: [completed]
    branches: [main, develop]

jobs:
  deploy-staging:
    name: Deploy to Staging
    if: ${{ github.event.workflow_run.conclusion == 'success' && github.event.workflow_run.head_branch == 'develop' }}
    runs-on: ubuntu-latest
    environment: staging

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-southeast-1

      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name kiteclass-staging --region ap-southeast-1

      - name: Deploy to EKS
        run: |
          export IMAGE_TAG=$(echo ${{ github.event.workflow_run.head_sha }} | cut -c1-8)
          envsubst < kubernetes/overlays/staging/kustomization.yaml | kubectl apply -k -

      - name: Verify deployment
        run: |
          kubectl rollout status deployment/kiteclass-core -n kiteclass-staging --timeout=300s
          kubectl rollout status deployment/kiteclass-gateway -n kiteclass-staging --timeout=300s

      - name: Notify Slack
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "Deployed to staging: ${{ github.event.workflow_run.head_sha }}"
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}

  deploy-production:
    name: Deploy to Production
    if: ${{ github.event.workflow_run.conclusion == 'success' && github.event.workflow_run.head_branch == 'main' }}
    runs-on: ubuntu-latest
    environment: production

    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-southeast-1

      - name: Update kubeconfig
        run: aws eks update-kubeconfig --name kiteclass-production --region ap-southeast-1

      - name: Deploy to EKS (Canary)
        run: |
          export IMAGE_TAG=$(echo ${{ github.event.workflow_run.head_sha }} | cut -c1-8)

          # Deploy canary (10% traffic)
          kubectl set image deployment/kiteclass-core-canary \
            kiteclass-core=${{ secrets.ECR_REGISTRY }}/kiteclass-core:$IMAGE_TAG \
            -n kiteclass-production

          kubectl rollout status deployment/kiteclass-core-canary -n kiteclass-production --timeout=300s

      - name: Health check
        run: |
          sleep 60
          # Run smoke tests against canary
          ./scripts/smoke-test.sh https://canary.kiteclass.com

      - name: Promote to stable
        run: |
          export IMAGE_TAG=$(echo ${{ github.event.workflow_run.head_sha }} | cut -c1-8)
          envsubst < kubernetes/overlays/production/kustomization.yaml | kubectl apply -k -
          kubectl rollout status deployment/kiteclass-core -n kiteclass-production --timeout=300s
```

---

## Secrets Management

### AWS Secrets Manager Structure

```
kiteclass/
├── staging/
│   ├── database           # DB credentials
│   ├── redis              # Redis auth
│   ├── jwt                # JWT secret
│   ├── external-services  # Zalo, VNPay API keys
│   └── oauth              # Google OAuth credentials
└── production/
    ├── database
    ├── redis
    ├── jwt
    ├── external-services
    └── oauth
```

### External Secrets Operator

```yaml
# kubernetes/base/external-secrets.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: kiteclass-db-secret
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: kiteclass-db-secret
    creationPolicy: Owner
  data:
    - secretKey: url
      remoteRef:
        key: kiteclass/${ENVIRONMENT}/database
        property: url
    - secretKey: username
      remoteRef:
        key: kiteclass/${ENVIRONMENT}/database
        property: username
    - secretKey: password
      remoteRef:
        key: kiteclass/${ENVIRONMENT}/database
        property: password
```

---

## Monitoring & Observability

### Stack

| Tool | Purpose |
|------|---------|
| Prometheus | Metrics collection |
| Grafana | Dashboards |
| Loki | Log aggregation |
| Jaeger | Distributed tracing |
| AlertManager | Alerting |

### Spring Boot Actuator Endpoints

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:local}
```

### Grafana Dashboard (Key Metrics)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    KiteClass Production Dashboard                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │ Request Rate │  │ Error Rate   │  │ P99 Latency  │               │
│  │   1.2k/s     │  │    0.1%      │  │    245ms     │               │
│  └──────────────┘  └──────────────┘  └──────────────┘               │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                      Request Latency (P50/P95/P99)              │ │
│  │  ═══════════════════════════════════════════════════════════   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐  │
│  │   CPU Usage by Pod          │  │   Memory Usage by Pod       │  │
│  │   ▓▓▓▓▓░░░░░ 45%           │  │   ▓▓▓▓▓▓▓░░░ 68%           │  │
│  └─────────────────────────────┘  └─────────────────────────────┘  │
│                                                                      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐  │
│  │   Database Connections      │  │   Redis Hit Rate            │  │
│  │   Active: 45/100            │  │   98.5%                      │  │
│  └─────────────────────────────┘  └─────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Alert Rules

```yaml
# prometheus/alerts.yml
groups:
  - name: kiteclass-alerts
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
          / sum(rate(http_server_requests_seconds_count[5m])) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }} (> 1%)"

      # High latency
      - alert: HighLatency
        expr: |
          histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High latency detected"
          description: "P99 latency is {{ $value }}s (> 2s)"

      # Pod not ready
      - alert: PodNotReady
        expr: |
          kube_pod_status_ready{condition="true", namespace=~"kiteclass-.*"} == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Pod not ready"
          description: "Pod {{ $labels.pod }} is not ready"

      # Database connection pool exhausted
      - alert: DBConnectionPoolExhausted
        expr: |
          hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool nearly exhausted"
          description: "{{ $value | humanizePercentage }} of connections in use"
```

---

## Cost Optimization

### Resource Sizing Guide

| Service | Staging | Production |
|---------|---------|------------|
| EKS Node | t3.medium (2) | t3.large (3-5) |
| RDS | db.t3.small | db.r6g.large (Multi-AZ) |
| ElastiCache | cache.t3.micro | cache.r6g.large (cluster) |
| S3 | Standard | Intelligent-Tiering |

### Cost-Saving Strategies

1. **Spot Instances** cho non-critical workloads
2. **Reserved Instances** cho production RDS, ElastiCache
3. **S3 Lifecycle Policies** cho old uploads
4. **Right-sizing** dựa trên CloudWatch metrics
5. **Scheduled scaling** cho off-peak hours

### Estimated Monthly Cost (USD)

| Component | Staging | Production |
|-----------|---------|------------|
| EKS Control Plane | $73 | $73 |
| EC2 Nodes | $150 | $400 |
| RDS PostgreSQL | $50 | $300 |
| ElastiCache Redis | $25 | $150 |
| ALB | $25 | $50 |
| S3 + CloudFront | $10 | $50 |
| **Total** | **~$333** | **~$1,023** |

---

## Disaster Recovery

### Backup Strategy

| Data | Backup | Retention | RTO | RPO |
|------|--------|-----------|-----|-----|
| RDS | Automated snapshots | 7 days | 1 hour | 5 min |
| S3 | Cross-region replication | Forever | 15 min | Near 0 |
| EKS | Velero backups | 30 days | 30 min | 1 hour |

### Recovery Procedures

```bash
# Restore RDS from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier kiteclass-restored \
  --db-snapshot-identifier kiteclass-snapshot-2025-01-26

# Restore EKS resources
velero restore create --from-backup kiteclass-backup-daily
```

---

## Security Checklist

### Network Security
- [ ] VPC with private subnets for data layer
- [ ] Security groups with minimal required ports
- [ ] Network policies in Kubernetes
- [ ] WAF enabled on ALB

### Application Security
- [ ] Secrets in AWS Secrets Manager (not env vars)
- [ ] TLS 1.3 everywhere
- [ ] CORS properly configured
- [ ] Rate limiting enabled

### Access Control
- [ ] IAM roles for service accounts (IRSA)
- [ ] Least privilege principle
- [ ] MFA for AWS console access
- [ ] Audit logging enabled

## Actions

### Deploy to staging
```bash
kubectl config use-context kiteclass-staging
kubectl apply -k kubernetes/overlays/staging/
```

### Check deployment status
```bash
kubectl get pods -n kiteclass-staging
kubectl logs -f deployment/kiteclass-core -n kiteclass-staging
```

### Rollback deployment
```bash
kubectl rollout undo deployment/kiteclass-core -n kiteclass-production
```

### Scale manually
```bash
kubectl scale deployment/kiteclass-core --replicas=5 -n kiteclass-production
```
