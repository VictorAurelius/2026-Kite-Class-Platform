#!/usr/bin/env bash
# backup-repo-snapshot — full local backup of kite-class-platform repo + state
#
# Creates compressed snapshot to ~/backups/ (or BACKUP_DIR if set).
# Skip heavy build artifacts (node_modules, target, .next) to keep tarball small.
# Preserves: .git history, all branches, all tags, 4 worktrees, all docs.
#
# Triggered by 2026-05-14 GitHub account suspension incident — pre-migration
# insurance per migration plan Phase 0.
#
# Usage:
#   bash scripts/backup-repo-snapshot.sh                    # default to ~/backups/
#   BACKUP_DIR=/mnt/external/backups bash scripts/...       # custom dir
#   VERIFY=1 bash scripts/...                               # extract + verify
#
# Output: ~/backups/kite-platform-backup-YYYYMMDD-HHMM.tar.gz + .sha256 + .meta

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"
VERIFY="${VERIFY:-0}"

if [[ ! -d "$REPO_ROOT/.git" ]]; then
  echo "ERROR: not inside a git repo (.git not found at $REPO_ROOT)" >&2
  exit 2
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M)"
TARBALL="$BACKUP_DIR/kite-platform-backup-$TIMESTAMP.tar.gz"
META_FILE="$BACKUP_DIR/kite-platform-backup-$TIMESTAMP.meta"
SHA_FILE="$BACKUP_DIR/kite-platform-backup-$TIMESTAMP.sha256"

cd "$(dirname "$REPO_ROOT")"
REPO_NAME="$(basename "$REPO_ROOT")"

echo "[1/5] Capturing repo metadata..."
{
  echo "# Kite Class Platform — backup metadata"
  echo "Timestamp: $TIMESTAMP"
  echo "Host: $(hostname)"
  echo "User: $(whoami)"
  echo "Repo path: $REPO_ROOT"
  echo ""
  echo "## Git state"
  echo "Current branch: $(git -C "$REPO_ROOT" branch --show-current)"
  echo "HEAD commit: $(git -C "$REPO_ROOT" rev-parse HEAD)"
  echo "HEAD commit msg: $(git -C "$REPO_ROOT" log -1 --pretty=%s)"
  echo ""
  echo "## Branches (local)"
  git -C "$REPO_ROOT" branch
  echo ""
  echo "## Branches (remote)"
  git -C "$REPO_ROOT" branch -r 2>/dev/null || echo "(no remote branches reachable — GitHub may be suspended)"
  echo ""
  echo "## Tags"
  git -C "$REPO_ROOT" tag | tail -20
  echo "Total tags: $(git -C "$REPO_ROOT" tag | wc -l)"
  echo ""
  echo "## Worktrees"
  git -C "$REPO_ROOT" worktree list
  echo ""
  echo "## Commit count"
  echo "All branches total: $(git -C "$REPO_ROOT" log --all --oneline | wc -l)"
  echo ""
  echo "## Disk usage (pre-backup)"
  du -sh "$REPO_ROOT" --exclude='node_modules' --exclude='target' --exclude='.next' 2>/dev/null || true
  echo ""
  echo "## Remotes"
  git -C "$REPO_ROOT" remote -v
} > "$META_FILE"

echo "[2/5] Creating compressed snapshot..."
echo "      Source: $REPO_ROOT"
echo "      Target: $TARBALL"
echo "      Excludes: node_modules, target, .next, *.log"

tar --create \
    --gzip \
    --file="$TARBALL" \
    --exclude="$REPO_NAME/**/node_modules" \
    --exclude="$REPO_NAME/**/target" \
    --exclude="$REPO_NAME/**/.next" \
    --exclude="$REPO_NAME/**/dist" \
    --exclude="$REPO_NAME/**/coverage" \
    --exclude="$REPO_NAME/**/.tsbuildinfo" \
    --exclude="$REPO_NAME/**/.cache" \
    --exclude="$REPO_NAME/**/*.log" \
    --exclude="$REPO_NAME/**/__pycache__" \
    --exclude="$REPO_NAME/**/.pytest_cache" \
    "$REPO_NAME"

TARBALL_SIZE="$(du -h "$TARBALL" | cut -f1)"
echo "      Size: $TARBALL_SIZE"

echo "[3/5] Computing SHA-256 checksum..."
sha256sum "$TARBALL" > "$SHA_FILE"
echo "      Checksum: $(cut -d' ' -f1 "$SHA_FILE")"

echo "[4/5] Updating metadata..."
{
  echo ""
  echo "## Backup artifact"
  echo "Tarball: $TARBALL"
  echo "Size: $TARBALL_SIZE"
  echo "SHA-256: $(cut -d' ' -f1 "$SHA_FILE")"
} >> "$META_FILE"

if [[ "$VERIFY" == "1" ]]; then
  echo "[5/5] Verifying tarball integrity..."
  TEMP_VERIFY="$(mktemp -d)"
  tar --extract --gzip --file="$TARBALL" --directory="$TEMP_VERIFY"
  EXTRACTED_FILES="$(find "$TEMP_VERIFY" -type f | wc -l)"
  EXTRACTED_BRANCHES="$(cd "$TEMP_VERIFY/$REPO_NAME" && git branch 2>/dev/null | wc -l || echo "0")"
  rm -rf "$TEMP_VERIFY"
  echo "      Files extracted: $EXTRACTED_FILES"
  echo "      Branches recoverable: $EXTRACTED_BRANCHES"
else
  echo "[5/5] Skipping verify (set VERIFY=1 to enable; takes ~30s extra)"
fi

echo ""
echo "============================================================"
echo "BACKUP COMPLETE"
echo "============================================================"
echo "Tarball:  $TARBALL ($TARBALL_SIZE)"
echo "Metadata: $META_FILE"
echo "Checksum: $SHA_FILE"
echo ""
echo "Next steps:"
echo "  1. Copy tarball to external location:"
echo "     cp $TARBALL /mnt/external/backups/    # external drive"
echo "     rclone copy $TARBALL gdrive:kite-backups/    # cloud (if rclone configured)"
echo "  2. Verify checksum on destination:"
echo "     sha256sum -c $SHA_FILE"
echo "  3. Keep 1 copy on different physical device (different SSD/laptop/cloud)"
echo ""
