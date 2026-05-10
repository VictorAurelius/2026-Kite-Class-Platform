/**
 * Web Push helper tests — Wave 49 Bucket 0.
 *
 * Covers the supported / unsupported / not-configured / denied / error
 * branches without hitting a real Push service. Browser APIs are stubbed
 * via vitest spies; the subscribe/unsubscribe network call is intercepted
 * with `vi.stubGlobal('fetch', ...)`.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  getSubscriptionState,
  isPushSupported,
  subscribe,
  unsubscribe,
  urlBase64ToUint8Array,
} from '../web-push';

const ORIGINAL_ENV = { ...process.env };

interface MockSubscription {
  endpoint: string;
  toJSON: () => Record<string, unknown>;
  unsubscribe: () => Promise<boolean>;
}

interface MockRegistration {
  pushManager: {
    getSubscription: () => Promise<MockSubscription | null>;
    subscribe: (opts: PushSubscriptionOptionsInit) => Promise<MockSubscription>;
  };
}

const buildMockSubscription = (
  endpoint = 'https://push.example/abc',
): MockSubscription => ({
  endpoint,
  toJSON: () => ({ endpoint, keys: { p256dh: 'p', auth: 'a' } }),
  unsubscribe: vi.fn().mockResolvedValue(true),
});

const buildMockRegistration = (
  existing: MockSubscription | null = null,
): MockRegistration => ({
  pushManager: {
    getSubscription: vi.fn().mockResolvedValue(existing),
    subscribe: vi.fn().mockResolvedValue(buildMockSubscription()),
  },
});

const installPushSupport = (registration: MockRegistration | null) => {
  // Notification (constructor + permission) — vitest jsdom may not provide it.
  vi.stubGlobal('Notification', {
    permission: 'default',
    requestPermission: vi.fn().mockResolvedValue('granted'),
  } as unknown as typeof Notification);

  vi.stubGlobal('PushManager', class {});

  // navigator.serviceWorker
  Object.defineProperty(navigator, 'serviceWorker', {
    configurable: true,
    value: {
      getRegistration: vi.fn().mockResolvedValue(registration),
      register: vi.fn().mockResolvedValue(registration ?? buildMockRegistration()),
    },
  });
};

const removePushSupport = () => {
  vi.unstubAllGlobals();
  // @ts-expect-error — intentionally remove the prop for negative tests
  delete navigator.serviceWorker;
};

beforeEach(() => {
  process.env = { ...ORIGINAL_ENV };
});

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  process.env = { ...ORIGINAL_ENV };
});

describe('urlBase64ToUint8Array', () => {
  it('decodes base64url with padding', () => {
    const out = urlBase64ToUint8Array('AQID'); // bytes [1,2,3]
    expect(Array.from(out)).toEqual([1, 2, 3]);
  });

  it('handles url-safe characters (- and _)', () => {
    // base64 'a-_/' would be 'a+/' in standard; the helper translates - and _
    // back to + and /. Check it doesn't throw and returns expected length.
    const out = urlBase64ToUint8Array('a-_a');
    expect(out.byteLength).toBe(3);
  });
});

describe('isPushSupported', () => {
  it('returns false when serviceWorker missing', () => {
    removePushSupport();
    expect(isPushSupported()).toBe(false);
  });

  it('returns true when full API surface present', () => {
    installPushSupport(buildMockRegistration());
    expect(isPushSupported()).toBe(true);
  });
});

describe('subscribe', () => {
  it('returns "unsupported" when serviceWorker missing', async () => {
    removePushSupport();
    const result = await subscribe();
    expect(result.outcome).toBe('unsupported');
  });

  it('returns "not-configured" when VAPID key missing', async () => {
    installPushSupport(buildMockRegistration());
    delete process.env.NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY;
    const result = await subscribe();
    expect(result.outcome).toBe('not-configured');
  });

  it('returns "denied" when user rejects permission', async () => {
    installPushSupport(buildMockRegistration());
    process.env.NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY = 'AQID';
    vi.stubGlobal('Notification', {
      permission: 'default',
      requestPermission: vi.fn().mockResolvedValue('denied'),
    } as unknown as typeof Notification);
    const result = await subscribe();
    expect(result.outcome).toBe('denied');
  });

  it('returns "subscribed" with endpoint on happy path', async () => {
    installPushSupport(buildMockRegistration());
    process.env.NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY = 'AQID';
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: true, status: 200 }),
    );

    const result = await subscribe();
    expect(result.outcome).toBe('subscribed');
    expect(result.endpoint).toBe('https://push.example/abc');
    expect(fetch).toHaveBeenCalledWith(
      '/api/push/subscribe',
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('returns "error" when backend POST fails', async () => {
    installPushSupport(buildMockRegistration());
    process.env.NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY = 'AQID';
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 500 }),
    );

    const result = await subscribe();
    expect(result.outcome).toBe('error');
    expect(result.error).toContain('500');
  });

  it('reuses existing subscription instead of re-creating', async () => {
    const existing = buildMockSubscription('https://push.example/old');
    const reg = buildMockRegistration(existing);
    installPushSupport(reg);
    process.env.NEXT_PUBLIC_WEB_PUSH_VAPID_PUBLIC_KEY = 'AQID';
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200 }));

    const result = await subscribe();
    expect(result.outcome).toBe('subscribed');
    expect(result.endpoint).toBe('https://push.example/old');
    expect(reg.pushManager.subscribe).not.toHaveBeenCalled();
  });
});

describe('unsubscribe', () => {
  it('returns "unsupported" when serviceWorker missing', async () => {
    removePushSupport();
    const result = await unsubscribe();
    expect(result.outcome).toBe('unsupported');
  });

  it('returns "unsubscribed" when no registration exists', async () => {
    installPushSupport(null);
    const result = await unsubscribe();
    expect(result.outcome).toBe('unsubscribed');
  });

  it('unsubscribes locally + posts to backend on happy path', async () => {
    const subRecord = buildMockSubscription();
    installPushSupport(buildMockRegistration(subRecord));
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200 }));

    const result = await unsubscribe();
    expect(result.outcome).toBe('unsubscribed');
    expect(result.endpoint).toBe('https://push.example/abc');
    expect(subRecord.unsubscribe).toHaveBeenCalled();
    expect(fetch).toHaveBeenCalledWith(
      '/api/push/unsubscribe',
      expect.objectContaining({ method: 'POST' }),
    );
  });
});

describe('getSubscriptionState', () => {
  it('returns "unsupported" when serviceWorker missing', async () => {
    removePushSupport();
    expect(await getSubscriptionState()).toBe('unsupported');
  });

  it('returns "denied" when permission denied', async () => {
    installPushSupport(buildMockRegistration());
    vi.stubGlobal('Notification', {
      permission: 'denied',
      requestPermission: vi.fn(),
    } as unknown as typeof Notification);
    expect(await getSubscriptionState()).toBe('denied');
  });

  it('returns "not-subscribed" when no SW registration', async () => {
    installPushSupport(null);
    expect(await getSubscriptionState()).toBe('not-subscribed');
  });

  it('returns "subscribed" when subscription exists', async () => {
    installPushSupport(buildMockRegistration(buildMockSubscription()));
    expect(await getSubscriptionState()).toBe('subscribed');
  });
});
