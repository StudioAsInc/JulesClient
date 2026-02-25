import React, { useState, useRef, useEffect, memo, useMemo } from 'react';
import { Plus, Rocket, ArrowRight, Check, Clock, MessageSquare, FileSearch, Search, GitBranch, ChevronDown, Type, Settings2 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { JulesSource, AutomationMode } from '../types';
import { twMerge } from 'tailwind-merge';
import { useViewportAwarePosition } from '../hooks/useViewportAwarePosition';
import InputTextarea from './InputTextarea';

export type SessionMode = 'SCHEDULED' | 'INTERACTIVE' | 'REVIEW' | 'START';

export interface SessionCreateOptions {
    mode: SessionMode;
    branch?: string;
    title?: string;
    automationMode?: AutomationMode;
}

interface InputAreaProps {
    onSendMessage: (text: string, options: SessionCreateOptions) => void;
    isLoading: boolean;
    variant?: 'default' | 'chat';
    placeholder?: string;
    currentSource?: JulesSource | null;
}

const BRANCH_BUTTON_MAX_WIDTH = '120px'; // For styles

// Memoized to prevent re-renders when parent (SessionView/App) updates (e.g. streaming tokens)
export const InputArea: React.FC<InputAreaProps> = memo(({
    onSendMessage,
    isLoading,
    variant = 'default',
    placeholder,
    currentSource
}) => {
    const [input, setInput] = useState('');
    const [isFocused, setIsFocused] = useState(false);

    // Modes
    const [selectedMode, setSelectedMode] = useState<SessionMode>('START');
    const [isModeMenuOpen, setIsModeMenuOpen] = useState(false);

    // Branches
    const [isBranchMenuOpen, setIsBranchMenuOpen] = useState(false);
    const [branchSearch, setBranchSearch] = useState('');
    const [selectedBranch, setSelectedBranch] = useState<string>('main');

    // Session title (optional)
    const [sessionTitle, setSessionTitle] = useState('');

    // Automation mode
    const [automationMode, setAutomationMode] = useState<AutomationMode>('AUTO_CREATE_PR');

    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const containerRef = useRef<HTMLDivElement>(null);

    // Positioning Refs
    const branchTriggerRef = useRef<HTMLDivElement>(null);
    const branchMenuRef = useRef<HTMLDivElement>(null);
    const modeTriggerRef = useRef<HTMLDivElement>(null);
    const modeMenuRef = useRef<HTMLDivElement>(null);

    // Viewport Aware Positioning
    const branchMenuPos = useViewportAwarePosition(branchTriggerRef, branchMenuRef, isBranchMenuOpen);
    const modeMenuPos = useViewportAwarePosition(modeTriggerRef, modeMenuRef, isModeMenuOpen);

    // Initialize selected branch
    useEffect(() => {
        if (currentSource?.githubRepo?.defaultBranch?.displayName) {
            setSelectedBranch(currentSource.githubRepo.defaultBranch.displayName);
        } else {
            setSelectedBranch('main');
        }
    }, [currentSource]);

    const handleSubmit = () => {
        if (!input.trim() || isLoading) return;
        onSendMessage(input, {
            mode: selectedMode,
            branch: selectedBranch,
            title: sessionTitle.trim() || undefined,
            automationMode
        });
        setInput('');
        setSessionTitle('');
        if (textareaRef.current) {
            textareaRef.current.style.height = 'auto';
            if (variant === 'default') {
                setIsFocused(false);
                textareaRef.current.blur();
            }
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit();
        }
    };

    // Click outside handlers
    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            // Check if clicking outside container to close focus
            if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
                if (!input.trim()) {
                    setIsFocused(false);
                }
            }

            // Close menus if clicking outside
            const target = event.target as Element;

            // Branch Menu
            if (isBranchMenuOpen &&
                branchTriggerRef.current && !branchTriggerRef.current.contains(target as Node) &&
                branchMenuRef.current && !branchMenuRef.current.contains(target as Node)) {
                setIsBranchMenuOpen(false);
            }

            // Mode Menu
            if (isModeMenuOpen &&
                modeTriggerRef.current && !modeTriggerRef.current.contains(target as Node) &&
                modeMenuRef.current && !modeMenuRef.current.contains(target as Node)) {
                setIsModeMenuOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, [input, isBranchMenuOpen, isModeMenuOpen]);

    // Optimization: Memoize filtering to prevent re-calculation on every 3.5s placeholder cycle
    const filteredBranches = useMemo(() => {
        const branches = currentSource?.githubRepo?.branches || [];
        return branches.filter(b =>
            b.displayName.toLowerCase().includes(branchSearch.toLowerCase())
        );
    }, [currentSource?.githubRepo?.branches, branchSearch]);

    const isExpanded = isFocused || input.length > 0;

    // --- CHAT VARIANT (Compact floating bar) ---
    if (variant === 'chat') {
        return (
            <div className="w-full max-w-3xl mx-auto px-2 sm:px-4">
                <motion.div
                    layout
                    className={twMerge(
                        "relative flex items-end gap-2 bg-surface/95 backdrop-blur-xl border rounded-[26px] p-1.5 transition-all duration-200",
                        isFocused ? 'border-zinc-500/40 ring-1 ring-zinc-500/10 shadow-2xl' : 'border-white/10 shadow-lg'
                    )}
                >
                    <button
                        aria-label="Add attachment"
                        className="w-10 h-10 flex items-center justify-center text-zinc-400 hover:text-white transition-colors rounded-full hover:bg-white/5 flex-shrink-0"
                    >
                        <Plus size={20} />
                    </button>

                    <InputTextarea
                        ref={textareaRef}
                        variant={variant}
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        onFocus={() => setIsFocused(true)}
                        onBlur={() => setIsFocused(false)}
                        staticPlaceholder={placeholder}
                        shouldPausePlaceholder={isFocused || input.length > 0}
                        isExpanded={isExpanded}
                        isFocused={isFocused}
                    />

                    <motion.button
                        aria-label="Send message"
                        whileTap={{ scale: 0.95 }}
                        onClick={handleSubmit}
                        disabled={!input.trim() || isLoading}
                        className={twMerge(
                            "w-10 h-10 flex items-center justify-center rounded-full transition-all flex-shrink-0 duration-300",
                            input.trim()
                                ? 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-lg shadow-indigo-500/25'
                                : 'bg-surfaceHighlight text-zinc-500 cursor-not-allowed border border-white/5'
                        )}
                    >
                        {isLoading ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : <ArrowRight size={18} />}
                    </motion.button>
                </motion.div>
            </div>
        );
    }

    // --- DEFAULT VARIANT (Hero Card) ---
    return (
        <div
            ref={containerRef}
            className={twMerge(
                "relative w-full bg-surface border flex flex-col cursor-text transition-all duration-200 ease-out",
                isExpanded
                    ? 'rounded-xl min-h-[140px] border-indigo-500/40 shadow-[0_4px_30px_-4px_rgba(99,102,241,0.15)] ring-1 ring-indigo-500/20'
                    : 'rounded-xl min-h-[60px] border-white/10 shadow-sm hover:border-white/20 hover:bg-surfaceHighlight'
            )}
            onClick={() => {
                if (!isFocused) {
                    setIsFocused(true);
                    textareaRef.current?.focus();
                }
            }}
        >
            <div className={twMerge(
                "w-full transition-all duration-200 ease-out",
                isExpanded ? 'p-3 pb-2' : 'p-3 px-4'
            )}>
                <InputTextarea
                    ref={textareaRef}
                    aria-label="Message input"
                    variant={variant}
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    onFocus={() => setIsFocused(true)}
                    staticPlaceholder={placeholder}
                    shouldPausePlaceholder={isFocused || input.length > 0}
                    isExpanded={isExpanded}
                    isFocused={isFocused}
                />
            </div>

            {isExpanded && <div className="flex-1" />}

            {/* Footer Controls */}
            <div className={twMerge(
                "flex items-center justify-between pointer-events-auto transition-all duration-200 ease-out bg-surface/50 rounded-b-xl",
                isExpanded ? 'px-3 pb-3 pt-2 opacity-100 border-t border-white/5' : 'px-3 pb-3 opacity-100'
            )}>

                {/* Left: Attach, Branch, Settings */}
                <div className={twMerge(
                    "flex items-center gap-2 transition-all duration-200 ease-out",
                    isExpanded ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-2 pointer-events-none'
                )}>
                    {/* Plus Button: Square 1:1, h-6 w-6 */}
                    <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        aria-label="Add attachment"
                        className="w-6 h-6 aspect-square flex-shrink-0 flex items-center justify-center rounded-md bg-surfaceHighlight hover:bg-surface border border-white/10 text-zinc-400 hover:text-white transition-all duration-150"
                    >
                        <Plus size={14} />
                    </motion.button>

                    {/* Branch Selector Pill */}
                    <div ref={branchTriggerRef} className="relative branch-menu-trigger flex-shrink-0">
                        <motion.button
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsBranchMenuOpen(!isBranchMenuOpen);
                            }}
                            aria-label={`Select branch (current: ${selectedBranch})`}
                            aria-haspopup="true"
                            aria-expanded={isBranchMenuOpen}
                            style={{ maxWidth: BRANCH_BUTTON_MAX_WIDTH }}
                            className="flex items-center gap-1.5 px-2 bg-surfaceHighlight hover:bg-surface border border-white/10 rounded-lg text-[10px] font-mono text-zinc-300 hover:text-white transition-all duration-150 h-6"
                        >
                            <GitBranch size={11} className="text-indigo-400 flex-shrink-0" />
                            <span className="truncate">{selectedBranch}</span>
                            <ChevronDown size={10} className="text-zinc-500 flex-shrink-0" />
                        </motion.button>

                        <AnimatePresence>
                            {isBranchMenuOpen && (
                                <motion.div
                                    ref={branchMenuRef}
                                    initial={{ opacity: 0, y: -8 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0, y: -8 }}
                                    transition={{ duration: 0.15, ease: 'easeOut' }}
                                    onClick={(e) => e.stopPropagation()}
                                    style={{
                                        position: 'fixed',
                                        top: branchMenuPos.top,
                                        left: branchMenuPos.left,
                                        transformOrigin: branchMenuPos.transformOrigin
                                    }}
                                    className="branch-menu-dropdown w-[260px] max-w-[calc(100vw-2rem)] bg-surface border border-white/10 rounded-xl shadow-2xl z-50 overflow-hidden flex flex-col ring-1 ring-black/50"
                                    role="menu"
                                >
                                    <div className="p-2 border-b border-white/5 bg-background">
                                        <div className="flex items-center gap-2 bg-surfaceHighlight border border-white/5 rounded-lg px-2.5 py-1.5">
                                            <Search size={12} className="text-zinc-500" />
                                            <input
                                                type="text"
                                                aria-label="Filter branches"
                                                placeholder="Find a branch..."
                                                value={branchSearch}
                                                onChange={(e) => setBranchSearch(e.target.value)}
                                                className="bg-transparent border-none outline-none text-xs text-white placeholder:text-zinc-600 w-full font-mono"
                                            />
                                        </div>
                                    </div>
                                    <div className="max-h-[180px] overflow-y-auto p-1">
                                        {filteredBranches.map(branch => (
                                            <button
                                                key={branch.displayName}
                                                role="menuitemradio"
                                                aria-checked={selectedBranch === branch.displayName}
                                                onClick={() => {
                                                    setSelectedBranch(branch.displayName);
                                                    setIsBranchMenuOpen(false);
                                                }}
                                                className={twMerge(
                                                    "w-full flex items-center gap-2 px-3 py-2 text-xs font-mono text-left rounded-lg transition-colors min-h-[36px]",
                                                    selectedBranch === branch.displayName ? 'bg-indigo-500/10 text-indigo-300' : 'text-zinc-400 hover:bg-white/5 hover:text-white'
                                                )}
                                            >
                                                <div className={twMerge("w-1.5 h-1.5 rounded-full", selectedBranch === branch.displayName ? 'bg-indigo-400' : 'bg-transparent border border-zinc-600')} />
                                                <span className="truncate">{branch.displayName}</span>
                                                {selectedBranch === branch.displayName && <Check size={12} className="ml-auto" />}
                                            </button>
                                        ))}
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>

                    {/* Tools/Settings Button: Square 1:1, h-6 w-6, Center content */}
                    <div ref={modeTriggerRef} className="relative mode-menu-trigger flex-shrink-0">
                        <motion.button
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                            onClick={(e) => {
                                e.stopPropagation();
                                setIsModeMenuOpen(!isModeMenuOpen);
                            }}
                            aria-label="Session settings"
                            aria-haspopup="true"
                            aria-expanded={isModeMenuOpen}
                            className={twMerge(
                                "flex items-center justify-center rounded-lg transition-all duration-150 border h-6 w-6 aspect-square",
                                isModeMenuOpen || sessionTitle
                                    ? 'bg-surface text-white border-white/15'
                                    : 'bg-surfaceHighlight hover:bg-surface text-zinc-400 hover:text-white border-white/10'
                            )}
                            title="Session Settings"
                        >
                            <Settings2 size={12} className={sessionTitle ? 'text-indigo-400' : ''} />
                        </motion.button>

                        <AnimatePresence>
                            {isModeMenuOpen && (
                                <motion.div
                                    ref={modeMenuRef}
                                    initial={{ opacity: 0, scale: 0.95, y: -8 }}
                                    animate={{ opacity: 1, scale: 1, y: 0 }}
                                    exit={{ opacity: 0, scale: 0.95, y: -8 }}
                                    transition={{ duration: 0.15, ease: 'easeOut' }}
                                    onClick={(e) => e.stopPropagation()}
                                    style={{
                                        position: 'fixed',
                                        top: modeMenuPos.top,
                                        left: modeMenuPos.left,
                                        transformOrigin: modeMenuPos.transformOrigin
                                    }}
                                    className="mode-menu-dropdown w-[280px] bg-surface border border-white/10 rounded-xl shadow-2xl overflow-hidden z-50 ring-1 ring-black/80 flex flex-col"
                                    role="menu"
                                >
                                    {/* Section 1: Title Input */}
                                    <div className="p-3 border-b border-white/5">
                                        <label className="text-[10px] uppercase font-bold text-zinc-500 mb-1.5 block tracking-wider">Session Title</label>
                                        <div className="flex items-center gap-2 bg-surfaceHighlight border border-white/5 rounded-lg px-2.5 py-1.5 focus-within:border-indigo-500/30 transition-colors">
                                            <Type size={12} className="text-zinc-500" />
                                            <input
                                                type="text"
                                                value={sessionTitle}
                                                onChange={(e) => setSessionTitle(e.target.value)}
                                                placeholder="Optional title..."
                                                className="bg-transparent border-none outline-none text-xs text-white placeholder:text-zinc-600 w-full"
                                                autoFocus
                                            />
                                        </div>
                                    </div>

                                    {/* Section 2: Mode Selection */}
                                    <div className="p-1.5 space-y-0.5 max-h-[240px] overflow-y-auto">
                                        <div className="px-2 py-1.5">
                                            <label className="text-[10px] uppercase font-bold text-zinc-500 tracking-wider">Session Mode</label>
                                        </div>
                                        {['START', 'SCHEDULED', 'INTERACTIVE', 'REVIEW'].map((mode) => (
                                            <button
                                                key={mode}
                                                role="menuitemradio"
                                                aria-checked={selectedMode === mode}
                                                onClick={() => { setSelectedMode(mode as SessionMode); setIsModeMenuOpen(false); }}
                                                className={twMerge(
                                                    "w-full text-left p-2 rounded-lg group transition-colors flex items-start gap-3 min-h-[36px]",
                                                    selectedMode === mode ? 'bg-indigo-500/10' : 'hover:bg-white/5'
                                                )}
                                            >
                                                <div className={twMerge("mt-0.5 transition-colors", selectedMode === mode ? 'text-indigo-400' : 'text-zinc-500 group-hover:text-zinc-300')}>
                                                    {mode === 'START' && <Rocket size={14} />}
                                                    {mode === 'SCHEDULED' && <Clock size={14} />}
                                                    {mode === 'INTERACTIVE' && <MessageSquare size={14} />}
                                                    {mode === 'REVIEW' && <FileSearch size={14} />}
                                                </div>
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-center gap-2">
                                                        <span className={twMerge("text-[12px] font-medium", selectedMode === mode ? 'text-indigo-200' : 'text-zinc-300')}>
                                                            {mode === 'START' ? 'Start immediately' :
                                                                mode === 'SCHEDULED' ? 'Scheduled task' :
                                                                    mode === 'INTERACTIVE' ? 'Interactive plan' : 'Review plan'}
                                                        </span>
                                                        {mode === 'SCHEDULED' && <span className="text-[9px] font-bold bg-[#6366F1]/20 text-[#818CF8] px-1.5 py-0.5 rounded tracking-wide">NEW</span>}
                                                    </div>
                                                </div>
                                                {selectedMode === mode && <Check size={12} className="text-indigo-400 mt-1" />}
                                            </button>
                                        ))}
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>

                    {/* Right: Send Button - Square 1:1, w-6 h-6 */}
                    <div className="flex items-center gap-2 flex-shrink-0">
                        <motion.button
                            whileHover={{ scale: 1.08 }}
                            whileTap={{ scale: 0.92 }}
                            onClick={(e) => {
                                e.stopPropagation();
                                handleSubmit();
                            }}
                            disabled={!input.trim() || isLoading}
                            aria-label="Send message"
                            className={twMerge(
                                "w-6 h-6 aspect-square flex items-center justify-center rounded-md transition-all duration-150 flex-shrink-0",
                                input.trim()
                                    ? 'bg-indigo-600 text-white hover:bg-indigo-500 shadow-lg shadow-indigo-500/25'
                                    : 'bg-surfaceHighlight text-zinc-500 cursor-not-allowed border border-white/5'
                            )}
                        >
                            {isLoading ? (
                                <div className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            ) : (
                                <ArrowRight size={14} />
                            )}
                        </motion.button>
                    </div>
                </div>
            </div>
        </div>
    );
});
InputArea.displayName = 'InputArea';
