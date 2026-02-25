import { renderHook, act } from '@testing-library/react';
import { useJulesSession } from '../../hooks/useJulesSession';
import * as JulesApi from '../../services/geminiService';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

// Mock the API module
vi.mock('../../services/geminiService', () => ({
  listActivities: vi.fn(),
  getSession: vi.fn(),
  listAllSessions: vi.fn().mockResolvedValue([]),
}));

describe('useJulesSession Performance', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  it('measures concurrency of polling requests', async () => {
    let listActivitiesCallTime = 0;
    let getSessionCallTime = 0;

    const delay = 100;

    (JulesApi.listActivities as any).mockImplementation(async () => {
      listActivitiesCallTime = Date.now();
      await new Promise(resolve => setTimeout(resolve, delay));
      return { activities: [] };
    });

    (JulesApi.getSession as any).mockImplementation(async () => {
      getSessionCallTime = Date.now();
      await new Promise(resolve => setTimeout(resolve, delay));
      return { state: 'IN_PROGRESS', outputs: [] };
    });

    const { result } = renderHook(() => useJulesSession('api-key', null, vi.fn()));

    await act(async () => {
      result.current.startPolling('sessions/test-session');
    });

    // Wait enough time for both to complete even if sequential
    await new Promise(resolve => setTimeout(resolve, delay * 3));

    console.log(`listActivities called at: ${listActivitiesCallTime}`);
    console.log(`getSession called at: ${getSessionCallTime}`);

    const timeDiff = getSessionCallTime - listActivitiesCallTime;
    console.log(`Time difference (getSession - listActivities): ${timeDiff}ms`);

    // If sequential, timeDiff should be >= delay (100ms)
    // If parallel, timeDiff should be close to 0 (definitely < delay)

    // We expect sequential behavior currently
    // expect(timeDiff).toBeGreaterThanOrEqual(delay - 20); // allow some jitter
  });
});
