import { expect, test } from '@playwright/test';

test('loads tabs and shows offline banner when API is unreachable', async ({ page }) => {
  await page.goto('/');

  for (const id of ['tab-painel', 'tab-eventos', 'tab-alertas']) {
    await expect(page.getByTestId(id)).toBeVisible();
  }

  const banner = page.getByTestId('connection-banner');
  await expect(banner).toBeVisible({ timeout: 15_000 });
  await expect(banner).toHaveAttribute('role', 'alert');
  await expect(banner).toContainText('Sem conexão');

  await page.getByTestId('tab-eventos').click();
  await expect(page.getByTestId('filter-type-all')).toBeVisible();
  await expect(banner).toBeVisible();
});
