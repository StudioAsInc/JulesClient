import { renderHook, act } from '@testing-library/react';
import { useJulesSession } from '../../hooks/useJulesSession';
import * as JulesApi from '../../services/geminiService';
import { vi, describe, it, expect, beforeEach } from 'vitest';

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

    // Wait enough time for both to complete
    await new Promise(resolve => setTimeout(resolve, delay * 3));

    const timeDiff = Math.abs(getSessionCallTime - listActivitiesCallTime);

    // With parallel execution, time difference should be negligible (much less than delay)
    expect(timeDiff).toBeLessThan(delay / 2);
  });
});
