import { renderHook } from '@testing-library/react';
import { useViewportAwarePosition } from '../../hooks/useViewportAwarePosition';
import { vi, describe, it, expect, beforeEach } from 'vitest';

describe('useViewportAwarePosition', () => {
    let triggerRef: any;
    let contentRef: any;

    beforeEach(() => {
        triggerRef = {
            current: {
                getBoundingClientRect: vi.fn(() => ({
                    top: 100,
                    bottom: 130,
                    left: 100,
                    right: 200,
                    width: 100,
                    height: 30
                }))
            }
        };
        contentRef = {
            current: {
                getBoundingClientRect: vi.fn(() => ({
                    width: 200,
                    height: 300
                }))
            }
        };

        // Mock window dimensions
        Object.defineProperty(window, 'innerWidth', { value: 1000, configurable: true });
        Object.defineProperty(window, 'innerHeight', { value: 800, configurable: true });
    });

    it('should position below trigger by default', () => {
        const { result } = renderHook(() => useViewportAwarePosition(triggerRef, contentRef, true));

        expect(result.current.top).toBe(138); // 130 (bottom) + 8 (gap)
        expect(result.current.left).toBe(100); // 100 (left)
        expect(result.current.position).toBe('fixed');
    });

    it('should flip to top if space below is insufficient', () => {
        // Mock viewport height to be small
        Object.defineProperty(window, 'innerHeight', { value: 400 });
        // Trigger at bottom: top 350, bottom 380
        triggerRef.current.getBoundingClientRect = vi.fn(() => ({
            top: 350,
            bottom: 380,
            left: 100,
            right: 200,
            width: 100,
            height: 30
        }));

        const { result } = renderHook(() => useViewportAwarePosition(triggerRef, contentRef, true));

        // Should flip up
        // Top = trigger.top (350) - content.height (300) - gap (8) = 42
        expect(result.current.top).toBe(42);
    });

    it('should clamp to right margin if overflowing right', () => {
        Object.defineProperty(window, 'innerWidth', { value: 375 }); // Mobile width
        // Trigger at right edge
        triggerRef.current.getBoundingClientRect = vi.fn(() => ({
            top: 100,
            bottom: 130,
            left: 300,
            right: 350,
            width: 50,
            height: 30
        }));

        const { result } = renderHook(() => useViewportAwarePosition(triggerRef, contentRef, true));

        // Left should be constrained
        // Content width 200. Window width 375. Margin 10.
        // Max left = 375 - 200 - 10 = 165.
        // Calculated left would be 300.
        expect(result.current.left).toBe(165);
    });

    it('should return default state if not open', () => {
        const { result } = renderHook(() => useViewportAwarePosition(triggerRef, contentRef, false));
        expect(result.current.top).toBe(-9999);
    });
});
