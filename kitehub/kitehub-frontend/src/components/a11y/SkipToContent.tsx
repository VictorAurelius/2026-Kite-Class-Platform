/**
 * Skip-to-content link (WCAG 2.4.1 Bypass Blocks, Level A).
 *
 * Visually hidden until focused; the first Tab on any page surfaces it so
 * keyboard / screen-reader users can jump past the nav/sidebar straight to
 * `#main-content`. GAP-1373 — shared so every top-level KiteHub layout wires
 * the same control instead of re-implementing it per layout.
 *
 * The consuming layout MUST render a `<main id="main-content">` target.
 */
export function SkipToContent({ targetId = 'main-content' }: { targetId?: string }) {
  return (
    <a
      href={`#${targetId}`}
      className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[100] focus:rounded focus:bg-primary focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-primary-foreground focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-primary"
    >
      Chuyển đến nội dung chính
    </a>
  );
}
