import { StyleSheet, Text, View } from 'react-native';
import { priorityLabel, typeLabel } from '../../domain/priority';
import { EVENT_TYPES, PRIORITY_LEVELS } from '../../domain/types';
import { EventList } from '../components/EventList';
import { useAlerts, useSummary } from '../hooks';
import { colors } from '../theme';

const TOP_ALERTS = 5;

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <View accessible accessibilityLabel={`${label}: ${value}`} style={styles.stat}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}

function Summary() {
  const { data } = useSummary();
  if (!data) return <Text style={styles.muted}>Resumo indisponível.</Text>;
  return (
    <View style={styles.summary}>
      <View testID="summary-total" accessible accessibilityLabel={`${data.totalActive} eventos ativos`} style={styles.total}>
        <Text style={styles.totalValue}>{data.totalActive}</Text>
        <Text style={styles.totalLabel}>eventos ativos</Text>
      </View>
      <View style={styles.grid}>
        {EVENT_TYPES.map((t) => (
          <Stat key={t} label={typeLabel(t)} value={data.byType[t] ?? 0} />
        ))}
      </View>
      <View style={styles.grid}>
        {PRIORITY_LEVELS.map((l) => (
          <Stat key={l} label={priorityLabel(l)} value={data.byLevel[l] ?? 0} />
        ))}
      </View>
      <Text accessibilityRole="header" style={styles.h2}>
        Principais alertas
      </Text>
    </View>
  );
}

export function DashboardScreen() {
  const { data, loading, refresh } = useAlerts();
  return (
    <View style={styles.screen}>
      <EventList
        testID="dashboard-list"
        header={<Summary />}
        events={data.slice(0, TOP_ALERTS)}
        empty="Nenhum alerta ativo."
        refreshing={loading}
        onRefresh={refresh}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  summary: { gap: 12, paddingBottom: 12 },
  total: { backgroundColor: colors.primary, borderRadius: 12, padding: 16 },
  totalValue: { color: colors.onPrimary, fontSize: 36, fontWeight: '800' },
  totalLabel: { color: colors.onPrimary, fontSize: 14 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  stat: {
    flexGrow: 1,
    minWidth: 96,
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: 10,
    padding: 10,
  },
  statValue: { color: colors.text, fontSize: 22, fontWeight: '700' },
  statLabel: { color: colors.textMuted, fontSize: 14 },
  h2: { color: colors.text, fontSize: 18, fontWeight: '700', marginTop: 8 },
  muted: { color: colors.textMuted, fontSize: 16, paddingBottom: 12 },
});
