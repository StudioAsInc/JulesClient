import React from 'react';
import { render, fireEvent, screen, act } from '@testing-library/react';
import { ChatHistory } from '../../components/ChatHistory';
import { describe, it } from 'vitest';
import { JulesActivity } from '../../types';

// Generate a large patch
const generateLargePatch = (lines: number) => {
    let patch = "diff --git a/file.ts b/file.ts\nindex 123..456 100644\n--- a/file.ts\n+++ b/file.ts\n@@ -1,1 +1,100 @@\n";
    for (let i = 0; i < lines; i++) {
        patch += `+ const line${i} = "added line";\n`;
    }
    return patch;
};

const largePatch = generateLargePatch(10000); // Increased size to make impact more visible

const activity: JulesActivity = {
    name: 'activities/1',
    createTime: new Date().toISOString(),
    originator: 'agent',
    description: 'Coding',
    artifacts: [
        {
            changeSet: {
                gitPatch: {
                    unidiffPatch: largePatch,
                    suggestedCommitMessage: 'Large Refactor'
                }
            }
        }
    ]
};

describe('ChatHistory Performance', () => {
    it('renders large patch and toggles efficiently', async () => {
        const { container } = render(
            <ChatHistory
                activities={[activity]}
                isStreaming={false}
                onApprovePlan={() => {}}
                defaultCardCollapsed={false} // Start expanded to force initial render cost
            />
        );

        // Initial render cost is already paid.
        // Now find the toggle button. In CodeChangeArtifact it's the header div.
        // We can find it by text "Code Changes Proposed"
        const toggleHeader = screen.getByText('Large Refactor');

        const start = performance.now();

        // Toggle collapse (hide)
        await act(async () => {
            fireEvent.click(toggleHeader);
        });

        // Toggle expand (show) - THIS is where re-rendering the list happens
        await act(async () => {
            fireEvent.click(toggleHeader);
        });

        const end = performance.now();
        console.log(`ChatHistory toggle time (10000 line patch): ${(end - start).toFixed(2)}ms`);
    });
});
