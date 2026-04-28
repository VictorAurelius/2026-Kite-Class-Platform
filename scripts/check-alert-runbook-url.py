#!/usr/bin/env python3
"""check-alert-runbook-url — verify every Prometheus alert has a runbook_url annotation.

Closes GAP-122 Slice A (CI gate paired with Slice B alerts + Slice C runbooks).
Spec lives in `documents/03-planning/waves/wave-2026-04-28-gap-122-platform-alerts.md`
("Annotation contract" section) — every `- alert: <Name>` block MUST contain a
non-empty `runbook_url:` annotation pointing at the matching runbook stub.

Why regex (no PyYAML):
    Helm-templated PrometheusRule files (e.g.
    `infrastructure/helm/kitehub/templates/prometheusrule.yaml`) embed
    `{{- if ... }}` directives + escaped `{{ "{{ $labels.X }}" }}` braces
    that PyYAML's safe_load rejects. We line-scan instead — pure stdlib, no
    template rendering needed.

Exit codes:
    0 = all alerts in scanned files have non-empty runbook_url
    1 = ≥1 alert missing or empty runbook_url
    2 = invocation error (missing file, bad arg, self-test fixtures gone)

Flags:
    --self-test    Run against `scripts/fixtures/alert-runbook-url/`; assert
                   `good-*` fixtures pass and `bad-*` fixtures fail. Used by
                   the `alert-runbook-url` job in `.github/workflows/script-quality.yml`.
    --paths FILE.. Validate listed files instead of the default scan set.
                   The CI job passes this when only specific alert files
                   changed.
    -h/--help      Show this header docstring.

Default scan set (when --paths not given):
    - kitehub/docker/prometheus/alert-rules.yml
    - kiteclass/docker/prometheus/alert-rules.yml
    - infrastructure/helm/kitehub/templates/prometheusrule.yaml

Used by:
    - `.github/workflows/script-quality.yml` job `alert-runbook-url` (PR gate)
    - manual local run before opening alert-related PR
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
FIXTURES_DIR = REPO_ROOT / "scripts" / "fixtures" / "alert-runbook-url"
DEFAULT_PATHS = [
    REPO_ROOT / "kitehub" / "docker" / "prometheus" / "alert-rules.yml",
    REPO_ROOT / "kiteclass" / "docker" / "prometheus" / "alert-rules.yml",
    REPO_ROOT / "infrastructure" / "helm" / "kitehub" / "templates" / "prometheusrule.yaml",
]

# Match `- alert: <Name>` capturing the indent + alert name + line number.
# Indent is leading spaces only (YAML doesn't allow tabs).
_ALERT_RE = re.compile(r"^(?P<indent>[ ]*)-\s+alert:\s*(?P<name>\S+)")
# Match a `runbook_url:` line at any indent. Captures the value (may be quoted).
_RUNBOOK_RE = re.compile(r"^[ ]*runbook_url:\s*(?P<value>.*?)\s*$")
# Sentinels that terminate an alert block: next `- alert:` or new `- name:` group.
_NEXT_ALERT_RE = re.compile(r"^[ ]*-\s+alert:\s*\S+")
_NEXT_GROUP_RE = re.compile(r"^[ ]*-\s+name:\s*\S+")


def _strip_quotes(raw: str) -> str:
    """Return value with surrounding single/double quotes removed if matched."""
    raw = raw.strip()
    if len(raw) >= 2 and raw[0] == raw[-1] and raw[0] in ("'", '"'):
        return raw[1:-1]
    return raw


def _scan_alerts(file_path: Path) -> list[tuple[int, str, str | None]]:
    """Return list of (line_number, alert_name, runbook_url_or_None) tuples.

    runbook_url_or_None is the parsed value if present and non-empty, else None.
    """
    findings: list[tuple[int, str, str | None]] = []
    text = file_path.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    n = len(lines)

    i = 0
    while i < n:
        m = _ALERT_RE.match(lines[i])
        if not m:
            i += 1
            continue

        alert_name = m.group("name")
        alert_lineno = i + 1  # 1-indexed for error reporting
        # Scan forward until next alert or next group at same/lower indent.
        runbook_value: str | None = None
        j = i + 1
        while j < n:
            line = lines[j]
            if _NEXT_ALERT_RE.match(line) or _NEXT_GROUP_RE.match(line):
                break
            rb = _RUNBOOK_RE.match(line)
            if rb:
                value = _strip_quotes(rb.group("value"))
                runbook_value = value if value else None
                break
            j += 1

        findings.append((alert_lineno, alert_name, runbook_value))
        i = j  # resume scan from where we stopped (next alert/group)

    return findings


def validate_file(file_path: Path) -> tuple[int, int, list[str]]:
    """Validate one file. Return (alert_count, fail_count, fail_messages)."""
    if not file_path.is_file():
        return 0, 1, [f"FAIL {file_path}:0 file not found"]

    findings = _scan_alerts(file_path)
    fail_msgs: list[str] = []
    for lineno, name, runbook in findings:
        if runbook is None:
            fail_msgs.append(
                f"FAIL {file_path}:{lineno} {name} missing or empty runbook_url"
            )
    return len(findings), len(fail_msgs), fail_msgs


def run_default_scan(paths: list[Path]) -> int:
    """Validate the listed paths. Print summary. Return exit code."""
    total_alerts = 0
    total_fails = 0
    files_with_fails: list[Path] = []

    for p in paths:
        if not p.exists():
            # Not a hard error if a default path is missing on a branch that
            # hasn't created it yet — just note and skip.
            print(f"SKIP {p} (not present)")
            continue
        n_alerts, n_fails, msgs = validate_file(p)
        total_alerts += n_alerts
        total_fails += n_fails
        if n_fails > 0:
            files_with_fails.append(p)
            for msg in msgs:
                print(msg)
        else:
            print(f"PASS {p} ({n_alerts} alert(s))")

    print()
    print("Alert runbook_url check")
    print("───────────────────────")
    print(f"  Files scanned:  {len([p for p in paths if p.exists()])}")
    print(f"  Alerts scanned: {total_alerts}")
    print(f"  Failures:       {total_fails}")
    if total_fails > 0:
        print(f"  ✗ Files with missing/empty runbook_url: {len(files_with_fails)}")
        return 1
    print("  ✓ All alerts have non-empty runbook_url annotations.")
    return 0


def run_self_test() -> int:
    """Run against fixtures dir; assert good-* PASS, bad-* FAIL. Return exit code."""
    if not FIXTURES_DIR.is_dir():
        print(f"Self-test ERROR: fixtures dir missing: {FIXTURES_DIR}", file=sys.stderr)
        return 2

    print(f"Self-test mode — running against fixtures in {FIXTURES_DIR}")
    print("─────────────────────────────────────────────────────────")
    passed = 0
    failed = 0

    fixtures = sorted(FIXTURES_DIR.glob("*.yml")) + sorted(FIXTURES_DIR.glob("*.yaml"))
    for fixture in fixtures:
        name = fixture.name
        if name.startswith("good"):
            expect = "PASS"
        elif name.startswith("bad"):
            expect = "FAIL"
        else:
            print(f"  SKIP {name} (unrecognized prefix; expected good*/bad*)")
            continue

        _, n_fails, msgs = validate_file(fixture)
        actual = "FAIL" if n_fails > 0 else "PASS"
        if actual == expect:
            print(f"  ✓ {name:<40s} expected={expect} got={actual}")
            passed += 1
        else:
            print(f"  ✗ {name:<40s} expected={expect} got={actual}")
            for m in msgs:
                print(f"    {m}")
            failed += 1

    print()
    print(f"Self-test summary: {passed} passed, {failed} failed")
    return 1 if failed > 0 else 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="check-alert-runbook-url",
        description=(
            "Verify every Prometheus alert has a non-empty runbook_url "
            "annotation. Tolerates Helm-templated YAML."
        ),
    )
    parser.add_argument(
        "--self-test",
        action="store_true",
        help="Run against scripts/fixtures/alert-runbook-url/ and assert PASS/FAIL.",
    )
    parser.add_argument(
        "--paths",
        nargs="+",
        metavar="FILE",
        help="Validate the given files instead of the default scan set.",
    )

    args = parser.parse_args(argv)

    if args.self_test:
        return run_self_test()

    if args.paths:
        paths = [Path(p) for p in args.paths]
    else:
        paths = DEFAULT_PATHS

    return run_default_scan(paths)


if __name__ == "__main__":
    sys.exit(main())
