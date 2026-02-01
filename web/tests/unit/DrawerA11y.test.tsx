import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import '@testing-library/jest-dom';
import { Drawer } from '../../components/Drawer';
import { JulesSession, JulesSource } from '../../types';
import { MemoryRouter } from 'react-router-dom';

// Mock Lucide icons
vi.mock('lucide-react', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        X: () => <div data-testid="icon-x" />,
        Search: () => <div data-testid="icon-search" />,
        ChevronDown: () => <div data-testid="icon-chevron-down" />,
        ChevronRight: () => <div data-testid="icon-chevron-right" />,
        MoreHorizontal: () => <div data-testid="icon-more-horizontal" />,
        Github: () => <div data-testid="icon-github" />,
        FileText: () => <div data-testid="icon-file-text" />,
        CheckCircle2: () => <div data-testid="icon-check-circle-2" />,
        Disc: () => <div data-testid="icon-disc" />,
        ArrowUp: () => <div data-testid="icon-arrow-up" />,
        Loader2: () => <div data-testid="icon-loader-2" />,
        Clock: () => <div data-testid="icon-clock" />,
        MessageCircle: () => <div data-testid="icon-message-circle" />,
        Pause: () => <div data-testid="icon-pause" />,
        XCircle: () => <div data-testid="icon-x-circle" />,
        AlertCircle: () => <div data-testid="icon-alert-circle" />,
        Lock: () => <div data-testid="icon-lock" />,
        Trash2: () => <div data-testid="icon-trash-2" />,
        Settings: () => <div data-testid="icon-settings" />,
    };
});

const mockSessions: JulesSession[] = [
    {
        name: 'sessions/1',
        title: 'Fix Login Bug',
        prompt: 'Fix the login bug',
        state: 'IN_PROGRESS',
        createTime: '2023-01-01T10:00:00Z'
    },
    {
        name: 'sessions/2',
        // No title, should fallback to prompt
        prompt: 'Refactor Header',
        state: 'PAUSED',
        createTime: '2023-01-02T10:00:00Z'
    }
];

const mockSources: JulesSource[] = [
    { name: 'sources/github/owner/repo1', displayName: 'owner/repo1' }
];

describe('Drawer Accessibility', () => {
    it('should have accessible session toggle', async () => {
        render(
            <MemoryRouter>
                <Drawer
                    isOpen={true}
                    onClose={() => {}}
                    sessions={mockSessions}
                    sources={mockSources}
                    onSelectSession={() => {}}
                    onSelectSource={() => {}}
                />
            </MemoryRouter>
        );

        // Wait for render
        await waitFor(() => expect(screen.getByText('Recent sessions')).toBeInTheDocument());

        const sessionToggle = screen.getByText('Recent sessions').closest('button');
        expect(sessionToggle).toHaveAttribute('aria-expanded', 'true');

        // Click to collapse
        fireEvent.click(sessionToggle!);
        expect(sessionToggle).toHaveAttribute('aria-expanded', 'false');
    });

    it('should have accessible repository toggle', async () => {
        render(
            <MemoryRouter>
                <Drawer
                    isOpen={true}
                    onClose={() => {}}
                    sessions={mockSessions}
                    sources={mockSources}
                    onSelectSession={() => {}}
                    onSelectSource={() => {}}
                />
            </MemoryRouter>
        );

        // Wait for render
        await waitFor(() => expect(screen.getByText('Repositories')).toBeInTheDocument());

        const repoToggle = screen.getByText('Repositories').closest('button');
        expect(repoToggle).toHaveAttribute('aria-expanded', 'true');

        // Click to collapse
        fireEvent.click(repoToggle!);
        expect(repoToggle).toHaveAttribute('aria-expanded', 'false');
    });

    it('should have accessible session options menu', async () => {
        render(
            <MemoryRouter>
                <Drawer
                    isOpen={true}
                    onClose={() => {}}
                    sessions={mockSessions}
                    sources={mockSources}
                    onSelectSession={() => {}}
                    onSelectSource={() => {}}
                />
            </MemoryRouter>
        );

        // Wait for render
        await waitFor(() => expect(screen.getByText('Fix Login Bug')).toBeInTheDocument());

        // Find the options button for the first session
        const optionsButton = screen.getByLabelText('Session options for Fix Login Bug');
        expect(optionsButton).toBeInTheDocument();
        expect(optionsButton).toHaveAttribute('aria-haspopup', 'true');
        expect(optionsButton).toHaveAttribute('aria-expanded', 'false');

        // Open menu
        fireEvent.click(optionsButton);
        expect(optionsButton).toHaveAttribute('aria-expanded', 'true');

        // Check menu role
        const menu = screen.getByRole('menu');
        expect(menu).toBeInTheDocument();

        // Check menu items role
        const menuItems = screen.getAllByRole('menuitem');
        // IN_PROGRESS session should have Pause and Delete options
        expect(menuItems.length).toBeGreaterThan(0);
        expect(menuItems[0]).toHaveTextContent(/Pause/i);
    });

    it('should fallback to prompt for session options label if title is missing', async () => {
        render(
            <MemoryRouter>
                <Drawer
                    isOpen={true}
                    onClose={() => {}}
                    sessions={mockSessions}
                    sources={mockSources}
                    onSelectSession={() => {}}
                    onSelectSource={() => {}}
                />
            </MemoryRouter>
        );

        await waitFor(() => expect(screen.getByText('Refactor Header')).toBeInTheDocument());

        // The second session has no title, so it should use the prompt "Refactor Header"
        const optionsButton = screen.getByLabelText('Session options for Refactor Header');
        expect(optionsButton).toBeInTheDocument();
    });

    it('should have accessible documentation button', async () => {
        render(
            <MemoryRouter>
                <Drawer
                    isOpen={true}
                    onClose={() => {}}
                    sessions={mockSessions}
                    sources={mockSources}
                    onSelectSession={() => {}}
                    onSelectSource={() => {}}
                />
            </MemoryRouter>
        );

        await waitFor(() => expect(screen.getByLabelText('Documentation')).toBeInTheDocument());
        const docButton = screen.getByLabelText('Documentation');
        expect(docButton).toHaveAttribute('title', 'Documentation');
    });
});
