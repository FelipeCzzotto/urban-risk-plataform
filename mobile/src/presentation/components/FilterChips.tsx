import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { priorityLabel, typeLabel } from '../../domain/priority';
import { EVENT_TYPES, PRIORITY_LEVELS, type EventFilters } from '../../domain/types';
import { colors, MIN_TOUCH } from '../theme';

function Chip({ id, label, a11y, selected, onPress }: { id: string; label: string; a11y: string; selected: boolean; onPress: () => void }) {
  return (
    <Pressable
      testID={id}
      accessibilityRole="button"
      accessibilityLabel={a11y}
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[styles.chip, selected && styles.chipOn]}
    >
      <Text style={[styles.chipText, selected && styles.chipTextOn]}>{label}</Text>
    </Pressable>
  );
}

export function FilterChips({ value, onChange }: { value: EventFilters; onChange: (f: EventFilters) => void }) {
  return (
    <View style={styles.wrap}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
        <Chip id="filter-type-all" label="Todos" a11y="Filtrar tipo: todos" selected={!value.type} onPress={() => onChange({ ...value, type: undefined })} />
        {EVENT_TYPES.map((t) => (
          <Chip key={t} id={`filter-type-${t}`} label={typeLabel(t)} a11y={`Filtrar tipo: ${typeLabel(t)}`} selected={value.type === t} onPress={() => onChange({ ...value, type: t })} />
        ))}
      </ScrollView>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row}>
        <Chip id="filter-level-all" label="Todas" a11y="Filtrar prioridade: todas" selected={!value.level} onPress={() => onChange({ ...value, level: undefined })} />
        {PRIORITY_LEVELS.map((l) => (
          <Chip key={l} id={`filter-level-${l}`} label={priorityLabel(l)} a11y={`Filtrar prioridade: ${priorityLabel(l)}`} selected={value.level === l} onPress={() => onChange({ ...value, level: l })} />
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 8, paddingVertical: 8 },
  row: { gap: 8, paddingHorizontal: 16 },
  chip: {
    minHeight: MIN_TOUCH,
    minWidth: MIN_TOUCH,
    paddingHorizontal: 14,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
    justifyContent: 'center',
    alignItems: 'center',
  },
  chipOn: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { color: colors.text, fontSize: 15 },
  chipTextOn: { color: colors.onPrimary, fontWeight: '600' },
});
