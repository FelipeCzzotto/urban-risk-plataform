export type EventType = 'FLOOD' | 'HEAT_ISLAND' | 'SERVICE_OUTAGE';
export type EventSource = 'SENSOR' | 'PUBLIC_DATA' | 'COMMUNITY';
export type PriorityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type EventStatus = 'ACTIVE' | 'RESOLVED';

export const EVENT_TYPES: EventType[] = ['FLOOD', 'HEAT_ISLAND', 'SERVICE_OUTAGE'];
export const PRIORITY_LEVELS: PriorityLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

/** EventDTO per docs/CONTRACT.md (reporterHash never exposed). */
export interface RiskEvent {
  id: string;
  type: EventType;
  title: string;
  description: string;
  latitude: number;
  longitude: number;
  neighborhood: string;
  severity: number;
  source: EventSource;
  sourceRef: string;
  confidence: number;
  priorityScore: number;
  priorityLevel: PriorityLevel;
  status: EventStatus;
  occurredAt: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DashboardSummary {
  totalActive: number;
  byType: Partial<Record<EventType, number>>;
  byLevel: Partial<Record<PriorityLevel, number>>;
  lastUpdatedAt: string | null;
}

export interface EventFilters {
  type?: EventType;
  level?: PriorityLevel;
}
