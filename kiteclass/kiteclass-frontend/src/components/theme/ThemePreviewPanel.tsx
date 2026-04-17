'use client';

/**
 * Theme Preview Panel (Dev mode only)
 * Shows floating panel to test theme colors live.
 * Only renders when ?preview=theme query param is present.
 *
 * Usage: http://localhost:4700?preview=theme
 *
 * @since PR-THEME-3 fix
 */

import { useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { useTheme } from '@/contexts/ThemeContext';
import { Palette, X, RotateCcw, Check } from 'lucide-react';

export function ThemePreviewPanel() {
  const searchParams = useSearchParams();
  const showPreview = searchParams.get('preview') === 'theme';
  const { theme, setTheme, resetTheme } = useTheme();
  const [isOpen, setIsOpen] = useState(true);

  // Draft colors (not applied yet, only on Apply click)
  const [draft, setDraft] = useState({ ...theme.colors });
  const [hasChanges, setHasChanges] = useState(false);

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

  const handleDraftChange = (field: string, value: string) => {
    setDraft(prev => ({ ...prev, [field]: value }));
    setHasChanges(true);
  };

  const handleApply = () => {
    // Apply draft colors to theme
    // ThemeContext will automatically apply CSS variables via useEffect
    const newTheme = {
      ...theme,
      colors: { ...theme.colors, ...draft },
    };
    setTheme(newTheme);

    setHasChanges(false);
  };

  const handleReset = () => {
    // Reset to default theme
    // ThemeContext will automatically apply CSS variables via useEffect
    resetTheme();
    setDraft({
      primary: '#3B82F6',
      secondary: '#8B5CF6',
      accent: '#F59E0B',
      background: '#FFFFFF',
    });
    setHasChanges(false);
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
          value={draft.primary}
          onChange={(v) => handleDraftChange('primary', v)}
        />
        <ColorPicker
          label="Secondary"
          value={draft.secondary}
          onChange={(v) => handleDraftChange('secondary', v)}
        />
        <ColorPicker
          label="Accent"
          value={draft.accent}
          onChange={(v) => handleDraftChange('accent', v)}
        />
        <ColorPicker
          label="Background"
          value={draft.background}
          onChange={(v) => handleDraftChange('background', v)}
        />
      </div>

      {/* Apply Button */}
      <div className="border-t p-3 space-y-2">
        <button
          onClick={handleApply}
          disabled={!hasChanges}
          className={`flex w-full items-center justify-center gap-2 rounded-lg px-3 py-2.5 text-sm font-semibold transition-colors ${
            hasChanges
              ? 'bg-primary text-white hover:bg-primary/90'
              : 'bg-muted text-muted-foreground cursor-not-allowed'
          }`}
        >
          <Check className="h-4 w-4" />
          Áp dụng Theme
        </button>
        <button
          onClick={handleReset}
          className="flex w-full items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm hover:bg-muted"
        >
          <RotateCcw className="h-3.5 w-3.5" />
          Reset về mặc định
        </button>
      </div>

      {/* Preview Colors */}
      <div className="border-t p-3">
        <p className="text-xs text-muted-foreground mb-2">Xem trước:</p>
        <div className="flex gap-2">
          <div className="h-8 w-8 rounded-lg border" style={{ backgroundColor: draft.primary }} title="Primary" />
          <div className="h-8 w-8 rounded-lg border" style={{ backgroundColor: draft.secondary }} title="Secondary" />
          <div className="h-8 w-8 rounded-lg border" style={{ backgroundColor: draft.accent }} title="Accent" />
          <div className="h-8 w-8 rounded-lg border" style={{ backgroundColor: draft.background }} title="Background" />
        </div>
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
