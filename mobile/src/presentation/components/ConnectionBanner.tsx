import { StyleSheet, Text, View } from 'react-native';
import type { ConnectionState } from '../../domain/connection';
import { colors } from '../theme';

function bannerFor(s: ConnectionState): { text: string; bg: string } | null {
  if (s.status === 'offline') return { text: 'Sem conexão. Exibindo dados salvos.', bg: colors.offline };
  if (s.status === 'reconnecting' && s.everConnected) return { text: 'Reconectando…', bg: colors.reconnecting };
  if (s.status === 'connected' && s.reconnected) return { text: 'Reconectado', bg: colors.reconnected };
  return null;
}

export function ConnectionBanner({ state }: { state: ConnectionState }) {
  const banner = bannerFor(state);
  if (!banner) return null;
  return (
    <View
      testID="connection-banner"
      accessibilityRole="alert"
      accessibilityLiveRegion={state.status === 'offline' ? 'assertive' : 'polite'}
      style={[styles.banner, { backgroundColor: banner.bg }]}
    >
      <Text style={styles.text}>{banner.text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: { paddingHorizontal: 16, paddingVertical: 10, minHeight: 44, justifyContent: 'center' },
  text: { color: colors.onStatus, fontSize: 15, fontWeight: '600' },
});
