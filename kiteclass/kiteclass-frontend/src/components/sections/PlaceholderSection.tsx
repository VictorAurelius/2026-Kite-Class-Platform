/**
 * Placeholder section for sections not yet implemented.
 * Will be replaced with real components in PR-THEME-3 (CMS Slots).
 *
 * @since PR-THEME-2
 */

interface PlaceholderSectionProps {
  title: string;
  description?: string;
}

export function PlaceholderSection({ title, description }: PlaceholderSectionProps) {
  return (
    <section className="py-16">
      <div className="container mx-auto px-4 text-center">
        <h2 className="text-3xl font-bold mb-4">{title}</h2>
        <p className="text-muted-foreground">
          {description || 'Nội dung đang được cập nhật. Vui lòng quay lại sau.'}
        </p>
      </div>
    </section>
  );
}
