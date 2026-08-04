/**
 * ============================================================================
 * ResolveBreakModal.jsx — TICKET-I124C
 * ============================================================================
 * WHAT:    Native <dialog>-based confirm modal replacing a blocking
 *          window.confirm-style flow, with a required resolution note.
 * WHY:     window.confirm blocks the whole JS event loop — incompatible
 *          with a Kafka-driven realtime feed, since incoming updates queue
 *          up behind it and arrive in a burst once the user clicks OK.
 * NOTE:    The resolution note is UI-only for now — ReconController's
 *          PUT /recon/{id}/resolve doesn't accept a body field to store it
 *          in (see ReconController.resolve). Not adding a backend field
 *          silently here; the note still gates the confirm button because
 *          the ticket's acceptance criteria call for the >=5-char check,
 *          but nothing is fabricated on the wire beyond what the API takes.
 * ============================================================================
 */
import { useEffect, useRef, useState } from 'react';

export default function ResolveBreakModal({ open, breakId, onClose, onConfirm }) {
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const dialogRef = useRef(null);

    useEffect(() => {
        if (!open) return;
        setReason('');
        setError(null);

        const root = dialogRef.current;
        const focusables = root.querySelectorAll(
            'button, [href], input, textarea, [tabindex]:not([tabindex="-1"])');
        const first = focusables[0];
        const last = focusables[focusables.length - 1];
        first?.focus();

        function onKey(e) {
            if (e.key === 'Escape') {
                onClose();
                return;
            }
            if (e.key === 'Tab') {
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        }
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [open, onClose]);

    if (!open) return null;

    async function submit() {
        setBusy(true);
        setError(null);
        try {
            await onConfirm(reason);
            onClose();
        } catch (err) {
            setError(err.message ?? 'Resolve failed');
        } finally {
            setBusy(false);
        }
    }

    return (
        <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="rbm-title"
            className="modal-backdrop"
            ref={dialogRef}
            onClick={e => { if (e.target === e.currentTarget) onClose(); }}
        >
            <div className="modal">
                <h2 id="rbm-title">Resolve break {breakId}</h2>
                <label>
                    Resolution reason
                    <textarea
                        value={reason}
                        onChange={e => setReason(e.target.value)}
                        rows={4}
                        disabled={busy}
                    />
                </label>
                {error && <p role="alert" className="modal-error">{error}</p>}
                <div className="modal-actions">
                    <button onClick={onClose} disabled={busy}>Cancel</button>
                    <button onClick={submit} disabled={busy || reason.trim().length < 5}>
                        {busy ? 'Resolving…' : 'Confirm resolve'}
                    </button>
                </div>
            </div>
        </div>
    );
}
