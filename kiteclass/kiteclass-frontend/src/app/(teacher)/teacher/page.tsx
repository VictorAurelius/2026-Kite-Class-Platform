/**
 * (teacher) root — redirects to /teacher/dashboard.
 *
 * @since Wave 49 Bucket B (GAP-268)
 */

import { redirect } from 'next/navigation';

export default function TeacherRootPage() {
  redirect('/teacher/dashboard');
}
