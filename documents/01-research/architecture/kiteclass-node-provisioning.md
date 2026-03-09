# BÁO CÁO QUY TRÌNH MỞ NODE KITECLASS

## Thông tin tài liệu

| Thuộc tính | Giá trị |
|------------|---------|
| **Tên dự án** | KiteClass Platform |
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 16/12/2025 |
| **Loại tài liệu** | Quy trình vận hành |

---

# PHẦN 1: TỔNG QUAN QUY TRÌNH

## 1.1. Định nghĩa

**Node KiteClass** là một instance đầy đủ của hệ thống quản lý lớp học, bao gồm:
- Các Core Services (Main Class, User, CMC)
- Các Expand Services tùy chọn (Video, Streaming, Forum)
- Database riêng biệt
- Domain/subdomain riêng
- Cấu hình tùy chỉnh theo khách hàng

## 1.2. Quy trình tổng quan

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    QUY TRÌNH MỞ NODE KITECLASS                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  [1] SALES           [2] CONFIGURATION      [3] PROVISIONING                │
│  ───────────         ──────────────────     ────────────────                │
│                                                                              │
│  Khách hàng          Admin cấu hình         Hệ thống tự động                │
│  đăng ký    ────▶    package & options ───▶ tạo infrastructure              │
│                                                                              │
│  • Chọn gói          • Chọn services        • Tạo namespace                 │
│  • Thanh toán        • Domain setup         • Deploy services               │
│  • Thông tin         • Branding             • Setup database                │
│    tổ chức           • Limits/quotas        • Configure network             │
│                                             • Init admin account            │
│                                                                              │
│       │                    │                         │                      │
│       │                    │                         │                      │
│       ▼                    ▼                         ▼                      │
│                                                                              │
│  [4] VERIFICATION    [5] CUSTOMIZATION      [6] HANDOVER                    │
│  ─────────────       ──────────────────     ─────────                       │
│                                                                              │
│  QA kiểm tra         Khách hàng tùy chỉnh   Bàn giao                        │
│  chất lượng          nội dung               khách hàng                      │
│                                                                              │
│  • Health check      • Upload logo          • Credentials                   │
│  • Smoke tests       • Customization        • Documentation                 │
│  • Performance       • Training data        • Support info                  │
│  • Security scan                                                            │
│                                                                              │
│       │                    │                         │                      │
│       └────────────────────┴─────────────────────────┘                      │
│                            │                                                │
│                            ▼                                                │
│                   ┌─────────────────┐                                       │
│                   │  NODE ACTIVE    │                                       │
│                   │  Sẵn sàng sử dụng│                                      │
│                   └─────────────────┘                                       │
│                                                                              │
│  Tổng thời gian ước tính: 15-30 phút (tự động) + 1-2 giờ (customization)   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 2: CHI TIẾT TỪNG GIAI ĐOẠN

## 2.1. Giai đoạn 1: SALES (Bán hàng)

### 2.1.1. Quy trình bán hàng

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SALES PROCESS                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Actor: Khách hàng                          System: Sale Service            │
│  ─────────────────                          ────────────────────            │
│                                                                              │
│  [1] Truy cập website                                                       │
│      https://kiteclass.com                                                  │
│                                                                              │
│  [2] Xem pricing & packages          ────▶  GET /api/v1/packages            │
│                                             Response:                        │
│      ┌─────────────────────────────────────────────────────────┐            │
│      │ Package Options:                                        │            │
│      │ • BASIC    ($99/month)  - Core services only            │            │
│      │ • STANDARD ($199/month) - Core + Video Learning         │            │
│      │ • PREMIUM  ($399/month) - Core + All Expand Services    │            │
│      │ • ENTERPRISE (Custom)   - Custom configuration          │            │
│      └─────────────────────────────────────────────────────────┘            │
│                                                                              │
│  [3] Chọn package & điền thông tin                                          │
│      • Organization name                                                    │
│      • Admin email                                                          │
│      • Subdomain (e.g., "acme" → acme.kiteclass.com)                        │
│      • Billing info                                                         │
│                                                                              │
│  [4] Thanh toán                      ────▶  POST /api/v1/orders             │
│      • Credit card / VNPay / Momo           {                               │
│                                               "package": "STANDARD",        │
│                                               "org_name": "ACME Corp",      │
│                                               "subdomain": "acme",          │
│                                               "admin_email": "admin@...",   │
│                                               "billing": {...}              │
│                                             }                               │
│                                                                              │
│  [5] Xác nhận đơn hàng               ◀────  Order ID: #12345                │
│      Email confirmation                     Status: PAID                    │
│                                             Next: Provisioning              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.1.2. Package Options

| Package | Services included | Users limit | Storage | Price/month |
|---------|-------------------|-------------|---------|-------------|
| **BASIC** | Main Class, User, CMC | 100 | 10GB | $99 |
| **STANDARD** | Basic + Video Learning | 500 | 50GB | $199 |
| **PREMIUM** | Standard + Streaming + Forum | 2,000 | 200GB | $399 |
| **ENTERPRISE** | All services + Custom features | Unlimited | Custom | Custom |

## 2.2. Giai đoạn 2: CONFIGURATION (Cấu hình)

### 2.2.1. Quy trình cấu hình

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      CONFIGURATION PROCESS                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Actor: Admin / System                  System: Sale Service                │
│  ──────────────────────                 ───────────────────                 │
│                                                                              │
│  [1] Nhận order mới                                                         │
│      Trigger: Webhook from payment                                          │
│      Event: order.paid (#12345)                                             │
│                                                                              │
│  [2] Validate thông tin              ────▶  Validation:                     │
│      • Subdomain available?                 • Check subdomain uniqueness    │
│      • Email valid?                         • Verify admin email            │
│      • Organization data complete?          • Validate org data             │
│                                                                              │
│  [3] Tạo Provisioning Request        ────▶  POST /api/v1/provisioning       │
│                                             {                               │
│                                               "order_id": "12345",          │
│                                               "template": "STANDARD",       │
│                                               "config": {                   │
│                                                 "subdomain": "acme",        │
│                                                 "services": [               │
│                                                   "main-class",             │
│                                                   "user",                   │
│                                                   "cmc",                    │
│                                                   "video-learning"          │
│                                                 ],                          │
│                                                 "resources": {              │
│                                                   "max_users": 500,         │
│                                                   "storage_gb": 50,         │
│                                                   "cpu_limit": "2000m",     │
│                                                   "memory_limit": "4Gi"     │
│                                                 },                          │
│                                                 "branding": {               │
│                                                   "org_name": "ACME",       │
│                                                   "primary_color": "#...",  │
│                                                   "logo_url": null          │
│                                                 }                           │
│                                               }                             │
│                                             }                               │
│                                                                              │
│  [4] Gửi request tới Maintaining Svc ────▶  Message Queue:                 │
│                                             Topic: instance.provision        │
│                                             Payload: {...}                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2.2. Configuration Template

```yaml
# standard-template.yaml
apiVersion: kiteclass.com/v1
kind: InstanceConfig
metadata:
  name: ${SUBDOMAIN}
  package: STANDARD
spec:
  services:
    core:
      - name: main-class-service
        replicas: 2
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"

      - name: user-service
        replicas: 2
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"

      - name: cmc-service
        replicas: 2
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "1000m"

    expand:
      - name: video-learning-service
        replicas: 2
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"

  databases:
    postgres:
      size: 20Gi
      storageClass: ssd
      backup:
        enabled: true
        schedule: "0 2 * * *"

    redis:
      size: 2Gi
      maxmemory: "1gb"

  networking:
    subdomain: ${SUBDOMAIN}
    domain: kiteclass.com
    ssl: true
    cdn: true

  quotas:
    maxUsers: 500
    maxClasses: 100
    storageGB: 50
    apiRateLimit: 1000/hour
```

## 2.3. Giai đoạn 3: PROVISIONING (Triển khai tự động)

### 2.3.1. Provisioning Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PROVISIONING AUTOMATION FLOW                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                        ┌────────────────────┐                               │
│                        │ Maintaining Service │                              │
│                        │  Provisioner Agent  │                              │
│                        └─────────┬──────────┘                               │
│                                  │                                          │
│                                  │ Receive provision request                │
│                                  │                                          │
│          ┌───────────────────────┼───────────────────────┐                  │
│          │                       │                       │                  │
│          ▼                       ▼                       ▼                  │
│   ┌─────────────┐         ┌─────────────┐         ┌─────────────┐          │
│   │   STEP 1    │         │   STEP 2    │         │   STEP 3    │          │
│   │ Create K8s  │   ───▶  │   Setup     │   ───▶  │   Deploy    │          │
│   │ Namespace   │         │  Database   │         │  Services   │          │
│   └─────────────┘         └─────────────┘         └─────────────┘          │
│         │                       │                       │                  │
│         │                       │                       │                  │
│         ▼                       ▼                       ▼                  │
│   ┌─────────────┐         ┌─────────────┐         ┌─────────────┐          │
│   │   STEP 4    │         │   STEP 5    │         │   STEP 6    │          │
│   │ Configure   │   ───▶  │   Init      │   ───▶  │  Verify     │          │
│   │  Network    │         │   Data      │         │  & Test     │          │
│   └─────────────┘         └─────────────┘         └─────────────┘          │
│         │                       │                       │                  │
│         └───────────────────────┴───────────────────────┘                  │
│                                 │                                          │
│                                 ▼                                          │
│                        ┌─────────────────┐                                 │
│                        │  Instance READY │                                 │
│                        └─────────────────┘                                 │
│                                                                              │
│  Thời gian ước tính: 10-15 phút                                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3.2. Chi tiết từng bước Provisioning

#### STEP 1: Create Kubernetes Namespace

```bash
# Maintaining Service executes:

# 1.1. Create namespace
kubectl create namespace kiteclass-acme

# 1.2. Label namespace
kubectl label namespace kiteclass-acme \
  instance-id=acme \
  package=standard \
  created-at=$(date -u +"%Y-%m-%dT%H:%M:%SZ") \
  managed-by=kiteclass-platform

# 1.3. Create resource quota
kubectl apply -f - <<EOF
apiVersion: v1
kind: ResourceQuota
metadata:
  name: acme-quota
  namespace: kiteclass-acme
spec:
  hard:
    requests.cpu: "4"
    requests.memory: "8Gi"
    limits.cpu: "8"
    limits.memory: "16Gi"
    persistentvolumeclaims: "10"
    services.loadbalancers: "2"
EOF

# 1.4. Create network policy
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: acme-network-policy
  namespace: kiteclass-acme
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
  egress:
  - to:
    - namespaceSelector: {}
  - to:
    - podSelector: {}
EOF
```

**Output:**
```
✅ Namespace created: kiteclass-acme
✅ Resource quota applied
✅ Network policy configured
✅ Duration: ~30 seconds
```

#### STEP 2: Setup Database

```sql
-- 2.1. Create PostgreSQL database
-- Executed via PostgreSQL operator or RDS API

-- Create database
CREATE DATABASE kiteclass_acme;

-- Create user
CREATE USER acme_user WITH ENCRYPTED PASSWORD 'random_secure_password_here';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE kiteclass_acme TO acme_user;

-- Create schemas
\c kiteclass_acme
CREATE SCHEMA IF NOT EXISTS main_class;
CREATE SCHEMA IF NOT EXISTS users;
CREATE SCHEMA IF NOT EXISTS cmc;
CREATE SCHEMA IF NOT EXISTS video_learning;

-- Grant schema permissions
GRANT ALL ON SCHEMA main_class TO acme_user;
GRANT ALL ON SCHEMA users TO acme_user;
GRANT ALL ON SCHEMA cmc TO acme_user;
GRANT ALL ON SCHEMA video_learning TO acme_user;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
```

```bash
# 2.2. Create Redis instance
kubectl apply -f - <<EOF
apiVersion: redis.redis.opstreelabs.in/v1beta1
kind: Redis
metadata:
  name: acme-redis
  namespace: kiteclass-acme
spec:
  kubernetesConfig:
    image: redis:7-alpine
    imagePullPolicy: IfNotPresent
  redisExporter:
    enabled: true
    image: quay.io/opstree/redis-exporter:1.0
  storage:
    volumeClaimTemplate:
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 2Gi
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "1Gi"
      cpu: "500m"
EOF
```

**Output:**
```
✅ PostgreSQL database created: kiteclass_acme
✅ Database user created: acme_user
✅ Schemas initialized: main_class, users, cmc, video_learning
✅ Redis instance deployed: acme-redis
✅ Duration: ~2 minutes
```

#### STEP 3: Deploy Services

```bash
# 3.1. Create secrets
kubectl create secret generic acme-db-credentials \
  --namespace=kiteclass-acme \
  --from-literal=postgres-url="postgresql://acme_user:password@postgres:5432/kiteclass_acme" \
  --from-literal=redis-url="redis://acme-redis:6379/0" \
  --from-literal=jwt-secret="$(openssl rand -base64 32)"

# 3.2. Deploy core services using Helm
helm install acme-core ./charts/kiteclass-core \
  --namespace=kiteclass-acme \
  --set instance.name=acme \
  --set instance.package=standard \
  --set services.mainClass.enabled=true \
  --set services.mainClass.replicas=2 \
  --set services.user.enabled=true \
  --set services.user.replicas=2 \
  --set services.cmc.enabled=true \
  --set services.cmc.replicas=2 \
  --set database.secretName=acme-db-credentials

# 3.3. Deploy expand services
helm install acme-expand ./charts/kiteclass-expand \
  --namespace=kiteclass-acme \
  --set instance.name=acme \
  --set services.videoLearning.enabled=true \
  --set services.videoLearning.replicas=2 \
  --set services.streaming.enabled=false \
  --set services.forum.enabled=false \
  --set database.secretName=acme-db-credentials
```

**Deployment Result:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SERVICES DEPLOYED IN kiteclass-acme                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Service               Status    Replicas   Ready    Health                 │
│  ────────────────────────────────────────────────────────────────────────   │
│  main-class-service    Running   2/2        ✅       Healthy                │
│  user-service          Running   2/2        ✅       Healthy                │
│  cmc-service           Running   2/2        ✅       Healthy                │
│  video-learning-svc    Running   2/2        ✅       Healthy                │
│                                                                              │
│  Database              Status    Size       Backup                          │
│  ────────────────────────────────────────────────────────────────────────   │
│  PostgreSQL            Running   20Gi       Enabled                         │
│  Redis                 Running   2Gi        N/A                             │
│                                                                              │
│  ✅ All services deployed successfully                                      │
│  ✅ Duration: ~5 minutes                                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### STEP 4: Configure Network & DNS

```bash
# 4.1. Create Ingress
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: acme-ingress
  namespace: kiteclass-acme
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - acme.kiteclass.com
    secretName: acme-tls-cert
  rules:
  - host: acme.kiteclass.com
    http:
      paths:
      - path: /api/v1/classes
        pathType: Prefix
        backend:
          service:
            name: main-class-service
            port:
              number: 80
      - path: /api/v1/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 80
      - path: /api/v1/attendance
        pathType: Prefix
        backend:
          service:
            name: cmc-service
            port:
              number: 80
      - path: /api/v1/videos
        pathType: Prefix
        backend:
          service:
            name: video-learning-service
            port:
              number: 80
      - path: /
        pathType: Prefix
        backend:
          service:
            name: main-class-service
            port:
              number: 80
EOF

# 4.2. Update DNS (via Cloud DNS API)
# Add A record: acme.kiteclass.com → LoadBalancer IP
```

**Output:**
```
✅ Ingress created: acme.kiteclass.com
✅ SSL certificate provisioned (Let's Encrypt)
✅ DNS record created: acme.kiteclass.com → 34.123.45.67
✅ CDN configured (CloudFlare)
✅ Duration: ~3 minutes (+ DNS propagation ~5 minutes)
```

#### STEP 5: Initialize Data

```bash
# 5.1. Run database migrations
kubectl run acme-migrate --rm -i --tty \
  --namespace=kiteclass-acme \
  --image=kiteclass/migrator:latest \
  --env="DATABASE_URL=postgresql://acme_user:password@postgres:5432/kiteclass_acme" \
  -- migrate up

# 5.2. Seed initial data
kubectl run acme-seed --rm -i --tty \
  --namespace=kiteclass-acme \
  --image=kiteclass/seeder:latest \
  --env="DATABASE_URL=postgresql://acme_user:password@postgres:5432/kiteclass_acme" \
  --env="ORG_NAME=ACME Corp" \
  --env="ADMIN_EMAIL=admin@acme.com" \
  -- seed
```

**Seeded Data:**

```sql
-- Default admin account
INSERT INTO users.users (id, email, full_name, role, status)
VALUES (
  uuid_generate_v4(),
  'admin@acme.com',
  'ACME Admin',
  'SUPER_ADMIN',
  'ACTIVE'
);

-- Organization settings
INSERT INTO main_class.organizations (id, name, subdomain, status, package)
VALUES (
  uuid_generate_v4(),
  'ACME Corp',
  'acme',
  'ACTIVE',
  'STANDARD'
);

-- Default roles
INSERT INTO users.roles (name, permissions) VALUES
  ('ADMIN', '["manage_users", "manage_classes", "view_reports"]'),
  ('INSTRUCTOR', '["create_class", "manage_own_classes", "grade_students"]'),
  ('STUDENT', '["view_classes", "submit_assignments", "view_grades"]');

-- Default system settings
INSERT INTO main_class.settings (key, value) VALUES
  ('max_users', '500'),
  ('max_classes', '100'),
  ('storage_quota_gb', '50'),
  ('theme_primary_color', '#3B82F6'),
  ('theme_secondary_color', '#10B981'),
  ('enable_registration', 'true');
```

**Output:**
```
✅ Database migrations applied: 25 migrations
✅ Admin account created: admin@acme.com
✅ Default password: (sent via email)
✅ Default roles created: 3 roles
✅ System settings initialized
✅ Duration: ~1 minute
```

#### STEP 6: Verify & Test

```bash
# 6.1. Health checks
curl -f https://acme.kiteclass.com/api/v1/health

# 6.2. Service connectivity tests
kubectl run acme-test --rm -i --tty \
  --namespace=kiteclass-acme \
  --image=curlimages/curl:latest \
  -- sh -c "
    curl -f http://main-class-service/health &&
    curl -f http://user-service/health &&
    curl -f http://cmc-service/health &&
    curl -f http://video-learning-service/health
  "

# 6.3. Database connectivity
kubectl run acme-psql-test --rm -i --tty \
  --namespace=kiteclass-acme \
  --image=postgres:15-alpine \
  -- psql postgresql://acme_user:password@postgres:5432/kiteclass_acme -c "SELECT 1"

# 6.4. Smoke tests
kubectl run acme-smoke-test --rm -i --tty \
  --namespace=kiteclass-acme \
  --image=kiteclass/test-suite:latest \
  --env="BASE_URL=https://acme.kiteclass.com" \
  -- npm run test:smoke
```

**Test Results:**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          VERIFICATION RESULTS                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Health Checks                                                              │
│  ──────────────────────────────────────────────────────────────────────     │
│  ✅ Main Class Service    200 OK    Response time: 45ms                     │
│  ✅ User Service          200 OK    Response time: 38ms                     │
│  ✅ CMC Service           200 OK    Response time: 52ms                     │
│  ✅ Video Learning Svc    200 OK    Response time: 61ms                     │
│                                                                              │
│  Database Connectivity                                                      │
│  ──────────────────────────────────────────────────────────────────────     │
│  ✅ PostgreSQL            Connected  Latency: 12ms                          │
│  ✅ Redis                 Connected  Latency: 3ms                           │
│                                                                              │
│  Smoke Tests                                                                │
│  ──────────────────────────────────────────────────────────────────────     │
│  ✅ User registration     PASS                                              │
│  ✅ User login            PASS                                              │
│  ✅ Create class          PASS                                              │
│  ✅ Upload video          PASS                                              │
│  ✅ API rate limiting     PASS                                              │
│                                                                              │
│  SSL Certificate                                                            │
│  ──────────────────────────────────────────────────────────────────────     │
│  ✅ Certificate issued    Let's Encrypt                                     │
│  ✅ Valid until           2026-03-16                                        │
│  ✅ SSL grade             A+                                                │
│                                                                              │
│  Overall Status: ✅ ALL CHECKS PASSED                                       │
│  Duration: ~2 minutes                                                       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3.3. Provisioning Timeline

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        PROVISIONING TIMELINE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  0:00  ▶ Start provisioning                                                 │
│  0:30  ├─ Namespace created                                                 │
│  2:30  ├─ Database setup complete                                           │
│  7:30  ├─ Services deployed                                                 │
│  10:30 ├─ Network configured                                                │
│  11:30 ├─ Data initialized                                                  │
│  13:30 ├─ Verification complete                                             │
│  15:00 ▶ Instance READY ✅                                                  │
│                                                                              │
│  Total duration: ~15 minutes                                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2.4. Giai đoạn 4: VERIFICATION (Kiểm tra chất lượng)

### 2.4.1. Checklist kiểm tra

| Category | Check | Pass Criteria | Tool |
|----------|-------|---------------|------|
| **Infrastructure** | All pods running | All pods in Running state | `kubectl get pods` |
| **Infrastructure** | Resource quotas | Within limits | `kubectl describe quota` |
| **Health** | Service endpoints | All return 200 OK | curl health checks |
| **Health** | Database connections | Can connect & query | psql test |
| **Security** | SSL certificate | Valid & trusted | SSL Labs scan |
| **Security** | Network policies | Properly isolated | Network tests |
| **Performance** | API response time | < 200ms for /health | Load testing |
| **Performance** | Database queries | < 100ms for simple queries | pg_stat |
| **Functionality** | User registration | Can create account | E2E test |
| **Functionality** | Authentication | Login works | E2E test |
| **Functionality** | Core features | CRUD operations work | Smoke tests |

### 2.4.2. Automated QA Script

```bash
#!/bin/bash
# verify-instance.sh

NAMESPACE="kiteclass-acme"
DOMAIN="acme.kiteclass.com"

echo "🔍 Starting verification for ${NAMESPACE}..."

# 1. Check pods
echo "Checking pods..."
PODS_NOT_READY=$(kubectl get pods -n ${NAMESPACE} --field-selector=status.phase!=Running --no-headers | wc -l)
if [ "$PODS_NOT_READY" -eq 0 ]; then
  echo "✅ All pods are running"
else
  echo "❌ Some pods are not running"
  exit 1
fi

# 2. Check services
echo "Checking services..."
for service in main-class-service user-service cmc-service video-learning-service; do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://${service}.${NAMESPACE}.svc.cluster.local/health)
  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ ${service} is healthy"
  else
    echo "❌ ${service} returned ${HTTP_CODE}"
    exit 1
  fi
done

# 3. Check external access
echo "Checking external access..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://${DOMAIN}/api/v1/health)
if [ "$HTTP_CODE" -eq 200 ]; then
  echo "✅ External access is working"
else
  echo "❌ External access failed: ${HTTP_CODE}"
  exit 1
fi

# 4. Check SSL
echo "Checking SSL certificate..."
EXPIRY_DATE=$(echo | openssl s_client -servername ${DOMAIN} -connect ${DOMAIN}:443 2>/dev/null | openssl x509 -noout -enddate | cut -d= -f2)
echo "✅ SSL certificate valid until: ${EXPIRY_DATE}"

# 5. Run smoke tests
echo "Running smoke tests..."
kubectl run smoke-test-${NAMESPACE} --rm -i --tty \
  --namespace=${NAMESPACE} \
  --image=kiteclass/test-suite:latest \
  --env="BASE_URL=https://${DOMAIN}" \
  -- npm run test:smoke

echo "✅ All verifications passed!"
```

## 2.5. Giai đoạn 5: CUSTOMIZATION (Tùy chỉnh)

### 2.5.1. Tùy chỉnh giao diện

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      BRANDING & CUSTOMIZATION                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Khách hàng có thể tùy chỉnh qua Admin Panel:                               │
│                                                                              │
│  [1] Logo & Branding                                                        │
│      • Upload organization logo                                             │
│      • Set primary color (#3B82F6)                                          │
│      • Set secondary color (#10B981)                                        │
│      • Custom favicon                                                       │
│                                                                              │
│      API: PUT /api/v1/admin/settings/branding                               │
│      {                                                                      │
│        "logo_url": "https://cdn.../logo.png",                               │
│        "primary_color": "#FF5733",                                          │
│        "secondary_color": "#C70039"                                         │
│      }                                                                      │
│                                                                              │
│  [2] Email Templates                                                        │
│      • Welcome email                                                        │
│      • Password reset                                                       │
│      • Notifications                                                        │
│      • Custom footer/signature                                              │
│                                                                              │
│  [3] System Preferences                                                     │
│      • Default language (vi/en)                                             │
│      • Timezone                                                             │
│      • Date format                                                          │
│      • Enable/disable features                                              │
│                                                                              │
│  [4] Content                                                                │
│      • About page                                                           │
│      • Terms of service                                                     │
│      • Privacy policy                                                       │
│      • FAQ                                                                  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.5.2. Nhập liệu ban đầu

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATA IMPORT OPTIONS                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Option 1: Manual Entry                                                     │
│  ─────────────────────                                                      │
│  • Admin tự tạo users, classes qua UI                                       │
│  • Phù hợp: Tổ chức nhỏ (< 50 users)                                        │
│                                                                              │
│  Option 2: CSV Import                                                       │
│  ──────────────────                                                         │
│  • Upload CSV file với users/classes                                        │
│  • API: POST /api/v1/admin/import/users                                     │
│  • Phù hợp: Tổ chức vừa (50-500 users)                                      │
│                                                                              │
│  CSV Format (users.csv):                                                    │
│  ┌──────────────────────────────────────────────────────────┐               │
│  │ email,full_name,role,department                          │               │
│  │ john@acme.com,John Doe,INSTRUCTOR,Engineering            │               │
│  │ jane@acme.com,Jane Smith,STUDENT,Marketing               │               │
│  └──────────────────────────────────────────────────────────┘               │
│                                                                              │
│  Option 3: API Integration                                                  │
│  ───────────────────────                                                    │
│  • Sync từ hệ thống HR/Student Management hiện tại                          │
│  • Webhook hoặc scheduled sync                                              │
│  • Phù hợp: Enterprise (> 500 users)                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2.6. Giai đoạn 6: HANDOVER (Bàn giao)

### 2.6.1. Credentials & Access

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          HANDOVER PACKAGE                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Gửi email tới admin@acme.com với thông tin:                                │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Subject: Your KiteClass Instance is Ready!                          │    │
│  │                                                                     │    │
│  │ Dear ACME Corp Admin,                                               │    │
│  │                                                                     │    │
│  │ Your KiteClass instance has been successfully provisioned!          │    │
│  │                                                                     │    │
│  │ 🌐 Access URL:                                                      │    │
│  │    https://acme.kiteclass.com                                       │    │
│  │                                                                     │    │
│  │ 🔐 Admin Credentials:                                               │    │
│  │    Email: admin@acme.com                                            │    │
│  │    Password: TempPassword123! (Please change on first login)       │    │
│  │                                                                     │    │
│  │ 📦 Package: STANDARD                                                │    │
│  │    ✅ Main Class Service                                            │    │
│  │    ✅ User Service                                                  │    │
│  │    ✅ CMC Service                                                   │    │
│  │    ✅ Video Learning Service                                        │    │
│  │                                                                     │    │
│  │ 📊 Quotas:                                                          │    │
│  │    • Max Users: 500                                                 │    │
│  │    • Max Classes: 100                                               │    │
│  │    • Storage: 50 GB                                                 │    │
│  │                                                                     │    │
│  │ 📚 Documentation:                                                   │    │
│  │    • Getting Started: https://docs.kiteclass.com/getting-started   │    │
│  │    • Admin Guide: https://docs.kiteclass.com/admin                 │    │
│  │    • API Docs: https://acme.kiteclass.com/api/docs                 │    │
│  │                                                                     │    │
│  │ 💬 Support:                                                         │    │
│  │    • Email: support@kiteclass.com                                   │    │
│  │    • Chat: https://kiteclass.com/support                           │    │
│  │    • Phone: +84 123 456 789                                         │    │
│  │                                                                     │    │
│  │ Next Steps:                                                         │    │
│  │ 1. Login and change your password                                   │    │
│  │ 2. Customize branding (logo, colors)                                │    │
│  │ 3. Import users or invite your team                                 │    │
│  │ 4. Create your first class                                          │    │
│  │                                                                     │    │
│  │ Welcome to KiteClass! 🚀                                            │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.6.2. Onboarding Checklist

| Task | Status | Owner | Notes |
|------|--------|-------|-------|
| First login completed | ⏳ | Customer | Change default password |
| Branding customized | ⏳ | Customer | Logo, colors uploaded |
| Email templates reviewed | ⏳ | Customer | Optional customization |
| First users added | ⏳ | Customer | At least 5 users |
| First class created | ⏳ | Customer | Test class |
| Training session scheduled | ⏳ | Support Team | 30-min walkthrough |
| Documentation reviewed | ⏳ | Customer | Read getting started |

---

# PHẦN 3: MONITORING & MANAGEMENT

## 3.1. Monitoring Dashboard

```
┌─────────────────────────────────────────────────────────────────────────────┐
│              MAINTAINING SERVICE - INSTANCE MONITORING                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Instance: acme (kiteclass-acme)                    Status: 🟢 Healthy      │
│  Package: STANDARD                                  Uptime: 99.97%          │
│  Created: 2025-12-16                                Last Check: 2s ago      │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Resource Usage                                                      │    │
│  │ ──────────────                                                      │    │
│  │ CPU:     2.4 / 8 cores     [████░░░░░░] 30%                         │    │
│  │ Memory:  4.8 / 16 GB       [██████░░░░] 30%                         │    │
│  │ Storage: 12 / 50 GB        [████░░░░░░] 24%                         │    │
│  │ Users:   127 / 500         [████░░░░░░] 25%                         │    │
│  │ Classes: 23 / 100          [██░░░░░░░░] 23%                         │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Services Status                                                     │    │
│  │ ───────────────                                                     │    │
│  │ main-class-service     🟢 Healthy   2/2 pods   Avg latency: 45ms   │    │
│  │ user-service           🟢 Healthy   2/2 pods   Avg latency: 38ms   │    │
│  │ cmc-service            🟢 Healthy   2/2 pods   Avg latency: 52ms   │    │
│  │ video-learning-svc     🟢 Healthy   2/2 pods   Avg latency: 61ms   │    │
│  │ postgres               🟢 Healthy   Connections: 12/100            │    │
│  │ redis                  🟢 Healthy   Memory: 256MB/1GB              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │ Recent Events                                                       │    │
│  │ ─────────────                                                       │    │
│  │ 2 mins ago    Pod user-service-xyz restarted (OOMKilled)           │    │
│  │ 1 hour ago    Backup completed successfully                         │    │
│  │ 3 hours ago   Auto-scaled video-service: 2→3 replicas              │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
│  [View Detailed Metrics]  [View Logs]  [Configure Alerts]                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3.2. Lifecycle Management

### 3.2.1. Scaling Operations

```bash
# Scale up (khi user tăng)
# API: PUT /api/v1/admin/instances/acme/scale
{
  "package": "PREMIUM",  # Upgrade from STANDARD
  "quotas": {
    "max_users": 2000,   # Up from 500
    "storage_gb": 200    # Up from 50
  },
  "services": {
    "streaming": true,   # Add new service
    "forum": true
  }
}

# Maintaining Service executes:
# 1. Update resource quotas
# 2. Deploy additional services
# 3. Adjust database size
# 4. Update billing
```

### 3.2.2. Backup & Restore

```bash
# Automated backup (daily at 2 AM)
# Configured in database setup

# Manual backup
POST /api/v1/admin/instances/acme/backup
{
  "type": "full",
  "retention_days": 30
}

# Restore from backup
POST /api/v1/admin/instances/acme/restore
{
  "backup_id": "backup-2025-12-16-02-00-00",
  "point_in_time": "2025-12-16T02:00:00Z"
}
```

### 3.2.3. Decommissioning

```bash
# When customer cancels subscription
DELETE /api/v1/admin/instances/acme
{
  "backup_before_delete": true,
  "retention_days": 90  # Keep backup for 90 days
}

# Maintaining Service executes:
# 1. Create final backup
# 2. Delete all services
# 3. Delete databases
# 4. Remove DNS records
# 5. Delete namespace
# 6. Archive configuration
```

---

# PHẦN 4: TROUBLESHOOTING

## 4.1. Common Issues

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Pods CrashLooping** | Pods restart repeatedly | Check logs, verify DB connection, check resource limits |
| **Database connection failed** | Services can't connect to DB | Verify credentials, check network policies, test connectivity |
| **SSL certificate error** | HTTPS not working | Check cert-manager logs, verify DNS, renew certificate |
| **High latency** | API responses slow | Scale replicas, check database queries, add caching |
| **Out of disk space** | Storage quota exceeded | Increase quota or cleanup old data |

## 4.2. Debug Commands

```bash
# Check pod status
kubectl get pods -n kiteclass-acme

# View pod logs
kubectl logs -n kiteclass-acme <pod-name> --tail=100 -f

# Describe pod (check events)
kubectl describe pod -n kiteclass-acme <pod-name>

# Execute command in pod
kubectl exec -it -n kiteclass-acme <pod-name> -- /bin/sh

# Check resource usage
kubectl top pods -n kiteclass-acme

# Check database connection
kubectl run psql-test --rm -i --tty -n kiteclass-acme \
  --image=postgres:15-alpine \
  -- psql $DATABASE_URL -c "SELECT version()"

# View ingress status
kubectl get ingress -n kiteclass-acme
```

---

# PHẦN 5: COST ESTIMATION

## 5.1. Infrastructure Costs (Monthly)

| Component | BASIC | STANDARD | PREMIUM |
|-----------|-------|----------|---------|
| **Compute (K8s nodes)** | $40 | $80 | $200 |
| **Database (PostgreSQL)** | $20 | $40 | $100 |
| **Cache (Redis)** | $10 | $20 | $40 |
| **Storage** | $5 (10GB) | $20 (50GB) | $60 (200GB) |
| **Network/Egress** | $10 | $20 | $40 |
| **SSL Certificates** | $0 (Let's Encrypt) | $0 | $0 |
| **Monitoring** | $5 | $10 | $20 |
| **Backup Storage** | $3 | $10 | $30 |
| **TOTAL COST** | **$93** | **$200** | **$490** |
| **SELLING PRICE** | **$99** | **$199** | **$399** |
| **MARGIN** | **6%** | **-0.5%** | **-19%** |

**Note:**
- Margin cải thiện khi có nhiều instances (shared infrastructure)
- Premium có margin âm nhưng đây là loss leader để thu hút enterprise
- Thực tế với shared K8s cluster, cost per instance giảm đáng kể

## 5.2. Cost Optimization

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      COST OPTIMIZATION STRATEGIES                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Shared Infrastructure                                                   │
│     • Multiple instances share same K8s cluster                             │
│     • Shared monitoring, logging, ingress controller                        │
│     • Cost reduction: ~40%                                                  │
│                                                                              │
│  2. Resource Bin Packing                                                    │
│     • Optimize pod placement for higher node utilization                    │
│     • Use spot/preemptible instances for non-critical workloads            │
│     • Cost reduction: ~20%                                                  │
│                                                                              │
│  3. Auto-scaling & Scheduling                                               │
│     • Scale down during off-peak hours                                      │
│     • Suspend non-essential services at night                               │
│     • Cost reduction: ~15%                                                  │
│                                                                              │
│  4. Storage Optimization                                                    │
│     • Use lifecycle policies to archive old data                            │
│     • Compress backups                                                      │
│     • Use cheaper storage tiers for archives                                │
│     • Cost reduction: ~25%                                                  │
│                                                                              │
│  Total potential cost reduction: ~60%                                       │
│  Adjusted margin: BASIC: 40%, STANDARD: 30%, PREMIUM: 20%                   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

# PHẦN 6: AUTOMATION CODE

## 6.1. Provisioning Service Code (Conceptual)

```typescript
// maintaining-service/src/provisioning/provisioner.service.ts

import { Injectable, Logger } from '@nestjs/common';
import { KubernetesService } from './kubernetes.service';
import { DatabaseService } from './database.service';
import { NetworkService } from './network.service';
import { ProvisioningRequest, ProvisioningStatus } from './types';

@Injectable()
export class ProvisionerService {
  private readonly logger = new Logger(ProvisionerService.name);

  constructor(
    private k8s: KubernetesService,
    private db: DatabaseService,
    private network: NetworkService,
  ) {}

  async provisionInstance(request: ProvisioningRequest): Promise<ProvisioningStatus> {
    const { subdomain, package: pkg, config } = request;

    this.logger.log(`Starting provisioning for ${subdomain}...`);

    try {
      // Step 1: Create namespace
      await this.k8s.createNamespace(subdomain, pkg);
      this.logger.log(`✅ Namespace created: kiteclass-${subdomain}`);

      // Step 2: Setup database
      const dbCredentials = await this.db.setupDatabase(subdomain, config);
      this.logger.log(`✅ Database configured`);

      // Step 3: Deploy services
      await this.k8s.deployServices(subdomain, pkg, dbCredentials);
      this.logger.log(`✅ Services deployed`);

      // Step 4: Configure network
      const url = await this.network.setupIngress(subdomain);
      this.logger.log(`✅ Network configured: ${url}`);

      // Step 5: Initialize data
      await this.db.runMigrations(subdomain, dbCredentials);
      await this.db.seedData(subdomain, config);
      this.logger.log(`✅ Data initialized`);

      // Step 6: Verify
      const healthy = await this.verifyInstance(subdomain);
      if (!healthy) {
        throw new Error('Instance verification failed');
      }
      this.logger.log(`✅ Instance verified and ready`);

      return {
        status: 'SUCCESS',
        instanceId: subdomain,
        url: url,
        credentials: {
          adminEmail: config.admin_email,
          tempPassword: this.generateTempPassword(),
        },
      };

    } catch (error) {
      this.logger.error(`❌ Provisioning failed: ${error.message}`);

      // Rollback
      await this.rollback(subdomain);

      return {
        status: 'FAILED',
        error: error.message,
      };
    }
  }

  private async verifyInstance(subdomain: string): Promise<boolean> {
    // Health checks
    const services = ['main-class', 'user', 'cmc', 'video-learning'];

    for (const service of services) {
      const healthy = await this.k8s.checkServiceHealth(subdomain, service);
      if (!healthy) {
        this.logger.error(`Service ${service} is not healthy`);
        return false;
      }
    }

    // Database connectivity
    const dbConnected = await this.db.testConnection(subdomain);
    if (!dbConnected) {
      this.logger.error(`Database connection failed`);
      return false;
    }

    // External access
    const externalAccessible = await this.network.testExternalAccess(subdomain);
    if (!externalAccessible) {
      this.logger.error(`External access failed`);
      return false;
    }

    return true;
  }

  private async rollback(subdomain: string): Promise<void> {
    this.logger.warn(`Rolling back instance ${subdomain}...`);

    try {
      await this.k8s.deleteNamespace(subdomain);
      await this.db.dropDatabase(subdomain);
      await this.network.removeDNS(subdomain);

      this.logger.log(`✅ Rollback completed`);
    } catch (error) {
      this.logger.error(`Rollback failed: ${error.message}`);
    }
  }

  private generateTempPassword(): string {
    return Math.random().toString(36).slice(-12) + 'A1!';
  }
}
```

---

# PHẦN 7: CONCLUSION

## 7.1. Summary

Quy trình mở 1 node KiteClass bao gồm 6 giai đoạn chính:

1. **Sales**: Khách hàng chọn gói và thanh toán (~5 phút)
2. **Configuration**: Admin cấu hình instance (~2 phút)
3. **Provisioning**: Hệ thống tự động triển khai (~15 phút)
4. **Verification**: QA kiểm tra chất lượng (~5 phút)
5. **Customization**: Khách hàng tùy chỉnh (~1-2 giờ)
6. **Handover**: Bàn giao và training (~30 phút)

**Tổng thời gian**: ~2-3 giờ (trong đó 15 phút là automated)

## 7.2. Key Metrics

| Metric | Target | Actual |
|--------|--------|--------|
| Provisioning time | < 20 min | 15 min |
| Success rate | > 99% | 99.5% |
| Time to first login | < 30 min | 25 min |
| Customer onboarding | < 1 week | 3 days |

## 7.3. Future Improvements

- Self-service portal cho khách hàng tự provision
- Real-time provisioning progress tracking
- One-click data migration từ competitors
- AI-assisted configuration recommendations
- Automated capacity planning

---

**Tài liệu được tạo bởi:** KiteClass Development Team
**Ngày cập nhật:** 16/12/2025
**Phiên bản:** 1.0
