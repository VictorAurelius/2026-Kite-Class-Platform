/**
 * K6 Load Test — Cohort 50 Signup Baseline
 *
 * Purpose: Verify hệ thống chịu được 50 concurrent users qua signup flow
 *   trước khi tag v1.0.0-rc.1 + Phase 1.5 invite ≥20 tenants.
 *
 * Wave 86 Bucket H H-AC1 — paired same PR với
 *   documents/05-guides/operations/load-test-k6-cohort-50-baseline.md
 *
 * Run:
 *   export K6_BASE_URL=https://app.kitehub.me
 *   export K6_TEST_EMAIL_DOMAIN=test.kitehub.me
 *   k6 run infrastructure/load-test/k6-cohort-50-signup.js
 *
 * Pass criteria (per runbook §4):
 *   - http_req_duration p(95) < 3000ms
 *   - http_req_failed rate < 0.01 (1%)
 *   - checks rate > 0.99 (99%)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
const signupSuccessRate = new Rate('signup_success');

export const options = {
  scenarios: {
    cohort_50_signup: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },  // Ramp up to 50 VUs over 2 min
        { duration: '5m', target: 50 },  // Hold 50 VUs for 5 min
        { duration: '2m', target: 0 },   // Ramp down over 2 min
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_duration{expected_response:true}': ['p(95)<3000'],  // P95 < 3s
    'http_req_failed': ['rate<0.01'],                              // < 1% errors
    'signup_success': ['rate>0.99'],                                // > 99% success
    'checks': ['rate>0.99'],                                        // > 99% checks pass
  },
};

const BASE_URL = __ENV.K6_BASE_URL || 'https://app.kitehub.me';
const TEST_EMAIL_DOMAIN = __ENV.K6_TEST_EMAIL_DOMAIN || 'test.kitehub.me';

// Sample data — VN-friendly per user-manual-content-standard.md §2 row 7
const SAMPLE_NAMES = [
  'Nguyễn Văn An', 'Trần Thị Hồng', 'Phạm Thị Mai', 'Lê Văn Bình',
  'Hoàng Thị Lan', 'Vũ Văn Đức', 'Đỗ Thị Hương', 'Bùi Văn Cường',
];
const SAMPLE_ORGS = [
  'Trung tâm Anh ngữ Sky Education', 'Trung tâm Toán Quang Minh',
  'Trung tâm Tiếng Anh Sao Mai', 'Trung tâm Bồi dưỡng Văn hóa Ánh Dương',
  'Trung tâm Anh ngữ Hà Nội', 'Trung tâm Toán Tài Năng',
];

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

export default function () {
  const vuId = __VU;
  const iterId = __ITER;
  const email = `k6-cohort-${vuId}-${iterId}-${Date.now()}@${TEST_EMAIL_DOMAIN}`;
  const payload = JSON.stringify({
    email: email,
    name: pick(SAMPLE_NAMES),
    organization: pick(SAMPLE_ORGS),
    source: 'k6-load-test-cohort-50',
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'User-Agent': 'k6-cohort-50-signup-baseline/1.0',
    },
    tags: { name: 'request-beta-access' },
  };

  const res = http.post(`${BASE_URL}/api/v1/auth/request-beta-access`, payload, params);

  const success = check(res, {
    'status is 201': (r) => r.status === 201,
    'response has id': (r) => {
      try {
        return JSON.parse(r.body).id !== undefined;
      } catch (e) {
        return false;
      }
    },
    'response time < 5s': (r) => r.timings.duration < 5000,
  });
  signupSuccessRate.add(success);

  // Realistic think time — user reading form/confirmation page
  sleep(1 + Math.random() * 2);
}

export function handleSummary(data) {
  const date = new Date().toISOString().split('T')[0];
  const summary = {
    timestamp: data.timestamp,
    duration_s: Math.round(data.state.testRunDurationMs / 1000),
    iterations: data.metrics.iterations.values.count,
    vus_max: data.metrics.vus_max.values.max,
    p95_ms: Math.round(data.metrics.http_req_duration.values['p(95)']),
    error_rate: data.metrics.http_req_failed.values.rate,
    signup_success_rate: data.metrics.signup_success ? data.metrics.signup_success.values.rate : null,
    checks_passed: data.metrics.checks.values.passes,
    checks_failed: data.metrics.checks.values.fails,
    verdict: (
      data.metrics.http_req_duration.values['p(95)'] < 3000 &&
      data.metrics.http_req_failed.values.rate < 0.01
    ) ? 'PASS' : 'FAIL',
  };

  return {
    'stdout': `\nCohort 50 Signup Baseline — ${date}\n` +
              `Verdict: ${summary.verdict}\n` +
              `P95: ${summary.p95_ms}ms (target <3000ms)\n` +
              `Error rate: ${(summary.error_rate * 100).toFixed(2)}% (target <1%)\n` +
              `Iterations: ${summary.iterations} | VUs max: ${summary.vus_max}\n` +
              `Checks: ${summary.checks_passed} passed / ${summary.checks_failed} failed\n\n`,
    [`infrastructure/load-test/results/cohort-50-${date}.json`]: JSON.stringify(summary, null, 2),
  };
}
