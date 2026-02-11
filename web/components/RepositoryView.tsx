import React, { useState, useMemo } from 'react';
import { JulesSource, JulesSession } from '../types';
import {
    CheckCircle2, Code2, Book, Plus, LayoutGrid,
    Terminal, Clock, Loader2, GitBranch, Github, Search,
    ArrowRight, Activity, Zap, Layers, ChevronRight,
    AlertCircle, Archive, Calendar
} from 'lucide-react';

interface RepositoryViewProps {
    source: JulesSource;
    sessions: JulesSession[];
    onSelectSession: (session: JulesSession) => void;
    onCreateNew: () => void;
}

export const RepositoryView: React.FC<RepositoryViewProps> = ({
    source,
    sessions,
    onSelectSession,
    onCreateNew
}) => {
    const [filter, setFilter] = useState<'all' | 'scheduled' | 'completed' | 'failed' | 'archived'>('all');
    const [activeTab, setActiveTab] = useState<'overview' | 'environment' | 'knowledge'>('overview');
    const [search, setSearch] = useState('');

    // Derived Stats
    const stats = useMemo(() => {
        const active = sessions.filter(s => ['PLANNING', 'IN_PROGRESS', 'AWAITING_PLAN_APPROVAL', 'AWAITING_USER_FEEDBACK', 'QUEUED'].includes(s.state));
        const completed = sessions.filter(s => s.state === 'COMPLETED');
        const failed = sessions.filter(s => s.state === 'FAILED');
        return {
            total: sessions.length,
            activeCount: active.length,
            completedCount: completed.length,
            failedCount: failed.length
        };
    }, [sessions]);

    // Filtering
    const displaySessions = useMemo(() => {
        let data = sessions;

        // Filter by Category
        if (filter === 'scheduled') {
            data = data.filter(s => s.state === 'QUEUED'); // Mapping Queued to Scheduled as per search context
        } else if (filter === 'completed') {
            data = data.filter(s => s.state === 'COMPLETED');
        } else if (filter === 'failed') {
            data = data.filter(s => s.state === 'FAILED');
        } else if (filter === 'archived') {
            // Placeholder: Filtering sessions older than 30 days as 'Archived'
            const thirtyDaysAgo = new Date();
            thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
            data = data.filter(s => new Date(s.createTime) < thirtyDaysAgo);
        }

        // Search
        if (search) {
            const q = search.toLowerCase();
            data = data.filter(s =>
                s.title?.toLowerCase().includes(q) ||
                s.prompt?.toLowerCase().includes(q)
            );
        }

        // Sort by Date (Newest first)
        return [...data].sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime());
    }, [sessions, filter, search]);

    return (
        <div className="flex-1 flex flex-col h-full bg-background overflow-y-auto animate-in fade-in duration-300">
            {/* Hero Header */}
            <div className="w-full bg-background border-b border-white/5 pt-6 md:pt-8 px-4 sm:px-8 pb-0">
                <div className="max-w-6xl mx-auto">
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-6 md:mb-8">
                        <div className="flex items-start gap-4 md:gap-5">
                            <div className="w-12 h-12 md:w-16 md:h-16 rounded-xl md:rounded-2xl bg-gradient-to-br from-indigo-500/10 to-purple-500/10 border border-white/10 flex items-center justify-center text-indigo-400 shadow-xl shadow-indigo-900/10 flex-shrink-0">
                                <Github className="w-6 h-6 md:w-8 md:h-8" />
                            </div>
                            <div className="min-w-0 flex-1">
                                <h1 className="text-2xl md:text-3xl font-bold text-white tracking-tight mb-2 truncate">
                                    {source.displayName || source.name.split('/').slice(-2).join('/')}
                                </h1>
                                <div className="flex flex-wrap items-center gap-2 md:gap-3 text-xs md:text-sm text-zinc-500">
                                    <div className="flex items-center gap-1.5 bg-white/5 px-2 md:px-2.5 py-1 rounded-md border border-white/5">
                                        <GitBranch size={12} className="text-zinc-400" />
                                        <span className="font-mono text-zinc-300 max-w-[100px] truncate">{source.githubRepo?.defaultBranch?.displayName || 'main'}</span>
                                    </div>
                                    <span className="flex items-center gap-1.5 px-1 font-medium">
                                        <div className="w-1.5 h-1.5 rounded-full bg-green-500/80 shadow-[0_0_8px_rgba(34,197,94,0.4)]" />
                                        <span className="hidden sm:inline">Repo Synced</span>
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div className="flex items-center gap-3 w-full md:w-auto">
                            <button
                                onClick={onCreateNew}
                                className="flex-1 md:flex-none group flex items-center justify-center gap-2 bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2.5 rounded-xl font-medium transition-all shadow-lg shadow-indigo-500/20 active:scale-95"
                            >
                                <Plus size={18} className="group-hover:rotate-90 transition-transform duration-300" />
                                <span>New Session</span>
                            </button>
                        </div>
                    </div>

                    {/* Tab Navigation - Scrollable on mobile */}
                    <div className="flex items-center gap-1 overflow-x-auto no-scrollbar -mx-4 px-4 md:mx-0 md:px-0">
                        <TabItem
                            label="Overview"
                            icon={<LayoutGrid size={16} />}
                            isActive={activeTab === 'overview'}
                            onClick={() => setActiveTab('overview')}
                        />
                        <TabItem
                            label="Environment"
                            icon={<Code2 size={16} />}
                            isActive={activeTab === 'environment'}
                            onClick={() => setActiveTab('environment')}
                        />
                        <TabItem
                            label="Knowledge"
                            icon={<Book size={16} />}
                            isActive={activeTab === 'knowledge'}
                            onClick={() => setActiveTab('knowledge')}
                        />
                    </div>
                </div>
            </div>

            {/* Main Content Area */}
            <div className="flex-1 w-full max-w-6xl mx-auto px-4 sm:px-8 py-6 md:py-8">

                {activeTab === 'overview' && (
                    <div className="space-y-6 md:space-y-8 animate-in slide-in-from-bottom-2 duration-300">
                        {/* Stats Grid - 2 cols on mobile, 3 on desktop */}
                        <div className="grid grid-cols-2 lg:grid-cols-3 gap-3 md:gap-4">
                            <StatCard
                                label="Active"
                                value={stats.activeCount}
                                icon={<Activity size={18} />}
                                color="text-indigo-400"
                                bg="bg-indigo-500/10"
                                border="border-indigo-500/20"
                            />
                            <StatCard
                                label="Completed"
                                value={stats.completedCount}
                                icon={<CheckCircle2 size={18} />}
                                color="text-green-400"
                                bg="bg-green-500/10"
                                border="border-green-500/20"
                            />
                            <StatCard
                                label="Failed"
                                value={stats.failedCount}
                                icon={<AlertCircle size={18} />}
                                color="text-red-400"
                                bg="bg-red-500/10"
                                border="border-red-500/20"
                            />
                        </div>

                        {/* Recent Sessions List */}
                        <div className="flex flex-col gap-4">
                            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                                <h2 className="text-lg md:text-xl font-semibold text-white">History</h2>

                                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                                    {/* Search */}
                                    <div className="relative flex-1">
                                        <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
                                        <input
                                            type="text"
                                            placeholder="Search tasks..."
                                            value={search}
                                            onChange={(e) => setSearch(e.target.value)}
                                            className="w-full sm:w-48 bg-surface border border-white/10 rounded-lg pl-9 pr-4 py-2 md:py-1.5 text-sm text-white placeholder:text-zinc-600 focus:outline-none focus:border-indigo-500/50 transition-all font-light"
                                        />
                                    </div>

                                    {/* Detailed Filters (As requested) */}
                                    <div className="bg-surface p-1 rounded-lg border border-white/10 flex items-center overflow-x-auto no-scrollbar">
                                        <FilterTab label="All" active={filter === 'all'} onClick={() => setFilter('all')} />
                                        <FilterTab label="Scheduled" active={filter === 'scheduled'} onClick={() => setFilter('scheduled')} />
                                        <FilterTab label="Completed" active={filter === 'completed'} onClick={() => setFilter('completed')} />
                                        <FilterTab label="Failed" active={filter === 'failed'} onClick={() => setFilter('failed')} />
                                        <FilterTab label="Archived" active={filter === 'archived'} onClick={() => setFilter('archived')} />
                                    </div>
                                </div>
                            </div>

                            {displaySessions.length === 0 ? (
                                <div className="h-64 flex flex-col items-center justify-center bg-background border border-dashed border-white/10 rounded-2xl p-4 text-center">
                                    <div className="w-12 h-12 rounded-xl bg-white/5 flex items-center justify-center text-zinc-500 mb-4">
                                        {filter === 'all' ? <Terminal size={24} /> : <Search size={24} />}
                                    </div>
                                    <p className="text-zinc-400 font-medium capitalize">{filter} sessions empty</p>
                                    <p className="text-zinc-600 text-sm mt-1">No tasks match your current selection</p>
                                    {filter !== 'all' && (
                                        <button
                                            onClick={() => setFilter('all')}
                                            className="mt-4 text-indigo-400 hover:text-indigo-300 text-sm font-medium transition-colors"
                                        >
                                            View all sessions
                                        </button>
                                    )}
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 gap-3">
                                    {displaySessions.map(session => (
                                        <SessionListItem
                                            key={session.name}
                                            session={session}
                                            onClick={() => onSelectSession(session)}
                                        />
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Other tabs remain same but with minor mobile tweaks */}
                {activeTab === 'environment' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6 animate-in slide-in-from-bottom-2 duration-300">
                        <div className="bg-background border border-white/5 rounded-2xl p-5 md:p-6 shadow-sm">
                            <div className="flex items-center gap-3 mb-6">
                                <div className="p-2 bg-blue-500/10 rounded-lg text-blue-400">
                                    <Code2 size={20} />
                                </div>
                                <h3 className="text-lg font-medium text-white">Project Structure</h3>
                            </div>
                            <div className="space-y-4">
                                <InfoRow label="Detected Language" value="TypeScript / React" />
                                <InfoRow label="Package Manager" value="npm" />
                                <InfoRow label="Build System" value="Vite" />
                                <InfoRow label="Framework" value="Tailwind CSS" />
                            </div>
                        </div>

                        <div className="bg-background border border-white/5 rounded-2xl p-5 md:p-6 shadow-sm">
                            <div className="flex items-center gap-3 mb-6">
                                <div className="p-2 bg-purple-500/10 rounded-lg text-purple-400">
                                    <Zap size={20} />
                                </div>
                                <h3 className="text-lg font-medium text-white">Capabilties</h3>
                            </div>
                            <div className="space-y-3">
                                <CapabilityItem label="Read Files" enabled />
                                <CapabilityItem label="Run Terminal Commands" enabled />
                                <CapabilityItem label="Create Pull Requests" enabled />
                                <CapabilityItem label="Deploy Preview" enabled={false} />
                            </div>
                        </div>
                    </div>
                )}

                {activeTab === 'knowledge' && (
                    <div className="flex flex-col items-center justify-center py-20 bg-background border border-white/5 rounded-2xl border-dashed animate-in slide-in-from-bottom-2 duration-300 p-4 text-center">
                        <div className="w-16 h-16 rounded-2xl bg-surface flex items-center justify-center text-zinc-500 mb-6 shadow-inner">
                            <Book size={32} />
                        </div>
                        <h3 className="text-xl font-medium text-white mb-2">Knowledge Base</h3>
                        <p className="text-zinc-500 text-center max-w-md mb-8 leading-relaxed">
                            Documentation and indexing status for this repository will appear here.
                            Jules is currently indexing your codebase.
                        </p>
                        <div className="flex items-center gap-2 text-xs text-zinc-600 bg-white/5 px-4 py-2 rounded-full border border-white/5">
                            <Loader2 size={12} className="animate-spin" />
                            Indexing in progress...
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

// --- Sub Components ---

const TabItem = ({ label, icon, isActive, onClick }: any) => (
    <button
        onClick={onClick}
        className={`
            flex items-center gap-2 px-4 md:px-6 py-3 md:py-4 text-sm font-medium border-b-2 transition-all whitespace-nowrap
            ${isActive
                ? 'border-indigo-500 text-white'
                : 'border-transparent text-zinc-500 hover:text-zinc-300 hover:bg-white/5'}
        `}
    >
        {icon}
        {label}
    </button>
);

const StatCard = ({ label, value, icon, color, bg, border }: any) => (
    <div className={`flex flex-col p-4 md:p-5 rounded-2xl bg-background border border-white/5 relative overflow-hidden group hover:border-white/10 transition-colors shadow-sm`}>
        <div className="flex justify-between items-start mb-3 md:mb-4 relative z-10">
            <span className="text-zinc-500 text-xs md:text-sm font-medium">{label}</span>
            <div className={`p-1.5 md:p-2 rounded-lg ${bg} ${color} ${border} border shadow-sm`}>
                {icon}
            </div>
        </div>
        <div className="text-2xl md:text-3xl font-bold text-white relative z-10">{value}</div>

        {/* Glow Effect */}
        <div className={`absolute -right-4 -bottom-4 w-16 h-16 md:w-24 md:h-24 ${bg} blur-2xl opacity-50 group-hover:opacity-75 transition-opacity`} />
    </div>
);

const FilterTab = ({ label, active, onClick }: any) => (
    <button
        onClick={onClick}
        className={`
            flex-1 sm:flex-none px-3 py-1.5 text-xs font-medium rounded-md transition-all text-center whitespace-nowrap mx-0.5
            ${active
                ? 'bg-surfaceHighlight text-white shadow-md'
                : 'text-zinc-500 hover:text-zinc-300'}
        `}
    >
        {label}
    </button>
);

const SessionListItem = ({ session, onClick }: { session: JulesSession, onClick: () => void }) => {
    const isScheduled = session.state === 'QUEUED';
    const isFailed = session.state === 'FAILED';
    const isCompleted = session.state === 'COMPLETED';

    return (
        <button
            onClick={onClick}
            className="w-full text-left group flex flex-col sm:flex-row sm:items-center justify-between p-3 md:p-4 bg-background hover:bg-surface border border-white/5 hover:border-indigo-500/30 rounded-xl transition-all shadow-sm gap-3 sm:gap-4 relative overflow-hidden"
        >
            <div className="flex items-start gap-3 sm:gap-4 overflow-hidden relative z-10">
                <div className={`
                    w-9 h-9 md:w-10 md:h-10 rounded-lg md:rounded-xl flex-shrink-0 flex items-center justify-center border
                    ${isScheduled ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                        isFailed ? 'bg-red-500/10 text-red-100 border-red-500/20' :
                            isCompleted ? 'bg-green-500/10 text-green-400 border-green-500/20' :
                                'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'}
                `}>
                    {isScheduled ? <Calendar size={16} /> : <Terminal size={16} className="md:w-[18px] md:h-[18px]" />}
                </div>

                <div className="flex flex-col min-w-0">
                    <h4 className="text-sm font-medium text-zinc-200 group-hover:text-white truncate pr-4 transition-colors">
                        {session.title || session.prompt || "Untitled Session"}
                    </h4>
                    <div className="flex items-center gap-2 text-xs text-zinc-500 mt-0.5 md:mt-1 font-light">
                        <span className="font-mono bg-white/5 px-1.5 py-0.5 rounded text-[10px] text-zinc-400">
                            {session.name.slice(-4)}
                        </span>
                        <span>•</span>
                        <Clock size={11} className="md:w-3 md:h-3" />
                        <span>{new Date(session.createTime).toLocaleDateString()}</span>
                    </div>
                </div>
            </div>

            <div className="flex items-center justify-between w-full sm:w-auto pl-12 sm:pl-0 sm:justify-end gap-3 md:gap-6 relative z-10">
                <StatusBadge state={session.state} />
                <ChevronRight className="text-zinc-600 group-hover:text-zinc-400 transition-colors hidden sm:block" size={16} />
            </div>

            {/* Subtle Gradient for Failed items */}
            {isFailed && <div className="absolute inset-0 bg-red-500/5 pointer-events-none" />}

        </button>
    );
};

const StatusBadge = ({ state }: { state: string }) => {
    let color = 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20';
    let label = state.replace(/_/g, ' ');

    if (state === 'COMPLETED') {
        color = 'bg-green-500/10 text-green-400 border-green-500/20';
        label = 'Completed';
    } else if (['PLANNING', 'IN_PROGRESS'].includes(state)) {
        color = 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20';
        label = 'In Progress';
    } else if (state === 'FAILED') {
        color = 'bg-red-500/10 text-red-200 border-red-500/20';
        label = 'Failed';
    } else if (state.includes('AWAITING')) {
        color = 'bg-amber-500/10 text-amber-400 border-amber-500/20';
        label = 'Action Needeed';
    } else if (state === 'PAUSED') {
        color = 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20';
        label = 'Paused';
    } else if (state === 'QUEUED') {
        color = 'bg-zinc-500/10 text-zinc-500 border-zinc-500/20';
        label = 'Scheduled';
    }

    return (
        <span className={`px-2.5 py-0.5 md:py-1 rounded-full text-[10px] font-bold border ${color} uppercase tracking-[0.05em] whitespace-nowrap`}>
            {label}
        </span>
    );
};

const InfoRow = ({ label, value }: any) => (
    <div className="flex justify-between items-center py-2 border-b border-white/5 last:border-0 relative font-light">
        <span className="text-sm text-zinc-500">{label}</span>
        <span className="text-sm text-zinc-300 font-mono tracking-tight">{value}</span>
    </div>
);

const CapabilityItem = ({ label, enabled }: any) => (
    <div className="flex items-center justify-between p-2 pl-0">
        <span className={`text-sm font-light ${enabled ? 'text-zinc-300' : 'text-zinc-600'}`}>{label}</span>
        {enabled ? (
            <CheckCircle2 size={16} className="text-green-500" />
        ) : (
            <Archive size={16} className="text-zinc-700" />
        )}
    </div>
);