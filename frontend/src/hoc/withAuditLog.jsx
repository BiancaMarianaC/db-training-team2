/**
 * ============================================================================
 * withAuditLog.jsx — TICKET-I124D
 * ============================================================================
 * WHAT:    HOC that logs mount + every render for the wrapped component,
 *          without changing its behavior or props.
 * WHY:     Also doubles as the debugging tool that proves BreakContext
 *          (I124A) isn't causing unrelated components to re-render — e.g.
 *          resolving a break should log the modal + badge, but NOT the
 *          trade table.
 * ============================================================================
 */
import { useEffect, useRef } from 'react';

export function withAuditLog(Component, label) {
    const name = label ?? Component.displayName ?? Component.name ?? 'Component';

    function AuditLoggedComponent(props) {
        const mounted = useRef(false);
        useEffect(() => {
            if (!mounted.current) {
                console.log(`[audit] ${name} mounted`);
                mounted.current = true;
            }
        }, []);
        console.log(`[audit] ${name} render`);
        return <Component {...props} />;
    }

    AuditLoggedComponent.displayName = `withAuditLog(${name})`;
    return AuditLoggedComponent;
}

export default withAuditLog;
