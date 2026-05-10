/**
 * /school-admin index — redirect to dashboard.
 *
 * Wave 50 Bucket A (GAP-271).
 */

import { redirect } from 'next/navigation';

export default function SchoolAdminIndex() {
  redirect('/school-admin/dashboard');
}
