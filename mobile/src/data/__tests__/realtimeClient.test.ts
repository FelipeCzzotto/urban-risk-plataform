import { ReconnectionTimeMode, type IFrame, type IMessage, type StompConfig } from '@stomp/stompjs';
import { createRealtimeClient, TOPIC_ALERTS, TOPIC_EVENTS } from '../realtimeClient';
import { makeEvent } from '../../test-utils/fixtures';

function setup() {
  let conf!: StompConfig;
  const subs: Record<string, (m: IMessage) => void> = {};
  const fake = {
    activate: jest.fn(),
    deactivate: jest.fn(() => Promise.resolve()),
    subscribe: jest.fn((topic: string, cb: (m: IMessage) => void) => {
      subs[topic] = cb;
      return { id: topic, unsubscribe: jest.fn() };
    }),
  };
  const handlers = { onStatus: jest.fn(), onEvent: jest.fn(), onAlert: jest.fn() };
  const client = createRealtimeClient({
    url: 'ws://h/ws',
    handlers,
    createClient: (c) => {
      conf = c;
      return fake;
    },
  });
  const msg = (body: string) => ({ body }) as IMessage;
  return { client, fake, handlers, subs, msg, conf: () => conf };
}

describe('realtimeClient', () => {
  it('configures exponential backoff and heartbeats', () => {
    const { client, fake, conf } = setup();
    client.start();
    expect(fake.activate).toHaveBeenCalled();
    expect(conf()).toMatchObject({
      brokerURL: 'ws://h/ws',
      reconnectTimeMode: ReconnectionTimeMode.EXPONENTIAL,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });
    expect(conf().reconnectDelay).toBeGreaterThan(0);
    expect(conf().maxReconnectDelay).toBeGreaterThan(conf().reconnectDelay!);
  });

  it('subscribes on connect and resubscribes after reconnect', () => {
    const { client, fake, handlers, conf } = setup();
    client.start();
    conf().onConnect!({} as IFrame);
    expect(handlers.onStatus).toHaveBeenLastCalledWith(true);
    expect(fake.subscribe.mock.calls.map((c) => c[0])).toEqual([TOPIC_EVENTS, TOPIC_ALERTS]);

    conf().onWebSocketClose!({} as CloseEvent);
    expect(handlers.onStatus).toHaveBeenLastCalledWith(false);

    conf().onConnect!({} as IFrame);
    expect(fake.subscribe).toHaveBeenCalledTimes(4);
    expect(handlers.onStatus).toHaveBeenLastCalledWith(true);
  });

  it('dispatches parsed events/alerts and ignores malformed frames', () => {
    const { client, handlers, subs, msg, conf } = setup();
    client.start();
    conf().onConnect!({} as IFrame);
    const ev = makeEvent();
    subs[TOPIC_EVENTS]!(msg(JSON.stringify(ev)));
    subs[TOPIC_ALERTS]!(msg(JSON.stringify(ev)));
    expect(handlers.onEvent).toHaveBeenCalledWith(ev);
    expect(handlers.onAlert).toHaveBeenCalledWith(ev);
    expect(() => subs[TOPIC_EVENTS]!(msg('garbage'))).not.toThrow();
    expect(() => subs[TOPIC_EVENTS]!(msg('{"id":1}'))).not.toThrow();
    expect(handlers.onEvent).toHaveBeenCalledTimes(1);
  });

  it('treats STOMP errors as disconnects and stop() deactivates', async () => {
    const { client, fake, handlers, conf } = setup();
    client.start();
    conf().onStompError!({} as IFrame);
    expect(handlers.onStatus).toHaveBeenLastCalledWith(false);
    await client.stop();
    expect(fake.deactivate).toHaveBeenCalled();
  });
});
