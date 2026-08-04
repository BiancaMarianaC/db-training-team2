import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import App from './App.jsx';

vi.mock('./pages/Dashboard.jsx', () => ({ default: () => <h1>Dashboard page</h1> }));
vi.mock('./pages/Trades.jsx', () => ({ default: () => <h1>Trades page</h1> }));
vi.mock('./components/AddTradeForm.jsx', () => ({ default: () => <h1>New trade page</h1> }));
vi.mock('./pages/Recon.jsx', () => ({ default: () => <h1>Recon page</h1> }));

afterEach(cleanup);

function renderAt(path) {
    return render(
        <MemoryRouter initialEntries={[path]}>
            <App />
        </MemoryRouter>
    );
}

describe('App routing', () => {
    it('redirects the root route to the dashboard', () => {
        renderAt('/');

        expect(screen.getByRole('heading', { name: 'Dashboard page' })).toBeInTheDocument();
    });

    it.each([
        ['/dashboard', 'Dashboard page'],
        ['/trades', 'Trades page'],
        ['/trades/new', 'New trade page'],
        ['/recon', 'Recon page']
    ])('renders %s', (path, heading) => {
        renderAt(path);

        expect(screen.getByRole('heading', { name: heading })).toBeInTheDocument();
    });

    it('uses NavLink active styling for exactly the current navigation item', () => {
        renderAt('/trades/new');

        expect(screen.getByRole('link', { name: '+ New Trade' })).toHaveClass('active');
        expect(screen.getByRole('link', { name: 'Trades' })).not.toHaveClass('active');
    });

    it('renders a 404 page and a route back to the dashboard', () => {
        renderAt('/does-not-exist');

        expect(screen.getByRole('heading', { name: '404 — Not Found' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'Back to dashboard' })).toHaveAttribute('href', '/dashboard');
    });
});
