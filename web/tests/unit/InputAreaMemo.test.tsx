import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { InputArea } from '../../components/InputArea';

// Mock Lucide icons
vi.mock('lucide-react', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        Plus: () => <div data-testid="icon-plus" />,
        Rocket: () => <div data-testid="icon-rocket" />,
        ArrowRight: () => <div data-testid="icon-arrow-right" />,
        Check: () => <div data-testid="icon-check" />,
        Clock: () => <div data-testid="icon-clock" />,
        MessageSquare: () => <div data-testid="icon-message-square" />,
        FileSearch: () => <div data-testid="icon-file-search" />,
        Search: () => <div data-testid="icon-search" />,
        GitBranch: () => <div data-testid="icon-git-branch" />,
        ChevronDown: () => <div data-testid="icon-chevron-down" />,
        Type: () => <div data-testid="icon-type" />,
        Settings2: () => <div data-testid="icon-settings-2" />,
    };
});

describe('InputArea Optimization', () => {
    it('should be a memoized component', () => {
        // React.memo components have a specific symbol type
        const memoSymbol = Symbol.for('react.memo');
        expect((InputArea as any).$$typeof).toBe(memoSymbol);
    });
});
