import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
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

describe('InputArea Accessibility', () => {
    const mockOnSendMessage = vi.fn();
    const mockSource = {
        name: 'sources/123',
        displayName: 'My Repo',
        githubRepo: {
            defaultBranch: { displayName: 'main' },
            branches: [
                { displayName: 'main' },
                { displayName: 'develop' }
            ]
        }
    };

    it('should have accessible branch menu', () => {
        render(
            <InputArea
                onSendMessage={mockOnSendMessage}
                isLoading={false}
                currentSource={mockSource}
            />
        );

        // Expand input
        const input = screen.getByLabelText('Message input');
        fireEvent.focus(input);
        fireEvent.change(input, { target: { value: ' ' } });

        // Open branch menu
        const branchButton = screen.getByLabelText(/Select branch/i);
        fireEvent.click(branchButton);

        // Check search input label
        const searchInput = screen.getByLabelText('Filter branches');
        expect(searchInput).toBeInTheDocument();

        // Check menu role
        const menu = screen.getByRole('menu');
        expect(menu).toBeInTheDocument();

        // Check menu items
        const options = screen.getAllByRole('menuitemradio');
        expect(options).toHaveLength(2);

        // Check selection state (main should be selected by default)
        expect(options[0]).toHaveTextContent('main');
        expect(options[0]).toHaveAttribute('aria-checked', 'true');

        expect(options[1]).toHaveTextContent('develop');
        expect(options[1]).toHaveAttribute('aria-checked', 'false');
    });

    it('should have accessible mode menu', () => {
        render(
            <InputArea
                onSendMessage={mockOnSendMessage}
                isLoading={false}
            />
        );

        // Expand input
        const input = screen.getByLabelText('Message input');
        fireEvent.focus(input);
        fireEvent.change(input, { target: { value: ' ' } });

        // Open mode menu
        const settingsButton = screen.getByLabelText('Session settings');
        fireEvent.click(settingsButton);

        // Check menu role
        // Note: There might be multiple menus if we didn't close the branch one in a real scenario,
        // but here we are in a fresh render.
        const menu = screen.getByRole('menu');
        expect(menu).toBeInTheDocument();

        // Check menu items
        const options = screen.getAllByRole('menuitemradio');
        // 'START', 'SCHEDULED', 'INTERACTIVE', 'REVIEW'
        expect(options).toHaveLength(4);

        // Check selection state (START is default)
        expect(options[0]).toHaveAttribute('aria-checked', 'true');
        expect(options[1]).toHaveAttribute('aria-checked', 'false');
    });
});
