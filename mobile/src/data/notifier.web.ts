import type { RiskEvent } from '../domain/types';

/** Web: no local notifications; the in-app toast (AppDataProvider) covers alerts. */
export function notifyAlert(_ev: RiskEvent): void {}
