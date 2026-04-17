/**
 * E2E tests for Theme System.
 *
 * Tests theme loading, persistence, postMessage updates, and CSS variable application.
 *
 * @since PR-THEME-1 (Task #11)
 */

import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';

// Test data
const customTheme = {
  colors: {
    primary: '#DC2626', // red-600
    secondary: '#EF4444', // red-500
    accent: '#F59E0B', // amber-500
    background: '#FFFFFF',
  },
  fonts: {
    heading: 'Inter',
    body: 'Inter',
  },
  borderRadius: '12px',
  shadows: {
    sm: '0 1px 2px rgba(0,0,0,0.05)',
    md: '0 4px 6px rgba(0,0,0,0.07)',
    lg: '0 10px 15px rgba(0,0,0,0.1)',
  },
};

const defaultTheme = {
  colors: {
    primary: '#3B82F6', // blue-500
    secondary: '#8B5CF6', // violet-500
    accent: '#F59E0B', // amber-500
    background: '#FFFFFF',
  },
};

/**
 * Helper: Get CSS variable value from :root
 */
async function getCSSVariable(page: Page, varName: string): Promise<string> {
  return await page.evaluate((name) => {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  }, varName);
}

/**
 * Helper: Send theme via postMessage
 */
async function sendThemeMessage(page: Page, theme: typeof customTheme): Promise<void> {
  await page.evaluate((themeData) => {
    window.postMessage(
      {
        type: 'APPLY_THEME',
        theme: themeData,
      },
      '*'
    );
  }, theme);
}

/**
 * Helper: Clear localStorage theme
 */
async function clearThemeStorage(page: Page): Promise<void> {
  await page.evaluate(() => {
    localStorage.removeItem('kiteclass_theme');
  });
}

/**
 * Helper: Get theme from localStorage
 */
async function getStoredTheme(page: Page): Promise<any> {
  return await page.evaluate(() => {
    const stored = localStorage.getItem('kiteclass_theme');
    return stored ? JSON.parse(stored) : null;
  });
}

test.describe('Theme System E2E', () => {
  test.beforeEach(async ({ page }) => {
    // Clear theme before each test
    await page.goto('/');
    await clearThemeStorage(page);
    await page.reload();
  });

  test.describe('Default Theme', () => {
    test('should load default theme on first visit', async ({ page }) => {
      await page.goto('/');

      // Wait for page to load
      await page.waitForLoadState('networkidle');

      // Check CSS variables are set
      const primary = await getCSSVariable(page, '--theme-primary');
      const secondary = await getCSSVariable(page, '--theme-secondary');
      const accent = await getCSSVariable(page, '--theme-accent');

      // Should have default theme colors
      expect(primary).toBe(defaultTheme.colors.primary);
      expect(secondary).toBe(defaultTheme.colors.secondary);
      expect(accent).toBe(defaultTheme.colors.accent);
    });

    test('should apply default theme to DOM elements', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Check that elements with theme classes exist and are styled
      const heroSection = page.locator('section').first();
      await expect(heroSection).toBeVisible();

      // Check computed styles use theme variables
      const primaryText = page.locator('.text-theme-primary').first();
      if (await primaryText.count() > 0) {
        const color = await primaryText.evaluate((el) => {
          return getComputedStyle(el).color;
        });
        // Should be using CSS variable (computed to RGB value)
        expect(color).toBeTruthy();
      }
    });
  });

  test.describe('Theme Persistence', () => {
    test('should persist custom theme to localStorage', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Send custom theme via postMessage
      await sendThemeMessage(page, customTheme);

      // Wait for theme to be applied
      await page.waitForTimeout(500);

      // Check localStorage
      const stored = await getStoredTheme(page);
      expect(stored).toBeTruthy();
      expect(stored.colors.primary).toBe(customTheme.colors.primary);
      expect(stored.colors.secondary).toBe(customTheme.colors.secondary);
    });

    test('should load persisted theme on page reload', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Set custom theme
      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      // Reload page
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Check CSS variables are still custom theme
      const primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe(customTheme.colors.primary);
    });

    test('should persist theme across navigation', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Set custom theme
      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      // Navigate to another page
      const catalogLink = page.getByRole('link', { name: /khóa học/i }).first();
      if (await catalogLink.isVisible()) {
        await catalogLink.click();
        await page.waitForLoadState('networkidle');

        // Check theme is still applied
        const primary = await getCSSVariable(page, '--theme-primary');
        expect(primary).toBe(customTheme.colors.primary);
      }
    });
  });

  test.describe('postMessage Theme Updates', () => {
    test('should update theme via postMessage', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Initial theme should be default
      let primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe(defaultTheme.colors.primary);

      // Send custom theme via postMessage
      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      // Theme should be updated
      primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe(customTheme.colors.primary);
    });

    test('should update multiple CSS variables', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      // Check all color variables
      const primary = await getCSSVariable(page, '--theme-primary');
      const secondary = await getCSSVariable(page, '--theme-secondary');
      const accent = await getCSSVariable(page, '--theme-accent');
      const background = await getCSSVariable(page, '--theme-background');

      expect(primary).toBe(customTheme.colors.primary);
      expect(secondary).toBe(customTheme.colors.secondary);
      expect(accent).toBe(customTheme.colors.accent);
      expect(background).toBe(customTheme.colors.background);
    });

    test('should update border radius and shadows', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      const borderRadius = await getCSSVariable(page, '--theme-border-radius');
      const shadowSm = await getCSSVariable(page, '--theme-shadow-sm');

      expect(borderRadius).toBe(customTheme.borderRadius);
      expect(shadowSm).toBe(customTheme.shadows.sm);
    });

    test('should handle rapid theme changes', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Send multiple theme changes rapidly
      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(100);

      const theme2 = {
        ...customTheme,
        colors: { ...customTheme.colors, primary: '#10B981' }, // green
      };
      await sendThemeMessage(page, theme2);
      await page.waitForTimeout(500);

      // Last theme should win
      const primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe('#10B981');
    });
  });

  test.describe('Invalid Theme Handling', () => {
    test('should ignore invalid postMessage data', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      const initialPrimary = await getCSSVariable(page, '--theme-primary');

      // Send invalid message (wrong type)
      await page.evaluate(() => {
        window.postMessage(
          {
            type: 'WRONG_TYPE',
            theme: {},
          },
          '*'
        );
      });
      await page.waitForTimeout(500);

      // Theme should not change
      const primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe(initialPrimary);
    });

    test('should ignore malformed theme data', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      const initialPrimary = await getCSSVariable(page, '--theme-primary');

      // Send incomplete theme
      await page.evaluate(() => {
        window.postMessage(
          {
            type: 'APPLY_THEME',
            theme: {
              colors: { primary: '#123' }, // incomplete
            },
          },
          '*'
        );
      });
      await page.waitForTimeout(500);

      // Theme should not change (falls back to default)
      const primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe(initialPrimary);
    });

    test('should handle corrupted localStorage gracefully', async ({ page }) => {
      await page.goto('/');

      // Set invalid data in localStorage
      await page.evaluate(() => {
        localStorage.setItem('kiteclass_theme', 'invalid json');
      });

      // Reload page
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Should fall back to default theme
      const primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBe(defaultTheme.colors.primary);
    });
  });

  test.describe('Tailwind Utilities Integration', () => {
    test('should apply bg-theme-primary correctly', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Set custom theme
      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      // Find element with bg-theme-primary
      const button = page.locator('.bg-theme-primary').first();
      if (await button.count() > 0) {
        await expect(button).toBeVisible();

        // Check computed background color
        const bgColor = await button.evaluate((el) => {
          return getComputedStyle(el).backgroundColor;
        });

        // Should be using custom theme primary (computed to RGB)
        expect(bgColor).toBeTruthy();
        // RGB values should match red-600 (#DC2626)
        expect(bgColor).toContain('220'); // R value of #DC2626
      }
    });

    test('should apply text-theme-primary correctly', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(500);

      const textElement = page.locator('.text-theme-primary').first();
      if (await textElement.count() > 0) {
        await expect(textElement).toBeVisible();

        const color = await textElement.evaluate((el) => {
          return getComputedStyle(el).color;
        });

        expect(color).toBeTruthy();
      }
    });
  });

  test.describe('Theme Sync from Backend', () => {
    test('should sync backend colors on landing page', async ({ page }) => {
      // Navigate to landing page (public)
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Landing page should have ThemeSync component
      // If backend returns colors, they should be applied
      // (In this test, we use mock data so we expect default theme)

      const primary = await getCSSVariable(page, '--theme-primary');
      expect(primary).toBeTruthy();
    });
  });

  test.describe('Visual Regression', () => {
    test('should render consistently with default theme', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      // Wait for images and fonts
      await page.waitForTimeout(1000);

      // Check that key elements are visible
      const hero = page.locator('section').first();
      await expect(hero).toBeVisible();

      const heading = page.locator('h1').first();
      await expect(heading).toBeVisible();
    });

    test('should render consistently with custom theme', async ({ page }) => {
      await page.goto('/');
      await page.waitForLoadState('networkidle');

      await sendThemeMessage(page, customTheme);
      await page.waitForTimeout(1000);

      // Check that elements are still visible after theme change
      const hero = page.locator('section').first();
      await expect(hero).toBeVisible();

      const heading = page.locator('h1').first();
      await expect(heading).toBeVisible();
    });
  });
});
