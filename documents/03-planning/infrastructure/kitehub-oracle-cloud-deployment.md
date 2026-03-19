# KiteHub Production: Oracle Cloud Always Free (Primary)

**Ngày tạo**: 2026-03-19
**Mục tiêu**: Deploy KiteHub platform trên Oracle Cloud Always Free ($0/tháng)
**Backup**: AWS (Terraform/Helm giữ nguyên, dùng khi Oracle không khả dụng)

---

## 1. Strategy: Dual-Cloud

```
┌─────────────────────────────────────────────────────┐
│                    KiteHub Platform                  │
│                                                     │
│  PRIMARY: Oracle Cloud Always Free ($0/tháng)       │
│  ├── KiteHub services (Spring Boot, Next.js)        │
│  ├── PostgreSQL (self-hosted on VM)                 │
│  ├── Redis (self-hosted on VM)                      │
│  ├── Ollama AI (llama3.1:8b)                        │
│  └── Nginx reverse proxy + SSL                      │
│                                                     │
│  BACKUP: AWS ($338/tháng)                           │
│  ├── EKS + RDS + ElastiCache (Terraform/Helm)       │
│  └── Kích hoạt khi Oracle fail/capacity issue        │
├─────────────────────────────────────────────────────┤
│                KiteClass Instances                   │
│  AWS (không đổi)                                    │
│  ├── Per-tenant databases                           │
│  ├── S3 storage                                     │
│  └── CloudFront CDN                                 │
└─────────────────────────────────────────────────────┘
```

---

## 2. Oracle Cloud Always Free Resources

**Source**: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm

| Resource | Specs | Dùng cho |
|----------|-------|----------|
| **ARM Compute** | 4 OCPU + 24GB RAM (VM.Standard.A1.Flex) | App + AI |
| **x86 Compute** | 2 VMs x (1/8 OCPU + 1GB) | Monitoring/bastion |
| **Block Storage** | 200GB total | Boot + data volumes |
| **Object Storage** | 20GB Standard + 10GB Archive | Assets, backups |
| **Load Balancer** | 1 Flexible LB (10 Mbps) | HTTPS termination |
| **Outbound** | 10 TB/tháng | Bandwidth |
| **VCN** | 2 VCNs | Networking |
| **Thời hạn** | **Vĩnh viễn** | - |

**Verification links:**
- Overview: https://www.oracle.com/cloud/free/
- Specs: https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm
- ARM: https://docs.oracle.com/en-us/iaas/Content/Compute/References/arm.htm
- FAQ: https://www.oracle.com/cloud/free/faq/

---

## 3. Architecture: 2 ARM VMs

### VM 1: Backend (2 OCPU / 12GB RAM)

```
VM 1 (2 OCPU / 12GB RAM / 100GB disk)
├── Docker Compose
│   ├── kitehub-gateway        (~512MB)
│   ├── kitehub-subscription   (~512MB)
│   ├── kitehub-branding       (~512MB)
│   ├── kitehub-admin          (~512MB)
│   ├── kitehub-email          (~256MB)
│   ├── postgres:15            (~1GB)
│   ├── redis:7                (~256MB)
│   └── rabbitmq:3             (~256MB)
│                         Total: ~4.5GB / 12GB available
└── OS + Docker overhead        (~1GB)
    Free RAM: ~6.5GB (buffer)
```

### VM 2: Frontend + AI (2 OCPU / 12GB RAM)

```
VM 2 (2 OCPU / 12GB RAM / 100GB disk)
├── Docker Compose
│   ├── kitehub-frontend (Next.js)  (~512MB)
│   ├── kiteclass-frontend          (~512MB)
│   ├── ollama (llama3.1:8b)        (~8GB)
│   └── nginx (reverse proxy + SSL) (~64MB)
│                              Total: ~9.1GB / 12GB available
└── OS + Docker overhead              (~1GB)
    Free RAM: ~1.9GB (tight but OK)
```

### Networking

```
Internet → Oracle LB (10 Mbps, free)
           ├── :443 → VM2 Nginx → kitehub-frontend (:3001)
           │                    → kiteclass-frontend (:3000)
           │                    → VM1 gateway (:9000) [proxy_pass]
           └── Health checks
```

---

## 4. Best Practices & Cảnh báo

> Tổng hợp từ Oracle docs + community best practices.

### 🔴 PAYG ngay sau khi tạo account (BẮT BUỘC)

**Vấn đề**: Account free-tier thuần bị idle reclamation (VM idle 7 ngày → xóa) và thường gặp "Out of Capacity".

**Giải pháp**: Chuyển sang **Pay-As-You-Go (PAYG)** ngay sau khi tạo account.
- Vẫn dùng toàn bộ Always Free resources
- Vẫn $0 nếu không vượt free limit
- **Không bị idle reclamation**
- **Ít gặp "Out of Capacity" hơn**

> ⛔ KHÔNG dùng cron job fake CPU load → rủi ro vi phạm ToS → bị ban account.

**Source**: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm

### 🔴 Chọn home region kỹ (BẮT BUỘC)

**Vấn đề**: Home region **không thể đổi** sau khi tạo account. Always Free compute chỉ tạo được ở home region. Các region popular (US East, Frankfurt, Mumbai) thường hết ARM capacity.

**Giải pháp**: Chọn region ít popular khi đăng ký:
- **Khuyến nghị**: Singapore, Sydney, Seoul, Osaka, Tokyo
- **Tránh**: US-Ashburn, US-Phoenix, Frankfurt, Mumbai, London
- PAYG account được ưu tiên capacity hơn
- Có sẵn AWS backup nếu không tạo được

### 🟠 Billing alert $1 (QUAN TRỌNG)

**Vấn đề**: Với PAYG, có thể bị charge nhỏ ngoài ý muốn (trial resources chuyển sang paid khi trial hết).

**Giải pháp**: OCI Console → Budgets → tạo budget alert ở **$1** để nhận email ngay nếu có charge.

### 🟠 ARM-native Docker images (QUAN TRỌNG)

**Vấn đề**: x86 images chạy qua emulation trên ARM → chậm hơn 3-5x, tốn RAM hơn.

**Giải pháp**:
- Ưu tiên Docker images có tag `linux/arm64` hoặc `aarch64`
- Kiểm tra: `docker manifest inspect <image> | grep arm64`
- Spring Boot (JDK 21) ✅ native ARM
- Next.js (Node.js) ✅ native ARM
- PostgreSQL ✅ native ARM
- Redis ✅ native ARM
- Ollama ✅ native ARM
- Dùng `restart: unless-stopped` trong Docker Compose

### 🟠 Oracle có 2 lớp firewall (QUAN TRỌNG)

**Vấn đề**: Nhiều người chỉ mở Security List (cloud-level) mà quên iptables (instance-level) → service không expose được.

**Giải pháp**: Mở **cả 2 lớp**:
```bash
# 1. Oracle Console: VCN → Security List → Ingress Rules
#    Allow TCP 22, 80, 443 from 0.0.0.0/0

# 2. Instance-level: iptables
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

### 🟡 Không allocate tối đa RAM ngay (NÊN LÀM)

**Vấn đề**: Dùng hết 4 CPU + 24GB cho 2 VMs ngay từ đầu → CPU utilization thấp (vì app chưa có traffic) → gần ngưỡng idle.

**Giải pháp**: Bắt đầu nhỏ hơn:
- VM 1: 1 OCPU / 8GB (backend đủ dùng)
- VM 2: 1 OCPU / 8GB (frontend + Ollama 8b vừa khít)
- Scale lên khi có traffic thật
- CPU utilization cao hơn tự nhiên → an toàn hơn

### 🟡 Backup ra ngoài Oracle (NÊN LÀM)

**Vấn đề**: Always Free instances vẫn có thể bị reclaim vì policy violations hoặc Oracle thay đổi chính sách.

**Giải pháp**:
- `pg_dump` daily → Object Storage (20GB free) **VÀ** rsync/SCP về local machine
- Docker volumes backup → local
- Git repo đã có toàn bộ code (không lo mất)
- Database là thứ duy nhất cần backup kỹ

### 🟡 Monitor với OCI metrics (NÊN LÀM)

**Giải pháp**: Dùng `docker stats` hoặc OCI built-in metrics (free) để theo dõi CPU, RAM real-time. Phát hiện khi nào gần đụng quota.

### ⚠️ Không có Managed Database

**Vấn đề**: Oracle Free Tier không có PostgreSQL managed. Phải self-host trên VM.

**Giải pháp**:
- PostgreSQL chạy trong Docker trên VM 1
- Backup: `pg_dump` → Object Storage + local
- Không có Multi-AZ, không auto-failover
- **Chấp nhận được cho giai đoạn đầu** (ít users)

### ⚠️ 10 Mbps Load Balancer

**Vấn đề**: Bandwidth giới hạn 10 Mbps (~1.2 MB/s).

**Giải pháp**:
- KiteHub giai đoạn đầu: ít users → đủ
- Static assets serve từ CDN (Cloudflare free) → giảm load LB
- Khi cần hơn → chuyển sang AWS hoặc paid Oracle

### Checklist ưu tiên

| Việc | Mức độ | Khi nào |
|------|--------|---------|
| Chuyển PAYG | 🔴 Bắt buộc | Ngay sau tạo account |
| Chọn region ít popular | 🔴 Bắt buộc | Lúc đăng ký |
| Billing alert $1 | 🟠 Quan trọng | Sau khi có PAYG |
| ARM-native Docker images | 🟠 Quan trọng | Khi build images |
| Mở 2 lớp firewall | 🟠 Quan trọng | Khi tạo VM |
| Backup định kỳ ra ngoài | 🟡 Nên làm | Sau khi deploy |
| OCI metrics monitoring | 🟡 Nên làm | Sau khi deploy |
| Bắt đầu VM nhỏ, scale sau | 🟡 Nên làm | Khi tạo VM |

---

## 5. So sánh Chi phí

| | Oracle Free (Primary) | AWS (Backup) |
|--|----------------------|--------------|
| Compute | $0 (4 OCPU, 24GB ARM) | ~$163 (EKS + EC2) |
| Database | $0 (self-hosted PostgreSQL) | ~$100 (RDS Multi-AZ) |
| Cache | $0 (self-hosted Redis) | ~$15 (ElastiCache) |
| Queue | $0 (self-hosted RabbitMQ) | ~$30 (Amazon MQ) |
| Storage | $0 (20GB Object) | ~$5 (S3) |
| LB | $0 (10 Mbps) | ~$25 (ALB) |
| AI | $0 (Ollama on ARM) | ~$15/month (OpenAI API) |
| **Total** | **$0/tháng** | **~$338/tháng** |

---

## 6. Deployment Checklist

### Phase 1: Oracle Cloud Setup
- [ ] Tạo Oracle Cloud account (chọn home region: Tokyo hoặc Seoul)
- [ ] **Upgrade sang PAYG** (vẫn free, tránh idle reclamation)
- [ ] Tạo VCN + Security Lists (mở ports 80, 443, 22)
- [ ] Tạo VM 1: A1.Flex (2 OCPU / 12GB / 100GB) - Backend
- [ ] Tạo VM 2: A1.Flex (2 OCPU / 12GB / 100GB) - Frontend + AI
- [ ] Cài Docker + Docker Compose trên cả 2 VMs
- [ ] Setup SSH key access

### Phase 2: Deploy Services
- [ ] VM 1: Copy docker-compose-prod.yml + start backend stack
- [ ] VM 2: Copy docker-compose-prod.yml + start frontend + Ollama
- [ ] Ollama pull model: `ollama pull llama3.1:8b` (~4.7GB)
- [ ] Verify PostgreSQL, Redis, RabbitMQ healthy
- [ ] Verify all Spring Boot services healthy

### Phase 3: Networking & SSL
- [ ] Setup Oracle Load Balancer → VM 2 Nginx
- [ ] DNS: kiteclass.com → Oracle LB IP (hoặc Cloudflare proxy)
- [ ] SSL: Let's Encrypt via certbot trên Nginx
- [ ] Verify HTTPS: https://kiteclass.com, https://api.kiteclass.com

### Phase 4: Data & Verification
- [ ] Run Flyway migrations
- [ ] Create admin user
- [ ] E2E test: Register → Login → Create Instance → KiteClass API
- [ ] Setup pg_dump backup cron → Object Storage

### Phase 5: AWS Backup (Keep Ready)
- [ ] Terraform files giữ nguyên (không xóa)
- [ ] Document: "Khi nào switch sang AWS"
- [ ] Test `terraform plan` vẫn valid

---

## 7. Khi nào Switch sang AWS?

| Trigger | Action |
|---------|--------|
| Oracle ARM capacity hết, không tạo được VM | Switch AWS |
| Oracle account bị suspend/ban | Switch AWS |
| Cần >24GB RAM (scale lớn) | Switch AWS |
| Cần Multi-AZ database (SLA yêu cầu) | Switch AWS |
| >50 concurrent users (10 Mbps không đủ) | Switch AWS hoặc paid Oracle |

**Thời gian switch**: ~2-3 giờ (Terraform apply → Helm deploy → DNS update)

---

## 8. AI Provider Strategy

| Môi trường | Provider | Model | RAM |
|------------|----------|-------|-----|
| Local (laptop 16GB) | Ollama | llama3.1:**8b** | ~8GB |
| Oracle Free (24GB) | Ollama | llama3.1:**8b** | ~8GB |
| AWS (nếu switch) | OpenAI API | GPT-4 | 0 (cloud) |

Cùng `AIClient` interface → chỉ đổi config `ai.provider` khi switch.

---

## 9. KiteClass Instances (Không đổi)

KiteClass tenant instances **vẫn dùng AWS**:
- Per-tenant PostgreSQL databases (RDS hoặc shared)
- S3 storage cho assets
- CloudFront CDN cho static content
- Lý do: mỗi tenant cần isolation, scale riêng, SLA riêng

KiteHub platform (Oracle) chỉ quản lý metadata + provisioning.
Actual tenant data/traffic đi qua AWS.
