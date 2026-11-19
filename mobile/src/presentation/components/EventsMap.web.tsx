import { StyleSheet, Text, View } from 'react-native';
import type { RiskEvent } from '../../domain/types';
import { EventList } from './EventList';
import { colors } from '../theme';

// react-native-maps has no web support: fall back to the list.
export function EventsMap({ events }: { events: RiskEvent[] }) {
  return (
    <View testID="events-map" style={styles.wrap}>
      <Text style={styles.note}>Mapa indisponível na web. Exibindo lista.</Text>
      <EventList events={events} empty="Nenhum evento encontrado." />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { flex: 1 },
  note: { color: colors.textMuted, fontSize: 14, paddingHorizontal: 16, paddingTop: 8 },
});
