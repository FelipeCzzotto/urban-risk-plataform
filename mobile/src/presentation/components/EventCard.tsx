import { memo } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { formatConfidence, priorityColors, priorityLabel, sourceLabel, typeLabel } from '../../domain/priority';
import type { RiskEvent } from '../../domain/types';
import { colors } from '../theme';

const dateFmt = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export const EventCard = memo(function EventCard({ event }: { event: RiskEvent }) {
  const level = priorityLabel(event.priorityLevel);
  const { bg, fg } = priorityColors(event.priorityLevel);
  const source = sourceLabel(event.source);
  const sourceText = event.source !== 'COMMUNITY' && event.sourceRef ? `${source} · ${event.sourceRef}` : source;
  const confidence = formatConfidence(event.confidence);
  const when = dateFmt.format(new Date(event.occurredAt));

  return (
    <View
      testID={`event-card-${event.id}`}
      accessible
      accessibilityLabel={`${event.title}. ${typeLabel(event.type)} em ${event.neighborhood}. Nível de prioridade ${level}, fonte ${source}, confiança ${confidence}. ${when}`}
      style={styles.card}
    >
      <View style={styles.header}>
        <Text style={styles.title}>{event.title}</Text>
        <View style={[styles.badge, { backgroundColor: bg }]}>
          <Text style={[styles.badgeText, { color: fg }]}>{level}</Text>
        </View>
      </View>
      <Text style={styles.meta}>
        {typeLabel(event.type)} · {event.neighborhood}
      </Text>
      <View style={styles.row}>
        <Text style={styles.meta}>{sourceText}</Text>
        <Text style={styles.meta}>Confiança {confidence}</Text>
      </View>
      <Text style={styles.meta}>{when}</Text>
    </View>
  );
});

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    gap: 6,
  },
  header: { flexDirection: 'row', alignItems: 'flex-start', gap: 8 },
  title: { flex: 1, color: colors.text, fontSize: 16, fontWeight: '700' },
  badge: { borderRadius: 999, paddingHorizontal: 10, paddingVertical: 4 },
  badgeText: { fontSize: 13, fontWeight: '700' },
  row: { flexDirection: 'row', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 },
  meta: { color: colors.textMuted, fontSize: 14 },
});
