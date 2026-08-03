/**
 * ============================================================================
 * App.jsx — TICKET-I110
 * ============================================================================
 * WHAT:    Top-level component + React Router config.
 * HOW:     <Routes> with one <Route> per page.
 * WHY:     Single source of routing truth. Easy to add new pages.
 * OBSERVE: Clicking a sidebar link doesn't reload the page — that's React
 *          Router doing client-side navigation.
 * ============================================================================
 */
import { Link, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import Trades from './pages/Trades.jsx';
import AddTradeForm from './components/AddTradeForm.jsx';
import Recon from './pages/Recon.jsx';

export default function App() {
    return (
        <div className="layout">
            <header className="topbar">
                <span className="logo">DB · TradeFlow</span>
                <span className="user">Logged in as <strong>trader</strong></span>
            </header>

            <div className="main">
                <nav className="sidebar" aria-label="Primary">
                    <ul>
                        <li><NavLink to="/dashboard" className={navClass}>Dashboard</NavLink></li>
                        <li><NavLink to="/trades" end className={navClass}>Trades</NavLink></li>
                        <li><NavLink to="/trades/new" className={navClass}>+ New Trade</NavLink></li>
                        <li><NavLink to="/recon" className={navClass}>Recon Breaks</NavLink></li>
                    </ul>
                </nav>

                <section className="content">
                    <Routes>
                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/trades" element={<Trades />} />
                        <Route path="/trades/new" element={<AddTradeForm />} />
                        <Route path="/recon" element={<Recon />} />
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </section>
            </div>
        </div>
    );
}

function navClass({ isActive }) {
    return isActive ? 'active' : '';
}

function NotFound() {
    return (
        <div>
            <h2>404 — Not Found</h2>
            <Link to="/dashboard">Back to dashboard</Link>
        </div>
    );
}
