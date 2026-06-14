import { useState, useCallback } from 'react';
import { apiClient } from '@/lib/api/client';
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
      // GAP-1336: generate-theme is a POST (@PostMapping("/generate-theme")) on the
      // OWNER-gated AI branding service. Use the shared apiClient so the request
      // carries the Authorization + X-Tenant-Id headers the endpoint requires
      // (the prior raw fetch sent neither, and the multi-line `method` option also
      // read as GET in the static FE↔BE contract check → 405/401 drift).
      const response = await apiClient.post('/api/platform/branding/ai/generate-theme', analysis);
      const config = response.data as ThemeConfig;
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
