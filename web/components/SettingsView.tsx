import React, { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import {
    ArrowLeft,
    Monitor,
    Palette,
    Download,
    Upload,
    RotateCcw,
    Check,
    AlertCircle,
    Sliders,
    Settings,
    ChevronRight,
    Info,
    Sun,
    Moon
} from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';
import { PRESET_THEMES, Theme } from '../types/themeTypes';

// Settings categories for mobile navigation
type SettingsCategory = 'general' | 'appearance' | 'theme' | 'import-export' | 'about';

interface ColorPickerProps {
    label: string;
    description?: string;
    value: string;
    onChange: (color: string) => void;
}

const ColorPicker: React.FC<ColorPickerProps> = ({ label, description, value, onChange }) => {
    return (
        <div className="flex items-center justify-between gap-3 p-4">
            <div className="space-y-0.5 flex-1 min-w-0">
                <div className="text-sm font-medium text-[var(--color-text-main)]">{label}</div>
                {description && (
                    <div className="text-xs text-[var(--color-text-muted)] truncate">{description}</div>
                )}
            </div>
            <div className="flex items-center gap-2 flex-shrink-0">
                <input
                    type="color"
                    value={value}
                    onChange={(e) => onChange(e.target.value)}
                    className="w-8 h-8 rounded-lg cursor-pointer border border-white/10 bg-transparent"
                    style={{ padding: 0 }}
                />
                <input
                    type="text"
                    value={value.toUpperCase()}
                    onChange={(e) => {
                        const val = e.target.value;
                        if (/^#[0-9A-Fa-f]{0,6}$/.test(val)) {
                            onChange(val);
                        }
                    }}
                    className="w-20 bg-black/30 border border-white/10 rounded-lg px-2 py-1.5 text-xs font-mono text-[var(--color-text-main)] focus:outline-none focus:border-[var(--color-primary)] transition-colors"
                />
            </div>
        </div>
    );
};

// Mobile Category List Item
const CategoryItem: React.FC<{
    icon: React.ReactNode;
    label: string;
    description?: string;
    onClick: () => void;
}> = ({ icon, label, description, onClick }) => (
    <button
        onClick={onClick}
        className="w-full flex items-center gap-4 p-4 hover:bg-white/5 active:bg-white/10 transition-colors text-left"
    >
        <div className="w-10 h-10 rounded-xl bg-[var(--color-primary)]/20 flex items-center justify-center text-[var(--color-primary)]">
            {icon}
        </div>
        <div className="flex-1 min-w-0">
            <div className="text-base font-medium text-[var(--color-text-main)]">{label}</div>
            {description && (
                <div className="text-sm text-[var(--color-text-muted)] truncate">{description}</div>
            )}
        </div>
        <ChevronRight size={20} className="text-[var(--color-text-muted)]" />
    </button>
);

// Toggle Switch Component
const ToggleSwitch: React.FC<{
    checked: boolean;
    onChange: (checked: boolean) => void;
    label: string;
    description?: string;
}> = ({ checked, onChange, label, description }) => (
    <div className="flex items-center justify-between gap-4 p-4">
        <div className="space-y-0.5 flex-1">
            <div className="text-sm font-medium text-[var(--color-text-main)]">{label}</div>
            {description && (
                <div className="text-xs text-[var(--color-text-muted)]">{description}</div>
            )}
        </div>
        <button
            onClick={() => onChange(!checked)}
            className={`
        relative w-12 h-7 rounded-full transition-colors flex-shrink-0
        ${checked ? 'bg-[var(--color-primary)]' : 'bg-white/20'}
      `}
        >
            <div className={`
        absolute top-1 w-5 h-5 rounded-full bg-white shadow-md transition-transform
        ${checked ? 'translate-x-6' : 'translate-x-1'}
      `} />
        </button>
    </div>
);

export const SettingsView: React.FC = () => {
    const {
        theme,
        updateTheme,
        setTheme,
        resetTheme,
        exportSettings,
        importSettings,
        defaultCardCollapsed,
        setDefaultCardCollapsed,
        pagination,
        setPagination
    } = useTheme();

    const fileInputRef = useRef<HTMLInputElement>(null);
    const [importError, setImportError] = useState<string | null>(null);
    const [importSuccess, setImportSuccess] = useState(false);
    const [mobileCategory, setMobileCategory] = useState<SettingsCategory | null>(null);

    const handlePageSizeChange = (val: string) => {
        let size = parseInt(val, 10);
        if (isNaN(size)) return;

        // Clamp between 1 and 100
        size = Math.max(1, Math.min(100, size));
        setPagination({ ...pagination, pageSize: size });
    };

    const handleExport = () => {
        const json = exportSettings();
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'jules-theme-settings.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    const handleImport = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (event) => {
            const content = event.target?.result as string;
            const result = importSettings(content);
            if (result.success) {
                setImportSuccess(true);
                setImportError(null);
                setTimeout(() => setImportSuccess(false), 3000);
            } else {
                setImportError(result.error || 'Import failed');
                setTimeout(() => setImportError(null), 5000);
            }
        };
        reader.readAsText(file);

        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const colorSettings: { key: keyof Theme; label: string; description: string }[] = [
        { key: 'background', label: 'Background', description: 'Main app background' },
        { key: 'surface', label: 'Surface', description: 'Cards and panels' },
        { key: 'surfaceHighlight', label: 'Highlight', description: 'Hover states' },
        { key: 'border', label: 'Border', description: 'Borders and dividers' },
        { key: 'primary', label: 'Primary', description: 'Accent color' },
        { key: 'textMain', label: 'Text', description: 'Primary text' },
        { key: 'textMuted', label: 'Text Muted', description: 'Secondary text' },
    ];

    // Mobile Back Header
    const MobileHeader: React.FC<{ title: string; onBack: () => void }> = ({ title, onBack }) => (
        <div className="flex-shrink-0 px-4 py-3 border-b border-white/5 bg-[var(--color-surface)]/80 backdrop-blur-sm sticky top-0 z-10 flex items-center gap-3">
            <button onClick={onBack} className="p-2 -ml-2 hover:bg-white/5 rounded-lg text-[var(--color-text-muted)] hover:text-[var(--color-text-main)] transition-colors">
                <ArrowLeft size={20} />
            </button>
            <h1 className="text-lg font-semibold text-[var(--color-text-main)]">{title}</h1>
        </div>
    );

    // General Settings Section
    const GeneralSection = () => (
        <div className="divide-y divide-white/5">
            <ToggleSwitch
                checked={defaultCardCollapsed}
                onChange={setDefaultCardCollapsed}
                label="Collapse Cards by Default"
                description="Improve performance for long sessions"
            />
        </div>
    );

    // Pagination Settings Section
    const PaginationSection = () => (
        <div className="divide-y divide-white/5">
            <ToggleSwitch
                checked={pagination.autoPaginate}
                onChange={(checked) => setPagination({ ...pagination, autoPaginate: checked })}
                label="Automatic Pagination"
                description="Automatically fetch all pages of results"
            />
            <div className="flex items-center justify-between gap-4 p-4">
                <div className="space-y-0.5 flex-1">
                    <div className="text-sm font-medium text-[var(--color-text-main)]">Page Size</div>
                    <div className="text-xs text-[var(--color-text-muted)]">Number of items per request</div>
                </div>
                <input
                    type="number"
                    min="1"
                    max="100"
                    value={pagination.pageSize}
                    onChange={(e) => handlePageSizeChange(e.target.value)}
                    className="w-20 bg-black/30 border border-white/10 rounded-lg px-3 py-1.5 text-sm text-[var(--color-text-main)] focus:outline-none focus:border-[var(--color-primary)] transition-colors"
                />
            </div>
        </div>
    );

    // Appearance Section (Presets)
    const AppearanceSection = () => (
        <div className="p-4 space-y-4">
            <p className="text-sm text-[var(--color-text-muted)]">Choose a theme preset</p>
            <div className="grid grid-cols-2 gap-3">
                {Object.entries(PRESET_THEMES).map(([name, presetTheme]) => (
                    <button
                        key={name}
                        onClick={() => setTheme(presetTheme)}
                        className="group relative p-3 rounded-xl border border-white/10 hover:border-[var(--color-primary)] transition-all"
                        style={{ backgroundColor: presetTheme.surface }}
                    >
                        <div className="flex items-center gap-2 mb-2">
                            <div
                                className="w-4 h-4 rounded-full border border-white/20"
                                style={{ backgroundColor: presetTheme.primary }}
                            />
                            <span
                                className="text-sm font-medium capitalize"
                                style={{ color: presetTheme.textMain }}
                            >
                                {name.replace(/([A-Z])/g, ' $1').trim()}
                            </span>
                        </div>
                        <div className="flex gap-1">
                            {[presetTheme.background, presetTheme.surface, presetTheme.surfaceHighlight].map((color, i) => (
                                <div
                                    key={i}
                                    className="flex-1 h-2 rounded-full"
                                    style={{ backgroundColor: color }}
                                />
                            ))}
                        </div>
                    </button>
                ))}
            </div>
        </div>
    );

    // Theme Customization Section
    const ThemeSection = () => (
        <div className="divide-y divide-white/5">
            {colorSettings.map(({ key, label, description }) => (
                <ColorPicker
                    key={key}
                    label={label}
                    description={description}
                    value={theme[key]}
                    onChange={(color) => updateTheme({ [key]: color })}
                />
            ))}
        </div>
    );

    // Import/Export Section
    const ImportExportSection = () => (
        <div className="p-4 space-y-4">
            {importError && (
                <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
                    <AlertCircle size={16} />
                    {importError}
                </div>
            )}
            {importSuccess && (
                <div className="flex items-center gap-2 p-3 bg-green-500/10 border border-green-500/20 rounded-lg text-green-400 text-sm">
                    <Check size={16} />
                    Theme imported successfully!
                </div>
            )}

            <div className="space-y-3">
                <button
                    onClick={handleExport}
                    className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-[var(--color-primary)] hover:opacity-90 text-white font-medium rounded-xl transition-all"
                >
                    <Download size={18} />
                    Export Theme
                </button>

                <input
                    ref={fileInputRef}
                    type="file"
                    accept=".json"
                    onChange={handleImport}
                    className="hidden"
                />
                <button
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-white/10 hover:bg-white/20 text-[var(--color-text-main)] font-medium rounded-xl transition-all"
                >
                    <Upload size={18} />
                    Import Theme
                </button>

                <button
                    onClick={resetTheme}
                    className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-white/5 hover:bg-white/10 text-[var(--color-text-muted)] hover:text-[var(--color-text-main)] font-medium rounded-xl transition-all"
                >
                    <RotateCcw size={18} />
                    Reset to Default
                </button>
            </div>

            <p className="text-xs text-[var(--color-text-muted)] text-center">
                Export your theme to share or backup your settings
            </p>
        </div>
    );

    // About Section
    const AboutSection = () => (
        <div className="divide-y divide-white/5">
            <div className="p-4 flex items-center justify-between">
                <span className="text-sm text-[var(--color-text-main)]">Version</span>
                <span className="text-sm font-mono text-[var(--color-text-muted)]">v0.1.0-alpha</span>
            </div>
            <div className="p-4 flex items-center justify-between">
                <span className="text-sm text-[var(--color-text-main)]">Build</span>
                <span className="text-sm font-mono text-[var(--color-text-muted)]">Web</span>
            </div>
        </div>
    );

    // Mobile Category View Renderer
    const renderMobileCategory = () => {
        const sections: Record<SettingsCategory, { title: string; content: React.ReactNode }> = {
            'general': {
                title: 'General',
                content: (
                    <>
                        <GeneralSection />
                        <div className="px-4 py-2 bg-white/5 text-[10px] font-bold text-[var(--color-text-muted)] uppercase tracking-wider">Pagination</div>
                        <PaginationSection />
                    </>
                )
            },
            'appearance': { title: 'Appearance', content: <AppearanceSection /> },
            'theme': { title: 'Theme Colors', content: <ThemeSection /> },
            'import-export': { title: 'Import / Export', content: <ImportExportSection /> },
            'about': { title: 'About', content: <AboutSection /> },
        };

        if (!mobileCategory) return null;
        const section = sections[mobileCategory];

        return (
            <div className="h-full flex flex-col bg-[var(--color-background)]">
                <MobileHeader title={section.title} onBack={() => setMobileCategory(null)} />
                <div className="flex-1 overflow-y-auto">
                    <div className="bg-[var(--color-surface)] border-y border-white/5">
                        {section.content}
                    </div>
                </div>
            </div>
        );
    };

    // Mobile Main Categories List
    const MobileCategoriesList = () => (
        <div className="h-full flex flex-col bg-[var(--color-background)]">
            <div className="flex-shrink-0 px-4 py-3 border-b border-white/5 bg-[var(--color-surface)]/80 backdrop-blur-sm sticky top-0 z-10 flex items-center gap-3">
                <Link to="/" className="p-2 -ml-2 hover:bg-white/5 rounded-lg text-[var(--color-text-muted)] hover:text-[var(--color-text-main)] transition-colors">
                    <ArrowLeft size={20} />
                </Link>
                <h1 className="text-lg font-semibold text-[var(--color-text-main)]">Settings</h1>
            </div>

            <div className="flex-1 overflow-y-auto pb-safe">
                <div className="mt-2 bg-[var(--color-surface)] border-y border-white/5 divide-y divide-white/5">
                    <CategoryItem
                        icon={<Settings size={20} />}
                        label="General"
                        description="Card defaults, behavior"
                        onClick={() => setMobileCategory('general')}
                    />
                    <CategoryItem
                        icon={<Sun size={20} />}
                        label="Appearance"
                        description="Theme presets"
                        onClick={() => setMobileCategory('appearance')}
                    />
                    <CategoryItem
                        icon={<Palette size={20} />}
                        label="Theme Colors"
                        description="Customize colors"
                        onClick={() => setMobileCategory('theme')}
                    />
                    <CategoryItem
                        icon={<Download size={20} />}
                        label="Import / Export"
                        description="Backup and restore"
                        onClick={() => setMobileCategory('import-export')}
                    />
                </div>

                <div className="mt-6 bg-[var(--color-surface)] border-y border-white/5 divide-y divide-white/5">
                    <CategoryItem
                        icon={<Info size={20} />}
                        label="About"
                        description="Version info"
                        onClick={() => setMobileCategory('about')}
                    />
                </div>
            </div>
        </div>
    );

    // Desktop Layout
    const DesktopLayout = () => (
        <div className="flex-1 flex flex-col h-full bg-[var(--color-background)] text-[var(--color-text-main)] overflow-y-auto">
            {/* Header */}
            <div className="flex-shrink-0 px-6 py-4 border-b border-white/5 bg-[var(--color-surface)]/80 backdrop-blur-sm sticky top-0 z-10 flex items-center gap-4">
                <Link to="/" className="p-2 hover:bg-white/5 rounded-lg text-[var(--color-text-muted)] hover:text-[var(--color-text-main)] transition-colors">
                    <ArrowLeft size={20} />
                </Link>
                <h1 className="text-xl font-semibold text-[var(--color-text-main)]">Settings</h1>
            </div>

            <div className="max-w-4xl mx-auto w-full p-6 lg:p-8 space-y-8">
                {/* General */}
                <section className="space-y-3">
                    <h2 className="text-sm font-medium text-[var(--color-text-muted)] uppercase tracking-wider flex items-center gap-2 px-1">
                        <Settings size={14} /> General
                    </h2>
                    <div className="bg-[var(--color-surface)] border border-white/5 rounded-xl overflow-hidden">
                        <GeneralSection />
                    </div>
                </section>

                {/* Pagination */}
                <section className="space-y-3">
                    <h2 className="text-sm font-medium text-[var(--color-text-muted)] uppercase tracking-wider flex items-center gap-2 px-1">
                        <Sliders size={14} /> Pagination
                    </h2>
                    <div className="bg-[var(--color-surface)] border border-white/5 rounded-xl overflow-hidden">
                        <PaginationSection />
                    </div>
                </section>

                {/* Appearance */}
                <section className="space-y-3">
                    <h2 className="text-sm font-medium text-[var(--color-text-muted)] uppercase tracking-wider flex items-center gap-2 px-1">
                        <Sun size={14} /> Appearance
                    </h2>
                    <div className="bg-[var(--color-surface)] border border-white/5 rounded-xl overflow-hidden">
                        <AppearanceSection />
                    </div>
                </section>

                {/* Theme Colors */}
                <section className="space-y-3">
                    <h2 className="text-sm font-medium text-[var(--color-text-muted)] uppercase tracking-wider flex items-center gap-2 px-1">
                        <Palette size={14} /> Theme Colors
                    </h2>
                    <div className="bg-[var(--color-surface)] border border-white/5 rounded-xl overflow-hidden">
                        <ThemeSection />
                    </div>
                </section>

                {/* Import/Export */}
                <section className="space-y-3">
                    <h2 className="text-sm font-medium text-[var(--color-text-muted)] uppercase tracking-wider flex items-center gap-2 px-1">
                        <Download size={14} /> Import / Export
                    </h2>
                    <div className="bg-[var(--color-surface)] border border-white/5 rounded-xl overflow-hidden">
                        <ImportExportSection />
                    </div>
                </section>

                {/* About */}
                <section className="space-y-3">
                    <h2 className="text-sm font-medium text-[var(--color-text-muted)] uppercase tracking-wider flex items-center gap-2 px-1">
                        <Info size={14} /> About
                    </h2>
                    <div className="bg-[var(--color-surface)] border border-white/5 rounded-xl overflow-hidden">
                        <AboutSection />
                    </div>
                </section>
            </div>
        </div>
    );

    // Render based on screen size
    return (
        <>
            {/* Mobile Layout (< 640px) */}
            <div className="sm:hidden h-full">
                {mobileCategory ? renderMobileCategory() : <MobileCategoriesList />}
            </div>

            {/* Desktop Layout (>= 640px) */}
            <div className="hidden sm:flex h-full">
                <DesktopLayout />
            </div>
        </>
    );
};
