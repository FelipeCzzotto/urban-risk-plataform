import type { RiskEvent } from '../domain/types';

export function makeEvent(overrides: Partial<RiskEvent> = {}): RiskEvent {
  return {
    id: '00000000-0000-0000-0000-000000000001',
    type: 'FLOOD',
    title: 'Alagamento na Av. Paulista',
    description: 'Água acima do meio-fio',
    latitude: -23.561,
    longitude: -46.656,
    neighborhood: 'Bela Vista',
    severity: 4,
    source: 'SENSOR',
    sourceRef: 'sensor-42',
    confidence: 0.9,
    priorityScore: 82.5,
    priorityLevel: 'CRITICAL',
    status: 'ACTIVE',
    occurredAt: '2026-11-18T10:00:00Z',
    createdAt: '2026-11-18T10:00:05Z',
    ...overrides,
  };
}
