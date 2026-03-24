type RuntimeConfig = typeof globalThis & {
  __PACR_API_BASE_URL__?: string;
};

function resolveDefaultApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return 'http://localhost:8080/api';
  }

  return `${window.location.protocol}//${window.location.hostname}:8080/api`;
}

export const apiBaseUrl = (
  (globalThis as RuntimeConfig).__PACR_API_BASE_URL__ ?? resolveDefaultApiBaseUrl()
).replace(/\/+$/, '');

export function apiUrl(path = ''): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${apiBaseUrl}${normalizedPath}`;
}
