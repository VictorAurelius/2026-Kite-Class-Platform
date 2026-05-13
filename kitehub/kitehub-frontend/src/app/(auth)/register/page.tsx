/**
 * /register — Phase 1 BETA invite-only redirect (GAP-372 closure follow-up #2).
 *
 * Public self-signup is disabled during Phase 1 BETA. This route now redirects
 * server-side to /request-beta-access, where visitors submit a beta access
 * request that the coordinator manually approves.
 *
 * URL is `/request-beta-access` (NOT `/auth/request-beta-access`) — `(auth)/` is
 * a Next.js route group (parentheses = code organization, NOT URL segment).
 *
 * @since Wave 45 — GAP-372 closure follow-up
 */
import { redirect } from 'next/navigation';

export const metadata = {
  title: 'Đăng ký — Beta giới hạn',
};

export default function RegisterPage() {
  redirect('/request-beta-access');
}
