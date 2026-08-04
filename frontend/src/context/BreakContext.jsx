/**
 * ============================================================================
 * BreakContext.jsx — TICKET-I124A
 * ============================================================================
 * WHAT:    Shared open-recon-breaks count via useReducer + Context, so the
 *          navbar badge and the dashboard StatCard update from one source
 *          without either refetching the other.
 * HOW:     <BreakProvider> hydrates from GET /api/v1/recon/results?status=OPEN
 *          on mount; RESOLVE/REOPEN adjust the count locally (optimistic
 *          resolve + rollback, wired in ResolveBreakModal in I124C).
 * ============================================================================
 */
import { createContext, useContext, useEffect, useReducer } from 'react';
import { getReconResults } from '../services/apiService.js';

const initial = { openCount: 0, lastUpdated: 0 };

function reducer(state, action) {
    switch (action.type) {
        case 'HYDRATE':
            return { openCount: action.count, lastUpdated: Date.now() };
        case 'RESOLVE':
            return { openCount: Math.max(0, state.openCount - 1), lastUpdated: Date.now() };
        case 'REOPEN':
            return { openCount: state.openCount + 1, lastUpdated: Date.now() };
        default:
            return state;
    }
}

const BreakContext = createContext(null);

export function BreakProvider({ children }) {
    const [state, dispatch] = useReducer(reducer, initial);

    useEffect(() => {
        getReconResults({ status: 'OPEN' })
            .then(page => {
                const results = page.content || page;
                dispatch({ type: 'HYDRATE', count: results.length });
            })
            .catch(() => { /* swallow — badge stays at 0 */ });
    }, []);

    return (
        <BreakContext.Provider value={{ state, dispatch }}>
            {children}
        </BreakContext.Provider>
    );
}

export function useBreaks() {
    const ctx = useContext(BreakContext);
    if (!ctx) throw new Error('useBreaks must be used inside <BreakProvider>');
    return ctx;
}
