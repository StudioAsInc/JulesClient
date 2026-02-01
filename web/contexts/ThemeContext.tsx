import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import {
    Theme,
    ThemeSettings,
    PaginationSettings,
    DEFAULT_THEME,
    DEFAULT_SETTINGS,
    isValidThemeSettings,
    isValidTheme
} from '../types/themeTypes';
import * as JulesApi from '../services/geminiService';

const STORAGE_KEY = 'jules_theme_settings';

interface ThemeContextValue {
    theme: Theme;
    settings: ThemeSettings;
    updateTheme: (updates: Partial<Theme>) => void;
    setTheme: (theme: Theme) => void;
    resetTheme: () => void;
    exportSettings: () => string;
    importSettings: (json: string) => { success: boolean; error?: string };
    defaultCardCollapsed: boolean;
    setDefaultCardCollapsed: (collapsed: boolean) => void;
    pagination: PaginationSettings;
    setPagination: (pagination: PaginationSettings) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

function applyThemeToDOM(theme: Theme) {
    const root = document.documentElement;
    root.style.setProperty('--color-background', theme.background);
    root.style.setProperty('--color-surface', theme.surface);
    root.style.setProperty('--color-surface-highlight', theme.surfaceHighlight);
    root.style.setProperty('--color-border', theme.border);
    root.style.setProperty('--color-primary', theme.primary);
    root.style.setProperty('--color-text-main', theme.textMain);
    root.style.setProperty('--color-text-muted', theme.textMuted);

    // Update body background
    document.body.style.backgroundColor = theme.background;
    document.body.style.color = theme.textMain;
}

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [settings, setSettings] = useState<ThemeSettings>(() => {
        try {
            const saved = localStorage.getItem(STORAGE_KEY);
            if (saved) {
                const parsed = JSON.parse(saved);
                if (isValidThemeSettings(parsed)) {
                    return parsed;
                }
            }
        } catch (e) {
            console.warn('Failed to load theme settings:', e);
        }
        return DEFAULT_SETTINGS;
    });

    // Apply theme to DOM on mount and when theme changes
    useEffect(() => {
        applyThemeToDOM(settings.theme);
    }, [settings.theme]);

    // Sync pagination settings with API service
    useEffect(() => {
        if (settings.pagination) {
            JulesApi.setPaginationSettings(settings.pagination);
        }
    }, [settings.pagination]);

    // Persist settings to localStorage
    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    }, [settings]);

    const updateTheme = useCallback((updates: Partial<Theme>) => {
        setSettings(prev => ({
            ...prev,
            theme: { ...prev.theme, ...updates }
        }));
    }, []);

    const setTheme = useCallback((theme: Theme) => {
        setSettings(prev => ({ ...prev, theme }));
    }, []);

    const resetTheme = useCallback(() => {
        setSettings(DEFAULT_SETTINGS);
    }, []);

    const exportSettings = useCallback(() => {
        return JSON.stringify(settings, null, 2);
    }, [settings]);

    const importSettings = useCallback((json: string): { success: boolean; error?: string } => {
        try {
            const parsed = JSON.parse(json);
            if (isValidThemeSettings(parsed)) {
                setSettings(parsed);
                return { success: true };
            } else if (isValidTheme(parsed)) {
                // Allow importing just a theme object
                setSettings(prev => ({ ...prev, theme: parsed }));
                return { success: true };
            }
            return { success: false, error: 'Invalid theme format. Please check the JSON structure.' };
        } catch (e) {
            return { success: false, error: 'Invalid JSON. Please check the file format.' };
        }
    }, []);

    const setDefaultCardCollapsed = useCallback((collapsed: boolean) => {
        setSettings(prev => ({ ...prev, defaultCardCollapsed: collapsed }));
    }, []);

    const setPagination = useCallback((pagination: PaginationSettings) => {
        setSettings(prev => ({ ...prev, pagination }));
    }, []);

    return (
        <ThemeContext.Provider value={{
            theme: settings.theme,
            settings,
            updateTheme,
            setTheme,
            resetTheme,
            exportSettings,
            importSettings,
            defaultCardCollapsed: settings.defaultCardCollapsed,
            setDefaultCardCollapsed,
            pagination: settings.pagination || DEFAULT_SETTINGS.pagination,
            setPagination,
        }}>
            {children}
        </ThemeContext.Provider>
    );
}

export function useTheme() {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
}
