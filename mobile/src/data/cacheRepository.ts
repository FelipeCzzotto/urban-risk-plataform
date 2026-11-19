interface KeyValueStore {
  getItem(key: string): Promise<string | null>;
  setItem(key: string, value: string): Promise<void>;
}

export interface CacheRepository {
  get<T>(key: string): Promise<T | null>;
  set(key: string, value: unknown): Promise<void>;
}

/** AsyncStorage-backed JSON cache. Swallows storage errors: a cache must never crash the app. */
export function createCacheRepository(store: KeyValueStore): CacheRepository {
  return {
    async get<T>(key: string) {
      try {
        const raw = await store.getItem(`cache:${key}`);
        return raw == null ? null : (JSON.parse(raw) as T);
      } catch {
        return null;
      }
    },
    async set(key, value) {
      try {
        await store.setItem(`cache:${key}`, JSON.stringify(value));
      } catch {
        // ignore: cache is best-effort
      }
    },
  };
}

export type CachedResult<T> = { data: T | null; fromCache: boolean; error?: unknown };

export async function fetchWithCache<T>(
  cache: CacheRepository,
  key: string,
  load: () => Promise<T>,
): Promise<CachedResult<T>> {
  try {
    const data = await load();
    await cache.set(key, data);
    return { data, fromCache: false };
  } catch (error) {
    return { data: await cache.get<T>(key), fromCache: true, error };
  }
}
