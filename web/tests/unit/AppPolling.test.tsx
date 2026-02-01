import React from 'react';
import { render, waitFor, act, screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../../App';
import * as JulesApi from '../../services/geminiService';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '../../contexts/ThemeContext';

// Mock the API
vi.mock('../../services/geminiService', () => ({
    setApiKey: vi.fn(),
    setPaginationSettings: vi.fn(),
    listSources: vi.fn(),
    listAllSessions: vi.fn(),
    getSession: vi.fn(),
    listActivities: vi.fn(),
}));

// Mock scrollIntoView
window.HTMLElement.prototype.scrollIntoView = vi.fn();

// Mock localStorage
const localStorageMock = (function() {
    let store: Record<string, string> = {};
    return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => { store[key] = value.toString(); },
        removeItem: (key: string) => { delete store[key]; },
        clear: () => { store = {}; }
    };
})();

Object.defineProperty(window, 'localStorage', {
    value: localStorageMock
});

describe('App Polling Logic', () => {
    beforeEach(() => {
        // Use real timers by default to avoid waitFor issues
        vi.useRealTimers();
        localStorageMock.clear();
        vi.clearAllMocks();

        // Setup default mocks
        localStorageMock.setItem('jules_api_key', 'test-key');
        (JulesApi.listSources as any).mockResolvedValue({ sources: [] });
        (JulesApi.listAllSessions as any).mockResolvedValue([]);
    });

    it('should stop polling when session is COMPLETED', async () => {
        const sessionName = 'sessions/123';
        const completedSession = {
            name: sessionName,
            state: 'COMPLETED',
            createTime: new Date().toISOString(),
            prompt: 'test',
        };

        (JulesApi.getSession as any).mockResolvedValue(completedSession);
        (JulesApi.listActivities as any).mockResolvedValue({ activities: [] });

        // Render App with a route that triggers session loading
        render(
            <ThemeProvider>
                <MemoryRouter initialEntries={[`/session/123`]}>
                    <App />
                </MemoryRouter>
            </ThemeProvider>
        );

        // Wait for initial load
        await waitFor(() => {
            expect(JulesApi.getSession).toHaveBeenCalledWith(sessionName);
        }, { timeout: 3000 });

        const initialCallCount = (JulesApi.getSession as any).mock.calls.length;

        // Wait for polling interval (2s) + some buffer
        // Using real timers means we just wait
        await act(async () => {
            await new Promise(r => setTimeout(r, 2500));
        });

        // Fixed behavior: it should NOT call getSession anymore because state is COMPLETED.
        // Initial load calls getSession (1).
        // Then startPolling calls poll -> getSession (2).
        // Then poll checks state -> COMPLETED -> Stops.
        // So count should remain equal to initialCallCount (which captures up to step 2 usually, or close to it).

        // Wait, initialCallCount was captured AFTER initial load.
        // Let's rely on absolute numbers if possible or just equality.
        // If initialCallCount captured the first poll call, then it should stay there.

        expect((JulesApi.getSession as any).mock.calls.length).toBe(initialCallCount);
    }, 10000);

    it('should stop polling when session is FAILED', async () => {
        const sessionName = 'sessions/456';
        const failedSession = {
            name: sessionName,
            state: 'FAILED',
            createTime: new Date().toISOString(),
            prompt: 'test',
        };

        (JulesApi.getSession as any).mockResolvedValue(failedSession);
        (JulesApi.listActivities as any).mockResolvedValue({ activities: [] });

        render(
            <ThemeProvider>
                <MemoryRouter initialEntries={[`/session/456`]}>
                    <App />
                </MemoryRouter>
            </ThemeProvider>
        );

        await waitFor(() => {
            expect(JulesApi.getSession).toHaveBeenCalledWith(sessionName);
        }, { timeout: 3000 });

        const initialCallCount = (JulesApi.getSession as any).mock.calls.length;

        await act(async () => {
            await new Promise(r => setTimeout(r, 2500));
        });

        expect((JulesApi.getSession as any).mock.calls.length).toBe(initialCallCount);
    }, 10000);
});
