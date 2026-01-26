import { useState, useEffect, RefObject } from 'react';

export interface ViewportPosition {
    top?: number;
    left?: number;
    transformOrigin?: string;
    position: 'fixed';
}

const VIEWPORT_MARGIN = 10;
const TRIGGER_GAP = 8;

/**
 * Calculates position to keep content within the viewport (with margin).
 * Returns styles for 'fixed' positioning.
 * Uses requestAnimationFrame to throttle resize/scroll events.
 */
export const useViewportAwarePosition = (
    triggerRef: RefObject<HTMLElement | null>,
    contentRef: RefObject<HTMLElement | null>,
    isOpen: boolean
): ViewportPosition => {
    const [style, setStyle] = useState<ViewportPosition>({ position: 'fixed', top: -9999, left: -9999 });

    useEffect(() => {
        if (!isOpen || !triggerRef.current || !contentRef.current) return;

        let rafId: number | null = null;

        const updatePosition = () => {
            if (!triggerRef.current || !contentRef.current) return;

            const triggerRect = triggerRef.current.getBoundingClientRect();
            const contentRect = contentRef.current.getBoundingClientRect();
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;

            // Default: Align bottom-left relative to trigger
            let top = triggerRect.bottom + TRIGGER_GAP;
            let left = triggerRect.left;
            let originY = 'top';

            // 1. Vertical Positioning
            const spaceBelow = viewportHeight - triggerRect.bottom;
            const spaceAbove = triggerRect.top;

            // If not enough space below, and more space above, flip up
            if (spaceBelow < contentRect.height + VIEWPORT_MARGIN && spaceAbove > spaceBelow) {
                top = triggerRect.top - contentRect.height - TRIGGER_GAP;
                originY = 'bottom';

                // If flipping up goes off-screen top
                if (top < VIEWPORT_MARGIN) {
                    top = VIEWPORT_MARGIN; // Clamp to top
                }
            } else {
                // Keep down, but clamp to bottom if needed
                if (top + contentRect.height > viewportHeight - VIEWPORT_MARGIN) {
                    top = viewportHeight - contentRect.height - VIEWPORT_MARGIN;
                }
            }

            // 2. Horizontal Positioning
            // If overflows right:
            if (left + contentRect.width > viewportWidth - VIEWPORT_MARGIN) {
                left = viewportWidth - contentRect.width - VIEWPORT_MARGIN;
            }

            // If overflows left (after adjustment or initially)
            if (left < VIEWPORT_MARGIN) {
                left = VIEWPORT_MARGIN;
            }

            // Calculate transform origin for nice animation
            const triggerCenter = triggerRect.left + triggerRect.width / 2;
            const relativeX = triggerCenter - left;

            setStyle({
                position: 'fixed',
                top,
                left,
                transformOrigin: `${relativeX}px ${originY}`
            });

            rafId = null;
        };

        const onResizeOrScroll = () => {
            if (rafId === null) {
                rafId = requestAnimationFrame(updatePosition);
            }
        };

        // Initial update
        updatePosition();

        // Listeners
        window.addEventListener('resize', onResizeOrScroll);
        window.addEventListener('scroll', onResizeOrScroll, true); // Capture phase

        return () => {
            window.removeEventListener('resize', onResizeOrScroll);
            window.removeEventListener('scroll', onResizeOrScroll, true);
            if (rafId !== null) cancelAnimationFrame(rafId);
        };
    }, [isOpen, triggerRef, contentRef]);

    return style;
};
