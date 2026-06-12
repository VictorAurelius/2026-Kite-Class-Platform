# Hướng Dẫn Deploy KiteHub lên Oracle Cloud

**Phiên bản:** 1.0
**Ngày tạo:** 2026-03-19
**Chi phí:** $0/tháng (Always Free tier)
**Thời gian deploy:** ~1-2 giờ

---

## Tổng Quan

KiteHub platform chạy trên Oracle Cloud Always Free với 2 VM ARM:
- **VM 1 (Backend)**: Spring Boot services + PostgreSQL + Redis + RabbitMQ
- **VM 2 (Frontend + AI)**: Next.js + Ollama AI + Nginx reverse proxy

**Tổng tài nguyên miễn phí**: 4 CPU, 24GB RAM, 200GB storage, 10TB bandwidth/tháng.

---

## Bước 1: Tạo Tài Khoản Oracle Cloud

1. Truy cập https://www.oracle.com/cloud/free/
2. Đăng ký tài khoản (cần thẻ credit/debit để verify - KHÔNG bị charge)
3. **Chọn home region cẩn thận** - không thể đổi sau:
   - ✅ Khuyến nghị: `Singapore`, `Sydney`, `Seoul`, `Osaka`
   - ❌ Tránh: `US East (Ashburn)`, `Frankfurt`, `Mumbai` (thường hết ARM capacity)
4. **Ngay sau khi tạo xong → Upgrade sang PAYG** (Pay-As-You-Go):
   - Vào Account → Upgrade to Paid
   - Vẫn dùng toàn bộ Always Free, vẫn $0
   - Tránh bị idle reclamation, ít bị "Out of Capacity"
5. **Tạo billing alert $1**: Budgets → Create Budget → Alert ở $1

---

## Bước 2: Tạo Infrastructure (Terraform)

### Cài đặt công cụ

```bash
# Cài Terraform
# https://developer.hashicorp.com/terraform/install

# Cài OCI CLI
bash -c "$(curl -L https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.sh)"
oci setup config  # Theo hướng dẫn nhập tenancy OCID, user OCID, region, API key
```

### Chạy Terraform

```bash
cd terraform-oracle
cp terraform.tfvars.example terraform.tfvars

# Sửa terraform.tfvars: điền OCI credentials
# - tenancy_ocid: Lấy từ OCI Console → Profile → Tenancy
# - user_ocid: Lấy từ OCI Console → Profile → User Settings
# - fingerprint: Lấy khi tạo API Key
# - ssh_public_key: SSH public key của bạn
nano terraform.tfvars

# Tạo infrastructure
terraform init
terraform plan    # Xem trước những gì sẽ tạo
terraform apply   # Nhập "yes" để xác nhận
```

### Kết quả sau terraform apply

```
Outputs:
  backend_public_ip  = "152.xx.xx.xx"
  frontend_public_ip = "168.xx.xx.xx"
  backend_private_ip = "10.0.1.xx"
  ssh_command_backend  = "ssh opc@152.xx.xx.xx"
  ssh_command_frontend = "ssh opc@168.xx.xx.xx"
```

> ⚠️ Nếu gặp lỗi "Out of Host Capacity" → đợi 1-2 giờ rồi thử lại, hoặc giảm OCPU/RAM.

---

## Bước 3: Deploy Backend (VM 1)

```bash
# SSH vào VM 1
ssh opc@<BACKEND_PUBLIC_IP>

# Clone repo
git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform/kitehub

# Tạo file cấu hình
cat > .env << 'EOF'
POSTGRES_USER=kitehub
POSTGRES_PASSWORD=<tạo_password_ngẫu_nhiên>
POSTGRES_DB=kitehub
RABBITMQ_USER=kitehub
RABBITMQ_PASSWORD=<tạo_password_ngẫu_nhiên>
JWT_SECRET=<openssl rand -base64 64>
ENCRYPTION_MASTER_KEY=<openssl rand -base64 32>
INTERNAL_API_SECRET=<openssl rand -base64 32>
AI_PROVIDER=ollama
OLLAMA_HOST=<FRONTEND_PRIVATE_IP>
EOF

# Bảo mật file .env
chmod 600 .env

# Build images (lần đầu mất ~10-15 phút trên ARM)
docker compose -f docker-compose.oracle-backend.yml build

# Khởi động services
docker compose -f docker-compose.oracle-backend.yml up -d

# Kiểm tra
docker compose -f docker-compose.oracle-backend.yml ps
curl http://localhost:9000/actuator/health
```

---

## Bước 4: Deploy Frontend + AI (VM 2)

```bash
# SSH vào VM 2
ssh opc@<FRONTEND_PUBLIC_IP>

# Clone repo
git clone https://github.com/VictorAurelius/2026-Kite-Class-Platform.git
cd 2026-Kite-Class-Platform/kitehub

# Tạo .env
cat > .env << 'EOF'
API_URL=http://<BACKEND_PRIVATE_IP>:9000
EOF

# Build frontend images
docker compose -f docker-compose.oracle-frontend.yml build

# Khởi động
docker compose -f docker-compose.oracle-frontend.yml up -d

# Tải model AI (mất ~5-10 phút, ~4.7GB)
docker exec kitehub-ollama ollama pull llama3.1:8b

# Kiểm tra
docker compose -f docker-compose.oracle-frontend.yml ps
curl http://localhost:3001          # Frontend
curl http://localhost:11434/api/tags # Ollama AI
```

---

## Bước 5: Cấu Hình Domain + SSL

### Option A: Cloudflare (Khuyến nghị - miễn phí)

1. Đăng ký Cloudflare: https://www.cloudflare.com
2. Thêm domain `kitehub.me`
3. Tạo DNS records:
   - `A` record: `kitehub.me` → `<FRONTEND_PUBLIC_IP>` (Proxy ON)
   - `A` record: `api.kitehub.me` → `<FRONTEND_PUBLIC_IP>` (Proxy ON)
4. SSL mode: Full (Strict)
5. Cloudflare tự cấp SSL + CDN miễn phí

### Option B: Let's Encrypt (tự quản lý)

```bash
# Trên VM 2
sudo dnf install certbot
sudo certbot certonly --standalone -d kitehub.me -d api.kitehub.me
# Copy cert vào nginx/ssl/
```

### Cấu hình Nginx

```bash
# Sửa nginx config: đặt IP VM 1 backend
cd ~/2026-Kite-Class-Platform/kitehub
sed -i "s/\${GATEWAY_HOST:-10.0.1.10}/<BACKEND_PRIVATE_IP>/g" nginx/nginx.conf

# Restart nginx
docker compose -f docker-compose.oracle-frontend.yml restart nginx
```

---

## Bước 6: Kiểm Tra End-to-End

```bash
# Từ máy local
curl https://kitehub.me                      # ✅ Frontend
curl https://api.kitehub.me/actuator/health   # ✅ Gateway

# Test đăng nhập
curl -X POST https://api.kitehub.me/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@kitehub.com","password":"Admin@KiteHub123"}'
# ✅ Trả về token
```

---

## Bước 7: Backup Tự Động

```bash
# Trên VM 1 - tạo script backup
cat > ~/backup-db.sh << 'SCRIPT'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR=~/backups
mkdir -p $BACKUP_DIR

# Dump PostgreSQL
docker exec kitehub-postgres pg_dumpall -U kitehub | gzip > $BACKUP_DIR/kitehub_${DATE}.sql.gz

# Giữ 7 ngày gần nhất
find $BACKUP_DIR -name "kitehub_*.sql.gz" -mtime +7 -delete

echo "[$(date)] Backup completed: kitehub_${DATE}.sql.gz"
SCRIPT

chmod +x ~/backup-db.sh

# Chạy daily lúc 3 giờ sáng
echo "0 3 * * * ~/backup-db.sh >> ~/backup.log 2>&1" | crontab -
```

**Backup ra ngoài Oracle** (khuyến nghị):
```bash
# Từ máy local, rsync backup về
rsync -avz opc@<BACKEND_IP>:~/backups/ ~/kitehub-backups/
```

---

## Xử Lý Sự Cố

### Lỗi "Out of Host Capacity"
- Đợi 1-2 giờ rồi thử lại
- Giảm OCPU/RAM (ví dụ: 1 CPU / 8GB mỗi VM)
- Nếu vẫn không được → dùng AWS backup

### Service không chạy
```bash
# Xem logs
docker compose -f docker-compose.oracle-backend.yml logs -f <service_name>

# Restart service
docker compose -f docker-compose.oracle-backend.yml restart <service_name>
```

### Không truy cập được từ internet
- Kiểm tra **Security List** trên OCI Console (cloud-level firewall)
- Kiểm tra **firewall-cmd** trên VM (instance-level firewall):
  ```bash
  sudo firewall-cmd --list-all
  sudo firewall-cmd --permanent --add-port=80/tcp
  sudo firewall-cmd --reload
  ```

### Chuyển sang AWS (backup)
Xem: [PRODUCTION-DEPLOY.md](../PRODUCTION-DEPLOY.md) → Option B

---

## Chi Phí

| Hạng mục | Chi phí |
|----------|---------|
| 2 ARM VMs (4 CPU, 24GB) | $0 |
| 200GB block storage | $0 |
| Object Storage (20GB) | $0 |
| Bandwidth (10 TB) | $0 |
| Cloudflare SSL + CDN | $0 |
| Ollama AI | $0 |
| **Tổng hàng tháng** | **$0** |

---

## Tham Khảo

- [Oracle Cloud Free Tier](https://www.oracle.com/cloud/free/)
- [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/resourceref.htm)
- [Oracle Cloud FAQ](https://www.oracle.com/cloud/free/faq/)
- [KiteHub Oracle Architecture](../../03-planning/infrastructure/kitehub-oracle-cloud-deployment.md)
- [AWS Backup Option](../PRODUCTION-DEPLOY.md)
