#!/usr/bin/env node
/**
 * Bundle Budget Check — fails CI when any route's First Load JS exceeds the budget.
 *
 * Usage:
 *   node scripts/check-bundle-budget.mjs                  # default 250 KB threshold
 *   BUNDLE_BUDGET_KB=300 node scripts/check-bundle-budget.mjs
 *   node scripts/check-bundle-budget.mjs --manifest .next/app-build-manifest.json
 *
 * Per-route overrides live in `bundle-budget.json` at the FE app root, format:
 *   {
 *     "default": 250,
 *     "routes": { "/marketing": 350, "/admin/payments": 320 }
 *   }
 *
 * Exit codes:
 *   0  → all routes within budget (or warning-only mode)
 *   1  → at least one route over budget
 *   2  → infrastructure error (manifest missing, malformed)
 *
 * See: documents/05-guides/frontend-bundle-budget.md
 *      documents/04-quality/gaps/GAP-236-fe-code-splitting-completion.md
 */

import { existsSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';
import { gzipSync } from 'node:zlib';

const __dirname = dirname(fileURLToPath(import.meta.url));
const FE_ROOT = resolve(__dirname, '..');

const DEFAULT_BUDGET_KB = 250;

function loadConfig(feRoot) {
  const configPath = join(feRoot, 'bundle-budget.json');
  if (!existsSync(configPath)) {
    return { default: null, routes: {} };
  }
  try {
    const raw = readFileSync(configPath, 'utf8');
    const cfg = JSON.parse(raw);
    return {
      default: typeof cfg.default === 'number' ? cfg.default : null,
      routes: cfg.routes && typeof cfg.routes === 'object' ? cfg.routes : {},
    };
  } catch (err) {
    console.error(`[bundle-budget] Failed to parse bundle-budget.json: ${err.message}`);
    return { default: null, routes: {} };
  }
}

/**
 * Cache for chunk sizes — same chunk may appear in many routes, only measure once.
 * Cleared per-invocation.
 */
const chunkCache = new Map();

/**
 * Return GZIPPED byte size of a chunk file, or 0 if missing.
 * Matches Next.js's First Load JS reporting (which shows gzip).
 */
function chunkSizeBytes(nextDir, chunk) {
  const cacheKey = `${nextDir}::${chunk}`;
  if (chunkCache.has(cacheKey)) return chunkCache.get(cacheKey);

  const fullPath = join(nextDir, chunk);
  if (!existsSync(fullPath)) {
    chunkCache.set(cacheKey, 0);
    return 0;
  }
  // Skip non-JS chunks (CSS chunks are negligible, fonts excluded from First Load JS)
  if (!chunk.endsWith('.js') && !chunk.endsWith('.mjs')) {
    chunkCache.set(cacheKey, 0);
    return 0;
  }
  const buf = readFileSync(fullPath);
  // Gzipped at level 6 — Next CLI uses similar setting; small drift vs CDN brotli is OK
  const gzippedLen = gzipSync(buf, { level: 6 }).length;
  chunkCache.set(cacheKey, gzippedLen);
  return gzippedLen;
}

/**
 * For tests — clear the chunk cache between runs.
 */
export function _resetChunkCache() {
  chunkCache.clear();
}

function bytesToKb(bytes) {
  return Math.round((bytes / 1024) * 100) / 100;
}

function pickBudget(route, config, envBudget) {
  // Priority: per-route config > env override > config default > hardcoded default
  if (config.routes[route] != null) return config.routes[route];
  if (envBudget != null) return envBudget;
  if (config.default != null) return config.default;
  return DEFAULT_BUDGET_KB;
}

/**
 * Compute First Load JS for each route from a Next.js build manifest.
 * Manifest schema (Next 15):
 *   { "pages": { "/_app": [...], "/about": [...] } }   (pages router)
 *   { "pages": { "/_app": [...] }, ... }
 * For app router, the equivalent is `.next/app-build-manifest.json`:
 *   { "pages": { "/page": ["chunk-a.js", "chunk-b.js"] } }
 *
 * Returns a Map<route, { chunks: string[], sizeBytes: number }>.
 */
export function computeFirstLoadJs(manifest, nextDir) {
  if (!manifest || typeof manifest !== 'object' || !manifest.pages) {
    throw new Error('Invalid manifest: missing `pages` field');
  }
  const routes = new Map();
  for (const [route, chunks] of Object.entries(manifest.pages)) {
    if (!Array.isArray(chunks)) continue;
    let sizeBytes = 0;
    for (const chunk of chunks) {
      sizeBytes += chunkSizeBytes(nextDir, chunk);
    }
    routes.set(route, { chunks, sizeBytes });
  }
  return routes;
}

/**
 * Apply budget to computed routes; produce per-route results.
 * Pure function — no I/O — so it's unit-testable.
 */
export function applyBudget(routes, config, envBudgetKb) {
  const results = [];
  for (const [route, data] of routes.entries()) {
    const budgetKb = pickBudget(route, config, envBudgetKb);
    const sizeKb = bytesToKb(data.sizeBytes);
    results.push({
      route,
      sizeKb,
      sizeBytes: data.sizeBytes,
      budgetKb,
      overBudget: sizeKb > budgetKb,
      chunkCount: data.chunks.length,
    });
  }
  results.sort((a, b) => b.sizeKb - a.sizeKb);
  return results;
}

function readManifest(manifestPath) {
  if (!existsSync(manifestPath)) {
    return null;
  }
  const raw = readFileSync(manifestPath, 'utf8');
  return JSON.parse(raw);
}

function loadCombinedManifest(nextDir) {
  // Prefer app-build-manifest (App Router). Fall back to build-manifest (Pages Router).
  // If both exist, merge — same route key would collide but Next emits app routes
  // distinct from pages routes by design.
  const appManifestPath = join(nextDir, 'app-build-manifest.json');
  const pagesManifestPath = join(nextDir, 'build-manifest.json');

  const appManifest = readManifest(appManifestPath);
  const pagesManifest = readManifest(pagesManifestPath);

  if (!appManifest && !pagesManifest) {
    return null;
  }

  const merged = { pages: {} };
  if (pagesManifest && pagesManifest.pages) {
    Object.assign(merged.pages, pagesManifest.pages);
  }
  if (appManifest && appManifest.pages) {
    Object.assign(merged.pages, appManifest.pages);
  }
  return merged;
}

function formatReport(results, threshold) {
  const lines = [];
  lines.push('Bundle Budget Report');
  lines.push('='.repeat(70));
  lines.push(
    `${'Route'.padEnd(40)} ${'Size'.padStart(10)} ${'Budget'.padStart(10)} ${'Status'.padStart(8)}`,
  );
  lines.push('-'.repeat(70));
  for (const r of results) {
    const sizeStr = `${r.sizeKb.toFixed(2)} KB`;
    const budgetStr = `${r.budgetKb} KB`;
    const status = r.overBudget ? 'OVER' : 'OK';
    lines.push(
      `${r.route.padEnd(40).slice(0, 40)} ${sizeStr.padStart(10)} ${budgetStr.padStart(10)} ${status.padStart(8)}`,
    );
  }
  lines.push('-'.repeat(70));
  const overCount = results.filter((r) => r.overBudget).length;
  lines.push(
    `Routes: ${results.length} | Default budget: ${threshold} KB | Over budget: ${overCount}`,
  );
  return lines.join('\n');
}

async function main() {
  const { values } = parseArgs({
    options: {
      manifest: { type: 'string' },
      'next-dir': { type: 'string' },
      json: { type: 'boolean', default: false },
      warn: { type: 'boolean', default: false },
    },
    allowPositionals: false,
  });

  const nextDir = resolve(values['next-dir'] || join(FE_ROOT, '.next'));
  const config = loadConfig(FE_ROOT);

  const envBudgetRaw = process.env.BUNDLE_BUDGET_KB;
  const envBudget = envBudgetRaw ? Number(envBudgetRaw) : null;
  if (envBudgetRaw && !Number.isFinite(envBudget)) {
    console.error(`[bundle-budget] BUNDLE_BUDGET_KB="${envBudgetRaw}" is not a number`);
    process.exit(2);
  }

  const effectiveDefault = pickBudget('__none__', config, envBudget);

  let manifest;
  if (values.manifest) {
    manifest = readManifest(resolve(values.manifest));
    if (!manifest) {
      console.error(`[bundle-budget] Manifest not found: ${values.manifest}`);
      process.exit(2);
    }
  } else {
    manifest = loadCombinedManifest(nextDir);
    if (!manifest) {
      console.error(
        `[bundle-budget] No build manifest found in ${nextDir}. Run \`pnpm build\` first.`,
      );
      process.exit(2);
    }
  }

  const routes = computeFirstLoadJs(manifest, nextDir);
  const results = applyBudget(routes, config, envBudget);

  if (values.json) {
    console.log(
      JSON.stringify(
        {
          defaultBudgetKb: effectiveDefault,
          routes: results,
        },
        null,
        2,
      ),
    );
  } else {
    console.log(formatReport(results, effectiveDefault));
  }

  const overBudget = results.filter((r) => r.overBudget);
  if (overBudget.length > 0) {
    console.error(
      `\n[bundle-budget] FAIL — ${overBudget.length} route(s) exceed budget:`,
    );
    for (const r of overBudget) {
      console.error(`  - ${r.route}: ${r.sizeKb.toFixed(2)} KB > ${r.budgetKb} KB`);
    }
    console.error(
      '\nSee documents/05-guides/frontend-bundle-budget.md for tuning + override docs.',
    );
    if (values.warn) {
      console.error('[bundle-budget] --warn mode: exiting 0 despite over-budget routes.');
      process.exit(0);
    }
    process.exit(1);
  }

  console.log(`\n[bundle-budget] OK — all ${results.length} route(s) within budget.`);
  process.exit(0);
}

// Only run main when invoked directly (not when imported by tests).
if (import.meta.url === `file://${process.argv[1]}`) {
  main().catch((err) => {
    console.error(`[bundle-budget] Unexpected error: ${err.message}`);
    console.error(err.stack);
    process.exit(2);
  });
}
