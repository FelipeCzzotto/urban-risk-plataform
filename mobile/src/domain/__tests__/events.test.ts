import { applyAlertUpdate, applyEventUpdate, filterEvents, sortEvents } from '../events';
import { makeEvent } from '../../test-utils/fixtures';

describe('events', () => {
  it('sorts by priorityScore desc then occurredAt desc', () => {
    const a = makeEvent({ id: 'a', priorityScore: 50, occurredAt: '2026-01-01T10:00:00Z' });
    const b = makeEvent({ id: 'b', priorityScore: 90 });
    const c = makeEvent({ id: 'c', priorityScore: 50, occurredAt: '2026-01-01T12:00:00Z' });
    expect(sortEvents([a, b, c]).map((e) => e.id)).toEqual(['b', 'c', 'a']);
  });

  it('upserts by id and keeps order', () => {
    const list = [makeEvent({ id: 'a', priorityScore: 80 }), makeEvent({ id: 'b', priorityScore: 40 })];
    const updated = applyEventUpdate(list, makeEvent({ id: 'b', priorityScore: 95 }));
    expect(updated.map((e) => e.id)).toEqual(['b', 'a']);
    expect(updated).toHaveLength(2);
    const added = applyEventUpdate(list, makeEvent({ id: 'z', priorityScore: 60 }));
    expect(added.map((e) => e.id)).toEqual(['a', 'z', 'b']);
  });

  it('removes events resolved via realtime', () => {
    const list = [makeEvent({ id: 'a' }), makeEvent({ id: 'b' })];
    expect(applyEventUpdate(list, makeEvent({ id: 'a', status: 'RESOLVED' })).map((e) => e.id)).toEqual(['b']);
  });

  it('does not mutate input', () => {
    const list = [makeEvent({ id: 'a' })];
    applyEventUpdate(list, makeEvent({ id: 'b' }));
    expect(list).toHaveLength(1);
  });

  it('alert list keeps only ACTIVE HIGH/CRITICAL', () => {
    const list = [makeEvent({ id: 'a', priorityLevel: 'HIGH' })];
    expect(applyAlertUpdate(list, makeEvent({ id: 'a', priorityLevel: 'MEDIUM' }))).toEqual([]);
    expect(applyAlertUpdate(list, makeEvent({ id: 'a', status: 'RESOLVED' }))).toEqual([]);
    expect(applyAlertUpdate(list, makeEvent({ id: 'b', priorityLevel: 'LOW' })).map((e) => e.id)).toEqual(['a']);
    expect(applyAlertUpdate([], makeEvent({ id: 'c', priorityLevel: 'CRITICAL' })).map((e) => e.id)).toEqual(['c']);
  });

  it('filters by type and level', () => {
    const list = [
      makeEvent({ id: '1', type: 'FLOOD', priorityLevel: 'HIGH' }),
      makeEvent({ id: '2', type: 'HEAT_ISLAND', priorityLevel: 'HIGH' }),
      makeEvent({ id: '3', type: 'FLOOD', priorityLevel: 'LOW' }),
    ];
    expect(filterEvents(list, {}).map((e) => e.id)).toEqual(['1', '2', '3']);
    expect(filterEvents(list, { type: 'FLOOD' }).map((e) => e.id)).toEqual(['1', '3']);
    expect(filterEvents(list, { type: 'FLOOD', level: 'HIGH' }).map((e) => e.id)).toEqual(['1']);
  });
});
