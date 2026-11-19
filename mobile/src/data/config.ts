const DEFAULT_API_URL = 'http://localhost:8080';

export function resolveApiUrl(raw: string | undefined): string {
  return (raw || DEFAULT_API_URL).replace(/\/+$/, '');
}

export function toWsUrl(apiUrl: string): string {
  return `${apiUrl.replace(/^http/, 'ws')}/ws`;
}

// Must be a static `process.env.EXPO_PUBLIC_*` reference so Expo inlines it at build time.
export const API_URL = resolveApiUrl(process.env.EXPO_PUBLIC_API_URL);
export const WS_URL = toWsUrl(API_URL);
