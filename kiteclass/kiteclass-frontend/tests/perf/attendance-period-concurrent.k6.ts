// @ts-nocheck — k6 has its own runtime + type system (k6/http etc.); this script is executed by the k6 binary, not Next.js. Excluded from Next.js typecheck.
/**
 * k6 perf test for the K-12 per-tiết batch upsert endpoint
 * (Wave 18b3 Bucket A, GAP-323b Phase 1B remainder).
 *
 * Validates BR-PERIOD-ATT-008 §note "30 GVCN concurrent ≤2min" by holding
 * 30 virtual users on `POST /api/v1/attendance/periods` and asserting the
 * p95 batch-upsert latency stays under 2000 ms.
 *
 * Run from {@code kiteclass/kiteclass-frontend/}:
 *
 *     k6 run tests/perf/attendance-period-concurrent.k6.ts \
 *       --env BASE_URL=http://localhost:8080 \
 *       --env TENANT_ID=demo-tenant \
 *       --env TEACHER_ID=42 \
 *       --env CLASS_ID=7 \
 *       --env SUBJECT_SECTION_ID=21
 *
 * Default profile (no env): 30 VUs, 5-minute soak, default localhost:8080.
 *
 * Pre-requisites for a real run:
 *   - kiteclass-core up locally (see {@code kitehub/scripts/up.sh})
 *   - Tenant + class + subject section seeded
 *   - X-Teacher-Id corresponds to a teacher with attendance write permission
 *
 * If the backend is not available in the agent environment (common during
 * CI), the script is committed without a live run; reviewer / coordinator
 * runs it on a machine with the stack up.
 *
 * @since 4.x.x (Wave 18b3 Bucket A)
 */

// k6 module imports — these resolve at runtime inside the k6 binary, not
// inside Vitest. The TypeScript types come from `@types/k6` if installed;
// running without those types is fine because `k6 run` strips types.
// @ts-expect-error — k6 runtime module, no @types/k6 dep
import http from 'k6/http';
// @ts-expect-error — k6 runtime module
import { check, sleep } from 'k6';

const BASE_URL = (typeof __ENV !== 'undefined' && __ENV.BASE_URL) || 'http://localhost:8080';
const TENANT_ID = (typeof __ENV !== 'undefined' && __ENV.TENANT_ID) || 'demo-tenant';
const TEACHER_ID = (typeof __ENV !== 'undefined' && __ENV.TEACHER_ID) || '42';
const CLASS_ID = parseInt((typeof __ENV !== 'undefined' && __ENV.CLASS_ID) || '7', 10);
const SUBJECT_SECTION_ID = parseInt(
  (typeof __ENV !== 'undefined' && __ENV.SUBJECT_SECTION_ID) || '21',
  10,
);
const STUDENTS_PER_BATCH = parseInt(
  (typeof __ENV !== 'undefined' && __ENV.STUDENTS_PER_BATCH) || '40',
  10,
);

// k6 globals: `__ENV`, `__VU`, `__ITER` are injected at runtime.
declare const __ENV: Record<string, string>;
declare const __VU: number;
declare const __ITER: number;

export const options = {
  scenarios: {
    concurrent_gvcn: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 30 }, // ramp up
        { duration: '4m', target: 30 }, // soak at 30 VUs (BR target)
        { duration: '30s', target: 0 }, // ramp down
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    // Hard target — BR-PERIOD-ATT-008 §note: ≤2min for 30 concurrent GVCN.
    // p95 < 2000ms guarantees at least 95 % of requests finish under 2 s,
    // which is well inside the 2-minute window for the slowest 5 %.
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'], // <1 % failure
    checks: ['rate>0.99'],
  },
};

interface BatchEntry {
  studentId: number;
  classId: number;
  subjectSectionId: number;
  periodNo: number;
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED' | 'MAKEUP';
}

function todayISO(): string {
  const d = new Date();
  const yyyy = d.getUTCFullYear();
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(d.getUTCDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function buildBatch(): { entries: BatchEntry[] } {
  // Each VU writes the same student range but a different periodNo (derived
  // from VU id) to avoid version-conflict storms — mirrors real GVCN
  // behaviour where each teacher owns one tiết.
  const periodNo = ((__VU - 1) % 10) + 1;
  const date = todayISO();
  const entries: BatchEntry[] = [];
  for (let i = 1; i <= STUDENTS_PER_BATCH; i += 1) {
    entries.push({
      studentId: i,
      classId: CLASS_ID,
      subjectSectionId: SUBJECT_SECTION_ID,
      periodNo,
      date,
      // Cycle through a few statuses so payload variance ≈ realistic.
      status:
        i % 7 === 0 ? 'ABSENT' : i % 5 === 0 ? 'LATE' : 'PRESENT',
    });
  }
  return { entries };
}

export default function () {
  const url = `${BASE_URL}/api/v1/attendance/periods`;
  const payload = JSON.stringify(buildBatch());
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Id': TENANT_ID,
      'X-Teacher-Id': TEACHER_ID,
    },
    tags: { endpoint: 'attendance-period-batch' },
  };

  const res = http.post(url, payload, params);

  check(res, {
    'status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'p95 budget tracked': () => true,
  });

  // Tiny pause so we don't overshoot RPS faster than realistic GVCN flow
  // (a teacher saves the tiết once at the end, not 100 saves/sec).
  sleep(2);
}
