import { test, expect } from '@playwright/test';

test('capture screenshots of the app', async ({ page }) => {
    await page.goto('http://localhost:3000');
    await page.screenshot({ path: 'initial_load.png' });

    const apiKeyInput = page.locator('input[name="key"]');
    if (await apiKeyInput.isVisible()) {
        await apiKeyInput.fill('invalid-key-for-testing');
        await page.getByRole('button', { name: /Enter App/i }).click();

        // Wait a bit for potential error message
        await page.waitForTimeout(2000);
        await page.screenshot({ path: 'after_login_attempt.png' });
    } else {
        console.log('Login skipped or input not found');
    }
});
