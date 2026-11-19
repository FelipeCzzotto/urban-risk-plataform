import { resolveApiUrl, toWsUrl } from '../config';

describe('config', () => {
  it('defaults API url to localhost:8080 and strips trailing slash', () => {
    expect(resolveApiUrl(undefined)).toBe('http://localhost:8080');
    expect(resolveApiUrl('')).toBe('http://localhost:8080');
    expect(resolveApiUrl('https://api.example.com/')).toBe('https://api.example.com');
  });

  it('derives ws url from http(s)', () => {
    expect(toWsUrl('http://localhost:8080')).toBe('ws://localhost:8080/ws');
    expect(toWsUrl('https://api.example.com')).toBe('wss://api.example.com/ws');
  });
});
