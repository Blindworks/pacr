type RuntimeConfig = typeof globalThis & {
  __PACR_API_BASE_URL__?: string;
};

function resolveDefaultApiBaseUrl(): string {
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
