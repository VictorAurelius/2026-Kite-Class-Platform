# Audit Report: Local vs Production Environment

**Ngày**: 2026-03-18
**PR**: 6.5
**Scope**: Đánh giá sự phân biệt giữa local và production trong code, docs, data, security

---

## Tổng quan

| Category | Issues Found | Critical | High | Medium |
|----------|-------------|----------|------|--------|
| Hardcoded Secrets | 30+ | 19 | 8 | 3 |
| Demo Data in Code | 3 | 2 | 1 | 0 |
| Missing Profiles | 4 | 0 | 4 | 0 |
| Gitignore Gaps | 2 | 0 | 2 | 0 |
| Doc Gaps | 3 | 0 | 0 | 3 |

---

## 1. CODE: Hardcoded Values & Missing Profiles

### 🔴 CRITICAL: Secrets hardcoded trong source code

**JWT Secrets (2 files, chạy ở MỌI environment):**

| File | Value | Risk |
|------|-------|------|
| `AuthService.java:42` | `kitehub-super-secret-key-that-is-at-least-256-bits-long-for-hs384-algorithm` | Nếu production quên set env var → dùng default này → JWT giả mạo được |
| `TokenService.java:30` | `kitehub-development-secret-key-must-be-at-least-256-bits` | Tương tự |
| `kiteclass-gateway application.yml:118` | `your-super-secret-key-min-512-bits-long...` | JWT KiteClass |

**Fix cần làm**: Không có default value cho JWT secret. Production PHẢI set env var, nếu không → app không start.

**Encryption Master Key (1 file):**

| File | Value | Risk |
|------|-------|------|
| `docker-compose.kitehub.yml:141` | `tOHICYANHtUnqfwWc+MwtOnvzf/Pnwz/2ZXsnWEtRuU=` | Master key giải mã TẤT CẢ tenant DB passwords |

**Fix cần làm**: Chuyển vào `.env` file (gitignored).

### 🟠 HIGH: Không có Spring profiles phân biệt

**Hiện trạng:**
- `SPRING_PROFILES_ACTIVE: dev` set trong docker-compose
- **Không có `application-dev.yml` hay `application-prod.yml`**
- Tất cả config nằm trong `application.yml` chung
- Demo user seed chạy ở MỌI profile (không kiểm tra)

**Fix cần làm**: Tạo profile-specific configs hoặc ít nhất conditional trên profile.

---

## 2. TÀI LIỆU: Local vs Production Documentation

### 🟡 MEDIUM: Thiếu tài liệu phân biệt

| Gap | Chi tiết |
|-----|----------|
| **Deployment guide** | Không có "Production Deployment Checklist" |
| **Environment matrix** | Không có bảng so sánh local vs staging vs prod |
| **Secret management guide** | Không có hướng dẫn quản lý secrets cho production |

**Có sẵn:**
- ✅ `kiteclass-docker-deployment.md` (446 lines) - Kubernetes design
- ✅ `kitehub-database-provisioning.md` (1238 lines) - DB provisioning
- ✅ `security-design.md` (1164 lines) - Security architecture
- ❌ Không có **practical deployment checklist**

---

## 3. DỮ LIỆU MẪU: Demo Data, Mock, KiteTeam

### 🔴 CRITICAL: Demo user hardcoded trong code

**File**: `AuthService.java` (lines 48-75)

```java
@PostConstruct
public void initDemoUser() {
    String demoEmail = "demo@kitehub.com";       // Hardcoded
    String passwordHash = encoder.encode("Demo@123"); // Hardcoded & logged
    USER_STORE.put(demoEmail, ...);
    // Also creates demo instance
    log.info("Demo user ready: {} / Demo@123", demoEmail); // PASSWORD IN LOG!
}
```

**Vấn đề:**
1. Chạy ở MỌI environment (không check profile)
2. Password logged plaintext
3. Tạo instance "demo" mỗi lần start
4. Nếu production deploy → có sẵn backdoor account

### 🟠 HIGH: Test data references trong code

| File | Data | Issue |
|------|------|-------|
| `test-api-e2e.sh:236` | `demo@kitehub.com / Demo@123` | Test depends on hardcoded demo user |
| `fixtures/test-data.ts:25` | `e2e-test@example.com` | Expects pre-existing account |
| OpenAI config | `sk-mock-key-for-local-testing` | Mock key in application.yml |

### Về KiteTeam data

**Hiện chưa có**. Cần tạo:
1. **KiteTeam internal test instance** - cho team phát triển testing
2. **KiteTeam demo showcase instance** - cho khách hàng xem demo
3. Cả hai nên được seed bằng script, KHÔNG hardcode trong code

---

## 4. BẢO MẬT: Secrets Management

### 🔴 CRITICAL: 30+ hardcoded passwords trong docker-compose

**Password `kitehub_dev_password` xuất hiện 15+ lần** trong docker-compose.kitehub.yml:
- PostgreSQL, Redis, RabbitMQ, MinIO
- Subscription, Branding, Admin, Email, Gateway services
- KiteClass Core service

**Password `kiteclass123` xuất hiện 10+ lần** trong docker-compose.dev.yml.

### 🔴 CRITICAL: .gitignore thiếu entries

| Repo | `.env` excluded? | Risk |
|------|-----------------|------|
| Root `.gitignore` | ❌ **KHÔNG** | `.env` có thể bị commit |
| kiteclass-core | ✅ Có | OK |
| kiteclass-gateway | ❌ **KHÔNG** | `.env` có thể bị commit |
| kitehub-subscription | ✅ Có | OK |

### 🟠 HIGH: `.env.example` chứa passwords thật

**File**: `kiteclass-gateway/.env.example`
```
DB_PASSWORD=kiteclass123        # KHÔNG NÊN có giá trị thật
JWT_SECRET=development-only-... # KHÔNG NÊN có giá trị thật
```

---

## Recommended Fix PRs (Ưu tiên)

### PR-FIX-1: Secrets → .env file (CRITICAL)
**Scope:**
- [ ] Tạo `.env.example` với placeholders (không passwords)
- [ ] Tạo `.env` với giá trị local dev (gitignored)
- [ ] Update docker-compose dùng `${VAR}` syntax
- [ ] Update `.gitignore` root: thêm `.env`, `*.key`
- [ ] Fix kiteclass-gateway `.gitignore`: thêm `.env`

### PR-FIX-2: Demo data → seed script (CRITICAL)
**Scope:**
- [ ] Xóa `@PostConstruct initDemoUser()` khỏi AuthService
- [ ] Tạo `scripts/seed-data.sh` tạo demo user + KiteTeam instances
- [ ] Update test scripts dùng seed script thay vì hardcoded demo user
- [ ] Password KHÔNG được log plaintext

### PR-FIX-3: JWT secrets - no default (HIGH)
**Scope:**
- [ ] Remove default values cho JWT secrets trong application.yml
- [ ] App PHẢI crash nếu JWT secret chưa set (fail-fast)
- [ ] Document required env vars cho mỗi service

### PR-FIX-4: Spring profiles (HIGH)
**Scope:**
- [ ] Tạo `application-dev.yml` cho local config
- [ ] Tạo `application-prod.yml` checklist
- [ ] Conditional demo data chỉ khi `profile=dev`
- [ ] DB provisioning simulation khi `profile!=dev` và lifecycle disabled

### PR-FIX-5: Documentation (MEDIUM)
**Scope:**
- [ ] Tạo "Production Deployment Checklist"
- [ ] Tạo "Environment Configuration Matrix" (local vs staging vs prod)
- [ ] Tạo "Secret Management Guide"
- [ ] Document KiteTeam instances (internal test + demo showcase)

---

## Summary

**Rủi ro lớn nhất**: Nếu ai đó deploy code hiện tại lên production mà không đọc docs kỹ, sẽ có:
1. ❌ Demo backdoor account (`demo@kitehub.com / Demo@123`)
2. ❌ JWT tokens dùng default secret (giả mạo được)
3. ❌ Encryption master key công khai (giải mã được tất cả DB passwords)
4. ❌ Tất cả services dùng chung 1 password

**Ưu tiên fix**: PR-FIX-1 (secrets) → PR-FIX-2 (demo data) → PR-FIX-3 (JWT) → PR-FIX-4 (profiles) → PR-FIX-5 (docs)
