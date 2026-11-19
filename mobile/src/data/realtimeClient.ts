import { Client, ReconnectionTimeMode, type IMessage, type StompConfig } from '@stomp/stompjs';
import type { RiskEvent } from '../domain/types';

export const TOPIC_EVENTS = '/topic/events';
export const TOPIC_ALERTS = '/topic/alerts';

interface StompLike {
  activate(): void;
  deactivate(): Promise<void>;
  subscribe(topic: string, cb: (m: IMessage) => void): { unsubscribe(): void };
}

export interface RealtimeHandlers {
  onStatus(connected: boolean): void;
  onEvent(ev: RiskEvent): void;
  onAlert(ev: RiskEvent): void;
}

export interface RealtimeOptions {
  url: string;
  handlers: RealtimeHandlers;
  createClient?: (conf: StompConfig) => StompLike;
}

export type RealtimeClient = ReturnType<typeof createRealtimeClient>;

function parseEvent(body: string): RiskEvent | null {
  try {
    const v = JSON.parse(body) as Partial<RiskEvent> | null;
    return v && typeof v.id === 'string' && typeof v.priorityLevel === 'string' ? (v as RiskEvent) : null;
  } catch {
    return null;
  }
}

export function createRealtimeClient({ url, handlers, createClient = (c) => new Client(c) }: RealtimeOptions) {
  let client: StompLike | null = null;
  const listen = (cb: (ev: RiskEvent) => void) => (m: IMessage) => {
    const ev = parseEvent(m.body);
    if (ev) cb(ev);
  };
  const down = () => handlers.onStatus(false);

  return {
    start() {
      if (client) return;
      const c = createClient({
        brokerURL: url,
        reconnectDelay: 1000,
        maxReconnectDelay: 30000,
        reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        // Subscriptions die with the socket; resubscribe on every (re)connect.
        onConnect: () => {
          c.subscribe(TOPIC_EVENTS, listen(handlers.onEvent));
          c.subscribe(TOPIC_ALERTS, listen(handlers.onAlert));
          handlers.onStatus(true);
        },
        onWebSocketClose: down,
        onStompError: down,
      });
      client = c;
      c.activate();
    },
    async stop() {
      const c = client;
      client = null;
      await c?.deactivate();
    },
  };
}
