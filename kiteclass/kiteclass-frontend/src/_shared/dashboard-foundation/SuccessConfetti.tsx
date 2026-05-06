/**
 * SuccessConfetti — vanilla canvas confetti utility (no new deps).
 *
 * Source spec: kiteclass-pro-v2/screens/success-confetti.html.
 *
 * Two surfaces:
 *   - `fireConfetti()` imperative API — call from event handlers
 *     (e.g. after invoice paid, class created). Mounts a transient canvas
 *     overlay, animates ~1.5s, removes itself.
 *   - `<SuccessConfetti trigger>` declarative component — fires once each
 *     time `trigger` becomes truthy.
 *
 * Respects `prefers-reduced-motion`: caller still gets the success state,
 * but no canvas mount.
 */

'use client';

import { useEffect, useRef } from 'react';

interface ConfettiOptions {
  /** Number of particles. Defaults to 80. */
  count?: number;
  /** Duration in ms. Defaults to 1500. */
  duration?: number;
  /** Optional palette — defaults to KC pro v2 brand mix. */
  colors?: string[];
}

const DEFAULT_COLORS = ['#6366F1', '#22D3EE', '#22C55E', '#F59E0B', '#EF4444'];

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  rotation: number;
  rotationSpeed: number;
  size: number;
  color: string;
  shape: 'rect' | 'circle';
}

function prefersReducedMotion(): boolean {
  if (typeof window === 'undefined') return true;
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

function createParticles(count: number, colors: string[], width: number): Particle[] {
  const particles: Particle[] = [];
  for (let i = 0; i < count; i += 1) {
    particles.push({
      x: width / 2 + (Math.random() - 0.5) * 80,
      y: -20,
      vx: (Math.random() - 0.5) * 8,
      vy: Math.random() * 4 + 4,
      rotation: Math.random() * Math.PI * 2,
      rotationSpeed: (Math.random() - 0.5) * 0.3,
      size: Math.random() * 6 + 4,
      color: colors[Math.floor(Math.random() * colors.length)] ?? colors[0] ?? '#6366F1',
      shape: Math.random() > 0.5 ? 'rect' : 'circle',
    });
  }
  return particles;
}

export function fireConfetti(options: ConfettiOptions = {}): void {
  if (typeof document === 'undefined') return;
  if (prefersReducedMotion()) return;

  const { count = 80, duration = 1500, colors = DEFAULT_COLORS } = options;

  const canvas = document.createElement('canvas');
  canvas.style.position = 'fixed';
  canvas.style.inset = '0';
  canvas.style.width = '100vw';
  canvas.style.height = '100vh';
  canvas.style.pointerEvents = 'none';
  canvas.style.zIndex = '9999';
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
  canvas.setAttribute('aria-hidden', 'true');
  canvas.setAttribute('data-testid', 'success-confetti-canvas');
  document.body.appendChild(canvas);

  const ctx = canvas.getContext('2d');
  if (!ctx) {
    document.body.removeChild(canvas);
    return;
  }

  const particles = createParticles(count, colors, canvas.width);
  const startedAt = performance.now();
  const gravity = 0.18;

  const tick = (now: number) => {
    const elapsed = now - startedAt;
    if (elapsed > duration) {
      if (canvas.parentNode) canvas.parentNode.removeChild(canvas);
      return;
    }
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    for (const p of particles) {
      p.vy += gravity;
      p.x += p.vx;
      p.y += p.vy;
      p.rotation += p.rotationSpeed;

      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate(p.rotation);
      ctx.fillStyle = p.color;
      if (p.shape === 'rect') {
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6);
      } else {
        ctx.beginPath();
        ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();
    }
    requestAnimationFrame(tick);
  };

  requestAnimationFrame(tick);
}

export interface SuccessConfettiProps {
  /** When this value flips truthy, fire confetti once. */
  trigger: unknown;
  options?: ConfettiOptions;
}

/**
 * Declarative wrapper — fires `fireConfetti(options)` whenever `trigger`
 * becomes truthy. Useful for binding to a success state in TanStack Query.
 */
export function SuccessConfetti({ trigger, options }: SuccessConfettiProps) {
  const last = useRef<unknown>(undefined);

  useEffect(() => {
    if (trigger && trigger !== last.current) {
      fireConfetti(options);
      last.current = trigger;
    }
    if (!trigger) {
      last.current = trigger;
    }
  }, [options, trigger]);

  return null;
}
