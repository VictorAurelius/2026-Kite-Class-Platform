#!/usr/bin/env bash
# render-rst-screenshots.sh — annotate raw RST screenshots với mũi tên đỏ + viền vàng + số bước
#
# Wave meta-6 Bucket C — reusable tooling cho user manual screenshot annotation
# per `.claude/rules/user-manual-content-standard.md` §2 row 6 mandate.
#
# Usage:
#   bash scripts/render-rst-screenshots.sh \
#       --input  /tmp/rst-screenshots/wave-106-mang-a \
#       --output /tmp/rst-screenshots/wave-106-mang-a/annotated \
#       --manifest documents/04-quality/audits/rst-html/wave-106-mang-a/annotations.yaml
#
#   bash scripts/render-rst-screenshots.sh --self-test
#
# Manifest format (YAML):
#   annotations:
#     - file: a1-trang-chu.png
#       overlays:
#         - { kind: arrow,  x1: 100, y1: 50,  x2: 200, y2: 100, color: "#dc2626" }
#         - { kind: box,    x1: 80,  y1: 30,  x2: 220, y2: 130, color: "#facc15" }
#         - { kind: number, x:  90,  y:  120, value: 1 }
#
# Engine: ImageMagick `convert` HOẶC `magick` (v7) — script auto-detect.
# Khi engine không có: WARN exit 0, không block.
#
# Cross-link với annotation mandate cho user manual:
#   Output filenames bám slug ASCII (vd "trang-chu" KHÔNG có dấu tiếng Việt) để
#   Zalo share-friendly per `user-manual-content-standard.md` §2 row 6.
#
# Color tokens:
#   - Mũi tên đỏ: #dc2626 (per `user-manual-content-standard.md` §2 row 6)
#   - Viền vàng: #facc15
#   - Step number background: trắng / viền đen / chữ #dc2626

set -euo pipefail

# ---- Defaults ----
INPUT_DIR=""
OUTPUT_DIR=""
MANIFEST=""
SELF_TEST=0
VERBOSE=0

# ---- Color tokens ----
COLOR_ARROW="#dc2626"
COLOR_BOX="#facc15"
COLOR_STEP_BG="#ffffff"
COLOR_STEP_BORDER="#000000"
COLOR_STEP_TEXT="#dc2626"

usage() {
  cat <<'USAGE'
Usage:
  bash scripts/render-rst-screenshots.sh \
      --input <dir>     # raw PNG screenshots directory
      --output <dir>    # annotated PNG output directory
      --manifest <file> # YAML manifest specifying per-screenshot overlays

  bash scripts/render-rst-screenshots.sh --self-test
      # Generate 1 sample annotation manifest + verify script runs without error
      # on a dummy PNG generated trên-the-fly. Exit 0 dù ImageMagick missing.

Tham khảo:
  - `.claude/rules/user-manual-content-standard.md` §2 row 6 (mũi tên đỏ + viền vàng + số bước)
  - `documents/04-quality/audits/rst-html/wave-106-mang-a/README.md` (Wave 106 sample usage)
USAGE
}

# ---- Parse args ----
while [[ $# -gt 0 ]]; do
  case "$1" in
    --input)    INPUT_DIR="$2"; shift 2 ;;
    --output)   OUTPUT_DIR="$2"; shift 2 ;;
    --manifest) MANIFEST="$2"; shift 2 ;;
    --self-test) SELF_TEST=1; shift ;;
    --verbose|-v) VERBOSE=1; shift ;;
    -h|--help)  usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 1 ;;
  esac
done

log() {
  if [[ "$VERBOSE" -eq 1 ]]; then echo "[render-rst] $*"; fi
}

# ---- Detect ImageMagick engine ----
detect_engine() {
  if command -v magick >/dev/null 2>&1; then
    echo "magick"
  elif command -v convert >/dev/null 2>&1; then
    echo "convert"
  else
    echo ""
  fi
}

ENGINE=$(detect_engine)

if [[ -z "$ENGINE" ]]; then
  echo "WARN: ImageMagick không tìm thấy (cần 'magick' hoặc 'convert' trong PATH)." >&2
  echo "  Cài đặt: apt-get install imagemagick   # Debian/Ubuntu/WSL2" >&2
  echo "           brew install imagemagick      # macOS" >&2
  echo "  Bỏ qua annotation step; original screenshots vẫn dùng được." >&2
  if [[ "$SELF_TEST" -eq 1 ]]; then
    echo "[render-rst] --self-test: ImageMagick missing → graceful WARN exit 0 (per script contract)."
    exit 0
  fi
fi

# ---- Helper: parse manifest YAML (minimal, no full YAML parser) ----
# Format expected — strict but minimal:
#   annotations:
#     - file: <name.png>
#       overlays:
#         - { kind: arrow,  x1: N, y1: N, x2: N, y2: N }
#         - { kind: box,    x1: N, y1: N, x2: N, y2: N }
#         - { kind: number, x: N, y: N, value: N }
parse_manifest_and_render() {
  local manifest="$1"
  local input_dir="$2"
  local output_dir="$3"

  if [[ ! -f "$manifest" ]]; then
    echo "ERROR: Manifest file không tồn tại: $manifest" >&2
    return 1
  fi
  mkdir -p "$output_dir"

  local current_file=""
  local cmd_args=()
  local has_engine=0
  if [[ -n "$ENGINE" ]]; then has_engine=1; fi

  while IFS= read -r line; do
    # Strip trailing CR (in case file edited on Windows)
    line="${line%$'\r'}"

    if [[ "$line" =~ ^[[:space:]]*-[[:space:]]*file:[[:space:]]*(.+)$ ]]; then
      # Flush previous file
      if [[ -n "$current_file" && "$has_engine" -eq 1 && ${#cmd_args[@]} -gt 0 ]]; then
        flush_annotation "$input_dir/$current_file" "$output_dir/$current_file" "${cmd_args[@]}"
      fi
      current_file="${BASH_REMATCH[1]}"
      cmd_args=()
      log "Processing: $current_file"
      continue
    fi

    if [[ "$line" =~ \{[[:space:]]*kind:[[:space:]]*arrow ]]; then
      local x1 y1 x2 y2
      x1=$(echo "$line" | grep -oE 'x1:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      y1=$(echo "$line" | grep -oE 'y1:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      x2=$(echo "$line" | grep -oE 'x2:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      y2=$(echo "$line" | grep -oE 'y2:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      if [[ -n "$x1" && -n "$y1" && -n "$x2" && -n "$y2" ]]; then
        cmd_args+=("-stroke" "$COLOR_ARROW" "-strokewidth" "4" "-fill" "none")
        cmd_args+=("-draw" "line $x1,$y1 $x2,$y2")
        # Arrowhead: small triangle pointing to (x2,y2)
        local dx dy ahx1 ahy1 ahx2 ahy2
        dx=$((x2 - x1))
        dy=$((y2 - y1))
        ahx1=$((x2 - dx / 8))
        ahy1=$((y2 - dy / 8 - 8))
        ahx2=$((x2 - dx / 8 + 8))
        ahy2=$((y2 - dy / 8))
        cmd_args+=("-draw" "line $x2,$y2 $ahx1,$ahy1" "-draw" "line $x2,$y2 $ahx2,$ahy2")
      fi
      continue
    fi

    if [[ "$line" =~ \{[[:space:]]*kind:[[:space:]]*box ]]; then
      local x1 y1 x2 y2
      x1=$(echo "$line" | grep -oE 'x1:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      y1=$(echo "$line" | grep -oE 'y1:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      x2=$(echo "$line" | grep -oE 'x2:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      y2=$(echo "$line" | grep -oE 'y2:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      if [[ -n "$x1" && -n "$y1" && -n "$x2" && -n "$y2" ]]; then
        cmd_args+=("-stroke" "$COLOR_BOX" "-strokewidth" "4" "-fill" "none")
        cmd_args+=("-draw" "rectangle $x1,$y1 $x2,$y2")
      fi
      continue
    fi

    if [[ "$line" =~ \{[[:space:]]*kind:[[:space:]]*number ]]; then
      local x y value
      x=$(echo "$line" | grep -oE '(^|[^a-z])x:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      y=$(echo "$line" | grep -oE '(^|[^a-z])y:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      value=$(echo "$line" | grep -oE 'value:[[:space:]]*[0-9]+' | grep -oE '[0-9]+' | head -1)
      if [[ -n "$x" && -n "$y" && -n "$value" ]]; then
        # Draw white circle + black border + red number
        local r=20
        cmd_args+=("-fill" "$COLOR_STEP_BG" "-stroke" "$COLOR_STEP_BORDER" "-strokewidth" "2")
        cmd_args+=("-draw" "circle $x,$y $((x+r)),$y")
        cmd_args+=("-fill" "$COLOR_STEP_TEXT" "-stroke" "none" "-pointsize" "24" "-font" "DejaVu-Sans-Bold")
        cmd_args+=("-draw" "text $((x-7)),$((y+8)) '$value'")
      fi
      continue
    fi
  done < "$manifest"

  # Flush last file
  if [[ -n "$current_file" && "$has_engine" -eq 1 && ${#cmd_args[@]} -gt 0 ]]; then
    flush_annotation "$input_dir/$current_file" "$output_dir/$current_file" "${cmd_args[@]}"
  fi
}

flush_annotation() {
  local input_file="$1"
  local output_file="$2"
  shift 2
  if [[ ! -f "$input_file" ]]; then
    echo "WARN: Input not found, skipping: $input_file" >&2
    return 0
  fi
  log "Rendering: $output_file"
  if [[ "$ENGINE" == "magick" ]]; then
    magick "$input_file" "$@" "$output_file" || {
      echo "WARN: ImageMagick render failed cho $input_file" >&2
      return 0
    }
  else
    convert "$input_file" "$@" "$output_file" || {
      echo "WARN: ImageMagick render failed cho $input_file" >&2
      return 0
    }
  fi
}

# ---- Self-test ----
run_self_test() {
  echo "[render-rst] Self-test: tạo dummy PNG + manifest + chạy script."
  local tmp_dir
  tmp_dir=$(mktemp -d)
  log "Self-test dir: $tmp_dir"

  # Generate dummy 200×200 PNG (if engine available)
  if [[ -n "$ENGINE" ]]; then
    if [[ "$ENGINE" == "magick" ]]; then
      magick -size 200x200 xc:white "$tmp_dir/dummy.png" 2>/dev/null || true
    else
      convert -size 200x200 xc:white "$tmp_dir/dummy.png" 2>/dev/null || true
    fi
  fi

  # Write minimal manifest
  cat > "$tmp_dir/manifest.yaml" <<'EOF'
annotations:
  - file: dummy.png
    overlays:
      - { kind: arrow,  x1: 50, y1: 50, x2: 150, y2: 100 }
      - { kind: box,    x1: 30, y1: 30, x2: 170, y2: 120 }
      - { kind: number, x: 100, y: 80, value: 1 }
EOF

  if [[ ! -f "$tmp_dir/dummy.png" ]]; then
    echo "[render-rst] ImageMagick missing → skip render step (self-test passes parse logic only)."
    # Still verify manifest parse logic doesn't crash
    parse_manifest_and_render "$tmp_dir/manifest.yaml" "$tmp_dir" "$tmp_dir/out" || {
      echo "ERROR: parse logic crashed on minimal manifest." >&2
      rm -rf "$tmp_dir"
      return 1
    }
    echo "[render-rst] Self-test PASS (parse logic OK, render skipped do ImageMagick missing)."
    rm -rf "$tmp_dir"
    return 0
  fi

  parse_manifest_and_render "$tmp_dir/manifest.yaml" "$tmp_dir" "$tmp_dir/out"

  if [[ -f "$tmp_dir/out/dummy.png" ]]; then
    echo "[render-rst] Self-test PASS — annotated PNG at $tmp_dir/out/dummy.png"
    echo "[render-rst] Engine: $ENGINE"
    # Keep tmp_dir for inspection — user can rm manually
    echo "[render-rst] (tmp_dir preserved cho inspection: $tmp_dir)"
    return 0
  else
    echo "ERROR: Self-test failed — output PNG không được tạo." >&2
    rm -rf "$tmp_dir"
    return 1
  fi
}

# ---- Main ----
if [[ "$SELF_TEST" -eq 1 ]]; then
  run_self_test
  exit $?
fi

if [[ -z "$INPUT_DIR" || -z "$OUTPUT_DIR" || -z "$MANIFEST" ]]; then
  echo "ERROR: --input, --output, --manifest đều bắt buộc (trừ khi --self-test)." >&2
  usage
  exit 1
fi

if [[ ! -d "$INPUT_DIR" ]]; then
  echo "ERROR: Input directory không tồn tại: $INPUT_DIR" >&2
  exit 1
fi

if [[ -z "$ENGINE" ]]; then
  echo "INFO: ImageMagick không có; output sẽ trống. Cài đặt rồi rerun." >&2
  exit 0
fi

parse_manifest_and_render "$MANIFEST" "$INPUT_DIR" "$OUTPUT_DIR"
echo "[render-rst] Done. Output: $OUTPUT_DIR"
