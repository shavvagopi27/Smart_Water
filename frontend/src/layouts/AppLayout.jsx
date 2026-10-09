import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const residentNav = [
  ['/dashboard', 'Dashboard'], ['/usage', 'My Usage'], ['/billing', 'Current Bill'],
  ['/invoices', 'Invoices'], ['/alerts', 'Alerts'], ['/tips', 'Saving Tips'], ['/profile', 'Profile'],
];
const adminNav = [
  ['/admin/dashboard', 'Dashboard'], ['/admin/apartments', 'Apartments'], ['/admin/households', 'Households'],
  ['/admin/residents', 'Residents'], ['/admin/meters', 'Meters'], ['/admin/water-usage', 'Water Usage'],
  ['/admin/tariffs', 'Tariffs'], ['/admin/water-purchases', 'Water Purchases'],
  ['/admin/billing-cycles', 'Billing Cycles'], ['/admin/invoices', 'Invoices'], ['/admin/alerts', 'Alerts'],
  ['/profile', 'Profile'],
];

export default function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const nav = isAdmin ? adminNav : residentNav;

  return (
    <div className="shell">
      <aside className={`sidebar ${open ? 'open' : ''}`}>
        <div className="brand">💧 SmartWater</div>
        <nav onClick={() => setOpen(false)}>
          {nav.map(([to, label]) => (
            <NavLink key={to} to={to} className={({ isActive }) => (isActive ? 'active' : '')}>{label}</NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="who">
            <strong>{user.name}</strong>
            <span className="muted">{user.role}</span>
          </div>
          <button className="btn ghost" onClick={() => { logout(); navigate('/login'); }}>Sign out</button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <button className="btn ghost menu" onClick={() => setOpen(!open)} aria-label="Toggle menu">☰</button>
          <span className="muted">{isAdmin ? 'Apartment administration' : `Flat ${user.flatNumber || '—'}`}</span>
        </header>
        <main className="content"><Outlet /></main>
      </div>
    </div>
  );
}
