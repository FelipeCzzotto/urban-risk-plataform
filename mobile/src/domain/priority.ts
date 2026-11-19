import type { EventSource, EventType, PriorityLevel } from './types';

const LEVEL_LABEL: Record<PriorityLevel, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
  CRITICAL: 'Crítica',
};

// White text on each bg is >= 4.5:1 (WCAG AA), asserted in tests.
const LEVEL_COLORS: Record<PriorityLevel, { bg: string; fg: string }> = {
  LOW: { bg: '#166534', fg: '#FFFFFF' },
  MEDIUM: { bg: '#854D0E', fg: '#FFFFFF' },
  HIGH: { bg: '#C2410C', fg: '#FFFFFF' },
  CRITICAL: { bg: '#B91C1C', fg: '#FFFFFF' },
};

const SOURCE_LABEL: Record<EventSource, string> = {
  SENSOR: 'Sensor',
  PUBLIC_DATA: 'Dados públicos',
  COMMUNITY: 'Comunidade',
};

const TYPE_LABEL: Record<EventType, string> = {
  FLOOD: 'Alagamento',
  HEAT_ISLAND: 'Ilha de calor',
  SERVICE_OUTAGE: 'Falha de serviço',
};

export const priorityLabel = (l: PriorityLevel) => LEVEL_LABEL[l];
export const priorityColors = (l: PriorityLevel) => LEVEL_COLORS[l];
export const sourceLabel = (s: EventSource) => SOURCE_LABEL[s];
export const typeLabel = (t: EventType) => TYPE_LABEL[t];

export function formatConfidence(c: number): string {
  const v = Number.isFinite(c) ? Math.min(1, Math.max(0, c)) : 0;
  return `${Math.round(v * 100)}%`;
}
