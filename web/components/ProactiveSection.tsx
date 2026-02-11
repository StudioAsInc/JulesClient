import React, { useState } from 'react';
import { Lightbulb, Clock, Zap, Palette, ShieldCheck, ListTodo, Rocket, MoreHorizontal, Circle } from 'lucide-react';
import { JulesSession } from '../types';

interface ProactiveSectionProps {
    sessions?: JulesSession[];
    onSelectSession?: (session: JulesSession) => void;
}

// Helper function to get readable status text
const getSessionStatusText = (state: JulesSession['state']): string => {
    switch (state) {
        case 'QUEUED': return 'Queued';
        case 'PLANNING': return 'Planning...';
        case 'AWAITING_PLAN_APPROVAL': return 'Awaiting Approval';
        case 'AWAITING_USER_FEEDBACK': return 'Needs Feedback';
        case 'IN_PROGRESS': return 'In Progress...';
        case 'PAUSED': return 'Paused';
        case 'COMPLETED': return 'Completed';
        case 'FAILED': return 'Failed';
        default: return 'Unknown';
    }
};

export const ProactiveSection: React.FC<ProactiveSectionProps> = ({ sessions = [], onSelectSession }) => {
    const [isEnabled, setIsEnabled] = useState(false);
    const [activeTab, setActiveTab] = useState('overview');

    // Find the latest running/active session (not completed or failed)
    const runningSession = sessions.find(s =>
        ['QUEUED', 'PLANNING', 'AWAITING_PLAN_APPROVAL', 'AWAITING_USER_FEEDBACK', 'IN_PROGRESS', 'PAUSED'].includes(s.state)
    );

    return (
        <div className="mt-4 space-y-6">

            {/* Tabs - Scrollable on mobile */}
            <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
                <TabButton label="Repo overview" isActive={activeTab === 'overview'} onClick={() => setActiveTab('overview')} />
                <TabButton label="Suggested" isActive={activeTab === 'suggested'} onClick={() => setActiveTab('suggested')} />
                <TabButton label="Scheduled" isActive={activeTab === 'scheduled'} onClick={() => setActiveTab('scheduled')} />
            </div>

            {/* Auto Find Issues Toggle Card - Improved Hierarchy */}
            <div className="bg-surface rounded-xl p-4 border border-white/5 flex flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-3.5">
                    <div className="w-10 h-10 rounded-full bg-surfaceHighlight border border-white/5 flex items-center justify-center text-indigo-400 flex-shrink-0">
                        <Lightbulb size={20} />
                    </div>
                    <div className="flex flex-col">
                        <div className="text-[15px] font-medium text-zinc-100 leading-snug">
                            Automatically find issues
                        </div>
                        <div className="text-xs text-zinc-500">
                            Scan codebase for bugs & improvements
                        </div>
                    </div>
                </div>

                <div className="flex items-center gap-3">
                    <span className="text-xs font-mono text-zinc-600 hidden sm:block">0/5</span>
                    <div
                        onClick={() => setIsEnabled(!isEnabled)}
                        className={`
                            w-[44px] h-[24px] rounded-full relative transition-colors duration-300 ease-in-out cursor-pointer flex-shrink-0
                            ${isEnabled ? 'bg-indigo-600' : 'bg-zinc-700'}
                        `}
                        role="switch"
                        aria-checked={isEnabled}
                    >
                        <span
                            className={`
                                absolute top-[2px] left-[2px] bg-white w-[20px] h-[20px] rounded-full shadow-sm transform transition-transform duration-300
                                ${isEnabled ? 'translate-x-[20px]' : 'translate-x-0'}
                            `}
                        />
                    </div>
                </div>
            </div>

            {/* Active Sessions Section - Only show if there are running sessions */}
            {runningSession && (
                <div className="space-y-3">
                    <div className="flex items-center gap-2 px-1">
                        <ListTodo size={16} className="text-zinc-500" />
                        <h2 className="text-sm font-medium text-textMuted">Active Session</h2>
                    </div>

                    {/* Active Session Card */}
                    <div
                        onClick={() => onSelectSession?.(runningSession)}
                        className="bg-surface rounded-xl p-4 border border-white/5 hover:border-white/10 transition-all cursor-pointer group shadow-sm"
                    >
                        <div className="flex items-start justify-between mb-3">
                            <div className="flex items-center gap-3 min-w-0 flex-1">
                                <div className={`p-1.5 rounded-lg border flex-shrink-0 ${runningSession.state === 'IN_PROGRESS' || runningSession.state === 'PLANNING'
                                    ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
                                    : runningSession.state === 'AWAITING_PLAN_APPROVAL' || runningSession.state === 'AWAITING_USER_FEEDBACK'
                                        ? 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                                        : 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20'
                                    }`}>
                                    <Rocket size={16} />
                                </div>
                                <span className="text-sm font-medium text-zinc-200 truncate">{runningSession.title || 'Untitled Session'}</span>
                            </div>
                            <button className="text-zinc-500 hover:text-white transition-colors flex-shrink-0">
                                <MoreHorizontal size={16} />
                            </button>
                        </div>

                        <div className="flex items-center gap-4 pl-1">
                            <div className="flex items-center gap-2 text-xs text-zinc-400">
                                <Circle size={8} className={`${runningSession.state === 'IN_PROGRESS' || runningSession.state === 'PLANNING'
                                    ? 'fill-indigo-400 text-indigo-400 animate-pulse'
                                    : runningSession.state === 'AWAITING_PLAN_APPROVAL' || runningSession.state === 'AWAITING_USER_FEEDBACK'
                                        ? 'fill-amber-400 text-amber-400'
                                        : 'fill-zinc-500 text-zinc-500'
                                    }`} />
                                <span className="font-medium">{getSessionStatusText(runningSession.state)}</span>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Schedule Section */}
            <div className="space-y-3">
                <div className="flex items-center gap-2 px-1">
                    <Clock size={16} className="text-zinc-500" />
                    <h2 className="text-sm font-medium text-textMuted">Schedule</h2>
                </div>

                <div className="flex flex-wrap items-center gap-2 w-full">
                    <CategoryPill icon={<Zap size={14} />} label="Performance" />
                    <CategoryPill icon={<Palette size={14} />} label="Design" />
                    <CategoryPill icon={<ShieldCheck size={14} />} label="Security" />
                </div>
            </div>
        </div>
    );
};

const TabButton: React.FC<{ label: string; isActive: boolean; onClick: () => void }> = ({ label, isActive, onClick }) => (
    <button
        onClick={onClick}
        className={`
            px-4 py-1.5 rounded-full text-xs font-medium transition-all whitespace-nowrap flex-shrink-0
            ${isActive
                ? 'bg-surfaceHighlight text-white border border-white/10 shadow-sm'
                : 'text-zinc-500 hover:text-zinc-300 hover:bg-white/5'}
        `}
    >
        {label}
    </button>
);

const CategoryPill: React.FC<{ icon: React.ReactNode; label: string }> = ({ icon, label }) => (
    <button className="
        flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-medium border transition-all whitespace-nowrap
        bg-surface text-zinc-400 border-white/5 hover:bg-surfaceHighlight hover:text-zinc-200 hover:border-white/10 active:scale-95
    ">
        {icon}
        <span>{label}</span>
    </button>
);