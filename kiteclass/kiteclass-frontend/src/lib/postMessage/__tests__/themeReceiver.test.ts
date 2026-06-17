/**
 * Tests for postMessage theme receiver.
 *
 * @since PR-THEME-1
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { initThemeReceiver, ALLOWED_ORIGINS, type ThemeUpdateCallback } from '../themeReceiver';
import type { ThemeMessage } from '@/lib/theme/types';
import { DEFAULT_THEME } from '@/lib/theme/defaultTheme';

describe('Theme Receiver', () => {
  let cleanup: (() => void) | null = null;
  let mockCallback: ThemeUpdateCallback;

  beforeEach(() => {
    mockCallback = vi.fn<ThemeUpdateCallback>();
    cleanup = null;
  });

  afterEach(() => {
    if (cleanup) {
      cleanup();
    }
  });

  describe('initThemeReceiver', () => {
    it('should add message event listener', () => {
      const addEventListenerSpy = vi.spyOn(window, 'addEventListener');

      cleanup = initThemeReceiver(mockCallback);

      expect(addEventListenerSpy).toHaveBeenCalledWith(
        'message',
        expect.any(Function)
      );

      addEventListenerSpy.mockRestore();
    });

    it('should return cleanup function', () => {
      cleanup = initThemeReceiver(mockCallback);

      expect(typeof cleanup).toBe('function');
    });

    it('should remove event listener when cleanup is called', () => {
      const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

      cleanup = initThemeReceiver(mockCallback);
      cleanup();

      expect(removeEventListenerSpy).toHaveBeenCalledWith(
        'message',
        expect.any(Function)
      );

      removeEventListenerSpy.mockRestore();
    });
  });

  describe('Message handling', () => {
    it('should call callback with valid theme message', () => {
      cleanup = initThemeReceiver(mockCallback);

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: 'http://localhost:4701', // KiteHub origin
      });

      window.dispatchEvent(event);

      expect(mockCallback).toHaveBeenCalledWith(DEFAULT_THEME);
    });

    it('should ignore messages with wrong type', () => {
      cleanup = initThemeReceiver(mockCallback);

      const wrongTypeMessage = {
        type: 'WRONG_TYPE',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: wrongTypeMessage,
        origin: 'http://localhost:4701',
      });

      window.dispatchEvent(event);

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it('should ignore messages with invalid theme (no colors)', () => {
      cleanup = initThemeReceiver(mockCallback);

      const invalidMessage = {
        type: 'APPLY_THEME',
        theme: {
          fonts: { heading: 'Inter' }, // no colors.primary → invalid
        },
      };

      const event = new MessageEvent('message', {
        data: invalidMessage,
        origin: 'http://localhost:4701',
      });

      window.dispatchEvent(event);

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it('should ignore messages from untrusted origins', () => {
      cleanup = initThemeReceiver(mockCallback);

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: 'https://evil.com', // untrusted
      });

      window.dispatchEvent(event);

      expect(mockCallback).not.toHaveBeenCalled();
    });

    it('should accept messages from same origin (self-post)', () => {
      // A page posting a theme message to itself (sendThemeToChild defaults to
      // window.parent === window on a standalone tenant landing). Same-origin is
      // inherently trusted regardless of dev/production build — covers production
      // tenant subdomains without enumerating them in the static allowlist.
      cleanup = initThemeReceiver(mockCallback);

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: window.location.origin, // self / same-origin
      });

      window.dispatchEvent(event);

      expect(mockCallback).toHaveBeenCalledWith(DEFAULT_THEME);
    });

    it('should accept messages from localhost:4701 (KiteHub)', () => {
      cleanup = initThemeReceiver(mockCallback);

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: 'http://localhost:4701',
      });

      window.dispatchEvent(event);

      expect(mockCallback).toHaveBeenCalledWith(DEFAULT_THEME);
    });

    it('should accept messages from production KiteHub', () => {
      cleanup = initThemeReceiver(mockCallback);

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: 'https://kitehub.me',
      });

      window.dispatchEvent(event);

      expect(mockCallback).toHaveBeenCalledWith(DEFAULT_THEME);
    });

    it('should accept custom theme variations', () => {
      cleanup = initThemeReceiver(mockCallback);

      const customTheme = {
        ...DEFAULT_THEME,
        colors: {
          ...DEFAULT_THEME.colors,
          primary: '#DC2626',
        },
      };

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: customTheme,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: 'http://localhost:4701',
      });

      window.dispatchEvent(event);

      expect(mockCallback).toHaveBeenCalledWith(customTheme);
    });

    it('should handle malformed message data gracefully', () => {
      cleanup = initThemeReceiver(mockCallback);

      const event = new MessageEvent('message', {
        data: 'not an object',
        origin: 'http://localhost:4701',
      });

      // Should not throw
      expect(() => window.dispatchEvent(event)).not.toThrow();
      expect(mockCallback).not.toHaveBeenCalled();
    });

    it('should handle null message data', () => {
      cleanup = initThemeReceiver(mockCallback);

      const event = new MessageEvent('message', {
        data: null,
        origin: 'http://localhost:4701',
      });

      expect(() => window.dispatchEvent(event)).not.toThrow();
      expect(mockCallback).not.toHaveBeenCalled();
    });
  });

  describe('ALLOWED_ORIGINS', () => {
    it('should include localhost origins', () => {
      expect(ALLOWED_ORIGINS).toContain('http://localhost:4701');
      expect(ALLOWED_ORIGINS).toContain('http://localhost:4700');
    });

    it('should include production origins', () => {
      expect(ALLOWED_ORIGINS).toContain('https://kitehub.me');
    });
  });

  describe('Multiple receivers', () => {
    it('should support multiple concurrent receivers', () => {
      const callback1 = vi.fn();
      const callback2 = vi.fn();

      const cleanup1 = initThemeReceiver(callback1);
      const cleanup2 = initThemeReceiver(callback2);

      const validMessage: ThemeMessage = {
        type: 'APPLY_THEME',
        theme: DEFAULT_THEME,
      };

      const event = new MessageEvent('message', {
        data: validMessage,
        origin: 'http://localhost:4701',
      });

      window.dispatchEvent(event);

      // Both callbacks should be called
      expect(callback1).toHaveBeenCalledWith(DEFAULT_THEME);
      expect(callback2).toHaveBeenCalledWith(DEFAULT_THEME);

      cleanup1();
      cleanup2();
    });
  });
});
