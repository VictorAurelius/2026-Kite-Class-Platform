#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${POSTGRES_IMAGE:-postgres:16}"
CONTAINER="kite-rls-coverage-$RANDOM-$$"

cleanup() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "Missing required command: $cmd" >&2
    exit 127
  fi
}

wait_for_postgres() {
  for _ in $(seq 1 60); do
    if docker exec "$CONTAINER" pg_isready -U postgres >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "Postgres container did not become ready in time" >&2
  docker logs "$CONTAINER" >&2 || true
  return 1
}

psql_exec() {
  local db="$1"
  shift
  docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U postgres -d "$db" "$@"
}

apply_migrations() {
  local db="$1"
  local dir="$2"
  local migration

  echo "Applying migrations for $db from $dir"
  while IFS= read -r migration; do
    echo "  -> $(basename "$migration")"
    psql_exec "$db" < "$migration" >/dev/null
  done < <(find "$dir" -maxdepth 1 -type f -name 'V*.sql' | sort -V)
}

check_direct_tenant_policy() {
  local db="$1"
  local force_required="$2"

  psql_exec "$db" -v force_required="$force_required" -At <<'SQL'
WITH tenant_columns AS (
    SELECT
        c.oid,
        c.relname AS table_name,
        a.attname AS tenant_column,
        c.relrowsecurity,
        c.relforcerowsecurity
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_attribute a ON a.attrelid = c.oid
    WHERE n.nspname = 'public'
      AND c.relkind = 'r'
      AND a.attnum > 0
      AND NOT a.attisdropped
      AND a.attname IN ('instance_id', 'tenant_id')
      AND format_type(a.atttypid, a.atttypmod) = 'uuid'
      -- 'instances': the tenant-registry table itself (not tenant-scoped data).
      -- 'auth_credentials': KC-native login lookup happens PRE-auth — no
      -- app.current_tenant_id GUC is set yet, so RLS would block the very lookup
      -- that establishes tenant binding. The credential row IS the source of
      -- instance_id; global-email-unique lookup is intentional (see
      -- V89__create_auth_credentials.sql header). Wave auth-1 / GAP-725.
      AND c.relname NOT IN ('instances', 'auth_credentials')
),
policy_summary AS (
    SELECT
        schemaname,
        tablename,
        string_agg(COALESCE(qual, '') || ' ' || COALESCE(with_check, ''), ' ') AS policy_expr
    FROM pg_policies
    WHERE schemaname = 'public'
    GROUP BY schemaname, tablename
),
misses AS (
    SELECT
        tc.table_name,
        tc.tenant_column,
        CASE
            WHEN NOT tc.relrowsecurity THEN 'RLS_DISABLED'
            WHEN :'force_required' = 'yes' AND NOT tc.relforcerowsecurity THEN 'RLS_NOT_FORCED'
            WHEN ps.policy_expr IS NULL THEN 'POLICY_MISSING'
            WHEN ps.policy_expr NOT LIKE '%' || tc.tenant_column || '%' THEN 'TENANT_COLUMN_MISSING_FROM_POLICY'
            WHEN ps.policy_expr NOT LIKE '%app.current_tenant_id%' THEN 'TENANT_GUC_MISSING_FROM_POLICY'
            ELSE NULL
        END AS reason
    FROM tenant_columns tc
    LEFT JOIN policy_summary ps ON ps.schemaname = 'public' AND ps.tablename = tc.table_name
    WHERE tc.table_name <> 'branding_templates'
)
SELECT table_name || '|' || tenant_column || '|' || reason
FROM misses
WHERE reason IS NOT NULL
ORDER BY table_name;
SQL
}

check_special_tables() {
  local db="$1"

  psql_exec "$db" -At <<'SQL'
WITH teacher_courses_state AS (
    SELECT
        c.relrowsecurity,
        c.relforcerowsecurity,
        EXISTS (
            SELECT 1
            FROM pg_policies p
            WHERE p.schemaname = 'public'
              AND p.tablename = 'teacher_courses'
              AND COALESCE(p.qual, '') LIKE '%teachers%'
              AND COALESCE(p.qual, '') LIKE '%app.current_tenant_id%'
        ) AS has_join_policy
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'teacher_courses'
),
branding_templates_state AS (
    SELECT
        c.relrowsecurity,
        c.relforcerowsecurity,
        EXISTS (
            SELECT 1
            FROM pg_policies p
            WHERE p.schemaname = 'public'
              AND p.tablename = 'branding_templates'
              AND p.cmd = 'SELECT'
              AND COALESCE(p.qual, '') = 'true'
        ) AS has_public_read
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'branding_templates'
),
misses AS (
    SELECT 'teacher_courses' AS table_name, 'RLS_POLICY_MISSING' AS reason
    FROM teacher_courses_state
    WHERE NOT relrowsecurity OR NOT relforcerowsecurity OR NOT has_join_policy
    UNION ALL
    SELECT 'branding_templates' AS table_name, 'PUBLIC_READ_POLICY_MISSING' AS reason
    FROM branding_templates_state
    WHERE NOT relrowsecurity OR NOT relforcerowsecurity OR NOT has_public_read
)
SELECT table_name || '|special|' || reason
FROM misses
ORDER BY table_name;
SQL
}

main() {
  require_cmd docker

  docker run --rm -d \
    --name "$CONTAINER" \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_DB=postgres \
    "$IMAGE" >/dev/null

  wait_for_postgres

  psql_exec postgres -c "CREATE DATABASE kiteclass;" >/dev/null
  psql_exec postgres -c "CREATE DATABASE kitehub;" >/dev/null
  psql_exec kiteclass -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" >/dev/null
  psql_exec kitehub -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" >/dev/null

  apply_migrations "kiteclass" "$ROOT_DIR/kiteclass/kiteclass-core/src/main/resources/db/migration"
  apply_migrations "kitehub" "$ROOT_DIR/kitehub/kitehub-subscription/src/main/resources/db/migration"

  local failures
  failures="$(
    {
      check_direct_tenant_policy "kiteclass" "yes" | sed 's/^/kiteclass|/'
      check_direct_tenant_policy "kitehub" "no" | sed 's/^/kitehub|/'
      check_special_tables "kiteclass" | sed 's/^/kiteclass|/'
      check_special_tables "kitehub" | sed 's/^/kitehub|/'
    } | sed '/^$/d'
  )"

  if [ -n "$failures" ]; then
    echo "RLS coverage check failed:" >&2
    echo "$failures" >&2
    exit 1
  fi

  echo "RLS coverage check passed for kiteclass + kitehub migrations."
}

main "$@"
