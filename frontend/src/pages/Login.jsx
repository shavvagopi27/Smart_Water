import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { errorMessage } from '../services/api';
import { Notice } from '../components/ui';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard'} replace />;

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const u = await login(email, password);
      navigate(u.role === 'ADMIN' ? '/admin/dashboard' : '/dashboard');
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>💧 SmartWater</h1>
        <p className="muted">Sign in to your apartment water portal</p>
        <Notice>{error}</Notice>
        <label><span>Email</span><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></label>
        <label><span>Password</span><input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
        <button className="btn primary block" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
        <p className="muted center">New resident? <Link to="/register">Create an account</Link></p>
        <div className="demo-box">
          <strong>Local demo credentials</strong>
          <div>Admin: admin@smartwater.local / Admin@12345</div>
          <div>Resident: resident1@smartwater.local / Resident@12345</div>
        </div>
      </form>
    </div>
  );
}
