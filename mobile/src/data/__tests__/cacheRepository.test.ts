import AsyncStorage from '@react-native-async-storage/async-storage';
import { createCacheRepository, fetchWithCache } from '../cacheRepository';

describe('cacheRepository', () => {
  beforeEach(() => AsyncStorage.clear());

  it('round-trips JSON values', async () => {
    const cache = createCacheRepository(AsyncStorage);
    await cache.set('k', { a: 1 });
    await expect(cache.get('k')).resolves.toEqual({ a: 1 });
  });

  it('returns null for missing or corrupt entries', async () => {
    const cache = createCacheRepository(AsyncStorage);
    await expect(cache.get('missing')).resolves.toBeNull();
    await AsyncStorage.setItem('cache:bad', '{not json');
    await expect(cache.get('bad')).resolves.toBeNull();
  });

  it('never throws when storage fails', async () => {
    const broken = {
      getItem: jest.fn(() => Promise.reject(new Error('disk'))),
      setItem: jest.fn(() => Promise.reject(new Error('disk'))),
    };
    const cache = createCacheRepository(broken);
    await expect(cache.get('k')).resolves.toBeNull();
    await expect(cache.set('k', 1)).resolves.toBeUndefined();
  });
});

describe('fetchWithCache', () => {
  beforeEach(() => AsyncStorage.clear());

  it('stores fresh data on success', async () => {
    const cache = createCacheRepository(AsyncStorage);
    const res = await fetchWithCache(cache, 'events', () => Promise.resolve([1, 2]));
    expect(res).toEqual({ data: [1, 2], fromCache: false });
    await expect(cache.get('events')).resolves.toEqual([1, 2]);
  });

  it('falls back to cached data on failure', async () => {
    const cache = createCacheRepository(AsyncStorage);
    await cache.set('events', [9]);
    const error = new Error('offline');
    const res = await fetchWithCache(cache, 'events', () => Promise.reject(error));
    expect(res).toEqual({ data: [9], fromCache: true, error });
  });

  it('returns null data with error when nothing cached', async () => {
    const cache = createCacheRepository(AsyncStorage);
    const error = new Error('offline');
    const res = await fetchWithCache(cache, 'events', () => Promise.reject(error));
    expect(res).toEqual({ data: null, fromCache: true, error });
  });
});
