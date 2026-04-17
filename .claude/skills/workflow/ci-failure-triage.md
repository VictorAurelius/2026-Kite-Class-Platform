---
name: ci-failure-triage
description: "Dùng khi CI fail, 'CI đỏ', 'build failed', 'tests broken', 'workflow fail', 'tại sao CI fail'. Systematic triage: identify → classify → fix."
user-invocable: true
---

# /ci-failure-triage — CI Failure Triage

## Process

1. **Get failed run:**
   ```bash
   gh run list --branch $(git branch --show-current) --status failure --limit 1 --json databaseId,workflowName --jq '.[0]'
   ```
2. **Get failed log:**
   ```bash
   gh run view <id> --log-failed 2>/dev/null | tail -50
   ```
3. **Classify** theo error pattern → suggested action (xem bảng dưới)
4. **Fix locally** → verify pass → push

## Error Classification (project-specific)

| Pattern in log | Type | Fix |
|----------------|------|-----|
| `UnnecessaryStubbingException` | Test | Add `lenient()` or remove unused stub |
| `constructor cannot be applied` | Compilation | New dependency added — update test constructor |
| `Not a managed type` | Config | `@EntityScan` missing package in test config |
| `RabbitTemplate bean not found` | Config | Add `@MockitoBean RabbitTemplate` in `@SpringBootTest` |
| `APPLICATION FAILED TO START` | Config | Missing bean or property — check `application-test.yml` |
| `Tests run:.*Failures: [1-9]` | Test | Read assertion message, check expected vs actual |
| `OOM\|heap space\|GC overhead` | Infra | Runner memory issue — re-run, or reduce test parallelism |
| `timeout\|timed out` | Infra | Flaky network/runner — re-run first |
| `Could not resolve dependencies` | Deps | Maven/npm version conflict — check pom.xml or package.json |
| `error TS` | Compilation | TypeScript error — read file:line from log |

## Gotchas

- Dùng `scripts/check-ci.sh <branch>` để monitor — KHÔNG poll thủ công
- Flaky test fail intermittently → re-run 1 lần trước khi debug code
- Sau fix CI, cleanup failed runs: `bash scripts/cleanup-ci-runs.sh`
- `@SpringBootTest` tests cần `application-test.yml` — thiếu config = startup fail
- Multi-module Maven: `-pl <module> -am` để test chỉ module bị lỗi (nhanh hơn full build)
