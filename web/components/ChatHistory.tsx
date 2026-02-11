import React, { useState, memo, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { JulesActivity, Step, formatRelativeTime } from '../types';
import { motion, AnimatePresence } from 'framer-motion';

const REMARK_PLUGINS = [remarkGfm];
import {
    Check, CheckCircle2, CircleDashed, GitPullRequest, Terminal,
    Loader2, Sparkles, GitMerge, ListTodo, ChevronRight,
    ChevronDown, Copy, ExternalLink, FileDiff, FileText, Image as ImageIcon,
    Command, Clock, Bot, Download, ArrowRight, MoreVertical, XCircle, GitBranch
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { twMerge } from 'tailwind-merge';

interface ChatHistoryProps {
    activities: JulesActivity[];
    isStreaming: boolean;
    onApprovePlan: (activityId: string) => void;
    sessionOutputs?: Array<{ pullRequest?: { url: string; title: string; description: string; branch?: string } }>;
    sessionPrompt?: string;
    sessionCreateTime?: string;
    defaultCardCollapsed?: boolean;
}

const formatTime = (isoString?: string) => {
    if (!isoString) return '';
    try {
        return new Date(isoString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
        return '';
    }
};

const getTextContent = (msg: any): string => {
    if (!msg) return "";
    if (typeof msg === 'string') return msg;
    // Handle nested message fields and their snake_case variants
    const content =
        msg.userMessage || msg.user_message ||
        msg.agentMessage || msg.agent_message ||
        msg.text || msg.message || msg.content ||
        msg.prompt || "";

    if (content) return content;

    if (msg.parts && Array.isArray(msg.parts)) {
        return msg.parts.map((p: any) => p.text || "").join("");
    }
    return "";
};

const MarkdownComponents = {
    code({ node, className, children, ...props }: any) {
        const match = /language-(\w+)/.exec(className || '')
        // If there is a language match, treat as block code
        if (match) {
            return (
                <div className="rounded-lg bg-background/40 border border-white/10 overflow-hidden my-3 w-full shadow-inner">
                    <div className="flex items-center justify-between px-3 py-1.5 bg-white/5 border-b border-white/5">
                        <span className="text-[10px] text-zinc-500 font-mono uppercase tracking-wider">{match[1]}</span>
                    </div>
                    <pre className="p-3 overflow-x-auto text-sm text-zinc-300 font-mono custom-scrollbar max-w-full">
                        <code className={className} {...props}>
                            {children}
                        </code>
                    </pre>
                </div>
            )
        }
        // Otherwise treat as inline code
        return (
            <code className={twMerge("bg-white/10 rounded px-1.5 py-0.5 text-[0.9em] font-mono text-zinc-200 break-words", className)} {...props}>
                {children}
            </code>
        )
    },
    a({ node, children, ...props }: any) {
        return <a target="_blank" rel="noopener noreferrer" className="text-indigo-400 hover:text-indigo-300 underline underline-offset-2 transition-colors break-words" {...props}>{children}</a>
    },
    ul({ children }: any) { return <ul className="list-disc pl-5 space-y-1 my-2 marker:text-zinc-500">{children}</ul> },
    ol({ children }: any) { return <ol className="list-decimal pl-5 space-y-1 my-2 marker:text-zinc-500">{children}</ol> },
    h1({ children }: any) { return <h1 className="text-lg font-semibold mt-4 mb-2 text-zinc-100">{children}</h1> },
    h2({ children }: any) { return <h2 className="text-base font-medium mt-3 mb-2 text-zinc-100">{children}</h2> },
    h3({ children }: any) { return <h3 className="text-sm font-medium mt-3 mb-1 text-zinc-200">{children}</h3> },
    p({ children }: any) { return <p className="mb-2 last:mb-0 leading-relaxed break-words">{children}</p> },
    li({ children }: any) { return <li className="pl-1 break-words leading-relaxed">{children}</li> },
    // Use fragment for pre to avoid nested pre tags when we render block code
    pre({ children }: any) { return <>{children}</> }
};

const UserMessageBubble: React.FC<{ text: string, time?: string }> = memo(({ text, time }) => {
    const [isExpanded, setIsExpanded] = useState(false);
    const lineCount = text.split('\n').length;
    const isLong = text.length > 500 || lineCount > 8;

    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
            className="flex gap-3 sm:gap-4 justify-end w-full overflow-hidden"
        >
            <div className="flex flex-col items-end gap-1.5 max-w-[92%] sm:max-w-[85%] md:max-w-[80%] min-w-0">
                <div
                    className={twMerge(
                        "group relative bg-surfaceHighlight text-white border border-white/5 rounded-[20px] sm:rounded-[24px] px-4 py-3 sm:px-5 sm:py-3.5 text-[15px] leading-relaxed shadow-lg w-full",
                        isLong && !isExpanded && "pb-8"
                    )}
                >
                    <div className={twMerge(
                        "prose prose-invert prose-p:my-0 prose-pre:my-2 max-w-none break-words overflow-hidden",
                        !isExpanded && isLong && 'line-clamp-[12]'
                    )}>
                        <ReactMarkdown
                            remarkPlugins={REMARK_PLUGINS}
                            components={MarkdownComponents}
                        >
                            {text}
                        </ReactMarkdown>
                    </div>

                    {/* Gradient fade for truncated content */}
                    {isLong && !isExpanded && (
                        <div className="absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-surfaceHighlight to-transparent pointer-events-none rounded-b-[20px] sm:rounded-b-[24px]" />
                    )}

                    {/* Expand/Collapse button inside bubble at bottom */}
                    {isLong && (
                        <button
                            onClick={() => setIsExpanded(!isExpanded)}
                            className="absolute bottom-2 left-1/2 -translate-x-1/2 flex items-center gap-1.5 px-3 py-1 text-xs font-medium text-zinc-400 hover:text-white bg-white/5 hover:bg-white/10 rounded-full border border-white/5 transition-all min-h-[28px]"
                        >
                            {isExpanded ? (
                                <>Show less <ChevronDown size={12} className="rotate-180" /></>
                            ) : (
                                <>Show more <ChevronDown size={12} /></>
                            )}
                        </button>
                    )}
                </div>
                {time && <span className="text-[10px] text-zinc-600 font-mono px-1">{time}</span>}
            </div>
        </motion.div>
    );
});
UserMessageBubble.displayName = 'UserMessageBubble';

const PlanStepItem: React.FC<{ step: Step, index: number }> = memo(({ step, index }) => {
    const [isExpanded, setIsExpanded] = useState(false);

    return (
        <motion.div
            layout
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex flex-col gap-2 p-3 rounded-xl hover:bg-white/5 transition-colors cursor-pointer group border border-transparent hover:border-white/5 select-none min-h-[44px]"
        >
            <div className="flex items-center gap-3">
                <div className="flex-shrink-0 w-6 h-6 rounded-full bg-surface border border-white/10 flex items-center justify-center text-[10px] font-mono text-zinc-500 group-hover:border-indigo-500/50 group-hover:text-indigo-300 transition-colors">
                    {index + 1}
                </div>
                <span className="text-sm font-medium text-zinc-200 group-hover:text-white flex-1 leading-snug truncate">
                    {step.title || "Untitled Step"}
                </span>
                <ChevronDown size={14} className={twMerge("text-zinc-500 group-hover:text-zinc-300 flex-shrink-0 transition-transform", isExpanded && "rotate-180")} />
            </div>

            <AnimatePresence>
                {isExpanded && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="pl-9 text-sm text-zinc-400 leading-relaxed font-light overflow-hidden"
                        onClick={e => e.stopPropagation()}
                    >
                        <ReactMarkdown components={MarkdownComponents}>
                            {step.description}
                        </ReactMarkdown>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
});
PlanStepItem.displayName = 'PlanStepItem';

const CommandArtifact: React.FC<{ command: string, output?: string, exitCode?: number, defaultCollapsed?: boolean }> = memo(({ command, output, exitCode, defaultCollapsed }) => {
    const [isExpanded, setIsExpanded] = useState(!defaultCollapsed);
    const isFailed = exitCode !== undefined && exitCode !== 0;

    return (
        <div className="w-full min-w-0 max-w-[calc(100vw-4rem)] sm:max-w-xl md:max-w-2xl box-border">
            <div
                className={twMerge(
                    "font-mono text-xs bg-background border rounded-xl overflow-hidden shadow-lg ring-1 transition-all w-full",
                    isFailed
                        ? "border-red-500/30 ring-red-500/10 hover:border-red-500/50"
                        : "border-white/10 ring-white/5 hover:border-white/20",
                    isExpanded ? (isFailed ? "border-red-500/50" : "border-white/20") : ""
                )}
            >
                {/* Header - Click to toggle */}
                <div
                    onClick={() => setIsExpanded(!isExpanded)}
                    className={twMerge(
                        "flex items-center justify-between px-3 py-2.5 border-b cursor-pointer transition-colors group min-h-[44px] w-full",
                        isFailed
                            ? "bg-red-500/[0.03] border-red-500/10 hover:bg-red-500/[0.06]"
                            : "bg-white/[0.02] border-white/5 hover:bg-white/[0.05]"
                    )}
                >
                    <div className="flex items-center gap-3 min-w-0 flex-1 overflow-hidden">
                        <Terminal size={14} className={isFailed ? "text-red-400 flex-shrink-0" : "text-zinc-500 flex-shrink-0"} />
                        <div className="flex items-center gap-2 min-w-0 flex-1 overflow-hidden">
                            <span className={twMerge("flex-shrink-0", isFailed ? "text-red-400 font-bold" : "text-green-500 font-bold")}>
                                {isFailed ? "✕" : "➜"}
                            </span>
                            <span className="font-medium text-zinc-300 truncate min-w-0">{command}</span>
                        </div>
                    </div>

                    <div className="flex items-center gap-2 pl-2 flex-shrink-0">
                        {isFailed && (
                            <span className="flex items-center gap-1 text-[10px] text-red-400 bg-red-500/10 px-1.5 py-0.5 rounded border border-red-500/20">
                                <XCircle size={10} />
                                Failed
                            </span>
                        )}
                        {!isExpanded && output && !isFailed && (
                            <span className="text-[10px] text-zinc-600 bg-white/5 px-1.5 py-0.5 rounded border border-white/5 hidden sm:inline">
                                Output
                            </span>
                        )}
                        <ChevronDown
                            size={14}
                            className={twMerge(
                                "transition-transform duration-200 flex-shrink-0",
                                isFailed ? "text-red-400" : "text-zinc-500",
                                isExpanded && "rotate-180"
                            )}
                        />
                    </div>
                </div>

                {/* Expanded Content */}
                <AnimatePresence>
                    {isExpanded && (
                        <motion.div
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: "auto", opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                            transition={{ duration: 0.2 }}
                            className="w-full overflow-hidden"
                        >
                            <div className={twMerge(
                                "p-3.5 overflow-x-auto overflow-y-auto custom-scrollbar max-h-[300px] border-t w-full",
                                isFailed ? "border-red-500/10 bg-red-500/[0.02]" : "border-white/5 bg-black/20"
                            )}>
                                {output ? (
                                    <div className={twMerge(
                                        "whitespace-pre-wrap break-all leading-relaxed font-mono text-xs w-full overflow-hidden",
                                        isFailed ? "text-red-300/90" : "text-zinc-400/90"
                                    )}>
                                        {output}
                                    </div>
                                ) : (
                                    <span className="text-zinc-600 italic">No output</span>
                                )}
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    );
});
CommandArtifact.displayName = 'CommandArtifact';

const CodeChangeArtifact: React.FC<{ changeSet?: any, defaultCollapsed?: boolean }> = memo(({ changeSet, defaultCollapsed }) => {
    const [isExpanded, setIsExpanded] = useState(!defaultCollapsed);

    if (!changeSet?.gitPatch?.unidiffPatch) return null;

    const getFileName = (patch: string) => {
        const match = patch.match(/^\+\+\+\s+(?:b\/)?(.+)$/m);
        if (match) {
            const fullPath = match[1].trim();
            const parts = fullPath.split('/');
            return parts.length > 2 ? `.../${parts.slice(-2).join('/')}` : fullPath;
        }
        return null;
    };

    const fileName = getFileName(changeSet.gitPatch.unidiffPatch);

    return (
        <div className="w-full min-w-0 max-w-[calc(100vw-4rem)] sm:max-w-xl md:max-w-2xl box-border">
            <div
                className={twMerge(
                    "bg-background border border-white/10 rounded-xl overflow-hidden shadow-lg ring-1 ring-white/5 transition-all hover:border-white/20 w-full",
                    isExpanded ? "border-white/20" : ""
                )}
            >
                {/* Header - Click to toggle */}
                <div
                    onClick={() => setIsExpanded(!isExpanded)}
                    className="flex items-center justify-between px-3 py-2.5 bg-white/[0.02] border-b border-white/5 cursor-pointer hover:bg-white/[0.05] transition-colors group min-h-[44px] w-full"
                >
                    <div className="flex items-center gap-2 min-w-0 flex-1 overflow-hidden">
                        <FileDiff size={14} className="text-zinc-500 flex-shrink-0" />
                        <div className="flex items-center gap-2 min-w-0 flex-1 overflow-hidden">
                            <span className="font-medium text-zinc-300 truncate font-mono text-xs min-w-0">
                                {changeSet.gitPatch?.suggestedCommitMessage || "Code Changes Proposed"}
                            </span>
                            {fileName && (
                                <span className="text-zinc-500 text-[10px] bg-white/5 px-1.5 py-0.5 rounded border border-white/5 truncate font-mono flex-shrink-0 max-w-[100px] sm:max-w-[150px] hidden sm:inline">
                                    {fileName}
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="flex items-center gap-2 pl-2 flex-shrink-0">
                        {!isExpanded && (
                            <span className="text-[10px] text-zinc-600 bg-white/5 px-1.5 py-0.5 rounded border border-white/5 hidden sm:inline">
                                View Diff
                            </span>
                        )}
                        <ChevronDown
                            size={14}
                            className={twMerge(
                                "text-zinc-500 transition-transform duration-200 flex-shrink-0",
                                isExpanded && "rotate-180"
                            )}
                        />
                    </div>
                </div>

                {/* Expanded Content */}
                <AnimatePresence>
                    {isExpanded && (
                        <motion.div
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: "auto", opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                            transition={{ duration: 0.2 }}
                            className="w-full overflow-hidden"
                        >
                            <div className="overflow-x-auto custom-scrollbar max-h-[500px] border-t border-white/5 bg-background w-full">
                                <pre className="p-3 font-mono text-xs leading-relaxed w-max min-w-full">
                                    {changeSet.gitPatch.unidiffPatch.split('\n').map((line: string, i: number) => {
                                        let color = "text-zinc-400";
                                        let bg = "transparent";

                                        if (line.startsWith('+') && !line.startsWith('+++')) {
                                            color = "text-green-400";
                                            bg = "bg-green-500/5";
                                        } else if (line.startsWith('-') && !line.startsWith('---')) {
                                            color = "text-red-400";
                                            bg = "bg-red-500/5";
                                        } else if (line.startsWith('@@')) {
                                            color = "text-indigo-400";
                                        }

                                        return (
                                            <div key={i} className={`${bg} px-2 -mx-2`}>
                                                <span className={`${color} whitespace-pre`}>{line}</span>
                                            </div>
                                        );
                                    })}
                                </pre>
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    );
});
CodeChangeArtifact.displayName = 'CodeChangeArtifact';

const CompactSessionCompleted: React.FC<{ timestamp?: string }> = memo(({ timestamp }) => {
    React.useEffect(() => {
        // Only trigger confetti if the completion event is recent (within last 10 seconds)
        // This prevents confetti from showing when viewing old history
        if (!timestamp) return;

        const eventTime = new Date(timestamp).getTime();
        // Allow a small window for clock skew/network delay, but generally we want "fresh" events
        // If the event is older than 10 seconds, assume it's history loading
        if (Date.now() - eventTime > 10000) return;

        const end = Date.now() + 1000;
        const colors = ['#a786ff', '#fd8bbc', '#eca184', '#f8deb1'];

        (function frame() {
            confetti({
                particleCount: 2,
                angle: 60,
                spread: 55,
                origin: { x: 0 },
                colors: colors
            });
            confetti({
                particleCount: 2,
                angle: 120,
                spread: 55,
                origin: { x: 1 },
                colors: colors
            });

            if (Date.now() < end) {
                requestAnimationFrame(frame);
            }
        }());
    }, [timestamp]);

    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="flex justify-center my-6"
        >
            <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-green-500/10 border border-green-500/20 shadow-lg shadow-green-500/10 backdrop-blur-sm">
                <CheckCircle2 size={16} className="text-green-400" />
                <span className="text-xs font-medium text-green-100">Session Completed Successfully</span>
            </div>
        </motion.div>
    );
});
CompactSessionCompleted.displayName = 'CompactSessionCompleted';

const CompactSessionFailed: React.FC<{ reason?: string }> = memo(({ reason }) => {
    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="flex justify-center my-6"
        >
            <div className="flex flex-col items-center gap-2 px-5 py-3 rounded-2xl bg-red-500/10 border border-red-500/20 shadow-lg shadow-red-500/10 backdrop-blur-sm max-w-md">
                <div className="flex items-center gap-2">
                    <XCircle size={16} className="text-red-400" />
                    <span className="text-xs font-medium text-red-100">Session Failed</span>
                </div>
                {reason && (
                    <p className="text-[11px] text-red-300/80 text-center leading-relaxed">
                        {reason}
                    </p>
                )}
            </div>
        </motion.div>
    );
});
CompactSessionFailed.displayName = 'CompactSessionFailed';

const PullRequestCard: React.FC<{ output: { pullRequest?: { url: string; title: string; description: string; branch?: string } } }> = memo(({ output }) => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const pr = output.pullRequest;

    if (!pr) return null;

    const getBranchUrl = () => {
        if (!pr.branch || !pr.url) return null;
        try {
            const urlParts = pr.url.split('/');
            if (urlParts.length >= 5) {
                const baseUrl = urlParts.slice(0, 5).join('/');
                return `${baseUrl}/tree/${pr.branch}`;
            }
        } catch (e) { return null; }
        return null; // Fallback or unable to parse
    };

    const branchUrl = getBranchUrl();

    return (
        <div className="w-full min-w-0 max-w-[calc(100vw-4rem)] sm:max-w-md md:max-w-lg bg-gradient-to-br from-surface to-background border border-white/10 rounded-xl overflow-hidden shadow-2xl ring-1 ring-white/5 hover:ring-indigo-500/20 hover:border-indigo-500/30 transition-all duration-300 group/card relative box-border">
            <div className="px-4 sm:px-5 py-3 sm:py-4 border-b border-white/5 flex items-center justify-between bg-white/[0.02] rounded-t-xl gap-2">
                <div className="flex items-center gap-2 sm:gap-3 min-w-0 flex-1">
                    <div className="p-1.5 sm:p-2 bg-green-500/10 rounded-lg border border-green-500/20 shadow-inner shadow-green-500/5 flex-shrink-0">
                        <GitPullRequest size={14} className="text-green-400 sm:w-4 sm:h-4" />
                    </div>
                    <div className="min-w-0">
                        <div className="text-zinc-200 font-medium text-xs sm:text-sm tracking-wide truncate">Pull Request Ready</div>
                        <div className="text-[10px] text-zinc-500 font-medium hidden sm:block">Click to review</div>
                    </div>
                </div>

                <div className="flex items-center gap-2 sm:gap-3 flex-shrink-0">
                    <span className="relative flex h-2 w-2">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                    </span>
                </div>
            </div>

            <div className="p-5 sm:p-6 space-y-4 sm:space-y-5 rounded-b-xl">
                <div className="min-w-0 overflow-hidden">
                    <h3 className="text-sm sm:text-base font-semibold text-white leading-snug mb-2 sm:mb-2.5">
                        <span className="line-clamp-2 break-words">{pr.title || "Untitled Pull Request"}</span>
                    </h3>
                    {pr.description && (
                        <p className="text-[11px] sm:text-xs text-zinc-400 leading-relaxed line-clamp-2 sm:line-clamp-3 break-words">
                            {pr.description}
                        </p>
                    )}
                    {pr.branch && (
                        <div className="flex items-center gap-1.5 mt-3">
                            <GitBranch size={11} className="text-zinc-500" />
                            <span className="text-[10px] font-mono text-zinc-500 bg-white/5 px-2 py-1 rounded border border-white/5">
                                {pr.branch}
                            </span>
                        </div>
                    )}
                </div>

                <div className="flex items-center gap-2 sm:gap-3 pt-2 sm:pt-3">
                    <a
                        href={pr.url}
                        target="_blank"
                        rel="noreferrer"
                        className="flex-1 flex items-center justify-center gap-2 px-3 sm:px-4 py-2.5 sm:py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs sm:text-sm font-medium transition-all shadow-lg shadow-indigo-500/20 hover:shadow-indigo-500/40 active:scale-[0.98] min-h-[44px]"
                    >
                        <ExternalLink size={14} />
                        <span className="truncate">View PR</span>
                    </a>
                    <button
                        onClick={() => {
                            if (pr.url) {
                                navigator.clipboard.writeText(pr.url);
                            }
                        }}
                        className="p-2.5 sm:p-3 bg-white/5 hover:bg-white/10 text-zinc-400 hover:text-white rounded-lg border border-white/5 hover:border-white/10 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center flex-shrink-0"
                        title="Copy URL"
                    >
                        <Copy size={16} />
                    </button>
                </div>

                <div className="text-[9px] sm:text-[10px] font-mono text-zinc-600/70 text-center select-all overflow-hidden">
                    <div className="truncate">{pr.url}</div>
                </div>
            </div>
        </div>
    );
});
PullRequestCard.displayName = 'PullRequestCard';



const ActivityItem: React.FC<{
    act: JulesActivity;
    onApprovePlan: (activityId: string) => void;
    isApproved: boolean;
    isCurrentlyActive: boolean;
    defaultCardCollapsed?: boolean;
}> = memo(({ act, onApprovePlan, isApproved, isCurrentlyActive, defaultCardCollapsed }) => {
    const timeString = formatTime(act.createTime);
    const items: React.ReactNode[] = [];

    // --- 0. System Messages ---
    if (act.originator === 'system' && !act.planGenerated && !act.userMessaged && !act.agentMessaged && act.description) {
        items.push(
            <div
                key="system"
                className="flex justify-center my-6"
            >
                <span className="text-[11px] text-zinc-500 bg-white/5 px-3 py-1 rounded-full border border-white/5 font-medium tracking-wide text-center">
                    {act.description}
                </span>
            </div>
        );
    }

    // --- 1. User Message ---
    if (act.userMessaged || act.userMessage) {
        const userText = getTextContent(act.userMessaged || act.userMessage);
        if (userText) {
            items.push(<UserMessageBubble key="user" text={userText} time={timeString} />);
        }
    }

    // --- 2. Agent Message ---
    if (act.agentMessaged || act.agentMessage) {
        const agentText = getTextContent(act.agentMessaged || act.agentMessage) || "Thinking...";
        items.push(
            <div
                key="agent"
                className="flex gap-3 sm:gap-5 justify-start group w-full overflow-hidden"
            >
                <div className="w-8 h-8 rounded-full bg-surface flex-shrink-0 flex items-center justify-center border border-white/10 mt-1 shadow-sm">
                    <Bot size={18} className="text-indigo-400" />
                </div>
                <div className="min-w-0 flex-1 max-w-full sm:max-w-[90%] flex flex-col gap-1 overflow-hidden">
                    <div className="text-zinc-200 text-[15px] leading-relaxed pt-1.5 font-light break-words overflow-hidden">
                        <ReactMarkdown
                            remarkPlugins={REMARK_PLUGINS}
                            components={MarkdownComponents}
                        >
                            {agentText}
                        </ReactMarkdown>
                    </div>
                    {timeString && (
                        <div className="text-[10px] text-zinc-600 font-mono opacity-0 group-hover:opacity-100 transition-opacity">
                            Jules • {timeString}
                        </div>
                    )}
                </div>
            </div>
        );
    }

    // --- 3. Plan Generated ---
    if (act.planGenerated) {
        items.push(
            <div
                key="plan"
                className="flex gap-3 sm:gap-5 justify-start w-full min-w-0"
            >
                <div className="w-8 h-8 flex-shrink-0" />
                <div className="w-full min-w-0 max-w-[calc(100vw-4rem)] sm:max-w-xl bg-surface border border-white/10 rounded-2xl overflow-hidden shadow-2xl ring-1 ring-white/5">
                    <div className="bg-surface px-5 py-3 border-b border-white/5 flex items-center justify-between">
                        <span className="text-sm font-medium text-white flex items-center gap-2">
                            <ListTodo size={16} className="text-indigo-400" />
                            Execution Plan
                        </span>
                        <span className="text-xs text-zinc-500 font-mono bg-white/5 px-2 py-0.5 rounded border border-white/5">
                            {act.planGenerated.plan?.steps?.length || 0} steps
                        </span>
                    </div>
                    <div className="p-2 space-y-1">
                        {act.planGenerated.plan?.steps ? (
                            act.planGenerated.plan.steps.map((step, i) => (
                                <PlanStepItem key={i} step={step} index={i} />
                            ))
                        ) : (
                            <div className="p-4 text-sm italic text-zinc-500">Generating plan details...</div>
                        )}
                    </div>
                    <div className="p-4 bg-black/20 border-t border-white/5 flex justify-between items-center gap-4">
                        <span className="text-xs text-zinc-500 hidden sm:block">Review the plan before continuing.</span>
                        <div className="ml-auto w-full sm:w-auto">
                            {isApproved ? (
                                <div className="flex justify-center sm:justify-end">
                                    <span className="flex items-center gap-2 text-xs font-medium text-green-400 bg-green-500/10 px-3 py-1.5 rounded-full border border-green-500/20">
                                        <Check size={12} /> Plan approved
                                    </span>
                                </div>
                            ) : (
                                <button
                                    onClick={() => onApprovePlan(act.name)}
                                    className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-sm font-medium transition-all shadow-lg shadow-indigo-500/10 hover:shadow-indigo-500/25 active:scale-95"
                                >
                                    Start Coding <ChevronRight size={14} />
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    // --- 4. Artifacts ---
    if (act.artifacts && act.artifacts.length > 0) {
        act.artifacts.forEach((artifact, i) => {
            if (artifact.bashOutput) {
                items.push(
                    <div
                        key={`art-${i}-bash`}
                        className="flex gap-3 sm:gap-5 justify-start w-full min-w-0"
                    >
                        <div className="w-8 h-8 flex-shrink-0" />
                        <CommandArtifact
                            command={artifact.bashOutput.command}
                            output={artifact.bashOutput.output}
                            exitCode={artifact.bashOutput.exitCode}
                            defaultCollapsed={defaultCardCollapsed}
                        />
                    </div>
                );
            }

            if (artifact.media) {
                items.push(
                    <div
                        key={`art-${i}-media`}
                        className="flex gap-3 sm:gap-5 justify-start"
                    >
                        <div className="w-8 h-8 flex-shrink-0" />
                        <div className="max-w-full sm:max-w-xl rounded-xl overflow-hidden border border-white/10 shadow-lg bg-background group">
                            <div className="flex items-center justify-between px-3 py-2 bg-white/5 border-b border-white/5">
                                <div className="flex items-center gap-2 text-xs text-zinc-400">
                                    <ImageIcon size={12} />
                                    <span>Generated Artifact</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <a
                                        href={`data:${artifact.media.mimeType};base64,${artifact.media.data}`}
                                        download={`artifact-${i}.${artifact.media.mimeType.split('/')[1] || 'png'}`}
                                        className="text-zinc-500 hover:text-zinc-300 transition-colors p-1 hover:bg-white/10 rounded"
                                        title="Download"
                                    >
                                        <Download size={14} />
                                    </a>
                                    <a
                                        onClick={(e) => {
                                            e.preventDefault();
                                            const win = window.open();
                                            win?.document.write(
                                                `<iframe src="data:${artifact.media.mimeType};base64,${artifact.media.data}" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>`
                                            );
                                        }}
                                        href="#"
                                        className="text-zinc-500 hover:text-zinc-300 transition-colors p-1 hover:bg-white/10 rounded"
                                        title="Open in new window"
                                    >
                                        <ExternalLink size={14} />
                                    </a>
                                </div>
                            </div>
                            <div className="relative bg-surfaceHighlight flex justify-center p-2">
                                <img
                                    src={`data:${artifact.media.mimeType};base64,${artifact.media.data}`}
                                    alt="Jules generated artifact"
                                    className="w-full h-auto object-contain max-h-[400px]"
                                />
                            </div>
                        </div>
                    </div>
                );
            }

            if (artifact.changeSet) {
                items.push(
                    <div
                        key={`art-${i}-diff`}
                        className="flex gap-3 sm:gap-5 justify-start w-full min-w-0"
                    >
                        <div className="w-8 h-8 flex-shrink-0" />
                        <CodeChangeArtifact changeSet={artifact.changeSet} defaultCollapsed={defaultCardCollapsed} />
                    </div>
                );
            }
        });
    }

    // --- 5. Progress Updates ---
    if (act.progressUpdated) {
        const progress = act.progressUpdated;
        const defaultTitle = isCurrentlyActive ? "Processing" : "Processed";
        const title = progress.title || progress.progress_title || progress.status || progress.status_update || act.description || defaultTitle;
        const description = progress.description || progress.progress_description || progress.text || progress.message;
        const cleanTitle = title.trim().toLowerCase();
        const cleanDesc = description ? description.trim().toLowerCase() : "";
        const isRedundant = !description || cleanTitle === cleanDesc || cleanTitle.includes(cleanDesc);

        items.push(
            <div key="progress" className="flex gap-3 sm:gap-5 justify-start group w-full overflow-hidden">
                <div className="w-8 h-8 rounded-full bg-surface flex-shrink-0 flex items-center justify-center border border-white/10 mt-1 shadow-sm">
                    <Bot size={18} className="text-indigo-400" />
                </div>
                <div className="min-w-0 flex-1 max-w-full sm:max-w-[90%] flex flex-col gap-1 overflow-hidden">
                    <div className="text-zinc-200 text-[15px] leading-relaxed pt-1.5 font-light break-words overflow-hidden">
                        <div className="flex items-center gap-2">
                            {(() => {
                                if (isCurrentlyActive) {
                                    return <Loader2 size={14} className="text-indigo-400 flex-shrink-0 animate-spin" />;
                                }
                                const lower = title.toLowerCase();
                                if (lower.includes('read') || lower.includes('analyz') || lower.includes('scan')) {
                                    return <FileText size={14} className="text-zinc-500 flex-shrink-0" />;
                                }
                                if (lower.includes('run') || lower.includes('exec') || lower.includes('command')) {
                                    return <Terminal size={14} className="text-zinc-500 flex-shrink-0" />;
                                }
                                if (lower.includes('think') || lower.includes('plan')) {
                                    return <Sparkles size={14} className="text-zinc-500 flex-shrink-0" />;
                                }
                                return <CheckCircle2 size={14} className="text-zinc-500 flex-shrink-0" />;
                            })()}
                            <span className={twMerge("font-medium", isCurrentlyActive ? "text-zinc-200" : "text-zinc-400")}>{title}</span>
                        </div>
                        {!isRedundant && (
                            <div className="text-zinc-400 text-sm mt-1 pl-6">{description}</div>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    // --- 6. Session Completed ---
    if (act.sessionCompleted) {
        items.push(<CompactSessionCompleted key="completed" timestamp={act.createTime} />);
    }

    // --- 7. Session Failed ---
    if (act.sessionFailed) {
        items.push(<CompactSessionFailed key="failed" reason={act.sessionFailed.reason} />);
    }

    if (items.length === 0) return null;
    return <React.Fragment>{items}</React.Fragment>;
});
ActivityItem.displayName = 'ActivityItem';

export const ChatHistory: React.FC<ChatHistoryProps> = memo(({ activities, isStreaming, onApprovePlan, sessionOutputs, sessionPrompt, sessionCreateTime, defaultCardCollapsed }) => {
    // Memoize the expensive initial prompt check
    const hasInitialPromptInActivities = useMemo(() => {
        if (!sessionPrompt) return false;
        return activities.some(act => {
            const userText = act.userMessaged ? getTextContent(act.userMessaged) : (act.userMessage ? getTextContent(act.userMessage) : "");
            return userText && (userText.trim() === sessionPrompt.trim() || sessionPrompt.trim().includes(userText.trim()));
        });
    }, [activities, sessionPrompt]);

    // Pre-calculate expensive loop conditions to avoid O(N^2)
    const { maxApprovedTime, lastSignificantIndex } = useMemo(() => {
        let maxTime = "";
        let lastSigIdx = -1;

        for (let i = 0; i < activities.length; i++) {
            const a = activities[i];
            if (a.planApproved && a.createTime > maxTime) {
                maxTime = a.createTime;
            }
            if (
                a.progressUpdated || a.agentMessage || a.agentMessaged ||
                a.planGenerated || a.sessionCompleted || a.sessionFailed
            ) {
                lastSigIdx = i;
            }
        }
        return { maxApprovedTime: maxTime, lastSignificantIndex: lastSigIdx };
    }, [activities]);

    return (
        <div className="space-y-6 sm:space-y-8 px-2 sm:px-4 w-full overflow-hidden">
            {/* Removed AnimatePresence to fix lag - animations were too heavy during polling */}
            <>
                {/* 0. Initial Prompt (if not in activities) */}
                {sessionPrompt && !hasInitialPromptInActivities && (
                    <UserMessageBubble
                        key="initial-prompt"
                        text={sessionPrompt}
                        time={formatTime(sessionCreateTime)}
                    />
                )}

                {activities.map((act, index) => {
                    // Optimized O(1) checks
                    const isApproved = maxApprovedTime > act.createTime;
                    const isCurrentlyActive = isStreaming && index >= lastSignificantIndex;

                    return (
                        <ActivityItem
                            key={act.name}
                            act={act}
                            onApprovePlan={onApprovePlan}
                            isApproved={isApproved}
                            isCurrentlyActive={isCurrentlyActive}
                            defaultCardCollapsed={defaultCardCollapsed}
                        />
                    );
                })}

                {/* Session Outputs */}
                {sessionOutputs && sessionOutputs.map((out, i) => (
                    <div
                        key={`out-${i}`}
                        className="flex gap-4 sm:gap-5 justify-start w-full min-w-0"
                    >
                        <div className="w-8 h-8 flex-shrink-0" />
                        <PullRequestCard output={out} />
                    </div>
                ))}

                {isStreaming && (
                    <div className="flex gap-3 sm:gap-5 justify-start w-full">
                        <div className="w-8 h-8 rounded-full bg-surface flex-shrink-0 border border-white/10 flex items-center justify-center mt-1">
                            <Bot size={18} className="text-indigo-400 opacity-70" />
                        </div>
                        <div className="flex flex-col gap-3 w-full max-w-[90%] sm:max-w-[75%] pt-1.5">
                            <div className="h-4 bg-gradient-to-r from-white/5 via-white/10 to-white/5 bg-[length:200%_100%] animate-shimmer rounded w-[90%]" />
                            <div className="h-4 bg-gradient-to-r from-white/5 via-white/10 to-white/5 bg-[length:200%_100%] animate-shimmer rounded w-[70%]" />
                            <div className="h-4 bg-gradient-to-r from-white/5 via-white/10 to-white/5 bg-[length:200%_100%] animate-shimmer rounded w-[80%]" />
                        </div>
                    </div>
                )}
            </>
        </div>
    );
});
ChatHistory.displayName = 'ChatHistory';