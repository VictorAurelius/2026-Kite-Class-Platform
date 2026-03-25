#!/bin/bash
# render-diagrams.sh — Render diagram source files to PNG
#
# Supports: PlantUML (.puml), Mermaid (.mmd)
# Auto-detects available tools
#
# Usage:
#   ./scripts/render-diagrams.sh              # Render all
#   ./scripts/render-diagrams.sh --check      # Check tools only
#   ./scripts/render-diagrams.sh file.puml    # Render single file

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
DIAGRAM_DIR="$ROOT_DIR/documents/06-diagrams"
SOURCE_DIR="$DIAGRAM_DIR/source"
RENDERED_DIR="$DIAGRAM_DIR/rendered"
TOOLS_DIR="$DIAGRAM_DIR/tools"

# Also check legacy paths (plantuml/ instead of source/)
[ ! -d "$SOURCE_DIR" ] && SOURCE_DIR="$DIAGRAM_DIR/plantuml"

mkdir -p "$RENDERED_DIR"

RENDERED=0
FAILED=0
SKIPPED=0

# ─── Check tools ───
HAS_PLANTUML=false
HAS_MERMAID=false

PLANTUML_JAR="$TOOLS_DIR/plantuml.jar"
[ ! -f "$PLANTUML_JAR" ] && PLANTUML_JAR="/tmp/plantuml.jar"

if [ -f "$PLANTUML_JAR" ] && command -v java &>/dev/null; then
    HAS_PLANTUML=true
fi

if command -v mmdc &>/dev/null; then
    HAS_MERMAID=true
fi

if [ "${1:-}" = "--check" ]; then
    echo "Diagram tools:"
    $HAS_PLANTUML && echo "  PlantUML: ✅ ($PLANTUML_JAR)" || echo "  PlantUML: ❌ (need Java + plantuml.jar)"
    $HAS_MERMAID && echo "  Mermaid:  ✅ (mmdc)" || echo "  Mermaid:  ❌ (npm install -g @mermaid-js/mermaid-cli)"
    echo ""
    echo "Source dir: $SOURCE_DIR"
    echo "Output dir: $RENDERED_DIR"
    [ -d "$SOURCE_DIR" ] && echo "Files: $(find "$SOURCE_DIR" -name "*.puml" -o -name "*.mmd" 2>/dev/null | wc -l)" || echo "Source dir not found"
    exit 0
fi

if [ ! -d "$SOURCE_DIR" ]; then
    echo "Source directory not found: $SOURCE_DIR"
    echo "Create it: mkdir -p documents/06-diagrams/source"
    exit 1
fi

echo "═══════════════════════════════════════════════"
echo "  Render Diagrams"
echo "═══════════════════════════════════════════════"
echo ""

# ─── Single file mode ───
if [ -n "${1:-}" ] && [ -f "$1" ]; then
    FILE="$1"
    BASENAME=$(basename "$FILE" | sed 's/\.[^.]*$//')
    case "$FILE" in
        *.puml)
            if $HAS_PLANTUML; then
                java -jar "$PLANTUML_JAR" -tpng -o "$RENDERED_DIR" "$FILE" && echo "✅ $BASENAME.png" || echo "❌ $BASENAME"
            else
                echo "❌ PlantUML not available"
            fi
            ;;
        *.mmd)
            if $HAS_MERMAID; then
                mmdc -i "$FILE" -o "$RENDERED_DIR/$BASENAME.png" && echo "✅ $BASENAME.png" || echo "❌ $BASENAME"
            else
                echo "❌ Mermaid CLI not available"
            fi
            ;;
    esac
    exit 0
fi

# ─── Render all PlantUML ───
PUML_FILES=$(find "$SOURCE_DIR" -name "*.puml" 2>/dev/null | sort)
if [ -n "$PUML_FILES" ]; then
    if $HAS_PLANTUML; then
        echo "PlantUML:"
        while IFS= read -r file; do
            basename=$(basename "$file" .puml)
            if java -jar "$PLANTUML_JAR" -tpng -o "$RENDERED_DIR" "$file" 2>/dev/null; then
                echo "  ✅ $basename.png"
                ((RENDERED++))
            else
                echo "  ❌ $basename (render failed)"
                ((FAILED++))
            fi
        done <<< "$PUML_FILES"
    else
        count=$(echo "$PUML_FILES" | wc -l)
        echo "PlantUML: ⏭️  $count files skipped (Java or plantuml.jar not found)"
        echo "  Install: curl -L -o $TOOLS_DIR/plantuml.jar https://github.com/plantuml/plantuml/releases/latest/download/plantuml.jar"
        SKIPPED=$count
    fi
    echo ""
fi

# ─── Render all Mermaid ───
MMD_FILES=$(find "$SOURCE_DIR" -name "*.mmd" 2>/dev/null | sort)
if [ -n "$MMD_FILES" ]; then
    if $HAS_MERMAID; then
        echo "Mermaid:"
        while IFS= read -r file; do
            basename=$(basename "$file" .mmd)
            if mmdc -i "$file" -o "$RENDERED_DIR/$basename.png" 2>/dev/null; then
                echo "  ✅ $basename.png"
                ((RENDERED++))
            else
                echo "  ❌ $basename (render failed)"
                ((FAILED++))
            fi
        done <<< "$MMD_FILES"
    else
        count=$(echo "$MMD_FILES" | wc -l)
        echo "Mermaid: ⏭️  $count files skipped (mmdc not found)"
        echo "  Install: npm install -g @mermaid-js/mermaid-cli"
        SKIPPED=$count
    fi
    echo ""
fi

echo "═══════════════════════════════════════════════"
echo "  ✅ Rendered: $RENDERED  ❌ Failed: $FAILED  ⏭️ Skipped: $SKIPPED"
echo "═══════════════════════════════════════════════"

if [ $RENDERED -gt 0 ]; then
    echo ""
    echo "  Don't forget: git add documents/06-diagrams/"
fi

[ $FAILED -gt 0 ] && exit 1 || exit 0
