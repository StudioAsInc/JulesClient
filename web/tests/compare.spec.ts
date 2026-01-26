import { test, expect } from '@playwright/test';

test('capture external and local comparison', async ({ page }) => {
    // Capture Localhost again for reference
    await page.goto('http://localhost:3000');
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'local_current_state.png' });

    // Try to capture the official one (will likely be a login page, but useful for style reference)
    try {
        await page.goto('https://jules.google.com/session', { timeout: 10000 });
        await page.waitForTimeout(2000);
        await page.screenshot({ path: 'official_jules_snapshot.png' });
    } catch (e) {
        console.log('Could not load official page within timeout or error occurred');
    }
});
