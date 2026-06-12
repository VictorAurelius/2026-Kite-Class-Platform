# KiteHub Infrastructure Design

**Version:** 1.1
**Created:** 2026-03-09
**Updated:** 2026-03-19
**Purpose:** Define infrastructure architecture for KiteHub platform services
**Status:** Design phase (before implementation)

> **Note (2026-05-07 update):** Production deployment locked **AWS Singapore Free Tier Architecture B** per [ADR-025](../../02-architecture/adr/ADR-025-aws-singapore-free-tier-architecture.md). Oracle Cloud path từ 2026-03-19 archived (signup reject rate ~50% VN). Phase 1 BETA = single EC2 + docker-compose; Phase 1.5+ = EKS migration per [`documents/05-guides/deploy/aws-architecture-sizing-matrix.md`](../../05-guides/deploy/aws-architecture-sizing-matrix.md).
> Single-source deploy sequence: [`release-1-deploy-runbook.md`](../roadmap/release-1-deploy-runbook.md)
> Oracle artifacts archived: [`documents/07-archived/oracle-deploy-2026/`](../../07-archived/oracle-deploy-2026/)
> Tài liệu này giữ nguyên thiết kế Kubernetes/AWS gốc — relevant lại cho Phase 1.5+ EKS migration.

---

## Table of Contents

1. [Overview](#overview)
2. [Service Discovery](#service-discovery)
3. [Database Strategy](#database-strategy)
4. [API Gateway Routing](#api-gateway-routing)
5. [Deployment Architecture](#deployment-architecture)
6. [Monitoring Stack](#monitoring-stack)

---

## Overview

KiteHub là platform-level service layer quản lý multi-tenant SaaS infrastructure cho KiteClass. Infrastructure design này cover 3 deployment environments:

- **Local Development**: Docker Compose
- **Staging**: Kubernetes (single replica)
- **Production**: Kubernetes (auto-scaling)

**Key Requirements:**
- Service-to-service communication
- Multi-tenant database isolation
- API routing and load balancing
- Health checks and auto-recovery
- Horizontal scalability

---

## Service Discovery

### Options Evaluated

#### Option A: Spring Cloud Eureka
**Pros:**
- Native Spring Cloud integration
- Dynamic service registration
- Client-side load balancing

**Cons:**
- Heavy resource footprint (~500MB per Eureka server)
- Requires separate Eureka service
- Overkill for small service count (6 services)

#### Option B: HashiCorp Consul
**Pros:**
- Service mesh capabilities
- Built-in health checks
- Key-value store

**Cons:**
- Requires HashiCorp stack learning curve
- Additional infrastructure complexity
- Not needed for current scale

#### Option C: Kubernetes DNS (Production) + Hardcoded URLs (Local)
**Pros:**
- ✅ Lightweight (no extra services)
- ✅ Built into Kubernetes
- ✅ Simple for local development
- ✅ Production-ready

**Cons:**
- Requires different config for local vs production

### Decision: Kubernetes DNS + Environment-Based Config

**Local Development (docker-compose):**
```yaml
# Service URLs hardcoded in environment variables
SUBSCRIPTION_SERVICE_URL=http://kitehub-subscription:8080
PAYMENT_SERVICE_URL=http://kitehub-payment:8080
BRANDING_SERVICE_URL=http://kitehub-branding:8080
```

**Production (Kubernetes):**
```yaml
# Kubernetes Service DNS
SUBSCRIPTION_SERVICE_URL=http://kitehub-subscription.kitehub.svc.cluster.local:8080
PAYMENT_SERVICE_URL=http://kitehub-payment.kitehub.svc.cluster.local:8080
BRANDING_SERVICE_URL=http://kitehub-branding.kitehub.svc.cluster.local:8080
```

**Spring Cloud Gateway Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: subscription-service
          uri: ${SUBSCRIPTION_SERVICE_URL}
          predicates:
            - Path=/api/v1/instances/**, /api/v1/subscriptions/**
          filters:
            - StripPrefix=0
```

**Benefits:**
- No service discovery overhead in local dev
- Kubernetes handles DNS resolution in production
- Easy to switch between environments with env vars

---

## Database Strategy

### Multi-Tenant Metadata Database

**Purpose:** Store platform-level metadata shared across all tenants

**Database:** `kitehub` (PostgreSQL 15)

**Tables:**
```sql
-- Instance metadata (tenant registration)
CREATE TABLE instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(63) UNIQUE NOT NULL,
    database_url TEXT NOT NULL,
    database_username VARCHAR(255) NOT NULL,
    database_password TEXT NOT NULL, -- Encrypted
    status VARCHAR(50) NOT NULL,     -- PROVISIONING, ACTIVE, SUSPENDED, DELETED
    trial_ends_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Subscription tracking
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY,
    instance_id UUID REFERENCES instances(id),
    plan VARCHAR(50) NOT NULL,       -- TRIAL, BASIC, PROFESSIONAL
    status VARCHAR(50) NOT NULL,     -- ACTIVE, EXPIRED, CANCELLED
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Payment transactions
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    instance_id UUID REFERENCES instances(id),
    amount BIGINT NOT NULL,          -- Amount in VND (cents)
    currency VARCHAR(3) DEFAULT 'VND',
    payment_method VARCHAR(50),      -- VIETQR, BANK_TRANSFER
    status VARCHAR(50) NOT NULL,     -- PENDING, COMPLETED, FAILED
    transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

-- AI branding job queue
CREATE TABLE branding_jobs (
    id UUID PRIMARY KEY,
    instance_id UUID REFERENCES instances(id),
    job_type VARCHAR(50) NOT NULL,   -- LOGO, BANNER, COLOR_PALETTE
    prompt TEXT,
    status VARCHAR(50) NOT NULL,     -- QUEUED, PROCESSING, COMPLETED, FAILED
    result_url TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Email logs
CREATE TABLE email_logs (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(50) NOT NULL,     -- SENT, FAILED, BOUNCED
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Instance Databases (Database-per-Tenant)

**Pattern:** Separate PostgreSQL database for each KiteClass instance

**Naming Convention:** `kiteclass_{instance_id_short}`

Example:
- Instance ID: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Database name: `kiteclass_a1b2c3d4`

**Why Database-per-Tenant:**
1. **Complete Isolation**: Instance A cannot physically access Instance B's data
2. **Independent Scaling**: Each instance can be scaled independently
3. **Backup/Restore**: Restore single instance without affecting others
4. **Compliance**: Easier to handle data residency requirements
5. **Security**: No risk of cross-tenant data leaks

**Schema:** Same as KiteClass Core (students, teachers, courses, classes, etc.)

**Provisioning:** Handled by KiteHub Subscription Service (PR 4.2)

---

## API Gateway Routing

### KiteHub Gateway (Port 9000)

**Purpose:** Route requests to KiteHub platform services

```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
            allowedHeaders: "*"
      routes:
        # Platform Management APIs
        - id: subscription-service
          uri: ${SUBSCRIPTION_SERVICE_URL}
          predicates:
            - Path=/api/v1/instances/**, /api/v1/subscriptions/**, /api/v1/trials/**
          filters:
            - StripPrefix=0
            - name: CircuitBreaker
              args:
                name: subscriptionCircuitBreaker
                fallbackUri: forward:/fallback/subscription

        - id: payment-service
          uri: ${PAYMENT_SERVICE_URL}
          predicates:
            - Path=/api/v1/payments/**
          filters:
            - StripPrefix=0
            - name: CircuitBreaker
              args:
                name: paymentCircuitBreaker
                fallbackUri: forward:/fallback/payment

        # AI Branding APIs
        - id: branding-service
          uri: ${BRANDING_SERVICE_URL}
          predicates:
            - Path=/api/v1/branding/**
          filters:
            - StripPrefix=0
            - name: CircuitBreaker
              args:
                name: brandingCircuitBreaker
                fallbackUri: forward:/fallback/branding

        # Email Service APIs
        - id: email-service
          uri: ${EMAIL_SERVICE_URL}
          predicates:
            - Path=/api/v1/emails/**
          filters:
            - StripPrefix=0

        # Admin APIs
        - id: admin-service
          uri: ${ADMIN_SERVICE_URL}
          predicates:
            - Path=/api/v1/admin/**
          filters:
            - StripPrefix=0
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # requests per second
                redis-rate-limiter.burstCapacity: 20
```

### KiteClass Instance Routing (Subdomain-Based)

**Pattern:** Route by subdomain to specific instance

Example:
- `abc123.kitehub.me` → Instance with subdomain "abc123"
- `xyz789.kitehub.me` → Instance with subdomain "xyz789"

**Implementation:**
```yaml
# In KiteHub Gateway
- id: instance-core
  uri: http://kiteclass-core-{instance-id}:8080
  predicates:
    - Host={subdomain}.kitehub.me
    - Path=/api/v1/**
  filters:
    - ResolveInstanceId  # Custom filter: extract instance from subdomain
    - TenantFilter       # Inject X-Tenant-Id header
```

**Custom Filter - ResolveInstanceId:**
```java
@Component
public class ResolveInstanceIdGatewayFilterFactory
    extends AbstractGatewayFilterFactory<ResolveInstanceIdGatewayFilterFactory.Config> {

    @Autowired
    private InstanceRepository instanceRepository;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String host = exchange.getRequest().getHeaders().getFirst("Host");
            String subdomain = extractSubdomain(host); // "abc123" from "abc123.kitehub.me"

            Instance instance = instanceRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new InstanceNotFoundException(subdomain));

            // Add instance ID to request headers
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-Instance-Id", instance.getId().toString())
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }
}
```

---

## Deployment Architecture

### Local Development (Docker Compose)

**File:** `kitehub/docker-compose.kitehub.yml`

**Architecture:**
```
┌─────────────────────────────────────────┐
│  Developer Laptop                       │
├─────────────────────────────────────────┤
│                                         │
│  Docker Compose:                        │
│  - kitehub-postgres (5433)              │
│  - kitehub-redis (6380)                 │
│  - kitehub-rabbitmq (5673, 15673)       │
│                                         │
│  IntelliJ/VSCode:                       │
│  - kitehub-subscription (run locally)   │
│  - kitehub-payment (run locally)        │
│  - kitehub-branding (run locally)       │
│                                         │
└─────────────────────────────────────────┘
```

**Start Infrastructure:**
```bash
docker-compose -f docker-compose.kitehub.yml up -d kitehub-postgres kitehub-redis kitehub-rabbitmq
```

**Run Services in IDE:**
- Configure Spring Boot run configuration
- Set environment variables pointing to localhost:5433, localhost:6380, etc.

**Benefits:**
- Fast iteration (no rebuild for code changes)
- Easy debugging (breakpoints work)
- Lower resource usage (only run services you're working on)

---

### Staging Environment (Kubernetes)

**Cluster:** AWS EKS or GKE
**Namespace:** `kitehub-staging`
**Purpose:** Integration testing before production

**Resource Allocation (per service):**
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

**Replicas:** 1 per service (single instance for cost savings)

**Kubernetes Services:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kitehub-subscription
  namespace: kitehub-staging
spec:
  selector:
    app: kitehub-subscription
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: ClusterIP
```

**Database:**
- CloudSQL (managed PostgreSQL) or RDS
- Connection via private IP (no public access)

**Ingress (NGINX):**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: kitehub-ingress
  namespace: kitehub-staging
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  tls:
    - hosts:
        - staging.kitehub.me
      secretName: kiteclass-staging-tls
  rules:
    - host: staging.kitehub.me
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: kitehub-gateway
                port:
                  number: 9000
```

---

### Production Environment (Kubernetes)

**Cluster:** AWS EKS (Multi-AZ)
**Namespace:** `kitehub-production`
**Purpose:** Live production traffic

**High Availability:**
- Minimum 3 replicas per service
- Deployed across 3 availability zones
- Auto-scaling based on CPU/memory

**Resource Allocation (per service):**
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "1000m"
  limits:
    memory: "2Gi"
    cpu: "2000m"
```

**Horizontal Pod Autoscaler:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: kitehub-subscription-hpa
  namespace: kitehub-production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: kitehub-subscription
  minReplicas: 3
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
```

**Database:**
- CloudSQL (High Availability - Multi-AZ)
- Read replicas for analytics queries
- Automated backups (daily)

**Load Balancer:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kitehub-gateway
  namespace: kitehub-production
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
spec:
  type: LoadBalancer
  selector:
    app: kitehub-gateway
  ports:
    - protocol: TCP
      port: 443
      targetPort: 9000
```

**Health Checks:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 3
```

---

## Monitoring Stack

**Status:** Deferred to Tier 3 (before production)

**Planned Components:**
- Prometheus (metrics collection)
- Grafana (dashboards)
- ELK Stack or CloudWatch (log aggregation)
- OpenTelemetry (distributed tracing)
- PagerDuty (alerting)

**See:** `documents/03-planning/infrastructure/monitoring-observability.md` (Tier 3)

---

## Service Communication Patterns

### Synchronous (REST)

**Use Case:** Gateway → Backend Services

**Example:**
```java
// Gateway calls Subscription Service
@Autowired
private WebClient.Builder webClientBuilder;

public Mono<Instance> getInstanceBySubdomain(String subdomain) {
    return webClientBuilder.build()
        .get()
        .uri(subscriptionServiceUrl + "/api/v1/instances/subdomain/" + subdomain)
        .retrieve()
        .bodyToMono(Instance.class);
}
```

**Circuit Breaker (Resilience4j):**
```java
@CircuitBreaker(name = "subscriptionService", fallbackMethod = "getInstanceFallback")
public Mono<Instance> getInstanceBySubdomain(String subdomain) {
    // ... call service
}

public Mono<Instance> getInstanceFallback(String subdomain, Exception ex) {
    log.error("Failed to fetch instance, using fallback", ex);
    return Mono.error(new ServiceUnavailableException("Subscription service is down"));
}
```

### Asynchronous (RabbitMQ)

**Use Case:** Subscription Service → Email Service (send welcome email)

**Publisher (Subscription Service):**
```java
@Autowired
private RabbitTemplate rabbitTemplate;

public void sendWelcomeEmail(Instance instance) {
    WelcomeEmailEvent event = new WelcomeEmailEvent(
        instance.getOwnerEmail(),
        instance.getSubdomain()
    );
    rabbitTemplate.convertAndSend("email.exchange", "email.welcome", event);
}
```

**Consumer (Email Service):**
```java
@RabbitListener(queues = "email.welcome.queue")
public void handleWelcomeEmail(WelcomeEmailEvent event) {
    emailService.sendWelcomeEmail(event.getEmail(), event.getSubdomain());
}
```

**Benefits:**
- Decoupled services (Email service can be down without blocking signup)
- Retry mechanism (RabbitMQ redelivers failed messages)
- Scalability (multiple consumers can process queue in parallel)

---

## Network Security

### Local Development
- All services in same Docker network: `kitehub-network`
- No authentication required (trusted environment)

### Staging/Production
- **Service-to-Service:** mTLS (Mutual TLS) via Istio service mesh
- **External API:** HTTPS/TLS 1.3 only
- **Database:** SSL mode=require (encrypted connections)

**Kubernetes NetworkPolicy:**
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: kitehub-network-policy
  namespace: kitehub-production
spec:
  podSelector:
    matchLabels:
      app: kitehub-subscription
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: kitehub-gateway
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: kitehub-postgres
      ports:
        - protocol: TCP
          port: 5432
```

**Effect:** Only Gateway can call Subscription Service, Subscription can only call Postgres

---

## Cost Estimation

### Staging Environment (Monthly)
- EKS Cluster: $72/month (control plane)
- 6 services × 512Mi RAM × $0.05/GB-hour: ~$22/month
- CloudSQL (db-f1-micro): $15/month
- **Total:** ~$110/month

### Production Environment (Monthly - 100 instances)
- EKS Cluster: $72/month
- 6 services × 3 replicas × 1GB RAM × $0.05/GB-hour: ~$200/month
- CloudSQL (db-n1-standard-2): $150/month
- Load Balancer: $20/month
- S3 Storage (branding assets): $50/month
- **Total:** ~$500/month

**Scaling:** At 1000 instances, cost increases to ~$1500/month (mainly database and compute)

---

## Disaster Recovery

### Backup Strategy
- **Platform Database (kitehub):** Daily automated snapshots (CloudSQL)
- **Instance Databases:** Daily pg_dump → S3
- **Configuration:** GitOps (all Kubernetes manifests in Git)

### Recovery Time Objective (RTO)
- **Platform Services:** 15 minutes (redeploy from Git)
- **Database:** 30 minutes (restore from snapshot)
- **Total RTO:** 1 hour

### Recovery Point Objective (RPO)
- **Platform Database:** 1 hour (point-in-time recovery)
- **Instance Databases:** 24 hours (daily backups)

---

## Next Steps

1. **Implement PR 4.1** (Subscription Service)
   - Connect to `kitehub-postgres`
   - Implement instance provisioning API
   - Test with local docker-compose

2. **Create Kubernetes manifests** (during PR 4.13 - Gateway)
   - Deployment YAMLs for all services
   - Service YAMLs
   - Ingress configuration

3. **Setup Staging Environment**
   - Create EKS cluster
   - Deploy all KiteHub services
   - Integration testing

4. **Production Deployment** (after full testing)
   - Setup CloudSQL High Availability
   - Configure auto-scaling
   - Enable monitoring (Tier 3)

---

**Related Documentation:**
- [Database Provisioning Design](./kitehub-database-provisioning.md) (Task 2.2)
- [Security Design](../04-quality/security-design.md) (Task 2.4)
- [KiteHub Implementation Plan](./kitehub-implementation-plan.md)
- [Docker Compose Template](../../kitehub/docker-compose.kitehub.yml)

---

**Last Updated:** 2026-03-09
**Status:** Design complete, ready for implementation
