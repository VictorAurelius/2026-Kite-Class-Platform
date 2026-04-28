# No-date fixture (README freshness self-test)

This file simulates a README without any `**Last Updated:**` line.

The `check-readme-freshness.sh` script must emit a `[WARN]` (advisory) for this
case — staleness can't be measured without a date, but absence isn't fatal in
default mode. `--strict` flag elevates the WARN to FAIL.

Used by: `scripts/check-readme-freshness.sh --self-test` (T5).
