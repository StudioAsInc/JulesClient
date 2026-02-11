import React from 'react';
import { InputArea, SessionCreateOptions } from './InputArea';
import { ProactiveSection } from './ProactiveSection';
import { Box, Terminal, Code, AlertCircle } from 'lucide-react';
import { JulesSource, JulesSession } from '../types';

interface HomeViewProps {
    onSendMessage: (text: string, options: SessionCreateOptions) => void;
    isProcessing: boolean;
    error: string | null;
    currentSource: JulesSource | null;
    onResetKey: () => void;
    sessions?: JulesSession[];
    onSelectSession?: (session: JulesSession) => void;
}

export const HomeView: React.FC<HomeViewProps> = ({
    onSendMessage,
    isProcessing,
    error,
    currentSource,
    onResetKey,
    sessions = [],
    onSelectSession
}) => {
    return (
        <div className="flex-1 flex flex-col w-full overflow-y-auto scroll-smooth">
            {/* Dynamic Centering Container - full width on mobile, constrained on desktop */}
            <div className="w-full max-w-3xl mx-auto flex flex-col items-center animate-in fade-in duration-500 px-4 md:px-6 lg:px-8 pt-6 sm:pt-16 pb-[calc(3rem+env(safe-area-inset-bottom))]">


                {!currentSource && (
                    <div className="w-full text-amber-500 mb-6 flex items-center gap-3 text-sm bg-amber-500/10 px-4 py-3 rounded-xl border border-amber-500/20">
                        <AlertCircle size={18} />
                        <span>No repositories found. Ensure the Jules App is installed on your GitHub.</span>
                    </div>
                )}

                {/* Main Input Card - The "Hero" */}
                {/* z-index ensures it stays on top during expansion animations */}
                <div className="w-full mb-8 relative z-20">
                    <InputArea
                        onSendMessage={onSendMessage}
                        isLoading={isProcessing}
                        variant="default"
                        placeholder="Describe a task or fix..."
                        currentSource={currentSource}
                    />

                    {error && (
                        <div className="mt-4 p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm flex items-center gap-2">
                            <AlertCircle size={16} />
                            {error}
                        </div>
                    )}
                </div>

                {/* Proactive Section (Cards below) */}
                <div className="w-full z-10">
                    <ProactiveSection sessions={sessions} onSelectSession={onSelectSession} />
                </div>

                {/* Footer Actions */}
                <div className="w-full mt-8 sm:mt-12 flex items-center justify-between gap-4">
                    <div className="flex gap-2">
                        <ActionButton icon={<Box size={14} />} label="Render" aria-label="Open Render Dashboard" />
                        <ActionButton icon={<Terminal size={14} />} label="CLI" aria-label="Open CLI Tool" />
                        <ActionButton icon={<Code size={14} />} label="API" aria-label="View API Documentation" />
                    </div>

                    <button
                        onClick={onResetKey}
                        className="text-xs text-zinc-600 hover:text-zinc-400 underline underline-offset-2 decoration-zinc-800 h-8 flex items-center"
                    >
                        Reset Key
                    </button>
                </div>
            </div>
        </div>
    );
};

const ActionButton: React.FC<{ icon: React.ReactNode; label: string; 'aria-label'?: string }> = ({ icon, label, 'aria-label': ariaLabel }) => (
    <button
        aria-label={ariaLabel || label}
        className="flex items-center gap-2 px-3 py-1.5 bg-surfaceHighlight hover:bg-surfaceHighlight border border-white/10 hover:border-white/20 rounded-lg text-xs font-mono text-zinc-400 hover:text-white transition-all h-8 shadow-sm"
    >
        {icon}
        <span>{label}</span>
    </button>
);