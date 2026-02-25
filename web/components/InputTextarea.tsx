import React, { memo, forwardRef, useRef, useEffect } from 'react';
import { twMerge } from 'tailwind-merge';
import { useDynamicPlaceholder } from '../hooks/useDynamicPlaceholder';

const DEFAULT_PLACEHOLDERS = [
    "Refactor this function...",
    "Fix CSS alignment issues...",
    "Add a new API endpoint...",
    "Optimize performance...",
    "Write unit tests...",
    "Explain this code..."
];

const PLACEHOLDER_CYCLE_INTERVAL = 3500;

interface InputTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
    variant: 'default' | 'chat';
    staticPlaceholder?: string;
    isExpanded: boolean;
    isFocused: boolean;
    shouldPausePlaceholder: boolean;
    value: string;
}

const InputTextarea = memo(forwardRef<HTMLTextAreaElement, InputTextareaProps>(({
    variant,
    staticPlaceholder,
    isExpanded,
    isFocused,
    shouldPausePlaceholder,
    value,
    className,
    style,
    ...props
}, ref) => {
    const dynamicPlaceholder = useDynamicPlaceholder(
        DEFAULT_PLACEHOLDERS,
        PLACEHOLDER_CYCLE_INTERVAL,
        shouldPausePlaceholder
    );
    const effectivePlaceholder = staticPlaceholder || dynamicPlaceholder;

    // Use a local ref to handle auto-resize logic internally, while also populating the forwarded ref
    const localRef = useRef<HTMLTextAreaElement>(null);

    // Sync forwarded ref
    useEffect(() => {
        if (!ref) return;
        if (typeof ref === 'function') {
            ref(localRef.current);
        } else {
            (ref as React.MutableRefObject<HTMLTextAreaElement | null>).current = localRef.current;
        }
    }, [ref]);

    // Auto-resize textarea logic moved here
    useEffect(() => {
        if (localRef.current) {
            localRef.current.style.height = 'auto';
            if (variant === 'default') {
                if (isExpanded) {
                    const scrollHeight = localRef.current.scrollHeight;
                    localRef.current.style.height = `${Math.max(scrollHeight, 40)}px`;
                } else {
                    localRef.current.style.height = '24px';
                }
            } else {
                localRef.current.style.height = `${localRef.current.scrollHeight}px`;
            }
        }
    }, [value, isExpanded, variant]);

    if (variant === 'chat') {
        return (
             <textarea
                ref={localRef}
                value={value}
                placeholder={effectivePlaceholder}
                className={twMerge("flex-1 bg-transparent border-none outline-none text-textMain placeholder:text-zinc-500 resize-none py-2 max-h-[200px] text-base leading-relaxed min-w-0 font-normal", className)}
                rows={1}
                {...props}
            />
        );
    }

    return (
        <textarea
            ref={localRef}
            value={value}
            placeholder={effectivePlaceholder}
            className={twMerge("w-full bg-transparent border-none outline-none text-[#E4E4E7] placeholder:text-zinc-600 resize-none font-normal leading-relaxed transition-all duration-200 selection:bg-indigo-500/30 text-[15px]", className)}
            rows={1}
            style={{
                height: isExpanded ? 'auto' : '24px',
                minHeight: isExpanded ? '28px' : '24px',
                ...style
            }}
            {...props}
        />
    );
}));

InputTextarea.displayName = 'InputTextarea';

export default InputTextarea;
