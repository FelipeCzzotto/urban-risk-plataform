import MapView, { Marker } from 'react-native-maps';
import { StyleSheet } from 'react-native';
import { priorityColors, priorityLabel } from '../../domain/priority';
import type { RiskEvent } from '../../domain/types';

// São Paulo centre (simulator generates events around it).
const REGION = { latitude: -23.55, longitude: -46.63, latitudeDelta: 0.3, longitudeDelta: 0.3 };

export function EventsMap({ events }: { events: RiskEvent[] }) {
  return (
    <MapView testID="events-map" style={styles.map} initialRegion={REGION} accessibilityLabel="Mapa de eventos">
      {events.map((e) => (
        <Marker
          key={e.id}
          coordinate={{ latitude: e.latitude, longitude: e.longitude }}
          title={e.title}
          description={`Prioridade ${priorityLabel(e.priorityLevel)} · ${e.neighborhood}`}
          pinColor={priorityColors(e.priorityLevel).bg}
        />
      ))}
    </MapView>
  );
}

const styles = StyleSheet.create({ map: { flex: 1 } });
