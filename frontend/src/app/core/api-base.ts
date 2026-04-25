import { Capacitor } from '@capacitor/core';

import { mobileApiBaseUrl } from './mobile-config';

type RuntimeConfig = typeof globalThis & {
  __PACR_API_BASE_URL__?: string;
};

/**
 * Resolves the API base URL based on the runtime environment.
 *
 * Priority:
 *   1. Explicit override via globalThis.__PACR_API_BASE_URL__ (e.g. set in index.html for E2E tests)
 *   2. Native platform (iOS/Android via Capacitor): absolute URL from mobile-config
 *   3. Default (web browser): relative '/api', proxied by Nginx in production / Angular dev-server in dev
 */
function resolveDefaultApiBaseUrl(): string {
  if (Capacitor.isNativePlatform()) {
    return mobileApiBaseUrl;
  }
  return '/api';
}

export const apiBaseUrl = (
  (globalThis as RuntimeConfig).__PACR_API_BASE_URL__ ?? resolveDefaultApiBaseUrl()
).replace(/\/+$/, '');

export function apiUrl(path = ''): string {
  if (!path) return apiBaseUrl;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${apiBaseUrl}${normalizedPath}`;
}
