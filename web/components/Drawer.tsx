import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { NavLink, Link } from 'react-router-dom';
import { X, Search, ChevronDown, ChevronRight, MoreHorizontal, Github, FileText, CheckCircle2, Disc, ArrowUp, Loader2, Clock, MessageCircle, Pause, XCircle, AlertCircle, Lock, Trash2, Settings } from 'lucide-react';
import { JulesSession, JulesSource, formatRelativeTime } from '../types';
import { sortSessions, getSessionDisplayInfo } from '../utils/session';
import { List, ListImperativeAPI } from 'react-window';
import { createPortal } from 'react-dom';

interface DrawerProps {
    isOpen: boolean;
    onClose: () => void;
    sessions: JulesSession[];
    sources: JulesSource[];
    currentSessionId?: string;
    currentSourceId?: string;
    onSelectSession: (session: JulesSession) => void;
    onSelectSource: (source: JulesSource) => void;
    onDeleteSession?: (sessionName: string) => void;
    onUpdateSession?: (sessionName: string, updates: Partial<JulesSession>, updateMask: string[]) => void;
    sessionsUsed?: number;
    dailyLimit?: number;
}

type DrawerItemType =
    | { type: 'session-header' }
    | { type: 'session-empty' }
    | { type: 'session'; data: JulesSession }
    | { type: 'source-header' }
    | { type: 'source-empty' }
    | { type: 'source'; data: JulesSource };

// Portal for Context Menu to avoid clipping
const MenuPortal = ({ children }: { children: React.ReactNode }) => {
    return createPortal(children, document.body);
};

// Minimal AutoSizer implementation to ensure reliability
const AutoSizer = ({ children }: { children: (size: { height: number, width: number }) => React.ReactNode }) => {
    const ref = useRef<HTMLDivElement>(null);
    const [size, setSize] = useState({ height: 0, width: 0 });

    useEffect(() => {
        if (!ref.current) return;

        // Initial size
        setSize({
            height: ref.current.clientHeight,
            width: ref.current.clientWidth
        });

        const observer = new ResizeObserver((entries) => {
            const entry = entries[0];
            if (entry) {
                setSize({
                    height: entry.contentRect.height,
                    width: entry.contentRect.width
                });
            }
        });
        observer.observe(ref.current);
        return () => observer.disconnect();
    }, []);

    return (
        <div ref={ref} style={{ height: '100%', width: '100%', overflow: 'hidden' }}>
            {size.height > 0 && size.width > 0 && children(size)}
        </div>
    );
};

interface DrawerItemRowProps {
    index: number;
    style: React.CSSProperties;
    items: DrawerItemType[];
    isSessionsOpen: boolean;
    toggleSessions: () => void;
    isCodebasesOpen: boolean;
    toggleCodebases: () => void;
    searchQuery: string;
    filteredSessionsCount: number;
    filteredSourcesCount: number;
    menuOpenId: string | null;
    setMenuOpenId: (id: string | null) => void;
    onClose: () => void;
    onUpdateSession?: (name: string, updates: Partial<JulesSession>, mask: string[]) => void;
    onDeleteSession?: (name: string) => void;
}

const DrawerItemRow = ({
    index,
    style,
    items,
    isSessionsOpen, toggleSessions,
    isCodebasesOpen, toggleCodebases,
    searchQuery, filteredSessionsCount, filteredSourcesCount,
    menuOpenId, setMenuOpenId,
    onClose, onUpdateSession, onDeleteSession
}: DrawerItemRowProps) => {
    const item = items[index];

    if (item.type === 'session-header') {
        return (
            <div style={style} className="px-2">
                 <button
                    onClick={toggleSessions}
                    aria-expanded={isSessionsOpen}
                    className="w-full flex items-center justify-between px-3 py-2 text-zinc-400 hover:text-white transition-colors group"
                >
                    <span className="text-sm font-medium group-hover:text-zinc-200">Recent sessions</span>
                    <div className="flex items-center gap-2">
                        {searchQuery && filteredSessionsCount > 0 && (
                            <span className="text-[10px] bg-white/10 px-1.5 rounded text-zinc-300">{filteredSessionsCount}</span>
                        )}
                        {isSessionsOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </div>
                </button>
            </div>
        );
    }

    if (item.type === 'session-empty') {
        return (
             <div style={style} className="px-6 py-2 text-xs text-zinc-600 italic">
                {searchQuery ? 'No matching sessions' : 'No recent sessions'}
            </div>
        );
    }

    if (item.type === 'session') {
        const session = item.data;
        const displayInfo = getSessionDisplayInfo(session.state);

        return (
            <div style={style} className="px-2">
                <NavLink
                    to={`/session/${session.name.replace('sessions/', '')}`}
                    onClick={onClose}
                    className={({ isActive }) => `
                        w-full flex items-start gap-3 px-3 py-2.5 rounded-lg transition-colors group
                        ${isActive ? 'bg-white/5' : 'hover:bg-white/5'}
                    `}
                >
                    {({ isActive }) => (
                        <>
                            <div className={`mt-0.5 text-base ${displayInfo.shimmer ? 'animate-pulse' : ''}`}>
                                {displayInfo.emoji}
                            </div>
                            <div className="flex flex-col min-w-0 flex-1 gap-0.5">
                                <span className={`text-sm truncate text-left font-light ${isActive ? 'text-white' : 'text-zinc-300'}`}>
                                    {session.title || session.prompt}
                                </span>
                                <div className="flex flex-col">
                                    <span className={`text-[10px] font-medium ${isActive ? 'text-indigo-300' : 'text-zinc-400'}`}>
                                        {displayInfo.label}
                                    </span>
                                    <span className="text-[10px] text-zinc-600 truncate">
                                        {displayInfo.helperText}
                                    </span>
                                    {displayInfo.cta !== 'none' && (
                                        <span className="text-[10px] text-indigo-400 font-bold mt-0.5 uppercase tracking-wide">
                                            {displayInfo.cta} →
                                        </span>
                                    )}
                                </div>
                            </div>

                            <div className="relative self-center"
                                 onClick={(e) => {
                                     e.preventDefault();
                                     e.stopPropagation();
                                 }}>
                                <button
                                    id={`menu-trigger-${session.name}`}
                                    onClick={(e) => {
                                        e.preventDefault();
                                        e.stopPropagation();
                                        setMenuOpenId(menuOpenId === session.name ? null : session.name);
                                    }}
                                    aria-label={`Session options for ${session.title || session.prompt}`}
                                    aria-haspopup="true"
                                    aria-expanded={menuOpenId === session.name}
                                    className={`p-1.5 rounded-md transition-all ${menuOpenId === session.name ? 'opacity-100 bg-white/10 text-white' : 'opacity-0 group-hover:opacity-100 text-zinc-500 hover:text-white hover:bg-white/10'}`}
                                >
                                    <MoreHorizontal size={16} />
                                </button>

                                {menuOpenId === session.name && (
                                    <MenuPortal>
                                        <div
                                            className="fixed inset-0 z-[60] bg-transparent"
                                            onClick={(e) => {
                                                e.preventDefault();
                                                e.stopPropagation();
                                                setMenuOpenId(null);
                                            }}
                                        />
                                        <div
                                            role="menu"
                                            className="fixed z-[61] w-32 bg-surface border border-white/10 rounded-lg shadow-xl overflow-hidden py-1"
                                            style={{
                                                top: document.getElementById(`menu-trigger-${session.name}`)?.getBoundingClientRect().bottom ?? 0,
                                                left: (document.getElementById(`menu-trigger-${session.name}`)?.getBoundingClientRect().left ?? 0) - 100
                                            }}
                                        >
                                            {(session.state === 'IN_PROGRESS' || session.state === 'PLANNING') && (
                                                <button
                                                    role="menuitem"
                                                    onClick={(e) => {
                                                        e.preventDefault();
                                                        e.stopPropagation();
                                                        onUpdateSession?.(session.name, { state: 'PAUSED' }, ['state']);
                                                        setMenuOpenId(null);
                                                    }}
                                                    className="w-full text-left px-3 py-2 text-xs text-zinc-300 hover:text-white hover:bg-white/5 flex items-center gap-2"
                                                >
                                                    <Pause size={12} /> Pause
                                                </button>
                                            )}

                                            {session.state === 'PAUSED' && (
                                                <button
                                                    role="menuitem"
                                                    onClick={(e) => {
                                                        e.preventDefault();
                                                        e.stopPropagation();
                                                        onUpdateSession?.(session.name, { state: 'IN_PROGRESS' }, ['state']);
                                                        setMenuOpenId(null);
                                                    }}
                                                    className="w-full text-left px-3 py-2 text-xs text-zinc-300 hover:text-white hover:bg-white/5 flex items-center gap-2"
                                                >
                                                    <Loader2 size={12} /> Resume
                                                </button>
                                            )}

                                            <button
                                                role="menuitem"
                                                onClick={(e) => {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    if (window.confirm('Are you sure you want to delete this session?')) {
                                                        onDeleteSession?.(session.name);
                                                    }
                                                    setMenuOpenId(null);
                                                }}
                                                className="w-full text-left px-3 py-2 text-xs text-red-400 hover:text-red-300 hover:bg-red-500/10 flex items-center gap-2"
                                            >
                                                <Trash2 size={12} /> Delete
                                            </button>
                                        </div>
                                    </MenuPortal>
                                )}
                            </div>
                        </>
                    )}
                </NavLink>
            </div>
        );
    }

    if (item.type === 'source-header') {
        return (
            <div style={style} className="px-2">
                 <button
                    onClick={toggleCodebases}
                    aria-expanded={isCodebasesOpen}
                    className="w-full flex items-center justify-between px-3 py-2 text-zinc-400 hover:text-white transition-colors group"
                >
                    <span className="text-sm font-medium group-hover:text-zinc-200">Repositories</span>
                    <div className="flex items-center gap-2">
                        {searchQuery && filteredSourcesCount > 0 && (
                            <span className="text-[10px] bg-white/10 px-1.5 rounded text-zinc-300">{filteredSourcesCount}</span>
                        )}
                        {isCodebasesOpen ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </div>
                </button>
            </div>
        );
    }

    if (item.type === 'source-empty') {
        return (
            <div style={style} className="px-6 py-2 text-xs text-zinc-600 italic">
                {searchQuery ? 'No matching repositories' : 'No repositories connected'}
            </div>
        );
    }

    if (item.type === 'source') {
        const source = item.data;
        return (
            <div style={style} className="px-2">
                <NavLink
                    to={`/repository/${source.name.replace('sources/', '')}`}
                    onClick={onClose}
                    className={({ isActive }) => `
                        w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors group
                        ${isActive ? 'bg-white/5' : 'hover:bg-white/5'}
                    `}
                >
                    {({ isActive }) => (
                        <>
                            <Github size={16} className={`${isActive ? 'text-white' : 'text-zinc-400'} flex-shrink-0`} />
                            <div className="flex flex-col items-start flex-1 min-w-0">
                                <div className="flex items-center gap-1.5 w-full">
                                    <span className={`text-sm truncate ${isActive ? 'text-white font-medium' : 'text-zinc-400'}`}>
                                        {source.displayName || source.name.split('/').slice(-2).join('/')}
                                    </span>
                                    {source.githubRepo?.isPrivate && (
                                        <Lock size={10} className="text-zinc-500 flex-shrink-0" />
                                    )}
                                </div>
                                {source.githubRepo?.defaultBranch?.displayName && (
                                    <span className="text-[10px] text-zinc-600 font-mono">
                                        {source.githubRepo.defaultBranch.displayName}
                                    </span>
                                )}
                            </div>
                            {isActive && (
                                <div className="w-5 h-5 rounded-full bg-indigo-500/20 text-indigo-300 flex items-center justify-center text-[10px] font-bold">
                                    1
                                </div>
                            )}
                        </>
                    )}
                </NavLink>
            </div>
        );
    }

    return null;
};

type RowDataProps = Omit<DrawerItemRowProps, 'index' | 'style'>;

export const Drawer: React.FC<DrawerProps> = ({
    isOpen,
    onClose,
    sessions,
    sources,
    currentSessionId,
    currentSourceId,
    onSelectSession,
    onSelectSource,
    onDeleteSession,
    onUpdateSession,
    sessionsUsed = 0,
    dailyLimit = 100
}) => {
    const [isSessionsOpen, setIsSessionsOpen] = useState(true);
    const [menuOpenId, setMenuOpenId] = useState<string | null>(null);
    const [isCodebasesOpen, setIsCodebasesOpen] = useState(true);
    const [searchQuery, setSearchQuery] = useState('');
    const listRef = useRef<ListImperativeAPI>(null);

    // Filter logic
    const filteredSessions = useMemo(() => sortSessions(sessions.filter(session => {
        const query = searchQuery.toLowerCase();
        return (session.title?.toLowerCase().includes(query) ||
            session.prompt?.toLowerCase().includes(query));
    })), [sessions, searchQuery]);

    const filteredSources = useMemo(() => sources.filter(source => {
        const query = searchQuery.toLowerCase();
        const displayName = source.displayName || source.name;
        return displayName.toLowerCase().includes(query);
    }), [sources, searchQuery]);

    // Auto-expand on search
    useEffect(() => {
        if (searchQuery) {
            setIsSessionsOpen(true);
            setIsCodebasesOpen(true);
        }
    }, [searchQuery]);

    // Construct flat list of items
    const drawerItems = useMemo<DrawerItemType[]>(() => {
        const items: DrawerItemType[] = [];

        // Sessions Section
        items.push({ type: 'session-header' });
        if (isSessionsOpen) {
            if (filteredSessions.length === 0) {
                items.push({ type: 'session-empty' });
            } else {
                filteredSessions.forEach(session => {
                    items.push({ type: 'session', data: session });
                });
            }
        }

        // Sources Section
        items.push({ type: 'source-header' });
        if (isCodebasesOpen) {
             if (filteredSources.length === 0) {
                items.push({ type: 'source-empty' });
            } else {
                filteredSources.forEach(source => {
                    items.push({ type: 'source', data: source });
                });
            }
        }

        return items;
    }, [isSessionsOpen, isCodebasesOpen, filteredSessions, filteredSources]);

    // Reset cache when items change size/order significantly
    // List in v2 handles updates automatically if props change.

    const getItemSize = useCallback((index: number) => {
        const item = drawerItems[index];
        switch (item.type) {
            case 'session-header': return 40;
            case 'session-empty': return 32;
            case 'session': return 76; // Increased slightly for spacing
            case 'source-header': return 40;
            case 'source-empty': return 32;
            case 'source': return 56;
            default: return 50;
        }
    }, [drawerItems]);


    const [isRendered, setIsRendered] = useState(false);
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setIsRendered(true);
            const timer = setTimeout(() => setIsVisible(true), 10);
            return () => clearTimeout(timer);
        } else {
            setIsVisible(false);
            const timer = setTimeout(() => setIsRendered(false), 300);
            return () => clearTimeout(timer);
        }
    }, [isOpen]);

    if (!isRendered) return null;

    return (
        <>
            {/* Overlay */}
            <div
                className={`fixed inset-0 bg-black/60 z-50 backdrop-blur-sm transition-opacity duration-300 ${isVisible ? 'opacity-100' : 'opacity-0'}`}
                onClick={onClose}
            />

            {/* Drawer */}
            <div className={`fixed inset-y-0 left-0 w-[320px] bg-background z-50 flex flex-col border-r border-white/5 transition-transform duration-300 ease-in-out ${isVisible ? 'translate-x-0' : '-translate-x-full'}`}>

                {/* Header */}
                <div className="flex items-center justify-between px-4 pb-4 pt-[calc(1rem+env(safe-area-inset-top))] border-b border-white/5">
                    <Link to="/" onClick={onClose} className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                        <img src="https://jules.google/squid.png" alt="Logo" className="w-6 h-6 object-contain opacity-80" />
                        <span className="font-semibold text-lg text-white tracking-tight">jules</span>
                    </Link>
                    <button onClick={onClose} aria-label="Close sidebar" className="p-1 hover:bg-white/5 rounded-lg text-zinc-400 hover:text-white transition-colors">
                        <X size={20} />
                    </button>
                </div>

                {/* Search */}
                <div className="p-4">
                    <div className="relative group">
                        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500 group-focus-within:text-indigo-400 transition-colors" />
                        <input
                            type="text"
                            placeholder="Search repositories & sessions..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-surface border border-white/10 rounded-xl py-2.5 pl-10 pr-4 text-sm text-white placeholder:text-zinc-600 focus:outline-none focus:border-indigo-500/50 focus:bg-surfaceHighlight transition-all"
                        />
                    </div>
                </div>

                {/* Content - Virtualized */}
                <div className="flex-1 px-0 pb-4 overflow-hidden">
                     <AutoSizer>
                        {({ height, width }: { height: number; width: number }) => (
                            <List<RowDataProps>
                                listRef={listRef}
                                className="custom-scrollbar"
                                style={{ height, width }}
                                rowCount={drawerItems.length}
                                rowHeight={getItemSize}
                                rowComponent={DrawerItemRow}
                                rowProps={{
                                    items: drawerItems,
                                    isSessionsOpen,
                                    toggleSessions: () => setIsSessionsOpen(prev => !prev),
                                    isCodebasesOpen,
                                    toggleCodebases: () => setIsCodebasesOpen(prev => !prev),
                                    searchQuery,
                                    filteredSessionsCount: filteredSessions.length,
                                    filteredSourcesCount: filteredSources.length,
                                    menuOpenId,
                                    setMenuOpenId,
                                    onClose,
                                    onUpdateSession,
                                    onDeleteSession
                                }}
                            />
                        )}
                    </AutoSizer>
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-white/5 bg-background">
                    <div className="mb-4">
                        <div className="flex justify-between items-end mb-2">
                            <span className="text-xs text-zinc-500">Daily session limit ({sessionsUsed}/{dailyLimit})</span>
                        </div>
                        <div className="h-1 bg-zinc-800 rounded-full overflow-hidden w-full">
                            <div
                                className="h-full bg-indigo-500 rounded-full transition-all duration-500"
                                style={{ width: `${Math.min(100, (sessionsUsed / dailyLimit) * 100)}%` }}
                            />
                        </div>
                    </div>

                    <div className="flex items-center gap-2">
                        <Link to="/settings" onClick={onClose} className="flex-1 flex items-center justify-center gap-2 bg-surface hover:bg-surfaceHighlight border border-white/5 py-2 rounded-lg text-sm text-zinc-300 transition-colors">
                            <Settings size={14} />
                            Settings
                        </Link>
                        <button aria-label="Documentation" className="w-10 h-10 flex items-center justify-center bg-surface hover:bg-surfaceHighlight border border-white/5 rounded-lg text-zinc-400 hover:text-white transition-colors" title="Documentation">
                            <FileText size={18} />
                        </button>
                        <button aria-label="Join Discord" className="w-10 h-10 flex items-center justify-center bg-surface hover:bg-surfaceHighlight border border-white/5 rounded-lg text-zinc-400 hover:text-white transition-colors">
                            <Disc size={18} />
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
};
