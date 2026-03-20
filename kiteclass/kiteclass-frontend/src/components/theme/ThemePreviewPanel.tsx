'use client';

/**
 * Theme Preview Panel (Dev mode only)
 * Shows floating panel to test theme colors live.
 * Only renders when ?preview=theme query param is present.
 *
 * Usage: http://localhost:3000?preview=theme
 *
 * @since PR-THEME-3 fix
 */

import { useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useTheme } from '@/contexts/ThemeContext';
import { Palette, X, RotateCcw } from 'lucide-react';

export function ThemePreviewPanel() {
  const searchParams = useSearchParams();
  const showPreview = searchParams.get('preview') === 'theme';
  const { theme, setTheme, resetTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(true);

  if (!showPreview) return null;
  if (!isOpen) {
    return (
      <button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-4 right-4 z-50 rounded-full bg-primary p-3 text-white shadow-lg hover:bg-primary/90"
        title="Mở Theme Preview"
      >
        <Palette className="h-5 w-5" />
      </button>
    );
  }

  const handleColorChange = (field: string, value: string) => {
    const newTheme = {
      ...theme,
      colors: { ...theme.colors, [field]: value },
    };
    setTheme(newTheme);

    // Also directly apply CSS variable for instant feedback
    document.documentElement.style.setProperty(`--theme-${field}`, value);
  };

  return (
    <div className="fixed bottom-4 right-4 z-50 w-72 rounded-2xl border bg-card shadow-xl">
      {/* Header */}
      <div className="flex items-center justify-between border-b p-3">
        <div className="flex items-center gap-2">
          <Palette className="h-4 w-4 text-primary" />
          <span className="text-sm font-semibold">Theme Preview</span>
        </div>
        <button onClick={() => setIsOpen(false)} className="text-muted-foreground hover:text-foreground">
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Color Pickers */}
      <div className="space-y-3 p-3">
        <ColorPicker
          label="Primary"
          value={theme.colors.primary}
          onChange={(v) => handleColorChange('primary', v)}
        />
        <ColorPicker
          label="Secondary"
          value={theme.colors.secondary}
          onChange={(v) => handleColorChange('secondary', v)}
        />
        <ColorPicker
          label="Accent"
          value={theme.colors.accent}
          onChange={(v) => handleColorChange('accent', v)}
        />
        <ColorPicker
          label="Background"
          value={theme.colors.background}
          onChange={(v) => handleColorChange('background', v)}
        />
      </div>

      {/* Actions */}
      <div className="border-t p-3">
        <button
          onClick={resetTheme}
          className="flex w-full items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm hover:bg-muted"
        >
          <RotateCcw className="h-3.5 w-3.5" />
          Reset về mặc định
        </button>
      </div>

      {/* Current Values */}
      <div className="border-t p-3">
        <p className="text-xs text-muted-foreground mb-1">CSS Variables:</p>
        <code className="text-[10px] text-muted-foreground block">
          --theme-primary: {theme.colors.primary}<br />
          --theme-secondary: {theme.colors.secondary}<br />
          --theme-accent: {theme.colors.accent}
        </code>
      </div>
    </div>
  );
}

function ColorPicker({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div className="flex items-center justify-between">
      <label className="text-sm">{label}</label>
      <div className="flex items-center gap-2">
        <input
          type="color"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="h-7 w-7 cursor-pointer rounded border-0 bg-transparent"
        />
        <span className="text-xs text-muted-foreground font-mono w-16">{value}</span>
      </div>
    </div>
  );
}
