/**
 * Build tenant URL based on environment.
 *
 * Local:      http://localhost:4700?tenant={subdomain}
 * Production: https://{subdomain}.kiteclass.com
 *
 * Configured via NEXT_PUBLIC_KITECLASS_URL_PATTERN env var.
 * Pattern uses {subdomain} as placeholder.
 */

const DEFAULT_PATTERN = 'https://{subdomain}.kiteclass.com';

export function getTenantUrl(subdomain: string): string {
  const pattern = process.env.NEXT_PUBLIC_KITECLASS_URL_PATTERN || DEFAULT_PATTERN;
  return pattern.replace('{subdomain}', subdomain);
}

export function getTenantDisplayUrl(subdomain: string): string {
  const isLocal = process.env.NEXT_PUBLIC_KITECLASS_URL_PATTERN?.includes('localhost');
  if (isLocal) {
    return `${subdomain} (local)`;
  }
  return `${subdomain}.kiteclass.com`;
}
