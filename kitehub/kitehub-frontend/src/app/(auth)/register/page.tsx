/**
 * /register — Phase 1 BETA invite-only redirect (GAP-372 closure follow-up #2).
 *
 * Public self-signup is disabled during Phase 1 BETA. This route now redirects
 * server-side to /auth/request-beta-access, where visitors submit a beta access
 * request that the coordinator manually approves.
 *
 * @since Wave 45 — GAP-372 closure follow-up
 */
import { redirect } from 'next/navigation';

export const metadata = {
  title: 'Đăng ký — Beta giới hạn',
};

export default function RegisterPage() {
  redirect('/auth/request-beta-access');
}
