-- seed-kh-owner-sso.sql
-- Mục đích (GAP-1305): seed 1 KiteHub OWNER credential DÀNH RIÊNG cho SSO walk + bind đúng
-- 1 instance, để luồng cross-product SSO KiteHub :3001 -> KiteClass :3000 (ADR-040 Option A)
-- browser-walk được local + land ĐÚNG tenant có data. Trước seed này, fresh DB không có owner
-- KH nào -> :3001/dashboard đá /login, nút "Mở quản lý trường" không render -> SSO dừng bước 1.
--
-- Chạy (DB `kitehub`, KHÔNG phải kiteclass_shared):
--   docker exec -i kite-postgres psql -U kitehub -d kitehub < kitehub/scripts/seed-kh-owner-sso.sql
--
-- BỔ SUNG cho seed-toan10a1-demo.sql (seed KiteClass auth_credentials + data của tenant
-- aaaabbbb-0000-0000-0000-000000000001). Seed này seed phía KiteHub.
--
-- === TẠI SAO owner RIÊNG (không tái dùng owner.test@test.vn) ===
-- resolveTenantIdForRole (AuthService.java:852) emit JWT tenantId = instance ĐẦU TIÊN của owner
-- (List.findFirst() KHÔNG ORDER BY). Javadoc:838 "Phase 1 BETA constraint: 1 user -> 1 tenant".
-- owner.test@test.vn cố ý sở hữu 2 instance (sky-test 22003e3c BASIC cho flow subscription/
-- branding/domain + skytest aaaabbbb FREE chứa KC data) -> vi phạm invariant -> tenantId claim
-- KHÔNG ổn định (lúc aaaabbbb lúc sky-test). Để SSO land DETERMINISTIC vào tenant có KC data:
--   * Owner SSO RIÊNG (sso.owner@skytest.test) sở hữu DUY NHẤT aaaabbbb -> tenantId luôn = aaaabbbb.
--   * owner.test còn lại 1 instance sky-test(22003e3c) -> cũng deterministic cho flow paid-tier.
-- Reassign owner_id của aaaabbbb chỉ chạm KH instances.owner_id (KC data + G3 minted-token walk
-- KHÔNG phụ thuộc) -> an toàn.
--
-- Idempotent: chạy lại không tạo trùng; ENSURE walk-critical (totp tắt, email_verified, password,
-- single-instance binding) để SSO walk luôn chạy + land đúng tenant.
--
-- Credential SSO walk (sau seed):
--   Email:    sso.owner@skytest.test
--   Password: Test@1234
--   Tenant:   aaaabbbb-0000-0000-0000-000000000001 (subdomain skytest, có KC data) — JWT tenantId
--
-- Verify sau seed (cả 2 phải HTTP 200, tenantId=aaaabbbb):
--   curl -s -X POST http://localhost:9000/api/auth/login -H 'Content-Type: application/json' \
--     -d '{"email":"sso.owner@skytest.test","password":"Test@1234"}'  # accessToken + tenantId=aaaabbbb
--   TOK=<accessToken>; curl -s -X POST http://localhost:9000/api/v1/auth/sso/issue-code \
--     -H "Authorization: Bearer $TOK"                                  # {"code":...,"expiresIn":60}

DO $$
DECLARE
    v_sso_id  uuid := 'aaaabbbb-0000-0000-0000-0000000000e1';  -- fixed UUID cho owner SSO riêng
    v_inst    uuid := 'aaaabbbb-0000-0000-0000-000000000001';  -- tenant skytest (có KC data)
    v_email   text := 'sso.owner@skytest.test';
    -- BCrypt($2b$12$) của Test@1234 — verified login HTTP 200 (2026-06-14)
    v_hash    text := '$2b$12$NMGWjN9gxiBXxCNwUkXfyOJVwQ.kRgaYyupzTkl/.a49RLNIbsTW2';
    v_exists  uuid;
BEGIN
    -- 1) Ensure dedicated SSO-walk OWNER (login surface :3001 /api/auth/login)
    SELECT id INTO v_exists FROM users WHERE email = v_email;
    IF v_exists IS NULL THEN
        INSERT INTO users (id, email, name, password_hash, role,
                           email_verified, totp_required, totp_enrolled_at,
                           created_at, updated_at)
        VALUES (v_sso_id, v_email, 'SSO Walk Owner', v_hash, 'OWNER',
                true, false, NULL, now(), now());
        RAISE NOTICE 'seed-kh-owner-sso: created dedicated SSO owner % (%).', v_email, v_sso_id;
    ELSE
        v_sso_id := v_exists;
        UPDATE users
           SET password_hash    = v_hash,
               role             = 'OWNER',
               email_verified   = true,
               totp_required    = false,
               totp_enrolled_at = NULL,
               updated_at       = now()
         WHERE id = v_sso_id;
        RAISE NOTICE 'seed-kh-owner-sso: ensured dedicated SSO owner % (%) walk-critical fields.', v_email, v_sso_id;
    END IF;

    -- 2) Bind tenant skytest (aaaabbbb, có KC data) cho DUY NHẤT owner SSO riêng.
    --    -> JWT tenantId claim của owner SSO luôn = aaaabbbb (single-instance, deterministic).
    --    -> owner.test còn lại sky-test(22003e3c) cũng single-instance, deterministic.
    --    Nút "Mở quản lý trường" render iff owner có >=1 non-deleted instance (use-instances byOwner).
    IF EXISTS (SELECT 1 FROM instances WHERE id = v_inst) THEN
        UPDATE instances
           SET owner_id   = v_sso_id,
               deleted    = false,
               status     = 'ACTIVE',
               tier       = 'FREE',
               updated_at = now()
         WHERE id = v_inst;
        RAISE NOTICE 'seed-kh-owner-sso: reassigned instance % (skytest) -> SSO owner.', v_inst;
    ELSE
        INSERT INTO instances (id, subdomain, organization_name, owner_id, tier, status,
                               database_url, database_username, database_password,
                               vertical_type, deleted, email_notifications, trial_reminders,
                               migration_phase, domain_status, created_at, updated_at)
        VALUES (v_inst, 'skytest', 'Sky Test Center', v_sso_id, 'FREE', 'ACTIVE',
                'jdbc:postgresql://kite-postgres:5432/kiteclass_shared', 'kitehub', 'x',
                'CENTER', false, true, true, 'NONE', 'NONE', now(), now());
        RAISE NOTICE 'seed-kh-owner-sso: created instance % (skytest) bound to SSO owner.', v_inst;
    END IF;
END $$;

-- Hiển thị kết quả: SSO owner -> 1 instance aaaabbbb (deterministic tenantId)
SELECT u.email, u.role, u.email_verified, u.totp_required,
       i.id AS tenant_id, i.subdomain, i.status,
       (SELECT count(*) FROM instances x WHERE x.owner_id = u.id AND x.deleted = false) AS owned_instances
  FROM users u
  JOIN instances i ON i.owner_id = u.id AND i.deleted = false
 WHERE u.email = 'sso.owner@skytest.test';
