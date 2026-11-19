import { FlatList, StyleSheet, Text, View, type ListRenderItem } from 'react-native';
import type { ReactElement } from 'react';
import type { RiskEvent } from '../../domain/types';
import { colors } from '../theme';
import { EventCard } from './EventCard';

const keyOf = (e: RiskEvent) => e.id;
const renderItem: ListRenderItem<RiskEvent> = ({ item }) => <EventCard event={item} />;
const Separator = () => <View style={styles.sep} />;

export function EventList({
  events,
  empty,
  header,
  refreshing,
  onRefresh,
  testID,
}: {
  events: RiskEvent[];
  empty: string;
  header?: ReactElement;
  refreshing?: boolean;
  onRefresh?: () => void;
  testID?: string;
}) {
  return (
    <FlatList
      testID={testID}
      data={events}
      keyExtractor={keyOf}
      renderItem={renderItem}
      ItemSeparatorComponent={Separator}
      ListHeaderComponent={header}
      ListEmptyComponent={<Text style={styles.empty}>{empty}</Text>}
      contentContainerStyle={styles.content}
      refreshing={refreshing}
      onRefresh={onRefresh}
    />
  );
}

const styles = StyleSheet.create({
  content: { padding: 16, paddingBottom: 32 },
  sep: { height: 12 },
  empty: { color: colors.textMuted, fontSize: 16, textAlign: 'center', paddingVertical: 32 },
});
