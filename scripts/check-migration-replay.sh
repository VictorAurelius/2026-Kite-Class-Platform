#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${POSTGRES_IMAGE:-postgres:16}"
CONTAINER="kite-migration-replay-$RANDOM-$$"

cleanup() {
  if [[ "${KEEP_MIGRATION_REPLAY_CONTAINER:-false}" == "true" ]]; then
    echo "Keeping Postgres container for debugging: $CONTAINER"
    return 0
  fi

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
  docker exec -i "$CONTAINER" psql -X -v ON_ERROR_STOP=1 -U postgres -d "$db" "$@"
}

create_replay_history() {
  local db="$1"

  psql_exec "$db" -q <<'SQL'
CREATE TABLE migration_replay_history (
  installed_rank INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  filename TEXT NOT NULL UNIQUE,
  installed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
SQL
}

record_replay_success() {
  local db="$1"
  local filename="$2"

  psql_exec "$db" -q -v replay_filename="$filename" <<'SQL'
INSERT INTO migration_replay_history (filename)
VALUES (:'replay_filename');
SQL
}

apply_migration_file() {
  local db="$1"
  local migration="$2"
  local label="$3"
  local basename_migration

  basename_migration="$(basename "$migration")"
  echo "  -> $basename_migration"

  if ! psql_exec "$db" -q < "$migration"; then
    echo "Migration replay failed for $label: $basename_migration" >&2
    docker logs "$CONTAINER" >&2 || true
    return 1
  fi

  record_replay_success "$db" "$basename_migration"
}

count_sql_migrations() {
  local dir="$1"
  find "$dir" -maxdepth 1 -type f -name 'V*.sql' | wc -l | tr -d '[:space:]'
}

applied_count() {
  local db="$1"
  psql_exec "$db" -At -c "SELECT count(*) FROM migration_replay_history;"
}

table_count() {
  local db="$1"
  psql_exec "$db" -At <<'SQL'
SELECT count(*)
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name <> 'migration_replay_history';
SQL
}

latest_replayed() {
  local db="$1"
  psql_exec "$db" -At <<'SQL'
SELECT filename
FROM migration_replay_history
ORDER BY installed_rank DESC
LIMIT 1;
SQL
}

replay_migrations() {
  local db="$1"
  local dir="$2"
  local label="$3"
  local expected
  local actual
  local tables
  local latest
  local migration

  if [[ ! -d "$dir" ]]; then
    echo "Migration directory not found for $label: $dir" >&2
    exit 1
  fi

  expected="$(count_sql_migrations "$dir")"
  if [[ "$expected" == "0" ]]; then
    echo "No V*.sql migrations found for $label in $dir" >&2
    exit 1
  fi

  echo "Replaying $label migrations from scratch ($expected files)"
  create_replay_history "$db"

  while IFS= read -r migration; do
    apply_migration_file "$db" "$migration" "$label"
  done < <(find "$dir" -maxdepth 1 -type f -name 'V*.sql' | sort -V)

  actual="$(applied_count "$db")"
  if [[ "$actual" != "$expected" ]]; then
    echo "Migration count mismatch for $label: expected=$expected actual=$actual" >&2
    exit 1
  fi

  tables="$(table_count "$db")"
  if [[ "$tables" == "0" ]]; then
    echo "Migration replay for $label created zero public tables" >&2
    exit 1
  fi

  latest="$(latest_replayed "$db")"
  echo "Replay OK for $label: applied=$actual tables=$tables latest=$latest"
}

main() {
  require_cmd docker

  docker run --rm -d \
    --name "$CONTAINER" \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_DB=postgres \
    "$IMAGE" >/dev/null

  wait_for_postgres

  psql_exec postgres -q -c "CREATE DATABASE kiteclass;" >/dev/null
  psql_exec postgres -q -c "CREATE DATABASE kitehub;" >/dev/null
  psql_exec kiteclass -q -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" >/dev/null
  psql_exec kitehub -q -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" >/dev/null

  replay_migrations \
    "kiteclass" \
    "$ROOT_DIR/kiteclass/kiteclass-core/src/main/resources/db/migration" \
    "kiteclass"

  replay_migrations \
    "kitehub" \
    "$ROOT_DIR/kitehub/kitehub-subscription/src/main/resources/db/migration" \
    "kitehub-subscription"

  echo "Migration replay check passed for kiteclass + kitehub-subscription."
}

main "$@"
