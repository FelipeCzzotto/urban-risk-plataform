import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, MIN_TOUCH } from '../theme';

export function Toast({ message, onDismiss }: { message: string | null; onDismiss: () => void }) {
  if (!message) return null;
  return (
    <View testID="alert-toast" accessibilityRole="alert" accessibilityLiveRegion="polite" style={styles.toast}>
      <Text style={styles.text}>{message}</Text>
      <Pressable
        testID="alert-toast-close"
        accessibilityRole="button"
        accessibilityLabel="Fechar aviso"
        onPress={onDismiss}
        hitSlop={8}
        style={styles.close}
      >
        <Text style={styles.text}>✕</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  toast: {
    position: 'absolute',
    left: 16,
    right: 16,
    bottom: 24,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: colors.text,
    borderRadius: 12,
    paddingLeft: 16,
  },
  text: { color: colors.surface, fontSize: 15, flexShrink: 1 },
  close: { minWidth: MIN_TOUCH, minHeight: MIN_TOUCH, justifyContent: 'center', alignItems: 'center', marginLeft: 'auto' },
});
