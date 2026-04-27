/**
 * Unit tests for check-bundle-budget.mjs.
 *
 * Run with:
 *   node --test scripts/check-bundle-budget.test.mjs
 *
 * Tests pure functions only (no FS). Build manifest fixtures are inline.
 * The script's main() (which does FS + process.exit) is integration-tested
 * via direct CLI invocation in CI after `pnpm build`.
 */

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

import { _resetChunkCache, applyBudget, computeFirstLoadJs } from './check-bundle-budget.mjs';

const __dirname = dirname(fileURLToPath(import.meta.url));
const SCRIPT = resolve(__dirname, 'check-bundle-budget.mjs');

function makeFakeBuild({ routes, chunkSizesBytes }) {
  // routes: { "/foo": ["chunks/a.js", "chunks/b.js"] }
  // chunkSizesBytes is the desired GZIPPED size — we approximate by writing random
  // bytes ~= gzipSize / 0.97 (random data barely compresses, so byte-count ~= gz size).
  const tmpDir = mkdtempSync(join(tmpdir(), 'budget-test-'));
  const nextDir = join(tmpDir, '.next');
  mkdirSync(nextDir, { recursive: true });
  for (const [chunkPath, size] of Object.entries(chunkSizesBytes)) {
    const fullPath = join(nextDir, chunkPath);
    mkdirSync(dirname(fullPath), { recursive: true });
    // Random bytes do not compress — gzip output ≈ input size + small overhead.
    writeFileSync(fullPath, randomBytes(size));
  }
  const manifestPath = join(nextDir, 'app-build-manifest.json');
  writeFileSync(manifestPath, JSON.stringify({ pages: routes }));
  _resetChunkCache();
  return { tmpDir, nextDir, manifestPath };
}

test('computeFirstLoadJs sums chunk gzipped sizes per route', () => {
  const { tmpDir, nextDir } = makeFakeBuild({
    routes: {
      '/': ['chunks/main.js', 'chunks/shared.js'],
      '/dashboard': ['chunks/main.js', 'chunks/dash.js'],
    },
    chunkSizesBytes: {
      'chunks/main.js': 50 * 1024,
      'chunks/shared.js': 30 * 1024,
      'chunks/dash.js': 100 * 1024,
    },
  });

  try {
    const manifest = {
      pages: {
        '/': ['chunks/main.js', 'chunks/shared.js'],
        '/dashboard': ['chunks/main.js', 'chunks/dash.js'],
      },
    };
    const routes = computeFirstLoadJs(manifest, nextDir);

    assert.equal(routes.size, 2);
    // Gzip of random bytes ≈ input size + ~0.1% overhead. Sanity-check ranges.
    const homeBytes = routes.get('/').sizeBytes;
    const dashBytes = routes.get('/dashboard').sizeBytes;
    assert.ok(
      homeBytes >= 80 * 1024 && homeBytes <= 82 * 1024,
      `home gzipped expected ~80KB, got ${homeBytes}`,
    );
    assert.ok(
      dashBytes >= 150 * 1024 && dashBytes <= 154 * 1024,
      `dashboard gzipped expected ~150KB, got ${dashBytes}`,
    );
  } finally {
    rmSync(tmpDir, { recursive: true, force: true });
  }
});

test('computeFirstLoadJs throws on invalid manifest', () => {
  assert.throws(() => computeFirstLoadJs(null, '/tmp'), /Invalid manifest/);
  assert.throws(() => computeFirstLoadJs({}, '/tmp'), /Invalid manifest/);
});

test('computeFirstLoadJs ignores missing chunk files (returns size 0 for those)', () => {
  const { tmpDir, nextDir } = makeFakeBuild({
    routes: { '/': ['chunks/exists.js'] },
    chunkSizesBytes: { 'chunks/exists.js': 10 * 1024 },
  });

  try {
    const manifest = { pages: { '/': ['chunks/exists.js', 'chunks/missing.js'] } };
    const routes = computeFirstLoadJs(manifest, nextDir);
    // missing.js contributes 0; exists.js gzipped ~ 10KB (random bytes don't compress)
    const sz = routes.get('/').sizeBytes;
    assert.ok(sz >= 10 * 1024 && sz <= 11 * 1024, `expected ~10KB gzipped, got ${sz}`);
  } finally {
    rmSync(tmpDir, { recursive: true, force: true });
  }
});

test('applyBudget marks over-budget routes', () => {
  const routes = new Map([
    ['/', { chunks: ['a.js'], sizeBytes: 200 * 1024 }],
    ['/heavy', { chunks: ['b.js'], sizeBytes: 300 * 1024 }],
  ]);
  const config = { default: null, routes: {} };
  const results = applyBudget(routes, config, 250);

  const heavyResult = results.find((r) => r.route === '/heavy');
  const homeResult = results.find((r) => r.route === '/');
  assert.equal(heavyResult.overBudget, true);
  assert.equal(homeResult.overBudget, false);
});

test('applyBudget honors per-route override', () => {
  const routes = new Map([
    ['/marketing', { chunks: ['m.js'], sizeBytes: 320 * 1024 }],
  ]);
  const config = { default: null, routes: { '/marketing': 350 } };
  const results = applyBudget(routes, config, 250);

  assert.equal(results[0].budgetKb, 350);
  assert.equal(results[0].overBudget, false);
});

test('applyBudget honors env budget over hardcoded default', () => {
  const routes = new Map([
    ['/', { chunks: ['a.js'], sizeBytes: 280 * 1024 }],
  ]);
  const config = { default: null, routes: {} };
  // env=300, route=280 → under budget
  const results = applyBudget(routes, config, 300);
  assert.equal(results[0].overBudget, false);
});

test('applyBudget — config default beats hardcoded default when env not set', () => {
  const routes = new Map([
    ['/', { chunks: ['a.js'], sizeBytes: 200 * 1024 }],
  ]);
  const config = { default: 150, routes: {} };
  const results = applyBudget(routes, config, null);
  // 200 > 150 → over budget
  assert.equal(results[0].budgetKb, 150);
  assert.equal(results[0].overBudget, true);
});

test('applyBudget — per-route override beats env override', () => {
  const routes = new Map([
    ['/admin', { chunks: ['a.js'], sizeBytes: 280 * 1024 }],
  ]);
  const config = { default: null, routes: { '/admin': 320 } };
  // env=200 would say over-budget, but per-route 320 wins
  const results = applyBudget(routes, config, 200);
  assert.equal(results[0].budgetKb, 320);
  assert.equal(results[0].overBudget, false);
});

test('applyBudget sorts results by size desc', () => {
  const routes = new Map([
    ['/small', { chunks: ['s.js'], sizeBytes: 100 * 1024 }],
    ['/big', { chunks: ['b.js'], sizeBytes: 300 * 1024 }],
    ['/medium', { chunks: ['m.js'], sizeBytes: 200 * 1024 }],
  ]);
  const results = applyBudget(routes, { default: null, routes: {} }, 500);
  assert.deepEqual(
    results.map((r) => r.route),
    ['/big', '/medium', '/small'],
  );
});

test('CLI exits 1 when route over budget (integration)', () => {
  const { tmpDir, nextDir, manifestPath } = makeFakeBuild({
    routes: { '/': ['chunks/big.js'] },
    chunkSizesBytes: { 'chunks/big.js': 400 * 1024 }, // 400 KB > 250 default
  });

  try {
    const result = spawnSync('node', [SCRIPT, '--manifest', manifestPath, '--next-dir', nextDir], {
      encoding: 'utf8',
      env: { ...process.env, BUNDLE_BUDGET_KB: '' },
    });
    assert.equal(result.status, 1, `expected exit 1, got ${result.status}\nstdout: ${result.stdout}\nstderr: ${result.stderr}`);
    assert.match(result.stderr, /FAIL/);
  } finally {
    rmSync(tmpDir, { recursive: true, force: true });
  }
});

test('CLI exits 0 when all routes under budget (integration)', () => {
  const { tmpDir, nextDir, manifestPath } = makeFakeBuild({
    routes: { '/': ['chunks/small.js'] },
    chunkSizesBytes: { 'chunks/small.js': 100 * 1024 },
  });

  try {
    const result = spawnSync('node', [SCRIPT, '--manifest', manifestPath, '--next-dir', nextDir], {
      encoding: 'utf8',
    });
    assert.equal(result.status, 0, `expected exit 0, got ${result.status}\nstderr: ${result.stderr}`);
    assert.match(result.stdout, /OK/);
  } finally {
    rmSync(tmpDir, { recursive: true, force: true });
  }
});

test('CLI honors BUNDLE_BUDGET_KB env override (integration)', () => {
  const { tmpDir, nextDir, manifestPath } = makeFakeBuild({
    routes: { '/': ['chunks/medium.js'] },
    chunkSizesBytes: { 'chunks/medium.js': 280 * 1024 }, // would fail at 250, pass at 300
  });

  try {
    const result = spawnSync('node', [SCRIPT, '--manifest', manifestPath, '--next-dir', nextDir], {
      encoding: 'utf8',
      env: { ...process.env, BUNDLE_BUDGET_KB: '300' },
    });
    assert.equal(result.status, 0, `expected exit 0 with budget=300, got ${result.status}\nstderr: ${result.stderr}`);
  } finally {
    rmSync(tmpDir, { recursive: true, force: true });
  }
});

test('CLI exits 2 when manifest missing (integration)', () => {
  const tmpDir = mkdtempSync(join(tmpdir(), 'budget-test-empty-'));
  try {
    const result = spawnSync('node', [SCRIPT, '--next-dir', tmpDir], {
      encoding: 'utf8',
    });
    assert.equal(result.status, 2);
    assert.match(result.stderr, /No build manifest/);
  } finally {
    rmSync(tmpDir, { recursive: true, force: true });
  }
});
