import { renderHook, act } from '@testing-library/react';
import { useDynamicPlaceholder } from '../../hooks/useDynamicPlaceholder';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('useDynamicPlaceholder', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should return first placeholder initially', () => {
        const placeholders = ['One', 'Two', 'Three'];
        const { result } = renderHook(() => useDynamicPlaceholder(placeholders));
        expect(result.current).toBe('One');
    });

    it('should cycle through placeholders', () => {
        const placeholders = ['One', 'Two', 'Three'];
        const { result } = renderHook(() => useDynamicPlaceholder(placeholders, 1000));

        expect(result.current).toBe('One');

        act(() => {
            vi.advanceTimersByTime(1000);
        });
        expect(result.current).toBe('Two');

        act(() => {
            vi.advanceTimersByTime(1000);
        });
        expect(result.current).toBe('Three');

        act(() => {
            vi.advanceTimersByTime(1000);
        });
        expect(result.current).toBe('One');
    });

    it('should pause cycling when shouldPause is true', () => {
        const placeholders = ['One', 'Two'];
        const { result, rerender } = renderHook(({ pause }) => useDynamicPlaceholder(placeholders, 1000, pause), {
            initialProps: { pause: false }
        });

        expect(result.current).toBe('One');

        act(() => {
            vi.advanceTimersByTime(1000);
        });
        expect(result.current).toBe('Two');

        // Pause
        rerender({ pause: true });

        act(() => {
            vi.advanceTimersByTime(2000);
        });
        // Should stay 'Two'
        expect(result.current).toBe('Two');

        // Resume
        rerender({ pause: false });
        // The interval restarts
        act(() => {
            vi.advanceTimersByTime(1000);
        });
        expect(result.current).toBe('One');
    });
});
