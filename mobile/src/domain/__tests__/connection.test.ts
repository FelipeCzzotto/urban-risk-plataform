import { connectionReducer, initialConnection, OFFLINE_AFTER_FAILURES } from '../connection';

describe('connectionReducer', () => {
  it('starts reconnecting (connecting) and not flagged as reconnected', () => {
    expect(initialConnection).toEqual({
      status: 'reconnecting',
      everConnected: false,
      reconnected: false,
      failures: 0,
    });
  });

  it('first connect is not a "reconnect"', () => {
    const s = connectionReducer(initialConnection, { type: 'CONNECTED' });
    expect(s.status).toBe('connected');
    expect(s.reconnected).toBe(false);
    expect(s.everConnected).toBe(true);
  });

  it('drop after connect goes to reconnecting, then offline after N failures', () => {
    let s = connectionReducer(initialConnection, { type: 'CONNECTED' });
    s = connectionReducer(s, { type: 'DISCONNECTED' });
    expect(s.status).toBe('reconnecting');
    for (let i = 1; i < OFFLINE_AFTER_FAILURES; i++) s = connectionReducer(s, { type: 'DISCONNECTED' });
    expect(s.status).toBe('offline');
  });

  it('connect after a drop flags reconnected, ack clears it', () => {
    let s = connectionReducer(initialConnection, { type: 'CONNECTED' });
    s = connectionReducer(s, { type: 'DISCONNECTED' });
    s = connectionReducer(s, { type: 'CONNECTED' });
    expect(s).toMatchObject({ status: 'connected', reconnected: true, failures: 0 });
    s = connectionReducer(s, { type: 'ACK_RECONNECTED' });
    expect(s.reconnected).toBe(false);
  });

  it('network error goes offline unless connected', () => {
    expect(connectionReducer(initialConnection, { type: 'NETWORK_ERROR' }).status).toBe('offline');
    const connected = connectionReducer(initialConnection, { type: 'CONNECTED' });
    expect(connectionReducer(connected, { type: 'NETWORK_ERROR' }).status).toBe('connected');
  });

  it('a drop clears a pending reconnected flag', () => {
    let s = connectionReducer(initialConnection, { type: 'CONNECTED' });
    s = connectionReducer(s, { type: 'DISCONNECTED' });
    s = connectionReducer(s, { type: 'CONNECTED' });
    s = connectionReducer(s, { type: 'DISCONNECTED' });
    expect(s.reconnected).toBe(false);
  });
});
