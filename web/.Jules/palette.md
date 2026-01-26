# UX & Accessibility Palette

## Layout
- **Dashboard Container**: Use `max-w-3xl mx-auto` for consistent desktop width.
- **Vertical Rhythm**: Use `pt-6 sm:pt-16` for main content top padding.

## Components
- **Buttons**: Icon-only buttons must have `aria-label`. Minimum visual height `h-8`.
- **Inputs**: Avoid conditional font sizes to prevent layout shifts.
- **Lists/Tabs**: Use `gap` and `overflow-auto` instead of negative margin hacks.
- **Indicators**: Use a shimmer animation (`animate-pulse`) for active work states (e.g., IN_PROGRESS, PLANNING) in lists to indicate background activity.

## Colors
- **Background**: `#0c0c0c`
- **Surface**: `#161619` or `#121215`
- **Text**: `#E4E4E7` (zinc-200 equivalent)

- **Chat Input**: Use `items-end` alignment in flex containers to ensure action buttons remain anchored to the bottom when the text input expands to multiple lines.
- **Styling**: Use `rounded-[26px]` to achieve a "pill" shape for input containers, consistent with the premium design language.
