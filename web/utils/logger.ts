/**
 * Centralized logging utility for the Jules web client.
 * Provides a wrapper around the console methods to allow for
 * consistent logging and potential future extensions.
 */
export const Logger = {
    info: (...args: any[]) => {
        console.log(...args);
    },
    warn: (...args: any[]) => {
        console.warn(...args);
    },
    error: (...args: any[]) => {
        console.error(...args);
    },
    debug: (...args: any[]) => {
        console.debug(...args);
    }
};
