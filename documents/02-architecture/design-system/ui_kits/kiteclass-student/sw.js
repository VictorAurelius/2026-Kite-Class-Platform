/**
 * kiteclass-student — Service Worker spec (PROTOTYPE)
 *
 * Persona: S. Student (mobile-primary 320–414px ~85% sessions, K-12 + vocational)
 * Direction D pivot per dossier/08-direction-decisions.md §2:
 *   web responsive + PWA-grade — NOT native app.
 *
 * SCOPE FOR ROUND 3 PROTOTYPE:
 * - Cache static shell (HTML, CSS, fonts, icons)
 * - Stale-while-revalidate for API GET /student/*
 * - Network-first for write requests (POST/PUT/DELETE — never cache)
 * - Push notification handler for grade-published / assignment-due / class-cancelled
 * - Background sync placeholder for offline assignment-submit queue
 *
 * NOT IN ROUND 3 (defer to Track 2 production port):
 * - Real cache versioning + cleanup orchestration
 * - Conflict resolution for offline assignment-submit queue (saved-draft model)
 * - Periodic background sync for grade refresh
 *
 * NOTE: This file is a SPEC/PROTOTYPE — production Service Worker will live
 * at kiteclass-frontend/public/sw.js with real Workbox tooling. Don't run as-is.
 */

const CACHE_VERSION = "kc-student-v1";
const STATIC_CACHE = `${CACHE_VERSION}-static`;
const RUNTIME_CACHE = `${CACHE_VERSION}-runtime`;

// App shell — cached at install, served from cache thereafter.
const STATIC_ASSETS = [
  "/student",
  "/student/today",
  "/student/classes",
  "/student/assignments",
  "/student/grades",
  "/student/attendance",
  "/student/profile",
  "/manifest.json",
  "/_shared/assets/kite-mark.svg",
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

  // Never cache writes — let them fail loud if offline
  // (UI shows error state; saved-draft model handles assignment-submit recovery)
  if (request.method !== "GET") return;

  // API GETs — stale-while-revalidate, scoped to student routes
  if (url.pathname.startsWith("/api/v1/student")) {
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
    .catch(() => cached);
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
    return caches.match("/student");
  }
}

// =================================================================
// PUSH — Web Push API handler
// Note: Zalo OA is the PRIMARY push channel for parents (~95% reach).
// For STUDENTS, Web Push works well (Chrome/Edge desktop + Android Chrome,
// iOS Safari 16.4+ requires Add-to-Home-Screen first).
// Students typically install the PWA on phone — Web Push is viable.
// =================================================================
self.addEventListener("push", (event) => {
  if (!event.data) return;

  const payload = event.data.json();
  // payload shape: { type, title, body, url, classId, assignmentId, ... }

  const options = {
    body: payload.body,
    icon: "/_shared/assets/icon-192.png",
    badge: "/_shared/assets/badge-72.png",
    tag: payload.type, // collapse same-type notifications
    data: {
      url: payload.url || "/student/today",
      classId: payload.classId,
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
    case "assignment.due-soon":
      return [
        { action: "view", title: "Mở bài tập" },
        { action: "remind", title: "Nhắc 1h nữa" },
      ];
    case "class.cancelled":
      return [
        { action: "view", title: "Xem chi tiết" },
        { action: "dismiss", title: "Đã hiểu" },
      ];
    case "attendance.warning":
      return [
        { action: "view", title: "Xem điểm danh" },
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

  const url = event.notification.data?.url || "/student/today";

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then(
      (clientList) => {
        for (const client of clientList) {
          if (client.url.includes("/student") && "focus" in client) {
            client.navigate(url);
            return client.focus();
          }
        }
        if (self.clients.openWindow) {
          return self.clients.openWindow(url);
        }
      },
    ),
  );
});

// =================================================================
// BACKGROUND SYNC — Round 4 offline assignment-submit queue
// Use case: student finishes essay on bus (offline), taps "Nộp bài" →
// queue request, fire when WiFi/4G returns. Critical for VN context
// where mobile data is patchy on rural buses.
// =================================================================
self.addEventListener("sync", (event) => {
  if (event.tag === "kc-student-pending-submits") {
    event.waitUntil(replayPendingSubmits());
  }
});

async function replayPendingSubmits() {
  // STUB — Round 4 implementation: read from IndexedDB queue, POST each, drain.
  // For Round 3 prototype, placeholder so the SW lifecycle is documented for
  // the eventual port to production at kiteclass-frontend/public/sw.js.
  return Promise.resolve();
}
