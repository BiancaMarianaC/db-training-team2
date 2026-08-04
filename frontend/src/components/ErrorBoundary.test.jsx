import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ErrorBoundary } from './ErrorBoundary.jsx';

function Bomb() {
    throw new Error('boundary test');
}

afterEach(cleanup);

describe('ErrorBoundary', () => {
    let consoleErrorSpy;

    beforeEach(() => {
        // React logs the error to console too; silence it for a clean test run.
        consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    it('renders children when there is no error', () => {
        render(
            <ErrorBoundary>
                <p>All good</p>
            </ErrorBoundary>
        );

        expect(screen.getByText('All good')).toBeInTheDocument();
    });

    it('renders the fallback and logs when a child throws', () => {
        render(
            <ErrorBoundary>
                <Bomb />
            </ErrorBoundary>
        );

        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.getByText('Something broke on this page.')).toBeInTheDocument();
        expect(consoleErrorSpy).toHaveBeenCalledWith(
            'ErrorBoundary caught:', expect.any(Error), expect.anything()
        );
    });

    it('isolates the error to one boundary — a sibling boundary keeps rendering', () => {
        render(
            <>
                <ErrorBoundary>
                    <Bomb />
                </ErrorBoundary>
                <ErrorBoundary>
                    <p>Still alive</p>
                </ErrorBoundary>
            </>
        );

        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.getByText('Still alive')).toBeInTheDocument();
    });
});
