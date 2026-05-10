/**
 * KiteClass production service worker — minimal PWA shell.
 *
 * Wave 49 Bucket 0 (Track 2 Phase 4 PWA infra). Distinct from MSW's
 * `mockServiceWorker.js` (dev/test tool); this SW handles production
 * install / push / offline-fallback for parent + student PWA personas.
 *
 * Scope: `/` (entire app). Skips waiting on activate so updates roll out
 * quickly. Does NOT pre-cache pages — lets Next.js / Vercel CDN handle
 * caching; this SW only provides install + push event surfaces required
 * by Web App Manifest + Web Push Protocol.
 */

const CACHE_VERSION = 'kc-pwa-v1';
const OFFLINE_URL = '/offline.html';

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_VERSION).then((cache) => {
      // Best-effort offline fallback. If the page doesn't exist yet, the
      // cache.add silently fails — install still completes.
      return cache.add(OFFLINE_URL).catch(() => undefined);
    }),
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key.startsWith('kc-pwa-') && key !== CACHE_VERSION)
          .map((key) => caches.delete(key)),
      ),
    ),
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  // Only intercept navigation requests to provide offline fallback.
  // All other requests (assets, API) bypass the SW and go straight to
  // network — Vercel CDN handles asset caching.
  if (event.request.mode !== 'navigate') return;

  event.respondWith(
    fetch(event.request).catch(() =>
      caches.match(OFFLINE_URL).then((response) => response || new Response('Bạn đang offline.', { status: 503 })),
    ),
  );
});

// Web Push — display incoming notification. Payload schema:
//   { title: string, body: string, url?: string, icon?: string, badge?: string, tag?: string }
self.addEventListener('push', (event) => {
  if (!event.data) return;

  let payload;
  try {
    payload = event.data.json();
  } catch {
    payload = { title: 'KiteClass', body: event.data.text() };
  }

  const title = payload.title || 'KiteClass';
  const options = {
    body: payload.body || '',
    icon: payload.icon || '/icons/icon-192.png',
    badge: payload.badge || '/icons/icon-192.png',
    data: { url: payload.url || '/' },
    tag: payload.tag,
  };

  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = (event.notification.data && event.notification.data.url) || '/';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if (client.url === url && 'focus' in client) return client.focus();
      }
      if (self.clients.openWindow) return self.clients.openWindow(url);
      return undefined;
    }),
  );
});
