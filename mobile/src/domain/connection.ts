export type ConnectionStatus = 'connected' | 'reconnecting' | 'offline';

export interface ConnectionState {
  status: ConnectionStatus;
  /** True once any connection succeeded; distinguishes first connect from reconnect. */
  everConnected: boolean;
  /** True right after recovering from a drop; UI shows "Reconectado" then dispatches ACK. */
  reconnected: boolean;
  failures: number;
}

export type ConnectionAction =
  | { type: 'CONNECTED' }
  | { type: 'DISCONNECTED' }
  | { type: 'NETWORK_ERROR' }
  | { type: 'ACK_RECONNECTED' };

/** Consecutive failed attempts before we stop saying "reconnecting" and say "offline". */
export const OFFLINE_AFTER_FAILURES = 3;

export const initialConnection: ConnectionState = {
  status: 'reconnecting',
  everConnected: false,
  reconnected: false,
  failures: 0,
};

export function connectionReducer(s: ConnectionState, a: ConnectionAction): ConnectionState {
  switch (a.type) {
    case 'CONNECTED':
      return {
        status: 'connected',
        everConnected: true,
        reconnected: s.status === 'offline' || (s.everConnected && s.status === 'reconnecting'),
        failures: 0,
      };
    case 'DISCONNECTED': {
      const failures = s.failures + 1;
      return {
        ...s,
        reconnected: false,
        failures,
        status: s.status === 'offline' || failures >= OFFLINE_AFTER_FAILURES ? 'offline' : 'reconnecting',
      };
    }
    case 'NETWORK_ERROR':
      return s.status === 'connected' ? s : { ...s, status: 'offline', reconnected: false };
    case 'ACK_RECONNECTED':
      return { ...s, reconnected: false };
  }
}
