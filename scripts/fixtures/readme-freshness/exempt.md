# Exempt fixture (README freshness self-test)

<!-- readme-freshness-exempt: synthetic fixture, never updates by design -->

This file simulates a README that has explicitly opted out of freshness checking
via the `<!-- readme-freshness-exempt: <reason> -->` HTML comment.

The `check-readme-freshness.sh` script must classify it as `[EXEMPT]` regardless
of any `**Last Updated:**` line value (or absence thereof).

Used by: `scripts/check-readme-freshness.sh --self-test` (T4).
