import { cn } from '@/lib/utils';
import { type HTMLAttributes } from 'react';

interface GradientTextProps extends HTMLAttributes<HTMLSpanElement> {
  as?: 'span' | 'h1' | 'h2' | 'h3' | 'p';
}

export function GradientText({ as: Tag = 'span', className, children, ...props }: GradientTextProps) {
  return (
    <Tag
      className={cn(
        'bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent',
        'bg-[length:200%_auto] animate-[gradient_3s_ease-in-out_infinite]',
        className
      )}
      {...props}
    >
      {children}
    </Tag>
  );
}
