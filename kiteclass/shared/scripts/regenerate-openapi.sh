#!/usr/bin/env bash
#
# Regenerate kiteclass/shared/openapi.json from a running kiteclass-core instance.
#
# Prerequisites: docker stack up (./kiteclass/scripts/up.sh) and kiteclass-core
# reachable on $CORE_BASE_URL (default http://localhost:8081). The output file is
# gitignored — CI re-generates on every PR; this script is for local inspection.
#
set -euo pipefail

CORE_BASE_URL="${CORE_BASE_URL:-http://localhost:8081}"
SPEC_PATH="${CORE_API_DOCS_PATH:-/api-docs}"
OUTPUT="${OUTPUT_FILE:-$(dirname "$0")/../openapi.json}"
MIN_PATHS="${MIN_PATHS:-60}"

echo "→ Fetching OpenAPI spec from ${CORE_BASE_URL}${SPEC_PATH}"
if ! curl -fsS "${CORE_BASE_URL}${SPEC_PATH}" -o "${OUTPUT}.tmp"; then
  echo "✗ Failed to fetch spec. Is kiteclass-core running on ${CORE_BASE_URL}?" >&2
  echo "  Start it via: ./kiteclass/scripts/up.sh" >&2
  exit 1
fi

if ! jq empty "${OUTPUT}.tmp" 2>/dev/null; then
  echo "✗ Response is not valid JSON" >&2
  rm -f "${OUTPUT}.tmp"
  exit 1
fi

PATH_COUNT=$(jq '.paths | length' "${OUTPUT}.tmp")
if [ "${PATH_COUNT}" -lt "${MIN_PATHS}" ]; then
  echo "✗ Spec has ${PATH_COUNT} paths, expected at least ${MIN_PATHS} (34 controllers should expose more endpoints)" >&2
  rm -f "${OUTPUT}.tmp"
  exit 1
fi

mv "${OUTPUT}.tmp" "${OUTPUT}"
echo "✓ Wrote $(realpath "${OUTPUT}") — ${PATH_COUNT} paths"
