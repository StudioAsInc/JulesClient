// Theme Types and Constants

export interface Theme {
    background: string;
    surface: string;
    surfaceHighlight: string;
    border: string;
    primary: string;
    textMain: string;
    textMuted: string;
}

export interface ThemeSettings {
    theme: Theme;
    defaultCardCollapsed: boolean;
}

export const DEFAULT_THEME: Theme = {
    background: '#2e2e36',
    surface: '#3b3b44',
    surfaceHighlight: '#464652',
    border: '#6b7280',
    primary: '#A855F7',
    textMain: '#F3F4F6',
    textMuted: '#9CA3AF',
};

export const PRESET_THEMES: Record<string, Theme> = {
    dark: DEFAULT_THEME,
    midnight: {
        background: '#0f172a',
        surface: '#1e293b',
        surfaceHighlight: '#334155',
        border: '#475569',
        primary: '#6366f1',
        textMain: '#f1f5f9',
        textMuted: '#94a3b8',
    },
    ocean: {
        background: '#0c4a6e',
        surface: '#075985',
        surfaceHighlight: '#0369a1',
        border: '#0284c7',
        primary: '#38bdf8',
        textMain: '#f0f9ff',
        textMuted: '#bae6fd',
    },
    forest: {
        background: '#14532d',
        surface: '#166534',
        surfaceHighlight: '#15803d',
        border: '#22c55e',
        primary: '#4ade80',
        textMain: '#f0fdf4',
        textMuted: '#bbf7d0',
    },
    light: {
        background: '#f8fafc',
        surface: '#ffffff',
        surfaceHighlight: '#f1f5f9',
        border: '#e2e8f0',
        primary: '#7c3aed',
        textMain: '#1e293b',
        textMuted: '#64748b',
    },
    warmDark: {
        background: '#292524',
        surface: '#3b3734',
        surfaceHighlight: '#4a4540',
        border: '#78716c',
        primary: '#f97316',
        textMain: '#faf5f0',
        textMuted: '#d6d3d1',
    },
};

export const DEFAULT_SETTINGS: ThemeSettings = {
    theme: DEFAULT_THEME,
    defaultCardCollapsed: false,
};

// Validation helpers
export function isValidHexColor(color: string): boolean {
    return /^#([0-9A-Fa-f]{3}){1,2}$/.test(color);
}

export function isValidTheme(obj: unknown): obj is Theme {
    if (typeof obj !== 'object' || obj === null) return false;
    const theme = obj as Record<string, unknown>;
    const requiredKeys: (keyof Theme)[] = [
        'background', 'surface', 'surfaceHighlight', 'border',
        'primary', 'textMain', 'textMuted'
    ];
    return requiredKeys.every(key =>
        typeof theme[key] === 'string' && isValidHexColor(theme[key] as string)
    );
}

export function isValidThemeSettings(obj: unknown): obj is ThemeSettings {
    if (typeof obj !== 'object' || obj === null) return false;
    const settings = obj as Record<string, unknown>;
    return isValidTheme(settings.theme) && typeof settings.defaultCardCollapsed === 'boolean';
}
