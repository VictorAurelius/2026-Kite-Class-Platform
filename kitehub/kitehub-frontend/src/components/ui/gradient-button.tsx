import { cn } from '@/lib/utils';
import { type ButtonHTMLAttributes, forwardRef } from 'react';

interface GradientButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  size?: 'default' | 'lg' | 'sm';
}

const GradientButton = forwardRef<HTMLButtonElement, GradientButtonProps>(
  ({ className, size = 'default', children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center rounded-xl font-semibold text-white',
          'bg-gradient-to-r from-primary via-primary/90 to-accent',
          'hover:shadow-soft-lg hover:scale-[1.02] active:scale-[0.98]',
          'transition-all duration-200',
          'disabled:opacity-50 disabled:pointer-events-none',
          size === 'sm' && 'h-9 px-4 text-sm',
          size === 'default' && 'h-11 px-6 text-sm',
          size === 'lg' && 'h-14 px-8 text-base',
          className
        )}
        {...props}
      >
        {children}
      </button>
    );
  }
);
GradientButton.displayName = 'GradientButton';

export { GradientButton };
