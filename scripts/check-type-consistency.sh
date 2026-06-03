#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${POSTGRES_IMAGE:-postgres:16}"
CONTAINER="kite-type-consistency-$RANDOM-$$"

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

check_money_columns() {
  local db="$1"

  psql_exec "$db" -At <<'SQL'
WITH candidate_columns AS (
    SELECT
        table_name,
        column_name,
        data_type,
        numeric_precision,
        numeric_scale
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND (
          column_name ~ '(^|_)(amount|price|tax|fee)($|_)'
          OR column_name IN (
              'amount_vnd',
              'amount_paid',
              'balance_due',
              'discount',
              'final_amount',
              'gross_amount',
              'net_amount',
              'price_vnd',
              'subtotal',
              'total',
              'tuition_amount',
              'unit_price'
          )
      )
      AND column_name NOT IN (
          'discount_percent',
          'max_percentage',
          'min_percentage',
          'response_status'
      )
      AND table_name NOT IN ('schema_version')
),
misses AS (
    SELECT
        table_name,
        column_name,
        data_type,
        numeric_precision,
        numeric_scale
    FROM candidate_columns
    WHERE data_type <> 'numeric'
       OR numeric_precision <> 19
       OR numeric_scale <> 2
)
SELECT table_name || '|' || column_name || '|money|' ||
       data_type || '(' || COALESCE(numeric_precision::text, '-') || ',' ||
       COALESCE(numeric_scale::text, '-') || ')'
FROM misses
ORDER BY table_name, column_name;
SQL
}

check_time_columns() {
  local db="$1"

  psql_exec "$db" -At <<'SQL'
WITH candidate_columns AS (
    SELECT table_name, column_name, data_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND (
          column_name LIKE '%\_at' ESCAPE '\'
          OR column_name LIKE '%\_time' ESCAPE '\'
          OR column_name LIKE '%\_date' ESCAPE '\'
      )
      AND table_name NOT IN ('schema_version')
      AND NOT (table_name = 'email_sent_log' AND column_name = 'sent_at')
),
misses AS (
    SELECT table_name, column_name, data_type
    FROM candidate_columns
    WHERE CASE
        WHEN column_name LIKE '%\_date' ESCAPE '\' AND data_type = 'date' THEN false
        WHEN column_name LIKE '%\_time' ESCAPE '\' AND data_type = 'time without time zone' THEN false
        ELSE data_type <> 'timestamp with time zone'
    END
)
SELECT table_name || '|' || column_name || '|time|' || data_type
FROM misses
ORDER BY table_name, column_name;
SQL
}

check_uppercase_checks() {
  local db="$1"

  psql_exec "$db" -At <<'SQL'
WITH check_constraints AS (
    SELECT
        c.conrelid::regclass::text AS table_name,
        c.conname,
        pg_get_constraintdef(c.oid) AS definition
    FROM pg_constraint c
    JOIN pg_namespace n ON n.oid = c.connamespace
    WHERE n.nspname = 'public'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%status%'
      AND c.conrelid::regclass::text IN ('invoices', 'payments')
),
misses AS (
    SELECT table_name, conname, definition
    FROM check_constraints
    WHERE definition ~ '''[a-z][a-z_]*'''
)
SELECT table_name || '|' || conname || '|enum|' || definition
FROM misses
ORDER BY table_name, conname;
SQL
}

check_vnd_only_currency() {
  local db="$1"

  psql_exec "$db" -At <<'SQL'
WITH currency_constraints AS (
    SELECT
        c.conrelid::regclass::text AS table_name,
        c.conname,
        pg_get_constraintdef(c.oid) AS definition
    FROM pg_constraint c
    JOIN pg_namespace n ON n.oid = c.connamespace
    WHERE n.nspname = 'public'
      AND c.contype = 'c'
      AND pg_get_constraintdef(c.oid) LIKE '%currency%'
),
misses AS (
    SELECT table_name, conname, definition
    FROM currency_constraints
    WHERE definition LIKE '%USD%'
)
SELECT table_name || '|' || conname || '|currency|' || definition
FROM misses
ORDER BY table_name, conname;
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
      check_money_columns "kiteclass" | sed 's/^/kiteclass|/'
      check_money_columns "kitehub" | sed 's/^/kitehub|/'
      check_time_columns "kiteclass" | sed 's/^/kiteclass|/'
      check_time_columns "kitehub" | sed 's/^/kitehub|/'
      check_uppercase_checks "kiteclass" | sed 's/^/kiteclass|/'
      check_uppercase_checks "kitehub" | sed 's/^/kitehub|/'
      check_vnd_only_currency "kiteclass" | sed 's/^/kiteclass|/'
      check_vnd_only_currency "kitehub" | sed 's/^/kitehub|/'
    } | sed '/^$/d'
  )"

  if [ -n "$failures" ]; then
    echo "Type consistency check failed:" >&2
    echo "$failures" >&2
    exit 1
  fi

  echo "Type consistency check passed for kiteclass + kitehub migrations."
}

main "$@"
