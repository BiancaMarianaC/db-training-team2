import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ResolveBreakModal from './ResolveBreakModal.jsx';

afterEach(cleanup);

describe('ResolveBreakModal', () => {
    it('renders nothing when closed', () => {
        render(<ResolveBreakModal open={false} breakId={1} onClose={vi.fn()} onConfirm={vi.fn()} />);
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('disables confirm until the reason has at least 5 characters', () => {
        render(<ResolveBreakModal open breakId={1} onClose={vi.fn()} onConfirm={vi.fn()} />);

        const confirmBtn = screen.getByRole('button', { name: /confirm resolve/i });
        expect(confirmBtn).toBeDisabled();

        fireEvent.change(screen.getByLabelText(/resolution reason/i), { target: { value: 'abcd' } });
        expect(confirmBtn).toBeDisabled();

        fireEvent.change(screen.getByLabelText(/resolution reason/i), { target: { value: 'abcde' } });
        expect(confirmBtn).not.toBeDisabled();
    });

    it('calls onConfirm with the reason and closes on success', async () => {
        const onConfirm = vi.fn().mockResolvedValue();
        const onClose = vi.fn();
        render(<ResolveBreakModal open breakId={7} onClose={onClose} onConfirm={onConfirm} />);

        fireEvent.change(screen.getByLabelText(/resolution reason/i), { target: { value: 'looks fine now' } });
        fireEvent.click(screen.getByRole('button', { name: /confirm resolve/i }));

        await vi.waitFor(() => expect(onClose).toHaveBeenCalled());
        expect(onConfirm).toHaveBeenCalledWith('looks fine now');
    });

    it('shows an inline error and stays open when onConfirm rejects', async () => {
        const onConfirm = vi.fn().mockRejectedValue(new Error('boom'));
        const onClose = vi.fn();
        render(<ResolveBreakModal open breakId={7} onClose={onClose} onConfirm={onConfirm} />);

        fireEvent.change(screen.getByLabelText(/resolution reason/i), { target: { value: 'looks fine now' } });
        fireEvent.click(screen.getByRole('button', { name: /confirm resolve/i }));

        expect(await screen.findByRole('alert')).toHaveTextContent('boom');
        expect(onClose).not.toHaveBeenCalled();
    });

    it('closes on Escape and on backdrop click', () => {
        const onClose = vi.fn();
        const { rerender } = render(<ResolveBreakModal open breakId={1} onClose={onClose} onConfirm={vi.fn()} />);

        fireEvent.keyDown(document, { key: 'Escape' });
        expect(onClose).toHaveBeenCalledTimes(1);

        rerender(<ResolveBreakModal open breakId={1} onClose={onClose} onConfirm={vi.fn()} />);
        fireEvent.click(screen.getByRole('dialog'));
        expect(onClose).toHaveBeenCalledTimes(2);
    });
});
