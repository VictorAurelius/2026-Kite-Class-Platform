/**
 * Form select component with react-hook-form integration.
 *
 * @author KiteClass Team
 * @since 1.0.0
 */

'use client';

import { forwardRef } from 'react';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { cn } from '@/lib/utils';

export interface SelectOption {
  value: string;
  label: string;
}

export interface FormSelectProps {
  label?: string;
  placeholder?: string;
  options: SelectOption[];
  value?: string;
  onValueChange?: (value: string) => void;
  error?: string;
  helperText?: string;
  required?: boolean;
  disabled?: boolean;
  className?: string;
  id?: string;
}

export const FormSelect = forwardRef<HTMLButtonElement, FormSelectProps>(
  ({ label, placeholder, options, value, onValueChange, error, helperText, required, disabled, className, id }, ref) => {
    const selectId = id || `select-${label?.toLowerCase().replace(/\s+/g, '-')}`;

    return (
      <div className="space-y-2">
        {label && (
          <Label htmlFor={selectId} className={error ? 'text-destructive' : ''}>
            {label}
            {required && <span className="ml-1 text-destructive">*</span>}
          </Label>
        )}
        <Select value={value} onValueChange={onValueChange} disabled={disabled}>
          <SelectTrigger
            id={selectId}
            ref={ref}
            className={cn(error && 'border-destructive focus:ring-destructive', className)}
            aria-invalid={!!error}
            aria-describedby={error ? `${selectId}-error` : helperText ? `${selectId}-helper` : undefined}
          >
            <SelectValue placeholder={placeholder || 'Chọn...'} />
          </SelectTrigger>
          <SelectContent>
            {options.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {error && (
          <p id={`${selectId}-error`} className="text-sm text-destructive" aria-live="polite">
            {error}
          </p>
        )}
        {!error && helperText && (
          <p id={`${selectId}-helper`} className="text-sm text-muted-foreground">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

FormSelect.displayName = 'FormSelect';
