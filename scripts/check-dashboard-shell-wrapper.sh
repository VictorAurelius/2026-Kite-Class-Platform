#!/usr/bin/env bash
#
# check-dashboard-shell-wrapper.sh — detect (dashboard) page.tsx missing shell chrome
#
# Convention (GAP-1071): every kiteclass-frontend `(dashboard)/**/page.tsx` must
# self-wrap a shell component so the page renders with header/sidebar/footer chrome.
# Accepted shells:
#   - <DashboardLayout>   (admin/owner desktop — @/components/layout)
#   - <ParentShell>       (parent mobile PWA shell)
#   - <StudentMobileShell>(student mobile shell)
#   - <TeacherShell>      (teacher shell)
#   - <MobileShell>       (generic mobile shell, if any)
#
# A page may opt out of shell chrome (full-screen wizard / print view / redirect-only)
# by adding a marker comment anywhere in the file:
#   // shell-exempt: <reason>
#
# A page that wraps NO shell AND has NO shell-exempt marker => WARN finding.
#
# WARN-mode: always exit 0 (advisory). Coordinator wires the CI job; HARD-STOP
# deferred until 2nd recurrence per incident-to-rule-pipeline premature-rule guard.
#
# Usage: bash scripts/check-dashboard-shell-wrapper.sh
#
set -euo pipefail

# Resolve repo root (script lives in scripts/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

DASHBOARD_DIR="$REPO_ROOT/kiteclass/kiteclass-frontend/src/app/(dashboard)"

# Shell components that satisfy the "page wraps chrome" requirement.
SHELL_PATTERN='<(DashboardLayout|ParentShell|StudentMobileShell|TeacherShell|MobileShell)\b'
# Opt-out marker.
EXEMPT_PATTERN='//[[:space:]]*shell-exempt:'

if [[ ! -d "$DASHBOARD_DIR" ]]; then
  echo "check-dashboard-shell-wrapper: (dashboard) dir not found at $DASHBOARD_DIR — skipping" >&2
  exit 0
fi

findings=0
scanned=0
exempt=0

# NUL-delimited to be safe with any odd paths (none expected, but robust).
while IFS= read -r -d '' page; do
  scanned=$((scanned + 1))
  rel="${page#"$REPO_ROOT/"}"

  if grep -qE "$EXEMPT_PATTERN" "$page"; then
    exempt=$((exempt + 1))
    continue
  fi

  if grep -qE "$SHELL_PATTERN" "$page"; then
    continue
  fi

  echo "WARN: missing shell wrapper (no <DashboardLayout/ParentShell/StudentMobileShell/TeacherShell> and no '// shell-exempt:' marker)"
  echo "      $rel"
  findings=$((findings + 1))
done < <(find "$DASHBOARD_DIR" -type f -name 'page.tsx' -print0)

echo ""
echo "check-dashboard-shell-wrapper: scanned=$scanned shell-exempt=$exempt findings=$findings"

if [[ "$findings" -gt 0 ]]; then
  echo "Above page(s) render bare (no header/sidebar/footer). FIX: wrap return in the role-appropriate shell," >&2
  echo "or add '// shell-exempt: <reason>' if full-screen by design. See GAP-1071." >&2
fi

# WARN-mode: do not fail the build.
exit 0
