/**
 * Loading state for public pages.
 * Displayed while async data is being fetched.
 *
 * @author KiteClass Team
 * @since 3.4.0
 */

import { Loader2 } from 'lucide-react';

export default function Loading() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8">
      <Loader2 className="h-12 w-12 animate-spin text-primary mb-4" />
      <p className="text-muted-foreground">Đang tải...</p>
    </div>
  );
}
