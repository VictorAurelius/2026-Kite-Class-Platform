#!/bin/bash
# Seed test users for local self-test JWT flow.
# Password: Test@1234 (bcrypt cost 12)
# Usage: bash scripts/local-test-fixtures/seed-test-users.sh
set -e
cd "$(dirname "$0")/../.."

if ! command -v python3 >/dev/null 2>&1; then
  echo "FAIL: python3 required for bcrypt hash generation"
  exit 1
fi
if ! python3 -c "import bcrypt" 2>/dev/null; then
  echo "FAIL: pip install bcrypt — required for password hashing"
  exit 1
fi

HASH=$(python3 -c "import bcrypt; print(bcrypt.hashpw(b'Test@1234', bcrypt.gensalt()).decode())")
echo "Generated bcrypt hash for 'Test@1234': cost=12"

docker exec -e HASH="$HASH" kite-postgres psql -U kitehub -d kitehub <<SQL
DELETE FROM users WHERE email LIKE '%@test.vn';
INSERT INTO users (id, email, name, phone, password_hash, role, email_verified, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'owner.test@test.vn', 'Test Owner', '0903333333', '\${HASH}', 'OWNER',          true, NOW(), NOW()),
  (gen_random_uuid(), 'admin.test@test.vn', 'Test Admin', '0904444444', '\${HASH}', 'PLATFORM_ADMIN', true, NOW(), NOW()),
  (gen_random_uuid(), 'staff.test@test.vn', 'Test Staff', '0905555555', '\${HASH}', 'STAFF',          true, NOW(), NOW())
ON CONFLICT (email) DO UPDATE SET password_hash = EXCLUDED.password_hash;

SELECT email, role FROM users WHERE email LIKE '%@test.vn' ORDER BY role;
SQL
echo "✅ Seeded 3 test users. Password: Test@1234"
echo "Login test: curl -X POST http://localhost:9000/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"owner.test@test.vn\",\"password\":\"Test@1234\"}'"
