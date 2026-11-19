import type { EventFilters, RiskEvent } from './types';

export function sortEvents(list: RiskEvent[]): RiskEvent[] {
  return [...list].sort(
    (a, b) => b.priorityScore - a.priorityScore || Date.parse(b.occurredAt) - Date.parse(a.occurredAt),
  );
}

/** Realtime upsert by id; RESOLVED events are removed (contract v1.1). */
export function applyEventUpdate(list: RiskEvent[], ev: RiskEvent): RiskEvent[] {
  const rest = list.filter((e) => e.id !== ev.id);
  return ev.status === 'RESOLVED' ? rest : sortEvents([...rest, ev]);
}

/** Alerts = ACTIVE events with level HIGH|CRITICAL; drop ones that downgrade or resolve. */
export function applyAlertUpdate(list: RiskEvent[], ev: RiskEvent): RiskEvent[] {
  const isAlert = ev.status === 'ACTIVE' && (ev.priorityLevel === 'HIGH' || ev.priorityLevel === 'CRITICAL');
  return isAlert ? applyEventUpdate(list, ev) : list.filter((e) => e.id !== ev.id);
}

export function filterEvents(list: RiskEvent[], f: EventFilters): RiskEvent[] {
  return list.filter((e) => (!f.type || e.type === f.type) && (!f.level || e.priorityLevel === f.level));
}
