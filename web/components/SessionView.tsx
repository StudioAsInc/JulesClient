import React, { useRef, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { ChatHistory } from './ChatHistory';
import { InputArea, SessionCreateOptions } from './InputArea';
import { JulesActivity, JulesSession } from '../types';
import { AlertCircle } from 'lucide-react';

interface SessionViewProps {
    session: JulesSession;
    activities: JulesActivity[];
    isProcessing: boolean;
    error: string | null;
    onSendMessage: (text: string) => void;
    onApprovePlan: (id: string) => void;
    defaultCardCollapsed: boolean;
}

export const SessionView: React.FC<SessionViewProps> = ({
    session,
    activities,
    isProcessing,
    error,
    onSendMessage,
    onApprovePlan,
    defaultCardCollapsed
}) => {
    const scrollRef = useRef<HTMLDivElement>(null);
    const isAtBottom = useRef(true); // Default to true so initial render scrolls to bottom
    const prevActivityCount = useRef(activities.length);

    const handleScroll = useCallback(() => {
        if (!scrollRef.current) return;
        const { scrollTop, scrollHeight, clientHeight } = scrollRef.current;
        const distanceToBottom = scrollHeight - scrollTop - clientHeight;
        // Check if user is near bottom (100px tolerance)
        isAtBottom.current = distanceToBottom < 100;
    }, []);

    // Auto-scroll to bottom on new activities or streaming updates
    useEffect(() => {
        if (!scrollRef.current) return;

        const { scrollHeight } = scrollRef.current;
        const isNewActivity = activities.length > prevActivityCount.current;
        const lastActivity = activities.length > 0 ? activities[activities.length - 1] : undefined;
        const isUserMessage = lastActivity?.originator === 'user';

        // Scroll if:
        // 1. We were already at the bottom (sticky scroll)
        // 2. A new message arrived AND it's from the user (force scroll for user action)
        if (isAtBottom.current || (isNewActivity && isUserMessage)) {
            scrollRef.current.scrollTop = scrollHeight;
            isAtBottom.current = true;
        }

        prevActivityCount.current = activities.length;
    }, [activities, isProcessing]);

    // Wrapper to adapt simple text callback to SessionCreateOptions signature
    const handleSendMessage = useCallback((text: string, _options: SessionCreateOptions) => {
        onSendMessage(text);
    }, [onSendMessage]);

    return (
        <div className="flex-1 flex flex-col h-full relative overflow-hidden bg-background">
            {/* Session Breadcrumbs / Header - Fixed top */}
            <div className="flex-shrink-0 px-4 py-2 sm:px-6 sm:py-3 border-b border-white/5 bg-surface/80 backdrop-blur-sm z-10 flex items-center gap-2 text-sm sticky top-0">
                <Link to="/" className="text-zinc-500 hover:text-white transition-colors">Chat</Link>
                <span className="text-zinc-700">/</span>
                <span className="text-zinc-300 font-mono truncate max-w-xl">{session.title || session.name}</span>
            </div>

            {/* Scrollable Chat Area */}
            <div
                ref={scrollRef}
                onScroll={handleScroll}
                className="flex-1 overflow-y-auto px-4 sm:px-0 scroll-smooth"
            >
                {/* 
                  Extra bottom padding ensures the last message can be scrolled 
                  above the floating input area. 
                  Input Area ~50-80px + spacing.
                */}
                <div className="max-w-4xl mx-auto py-6" style={{ paddingBottom: '160px' }}>
                    <ChatHistory
                        activities={activities}
                        isStreaming={isProcessing}
                        onApprovePlan={onApprovePlan}
                        sessionOutputs={session.outputs}
                        sessionPrompt={session.prompt}
                        sessionCreateTime={session.createTime}
                        defaultCardCollapsed={defaultCardCollapsed}
                    />

                    {error && (
                        <div className="mx-4 sm:mx-0 mt-4 p-3 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
                            <AlertCircle size={16} />
                            {error}
                        </div>
                    )}
                </div>
            </div>

            {/* Floating Bottom Input Area - Overlay */}
            <div className="absolute bottom-0 left-0 right-0 z-20 pointer-events-none">
                {/* Gradient fade to seamlessly blend scrolling content */}
                <div className="absolute inset-0 bg-gradient-to-t from-background via-background to-transparent" style={{ top: '-40px' }} />

                {/* Actual Input Container */}
                <div className="relative pointer-events-auto px-2 sm:px-4 pb-[calc(1rem+env(safe-area-inset-bottom))] sm:pb-[calc(1.5rem+env(safe-area-inset-bottom))] pt-4">
                    <InputArea
                        onSendMessage={handleSendMessage}
                        isLoading={isProcessing}
                        variant="chat"
                    />
                </div>
            </div>
        </div>
    );
};
