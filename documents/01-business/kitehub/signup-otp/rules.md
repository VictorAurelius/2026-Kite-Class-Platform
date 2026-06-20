# Signup OTP — Business Rules

**Domain:** KiteHub auth / mobile signup
**Status:** 🟡 Phase 1 scaffold (mock delivery) — live ZNS/SMS = Phase 2
**Related:** `use-cases.md` · `api-contract.md` · GAP-286 · GAP-063 (Zalo/SMS channel) · `documents/00-brd/compliance-checklist.md`

---

## Review metadata (per `.claude/rules/business-logic-review.md` §2 — 5 attributes)

Áp dụng cho toàn bộ BR-OTP-* dưới đây (giá trị cùng class OTP security defaults):

- **Source:** Industry standard OTP security (OWASP Authentication Cheat Sheet — code length / TTL / rate-limit / attempt-cap) + informed gut cho VN-specific (Zalo-primary, phone format). Không có internal A/B data — flag re-review Q3.
- **Rationale:** xem từng BR (cột Lý do).
- **Reviewer:** @nguyenvankiet (acting Product Owner + Security scout, solo-dev, 2026-06-21). Legal review N/A cho OTP mechanics; PDPL áp cho lưu số điện thoại (xem Compliance).
- **Compliance check:** **Considered** — số điện thoại = PII per PDPL (NĐ13/2023 L1) → OTP store hash + TTL ngắn + không log plaintext số ở production. SMS/ZNS nội dung không spam per Consumer Protection (L3) + anti-spam NĐ91/2020. Counsel review queued GAP-156 AC-D.
- **Review cadence:** Quarterly. **Next review:** 2026-09-21. Event trigger: thay đổi rate-limit do abuse, đổi provider, hoặc PDPL hướng dẫn mới.

---

## Business Rules

> Config prefix thực tế trong code: `kitehub.auth.signup-otp.*` (+ `jwt.signup-secret` cho signupToken). Một số giá trị hardcoded (đánh dấu) — promote thành config key khi cần tune.

| ID | Rule | Giá trị (config key) | Lý do (Rationale) |
|---|---|---|---|
| **BR-OTP-001** | Độ dài mã OTP | 6 chữ số (hardcoded) | 6 digit = chuẩn ngành (Google/bank VN); 4 quá yếu (10k tổ hợp), 8 khó nhập mobile |
| **BR-OTP-002** | Thời hạn (TTL) | 5 phút / 300s (`kitehub.auth.signup-otp.code-ttl-seconds=300`) | Đủ để user nhận ZNS/SMS + nhập (mạng VN ~10-30s); >10min tăng cửa sổ brute-force |
| **BR-OTP-003** | Rate limit request | 3 request / 15 phút / số (`kitehub.auth.signup-otp.rate-limit-max-requests=3`, `rate-limit-window-minutes=15`) | Chặn SMS-bombing + chi phí; 3 lần đủ cho retry hợp lệ (mạng lỗi) |
| **BR-OTP-004** | Số lần verify tối đa / mã | 5 lần, sau đó vô hiệu mã (`kitehub.auth.signup-otp.max-verify-attempts=5`) | Chặn brute-force 6-digit (5 lần = 5/1tr xác suất); buộc request mã mới |
| **BR-OTP-005** | Định dạng số điện thoại | VN `^0\d{9,10}$` (hardcoded validation) | Số VN bắt đầu `0` + 9-10 chữ số; chặn input rác trước khi tốn 1 OTP |
| **BR-OTP-006** | Kênh giao | Zalo ZNS primary → SMS fallback (Phase 2); **mock Phase 1** (`kitehub.auth.signup-otp.mock-enabled=true`; `channel` trong request body, default ZALO) | Zalo = kênh chủ đạo VN (per `sms-provider-evaluation.md`); email KHÔNG phù hợp persona phụ huynh/gia sư |
| **BR-OTP-007** | signupToken TTL | 10 phút (HS256, `jwt.signup-secret`; TTL hardcoded) — mirror `twofactor/ChallengeTokenService` | Token chứng minh sở hữu số → bước tạo tenant kế tiếp; ngắn để giảm replay |
| **BR-OTP-008** | Không log OTP plaintext ở production | mock-mode log INFO (dev `[OTP-MOCK]`); production KHÔNG log mã | PDPL + security — mã là credential tạm |

---

## Out of scope (Phase 2 / follow-up)

- Live ZNS/SMS delivery (vendor account — GAP-063 Phase 2, vendor-blocked)
- Per-signup cost telemetry (Zalo ZNS vs SMS unit cost)
- Redis-backed OTP store (Phase 1 = in-memory TTL store; Redis khi scale đa-instance)
- Fast tenant provisioning sub-30s (GAP-286 separate sub-task)

---

## Log

- **2026-06-21 (v1.0):** Created cùng GAP-286 backend OTP scaffold. 8 BR-OTP rules với 5-attribute review metadata (born-compliant per `business-logic-review.md` §2). Phase 1 = mock delivery + in-memory store; live + cost telemetry = Phase 2. Author: @nguyenvankiet (acting PO + Security scout, solo-dev).
