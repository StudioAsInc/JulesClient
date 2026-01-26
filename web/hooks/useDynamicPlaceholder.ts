import { useState, useEffect } from 'react';

export const useDynamicPlaceholder = (
    placeholders: string[],
    intervalMs: number = 3000,
    shouldPause: boolean = false
) => {
    const [index, setIndex] = useState(0);

    useEffect(() => {
        if (shouldPause || placeholders.length <= 1) return;

        const interval = setInterval(() => {
            setIndex((prev) => (prev + 1) % placeholders.length);
        }, intervalMs);

        return () => clearInterval(interval);
    }, [placeholders.length, intervalMs, shouldPause]);

    return placeholders[index];
};
