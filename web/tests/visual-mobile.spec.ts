import { test, expect } from '@playwright/test';

const MOBILE_VIEWPORTS = [
    { width: 375, height: 667, name: 'iPhone-SE' },
    { width: 390, height: 844, name: 'iPhone-13' },
];

for (const viewport of MOBILE_VIEWPORTS) {
    test(`visual regression mobile - ${viewport.name}`, async ({ page }) => {
        await page.setViewportSize({ width: viewport.width, height: viewport.height });
        await page.goto('http://localhost:3000');

        // Handle Login if present
        const keyInput = page.locator('input[name="key"]');
        try {
            await keyInput.waitFor({ state: 'visible', timeout: 3000 });
            await keyInput.fill('test-key');
            await page.getByRole('button', { name: /Enter App/i }).click();
        } catch (e) {
            console.log('Login skipped or input not found');
        }

        // Wait for input area to be visible
        await expect(page.locator('textarea')).toBeVisible({ timeout: 10000 });

        // Screenshot initial state
        await page.screenshot({ path: `tests/visual/mobile-${viewport.name}-initial.png` });

        // Focus input and type to keep expanded
        await page.locator('textarea').fill('Test message');
        await page.waitForTimeout(500);
        await page.screenshot({ path: `tests/visual/mobile-${viewport.name}-expanded.png` });

        // Open Branch Menu
        await page.locator('.branch-menu-trigger button').first().click();
        await page.waitForTimeout(500);
        await page.screenshot({ path: `tests/visual/mobile-${viewport.name}-branch-menu.png` });

        // Close Branch Menu by clicking the trigger again (toggle)
        await page.locator('.branch-menu-trigger button').first().click();
        await page.waitForTimeout(300);

        // Open Mode Menu (Tools/Settings)
        await page.locator('.mode-menu-trigger button').first().click();
        await page.waitForTimeout(500);
        await page.screenshot({ path: `tests/visual/mobile-${viewport.name}-mode-menu.png` });
    });
}
