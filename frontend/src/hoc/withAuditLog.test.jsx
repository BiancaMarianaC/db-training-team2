import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { withAuditLog } from './withAuditLog.jsx';

function Greeting({ name }) {
    return <p>Hello {name}</p>;
}

afterEach(cleanup);

describe('withAuditLog', () => {
    let logSpy;

    beforeEach(() => {
        logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
    });

    it('forwards props to the wrapped component', () => {
        const Wrapped = withAuditLog(Greeting, 'Greeting');
        render(<Wrapped name="Ops" />);

        expect(screen.getByText('Hello Ops')).toBeInTheDocument();
    });

    it('sets a displayName that identifies the wrapped component', () => {
        const Wrapped = withAuditLog(Greeting, 'Greeting');

        expect(Wrapped.displayName).toBe('withAuditLog(Greeting)');
    });

    it('logs a mount line once and a render line on every render', () => {
        const Wrapped = withAuditLog(Greeting, 'Greeting');
        const { rerender } = render(<Wrapped name="Ops" />);
        rerender(<Wrapped name="Ops2" />);

        const mountCalls = logSpy.mock.calls.filter(c => c[0] === '[audit] Greeting mounted');
        const renderCalls = logSpy.mock.calls.filter(c => c[0] === '[audit] Greeting render');

        expect(mountCalls).toHaveLength(1);
        expect(renderCalls.length).toBeGreaterThanOrEqual(2);
    });
});
