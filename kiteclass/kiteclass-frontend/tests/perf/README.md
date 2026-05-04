# Performance tests

Wave 18b3 Bucket A (GAP-323b Phase 1B remainder).

## attendance-period-concurrent.k6.ts

Validates BR-PERIOD-ATT-008 §note ("30 GVCN concurrent ≤ 2 min") for the
K-12 per-tiết batch upsert endpoint shipped in PR #769.

### Why k6 and not Playwright

- 30 concurrent virtual users is a load-test profile, not a UI scenario
- k6's `ramping-vus` executor + `http_req_duration` thresholds are
  purpose-built; Playwright would need a custom multi-tab harness
- Output is Grafana-compatible if we want to plot p50/p95/p99 over time

### Pre-requisites for a real run

1. Backend stack up locally:
   ```
   ./kitehub/scripts/up.sh
   ```
   (Or any environment serving `kiteclass-core` on `localhost:8080`.)

2. Tenant + class + subject section seeded (see fixtures in
   `kitehub/scripts/seed-*.sh`).

3. `k6` binary installed:
   ```
   # macOS
   brew install k6
   # Linux
   sudo gpg -k
   sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update && sudo apt-get install k6
   ```

### Run

From `kiteclass/kiteclass-frontend/`:

```
k6 run tests/perf/attendance-period-concurrent.k6.ts \
  --env BASE_URL=http://localhost:8080 \
  --env TENANT_ID=demo-tenant \
  --env TEACHER_ID=42 \
  --env CLASS_ID=7 \
  --env SUBJECT_SECTION_ID=21
```

Defaults (no env): `BASE_URL=http://localhost:8080`,
`TENANT_ID=demo-tenant`, `TEACHER_ID=42`, `CLASS_ID=7`,
`SUBJECT_SECTION_ID=21`, `STUDENTS_PER_BATCH=40`.

### Expected output (PASS)

```
checks.........................: 100.00% ✓ <count> ✗ 0
http_req_duration..............: avg=<X>ms  med=<X>ms  p(95)=<<2000>ms
http_req_failed................: 0.00%
http_reqs......................: <N> requests
iteration_duration.............: avg=<X>s

✓ http_req_duration..............: p(95)<2000
✓ http_req_failed................: rate<0.01
✓ checks.........................: rate>0.99
```

### Interpreting results

- **PASS** (all 3 thresholds green) → the endpoint clears the
  BR-PERIOD-ATT-008 SLA at the documented load profile
- **FAIL on p95** → batch upsert needs index review (V50 unique tuple
  index is the suspect, plus any join over `subject_section`)
- **FAIL on http_req_failed** → server-side errors at sustained
  concurrency; check optimistic-lock retry rate and tenant interceptor

### Live-run status (Wave 18b3 Bucket A, 2026-05-04)

The script committed by Bucket A is **not** run live in the agent
environment because the kiteclass-core backend stack is not booted
during `pnpm test` / agent worktree work. The run is queued for a
post-merge follow-up by the wave coordinator (or for whoever first
needs the SLA validated). The script itself is exercised by
`grep -F 'export default function'` so it parses; full execution
requires an environment with the prerequisites above.
