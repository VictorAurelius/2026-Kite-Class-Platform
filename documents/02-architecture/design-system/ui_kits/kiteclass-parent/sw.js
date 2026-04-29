/**
 * kiteclass-parent — Service Worker spec (PROTOTYPE)
 *
 * Persona: Pa. Parent (mobile-first 320–414px, Zalo OA primary push)
 * Direction D pivot per dossier/08-direction-decisions.md §2:
 *   web responsive + PWA-grade — NOT native app.
 *
 * SCOPE FOR ROUND 2 PROTOTYPE:
 * - Cache static shell (HTML, CSS, fonts, icons)
 * - Stale-while-revalidate for API GET /parent/*
 * - Network-first for write requests (POST/PUT/DELETE — never cache)
 * - Push notification handler stub (Web Push API + Zalo OA fallback flow)
 * - Background sync placeholder for offline queue (Round 3)
 *
 * NOT IN ROUND 2 (defer to Track 2 production port):
 * - Real cache versioning + cleanup orchestration
 * - Conflict resolution for offline write queue
 * - Periodic background sync
 *
 * NOTE: This file is a SPEC/PROTOTYPE — production Service Worker will live
 * at kiteclass-frontend/public/sw.js with real Workbox tooling. Don't run as-is.
 */

const CACHE_VERSION = "kc-parent-v1";
const STATIC_CACHE = `${CACHE_VERSION}-static`;
const RUNTIME_CACHE = `${CACHE_VERSION}-runtime`;

// App shell — cached at install, served from cache thereafter.
const STATIC_ASSETS = [
  "/parent",
  "/parent/grades",
  "/parent/attendance",
  "/parent/billing",
  "/parent/settings",
  "/manifest.json",
  "/_shared/assets/kite-mark.svg",
  // Fonts loaded from Google CDN — see fetch handler below
];

// =================================================================
// INSTALL — pre-cache the app shell
// =================================================================
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.addAll(STATIC_ASSETS)),
  );
  self.skipWaiting();
});

// =================================================================
// ACTIVATE — clean up old caches
// =================================================================
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((k) => !k.startsWith(CACHE_VERSION))
          .map((k) => caches.delete(k)),
      ),
    ),
  );
  self.clients.claim();
});

// =================================================================
// FETCH — routing strategy by request type
// =================================================================
self.addEventListener("fetch", (event) => {
  const { request } = event;
  const url = new URL(request.url);

  // Never cache writes — let them fail loud if offline (UI shows error state)
  if (request.method !== "GET") return;

  // API GETs — stale-while-revalidate, scoped to parent routes
  if (url.pathname.startsWith("/api/v1/parent")) {
    event.respondWith(staleWhileRevalidate(request));
    return;
  }

  // Static shell — cache-first
  if (STATIC_ASSETS.includes(url.pathname)) {
    event.respondWith(cacheFirst(request));
    return;
  }

  // HTML navigation — network-first, fallback to offline shell
  if (request.mode === "navigate") {
    event.respondWith(networkFirstWithOfflineShell(request));
    return;
  }

  // Default — network passthrough
  event.respondWith(fetch(request).catch(() => caches.match(request)));
});

async function cacheFirst(request) {
  const cached = await caches.match(request);
  if (cached) return cached;
  const response = await fetch(request);
  if (response.ok) {
    const cache = await caches.open(STATIC_CACHE);
    cache.put(request, response.clone());
  }
  return response;
}

async function staleWhileRevalidate(request) {
  const cache = await caches.open(RUNTIME_CACHE);
  const cached = await cache.match(request);
  const networkPromise = fetch(request)
    .then((response) => {
      if (response.ok) cache.put(request, response.clone());
      return response;
    })
    .catch(() => cached); // network failed, return cached
  return cached || networkPromise;
}

async function networkFirstWithOfflineShell(request) {
  try {
    const response = await fetch(request);
    if (response.ok) {
      const cache = await caches.open(RUNTIME_CACHE);
      cache.put(request, response.clone());
    }
    return response;
  } catch (err) {
    const cached = await caches.match(request);
    if (cached) return cached;
    // Offline shell — last resort
    return caches.match("/parent");
  }
}

// =================================================================
// PUSH — Web Push API handler
// Note: Zalo OA is the PRIMARY push channel (~95% reach for VN parents).
// Web Push is the fallback for users who declined Zalo OA OR for in-app
// users on Chrome/Edge desktop or Android.
// =================================================================
self.addEventListener("push", (event) => {
  if (!event.data) return;

  const payload = event.data.json();
  // payload shape: { type, title, body, url, tenantId, childId, ... }

  const options = {
    body: payload.body,
    icon: "/_shared/assets/icon-192.png",
    badge: "/_shared/assets/badge-72.png",
    tag: payload.type, // collapse same-type notifications
    data: {
      url: payload.url || "/parent",
      tenantId: payload.tenantId,
    },
    vibrate: [200, 100, 200],
    requireInteraction: payload.priority === "high",
    actions: buildActionsFor(payload.type),
    lang: "vi-VN",
  };

  event.waitUntil(
    self.registration.showNotification(payload.title, options),
  );
});

function buildActionsFor(type) {
  switch (type) {
    case "grade.published":
      return [
        { action: "view", title: "Xem điểm" },
        { action: "dismiss", title: "Bỏ qua" },
      ];
    case "attendance.absent":
      return [
        { action: "view", title: "Xem lịch" },
        { action: "contact", title: "Nhắn GVCN" },
      ];
    case "billing.due":
      return [
        { action: "pay", title: "Đóng ngay" },
        { action: "later", title: "Để sau" },
      ];
    default:
      return [];
  }
}

// =================================================================
// NOTIFICATION CLICK — open app at relevant deep link
// =================================================================
self.addEventListener("notificationclick", (event) => {
  event.notification.close();

  const url = event.notification.data?.url || "/parent";

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then(
      (clientList) => {
        // Focus existing tab if open
        for (const client of clientList) {
          if (client.url.includes("/parent") && "focus" in client) {
            client.navigate(url);
            return client.focus();
          }
        }
        // Otherwise open new window
        if (self.clients.openWindow) {
          return self.clients.openWindow(url);
        }
      },
    ),
  );
});

// =================================================================
// BACKGROUND SYNC — placeholder for Round 3 offline write queue
// Use case: parent marks "đã đọc" on a notification while offline → queue
// the ack request, fire when back online.
// =================================================================
self.addEventListener("sync", (event) => {
  if (event.tag === "kc-parent-pending-acks") {
    event.waitUntil(replayPendingAcks());
  }
});

async function replayPendingAcks() {
  // STUB — Round 3 implementation: read from IndexedDB queue, POST each, drain.
  // For Round 2 prototype, this is just a placeholder so the SW lifecycle is
  // documented for the eventual port to production.
  return Promise.resolve();
}
