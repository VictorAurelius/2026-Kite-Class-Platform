#!/usr/bin/env bash
#
# seed-demo-assets.sh — seed placeholder logo/cover/hero assets directly into MinIO
#
# GAP-805 Bucket C (2026-05-28): the StorageController upload path is fail-closed
# (blocked by GAP-798b), so demo tenants render broken image icons for branding
# assets (logo / cover / hero). This script bypasses the controller and puts
# generated gradient placeholders straight into the MinIO bucket so a demo /
# thesis walkthrough shows real images instead of broken-icon boxes.
#
# It seeds under the `mocks/assets/` prefix matching the paths the
# BrandingDataSeeder references (e.g. /mocks/assets/logo-thanglong.png).
#
# Usage:
#   bash scripts/seed-demo-assets.sh            # generate + upload placeholders (idempotent)
#   bash scripts/seed-demo-assets.sh --dry-run  # show what would be generated/uploaded, no write
#   bash scripts/seed-demo-assets.sh --cleanup  # remove seeded placeholder objects from MinIO
#   bash scripts/seed-demo-assets.sh --help     # show this help
#
# Assets generated (gradient SVG with centered Vietnamese label — no large binary
# is committed to the repo; the script generates them at runtime into a temp dir):
#
#   mocks/assets/logo-skyedu.svg     "Sky Education"          (square logo)
#   mocks/assets/cover-skyedu.svg    "Trung tâm Anh ngữ Sky"  (wide cover)
#   mocks/assets/hero-skyedu.svg     "Sky Education"          (wide hero)
#   mocks/assets/logo-quangminh.svg  "Toán Quang Minh"        (square logo)
#   mocks/assets/cover-quangminh.svg "Trung tâm Toán Quang Minh"
#   mocks/assets/hero-quangminh.svg  "Quang Minh"
#
# MinIO connection (matches kitehub/docker-compose.kitehub.yml):
#   container/alias : kite-minio  (endpoint http://kite-minio:9000)
#   bucket          : kitehub-assets
#   creds           : MINIO_ROOT_USER (default kitehub) / MINIO_ROOT_PASSWORD (required)
#
# How it uploads (in priority order):
#   1. `mc` available on host        → `mc cp` to the kite-minio endpoint
#   2. `docker` available            → run a throwaway `minio/mc` container on the
#                                       kite-network and `mc cp` from there
#   3. `aws` CLI available           → `aws s3 cp --endpoint-url ...`
#   If none available the script prints the manual `mc` commands and exits 1.
#
set -euo pipefail

# --- config -----------------------------------------------------------------
MINIO_ALIAS="kite"
MINIO_HOST_ENDPOINT="${MINIO_HOST_ENDPOINT:-http://localhost:9000}"   # host-reachable
MINIO_NET_ENDPOINT="http://kite-minio:9000"                           # docker-network reachable
MINIO_USER="${MINIO_ROOT_USER:-kitehub}"
MINIO_PASS="${MINIO_ROOT_PASSWORD:-}"
MINIO_BUCKET="${MINIO_BUCKET:-kitehub-assets}"
ASSET_PREFIX="mocks/assets"
DOCKER_NETWORK="${KITE_DOCKER_NETWORK:-kite-network}"

MODE="seed"

# asset spec: "object-name|label|width|height"
ASSETS=(
  "logo-skyedu.svg|Sky Education|400|400"
  "cover-skyedu.svg|Trung tâm Anh ngữ Sky Education|1200|400"
  "hero-skyedu.svg|Sky Education|1600|600"
  "logo-quangminh.svg|Toán Quang Minh|400|400"
  "cover-quangminh.svg|Trung tâm Toán Quang Minh|1200|400"
  "hero-quangminh.svg|Quang Minh|1600|600"
)

# --- helpers ----------------------------------------------------------------
log() { printf '%s\n' "$*" >&2; }

usage() {
  sed -n '2,40p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
}

# Generate a gradient SVG placeholder with a centered Vietnamese label.
# Args: <out-file> <label> <width> <height>
generate_svg() {
  local out="$1" label="$2" width="$3" height="$4"
  local font_size=$(( height / 8 ))
  [ "$font_size" -lt 28 ] && font_size=28
  cat > "$out" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
  <defs>
    <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#6366f1"/>
      <stop offset="100%" stop-color="#0ea5e9"/>
    </linearGradient>
  </defs>
  <rect width="${width}" height="${height}" fill="url(#g)"/>
  <text x="50%" y="50%" fill="#ffffff" font-family="sans-serif" font-size="${font_size}"
        font-weight="600" text-anchor="middle" dominant-baseline="middle">${label}</text>
</svg>
SVG
}

# --- arg parse --------------------------------------------------------------
case "${1:-}" in
  --help|-h) usage ;;
  --dry-run) MODE="dry-run" ;;
  --cleanup) MODE="cleanup" ;;
  "" )       MODE="seed" ;;
  *) log "Unknown arg: $1 (use --help)"; exit 2 ;;
esac

# --- detect uploader --------------------------------------------------------
UPLOADER=""
if command -v mc >/dev/null 2>&1; then
  UPLOADER="mc-host"
elif command -v docker >/dev/null 2>&1; then
  UPLOADER="mc-docker"
elif command -v aws >/dev/null 2>&1; then
  UPLOADER="aws"
fi

if [ "$MODE" != "dry-run" ] && [ -z "$UPLOADER" ]; then
  log "ERROR: no uploader found (need mc / docker / aws on PATH)."
  log ""
  log "Manual fallback — run inside the running kite-minio stack:"
  log "  docker exec kite-minio sh -c 'mc alias set $MINIO_ALIAS $MINIO_NET_ENDPOINT $MINIO_USER \$MINIO_ROOT_PASSWORD'"
  log "  # then for each generated SVG:"
  log "  docker cp <local.svg> kite-minio:/tmp/<obj>"
  log "  docker exec kite-minio mc cp /tmp/<obj> $MINIO_ALIAS/$MINIO_BUCKET/$ASSET_PREFIX/<obj>"
  exit 1
fi

if [ "$MODE" != "dry-run" ] && [ "$MODE" != "cleanup" ] && [ "$UPLOADER" != "mc-docker" ] && [ -z "$MINIO_PASS" ]; then
  log "ERROR: MINIO_ROOT_PASSWORD not set (export it or source your .env)."
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

# --- mc helpers (configure alias once per uploader) -------------------------
mc_host() { mc "$@"; }

mc_docker() {
  docker run --rm --network "$DOCKER_NETWORK" \
    -e "MC_HOST_${MINIO_ALIAS}=http://${MINIO_USER}:${MINIO_ROOT_PASSWORD}@kite-minio:9000" \
    -v "$TMP_DIR:/seed:ro" \
    minio/mc "$@"
}

configure_mc_host() {
  mc alias set "$MINIO_ALIAS" "$MINIO_HOST_ENDPOINT" "$MINIO_USER" "$MINIO_PASS" >/dev/null
}

# --- run --------------------------------------------------------------------
log "seed-demo-assets.sh — mode=$MODE bucket=$MINIO_BUCKET prefix=$ASSET_PREFIX uploader=${UPLOADER:-none}"

if [ "$UPLOADER" = "mc-host" ] && [ "$MODE" != "dry-run" ]; then
  configure_mc_host
fi

for spec in "${ASSETS[@]}"; do
  IFS='|' read -r obj label width height <<< "$spec"
  remote="$MINIO_ALIAS/$MINIO_BUCKET/$ASSET_PREFIX/$obj"

  case "$MODE" in
    dry-run)
      log "  [dry-run] generate ${width}x${height} gradient '${label}' -> $ASSET_PREFIX/$obj"
      ;;
    cleanup)
      log "  [cleanup] remove $ASSET_PREFIX/$obj"
      case "$UPLOADER" in
        mc-host)   mc_host rm --force "$remote" 2>/dev/null || true ;;
        mc-docker) mc_docker rm --force "$remote" 2>/dev/null || true ;;
        aws)       aws --endpoint-url "$MINIO_HOST_ENDPOINT" s3 rm "s3://$MINIO_BUCKET/$ASSET_PREFIX/$obj" 2>/dev/null || true ;;
      esac
      ;;
    seed)
      generate_svg "$TMP_DIR/$obj" "$label" "$width" "$height"
      log "  [seed] $ASSET_PREFIX/$obj  (${width}x${height}, '${label}')"
      case "$UPLOADER" in
        mc-host)   mc_host cp "$TMP_DIR/$obj" "$remote" >/dev/null ;;
        mc-docker) mc_docker cp "/seed/$obj" "$remote" >/dev/null ;;
        aws)
          AWS_ACCESS_KEY_ID="$MINIO_USER" AWS_SECRET_ACCESS_KEY="$MINIO_PASS" \
            aws --endpoint-url "$MINIO_HOST_ENDPOINT" s3 cp "$TMP_DIR/$obj" \
              "s3://$MINIO_BUCKET/$ASSET_PREFIX/$obj" --content-type "image/svg+xml" >/dev/null
          ;;
      esac
      ;;
  esac
done

log ""
case "$MODE" in
  dry-run) log "Dry-run complete — no objects written. Re-run without --dry-run to seed." ;;
  cleanup) log "Cleanup complete — placeholder objects removed from $MINIO_BUCKET/$ASSET_PREFIX." ;;
  seed)    log "Seed complete — ${#ASSETS[@]} placeholder assets in $MINIO_BUCKET/$ASSET_PREFIX (public-read bucket)." ;;
esac
