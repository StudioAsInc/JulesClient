import { useState, useEffect, useCallback } from 'react';
import { GeminiService } from '../services/geminiService';
import { JulesSource } from '../types';

export function useSources(service: GeminiService | null) {
    const [sources, setSources] = useState<JulesSource[]>([]);
    const [currentSource, setCurrentSource] = useState<JulesSource | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchSources = useCallback(async () => {
        if (!service) return;
        setIsLoading(true);
        setError(null);
        try {
            const response = await service.listSources();
            setSources(response.sources);

            // Default to first source if none selected
            if (response.sources.length > 0) {
                 setCurrentSource(prev => prev || response.sources[0]);
            }
        } catch (e: any) {
            console.error(e);
            if (e.message?.includes('Invalid API Key')) {
                 setError("Invalid API Key. Please reset.");
            } else {
                 setError(e.message || "Failed to load sources");
            }
        } finally {
            setIsLoading(false);
        }
    }, [service]);

    useEffect(() => {
        if (service) {
            fetchSources();
        }
    }, [service, fetchSources]);

    return {
        sources,
        currentSource,
        setCurrentSource,
        fetchSources,
        isLoading,
        error
    };
}
