/**
 * Web Push subscription helpers — KiteClass parent + student PWA.
 *
 * Wave 49 Bucket 0 (Track 2 Phase 4 PWA infra). Wraps the browser Push API
 * + Notification permission flow into a small typed surface that GAP-267
 * (kc-parent) and GAP-269 (kc-student) consume from their settings UIs.
 *
 * Backend endpoint `/api/push/subscribe` is currently MSW-mocked — the
 * actual kc-core REST controller is deferred to a follow-up gap (see
 * Wave 49 plan §1 Q3 R5). The shape of the request body is fixed here so
 * the BE wiring can drop in without UI changes.
 *
 * VAPID public key is read from `NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY` so
 * deploys can rotate keys without code change. When unset (e.g. local dev
 * without push), `subscribe()` returns the `'not-configured'` outcome so
 * UIs can render a placeholder instead of failing.
 */

export type WebPushOutcome =
  | 'subscribed'
  | 'unsubscribed'
  | 'denied'
  | 'unsupported'
  | 'not-configured'
  | 'error';

export interface WebPushSubscribeResult {
  outcome: WebPushOutcome;
  endpoint?: string;
  error?: string;
}

const SUBSCRIBE_ENDPOINT = '/api/push/subscribe';
const UNSUBSCRIBE_ENDPOINT = '/api/push/unsubscribe';

/**
 * Capability check — true when the runtime supports Service Worker + Push API
 * + Notification API. Use to gate UI rendering on devices that don't support
 * push (e.g. iOS Safari pre-16.4, in-app webviews).
 */
export function isPushSupported(): boolean {
  if (typeof window === 'undefined') return false;
  return (
    'serviceWorker' in navigator &&
    'PushManager' in window &&
    'Notification' in window
  );
}

/**
 * Convert a base64url-encoded VAPID public key to the Uint8Array shape the
 * Push API expects for `applicationServerKey`.
 */
export function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = atob(base64);
  const outputArray = new Uint8Array(rawData.length);
  for (let i = 0; i < rawData.length; ++i) {
    outputArray[i] = rawData.charCodeAt(i);
  }
  return outputArray;
}

/**
 * Subscribe the current user to Web Push. Returns the outcome plus the
 * created endpoint if successful. The caller is expected to surface the
 * outcome to the user (toast / inline message) and persist the endpoint
 * locally if needed.
 *
 * Side effects: requests Notification permission if not already decided;
 * registers the service worker if not yet registered; POSTs the
 * subscription to `/api/push/subscribe` (currently MSW-mocked).
 */
export async function subscribe(): Promise<WebPushSubscribeResult> {
  if (!isPushSupported()) return { outcome: 'unsupported' };

  const vapidKey = process.env.NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY;
  if (!vapidKey) return { outcome: 'not-configured' };

  try {
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') return { outcome: 'denied' };

    const registration =
      (await navigator.serviceWorker.getRegistration('/')) ||
      (await navigator.serviceWorker.register('/sw.js', { scope: '/' }));

    const existing = await registration.pushManager.getSubscription();
    const subscription =
      existing ||
      (await registration.pushManager.subscribe({
        userVisibleOnly: true,
        // Cast: TS 5.7+ types `Uint8Array<ArrayBufferLike>`, but PushManager
        // expects `BufferSource` (ArrayBuffer | ArrayBufferView). The Uint8Array
        // we produce always backs an ArrayBuffer at runtime, so the cast is
        // safe; the structural compatibility issue is purely type-level.
        applicationServerKey: urlBase64ToUint8Array(vapidKey) as BufferSource,
      }));

    const response = await fetch(SUBSCRIBE_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(subscription.toJSON()),
    });

    if (!response.ok) {
      return {
        outcome: 'error',
        error: `Subscribe POST failed: ${response.status}`,
      };
    }

    return { outcome: 'subscribed', endpoint: subscription.endpoint };
  } catch (err) {
    return {
      outcome: 'error',
      error: err instanceof Error ? err.message : String(err),
    };
  }
}

/**
 * Unsubscribe the current user from Web Push. Notifies the backend so the
 * subscription record can be soft-deleted (DSAR + retention compliance).
 */
export async function unsubscribe(): Promise<WebPushSubscribeResult> {
  if (!isPushSupported()) return { outcome: 'unsupported' };

  try {
    const registration = await navigator.serviceWorker.getRegistration('/');
    if (!registration) return { outcome: 'unsubscribed' };

    const subscription = await registration.pushManager.getSubscription();
    if (!subscription) return { outcome: 'unsubscribed' };

    const endpoint = subscription.endpoint;
    await subscription.unsubscribe();

    await fetch(UNSUBSCRIBE_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ endpoint }),
    }).catch(() => undefined); // best-effort; local unsubscribe already done

    return { outcome: 'unsubscribed', endpoint };
  } catch (err) {
    return {
      outcome: 'error',
      error: err instanceof Error ? err.message : String(err),
    };
  }
}

/**
 * Current subscription state — useful for rendering the right UI without
 * triggering a permission prompt.
 */
export async function getSubscriptionState(): Promise<
  'subscribed' | 'not-subscribed' | 'denied' | 'unsupported'
> {
  if (!isPushSupported()) return 'unsupported';
  if (Notification.permission === 'denied') return 'denied';

  const registration = await navigator.serviceWorker.getRegistration('/');
  if (!registration) return 'not-subscribed';

  const subscription = await registration.pushManager.getSubscription();
  return subscription ? 'subscribed' : 'not-subscribed';
}
