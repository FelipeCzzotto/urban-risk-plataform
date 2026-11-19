import * as Notifications from 'expo-notifications';
import { priorityLabel } from '../domain/priority';
import type { RiskEvent } from '../domain/types';

let ready: Promise<boolean> | null = null;

function ensurePermission(): Promise<boolean> {
  ready ??= (async () => {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({
        shouldShowBanner: true,
        shouldShowList: true,
        shouldPlaySound: true,
        shouldSetBadge: false,
      }),
    });
    // ponytail: uses the default Android channel; add a dedicated high-importance channel if alerts must bypass DND.
    const { granted } = await Notifications.requestPermissionsAsync();
    return granted;
  })().catch(() => false);
  return ready;
}

/** Native: immediate local notification for /topic/alerts. Never throws. */
export function notifyAlert(ev: RiskEvent): void {
  ensurePermission()
    .then((ok) =>
      ok
        ? Notifications.scheduleNotificationAsync({
            content: {
              title: `Alerta ${priorityLabel(ev.priorityLevel)}: ${ev.title}`,
              body: ev.neighborhood,
              data: { id: ev.id },
            },
            trigger: null,
          })
        : undefined,
    )
    .catch(() => undefined);
}
