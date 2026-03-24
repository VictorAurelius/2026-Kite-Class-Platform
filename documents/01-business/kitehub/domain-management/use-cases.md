# Domain Management — Use Cases

### UC-DOM-01: Setup Custom Domain
- **Actor:** Owner (PREMIUM/ENTERPRISE tier)
- **Precondition:** Instance tier = PREMIUM hoặc ENTERPRISE (DOM-01)
- **Steps:**
  1. FE: Settings → Custom Domain tab, nhập domain
  2. User: submit domain (e.g., school.edu.vn)
  3. System: validate tier đủ điều kiện (DOM-01)
  4. System: check domain chưa được dùng bởi instance khác (DOM-05)
  5. System: generate token: `kitehub-verify={uuid}` (DOM-02)
  6. System: set domainStatus = PENDING_VERIFY
  7. System: trả về token + DNS instructions + backup URL
- **Postcondition:** Domain PENDING_VERIFY, user có instructions để cài TXT record
- **Errors:**
  - 403: tier không đủ (FREE/BASIC) → "Custom domain requires PREMIUM or higher"
  - 409: domain đã được dùng bởi instance khác
- **FE Behavior:** Hiển thị DNS setup instructions với copy button cho token

### UC-DOM-02: Verify Custom Domain
- **Actor:** Owner (sau khi đã cài TXT record)
- **Precondition:** Domain status = PENDING_VERIFY
- **Steps:**
  1. FE: hiển thị "Verify" button (sau khi user đã cài TXT record)
  2. User: click verify
  3. System: POST /api/instances/{id}/domain/verify
  4. System: DNS TXT lookup cho domain
  5. System (verified): nếu TXT record khớp token → VERIFIED, set verifiedAt
  6. System (mock mode): DNS lookup fail → giữ PENDING_VERIFY (DOM-09)
  7. System (production): DNS lookup fail → giữ PENDING_VERIFY (DOM-10)
- **Postcondition:** VERIFIED hoặc vẫn PENDING_VERIFY
- **FE Behavior:** Hiển thị "Verified ✓" hoặc "Still pending — check DNS settings"

### UC-DOM-03: Xem Domain Status
- **Actor:** Owner
- **Steps:**
  1. FE: GET /api/instances/{id}/domain
  2. System: trả về currentDomain, status, backupUrl, instructions
- **FE Behavior:** Hiển thị badge (VERIFIED / PENDING_VERIFY / NONE)

### UC-DOM-04: Xóa Custom Domain
- **Actor:** Owner
- **Steps:**
  1. FE: confirm dialog "Remove custom domain?"
  2. System: xóa customDomain, token, verifiedAt
  3. System: set domainStatus = NONE
- **Postcondition:** Instance chỉ còn backup subdomain URL (DOM-07)
