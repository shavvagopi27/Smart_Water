import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './layouts/AppLayout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Usage from './pages/Usage';
import Billing from './pages/Billing';
import Invoices from './pages/Invoices';
import Alerts from './pages/Alerts';
import Tips from './pages/Tips';
import Profile from './pages/Profile';
import AdminDashboard from './pages/admin/Dashboard';
import Apartments from './pages/admin/Apartments';
import Households from './pages/admin/Households';
import Residents from './pages/admin/Residents';
import Meters from './pages/admin/Meters';
import WaterUsage from './pages/admin/WaterUsage';
import Tariffs from './pages/admin/Tariffs';
import WaterPurchases from './pages/admin/WaterPurchases';
import BillingCycles from './pages/admin/BillingCycles';

function Home() {
  const { user, ready } = useAuth();
  if (!ready) return null;
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute role="RESIDENT" />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/usage" element={<Usage />} />
          <Route path="/billing" element={<Billing />} />
          <Route path="/invoices" element={<Invoices />} />
          <Route path="/alerts" element={<Alerts />} />
          <Route path="/tips" element={<Tips />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute role="ADMIN" />}>
        <Route element={<AppLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/apartments" element={<Apartments />} />
          <Route path="/admin/households" element={<Households />} />
          <Route path="/admin/residents" element={<Residents />} />
          <Route path="/admin/meters" element={<Meters />} />
          <Route path="/admin/water-usage" element={<WaterUsage />} />
          <Route path="/admin/tariffs" element={<Tariffs />} />
          <Route path="/admin/water-purchases" element={<WaterPurchases />} />
          <Route path="/admin/billing-cycles" element={<BillingCycles />} />
          <Route path="/admin/invoices" element={<Invoices />} />
          <Route path="/admin/alerts" element={<Alerts />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/profile" element={<Profile />} />
        </Route>
      </Route>

      <Route path="*" element={<Home />} />
    </Routes>
  );
}
