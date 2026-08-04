/**
 * ============================================================================
 * ErrorBoundary.jsx — TICKET-I124B
 * ============================================================================
 * WHAT:    Per-route error boundary — one broken page shouldn't white-screen
 *          the whole app.
 * HOW:     Class component (still the only way to catch render errors in
 *          React 18) implementing getDerivedStateFromError + componentDidCatch.
 * ============================================================================
 */
import { Component } from 'react';

export class ErrorBoundary extends Component {
    state = { hasError: false, error: null };

    static getDerivedStateFromError(error) {
        return { hasError: true, error };
    }

    componentDidCatch(error, info) {
        console.error('ErrorBoundary caught:', error, info);
    }

    render() {
        if (this.state.hasError) {
            return this.props.fallback ?? (
                <div role="alert" style={{ padding: 24 }}>
                    <h2>Something broke on this page.</h2>
                    <p>Refresh, or jump back to the dashboard.</p>
                </div>
            );
        }
        return this.props.children;
    }
}

export default ErrorBoundary;
