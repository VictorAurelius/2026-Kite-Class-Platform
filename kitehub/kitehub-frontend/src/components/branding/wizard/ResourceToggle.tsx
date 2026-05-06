'use client';

// ---------------------------------------------------------------------------
// ResourceToggle — per-resource approve checkbox for Step 6 (ApprovalStep)
// Owned by Wave 32 Bucket C.
// ---------------------------------------------------------------------------

import { Check } from 'lucide-react';

interface ResourceToggleProps {
  resource: string;
  label: string;
  icon: string;
  approved: boolean;
  onApprove: () => void;
}

export function ResourceToggle({ label, icon, approved, onApprove }: ResourceToggleProps) {
  return (
    <div
      role="checkbox"
      aria-checked={approved}
      aria-label={label}
      onClick={onApprove}
      className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors select-none ${
        approved
          ? 'border-primary bg-primary/5'
          : 'border-border hover:border-primary/50'
      }`}
    >
      <span className="text-xl" aria-hidden="true">
        {icon}
      </span>
      <span className="flex-1 text-sm font-medium">{label}</span>
      {approved && (
        <Check className="h-4 w-4 text-primary" aria-hidden="true" />
      )}
    </div>
  );
}
