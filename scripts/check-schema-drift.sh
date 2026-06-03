#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="${POSTGRES_IMAGE:-postgres:16}"
CONTAINER="kite-schema-drift-$RANDOM-$$"
BOOT_TIMEOUT_SECONDS="${SCHEMA_DRIFT_BOOT_TIMEOUT_SECONDS:-240}"

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

container_port() {
  docker port "$CONTAINER" 5432/tcp | sed 's/.*://'
}

run_with_timeout() {
  local label="$1"
  shift
  local log_file
  local pid
  local start
  local code=0

  echo "==> $label"
  log_file="$(mktemp)"

  set +e
  setsid "$@" > >(tee "$log_file") 2>&1 &
  pid=$!
  start="$(date +%s)"

  while kill -0 "$pid" >/dev/null 2>&1; do
    if [ $(( $(date +%s) - start )) -ge "$BOOT_TIMEOUT_SECONDS" ]; then
      kill -TERM "-$pid" >/dev/null 2>&1 || true
      sleep 5
      kill -KILL "-$pid" >/dev/null 2>&1 || true
      wait "$pid" >/dev/null 2>&1
      code=124
      break
    fi
    sleep 1
  done

  if [ "$code" -eq 0 ]; then
    wait "$pid"
    code=$?
  fi
  set -e

  if [ "$code" -eq 124 ] \
      && grep -q "Initialized JPA EntityManagerFactory" "$log_file" \
      && ! grep -Eq "SchemaManagementException|Failed to initialize JPA EntityManagerFactory|Application run failed" "$log_file"; then
    echo "$label reached Hibernate validate successfully; stopped after timeout because non-web background threads kept running."
    rm -f "$log_file"
    return 0
  fi

  rm -f "$log_file"

  if [ "$code" -ne 0 ]; then
    if [ "$code" -eq 124 ]; then
      echo "$label timed out after ${BOOT_TIMEOUT_SECONDS}s" >&2
    fi
    return "$code"
  fi
}

common_boot_args() {
  cat <<'ARGS'
--spring.main.web-application-type=none
--server.port=0
--spring.jpa.hibernate.ddl-auto=validate
--spring.jpa.open-in-view=false
--spring.rabbitmq.listener.simple.auto-startup=false
--spring.rabbitmq.listener.direct.auto-startup=false
--spring.task.scheduling.enabled=false
--management.endpoints.enabled-by-default=false
ARGS
}

validate_kiteclass_core() {
  local port="$1"
  local args
  args="$(common_boot_args)
--spring.flyway.enabled=true
--spring.datasource.url=jdbc:postgresql://127.0.0.1:${port}/kiteclass
--spring.datasource.username=postgres
--spring.datasource.password=postgres
--spring.data.redis.host=127.0.0.1
--spring.data.redis.port=1
--spring.rabbitmq.host=127.0.0.1
--spring.rabbitmq.port=1"

  (
    cd "$ROOT_DIR/kiteclass/kiteclass-core"
    SPRING_PROFILES_ACTIVE=local \
      run_with_timeout "kiteclass-core Flyway replay + Hibernate validate" \
      ./mvnw -q -DskipTests spring-boot:run \
        -Dspring-boot.run.arguments="$args"
  )
}

validate_kitehub_subscription() {
  local port="$1"
  local args
  args="$(common_boot_args)
--spring.flyway.enabled=true
--spring.datasource.url=jdbc:postgresql://127.0.0.1:${port}/kitehub
--spring.datasource.username=postgres
--spring.datasource.password=postgres
--spring.rabbitmq.host=127.0.0.1
--spring.rabbitmq.port=1
--outbox.dispatcher.enabled=false
--jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

  (
    cd "$ROOT_DIR/kitehub"
    run_with_timeout "kitehub-subscription Flyway replay + Hibernate validate" \
      ./mvnw -q -f kitehub-subscription/pom.xml -DskipTests spring-boot:run \
        -Dspring-boot.run.arguments="$args"
  )
}

validate_kitehub_branding() {
  local port="$1"
  local args
  args="$(common_boot_args)
--spring.flyway.enabled=false
--spring.datasource.url=jdbc:postgresql://127.0.0.1:${port}/kitehub
--spring.datasource.username=postgres
--spring.datasource.password=postgres
--spring.data.redis.host=127.0.0.1
--spring.data.redis.port=1
--spring.rabbitmq.host=127.0.0.1
--spring.rabbitmq.port=1"

  (
    cd "$ROOT_DIR/kitehub"
    run_with_timeout "kitehub-branding Hibernate validate against migrated kitehub schema" \
      ./mvnw -q -f kitehub-branding/pom.xml -DskipTests spring-boot:run \
        -Dspring-boot.run.arguments="$args"
  )
}

main() {
  require_cmd docker
  require_cmd timeout

  docker run --rm -d \
    --name "$CONTAINER" \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_DB=postgres \
    -p 127.0.0.1::5432 \
    "$IMAGE" >/dev/null

  wait_for_postgres

  local port
  port="$(container_port)"
  docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U postgres -d postgres <<'SQL' >/dev/null
CREATE DATABASE kiteclass;
CREATE DATABASE kitehub;
\connect kiteclass
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\connect kitehub
CREATE EXTENSION IF NOT EXISTS pgcrypto;
SQL

  local failures=0

  validate_kitehub_subscription "$port" || failures=1
  validate_kitehub_branding "$port" || failures=1
  validate_kiteclass_core "$port" || failures=1

  if [ "$failures" -ne 0 ]; then
    echo "Schema drift check failed. Review SchemaManagementException output above." >&2
    exit 1
  fi

  echo "Schema drift check passed for kitehub-subscription, kitehub-branding, and kiteclass-core."
}

main "$@"
