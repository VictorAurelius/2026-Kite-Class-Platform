import { useState, useCallback } from 'react';
import { LogoAnalysis } from '@/types/branding';
import { ThemeConfig } from '@/types/theme';

interface UseThemeGenerationResult {
  themeConfig: ThemeConfig | null;
  isLoading: boolean;
  error: string | null;
  generateTheme: (analysis: LogoAnalysis) => Promise<void>;
}

/**
 * Hook to generate theme configuration from logo analysis.
 * Calls KiteHub AI branding service to create complete theme JSON.
 */
export function useThemeGeneration(): UseThemeGenerationResult {
  const [themeConfig, setThemeConfig] = useState<ThemeConfig | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const generateTheme = useCallback(async (analysis: LogoAnalysis) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/platform/branding/ai/generate-theme', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(analysis),
      });

      if (!response.ok) {
        throw new Error(`Failed to generate theme: ${response.statusText}`);
      }

      const config: ThemeConfig = await response.json();
      setThemeConfig(config);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error occurred';
      setError(message);
      console.error('Theme generation error:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    themeConfig,
    isLoading,
    error,
    generateTheme,
  };
}
