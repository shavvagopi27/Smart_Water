import { useEffect, useState } from 'react';
import api, { errorMessage } from '../services/api';
import { useAuth } from '../hooks/useAuth';
import useFetch from '../hooks/useFetch';
import { Badge, Card, Notice, PageHeader } from '../components/ui';

export default function Profile() {
  const { user, refresh } = useAuth();
  const household = useFetch(user.householdId ? `/households/${user.householdId}` : null);
  const [form, setForm] = useState({ name: user.name, phone: user.phone || '' });
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  useEffect(() => setForm({ name: user.name, phone: user.phone || '' }), [user]);

  const save = async (e) => {
    e.preventDefault();
    setMsg(''); setErr('');
    try {
      await api.put(`/users/${user.id}`, { name: form.name, phone: form.phone || null });
      await refresh();
      setMsg('Profile updated.');
    } catch (e2) { setErr(errorMessage(e2)); }
  };

  const h = household.data;
  return (
    <>
      <PageHeader title="My profile" />
      <Notice>{err}</Notice>
      <Notice type="success">{msg}</Notice>
      <div className="grid-2">
        <Card title="Account">
          <form className="form-grid" onSubmit={save}>
            <label><span>Name</span><input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></label>
            <label><span>Phone</span><input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>
            <label><span>Email</span><input value={user.email} disabled /></label>
            <label><span>Role</span><div><Badge value={user.role} /></div></label>
            <div className="form-actions"><button className="btn primary">Save</button></div>
          </form>
        </Card>
        {user.role === 'RESIDENT' && (
          <Card title="My household">
            {!user.householdId ? <p className="muted">You are not linked to a household yet. Please ask your apartment admin.</p> : !h ? <p className="muted">Loading…</p> : (
              <div className="kv">
                <div><span>Apartment</span><strong>{h.apartmentName}</strong></div>
                <div><span>Flat</span><strong>{h.flatNumber}</strong></div>
                <div><span>Household size</span><strong>{h.householdSize}</strong></div>
                <div><span>Area</span><strong>{h.areaSqft} sq ft</strong></div>
                <div><span>Water meter</span><strong>{h.meterNumber || 'Not installed'}</strong></div>
                <div><span>Residents</span><strong>{h.residentNames.join(', ')}</strong></div>
              </div>
            )}
          </Card>
        )}
      </div>
    </>
  );
}
