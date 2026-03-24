# KiteHub Local End-to-End Roadmap

**Mục tiêu**: Local environment hoạt động end-to-end - từ register instance đến access KiteClass app.
**Ngày tạo**: 2026-03-18

---

## Current State (sau PR 5.13)

```
User → KiteHub FE (3001) → Gateway (9000) → Services → PostgreSQL (5433)
                                                         ↑ Metadata only
                                                         ↓
                                              Instance record created
                                              BUT no real DB provisioned
                                              AND no KiteClass running

KiteClass (chạy riêng, không kết nối):
  FE (3000) → Gateway (8080) → Core (8081) → PostgreSQL (5432)
```

## Target State (sau roadmap này)

```
User → KiteHub FE (3001) → Gateway (9000) → Subscription Service
                                                    ↓
                                         1. Create instance record
                                         2. Provision DB: kiteclass_{id}
                                         3. Run Flyway migrations
                                                    ↓
User → customer.localhost:3001 → Gateway (9000) → TenantResolver
                                                    ↓
                                         Lookup instance by subdomain
                                         Set X-Tenant-Id header
                                                    ↓
                                         KiteClass Core (shared)
                                              ↓
                                    kiteclass_{id} DB (isolated)
```

---

## PRs Cần Thực Hiện (Ưu Tiên)

### 🔴 P0 - Critical Path (Local E2E)

#### PR 6.1: Database Provisioning Thật
**Scope**: Enable real database creation cho local environment
**Tasks**:
- [ ] Enable `database.lifecycle.enabled: true` cho local profile
- [ ] Implement `DatabaseProvisioningService.createPhysicalDatabase()`
  - CREATE USER, CREATE DATABASE, GRANT PRIVILEGES
  - Connect to admin PostgreSQL (localhost:5433/postgres)
- [ ] Run Flyway migrations trên DB mới tạo
  - Copy migrations từ kiteclass-core
- [ ] Encrypt credentials với AES-256-GCM
- [ ] Test: register → verify DB created → verify tables exist
**Estimate**: 1-2 ngày
**Dependencies**: Không

#### PR 6.2: TenantResolver Gateway Filter
**Scope**: Route requests theo subdomain đến đúng instance
**Tasks**:
- [ ] Implement `TenantResolverGatewayFilterFactory`
  - Extract subdomain từ Host header
  - Lookup instance trong DB
  - Verify status (ACTIVE/TRIAL)
  - Add X-Tenant-Id, X-Instance-Id headers
- [ ] Configure gateway route cho KiteClass
- [ ] Handle custom domain (PREMIUM)
- [ ] Local DNS: configure /etc/hosts hoặc dùng nip.io
- [ ] Test: access demo.localhost → routed to KiteClass with correct tenant
**Estimate**: 1-2 ngày
**Dependencies**: PR 6.1

#### PR 6.3: KiteClass Shared Instance (Local)
**Scope**: Run 1 KiteClass instance phục vụ tất cả tenants (local only)
**Tasks**:
- [ ] Add KiteClass Core vào docker-compose.kitehub.yml
  - Share cùng PostgreSQL (port 5433)
  - Environment: multi-tenant mode
- [ ] Configure KiteClass Gateway nhận X-Tenant-Id từ KiteHub Gateway
- [ ] KiteClass Core: Hibernate filter by instanceId
- [ ] Test: register instance → login → access KiteClass → see empty data
**Estimate**: 1-2 ngày
**Dependencies**: PR 6.1, PR 6.2

#### PR 6.4: End-to-End Integration Test
**Scope**: Test script verify toàn bộ flow local
**Tasks**:
- [ ] Extend `test-api-e2e.sh`:
  - Register user + instance
  - Verify DB provisioned
  - Access KiteClass qua tenant routing
  - CRUD students/courses qua KiteClass API
  - Verify data isolation giữa 2 instances
- [ ] Add FE E2E test: register → dashboard → click "Truy cập KiteClass" → see KiteClass UI
**Estimate**: 1 ngày
**Dependencies**: PR 6.1, PR 6.2, PR 6.3

---

### 🟠 P1 - Important (Feature Complete)

#### PR 6.5: Payment Flow (VietQR Mock)
**Scope**: Payment processing hoạt động ở local (mock VietQR)
**Tasks**:
- [ ] Create mock VietQR service (return fake QR code)
- [ ] Implement payment webhook callback
- [ ] Billing page: select tier → create payment → scan QR → confirm
- [ ] Auto-activate subscription after payment confirmed
- [ ] Admin: confirm/reject pending payments
**Estimate**: 2 ngày

#### PR 6.6: AI Branding (Mock Mode)
**Scope**: Branding wizard hoạt động ở local (mock AI responses)
**Tasks**:
- [ ] Create mock AI service khi OPENAI_API_KEY = mock key
  - Return sample logo analysis
  - Return placeholder images
  - Return sample marketing copy
- [ ] Full wizard flow: upload → analyze → generate → review → publish
- [ ] Store assets in MinIO
**Estimate**: 1-2 ngày

#### PR 6.7: Email Notifications (Local)
**Scope**: Email service gửi email ở local (MailHog hoặc log)
**Tasks**:
- [ ] Add MailHog container cho local dev
- [ ] Implement email templates (welcome, trial expiring, payment confirmed)
- [ ] RabbitMQ consumer xử lý email events
**Estimate**: 1 ngày

---

### 🟡 P2 - Nice to Have (Polish)

#### PR 6.8: Settings Page Full Implementation
**Scope**: Settings forms hoạt động thật
**Tasks**:
- [ ] AccountTab: update profile, change password
- [ ] InstanceTab: custom domain, notification toggles
- [ ] DangerZone: cancel subscription, delete instance

#### PR 6.9: Admin Dashboard Real Data
**Scope**: Admin dashboard hiển thị data thật
**Tasks**:
- [ ] Revenue chart (monthly/yearly)
- [ ] Instance growth chart
- [ ] Payment confirmation flow end-to-end

#### PR 6.10: KiteClass Frontend Integration
**Scope**: KiteClass FE chạy trong Docker, accessible qua subdomain
**Tasks**:
- [ ] Add kiteclass-frontend vào docker-compose
- [ ] Configure reverse proxy cho subdomain routing
- [ ] Branding assets hiển thị trên KiteClass landing page

---

## Thứ Tự Thực Hiện

```
PR 6.1 (DB Provisioning) ────→ PR 6.2 (TenantResolver) ────→ PR 6.3 (KiteClass Shared)
                                                                        ↓
                                                              PR 6.4 (E2E Integration Test)
                                                                        ↓
                              PR 6.5 (Payment Mock) ←── có thể song song với 6.1-6.4
                              PR 6.6 (AI Branding Mock) ←── có thể song song
                              PR 6.7 (Email Local) ←── có thể song song
                                                                        ↓
                              PR 6.8-6.10 (Polish) ←── sau khi core hoạt động
```

## Definition of Done

**Local E2E hoạt động** = user có thể:
1. ✅ Mở http://localhost:3001 → thấy landing page
2. ✅ Register account → tạo instance
3. ✅ Login → thấy dashboard với instance
4. ✅ Click "Truy cập KiteClass" → KiteClass API qua gateway (PR 6.1-6.3, merged #114-#116)
5. ✅ Tạo student/course trong KiteClass (PR 6.3, merged #116)
6. ✅ Data isolated giữa 2 instances (PR 6.4 - covered by test-api-e2e.sh)
7. ✅ Mock payment/branding/email hoạt động (PR-A3, merged #123)

## Completion Status (2026-03-18)

| PR | Status | GitHub |
|---|---|---|
| PR 6.1 DB Provisioning | ✅ DONE | #114 |
| PR 6.2 TenantResolver | ✅ DONE | #115 |
| PR 6.3 KiteClass Shared | ✅ DONE | #116 |
| PR 6.4 E2E Integration | ✅ DONE | Covered by 63 API E2E tests |
| PR 6.5 Payment Mock | ✅ DONE | Merged into PR-A3 #123 |
| PR 6.6 AI Branding Mock | ✅ DONE | Merged into PR-A3 #123 |
| PR 6.7 Email Local | ✅ DONE | Already existed (AWS_SES_MOCK_MODE) |

### Remaining (P2 - Nice to have):
| PR | Description | Status |
|---|---|---|
| PR 6.8 | Settings Page Full Implementation | ⬜ Future |
| PR 6.9 | Admin Dashboard Real Data | ⬜ Future |
| PR 6.10 | KiteClass Frontend Integration | ⬜ Future |
8. ⬜ AI generate branding → see on landing page (cần PR 6.6)
