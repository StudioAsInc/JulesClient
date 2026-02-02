import React from 'react';
import { render, screen } from '@testing-library/react';
import { vi, describe, it, expect } from 'vitest';
import '@testing-library/jest-dom';
import { InputArea } from '../../components/InputArea';

// Mock Lucide icons
vi.mock('lucide-react', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        Plus: () => <div data-testid="icon-plus" />,
        Rocket: () => <div data-testid="icon-rocket" />,
        ArrowRight: () => <div data-testid="icon-arrow-right" />,
    };
});

// Mock hooks
vi.mock('../../hooks/useViewportAwarePosition', () => ({
    useViewportAwarePosition: () => ({ top: 0, left: 0, transformOrigin: 'top left' })
}));

vi.mock('../../hooks/useDynamicPlaceholder', () => ({
    useDynamicPlaceholder: () => 'Type something...'
}));

describe('InputArea Chat Variant Accessibility', () => {
    const mockOnSendMessage = vi.fn();

    it('should have accessible buttons in chat variant', () => {
        render(
            <InputArea
                onSendMessage={mockOnSendMessage}
                isLoading={false}
                variant="chat"
            />
        );

        // Check for "Add attachment" button by label
        const attachButton = screen.getByLabelText('Add attachment');
        expect(attachButton).toBeInTheDocument();

        // Check for "Send message" button by label
        const sendButton = screen.getByLabelText('Send message');
        expect(sendButton).toBeInTheDocument();
    });
});
