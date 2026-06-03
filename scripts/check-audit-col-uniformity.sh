#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${POSTGRES_IMAGE:-postgres:16}"
CONTAINER="kite-audit-col-uniformity-$RANDOM-$$"
KITECLASS_MIGRATIONS_DIR="${KITECLASS_MIGRATIONS_DIR:-$ROOT_DIR/kiteclass/kiteclass-core/src/main/resources/db/migration}"
KITEHUB_MIGRATIONS_DIR="${KITEHUB_MIGRATIONS_DIR:-$ROOT_DIR/kitehub/kitehub-subscription/src/main/resources/db/migration}"

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
    # Probe with a real psql query (not just pg_isready) — pg_isready can report
    # "accepting connections" before the unix socket inside the container is fully
    # ready, causing psql_exec socket errors under concurrent CI Docker contention.
    if docker exec "$CONTAINER" psql -U postgres -d postgres -c 'SELECT 1' >/dev/null 2>&1; then
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

check_kiteclass_actor_columns() {
  psql_exec kiteclass -At <<'SQL'
WITH target_columns AS (
    SELECT
        c.table_name,
        c.column_name,
        c.data_type
    FROM information_schema.columns c
    JOIN information_schema.tables t
      ON t.table_schema = c.table_schema
     AND t.table_name = c.table_name
    WHERE c.table_schema = 'public'
      AND t.table_type = 'BASE TABLE'
      AND (
          c.column_name LIKE '%\_by' ESCAPE '\'
          OR (c.table_name = 'payments' AND c.column_name IN ('payer_id', 'received_by'))
          OR (c.table_name = 'payment_idempotency_keys' AND c.column_name = 'user_id')
          OR (c.table_name = 'rebrand_approvals' AND c.column_name IN ('initiator_user_id', 'approver_user_id'))
          OR (c.table_name = 'audit_log' AND c.column_name = 'actor_user_id')
          OR (c.table_name = 'parent_invitations' AND c.column_name = 'invited_by_user_id')
          OR (c.table_name = 'staff_invitations' AND c.column_name IN ('invited_by_user_id', 'accepted_user_id'))
          OR (c.table_name = 'incidents' AND c.column_name IN ('reporter_user_id', 'assigned_officer_user_id'))
          OR (c.table_name = 'dmca_takedown_requests' AND c.column_name = 'reviewer_user_id')
          OR (c.table_name = 'vettings' AND c.column_name = 'decided_by_user_id')
      )
)
SELECT table_name || '|' || column_name || '|' || data_type
FROM target_columns
WHERE data_type <> 'uuid'
ORDER BY table_name, column_name;
SQL
}

check_kitehub_actor_columns() {
  psql_exec kitehub -At <<'SQL'
WITH target_columns AS (
    SELECT
        c.table_name,
        c.column_name,
        c.data_type
    FROM information_schema.columns c
    JOIN information_schema.tables t
      ON t.table_schema = c.table_schema
     AND t.table_name = c.table_name
    WHERE c.table_schema = 'public'
      AND t.table_type = 'BASE TABLE'
      AND (
          c.column_name IN ('created_by', 'updated_by')
          OR c.column_name LIKE '%\_by' ESCAPE '\'
      )
)
SELECT table_name || '|' || column_name || '|' || data_type
FROM target_columns
WHERE data_type <> 'uuid'
ORDER BY table_name, column_name;
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

  apply_migrations "kiteclass" "$KITECLASS_MIGRATIONS_DIR"
  apply_migrations "kitehub" "$KITEHUB_MIGRATIONS_DIR"

  local failures
  failures="$(
    {
      check_kiteclass_actor_columns | sed 's/^/kiteclass|/'
      check_kitehub_actor_columns | sed 's/^/kitehub|/'
    } | sed '/^$/d'
  )"

  if [ -n "$failures" ]; then
    echo "Audit column uniformity check failed; expected UUID actor columns:" >&2
    echo "$failures" >&2
    exit 1
  fi

  echo "Audit column uniformity check passed for kiteclass + kitehub migrations."
}

main "$@"
