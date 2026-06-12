# KiteHub Infrastructure Design

**Created:** 2026-03-10
**Version:** 1.0
**Purpose:** Document KiteHub platform infrastructure including service discovery, database strategy, API gateway routing, and deployment architecture.

---

## Table of Contents

1. [Service Discovery](#1-service-discovery)
2. [Database Strategy](#2-database-strategy)
3. [API Gateway Routing](#3-api-gateway-routing)
4. [Deployment Architecture](#4-deployment-architecture)
5. [Rate Limiting & Circuit Breakers](#5-rate-limiting--circuit-breakers)
6. [Monitoring & Observability](#6-monitoring--observability)

---

## 1. Service Discovery

### 1.1. Options Evaluated

**Considered Approaches:**
1. **Spring Cloud Eureka** - Service registry with client-side load balancing
   - ❌ Heavy (requires separate Eureka server)
   - ❌ Additional operational complexity
   - ❌ Overkill for Docker Compose / Kubernetes environments

2. **HashiCorp Consul** - Service mesh with health checking
   - ❌ Requires Consul agent on each service
   - ❌ Additional infrastructure components
   - ❌ More suitable for multi-cloud/hybrid setups

3. **Kubernetes DNS** (Native) - Built-in DNS-based service discovery
   - ✅ Lightweight (no additional components)
   - ✅ Production-ready
   - ✅ Automatic load balancing via Kubernetes Services
   - ✅ Zero configuration overhead

**Decision:** Use **Kubernetes DNS** for production, hardcoded URLs for local development.

---

### 1.2. Service Discovery Configuration

#### Local Development (docker-compose)

**Network:** `kitehub-network` (bridge network)

**Service Names:**
```yaml
services:
  kitehub-gateway:
    networks:
      - kitehub-network
    ports:
      - "9000:9000"

  kitehub-subscription:
    networks:
      - kitehub-network
    ports:
      - "8081:8080"

  kitehub-branding:
    networks:
      - kitehub-network
    ports:
      - "8082:8080"
```

**Gateway Configuration (local):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: platform-subscription
          uri: http://kitehub-subscription:8080  # Docker Compose service name
          predicates:
            - Path=/api/platform/subscriptions/**
```

**How it works:**
- Docker Compose creates DNS entries for each service name
- Services communicate using service names (e.g., `kitehub-subscription:8080`)
- No load balancing (single instance per service)

---

#### Production (Kubernetes)

**Namespace:** `kitehub` (for platform services)

**Kubernetes Service Resources:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: kitehub-subscription
  namespace: kitehub
spec:
  selector:
    app: kitehub-subscription
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
  type: ClusterIP
```

**DNS Names (Kubernetes):**
- Short name (within same namespace): `kitehub-subscription`
- FQDN (cross-namespace): `kitehub-subscription.kitehub.svc.cluster.local`
- Port: `8080`

**Gateway Configuration (production):**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: platform-subscription
          uri: http://kitehub-subscription:8080  # Kubernetes Service DNS
          predicates:
            - Path=/api/platform/subscriptions/**
```

**How it works:**
- Kubernetes Service provides stable DNS name
- kube-proxy handles load balancing to pods
- Automatic failover if pod dies (Deployment controller recreates)
- No code changes needed between local/production (same service names)

---

## 2. Database Strategy

### 2.1. Multi-Tenant Metadata (Shared Database)

**Database Name:** `kitehub`
**Purpose:** Store platform-level data (instances, subscriptions, branding jobs, payments)

**Schema:**
```sql
CREATE DATABASE kitehub;

-- Platform tables
CREATE SCHEMA platform;

CREATE TABLE platform.instances (
    id UUID PRIMARY KEY,
    subdomain VARCHAR(100) UNIQUE NOT NULL,
    custom_domain VARCHAR(255) UNIQUE,
    organization_name VARCHAR(255) NOT NULL,
    subscription_tier VARCHAR(50) NOT NULL,  -- FREE, BASIC, PREMIUM, ENTERPRISE
    status VARCHAR(50) NOT NULL,              -- ACTIVE, SUSPENDED, TRIAL_EXPIRED
    database_url VARCHAR(500),                -- Instance database connection
    database_username VARCHAR(100),
    database_password TEXT,                   -- Encrypted with AES-256
    trial_start_date TIMESTAMP,
    trial_end_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE platform.subscriptions (
    id UUID PRIMARY KEY,
    instance_id UUID REFERENCES platform.instances(id),
    tier VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,              -- ACTIVE, CANCELLED, PAST_DUE
    billing_cycle VARCHAR(50),                -- MONTHLY, YEARLY
    amount DECIMAL(10,2),
    next_billing_date DATE,
    auto_renew BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE platform.branding_jobs (
    id UUID PRIMARY KEY,
    instance_id UUID REFERENCES platform.instances(id),
    job_type VARCHAR(50) NOT NULL,            -- LOGO_GENERATION, LANDING_PAGE
    status VARCHAR(50) NOT NULL,              -- QUEUED, PROCESSING, COMPLETED, FAILED
    input_data JSONB,
    output_data JSONB,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE platform.payments (
    id UUID PRIMARY KEY,
    instance_id UUID REFERENCES platform.instances(id),
    subscription_id UUID REFERENCES platform.subscriptions(id),
    amount DECIMAL(10,2),
    currency VARCHAR(10) DEFAULT 'VND',
    payment_method VARCHAR(50),               -- VIETQR, MOMO, STRIPE
    transaction_id VARCHAR(255) UNIQUE,
    status VARCHAR(50),                       -- PENDING, COMPLETED, FAILED
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Shared Across:**
- All KiteHub microservices (Subscription, Branding, Admin, Email)
- Single source of truth for instance metadata
- Centralized subscription and payment tracking

---

### 2.2. Instance Databases (Database-per-Tenant)

**Strategy:** Each KiteClass instance gets its own PostgreSQL database for complete data isolation.

**Database Naming:** `kiteclass_{instance_id_short}`

**Example:**
- Instance ID: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
- Database name: `kiteclass_a1b2c3d4`
- Database user: `kiteclass_a1b2c3d4_user`

**Provisioning Workflow:**
1. User signs up → Instance record created in `kitehub.platform.instances`
2. DatabaseProvisioningService creates new PostgreSQL database
3. Flyway runs migrations (students, teachers, courses, etc.)
4. Database credentials encrypted and stored in `instances` table
5. KiteClass Core Service connects to instance-specific database

**Benefits:**
- **Complete isolation**: Instance A cannot access Instance B's data (physically impossible)
- **Security**: Database breach affects only 1 customer
- **Compliance**: Easier to meet data residency requirements (can place DB in specific region)
- **Performance**: No query overhead from multi-tenant filters
- **Scaling**: Can move instance to different database server independently

**Challenges:**
- **Connection pooling**: HikariCP manages N connection pools (1 per instance)
- **Database management**: More databases to monitor and backup
- **Cost**: Higher than shared database (but acceptable for SaaS model)

See [Database Provisioning Design](./kitehub-database-provisioning.md) for detailed implementation.

---

## 3. API Gateway Routing

### 3.1. Spring Cloud Gateway Architecture

**Gateway Service:** `kitehub-gateway`
**Port:** 9000
**Technology:** Spring Cloud Gateway (Reactive, built on Project Reactor)

**Key Responsibilities:**
- Route requests to appropriate microservice
- Multi-tenant routing (subdomain → instance)
- Rate limiting by subscription tier
- Circuit breaker for fault tolerance
- CORS handling
- Response header manipulation

---

### 3.2. Gateway Routes Configuration

**File:** `kitehub-gateway/src/main/resources/application.yml`

#### Platform APIs (KiteHub Services)

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Subscription Management
        - id: platform-subscription
          uri: http://kitehub-subscription:8080
          predicates:
            - Path=/api/platform/subscriptions/**
          filters:
            - name: CircuitBreaker
              args:
                name: subscriptionCircuitBreaker
                fallbackUri: forward:/fallback/subscription

        # Payment Processing
        - id: platform-payment
          uri: http://kitehub-subscription:8080
          predicates:
            - Path=/api/platform/payments/**
          filters:
            - name: CircuitBreaker
              args:
                name: paymentCircuitBreaker
                fallbackUri: forward:/fallback/payment

        # AI Branding Service
        - id: platform-branding
          uri: http://kitehub-branding:8080
          predicates:
            - Path=/api/platform/branding/**
          filters:
            - name: CircuitBreaker
              args:
                name: brandingCircuitBreaker
                fallbackUri: forward:/fallback/branding

        # Admin Management
        - id: platform-admin
          uri: http://kitehub-admin:8080
          predicates:
            - Path=/api/platform/admin/**
          filters:
            - name: CircuitBreaker
              args:
                name: adminCircuitBreaker
                fallbackUri: forward:/fallback/admin

        # Email Service
        - id: platform-email
          uri: http://kitehub-email:8080
          predicates:
            - Path=/api/platform/email/**
          filters:
            - name: CircuitBreaker
              args:
                name: emailCircuitBreaker
                fallbackUri: forward:/fallback/email
```

**Routing Pattern:**
- URL: `/api/platform/{service}/**`
- Service name matches route ID
- Example: `POST /api/platform/subscriptions/create` → `kitehub-subscription:8080`

---

#### Instance APIs (KiteClass Multi-Tenant Routing)

```yaml
        # Instance APIs - Route to KiteClass instances
        # Requires TenantResolverFilter to extract subdomain
        - id: instance-apis
          uri: http://kiteclass-gateway:8080
          predicates:
            - Path=/api/v1/**
          filters:
            - TenantResolver  # Custom filter
            - name: CircuitBreaker
              args:
                name: instanceCircuitBreaker
                fallbackUri: forward:/fallback/instance
```

**TenantResolverFilter Workflow:**

**File:** `kitehub-gateway/.../filter/TenantResolverFilter.java`

```java
@Component
public class TenantResolverFilter extends AbstractGatewayFilterFactory<Config> {

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 1. Extract subdomain from Host header
            String host = exchange.getRequest().getURI().getHost();
            String subdomain = extractSubdomain(host);  // "customer1" from "customer1.kitehub.me"

            // 2. Lookup instance in database
            Optional<Instance> instance = instanceRepository.findBySubdomain(subdomain);
            if (instance.isEmpty()) {
                // Try custom domain
                instance = instanceRepository.findByCustomDomain(host);
            }

            if (instance.isEmpty()) {
                return respondWithError(exchange, HttpStatus.NOT_FOUND, "Instance not found");
            }

            // 3. Verify instance is ACTIVE
            if (!InstanceStatus.ACTIVE.equals(instance.get().getStatus())) {
                return respondWithError(exchange, HttpStatus.SERVICE_UNAVAILABLE,
                    "Instance is " + instance.get().getStatus().name().toLowerCase());
            }

            // 4. Add X-Tenant-Id header for downstream services
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("X-Tenant-Id", instance.get().getId().toString())
                .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private String extractSubdomain(String host) {
        // "customer1.kitehub.me" → "customer1"
        if (!host.endsWith(".kitehub.me")) {
            return host;  // Custom domain or localhost
        }
        return host.substring(0, host.indexOf(".kitehub.me"));
    }
}
```

**Request Flow:**
```
1. Browser → https://customer1.kitehub.me/api/v1/students

2. KiteHub Gateway (port 9000) receives request
   - Host: customer1.kitehub.me
   - Path: /api/v1/students

3. TenantResolverFilter executes:
   - Extracts subdomain: "customer1"
   - Queries DB: SELECT * FROM instances WHERE subdomain = 'customer1'
   - Verifies status = ACTIVE
   - Adds header: X-Tenant-Id: a1b2c3d4-e5f6-7890-abcd-ef1234567890

4. Gateway routes to KiteClass Gateway:
   - URL: http://kiteclass-gateway:8080/api/v1/students
   - Headers: X-Tenant-Id = a1b2c3d4-...

5. KiteClass Gateway (instance):
   - Reads X-Tenant-Id header
   - Sets TenantContext for Hibernate filters
   - Routes to Core Service

6. Core Service:
   - Hibernate filters automatically add WHERE instance_id = 'a1b2c3d4-...'
   - Returns only data for customer1's instance
```

---

### 3.3. Custom Domain Support

**Feature:** Customers can use their own domain (e.g., `school.edu.vn` instead of `school.kitehub.me`)

**Configuration:**
1. Customer configures DNS CNAME: `school.edu.vn` → `kitehub.me`
2. Admin updates instance record: `UPDATE instances SET custom_domain = 'school.edu.vn' WHERE id = ...`
3. TenantResolverFilter checks both `findBySubdomain()` and `findByCustomDomain()`

**SSL/TLS:**
- Let's Encrypt wildcard certificate for `*.kitehub.me`
- Customer domains require separate certificates (manual or AWS Certificate Manager)

---

## 4. Deployment Architecture

### 4.1. Local Development (Docker Compose)

**File:** `docker-compose.kitehub.yml`

**Components:**
```yaml
services:
  # Infrastructure
  kitehub-postgres:      # Port 5433
  kitehub-redis:         # Port 6380
  kitehub-rabbitmq:      # Ports 5673, 15673
  kitehub-minio:         # Ports 9100, 9191 (S3-compatible storage)

  # KiteHub Services
  kitehub-gateway:       # Port 9000
  kitehub-subscription:  # Port 8081 → internal 8080
  kitehub-branding:      # Port 8082 → internal 8080
  kitehub-admin:         # Port 8083 → internal 8080
  kitehub-email:         # Port 8084 → internal 8080
```

**Network:** `kitehub-network` (bridge)

**Port Mapping Strategy:**
- External ports: 9000, 8081-8085 (avoid conflicts with KiteClass services)
- Internal ports: Always 8080 for services, 9000 for gateway
- Infrastructure: 5433 (Postgres), 6380 (Redis), 5673/15673 (RabbitMQ), 9100/9191 (MinIO)

**Usage:**
```bash
cd kitehub
docker-compose -f docker-compose.kitehub.yml up -d

# Check health
curl http://localhost:9000/actuator/health
curl http://localhost:9191/minio/health/live  # MinIO health
```

See `kitehub/README.md` for detailed setup instructions.

---

### 4.2. Staging Environment (Kubernetes)

**Namespace:** `kitehub-staging`

**Resource Allocation:**
```yaml
# Each service
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

**Replicas:**
- Gateway: 2 replicas (for load balancing)
- Other services: 1 replica (cost optimization)

**Database:**
- AWS RDS PostgreSQL (db.t4g.small)
- Multi-AZ for high availability
- Automated backups (7-day retention)

**Redis:**
- AWS ElastiCache Redis (cache.t4g.micro)
- Single node (cost optimization)

**Ingress:**
- AWS Application Load Balancer (ALB)
- SSL/TLS termination with ACM certificates
- Path-based routing to Gateway service

---

### 4.3. Production Environment (Kubernetes)

**Namespace:** `kitehub` (platform services)

**Deployment Topology:**

```
┌─────────────────────────────────────────────────────────────┐
│                    AWS EKS Cluster                          │
│                                                             │
│  ┌───────────────────────────────────────────────────┐     │
│  │  Namespace: kitehub                               │     │
│  │  ┌─────────────────────────────────────────────┐  │     │
│  │  │  kitehub-gateway (3 replicas)               │  │     │
│  │  │  - Port 9000                                │  │     │
│  │  │  - Rate limiting, routing, circuit breaker  │  │     │
│  │  └─────────────────────────────────────────────┘  │     │
│  │  ┌─────────────────────────────────────────────┐  │     │
│  │  │  kitehub-subscription (2 replicas)          │  │     │
│  │  │  - Subscription CRUD, payment processing    │  │     │
│  │  └─────────────────────────────────────────────┘  │     │
│  │  ┌─────────────────────────────────────────────┐  │     │
│  │  │  kitehub-branding (2 replicas)              │  │     │
│  │  │  - OpenAI integration, asset generation     │  │     │
│  │  └─────────────────────────────────────────────┘  │     │
│  │  ┌─────────────────────────────────────────────┐  │     │
│  │  │  kitehub-admin (1 replica)                  │  │     │
│  │  │  - Admin portal, monitoring                 │  │     │
│  │  └─────────────────────────────────────────────┘  │     │
│  │  ┌─────────────────────────────────────────────┐  │     │
│  │  │  kitehub-email (2 replicas)                 │  │     │
│  │  │  - AWS SES integration, templates           │  │     │
│  │  └─────────────────────────────────────────────┘  │     │
│  └───────────────────────────────────────────────────┘     │
│                                                             │
│  ┌───────────────────────────────────────────────────┐     │
│  │  Namespace: kiteclass-{instance-id-1}             │     │
│  │  - kiteclass-gateway (2 replicas)                 │     │
│  │  - kiteclass-core (2 replicas)                    │     │
│  │  - kiteclass-frontend (2 replicas)                │     │
│  └───────────────────────────────────────────────────┘     │
│                                                             │
│  ┌───────────────────────────────────────────────────┐     │
│  │  Namespace: kiteclass-{instance-id-N}             │     │
│  │  - kiteclass-gateway (2 replicas)                 │     │
│  │  - kiteclass-core (2 replicas)                    │     │
│  │  - kiteclass-frontend (2 replicas)                │     │
│  └───────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

**Resource Allocation (Production):**

| Service | Replicas | Memory Request | Memory Limit | CPU Request | CPU Limit |
|---------|----------|----------------|--------------|-------------|-----------|
| **Gateway** | 3 | 512Mi | 1Gi | 500m | 1000m |
| **Subscription** | 2 | 512Mi | 1Gi | 500m | 1000m |
| **Branding** | 2 | 1Gi | 2Gi | 1000m | 2000m |
| **Admin** | 1 | 256Mi | 512Mi | 250m | 500m |
| **Email** | 2 | 256Mi | 512Mi | 250m | 500m |

**Instance Services** (per instance):

| Service | Replicas | Memory | CPU |
|---------|----------|--------|-----|
| **Instance Gateway** | 2 | 512Mi | 500m |
| **Instance Core** | 2 | 1Gi | 1000m |
| **Instance Frontend** | 2 | 256Mi | 250m |

**Auto-Scaling:**
- Horizontal Pod Autoscaler (HPA)
- Target CPU: 70%
- Min replicas: As configured above
- Max replicas: 10 (Gateway), 5 (other services)

**Database (Production):**
- KiteHub: AWS RDS PostgreSQL (db.r6g.large, Multi-AZ)
- Instances: Aurora PostgreSQL Serverless v2 (0.5-4 ACU per instance)

**Redis (Production):**
- AWS ElastiCache Redis (cache.r6g.large)
- Cluster mode enabled (3 shards, 1 replica each)
- Automatic failover

---

### 4.4. Instance Provisioning Workflow

**Template-Based Deployment:**

**Files:** `infrastructure/k8s/kiteclass-template/*.yaml`

**Templates:**
- `namespace.yaml` - Instance namespace (kiteclass-{{INSTANCE_ID}})
- `secrets.yaml` - Database credentials, API keys
- `configmap.yaml` - Environment-specific config
- `core-deployment.yaml` - Core service deployment
- `gateway-deployment.yaml` - Instance gateway deployment
- `frontend-deployment.yaml` - Frontend deployment
- `ingress.yaml` - Subdomain routing

**Provisioning Steps:**
1. User signs up → Instance record created
2. DatabaseProvisioningService creates PostgreSQL database
3. KubernetesService replaces template placeholders:
   ```bash
   sed -e "s/{{INSTANCE_ID}}/$INSTANCE_ID/g" \
       -e "s/{{ORGANIZATION_NAME}}/$ORG_NAME/g" \
       -e "s/{{SUBSCRIPTION_TIER}}/$TIER/g" \
       namespace.yaml | kubectl apply -f -
   ```
4. Apply all manifests (namespace, secrets, deployments, ingress)
5. Wait for health checks to pass
6. Update instance status to ACTIVE

**Result:**
- New Kubernetes namespace: `kiteclass-a1b2c3d4`
- 3 services running (gateway, core, frontend)
- Subdomain accessible: `https://customer1.kitehub.me`

---

## 5. Rate Limiting & Circuit Breakers

### 5.1. Rate Limiting by Subscription Tier

**Configuration:**
```yaml
kitehub:
  rate-limit:
    default-limit: 100
    limits:
      FREE: 100        # requests per minute
      BASIC: 500
      PREMIUM: 2000
      ENTERPRISE: 10000
```

**Implementation:**
- Uses Redis for distributed rate limiting
- Key format: `rate-limit:{instance-id}:{minute}`
- Sliding window algorithm
- Returns `429 Too Many Requests` when exceeded

**Headers:**
```
X-RateLimit-Limit: 500
X-RateLimit-Remaining: 487
X-RateLimit-Reset: 1678890000
```

---

### 5.2. Circuit Breakers (Resilience4j)

**Configuration:**
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10           # Last 10 requests
        minimumNumberOfCalls: 5         # Min calls before evaluating
        failureRateThreshold: 50        # Open circuit if 50% fail
        waitDurationInOpenState: 10s    # Wait before trying half-open
        permittedNumberOfCallsInHalfOpenState: 3
```

**Circuit States:**
1. **CLOSED** - Normal operation, all requests pass through
2. **OPEN** - Too many failures, reject all requests immediately (return fallback)
3. **HALF_OPEN** - Testing if service recovered (allow limited requests)

**Fallback Endpoints:**
```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/subscription")
    public ResponseEntity<Map<String, String>> subscriptionFallback() {
        return ResponseEntity.status(503)
            .body(Map.of("error", "Subscription service is temporarily unavailable"));
    }

    @GetMapping("/branding")
    public ResponseEntity<Map<String, String>> brandingFallback() {
        return ResponseEntity.status(503)
            .body(Map.of("error", "Branding service is temporarily unavailable"));
    }
}
```

---

## 6. Monitoring & Observability

**Status:** To be designed in Tier 3 (DEFERRED)

**Planned Components:**
- Prometheus for metrics collection
- Grafana for dashboards
- ELK Stack or CloudWatch Logs for log aggregation
- OpenTelemetry for distributed tracing
- PagerDuty for alerting

See future documentation: `documents/03-planning/infrastructure/monitoring-observability.md`

---

## Summary

### Key Decisions

1. **Service Discovery:** Kubernetes DNS (production) + hardcoded URLs (local)
   - Rationale: Simple, lightweight, no additional infrastructure

2. **Database Strategy:** Database-per-tenant + shared KiteHub metadata
   - Rationale: Complete isolation, security, compliance, independent scaling

3. **API Gateway:** Spring Cloud Gateway with reactive programming
   - Rationale: High performance, native Spring Boot integration, circuit breakers

4. **Multi-Tenant Routing:** Subdomain extraction + instance lookup
   - Rationale: Clean URLs, custom domain support, flexible

5. **Deployment:** Kubernetes namespaces per instance
   - Rationale: Isolation, resource quotas, independent lifecycle

### Production Readiness Checklist

**Infrastructure:**
- ✅ Gateway routing configured
- ✅ TenantResolverFilter implemented
- ✅ Circuit breakers configured
- ✅ Rate limiting by tier
- ✅ Kubernetes manifests created
- ❌ Production database provisioning (see Task 2.2)
- ❌ Monitoring & alerting (see Tier 3)

**Security:**
- ✅ Multi-tenant isolation (database + Hibernate filters)
- ✅ Instance status verification
- ❌ Secrets encryption (see Security Design doc)
- ❌ Audit logging (see Security Design doc)

**Operations:**
- ✅ Health check endpoints
- ✅ Auto-scaling configuration
- ❌ Runbooks (see Tier 3)
- ❌ Incident response plan (see Tier 3)

---

**Last Updated:** 2026-03-10
**Author:** Architecture Team
**Status:** Draft v1.0 (awaiting review)
