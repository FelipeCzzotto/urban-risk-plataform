import { StyleSheet, View } from 'react-native';
import { EventList } from '../components/EventList';
import { useAlerts } from '../hooks';
import { colors } from '../theme';

export function AlertsScreen() {
  const { data, loading, refresh } = useAlerts();
  return (
    <View style={styles.screen}>
      <EventList testID="alerts-list" events={data} empty="Nenhum alerta ativo." refreshing={loading} onRefresh={refresh} />
    </View>
  );
}

const styles = StyleSheet.create({ screen: { flex: 1, backgroundColor: colors.bg } });
