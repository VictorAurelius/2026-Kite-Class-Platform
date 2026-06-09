// shell-exempt: redirect-only route (never renders UI), no dashboard chrome needed
/**
 * `/student` redirects to `/student/today` — the canonical landing per the
 * `kiteclass-student` HTML kit (today.html is screen #1).
 *
 * Wave 49 Bucket C (GAP-269).
 */
import { redirect } from 'next/navigation';

export default function StudentIndexPage() {
  redirect('/student/today');
}
