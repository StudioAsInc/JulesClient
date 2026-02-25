import { test, expect } from '@playwright/test';

test.describe('Navigation and Environment Variables', () => {

    test('should bypass login if API key is present (simulated) and navigate pages', async ({ page }) => {
        // 1. Simulate a logged-in state by injecting the key into localStorage.
        // This validates that IF the app logic works (checking env var OR localStorage),
        // it proceeds to the main app. Since we can't easily inject env vars into the
        // running Vite server from here without restarting it, we rely on localStorage
        // to verify the "authenticated" flow.
        await page.addInitScript(() => {
            localStorage.setItem('jules_api_key', 'test-dummy-key');
        });

        // 2. Navigate to Home
        await page.goto('http://localhost:3000/');

        // Verify we are on the home page (looking for the input placeholder or hero text)
        // The placeholder in HomeView is "Describe a task or fix..."
        // Or we can look for "Jules" logo or header.
        await expect(page.getByPlaceholder(/Describe a task/i)).toBeVisible({ timeout: 10000 });
        await page.screenshot({ path: 'home_logged_in.png' });

        // 3. Navigate to Settings directly
        await page.goto('http://localhost:3000/settings');
        // Use more specific locators to avoid ambiguity
        await expect(page.getByRole('heading', { name: 'General' })).toBeVisible();
        await expect(page.getByRole('heading', { name: 'Appearance' })).toBeVisible();
        await page.screenshot({ path: 'settings_view.png' });

        // 4. Navigate to a dummy Repository
        // Since we don't have real data, this might show a loading state or the view with empty data.
        // The URL pattern is /repository/:repoName
        await page.goto('http://localhost:3000/repository/owner/repo');
        // It should not crash. It might show "Loading repository..." or the repository view.
        // RepositoryView checks `currentSource`. If not found, it shows "Loading repository...".
        // Since we didn't mock the API to return sources, it will likely stay loading or show an error if it tries to fetch.
        // However, the test ensures it doesn't *crash* (white screen).
        // We can check for the loading text or the header.
        // The Header component is always present.
        await expect(page.locator('header')).toBeVisible();

        // 5. Navigate to a dummy Session
        await page.goto('http://localhost:3000/session/123');
        // Similarly, check if it handles the missing session gracefully.
        // SessionView shows "Loading session..." if currentSession is null.
        await expect(page.getByText('Loading session...')).toBeVisible();

        // Check for no console errors (Playwright doesn't fail on console errors by default,
        // but we can add a listener if we want to be strict. For now, visible crash check is enough).
    });
});
