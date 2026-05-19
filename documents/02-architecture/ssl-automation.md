---
title: SSL Automation Strategy
audience: dev
created: 2026-03-24
last-reviewed: 2026-05-19
status: living
---

# SSL Automation Strategy

## Overview

KiteHub hỗ trợ custom domain cho tenants (ví dụ: `lms.trungtamabc.vn`).
Mỗi custom domain cần SSL certificate hợp lệ để truy cập qua HTTPS.

## Recommended: Cloudflare for SaaS

### Tại sao chọn Cloudflare for SaaS
- **Zero-downtime** provisioning — certificate tự động issue khi domain verify xong
- **Wildcard + custom hostname** — 1 zone quản lý tất cả tenant domains
- **DDoS protection** + CDN miễn phí đi kèm
- **API-driven** — tích hợp tốt với DomainService hiện tại

### Flow
1. Tenant thêm custom domain qua UI (`CustomDomainTab`)
2. `DomainService` gọi Cloudflare API → tạo Custom Hostname
3. Cloudflare trả về CNAME target (ví dụ: `abc123.cdn.cloudflare.net`)
4. Tenant trỏ CNAME record của domain về target trên
5. Cloudflare tự động issue và renew SSL certificate (DCV qua CNAME)
6. `DomainVerificationScheduler` poll trạng thái cho đến khi `ssl.status = active`

### Cloudflare API Integration
```
POST /zones/{zone_id}/custom_hostnames
{
  "hostname": "lms.trungtamabc.vn",
  "ssl": {
    "method": "cname",
    "type": "dv"
  }
}
```

### Chi phí
- Cloudflare for SaaS: 100 custom hostnames miễn phí, sau đó $0.10/hostname/tháng
- Phù hợp scale từ 0 đến hàng nghìn tenants

## Alternative: Let's Encrypt (ACME)

### Khi nào dùng
- Self-hosted infrastructure (không qua Cloudflare)
- Cần full control over certificate lifecycle
- Budget constraint (hoàn toàn miễn phí)

### Flow
1. Tenant thêm custom domain và trỏ DNS về IP/CNAME của KiteHub
2. `CertificateService` tạo ACME order qua Let's Encrypt
3. HTTP-01 hoặc DNS-01 challenge để verify domain ownership
4. Let's Encrypt issue certificate (valid 90 ngày)
5. Certificate lưu vào secure storage (Vault hoặc encrypted DB)
6. Reverse proxy (Nginx/Caddy) tự động reload certificate

### Certificate Renewal
- Cron job chạy hàng ngày, renew certificates còn < 30 ngày
- Retry logic: 3 lần, exponential backoff
- Alert nếu renewal fail liên tiếp 3 ngày

### Hạn chế
- Rate limit: 50 certificates/domain/tuần
- Cần manage certificate storage và rotation
- Không có CDN/DDoS protection đi kèm

## Custom Domain SSL Flow (End-to-End)

```
Tenant UI          Backend              DNS Provider       SSL Provider
   |                  |                      |                  |
   |-- Add domain --->|                      |                  |
   |                  |-- Create record ---->|                  |
   |<-- DNS config ---|                      |                  |
   |                  |                      |                  |
   | (tenant updates DNS)                    |                  |
   |                  |                      |                  |
   |-- Verify ------->|-- Check DNS -------->|                  |
   |                  |<-- DNS OK -----------|                  |
   |                  |-- Request cert ------|----------------->|
   |                  |<-- Cert issued ------|------------------|
   |<-- SSL Active ---|                      |                  |
```

## Certificate Renewal Automation

| Aspect              | Cloudflare for SaaS       | Let's Encrypt            |
|---------------------|---------------------------|--------------------------|
| Renewal             | Automatic (managed)       | Cron job (self-managed)  |
| Validity            | 1 year (auto-renew)       | 90 days                  |
| Monitoring          | Cloudflare dashboard      | Custom alerting needed   |
| Failure handling    | Cloudflare manages retry  | App-level retry + alert  |
| Storage             | Cloudflare edge           | Vault / encrypted DB     |

## Configuration

```yaml
# application.yml
kitehub:
  ssl:
    provider: cloudflare  # or letsencrypt
    cloudflare:
      zone-id: ${CLOUDFLARE_ZONE_ID}
      api-token: ${CLOUDFLARE_API_TOKEN}
    letsencrypt:
      acme-server: https://acme-v02.api.letsencrypt.org/directory
      challenge-type: http-01
      renewal-days-before-expiry: 30
```

## Decision

**Production recommendation: Cloudflare for SaaS**
- Ít operational overhead nhất
- SSL + CDN + DDoS trong 1 package
- Phù hợp với multi-tenant SaaS architecture của KiteHub
- Let's Encrypt giữ làm fallback cho self-hosted deployments
