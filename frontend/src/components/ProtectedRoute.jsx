import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { Loading } from './ui';

export default function ProtectedRoute({ role }) {
  const { user, ready } = useAuth();
  if (!ready) return <Loading />;
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'} replace />;
  }
  return <Outlet />;
}
