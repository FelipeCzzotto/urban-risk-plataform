import {
  formatConfidence,
  priorityColors,
  priorityLabel,
  sourceLabel,
  typeLabel,
} from '../priority';
import type { PriorityLevel } from '../types';

const LEVELS: PriorityLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

// WCAG relative luminance contrast, used to guarantee AA (>= 4.5) for badge text.
function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  }) as [number, number, number];
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x) as [number, number];
  return (hi + 0.05) / (lo + 0.05);
}

describe('priority mapping', () => {
  it('labels levels in pt-BR', () => {
    expect(LEVELS.map(priorityLabel)).toEqual(['Baixa', 'Média', 'Alta', 'Crítica']);
  });

  it.each(LEVELS)('colors for %s meet AA contrast', (level) => {
    const { bg, fg } = priorityColors(level);
    expect(contrast(bg, fg)).toBeGreaterThanOrEqual(4.5);
  });

  it('uses distinct colors per level', () => {
    expect(new Set(LEVELS.map((l) => priorityColors(l).bg)).size).toBe(4);
  });

  it('labels sources and types in pt-BR', () => {
    expect(sourceLabel('SENSOR')).toBe('Sensor');
    expect(sourceLabel('PUBLIC_DATA')).toBe('Dados públicos');
    expect(sourceLabel('COMMUNITY')).toBe('Comunidade');
    expect(typeLabel('FLOOD')).toBe('Alagamento');
    expect(typeLabel('HEAT_ISLAND')).toBe('Ilha de calor');
    expect(typeLabel('SERVICE_OUTAGE')).toBe('Falha de serviço');
  });

  it('formats confidence 0..1 as integer percent, clamped', () => {
    expect(formatConfidence(0.9)).toBe('90%');
    expect(formatConfidence(0.555)).toBe('56%');
    expect(formatConfidence(1.4)).toBe('100%');
    expect(formatConfidence(-1)).toBe('0%');
    expect(formatConfidence(Number.NaN)).toBe('0%');
  });
});
