import { useMemo, useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { filterEvents } from '../../domain/events';
import type { EventFilters } from '../../domain/types';
import { EventList } from '../components/EventList';
import { EventsMap } from '../components/EventsMap';
import { FilterChips } from '../components/FilterChips';
import { useEvents } from '../hooks';
import { colors, MIN_TOUCH } from '../theme';

export function EventsScreen() {
  const { data, loading, refresh } = useEvents();
  const [filters, setFilters] = useState<EventFilters>({});
  const [showMap, setShowMap] = useState(false);
  const visible = useMemo(() => filterEvents(data, filters), [data, filters]);

  return (
    <View style={styles.screen}>
      <FilterChips value={filters} onChange={setFilters} />
      <View style={styles.toolbar}>
        <Text style={styles.count}>{visible.length} evento(s)</Text>
        <Pressable
          testID="toggle-map"
          accessibilityRole="button"
          accessibilityLabel={showMap ? 'Mostrar lista' : 'Mostrar mapa'}
          onPress={() => setShowMap((v) => !v)}
          style={styles.toggle}
        >
          <Text style={styles.toggleText}>{showMap ? 'Lista' : 'Mapa'}</Text>
        </Pressable>
      </View>
      {showMap ? (
        <EventsMap events={visible} />
      ) : (
        <EventList testID="events-list" events={visible} empty="Nenhum evento encontrado." refreshing={loading} onRefresh={refresh} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.bg },
  toolbar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16 },
  count: { color: colors.textMuted, fontSize: 14 },
  toggle: {
    minHeight: MIN_TOUCH,
    minWidth: MIN_TOUCH,
    paddingHorizontal: 16,
    borderRadius: 8,
    backgroundColor: colors.primary,
    justifyContent: 'center',
  },
  toggleText: { color: colors.onPrimary, fontSize: 15, fontWeight: '600' },
});
