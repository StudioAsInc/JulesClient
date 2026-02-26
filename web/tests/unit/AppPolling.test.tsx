import React from 'react';
import { render, waitFor, act, screen } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import App from '../../App';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '../../contexts/ThemeContext';

// Define mocks
const mockListSources = vi.fn();
const mockListAllSessions = vi.fn();
const mockGetSession = vi.fn();
const mockListActivities = vi.fn();

// Mock the API
vi.mock('../../services/geminiService', () => {
    return {
        GeminiService: vi.fn(function() {
            return {
                listSources: mockListSources,
                listAllSessions: mockListAllSessions,
                getSession: mockGetSession,
                listActivities: mockListActivities,
            };
        })
    };
});

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
        mockListSources.mockResolvedValue({ sources: [] });
        mockListAllSessions.mockResolvedValue([]);
    });

    it('should stop polling when session is COMPLETED', async () => {
        const sessionName = 'sessions/123';
        const completedSession = {
            name: sessionName,
            state: 'COMPLETED',
            createTime: new Date().toISOString(),
            prompt: 'test',
        };

        mockGetSession.mockResolvedValue(completedSession);
        mockListActivities.mockResolvedValue({ activities: [] });

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
            expect(mockGetSession).toHaveBeenCalledWith(sessionName);
        }, { timeout: 3000 });

        const initialCallCount = mockGetSession.mock.calls.length;

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

        expect(mockGetSession.mock.calls.length).toBe(initialCallCount);
    }, 10000);

    it('should stop polling when session is FAILED', async () => {
        const sessionName = 'sessions/456';
        const failedSession = {
            name: sessionName,
            state: 'FAILED',
            createTime: new Date().toISOString(),
            prompt: 'test',
        };

        mockGetSession.mockResolvedValue(failedSession);
        mockListActivities.mockResolvedValue({ activities: [] });

        render(
            <ThemeProvider>
                <MemoryRouter initialEntries={[`/session/456`]}>
                    <App />
                </MemoryRouter>
            </ThemeProvider>
        );

        await waitFor(() => {
            expect(mockGetSession).toHaveBeenCalledWith(sessionName);
        }, { timeout: 3000 });

        const initialCallCount = mockGetSession.mock.calls.length;

        await act(async () => {
            await new Promise(r => setTimeout(r, 2500));
        });

        expect(mockGetSession.mock.calls.length).toBe(initialCallCount);
    }, 10000);
});
