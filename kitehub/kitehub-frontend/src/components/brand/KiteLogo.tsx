import { cn } from '@/lib/utils';

interface KiteLogoProps {
  className?: string;
  showText?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

const sizes = {
  sm: { icon: 24, text: 'text-lg' },
  md: { icon: 32, text: 'text-xl' },
  lg: { icon: 48, text: 'text-3xl' },
};

export function KiteLogo({ className, showText = true, size = 'md' }: KiteLogoProps) {
  const { icon, text } = sizes[size];

  return (
    <div className={cn('flex items-center gap-2', className)}>
      <svg
        width={icon}
        height={icon}
        viewBox="0 0 48 48"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="flex-shrink-0"
      >
        {/* Kite body - diamond shape */}
        <path
          d="M24 4L40 24L24 44L8 24L24 4Z"
          className="fill-primary"
        />
        {/* Kite cross struts */}
        <path
          d="M24 4V44M8 24H40"
          stroke="white"
          strokeWidth="2"
          strokeOpacity="0.3"
        />
        {/* Kite center */}
        <circle cx="24" cy="24" r="4" className="fill-accent" />
        {/* Kite tail */}
        <path
          d="M24 44C24 44 20 48 18 50C16 52 22 52 24 50C26 52 32 52 30 50C28 48 24 44 24 44Z"
          className="fill-accent"
          opacity="0.8"
        />
        {/* Decorative bows on tail */}
        <circle cx="20" cy="52" r="2" className="fill-primary" opacity="0.6" />
        <circle cx="28" cy="54" r="2" className="fill-primary" opacity="0.6" />
      </svg>
      {showText && (
        <span className={cn('font-bold tracking-tight', text)}>
          <span className="text-primary">Kite</span>
          <span className="text-foreground">Hub</span>
        </span>
      )}
    </div>
  );
}
