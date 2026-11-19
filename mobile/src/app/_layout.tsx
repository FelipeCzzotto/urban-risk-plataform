import AsyncStorage from '@react-native-async-storage/async-storage';
import { Tabs } from 'expo-router/js-tabs';
import { StatusBar } from 'expo-status-bar';
import { Platform, StyleSheet, Text, View, type ColorValue } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { createApiClient } from '../data/apiClient';
import { createCacheRepository } from '../data/cacheRepository';
import { API_URL, WS_URL } from '../data/config';
import { notifyAlert } from '../data/notifier';
import { createRealtimeClient } from '../data/realtimeClient';
import { AppDataProvider, type AppDeps } from '../presentation/AppDataProvider';
import { ConnectionBanner } from '../presentation/components/ConnectionBanner';
import { Toast } from '../presentation/components/Toast';
import { useConnection, useToast } from '../presentation/hooks';
import { colors } from '../presentation/theme';

const deps: AppDeps = {
  api: createApiClient({ baseUrl: API_URL }),
  cache: createCacheRepository(AsyncStorage),
  createRealtime: (handlers) => createRealtimeClient({ url: WS_URL, handlers }),
  notify: notifyAlert,
};

const icon = (glyph: string) =>
  function TabIcon({ color }: { color: ColorValue }) {
    return (
      <Text style={{ color, fontSize: 20 }} accessibilityElementsHidden importantForAccessibility="no">
        {glyph}
      </Text>
    );
  };

function Shell() {
  const connection = useConnection();
  const toast = useToast();
  return (
    <SafeAreaView style={styles.root} edges={['top']}>
      <ConnectionBanner state={connection} />
      <View style={styles.root}>
        <Tabs
          screenOptions={{
            tabBarActiveTintColor: colors.primary,
            tabBarInactiveTintColor: colors.textMuted,
            tabBarLabelStyle: styles.tabLabel,
            tabBarItemStyle: styles.tabItem,
            headerTitleStyle: styles.headerTitle,
          }}
        >
          <Tabs.Screen
            name="index"
            options={{ title: 'Painel', tabBarButtonTestID: 'tab-painel', tabBarAccessibilityLabel: 'Painel', tabBarIcon: icon('▦') }}
          />
          <Tabs.Screen
            name="eventos"
            options={{ title: 'Eventos', tabBarButtonTestID: 'tab-eventos', tabBarAccessibilityLabel: 'Eventos', tabBarIcon: icon('☰') }}
          />
          <Tabs.Screen
            name="alertas"
            options={{ title: 'Alertas', tabBarButtonTestID: 'tab-alertas', tabBarAccessibilityLabel: 'Alertas', tabBarIcon: icon('⚠') }}
          />
        </Tabs>
      </View>
      {/* Native gets a local notification (notifier.ts); web gets the in-app toast. */}
      {Platform.OS === 'web' ? <Toast message={toast.message} onDismiss={toast.dismiss} /> : null}
      <StatusBar style="dark" />
    </SafeAreaView>
  );
}

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <AppDataProvider deps={deps}>
        <Shell />
      </AppDataProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  tabLabel: { fontSize: 13 },
  tabItem: { minHeight: 44 },
  headerTitle: { color: colors.text },
});
