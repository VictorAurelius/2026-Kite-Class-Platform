#!/usr/bin/env bash
# GAP-376 (Wave 33 Bucket A): Production data seed wrapper.
#
# Purpose:
#   - Sanity-check DB connectivity + required env vars BEFORE invoking the
#     Spring Boot ProductionSeedRunner.
#   - Provide a `--dry-run` mode that validates configuration without
#     starting a JVM (useful in CI gates + first-deploy runbook).
#
# Usage:
#   scripts/seed-production.sh --dry-run
#   scripts/seed-production.sh
#
# Required env vars (production mode):
#   DATABASE_URL              jdbc:postgresql://host:5432/kitehub
#   DATABASE_USERNAME         postgres
#   DATABASE_PASSWORD         <secret>
#   SEED_ADMIN_PASSWORD       <secret — sourced from secrets manager per GAP-379>
#
# Optional:
#   SEED_ADMIN_EMAIL          default admin@kitehub.vn
#   JAVA_OPTS                 forwarded to the spring-boot run
#   KITE_SUBSCRIPTION_JAR     path to the built jar (default kitehub/kitehub-subscription/target/*.jar)
#
# Exit codes:
#   0 — dry-run passed OR seed completed
#   1 — missing required env var
#   2 — DB connectivity failure
#   3 — jar not found / runner failure

set -euo pipefail

DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=1 ;;
        --help|-h)
            grep '^# ' "$0" | sed 's/^# //'
            exit 0
            ;;
        *) ;;
    esac
done

log() { printf '[seed-production] %s\n' "$*"; }
err() { printf '[seed-production] ERROR: %s\n' "$*" >&2; }

# 1. Required env vars
REQUIRED_VARS=(DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD SEED_ADMIN_PASSWORD)
MISSING=()
for v in "${REQUIRED_VARS[@]}"; do
    if [[ -z "${!v:-}" ]]; then
        MISSING+=("$v")
    fi
done

if (( ${#MISSING[@]} > 0 )); then
    err "missing required env vars: ${MISSING[*]}"
    err "see scripts/seed-production.sh header for the full list"
    if (( DRY_RUN == 1 )); then
        log "dry-run: env validation FAILED"
        exit 1
    fi
    exit 1
fi

log "env validation: OK (4 required vars present)"

ADMIN_EMAIL="${SEED_ADMIN_EMAIL:-admin@kitehub.vn}"
log "admin email target: ${ADMIN_EMAIL}"

# 2. DB connectivity check (best-effort — uses pg_isready if available)
if command -v pg_isready >/dev/null 2>&1; then
    HOST=$(echo "${DATABASE_URL}" | sed -E 's|.*//([^:/]+).*|\1|')
    PORT=$(echo "${DATABASE_URL}" | sed -E 's|.*:([0-9]+)/.*|\1|')
    if pg_isready -h "${HOST}" -p "${PORT:-5432}" -t 5 >/dev/null 2>&1; then
        log "DB connectivity: OK (${HOST}:${PORT:-5432})"
    else
        err "DB connectivity: FAILED (${HOST}:${PORT:-5432})"
        if (( DRY_RUN == 0 )); then
            exit 2
        fi
        log "dry-run: continuing despite DB unreachable"
    fi
else
    log "pg_isready not on PATH — skipping connectivity probe (non-fatal)"
fi

if (( DRY_RUN == 1 )); then
    log "dry-run: configuration valid; production seed would run with mode=production"
    log "dry-run: PASS"
    exit 0
fi

# 3. Locate the subscription jar
JAR="${KITE_SUBSCRIPTION_JAR:-}"
if [[ -z "${JAR}" ]]; then
    JAR=$(find kitehub/kitehub-subscription/target -maxdepth 1 -name 'kitehub-subscription-*.jar' \
            ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null | head -n 1 || true)
fi

if [[ -z "${JAR}" || ! -f "${JAR}" ]]; then
    err "subscription jar not found — build with: mvn -pl kitehub/kitehub-subscription -am package"
    exit 3
fi

log "launching seed runner — jar=${JAR}"

# JAVA_OPTS intentionally word-splits (multiple JVM flags).
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} \
    -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-production}" \
    -Dkite.seed.mode=production \
    -Dkite.seed.admin-email="${ADMIN_EMAIL}" \
    -Dkite.seed.admin-password="${SEED_ADMIN_PASSWORD}" \
    -jar "${JAR}"
