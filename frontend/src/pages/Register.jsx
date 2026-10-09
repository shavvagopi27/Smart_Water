import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { errorMessage } from '../services/api';
import { Notice } from '../components/ui';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await register({ ...form, phone: form.phone || null });
      navigate('/dashboard');
    } catch (err) { setError(errorMessage(err)); } finally { setBusy(false); }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>Create account</h1>
        <p className="muted">After registering, your apartment admin must link you to your flat.</p>
        <Notice>{error}</Notice>
        <label><span>Full name</span><input value={form.name} onChange={set('name')} required /></label>
        <label><span>Email</span><input type="email" value={form.email} onChange={set('email')} required /></label>
        <label><span>Phone (optional)</span><input value={form.phone} onChange={set('phone')} /></label>
        <label><span>Password (min 8 characters)</span><input type="password" minLength={8} value={form.password} onChange={set('password')} required /></label>
        <button className="btn primary block" disabled={busy}>{busy ? 'Creating…' : 'Register'}</button>
        <p className="muted center">Already registered? <Link to="/login">Sign in</Link></p>
      </form>
    </div>
  );
}
