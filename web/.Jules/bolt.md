## 2026-02-02 - InputArea Re-render Cycle
**Learning:** The `InputArea` component intentionally re-renders every 3.5s to cycle placeholders via `useDynamicPlaceholder`. This creates a recurring performance cost for derived state like `filteredBranches` which runs on every render.
**Action:** Always memoize expensive derived state in components that have self-triggered re-renders (animations, timers), even if props are stable.
