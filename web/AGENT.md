# AGENT.md – High-Performance & Premium UI Guidelines

## 🎯 **Core Directive**
You are an expert engineer and designer. **Mediocrity is unacceptable.**
All output must be **production-ready**, **accessible**, **fully responsive**, and **visually stunning**. 
Do not suggest "quick fixes" or "placeholders". Solve the problem correctly, robustly, and beautifully.

---

## � **Design System & Aesthetics (Premium UI)**
*Aim for a "Silicon Valley Product" level of polish.*

### **Color Palette (Dynamic Theme)**
Use CSS variables for ALL colors to ensure seamless light/dark mode switching.
- **Variables**:
  - `--bg-app`: Main background (e.g., `#FFFFFF` / `#0F1117`)
  - `--bg-surface`: Cards/Modals (lightly distinct from bg)
  - `--text-primary`: High contrast text (e.g., `#1A1A1A` / `#EDEDED`)
  - `--text-secondary`: Muted text (e.g., `#666666` / `#A1A1A6`)
  - `--primary-brand`: Vibrant accent color (e.g., `hsl(220, 90%, 55%)`)
  - `--border-subtle`: Subtle dividers (e.g., `rgba(0,0,0,0.08)` / `rgba(255,255,255,0.1)`)

### **Typography & Hierarchy**
- **Font**: Inter (Google Fonts). fallback: system-ui.
- **Rules**:
  - `h1`: 24px-32px, weights 600-700.
  - `body`: 14px-16px, weight 400 (Line height 1.5).
  - `caption`: 12px, weight 500 (Muted).
- **Whitespace**: Use a **4px grid system** (4, 8, 16, 24, 32px). `gap`, `padding`, and `margin` must align with this.

### **Advanced UX & Micro-Interactions**
- **Hover States**: Every interactive element MUST have a visible hover state (e.g., slight lift `translateY(-1px)`, color shift).
- **Focus States**: Custom focus rings (offset by 2px) for keyboard users. Never remove outline without replacement.
- **Glassmorphism**: Use `backdrop-filter: blur(12px)` + `background: rgba(var(--bg-surface), 0.7)` for overlays/headers.
- **Transitions**: `all 0.2s cubic-bezier(0.25, 0.46, 0.45, 0.94)` (smooth and snappy).
- **Feedback**:
  - **Loading**: Skeletons or vibrant spinners. NO "Loading..." text alone.
  - **Errors**: Friendly, actionable error banners. NO generic "Error occurred".

---

## 📐 **Responsive & Adaptive Strategy**
**"Sync" philosophy**: One codebase, perfect execution everywhere.
- **Container Queries**: Use where appropriate for component-level responsiveness.
- **Mobile (< 480px)**:
  - Touch targets ≥ 44px.
  - Bottom sheets instead of modals.
  - Sidebar becomes a drawer or bottom nav.
- **Desktop (≥ 1024px)**:
  - Exploit width: Side-by-side views, hover cards, expanded menus.
- **Testing**: Assume the user might resize the window aggressively. UI must not break.

---

## 🛡️ **Code Quality & Engineering Standards**
*Write code that other senior engineers would praise.*

### **TypeScript Rules**
- **Strict Typing**: NO `any`. Use `unknown` if necessary and narrow types.
- **Interfaces**: Define explicit `interface` for all Props and API responses.
- **Null Safety**: Handle `null` / `undefined` explicitly. Use optional chaining `?.`.

### **React Best Practices**
- **Hooks**: Isolate complex logic into custom hooks (`useChatSession`, `useTheme`).
- **Performance**:
  - Memoize expensive calculations (`useMemo`).
  - Memoize callbacks passed to children (`useCallback`).
  - Use `React.memo` for list items to prevent mass re-renders.
  - **Code Splitting**: `lazy` load heavy components/routes.
- **Error Boundaries**: Wrap major UI sections in Error Boundaries to prevent full app crashes.

### **File Structure**
- `/components`: Reusable UI atoms/molecules.
- `/features`: Domain-specific logic (e.g., `/chat`, `/dashboard`).
- `/hooks`: Shared application logic.
- `/utils`: Pure functions (formatting, validation).

---

## 🚀 **Workflow & Verification**
Before marking a task as "Done":
1. **Self-Review**: "Would I ship this to a paying customer?"
2. **Mobile Check**: "Does this button work with a thumb?"
3. **Optimized Build**: "Are there unused imports or console logs?"
4. **Clean Diff**: Remove any commented-out code.

